/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 *
 * by lindat-dev team
 */
package cz.cuni.mff.ufal.health;


import cz.cuni.mff.ufal.checks.ImportantLogs;
import cz.cuni.mff.ufal.dspace.IOUtils;
import org.dspace.app.statistics.LogAnalyser;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.health.Check;
import org.dspace.health.ReportInfo;

import java.io.*;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;

public class LogsCheck extends Check {

    final static private int MAX_LINES = 10;

    @Override
    public String run( ReportInfo ri ) {
        StringBuilder sb = new StringBuilder();

        // get list of dates between from to till
        List<String> dates = new ArrayList<>();
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(ri.from());
        while (calendar.getTime().before(ri.till())) {
            Date resultado = calendar.getTime();
            dates.add(new SimpleDateFormat("yyyy-MM-dd").format(resultado));
            calendar.add(Calendar.DATE, 1);
        }

        // open the files
        String log_dir = ConfigurationManager.getProperty("log.dir");
        for (final String date_str : dates.toArray(new String[dates.size()])) {

            File dir = new File( log_dir );
            File[] files = dir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.contains(date_str);
                }
            });

            for (int i = 0; i < files.length; ++i) {
                String input_file = files[i].getName();
                System.err.println(String.format(
                    "    #%d. Parsing [%s] log file at [%s]", i,
                    input_file, new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss.SSS").format(new Date())));

                BufferedReader safe_reader = null;
                try {
                    if( files[i].exists() && 0 == files[i].length() ) {
                        continue;
                    }
                    safe_reader = new BufferedReader(
                        new InputStreamReader(
                            new FileInputStream(files[i]),
                            Charset.forName("UTF8")) );

                } catch (Exception e) {
                    error(e, String.format("Problematic file %s\n", input_file) );
                    continue;
                }

                // output warnings
                ImportantLogs logs = new ImportantLogs(safe_reader,
                    IOUtils.get_date_from_log_file(input_file));
                boolean problem_found = (logs._lines.size() != 0 || 0 < logs
                    .warnings().size());
                // output info
                if (problem_found) {
                    sb.append(String.format(
                        "File: [%s] Warnings/Errors: [%d/%d]\n\n",
                        input_file, logs.warnings().size(),
                        logs._lines.size()));
                    for (int j = 0; j < logs._lines.size(); ++j) {
                        if (j > MAX_LINES) {
                            sb.append(String
                                .format("****\n... truncated [%d] lines...\n************\n\n",
                                    logs._lines.size() - j));
                            break;
                        }
                        String l = logs._lines.get(j);
                        sb.append(String.format("\t%s\n", l));
                    }
                }
            }
        }
        return sb.toString();
    }
}
