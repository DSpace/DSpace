/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.curate;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;

/**
 * Save a curation report to a unique file in the reports directory.
 * Reports are named by the date and time of day, for example:
 * "curation-20180916T113903045.report".
 *
 * @author mhwood
 */
public class FileReporter
        implements Reporter {
    private final Writer writer;

    /**
     * Open a writer to a file in a directory named by the configuration
     * property {@code report.dir}, or in {@code [DSpace]/reports} if not
     * configured.
     *
     * @throws IOException if there is a problem with the file path.
     */
    public FileReporter()
            throws IOException {
        // Calculate a unique(?) file name.
        Date now = GregorianCalendar.getInstance().getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'hhmmssSSS");
        String filename = String.format("curation-%s.report", sdf.format(now));

        // Build a path to the directory which is to receive the file.
        ConfigurationService cfg = new DSpace().getConfigurationService();
        String reportDir = cfg.getProperty("report.dir");
        Path reportPath;
        if (null == reportDir) {
            reportPath = Paths.get(cfg.getProperty("dspace.dir"),
                    "reports",
                    filename);
        } else {
            reportPath = Paths.get(reportDir, filename);
        }

        // Open the file.
        writer = new FileWriter(reportPath.toFile());
    }

    @Override
    public Appendable append(CharSequence cs)
            throws IOException {
        writer.append(cs);
        return this;
    }

    @Override
    public Appendable append(CharSequence cs, int i, int i1)
            throws IOException {
        writer.append(cs, i, i1);
        return this;
    }

    @Override
    public Appendable append(char c) throws IOException {
        writer.append(c);
        return this;
    }

    @Override
    public void close() throws Exception {
        writer.close();
    }
}
