/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.statistics;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.inject.Inject;

import org.dspace.scripts.DSpaceRunnable;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Tool to fetch a release from a Github repository.
 *
 * @author mwood
 */
public class GetGithubRelease
        extends DSpaceRunnable<GetGithubReleaseScriptConfiguration> {
    @Inject
    GetGithubReleaseScriptConfiguration configuration;

    private boolean verbose;

    Path archiveFilePath;

    @Override
    public GetGithubReleaseScriptConfiguration getScriptConfiguration() {
        return configuration;
    }

    @Override
    public void setup() {
        // XXX This method intentionally left blank.
    }

    @Override
    public void internalRun()
            throws Exception {
        // If asked, display help and exit.
        if (commandLine.hasOption(GetGithubReleaseOptions.OPT_HELP)) {
            printHelp();
            return;
        }

        // Not help.  Remember whether verbose was required.
        verbose = commandLine.hasOption(GetGithubReleaseOptions.OPT_VERBOSE);

        // Identify the latest release.
        String owner = commandLine.getOptionValue(GetGithubReleaseOptions.OPT_OWNER);
        String repo = commandLine.getOptionValue(GetGithubReleaseOptions.OPT_REPO);
        URL releaseConnection = new URL(String.format("https://api.github.com/repos/%s/%s/releases/latest",
                owner, repo));
        @SuppressWarnings("UnusedAssignment")
        JSONObject releaseParsed = null;
        try (InputStream releaseStream = releaseConnection.openStream();) {
            releaseParsed = new JSONObject(new JSONTokener(releaseStream));
        }

        String archiveUrl = releaseParsed.getString("zipball_url");

        // Get the release's Zip archive and save it or extract members.
        URL archiveConnection = new URL(archiveUrl);
        try (InputStream archiveStream = archiveConnection.openStream();) {
            if (commandLine.hasOption(GetGithubReleaseOptions.OPT_FILE)) {
                try (ZipInputStream zipStream = new ZipInputStream(archiveStream);) {
                    extractZipMembers(zipStream,
                            commandLine.getOptionValues(GetGithubReleaseOptions.OPT_FILE));
                }
            } else {
                String archiveDate = releaseParsed.getString("published_at");
                archiveFilePath = Paths.get(owner + "-" + repo + "_" + archiveDate + ".zip");
                saveArchive(archiveStream, archiveFilePath);
            }
        }
    }

    /**
     * Copy the retrieved archive stream to a local file.
     *
     * @param archiveStream body of the GET response.
     * @param outputPath where to write the archive.
     * @throws IOException passed through.
     */
    private static void saveArchive(InputStream archiveStream, Path outputPath)
            throws IOException {
        Files.copy(archiveStream, outputPath);
    }

    /**
     * Pick zero or more elements out of the archive and write copies of them to
     * the current directory.
     * <em>Any existing version of a selected file will be replaced.</em>
     *
     * <p>
     * For some reason, Github invents an unpredictable parent directory for the
     * release content.  This method strips that leading path element when
     * matching paths so that the user doesn't have to know it.
     *
     * @param archive body of the GET response.
     * @param paths paths to desired elements within the bogus parent directory.
     * @throws IOException passed through.
     */
    private void extractZipMembers(ZipInputStream archive, String... paths)
            throws IOException {
        while (true) {
            ZipEntry entry = archive.getNextEntry();
            if (null == entry) {
                break;
            }
            String entryName = entry.getName();
            Path entryPath = Paths.get(entryName);

            // Elide the entry's leading path element as discussed above.
            int pathLength = entryPath.getNameCount();
            Path trimmedPath = pathLength > 1
                    ? entryPath.subpath(1, pathLength)
                    : entryPath;

            // See if this entry is wanted, and save a copy if so.
            for (String candidateName : paths) {
                if (verbose) {
                    handler.logDebug(String.format(
                            "entryName = %s; trimmedPath = %s; candidateName = %s%n",
                            entryName, trimmedPath.toString(), candidateName));
                }
                if (trimmedPath.equals(Paths.get(candidateName))) {
                    if (verbose) {
                        handler.logInfo(String.format("Writing %s%n",
                                trimmedPath.getFileName()));
                    }
                    Files.copy(archive, trimmedPath.getFileName(),
                            StandardCopyOption.REPLACE_EXISTING);
                }
            }
            archive.closeEntry();
        }
    }

    /**
     * Helper method for testing.
     *
     * @return path to where the retrieved archive was stored.
     */
    Path getArchiveFilePath() {
        return archiveFilePath;
    }
}
