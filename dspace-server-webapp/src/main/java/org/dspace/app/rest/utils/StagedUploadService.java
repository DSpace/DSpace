/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.StagedUploadRest;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Disk-backed, bounded staging of uploads for any authenticated user. A file arrives in sequential
 * chunks and is later consumed by a target endpoint (a script process, a workspace item, ...) that
 * reads it as a {@link MultipartFile}. Every operation locks its upload on disk, so retries and
 * backend nodes sharing a filesystem cannot modify the same upload concurrently. Upload directories
 * and their contents are exclusively managed by this service.
 */
@Service
public class StagedUploadService {
    public static final String UPLOADING = "UPLOADING";
    public static final String CONSUMING = "CONSUMING";
    public static final String CONSUMED = "CONSUMED";
    public static final String FAILED = "FAILED";
    private static final Logger log = LogManager.getLogger();
    private final Path root;
    private final long maxSize;
    private final int chunkSize;
    private final int maxPerUser;
    private final Duration retention;
    private final Clock clock;

    /** Initialize staging from DSpace configuration. */
    @Autowired
    public StagedUploadService(ConfigurationService configuration, MultipartProperties multipart) {
        this(Path.of(configuration.getProperty("staged-upload.directory",
                 configuration.getProperty("dspace.dir") + "/var/staged-uploads")),
             multipart.getMaxFileSize().toBytes() < 0 ? Long.MAX_VALUE : multipart.getMaxFileSize().toBytes(),
             (int) Math.min(configuration.getIntProperty("staged-upload.chunk-size", 8 * 1024 * 1024),
                 multipart.getMaxRequestSize().toBytes() < 0 ? Integer.MAX_VALUE
                     : multipart.getMaxRequestSize().toBytes()),
             configuration.getIntProperty("staged-upload.max-active-per-user", 8),
             Duration.ofHours(configuration.getIntProperty("staged-upload.retention-hours", 24)),
             Clock.systemUTC());
    }

    /** Initialize an isolated store, also used by filesystem tests. */
    public StagedUploadService(Path root, long maxSize, int chunkSize, int maxPerUser, Duration retention,
                               Clock clock) {
        if (maxSize <= 0 || chunkSize <= 0 || maxPerUser <= 0 || retention.isNegative() || retention.isZero()) {
            throw new IllegalArgumentException("Staged upload limits must be positive");
        }
        this.root = root;
        this.maxSize = maxSize;
        this.chunkSize = chunkSize;
        this.maxPerUser = maxPerUser;
        this.retention = retention;
        this.clock = clock;
    }

