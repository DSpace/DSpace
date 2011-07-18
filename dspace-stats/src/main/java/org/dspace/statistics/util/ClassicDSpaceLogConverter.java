/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.util;

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.dspace.app.statistics.LogAnalyser;
import org.dspace.app.statistics.LogLine;
import org.dspace.content.*;
import org.dspace.handle.HandleManager;
import org.dspace.core.Context;

import java.io.*;
import java.sql.SQLException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.text.SimpleDateFormat;
import java.text.ParsePosition;

/**
 * A utility class to convert the classic dspace.log (as generated
 * by log4j) files into an intermediate format for ingestion into
 * the new solr stats.
 *
 * @author Stuart Lewis
 */
public class ClassicDSpaceLogConverter {
    private Logger log = Logger.getLogger(ClassicDSpaceLogConverter.class);

    /** A DSpace context */
    private Context context;

    /** Whether or not to provide verbose output */
    private boolean verbose = false;

    /** Whether to include actions logged by org.dspace.usage.LoggerUsageEventListener */
    private boolean newEvents = false;

    /** A regular expression for extracting the IP address from a log line */
    private Pattern ipaddrPattern = Pattern.compile("ip_addr=(\\d*\\.\\d*\\.\\d*\\.\\d*):");

    /** Date format (in) from the log line */
    private SimpleDateFormat dateFormatIn = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /** Date format out (for solr) */
    private SimpleDateFormat dateFormatOut = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    /** Date format (in) from the log line for the UID */
    private SimpleDateFormat dateFormatInUID = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");

    /** Date format out (for uid) */
    private SimpleDateFormat dateFormatOutUID = new SimpleDateFormat("yyyyMMddHHmmssSSS");


    /**
     * Create an instance of the converter utility
     *
     * @param c The context
     * @param v Whether or not to provide verbose output
     * @param nE Whether to include actions logged by org.dspace.usage.LoggerUsageEventListener
     */
    public ClassicDSpaceLogConverter(Context c, boolean v, boolean nE)
    {
        // Set up some variables
        context = c;
        verbose = v;
        newEvents = nE;
    }

    /**
     * Convert a classic log file
     *
     * @param in The filename to read from
     * @param out The filename to write to
     * @return The number of lines processed
     */
    public int convert(String in, String out)
    {
        // Line counter
        int counter = 0;
        int lines = 0;

        // Say what we're going to do
        System.out.println(" About to convert '" + in + "' to '" + out + "'");

        // Setup the regular expressions for the log file
        LogAnalyser.setRegex(in);

        // Open the file and read it line by line
        BufferedReader input = null;
        Writer output = null;

        try {
            String line;
            LogLine lline;
            String lout;
            String id;
            String handle;
            String ip;
            String date;
            DSpaceObject dso;
            String uid;
            String lastLine = "";

            input =  new BufferedReader(new FileReader(new File(in)));
            output = new BufferedWriter(new FileWriter(new File(out)));

            while ((line = input.readLine()) != null)
            {
                // Read inthe line and covnert it to a LogLine
                lines++;
                if (verbose)
                {
                    System.out.println("  - IN: " + line);
                }
                lline = LogAnalyser.getLogLine(line);

                // Get rid of any lines that aren't INFO
                if ((lline == null) || (!lline.isLevel("INFO")))
                {
                    if (verbose)
                    {
                        System.out.println("   - IGNORED!");
                    }
                    continue;
                }

                // Get the IP address of the user
                Matcher matcher = ipaddrPattern.matcher(line);
                if (matcher.find())
                {
                    ip = matcher.group(1);
                }
                else
                {
                    ip = "unknown";
                }

                // Get and format the date
                // We can use lline.getDate() as this strips the time element
                date = dateFormatOut.format(
                       dateFormatIn.parse(line.substring(0, line.indexOf(',')),
                                             new ParsePosition(0)));

                // Generate a UID for the log line
                // - based on the date/time
                uid = dateFormatOutUID.format(
                       dateFormatInUID.parse(line.substring(0, line.indexOf(' ', line.indexOf(' ') + 1)),
                                             new ParsePosition(0)));

                try
                {
                    // What sort of view is it?
                    // (ignore lines from org.dspace.usage.LoggerUsageEventListener which is 1.6 code)
                    if ((lline.getAction().equals("view_bitstream")) &&
                        (!lline.getParams().contains("invalid_bitstream_id")) &&
                        (!lline.getParams().contains("withdrawn")) &&
                        ((!line.contains("org.dspace.usage.LoggerUsageEventListener")) || newEvents))
                    {
                        id = lline.getParams().substring(13);
                    }
                    else if ((lline.getAction().equals("view_item")) &&
                            ((!line.contains("org.dspace.usage.LoggerUsageEventListener")) || newEvents))
                    {
                        handle = lline.getParams().substring(7);
                        dso = HandleManager.resolveToObject(context, handle);
                        id = "" + dso.getID();
                    }
                    else if ((lline.getAction().equals("view_collection")) &&
                             ((!line.contains("org.dspace.usage.LoggerUsageEventListener")) || newEvents))
                    {
                        id = lline.getParams().substring(14);
                    }
                    else if ((lline.getAction().equals("view_community")) &&
                             ((!line.contains("org.dspace.usage.LoggerUsageEventListener")) || newEvents))
                    {
                        id = lline.getParams().substring(13);
                    }
                    else
                    {
                        //if (verbose) System.out.println("   - IGNORED!");
                        continue;
                    }

                    // Construct the log line
                    lout = uid + "," +
                           lline.getAction() + "," +
                           id + "," +
                           date + "," +
                           lline.getUser() + "," +
                           ip + "\n";
                }
                catch (Exception e)
                {
                    if (verbose)
                    {
                        System.out.println("  - IN: " + line);
                    }
                    if (verbose)
                    {
                        System.err.println("Error with log line! " + e.getMessage());
                    }
                    continue;
                }

                if ((verbose) && (!"".equals(lout)))
                {
                    System.out.println("  - IN: " + line);
                    System.out.println("  - OUT: " + lout);
                }

                // Write the output line
                if ((!"".equals(lout)) && (!lout.equals(lastLine)))
                {
                    output.write(lout);
                    counter++;
                    lastLine = lout;
                }
            }
        }
        catch (IOException e)
        {
            log.error("File access problem", e);
        }
        finally
        {
            // Clean up the input and output streams
            try { input.close();  } catch (IOException e) { log.error(e.getMessage(), e); }
            try { output.flush(); } catch (IOException e) { log.error(e.getMessage(), e); }
            try { output.close(); } catch (IOException e) { log.error(e.getMessage(), e); }
        }

        // Tell the user what we have done
        System.out.println("  Read " + lines + " lines and recorded " + counter + " events");
        return counter;
    }

