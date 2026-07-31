/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.bulkimport.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.scripts.handler.DSpaceRunnableHandler;
import org.dspace.services.factory.DSpaceServicesFactory;

/*
 * @author Jurgen Mamani
 */
public class ImportFileUtil {

    public static final String REMOTE = "REMOTE";
    public static final String FTP = "FTP";
    private static final Logger log = LogManager.getLogger(ImportFileUtil.class);
    private static final String LOCAL = "LOCAL";
    private static final String HTTP_PREFIX = "http:";

    private static final String HTTPS_PREFIX = "https:";

    private static final String LOCAL_PREFIX = "file:";

    private static final String FTP_PREFIX = "ftp:";

    private static final String UNKNOWN = "UNKNOWN";
    protected DSpaceRunnableHandler handler;

    public ImportFileUtil() {
    }

    public ImportFileUtil(DSpaceRunnableHandler handler) {
        this.handler = handler;
    }

    /**
     * This method check if the given {@param possibleChild} is contained in the specified
     * {@param possibleParent}, i.e. it's a sub-path of it.
     *
     * @param possibleParent
     * @param possibleChild
     * @return true if sub-path, false otherwise.
     */
    private static boolean isChild(File possibleParent, File possibleChild) {
        File parent = possibleChild.getParentFile();
        while (parent != null) {
            if (parent.equals(possibleParent)) {
                return true;
            }
            parent = parent.getParentFile();
        }
        return false;
    }

    protected String[] getAllowedIps() {
        return DSpaceServicesFactory.getInstance()
                                    .getConfigurationService()
                                    .getArrayProperty("allowed.ips.import");
    }

    public Optional<InputStream> getInputStream(String path) {
        String fileLocationType = getFileLocationTypeByPath(path);

        if (UNKNOWN.equals(fileLocationType)) {
            logWarning("File path is of UNKNOWN type: [" + path + "]");
            return Optional.empty();
        }

        return getInputStream(path, fileLocationType);
    }

    protected void logWarning(String message) {
        log.warn(message);
        if (handler != null) {
            handler.logWarning(message);
        }
    }


    private String getFileLocationTypeByPath(String path) {
        if (StringUtils.isNotBlank(path)) {
            if (path.startsWith(HTTP_PREFIX) || path.startsWith(HTTPS_PREFIX)) {
                return REMOTE;
            } else if (path.startsWith(LOCAL_PREFIX)) {
                return LOCAL;
            } else if (path.startsWith(FTP_PREFIX)) {
                return FTP;
            } else {
                return UNKNOWN;
            }
        }

        return UNKNOWN;
    }

    private Optional<InputStream> getInputStream(String path, String fileLocationType) {
        try {
            switch (fileLocationType) {
                case REMOTE:
                    return getInputStreamOfRemoteFile(path);
                case LOCAL:
                    return getInputStreamOfLocalFile(path);
                case FTP:
                    return getInputStreamOfFtpFile(path);
                default:
            }
        } catch (IOException e) {
            logError(e.getMessage());
        }

        return Optional.empty();
    }

    private void logError(String message) {
        log.error(message);
        if (handler != null) {
            handler.logError(message);
        }
    }

    private Optional<InputStream> getInputStreamOfLocalFile(String path) throws IOException {
        Path uploadPath = Paths.get(
            DSpaceServicesFactory.getInstance()
                                 .getConfigurationService()
                                 .getProperty("uploads.local-folder")
        );
        File file = uploadPath.resolve(path.replace(LOCAL_PREFIX + "//", ""))
                              .toFile()
                              .getCanonicalFile();
        File possibleParent = uploadPath.toFile().getCanonicalFile();
        if (!isChild(possibleParent, file)) {
            throw new IOException("Access to the specified file " + path + " is not allowed");
        }
        if (!file.exists()) {
            throw new IOException("file " + path + " is not found");
        }
        return Optional.of(FileUtils.openInputStream(file));
    }

    private Optional<InputStream> getInputStreamOfRemoteFile(String path) throws IOException {
        URL url = getUrl(path);
        if (!isHostAllowed(url, path)) {
            return Optional.empty();
        }
        return Optional.ofNullable(openStream(url));
    }

    /**
     * Checks whether the host of the given URL is allowed to be contacted by the bulk
     * import, according to the {@code allowed.ips.import} configuration property.
     * <p>
     * The same validation is applied to every URL-based retrieval (both HTTP/HTTPS and
     * FTP) to mitigate server-side request forgery: when an allow-list is configured only
     * the listed hosts can be reached. When the property is not set or empty no
     * restriction is applied and any host is allowed.
     * </p>
     *
     * @param url  the URL whose host has to be validated
     * @param path the original file path, used only for logging a rejected request
     * @return {@code true} if the host is allowed or no allow-list is configured,
     *         {@code false} otherwise
     */
    private boolean isHostAllowed(URL url, String path) {
        Set<String> allowedIps = Set.of(getAllowedIps());
        String host = url.getHost();

        if (allowedIps.isEmpty() || allowedIps.contains(host)) {
            return true;
        }

        // Log the rejected domain
        logWarning(String.format("Domain '%s' is not in the allowed list. Path: %s", host, path));

        return false;
    }

    protected InputStream openStream(URL url) {
        try {
            return url.openStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected URL getUrl(String path) throws IOException {
        return new URL(path);
    }

    private Optional<InputStream> getInputStreamOfFtpFile(String url) throws IOException {
        URL urlObject = getUrl(url);
        if (!isHostAllowed(urlObject, url)) {
            return Optional.empty();
        }
        URLConnection urlConnection = urlObject.openConnection();
        InputStream inputStream = urlConnection.getInputStream();
        if (inputStream == null) {
            logError(
                String.format("Failed to obtain InputStream for FTP URL: %s. getInputStream() returned null", url));
            return Optional.empty();
        }
        return Optional.of(inputStream);
    }
}
