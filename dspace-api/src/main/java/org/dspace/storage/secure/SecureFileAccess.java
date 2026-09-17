/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.secure;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Decent I/O path validation - not perfect when symlinks are used and we are writing
 * as 'toRealPath' check on the resolved path fails for new files
 *
 * @author Kim Shepherd <kim-at-shepherd-dot-nz>
 */
public final class SecureFileAccess {

    private SecureFileAccess() {}

    /**
     * Validate a given path against allowed base paths.
     *
     * Calculates the "real path" for as much of the given path as exists,
     * (traversing existing symlinks) with non-existent folders/files being
     * appended to the existing path, and then validating against the
     * allowed base paths.
     *
     * @param file the unvalidated file, usually derived from user input or configuration
     *             This MUST be an absolute path, and the caller is expected to calculate it based on best
     *             context (e.g. configured base path, CWD, dspace.dir, and so on)
     * @param allowedBasePaths list of allowed base paths for this use case as per system configuration
     * @param purpose the name of the calling component / use case for logging and inspection
     * @throws IOException on validation failure
     */
    public static Path validatePathForWrite(String file, List<String> allowedBasePaths, String purpose)
            throws IOException {
        Path filePath = Path.of(file);
        if (!filePath.isAbsolute()) {
            throw new IOException("Absolute path required for I/O (%s): %s".formatted(purpose, file));
        }

        Path resolvedPath = resolvePathForWrite(filePath);

        for (String allowedBasePath : allowedBasePaths) {
            if (allowedBasePath == null) {
                throw new IOException("Null base path can not be provided for validation");
            }

            Path basePath = Path.of(allowedBasePath)
                                .toRealPath()
                                .normalize();

            if (resolvedPath.startsWith(basePath)) {
                return resolvedPath;
            }
        }

        // If no valid path was resolved and returned by now
        // we raise an exception and treat this as illegal access
        throw new IOException("Illegal file path attempted for I/O (%s): %s".formatted(purpose, file));
    }

    /**
     * Resolves a Path intended for writing without requiring the complete path
     * to exist.
     *
     * Before validation, existing segments of the path are converted to the
     * "real path" (traversing symlinks), with non-existent segments of the path
     * are then appended to the real path in their original order.
     *
     * Note: The validated path reflects the state of the filesystem when this
     * method executes. It does not prevent another process from replacing a
     * validated path component before the caller opens the file.
     *
     * @param path the absolute path intended for writing
     * @return the normalized path formed from the real path of the nearest
     *         existing ancestor and any trailing nonexistent path elements
     * @throws IOException if no existing ancestor can be resolved, if an
     *         existing symbolic link is dangling, if filesystem access is
     *         denied, or if another I/O error occurs
     */
    private static Path resolvePathForWrite(Path path) throws IOException {
        Path normalizedPath = path.normalize();
        Path existingAncestor = normalizedPath;
        Deque<Path> nonexistentElements = new ArrayDeque<>();

        // Determine the existing and non-existing path segments
        while (true) {
            try {
                Files.readAttributes(
                    existingAncestor,
                    BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS
                );
                break;
            } catch (NoSuchFileException e) {
                Path fileName = existingAncestor.getFileName();
                Path parent = existingAncestor.getParent();

                if (fileName == null || parent == null) {
                    throw e;
                }

                nonexistentElements.addFirst(fileName);
                existingAncestor = parent;
            }
        }

        // Determine "real path" for existing segments
        Path resolvedPath = existingAncestor.toRealPath();

        // Append non-existing segments to real path
        for (Path element : nonexistentElements) {
            resolvedPath = resolvedPath.resolve(element);
        }

        return resolvedPath.normalize();
    }

    /**
     * Validate a given path against an allowed base path.
     * More secure than the 'write' variant because we can explicitly resolve links as well.
     *
     * @param file the unvalidated file, usually derived from user input or configuration
     *             This MUST be an absolute path, and the caller is expected to calculate it based on best
     *             context (e.g. configured base path, CWD, dspace.dir, and so on)
     * @param allowedBasePaths the allowed base paths for this use case as per system configuration
     * @param purpose the name of the calling component / use case for logging and inspection
     * @throws IOException on validation failure
     */
    public static Path validatePathForRead(String file, List<String> allowedBasePaths, String purpose)
            throws IOException {
        Path filePath = Path.of(file);
        if (!filePath.isAbsolute()) {
            throw new IOException("Absolute path required for I/O (%s): %s".formatted(purpose, file));
        }
        for (String allowedBasePath : allowedBasePaths) {
            Path basePath = Path.of(allowedBasePath)
                                .toRealPath()
                                .normalize();
            Path resolvedPath = basePath.resolve(file).toRealPath().normalize();
            if (resolvedPath.startsWith(basePath)) {
                return resolvedPath;
            }
        }
        // If no valid path was resolved and returned by now
        // we raise an exception and treat this as illegal access
        throw new IOException("Illegal file path attempted for I/O (%s): %s".formatted(purpose, file));
    }

