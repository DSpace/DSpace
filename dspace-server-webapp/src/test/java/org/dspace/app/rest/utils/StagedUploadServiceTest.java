/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.dspace.app.rest.model.StagedUploadRest;
import org.dspace.services.ConfigurationService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.web.server.ResponseStatusException;

public class StagedUploadServiceTest {
    private static final String RESULT = "/api/system/processes/42";
    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();
    private final UUID owner = UUID.randomUUID();
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    private Path directory;
    private StagedUploadService service;

    @Before
    public void setUp() {
        directory = temporary.getRoot().toPath();
        service = store(clock);
    }

    private StagedUploadService store(Clock time) {
        return new StagedUploadService(directory, 20, 4, 2, Duration.ofHours(1), time);
    }

    @Test
    public void existingMultipartLimitsApplyBeforeReservingAnUpload() throws Exception {
        ConfigurationService configuration = mock(ConfigurationService.class);
        when(configuration.getProperty(anyString(), anyString())).thenReturn(directory.toString());
        when(configuration.getIntProperty(anyString(), anyInt())).thenAnswer(call -> call.getArgument(1));
        MultipartProperties multipart = new MultipartProperties();
        multipart.setMaxFileSize(DataSize.ofBytes(6));
        multipart.setMaxRequestSize(DataSize.ofBytes(4));
        StagedUploadService configured = new StagedUploadService(configuration, multipart);
        assertEquals(413, assertThrows(ResponseStatusException.class,
            () -> configured.create(owner, "too-large.zip", 7)).getStatusCode().value());
        try (var entries = Files.list(directory)) {
            assertEquals(0, entries.count());
        }
        StagedUploadRest upload = configured.create(owner, "allowed.zip", 6);
        assertEquals(4, upload.chunkSize());
        assertEquals(0, upload.receivedBytes());
    }

    @Test
    public void retriesAndRestartPreserveCommittedBytes() throws Exception {
        StagedUploadRest upload = service.create(owner, "bundle.zip", 6);
        assertEquals(4, service.put(owner, upload.id(), 0, bytes(1, 2, 3, 4)).receivedBytes());
        service = store(clock);
        assertEquals(4, service.put(owner, upload.id(), 0, bytes(1, 2, 3, 4)).receivedBytes());
        assertEquals(6, service.put(owner, upload.id(), 4, bytes(5, 6)).receivedBytes());
        AtomicInteger calls = new AtomicInteger();
        assertEquals(RESULT, service.consume(owner, List.of(upload.id()), (files, record) -> {
            calls.incrementAndGet();
            assertEquals(1, files.size());
            assertEquals("bundle.zip", files.get(0).getOriginalFilename());
            try (InputStream input = files.get(0).getInputStream()) {
                assertArrayEquals(new byte[] {1, 2, 3, 4, 5, 6}, input.readAllBytes());
            }
            record.accept(RESULT);
            return RESULT;
        }));
        service = store(clock);
        assertEquals(RESULT, service.consume(owner, List.of(upload.id()), (files, record) -> {
            calls.incrementAndGet();
            return "/api/system/processes/99";
        }));
        assertEquals(1, calls.get());
        StagedUploadRest consumed = service.get(owner, upload.id());
        assertEquals(StagedUploadService.CONSUMED, consumed.state());
        assertEquals(RESULT, consumed.result());
        assertFalse(Files.exists(directory.resolve(upload.id().toString()).resolve("bundle")));
    }

