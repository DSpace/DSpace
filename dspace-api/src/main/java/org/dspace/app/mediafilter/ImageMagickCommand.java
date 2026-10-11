/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mediafilter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * A single invocation of an ImageMagick command line tool ({@code convert} or {@code identify}).
 * <p>
 * Arguments are passed to the process as a list, never through a shell, so file names
 * and option values do not need any quoting.
 */
final class ImageMagickCommand {

    /**
     * File name extensions tried, in order, when looking for an executable in the search path
     * (the Windows ones are harmless elsewhere).
     */
    private static final String[] EXECUTABLE_EXTENSIONS = {"", ".exe", ".cmd", ".bat"};

    /**
     * Directories to look for the ImageMagick executables in, separated by the platform's
     * path separator. When blank or when the executable is not found there, the operating
     * system's PATH is used.
     */
    private static volatile String searchPath;

    private final String tool;
    private final List<String> arguments = new ArrayList<>();

    private ImageMagickCommand(String tool) {
        this.tool = tool;
    }

    /**
     * @return a new {@code convert} command without arguments
     */
    static ImageMagickCommand convert() {
        return new ImageMagickCommand("convert");
    }

    /**
     * @return a new {@code identify} command without arguments
     */
    static ImageMagickCommand identify() {
        return new ImageMagickCommand("identify");
    }

    /**
     * Set the directories to look for the ImageMagick executables in.
     *
     * @param path directories separated by the platform's path separator, or null to use the PATH
     */
    static void setSearchPath(String path) {
        searchPath = path;
    }

    /**
     * Append arguments (options, option values or image file names) to the command.
     *
     * @param args the arguments to append, in order
     * @return this command
     */
    ImageMagickCommand add(String... args) {
        arguments.addAll(List.of(args));
        return this;
    }

    /**
     * Run the command and wait for it to finish.
     *
     * @return what the command wrote to its standard output
     * @throws IOException          if the command cannot be started or exits with an error
     * @throws InterruptedException if interrupted while waiting for the command
     */
    String run() throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(arguments.size() + 1);
        command.add(resolveExecutable());
        command.addAll(arguments);

        // Standard error goes to a file, so a chatty tool cannot block on a full pipe
        // while we read standard output.
        Path errors = Files.createTempFile("imagemagick", ".err");
        try {
            Process process = new ProcessBuilder(command)
                .redirectError(errors.toFile())
                .start();
            String output;
            try (InputStream stdout = process.getInputStream()) {
                output = new String(stdout.readAllBytes(), Charset.defaultCharset());
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException(tool + " failed with exit code " + exitCode + ": "
                                          + Files.readString(errors, Charset.defaultCharset()).trim());
            }
            return output;
        } finally {
            Files.deleteIfExists(errors);
        }
    }

    /**
     * Find the executable in the configured search path, falling back to the bare tool name.
     */
    private String resolveExecutable() {
        String path = searchPath;
        if (StringUtils.isNotBlank(path)) {
            for (String directory : path.split(File.pathSeparator)) {
                for (String extension : EXECUTABLE_EXTENSIONS) {
                    File candidate = new File(directory.trim(), tool + extension);
                    if (candidate.exists()) {
                        return candidate.getAbsolutePath();
                    }
                }
            }
        }
        return tool;
    }

    /**
     * @return the arguments, separated by spaces, for logging
     */
    @Override
    public String toString() {
        return String.join(" ", arguments);
    }
}