    /**
     * Print the help message
     *
     * @param options The command line options the user gave
     * @param exitCode the system exit code to use
     */
    private static void printHelp(Options options, int exitCode)
    {
        // print the help message
        HelpFormatter myhelp = new HelpFormatter();
        myhelp.printHelp("ClassicDSpaceLogConverter\n", options);
        System.out.println("\n\tClassicDSpaceLogConverter -i infilename -o outfilename -v (for verbose output)");
        System.exit(exitCode);
    }

    /**
     * Main method to execute the converter
     *
     * @param args CLI args
     */
    public static void main(String[] args)
    {
        CommandLineParser parser = new PosixParser();

        Options options = new Options();

        options.addOption("i", "in", true, "source file");
        options.addOption("o", "out", true, "destination directory");
        options.addOption("m", "multiple",false, "treat the input file as having a wildcard ending");
        options.addOption("n", "newformat",false, "process new format log lines (1.6+)");
        options.addOption("v", "verbose", false, "display verbose output (useful for debugging)");
        options.addOption("h", "help", false, "help");

        // Parse the command line arguments
        CommandLine line;
        try
        {
            line = parser.parse(options, args);
        }
        catch (ParseException pe)
        {
            System.err.println("Error parsing command line arguments: " + pe.getMessage());
            System.exit(1);
            return;
        }

        // Did the user ask to see the help?
        if (line.hasOption('h'))
        {
            printHelp(options, 0);
        }

        // Check we have an input and output file
        if ((!line.hasOption('i')) && (!line.hasOption('o')))
        {
            System.err.println("-i and -o input and output file names are required");
            printHelp(options, 1);
        }
        else if (!line.hasOption('i'))
        {
            System.err.println("-i input file name is required");
            printHelp(options, 1);
        }

        if (!line.hasOption('o'))
        {
            System.err.println("-o output file names is required");
            printHelp(options, 1);
        }

        // Whether or not to include event created by org.dspace.usage.LoggerUsageEventListener
        boolean newEvents = line.hasOption('n');

        // Create a copy of the converter
        Context context = null;
        try
        {
            context = new Context();
            context.turnOffAuthorisationSystem();
        }
        catch (SQLException sqle)
        {
            System.err.println("Unable to create DSpace context: " + sqle.getMessage());
            System.exit(1);
        }

        ClassicDSpaceLogConverter converter = new ClassicDSpaceLogConverter(context,
                                                                            line.hasOption('v'),
                                                                            newEvents);

        // Set up the log analyser
        try
        {
            LogAnalyser.readConfig();
        }
        catch (IOException ioe)
        {
            System.err.println("Unable to read config file: " + LogAnalyser.getConfigFile());
            System.exit(1);
        }

        // Are we converting multiple files?
        if (line.hasOption('m'))
        {
            // Convert all the files
            final File sample = new File(line.getOptionValue('i'));
            File dir = sample.getParentFile();
            FilenameFilter filter = new FilenameFilter()
            {
                public boolean accept(File dir, String name)
                {
                    return name.startsWith(sample.getName());
                }
            };
            String[] children = dir.list(filter);
            for (String in : children)
            {
                System.out.println(in);
                String out = line.getOptionValue('o') +
                             (dir.getAbsolutePath() +
                              System.getProperty("file.separator") + in).substring(line.getOptionValue('i').length());

                converter.convert(dir.getAbsolutePath() + System.getProperty("file.separator") + in, out);
            }
        }
        else
        {
            // Just convert the one file
            converter.convert(line.getOptionValue('i'), line.getOptionValue('o'));
        }

        // Clean everything up
        context.restoreAuthSystemState();
        context.abort();
    }
}
