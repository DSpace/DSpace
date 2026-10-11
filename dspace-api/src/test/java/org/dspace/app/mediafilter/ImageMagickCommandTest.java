/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mediafilter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link ImageMagickCommand}, using small shell scripts in place of the ImageMagick tools.
 */
@DisabledOnOs(OS.WINDOWS)
public class ImageMagickCommandTest {

    @TempDir
    Path toolDirectory;

    @AfterEach
    public void resetSearchPath() {
        ImageMagickCommand.setSearchPath(null);
    }

    @Test
    public void testRunPassesArgumentsUnchangedAndReturnsOutput() throws Exception {
        // Prints each argument on its own line, so quoting problems would show up
        writeTool("identify", "#!/bin/sh\nfor a in \"$@\"; do echo \"[$a]\"; done\n");
        ImageMagickCommand.setSearchPath(toolDirectory.toString());

        String output = ImageMagickCommand.identify()
            .add("-ping", "-format", "%r", "/tmp/a file with spaces & $chars.pdf[0]")
            .run();

        assertEquals("[-ping]\n[-format]\n[%r]\n[/tmp/a file with spaces & $chars.pdf[0]]\n", output);
    }

    @Test
    public void testRunFailureReportsExitCodeAndErrorOutput() throws Exception {
        writeTool("convert", "#!/bin/sh\necho 'convert: no images defined' >&2\nexit 1\n");
        ImageMagickCommand.setSearchPath(toolDirectory.toString());

        IOException e = assertThrows(IOException.class, () -> ImageMagickCommand.convert().add("in", "out").run());

        assertThat(e.getMessage(), containsString("exit code 1"));
        assertThat(e.getMessage(), containsString("convert: no images defined"));
    }

    @Test
    public void testToStringListsArguments() {
        assertEquals("-auto-orient in.pdf[0] -thumbnail 180x120 out.jpg",
                     ImageMagickCommand.convert()
                                       .add("-auto-orient")
                                       .add("in.pdf[0]")
                                       .add("-thumbnail", "180x120")
                                       .add("out.jpg")
                                       .toString());
    }

    private void writeTool(String name, String script) throws IOException {
        Path tool = toolDirectory.resolve(name);
        Files.writeString(tool, script);
        Files.setPosixFilePermissions(tool, PosixFilePermissions.fromString("rwxr-xr-x"));
    }
}
