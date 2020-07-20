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
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Tool to fetch a release from a Github repository.
 *
 * @author mwood
 */
public class GetGithubRelease {
    private static final String OPT_HELP = "h";
    private static final String OPT_FILE = "f";
    private static final String OPT_OWNER = "o";
    private static final String OPT_REPO = "r";
    private static final String OPT_VERBOSE = "v";

    private static boolean verbose;

    private GetGithubRelease() { }

    public static void main(String[] argv) throws MalformedURLException {
        // Parse the command line.
        Options options = new Options();
        Option option;

        options.addOption(OPT_HELP, "help", false, "describe options");

        option = Option.builder(OPT_FILE)
                .longOpt("file")
                .hasArg()
                .hasArgs() // Repeatable
                .desc("path to extract from archive (repeatable)")
                .argName("path")
                .build();
        options.addOption(option);

        option = Option.builder(OPT_OWNER)
                .longOpt("owner")
                .hasArg()
                .desc("Owner of the repository")
                .required()
                .build();
        options.addOption(option);

        option = Option.builder(OPT_REPO)
                .longOpt("repository")
                .hasArg()
                .desc("Repository having the release")
                .required()
                .build();
        options.addOption(option);

        options.addOption("v", "verbose", false, "Show lots of debugging information");

        DefaultParser commandParser = new DefaultParser();
        @SuppressWarnings("UnusedAssignment")
        CommandLine command = null;
        try {
            command = commandParser.parse(options, argv);
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            printHelp(options);
            System.exit(1);
        }

        // If asked, display help and exit.
        if (command.hasOption(OPT_HELP)) {
            printHelp(options);
            System.exit(0);
        }

        // Not help.  Remember whether verbose was required.
        verbose = command.hasOption(OPT_VERBOSE);

        // Identify the latest release.
        String owner = command.getOptionValue(OPT_OWNER);
        String repo = command.getOptionValue(OPT_REPO);
        URL releaseConnection = new URL(String.format("https://api.github.com/repos/%s/%s/releases/latest",
                owner, repo));
        @SuppressWarnings("UnusedAssignment")
        JSONObject releaseParsed = null;
        try (InputStream releaseStream = releaseConnection.openStream();) {
            releaseParsed = new JSONObject(new JSONTokener(releaseStream));
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

        String archiveUrl = releaseParsed.getString("zipball_url");

        // Get the release's Zip archive and save it or extract members.
        URL archiveConnection = new URL(archiveUrl);
        try (InputStream archiveStream = archiveConnection.openStream();) {
            if (command.hasOption(OPT_FILE)) {
                extractZipMembers(new ZipInputStream(archiveStream),
                        command.getOptionValues(OPT_FILE));
            } else {
                String archiveDate = releaseParsed.getString("published_at");
                Path archiveFilePath = Paths.get(owner + "-" + repo + "_" + archiveDate + ".zip");
                saveArchive(archiveStream, archiveFilePath);
            }
        } catch (IOException e) {
            System.err.format("%s:  %s%n", e.getClass().getSimpleName(), e.getMessage());
            System.exit(1);
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
    private static void extractZipMembers(ZipInputStream archive, String... paths)
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
                    System.err.format("entryName = %s; trimmedPath = %s; candidateName = %s%n",
                            entryName, trimmedPath.toString(), candidateName);
                }
                if (trimmedPath.equals(Paths.get(candidateName))) {
                    if (verbose) {
                        System.err.format("Writing %s%n", trimmedPath.getFileName());
                    }
                    Files.copy(archive, trimmedPath.getFileName(),
                            StandardCopyOption.REPLACE_EXISTING);
                }
            }
            archive.closeEntry();
        }
    }

    /**
     * Help the user understand the command line options.
     *
     * @param options all known options.
     */
    private static void printHelp(Options options) {
        new HelpFormatter().printHelp(GetGithubRelease.class.getCanonicalName(), options);
    }
}
