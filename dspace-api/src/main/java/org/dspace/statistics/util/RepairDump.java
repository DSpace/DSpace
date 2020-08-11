/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.util;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;

/**
 * Repair various problems in a statistics core export CSV.
 * @author Mark H. Wood <mwood@iupui.edu>
 */
public class RepairDump {
    private RepairDump() {}

    /**
     * Repair known classes of problems with exported statistics.
     * Reads standard input, writes repaired CSV to standard output.
     * @param args
     */
    public static void main( String[] args ) {
        long recordCount = 0; // Input record counter.
        long repairCount = 0; // Repaired records counter.

        boolean verbose; // Give more information about what's happening.

        // Analyze the command line.
        Options options = new Options();
        options.addOption("h", "help", false, "Give help on options.");
        options.addOption("v", "verbose", false, "Write extra information to standard error.");

        CommandLine command = null;
        try {
            command = new DefaultParser().parse(options, args);
        } catch (ParseException ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
        }

        if (command.hasOption("h")) {
            giveHelp(options);
            System.exit(0);
        }

        verbose = command.hasOption("v");

        // Copy standard in to standard out, fixing problems.
        try (
                Reader input = new InputStreamReader(System.in, StandardCharsets.UTF_8);
                CSVReader csvReader = new CSVReader(input);
                Writer output = new OutputStreamWriter(System.out, StandardCharsets.UTF_8);
                CSVWriter csvWriter = new CSVWriter(output);
                ) {
            // Read the column headers.
            String[] fields = csvReader.readNext();

            // Which column is "uid"?
            int uidIndex = -1;
            for (int i = 0; i < fields.length; i++) {
                if (fields[i].equals("uid")) {
                    uidIndex = i;
                    break;
                }
            }
            if (uidIndex < 0) {
                System.err.println("Error:  input contains no 'uid' column.");
                System.exit(1);
            }

            // Copy the headers to output.
            csvWriter.writeNext(fields);

            // Copy records to output, repairing any problems that we find.
            while (null != (fields = csvReader.readNext())) {
                recordCount++;

                // Set 'uid' to a new random UUID if empty.
                if (fields.length < uidIndex + 1) { // Too short to have 'uid'
                    fields = Arrays.copyOf(fields, uidIndex);
                }
                if (StringUtils.isBlank(fields[uidIndex])) { // 'uid' field is empty.
                    if (verbose) {
                        System.err.format("Missing 'uid' at record %d%n", recordCount);
                    }
                    fields[uidIndex] = UUID.randomUUID().toString();
                    repairCount++;
                }

                // Write repaired record.
                csvWriter.writeNext(fields);
            }
        } catch (IOException ex) {
            System.err.format("Could not read the export at record %d:  ", recordCount);
            System.err.println(ex.getMessage());
        } finally {
            System.err.format("Repaired %d out of %d records.%n",
                    repairCount, recordCount);
        }
    }

    private static void giveHelp(Options options) {
        String className = MethodHandles.lookup().lookupClass().getCanonicalName();
        new HelpFormatter().printHelp(className + " [options]", options);
    }
}