    /**
     * Get a buffered reader after validating file path.
     * @param unvalidatedFile the unvalidated file, usually derived from user input or configuration
     * @param allowedBasePaths the allowed base paths for this use case as per system configuration
     * @param purpose the name of the calling component / use case for logging and inspection
     * @param charset the charset to use, or UTF-8 if null
     * @throws IOException on validation failure
     */
    public static BufferedReader getBufferedReader(String unvalidatedFile, List<String> allowedBasePaths,
            String purpose, Charset charset) throws IOException {
        if (charset == null) {
            charset = StandardCharsets.UTF_8;
        }
        Path validatedFile = validatePathForRead(unvalidatedFile, allowedBasePaths, purpose);
        return Files.newBufferedReader(validatedFile, charset);
    }

    /**
     * Get an input stream after validating file path.
     * @param unvalidatedFile the unvalidated file, usually derived from user input or configuration
     * @param allowedBasePaths the allowed base paths for this use case as per system configuration
     * @param purpose the name of the calling component / use case for logging and inspection
     * @throws IOException on validation failure
     */
    public static InputStream getInputStream(String unvalidatedFile, List<String> allowedBasePaths, String purpose)
            throws IOException {
        Path validatedFile = validatePathForRead(unvalidatedFile, allowedBasePaths, purpose);
        return Files.newInputStream(validatedFile);

    }

    /**
     * Get an output stream after validating file path. New files can't use toRealPath() for link calculation so
     * there is a bit of a trade-off in allowing some symlink traversal to occur
     * @param unvalidatedFile the unvalidated file, usually derived from user input or configuration
     * @param allowedBasePaths the allowed base paths for this use case as per system configuration
     * @param purpose the name of the calling component / use case for logging and inspection
     * @throws IOException on validation failure
     */
    public static OutputStream getOutputStream(String unvalidatedFile, List<String> allowedBasePaths, String purpose)
            throws IOException {
        Path validatedFile = validatePathForWrite(unvalidatedFile, allowedBasePaths, purpose);
        return Files.newOutputStream(validatedFile);
    }

    /**
     * Get a buffered writer after validating file path. As with {@link #getOutputStream}, new files
     * can't use toRealPath() for link calculation so there is a trade-off allowing some symlink
     * traversal to occur.
     * @param unvalidatedFile the unvalidated file, usually derived from user input or configuration
     * @param allowedBasePaths the allowed base paths for this use case as per system configuration
     * @param purpose the name of the calling component / use case for logging and inspection
     * @param charset the charset to use, or UTF-8 if null
     * @throws IOException on validation failure
     */
    public static BufferedWriter getBufferedWriter(String unvalidatedFile, List<String> allowedBasePaths,
            String purpose, Charset charset) throws IOException {
        if (charset == null) {
            charset = StandardCharsets.UTF_8;
        }
        Path validatedFile = validatePathForWrite(unvalidatedFile, allowedBasePaths, purpose);
        return Files.newBufferedWriter(validatedFile, charset);
    }

    /**
     * Calculate an absolute path (if not already absolute) using current working dir as a root
     * for relative file paths
     * @param file the relative or absolute file given as input
     * @return absolute path calculated from file and cwd
     */
    public static String calculateAbsolutePathUsingCwd(String file) {
        String filePath = file;
        Path path = Path.of(filePath);
        if (!path.isAbsolute()) {
            filePath = Path.of("").toAbsolutePath().resolve(path).normalize().toString();
        }
        return filePath;
    }

    /**
     * Calculate an absolute path (if not already absolute) using a given base dir as a root
     * for relative file paths
     * @param file the relative or absolute file given as input
     * @return absolute path calculated from file and base dir
     */
    public static String calculateAbsolutePathUsingBaseDir(String file, String baseDir) {
        String filePath = file;
        Path path = Path.of(filePath);
        if (!path.isAbsolute()) {
            filePath = Path.of(baseDir).toAbsolutePath().resolve(path).normalize().toString();
        }
        return filePath;
    }
}
