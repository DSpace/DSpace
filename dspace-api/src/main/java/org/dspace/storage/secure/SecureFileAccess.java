/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.secure;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
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
     * Validate a given path against an allowed base path. Does not attempt to calculate "real path"
     * before validation, as this breaks for new files which don't yet exist. This can make the resulting
     * validation still vulnerable to symlink traversal in some cases
     * @param file the unvalidated file, usually derived from user input or configuration
     * @param allowedBasePaths list of allowed base paths for this use case as per system configuration
     * @param purpose the name of the calling component / use case for logging and inspection
     * @throws IOException on validation failure
     */
    public static Path validatePathForWrite(String file, List<String> allowedBasePaths, String purpose)
            throws IOException {
        for (String allowedBasePath : allowedBasePaths) {
            Path basePath = Path.of(allowedBasePath)
                                .toRealPath()
                                .normalize();
            if (basePath == null) {
                throw new IOException("Null base path can not be provided for validation");
            }
            Path resolvedPath = basePath.resolve(file).normalize();
            if (resolvedPath.startsWith(basePath)) {
                return resolvedPath;
            }
        }
        
        // If no valid path was resolved and returned by now
        // we raise an exception and treat this as illegal access
        throw new IOException("Illegal file path attempted for I/O (%s): %s".formatted(purpose, file));
    }

    /**
     * Validate a given path against an allowed base path.
     * More secure than the 'write' variant because we can explicitly resolve links as well.
     *
     * @param file the unvalidated file, usually derived from user input or configuration
     * @param allowedBasePath the allowed base path for this use case as per system configuration
     * @param purpose the name of the calling component / use case for logging and inspection
     * @throws IOException on validation failure
     */
    public static Path validatePathForRead(String file, List<String> allowedBasePaths, String purpose)
            throws IOException {
        for (String allowedBasePath : allowedBasePaths) {
            Path basePath = Path.of(allowedBasePath)
                                .toRealPath()
                                .normalize();
            if (basePath == null) {
                throw new IOException("Null base path can not be provided for validation");
            }
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
     * Get an input stream after validating file path. Adds NOFOLLOW_LINKS link option to
     * help ensure some extra safety since new files can't use toRealPath() for link calculation
     * @param unvalidatedFile the unvalidated file, usually derived from user input or configuration
     * @param allowedBasePaths the allowed base paths for this use case as per system configuration
     * @param purpose the name of the calling component / use case for logging and inspection
     * @throws IOException on validation failure
     */
    public static OutputStream getOutputStream(String unvalidatedFile, List<String> allowedBasePaths, String purpose)
            throws IOException {
        Path validatedFile = validatePathForWrite(unvalidatedFile, allowedBasePaths, purpose);
        return Files.newOutputStream(validatedFile, LinkOption.NOFOLLOW_LINKS);
    }
}