    /** Create an upload owned by the authenticated user, reserving one of that user's staging slots. */
    public StagedUploadRest create(UUID owner, String name, long size) throws IOException {
        if (name == null || name.isBlank() || name.length() > 255 || name.contains("..")
            || name.contains("/") || name.contains("\\") || name.chars().anyMatch(Character::isISOControl)) {
            throw failure(HttpStatus.BAD_REQUEST, "Invalid filename");
        }
        if (size <= 0 || size > maxSize) {
            throw failure(HttpStatus.PAYLOAD_TOO_LARGE, "Maximum file size is " + maxSize + " bytes");
        }
        Files.createDirectories(root);
        try (FileChannel channel = FileChannel.open(root.resolve("creation.lock"),
                 StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock lock = acquire(channel)) {
            if (activeUploadsOf(owner) >= maxPerUser) {
                throw failure(HttpStatus.TOO_MANY_REQUESTS, "Too many active uploads for this user");
            }
            UUID id = UUID.randomUUID();
            Path directory = Files.createDirectory(root.resolve(id.toString()));
            Properties metadata = new Properties();
            metadata.setProperty("owner", owner.toString());
            metadata.setProperty("name", name);
            metadata.setProperty("size", Long.toString(size));
            metadata.setProperty("chunkSize", Integer.toString(chunkSize));
            try (OutputStream output = Files.newOutputStream(directory.resolve("metadata"))) {
                metadata.store(output, "Staged upload");
            }
            touch(directory);
            return status(directory, id, metadata);
        }
    }

    /** Read committed progress, enforcing ownership even for administrators. */
    public StagedUploadRest get(UUID owner, UUID id) throws Exception {
        return locked(owner, id, (directory, metadata) -> status(directory, id, metadata));
    }

    /**
     * Commit the next chunk atomically. A retry at an earlier offset must have identical bytes.
     * Partial bodies are never counted as committed progress.
     */
    public StagedUploadRest put(UUID owner, UUID id, long offset, InputStream input) throws Exception {
        return locked(owner, id, (directory, metadata) -> {
            StagedUploadRest current = status(directory, id, metadata);
            if (!UPLOADING.equals(current.state())) {
                throw failure(HttpStatus.CONFLICT, "Upload is already being consumed");
            }
            if (offset < 0 || offset >= current.size() || offset % current.chunkSize() != 0
                || offset > current.receivedBytes()) {
                throw failure(HttpStatus.CONFLICT, "Chunk offset does not match committed upload data");
            }
            long expected = Math.min(current.chunkSize(), current.size() - offset);
            Path temporary = Files.createTempFile(directory, "chunk-", ".tmp");
            try {
                try (OutputStream output = Files.newOutputStream(temporary)) {
                    copyExactly(input, output, expected);
                }
                Path chunk = directory.resolve(offset + ".part");
                if (Files.exists(chunk)) {
                    if (Files.mismatch(temporary, chunk) != -1) {
                        throw failure(HttpStatus.CONFLICT, "Retried chunk differs from committed data");
                    }
                } else {
                    Files.move(temporary, chunk, StandardCopyOption.ATOMIC_MOVE);
                }
                touch(directory);
                return status(directory, id, metadata);
            } finally {
                Files.deleteIfExists(temporary);
            }
        });
    }

    /**
     * Hand complete uploads to a target once. The target records the link of what it created as soon
     * as that exists, before anything that can still fail, so a repeated request returns the same
     * result instead of consuming again. Uploads are locked in identifier order. A crash between the
     * target's action and its receipt leaves CONSUMING markers that are deliberately never retried.
     * @param owner the authenticated user
     * @param ids uploads to consume together, all complete and owned by the user
     * @param target the consumer of the staged files
     * @return the recorded result link
     * @throws Exception whatever the target throws; the uploads are then marked FAILED
     */
    public String consume(UUID owner, List<UUID> ids, Target target) throws Exception {
        List<UUID> ordered = ids == null ? List.of() : ids.stream().distinct().sorted().toList();
        if (ordered.isEmpty()) {
            throw failure(HttpStatus.BAD_REQUEST, "No uploads to consume");
        }
        return lockedAll(owner, ordered, new ArrayList<>(), uploads -> {
            List<String> results = uploads.stream().map(upload -> readMarker(upload.directory(), "result")).toList();
            if (results.stream().allMatch(Objects::nonNull) && results.stream().distinct().count() == 1) {
                return results.get(0);
            }
            for (Locked upload : uploads) {
                StagedUploadRest current = status(upload.directory(), upload.id(), upload.metadata());
                if (!UPLOADING.equals(current.state()) || current.receivedBytes() != current.size()) {
                    throw failure(HttpStatus.CONFLICT,
                                  "Upload " + upload.id() + " is incomplete or has already been consumed");
                }
            }
            List<MultipartFile> files = new ArrayList<>();
            for (Locked upload : uploads) {
                StagedUploadRest current = status(upload.directory(), upload.id(), upload.metadata());
                Path assembled = upload.directory().resolve("bundle");
                try (OutputStream output = Files.newOutputStream(assembled)) {
                    for (long offset = 0; offset < current.size(); offset += current.chunkSize()) {
                        Files.copy(upload.directory().resolve(offset + ".part"), output);
                    }
                }
                writeMarker(upload.directory().resolve("state"), CONSUMING);
                touch(upload.directory());
                files.add(new StagedFile(assembled, current.name()));
            }
            try {
                String result = target.consume(files, value -> {
                    try {
                        if (value == null || value.isBlank()) {
                            throw new IOException("Invalid result receipt");
                        }
                        for (Locked upload : uploads) {
                            if (Files.exists(upload.directory().resolve("result"))) {
                                throw new IOException("Duplicate result receipt for upload " + upload.id());
                            }
                            writeMarker(upload.directory().resolve("result"), value);
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
                for (Locked upload : uploads) {
                    if (!Objects.equals(result, readMarker(upload.directory(), "result"))) {
                        throw new IOException("Result receipt does not match the target's result");
                    }
                    removeData(upload.directory());
                    writeMarker(upload.directory().resolve("state"), CONSUMED);
                }
                return result;
            } catch (Exception e) {
                for (Locked upload : uploads) {
                    writeMarker(upload.directory().resolve("state"), FAILED);
                }
                throw e;
            }
        });
    }

    /** Cancel staging; this never deletes what an earlier handoff created. */
    public void delete(UUID owner, UUID id) throws Exception {
        locked(owner, id, (directory, metadata) -> {
            if (Files.exists(directory.resolve("state"))) {
                throw failure(HttpStatus.CONFLICT, "Uploads that are being or have been consumed cannot be cancelled");
            }
            removeData(directory);
            Files.delete(directory.resolve("metadata"));
            return null;
        });
    }

    /** Remove expired staging data and handoff receipts, skipping uploads a request currently holds. */
    @Scheduled(fixedDelay = 3600000)
    public void cleanup() throws IOException {
        if (!Files.isDirectory(root)) {
            return;
        }
        try (FileChannel channel = FileChannel.open(root.resolve("creation.lock"),
                 StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock lock = acquire(channel);
             Stream<Path> entries = Files.list(root)) {
            for (Path directory : entries.filter(Files::isDirectory).toList()) {
                try {
                    UUID id = UUID.fromString(directory.getFileName().toString());
                    locked(null, id, (path, metadata) -> {
                        if (Files.exists(path.resolve("metadata"))
                            && Files.getLastModifiedTime(path.resolve("metadata"))
                            .toInstant().plus(retention).isAfter(clock.instant())) {
                            return null;
                        }
                        removeData(path);
                        Files.deleteIfExists(path.resolve("metadata"));
                        Files.deleteIfExists(path.resolve("state"));
                        Files.deleteIfExists(path.resolve("result"));
                        return null;
                    });
                    // Keep the lock inode until no metadata remains; identifiers are never reused.
                    if (!Files.exists(directory.resolve("metadata"))) {
                        Files.deleteIfExists(directory.resolve("lock"));
                        Files.deleteIfExists(directory);
                    }
                } catch (ResponseStatusException e) {
                    // Busy or already removed by another node.
                } catch (Exception e) {
                    log.warn("Could not clean staged upload {}", directory.getFileName(), e);
                }
            }
        }
    }

    private long activeUploadsOf(UUID owner) throws IOException {
        try (Stream<Path> entries = Files.list(root)) {
            return entries.filter(Files::isDirectory).filter(path -> Files.exists(path.resolve("metadata")))
                          .filter(path -> owner.toString().equals(readOwner(path)))
                          .filter(path -> !CONSUMED.equals(readMarker(path, "state"))).count();
        }
    }

    private String readOwner(Path directory) {
        Properties metadata = new Properties();
        try (InputStream input = Files.newInputStream(directory.resolve("metadata"))) {
            metadata.load(input);
        } catch (IOException e) {
            return null;
        }
        return metadata.getProperty("owner");
    }

    private <T> T locked(UUID owner, UUID id, Operation<T> operation) throws Exception {
        Path directory = root.resolve(id.toString());
        if (!Files.isDirectory(directory)) {
            throw failure(HttpStatus.NOT_FOUND, "Upload not found");
        }
        try (FileChannel channel = FileChannel.open(directory.resolve("lock"),
                 StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock lock = acquire(channel)) {
            Properties metadata = new Properties();
            if (Files.exists(directory.resolve("metadata"))) {
                try (InputStream input = Files.newInputStream(directory.resolve("metadata"))) {
                    metadata.load(input);
                }
            }
            if (owner != null && !owner.toString().equals(metadata.getProperty("owner"))) {
                throw failure(HttpStatus.NOT_FOUND, "Upload not found");
            }
            return operation.run(directory, metadata);
        }
    }

    private <T> T lockedAll(UUID owner, List<UUID> ids, List<Locked> acquired, MultiOperation<T> operation)
        throws Exception {
        if (acquired.size() == ids.size()) {
            return operation.run(acquired);
        }
        UUID id = ids.get(acquired.size());
        return locked(owner, id, (directory, metadata) -> {
            acquired.add(new Locked(id, directory, metadata));
            return lockedAll(owner, ids, acquired, operation);
        });
    }

    private FileLock acquire(FileChannel channel) throws IOException {
        try {
            FileLock lock = channel.tryLock();
            if (lock != null) {
                return lock;
            }
        } catch (OverlappingFileLockException e) {
            // Another thread in this JVM is operating on this upload.
        }
        throw failure(HttpStatus.CONFLICT, "Upload is busy; retry later");
    }

    private StagedUploadRest status(Path directory, UUID id, Properties metadata) throws IOException {
        long size = Long.parseLong(metadata.getProperty("size"));
        int blockSize = Integer.parseInt(metadata.getProperty("chunkSize"));
        String result = readMarker(directory, "result");
        String state = readMarker(directory, "state");
        if (state == null) {
            state = UPLOADING;
        }
        long received = 0;
        while (received < size && Files.exists(directory.resolve(received + ".part"))) {
            long length = Files.size(directory.resolve(received + ".part"));
            if (length != Math.min(blockSize, size - received)) {
                throw new IOException("Invalid stored chunk length");
            }
            received += length;
        }
        return new StagedUploadRest(id, metadata.getProperty("name"), size, blockSize,
                                    CONSUMED.equals(state) ? size : received, state, result);
    }

    private void touch(Path directory) throws IOException {
        Files.setLastModifiedTime(directory.resolve("metadata"), FileTime.from(clock.instant()));
    }

    private String readMarker(Path directory, String name) {
        try {
            return Files.exists(directory.resolve(name)) ? Files.readString(directory.resolve(name)) : null;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Publish complete markers atomically, never a partially written receipt. */
    private void writeMarker(Path destination, String value) throws IOException {
        Path temporary = Files.createTempFile(destination.getParent(), "marker-", ".tmp");
        try {
            Files.writeString(temporary, value);
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                channel.force(true);
            }
            Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private void removeData(Path directory) throws IOException {
        try (Stream<Path> entries = Files.list(directory)) {
            for (Path file : entries.toList()) {
                String name = file.getFileName().toString();
                if (name.endsWith(".part") || name.endsWith(".tmp") || name.equals("bundle")) {
                    Files.deleteIfExists(file);
                }
            }
        }
    }

    private void copyExactly(InputStream input, OutputStream output, long expected) throws IOException {
        byte[] buffer = new byte[8192];
        long remaining = expected;
        while (remaining > 0) {
            int length = input.read(buffer, 0, (int) Math.min(buffer.length, remaining));
            if (length == -1) {
                throw failure(HttpStatus.BAD_REQUEST, "Incomplete chunk");
            }
            output.write(buffer, 0, length);
            remaining -= length;
        }
        if (input.read() != -1) {
            throw failure(HttpStatus.PAYLOAD_TOO_LARGE, "Chunk exceeds expected length");
        }
    }

    private static ResponseStatusException failure(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }

    @FunctionalInterface
    private interface Operation<T> {
        T run(Path directory, Properties metadata) throws Exception;
    }

    @FunctionalInterface
    private interface MultiOperation<T> {
        T run(List<Locked> uploads) throws Exception;
    }

    private record Locked(UUID id, Path directory, Properties metadata) {
    }

    /** Adapter to whatever consumes staged files. */
    @FunctionalInterface
    public interface Target {
        /**
         * Consume the files. Record the link of the created resource as soon as it exists, before any step
         * that can still fail, so a retried handoff can return it.
         * @param files the staged files, in the order of the requested upload identifiers
         * @param recordResult receives the link of the created resource, exactly once
         * @return that same link
         * @throws Exception if consuming fails
         */
        String consume(List<MultipartFile> files, Consumer<String> recordResult) throws Exception;
    }

    private record StagedFile(Path path, String filename) implements MultipartFile {
        @Override
        public String getName() {
            return "file";
        }

        @Override
        public String getOriginalFilename() {
            return filename;
        }

        @Override
        public String getContentType() {
            return "application/octet-stream";
        }

        @Override
        public boolean isEmpty() {
            return getSize() == 0;
        }

        @Override
        public long getSize() {
            try {
                return Files.size(path);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public byte[] getBytes() throws IOException {
            return Files.readAllBytes(path);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return Files.newInputStream(path);
        }

        @Override
        public void transferTo(File destination) throws IOException {
            Files.copy(path, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