    @Test
    public void severalUploadsAreConsumedTogetherAndOnlyOnce() throws Exception {
        StagedUploadRest first = service.create(owner, "first.bin", 4);
        StagedUploadRest second = service.create(owner, "second.bin", 2);
        service.put(owner, first.id(), 0, bytes(1, 2, 3, 4));
        service.put(owner, second.id(), 0, bytes(5, 6));
        AtomicInteger calls = new AtomicInteger();
        List<UUID> ordered = List.of(first.id(), second.id()).stream().sorted().toList();
        assertEquals(RESULT, service.consume(owner, List.of(second.id(), first.id()), (files, record) -> {
            calls.incrementAndGet();
            assertEquals(2, files.size());
            assertEquals(ordered.get(0).equals(first.id()) ? "first.bin" : "second.bin",
                         files.get(0).getOriginalFilename());
            record.accept(RESULT);
            return RESULT;
        }));
        assertEquals(RESULT, service.consume(owner, List.of(first.id(), second.id()), (files, record) -> RESULT));
        assertEquals(RESULT, service.consume(owner, List.of(second.id()), (files, record) -> RESULT));
        assertEquals(1, calls.get());
        // a consumed upload cannot be combined with a fresh one
        service = new StagedUploadService(directory, 20, 4, 3, Duration.ofHours(1), clock);
        StagedUploadRest third = service.create(owner, "third.bin", 4);
        service.put(owner, third.id(), 0, bytes(7, 8, 9, 0));
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> service.consume(owner,
            List.of(first.id(), third.id()), (files, record) -> RESULT)).getStatusCode().value());
        assertEquals(StagedUploadService.UPLOADING, service.get(owner, third.id()).state());
    }

    @Test
    public void partialAndOversizedBodiesNeverCommit() throws Exception {
        StagedUploadRest upload = service.create(owner, "bundle.zip", 6);
        assertEquals(400, assertThrows(ResponseStatusException.class,
            () -> service.put(owner, upload.id(), 0, bytes(1, 2))).getStatusCode().value());
        assertEquals(413, assertThrows(ResponseStatusException.class,
            () -> service.put(owner, upload.id(), 0, bytes(1, 2, 3, 4, 5))).getStatusCode().value());
        assertEquals(0, service.get(owner, upload.id()).receivedBytes());
    }

    @Test
    public void changedRetriesAndOutOfOrderChunksAreRejected() throws Exception {
        StagedUploadRest upload = service.create(owner, "bundle.zip", 6);
        assertThrows(ResponseStatusException.class, () -> service.put(owner, upload.id(), 4, bytes(5, 6)));
        service.put(owner, upload.id(), 0, bytes(1, 2, 3, 4));
        assertEquals(409, assertThrows(ResponseStatusException.class,
            () -> service.put(owner, upload.id(), 0, bytes(4, 3, 2, 1))).getStatusCode().value());
        assertThrows(ResponseStatusException.class, () -> service.put(owner, upload.id(), -4, bytes(1, 2, 3, 4)));
        assertThrows(ResponseStatusException.class, () -> service.put(owner, upload.id(), 1, bytes(1, 2, 3, 4)));
        assertEquals(4, service.get(owner, upload.id()).receivedBytes());
    }

    @Test
    public void anotherUserCannotReadWriteConsumeOrDelete() throws Exception {
        UUID stranger = UUID.randomUUID();
        StagedUploadRest upload = service.create(owner, "bundle.zip", 4);
        service.put(owner, upload.id(), 0, bytes(1, 2, 3, 4));
        assertEquals(404, assertThrows(ResponseStatusException.class,
            () -> service.get(stranger, upload.id())).getStatusCode().value());
        assertThrows(ResponseStatusException.class, () -> service.put(stranger, upload.id(), 0, bytes(1, 2, 3, 4)));
        assertThrows(ResponseStatusException.class, () -> service.delete(stranger, upload.id()));
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> service.consume(stranger,
            List.of(upload.id()), (files, record) -> RESULT)).getStatusCode().value());
        assertEquals(StagedUploadService.UPLOADING, service.get(owner, upload.id()).state());
    }

    @Test
    public void incompleteUploadCannotBeConsumed() throws Exception {
        StagedUploadRest upload = service.create(owner, "bundle.zip", 6);
        service.put(owner, upload.id(), 0, bytes(1, 2, 3, 4));
        AtomicInteger calls = new AtomicInteger();
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> service.consume(owner,
            List.of(upload.id()), (files, record) -> {
                calls.incrementAndGet();
                return RESULT;
            })).getStatusCode().value());
        assertEquals(0, calls.get());
        assertEquals(400, assertThrows(ResponseStatusException.class,
            () -> service.consume(owner, List.of(), (files, record) -> RESULT)).getStatusCode().value());
    }

    @Test
    public void failedHandoffWithKnownResultReturnsThatResult() throws Exception {
        StagedUploadRest upload = service.create(owner, "bundle.zip", 4);
        service.put(owner, upload.id(), 0, bytes(1, 2, 3, 4));
        assertThrows(IOException.class, () -> service.consume(owner, List.of(upload.id()), (files, record) -> {
            record.accept(RESULT);
            throw new IOException("Connection lost after the process was created");
        }));
        assertEquals(StagedUploadService.FAILED, service.get(owner, upload.id()).state());
        assertEquals(RESULT, service.consume(owner, List.of(upload.id()), (files, record) -> "other"));
    }

    @Test
    public void uncertainHandoffNeverConsumesAgain() throws Exception {
        StagedUploadRest upload = service.create(owner, "bundle.zip", 4);
        service.put(owner, upload.id(), 0, bytes(1, 2, 3, 4));
        assertThrows(IOException.class, () -> service.consume(owner, List.of(upload.id()), (files, record) -> {
            throw new IOException("Outcome unknown");
        }));
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> service.consume(owner,
            List.of(upload.id()), (files, record) -> RESULT)).getStatusCode().value());
        assertEquals(409, assertThrows(ResponseStatusException.class,
            () -> service.delete(owner, upload.id())).getStatusCode().value());
    }

    @Test
    public void cancelledAndExpiredUploadsAreRemoved() throws Exception {
        StagedUploadRest cancelled = service.create(owner, "cancel.zip", 4);
        service.delete(owner, cancelled.id());
        assertThrows(ResponseStatusException.class, () -> service.get(owner, cancelled.id()));
        StagedUploadRest expired = service.create(owner, "expire.zip", 4);
        service.put(owner, expired.id(), 0, bytes(1, 2, 3, 4));
        store(Clock.offset(clock, Duration.ofHours(2))).cleanup();
        assertFalse(Files.exists(directory.resolve(expired.id().toString())));
        assertFalse(Files.exists(directory.resolve(cancelled.id().toString())));
    }

    @Test
    public void locksPreventConcurrentWritesAndCleanup() throws Exception {
        StagedUploadRest upload = service.create(owner, "bundle.zip", 4);
        try (FileChannel channel = FileChannel.open(directory.resolve(upload.id().toString()).resolve("lock"),
                 StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock lock = channel.lock()) {
            assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> service.put(owner, upload.id(), 0, bytes(1, 2, 3, 4))).getStatusCode().value());
            store(Clock.offset(clock, Duration.ofHours(2))).cleanup();
        }
        assertEquals(0, service.get(owner, upload.id()).receivedBytes());
    }

    @Test
    public void limitsAndFilenamesAreValidatedPerUser() throws Exception {
        assertThrows(ResponseStatusException.class, () -> service.create(owner, "../bundle.zip", 4));
        assertThrows(ResponseStatusException.class, () -> service.create(owner, "bundle.zip", 21));
        assertThrows(ResponseStatusException.class, () -> service.create(owner, "bundle.zip", 0));
        service.create(owner, "first.zip", 4);
        service.create(owner, "second.zip", 4);
        assertEquals(429, assertThrows(ResponseStatusException.class,
            () -> service.create(owner, "third.zip", 4)).getStatusCode().value());
        // the quota is per user, not global
        service.create(UUID.randomUUID(), "other.zip", 4);
    }

    private ByteArrayInputStream bytes(int... values) {
        byte[] data = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            data[i] = (byte) values[i];
        }
        return new ByteArrayInputStream(data);
    }
}
