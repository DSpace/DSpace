/*
 * ReportGenerator.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package org.dspace.app.statistics;

import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.uri.ExternalIdentifier;
import org.dspace.uri.ExternalIdentifierService;
import org.dspace.uri.ObjectIdentifier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class performs the action of coordinating a usage report being
 * generated using the standard internal aggregation file format as a basis.
 * All it's configuration information must come from that file.  There is the
 * opportunity for different output format options such as HTML.
 *
 * Use the -help flag for more information
 *
 * @author  Richard Jones
 */
public class ReportGenerator 
{
    // set up our class globals
    
    /////////////////
    // aggregators
    /////////////////
    
    /** aggregator for all actions performed in the system */
    private static Map actionAggregator = new HashMap();
    
    /** aggregator for all searches performed */
    private static Map searchAggregator = new HashMap();
    
    /** aggregator for user logins */
    private static Map userAggregator = new HashMap();
    
    /** aggregator for item views */
    private static Map itemAggregator = new HashMap();
    
    /** aggregator for current archive state statistics */
    private static Map archiveStats = new HashMap();
    
    
    //////////////////
    // statistics config data
    //////////////////
    
    /** bottom limit to output for search word analysis */
    private static int searchFloor;
    
    /** bottom limit to output for item view analysis */
    private static int itemFloor;
    
    /** number of items from most popular to be looked up in the database */
    private static int itemLookup;
    
    /** mode to use for user email display */
    private static String userEmail;
    
    /** URL of the service being analysed */
    private static String url;
    
    /** Name of the service being analysed */
    private static String name;

    /** average number of views per item */
    private static int avgItemViews;
    
    /** name of the server being analysed */
    private static String serverName;
    
    /** start date of this report */
    private static Date startDate = null;
    
    /** end date of this report */
    private static Date endDate = null;
    
    /** the time taken to build the aggregation file from the log */
    private static int processTime;
  
    /** the number of log lines analysed */
    private static int logLines;
    
    /** the number of warnings encountered */
    private static int warnings;
    
    /** the list of results to be displayed in the general summary */
    private static List generalSummary = new ArrayList();
    
    //////////////////
    // regular expressions
    //////////////////
    
    /** pattern that matches an unqualified aggregator property */
    private static Pattern real = Pattern.compile("^(.+)=(.+)");
    
    //////////////////////////
   // Miscellaneous variables
   //////////////////////////
   
   /** process timing clock */
   private static Calendar startTime = null;
   
   /** a map from log file action to human readable action */
   private static Map actionMap = new HashMap();
   
    /////////////////
    // report generator config data
    ////////////////
    
    /** the format of the report to be output */
    private static String format = null;
    
    /** the input file to build the report from */
    private static String input = null;
    
    /** the output file to which to write aggregation data */
   private static String output = ConfigurationManager.getProperty("dspace.dir") + 
                            File.separator + "log" + File.separator + "report";
   
   /** the log file action to human readable action map */
   private static String map = ConfigurationManager.getProperty("dspace.dir") +
                            File.separator + "config" + File.separator + "dstat.map";
   
   
    /**
     * main method to be run from command line.  See usage information for
     * details as to how to use the command line flags
     */
    public static void main(String [] argv) throws Exception
    {
        // create context as super user
        Context context = new Context();
        context.setIgnoreAuthorization(true);
        
        String myFormat = null;
        String myInput = null;
        String myOutput = null;
        String myMap = null;
        
        // read in our command line options
        for (int i = 0; i < argv.length; i++)
        {
            if (argv[i].equals("-format"))
            {
                myFormat = argv[i+1].toLowerCase();
            }
            
            if (argv[i].equals("-in"))
            {
                myInput = argv[i+1];
            }
            
            if (argv[i].equals("-out"))
            {
                myOutput = argv[i+1];
            }
            
            if (argv[i].equals("-map"))
            {
                myMap = argv[i+1];
            }
            
            if (argv[i].equals("-help"))
            {
                usage();
                System.exit(0);
            }
        }
        
        processReport(context, myFormat, myInput, myOutput, myMap);
    }
    
    /**
     * using the pre-configuration information passed here, read in the 
     * aggregation data and output a file containing the report in the
     * requested format
     *
     * @param   context     the DSpace context in which this action is performed
     * @param   myFormat    the desired output format (currently on HTML supported)
     * @param   myInput     the aggregation file to be turned into a report
     * @param   myOutput    the file into which to write the report
     */
    public static void processReport(Context context, String myFormat, 
                                     String myInput, String myOutput,
                                     String myMap)
        throws Exception
    {
        startTime = new GregorianCalendar();
        
        // set the parameters for this analysis
        setParameters(myFormat, myInput, myOutput, myMap);
        
        // pre prepare our standard file readers and buffered readers
        FileReader fr = null;
        BufferedReader br = null;
        
        // read the input file
        readInput(input);
        
        // load the log file action to human readable action map
        readMap(map);
        
        // create the relevant report type
        // FIXME: at the moment we only support HTML report generation
        Report report = null;
        if (format.equals("html"))
        {
            report = new HTMLReport();
        }
        
        report.setStartDate(startDate);
        report.setEndDate(endDate);
        report.setMainTitle(name, serverName);
        
        // define our standard variables for re-use
        // FIXME: we probably don't need these once we've finished re-factoring
        Iterator keys = null;
        int i = 0;
        String explanation = null;
        int value;
        
        // FIXME: All of these sections should probably be buried in their own
        // custom methods
        
        Statistics overview = new Statistics();
        
        overview.setSectionHeader("General Overview");
        
        Iterator summaryEntries = generalSummary.iterator();
        while (summaryEntries.hasNext())
        {
            String entry = (String) summaryEntries.next();
            if (actionAggregator.containsKey(entry))
            {
                int count = Integer.parseInt((String) actionAggregator.get(entry));
                overview.add(new Stat(translate(entry), count));
            }
        }
        
        report.addBlock(overview);
        
        // prepare the archive statistics package
        if (archiveStats.size() > 0)
        {
            Statistics archiveInfo = prepareStats(archiveStats, true, false);
            archiveInfo.setSectionHeader("Archive Information");
            archiveInfo.setStatName("Content Type");
            archiveInfo.setResultName("Number of items");
        
            report.addBlock(archiveInfo);
        }
        
        
        
        // process the items in preparation to be displayed.  This includes sorting
        // by view number, building the links, and getting further info where
        // necessary
        Statistics viewedItems = new Statistics("Item/URI", "Number of views", itemFloor);
        viewedItems.setSectionHeader("Items Viewed");
        
        Stat[] items = new Stat[itemAggregator.size()];
        
        keys = itemAggregator.keySet().iterator();
        i = 0;
        while (keys.hasNext())
        {
            String key = (String) keys.next();
            String link = url + "uri/" + key;
            value = Integer.parseInt((String) itemAggregator.get(key));
            items[i] = new Stat(key, value, link);
            i++;
        }
        
        Arrays.sort(items);
        
        String info = null;
        for (i = 0; i < items.length; i++)
        {
            if (i < itemLookup)
            {
                info = getItemInfo(context, items[i].getKey());
            }
               
            // if we get something back from the db then set it as the key,
            // else just use the link
            if (info != null)
            {
                items[i].setKey(info  + " (" + items[i].getKey() + ")");
            }
            else
            {
                items[i].setKey(items[i].getReference());
            }
            
            // reset the info register
            info = null;
        }
        
        viewedItems.add(items);
        
        report.addBlock(viewedItems);
        
        // prepare a report of the full action statistics
        Statistics fullInfo = prepareStats(actionAggregator, true, true);
        fullInfo.setSectionHeader("All Actions Performed");
        fullInfo.setStatName("Action");
        fullInfo.setResultName("Number of times");
        
        report.addBlock(fullInfo);
        
        // prepare the user login statistics package
        if (!userEmail.equals("off"))
        {
            Statistics userLogins = prepareStats(userAggregator, true, false);
            userLogins.setSectionHeader("User Logins");
            userLogins.setStatName("User");
            userLogins.setResultName("Number of logins");
            if (userEmail.equals("alias"))
            {
                explanation = "(distinct addresses)";
                userLogins.setExplanation(explanation);
            }
        
            report.addBlock(userLogins);
        }

        // prepare the search word statistics package
        Statistics searchWords = prepareStats(searchAggregator, true, false);
        searchWords.setSectionHeader("Words Searched");
        searchWords.setStatName("Word");
        searchWords.setResultName("Number of searches");
        searchWords.setFloor(searchFloor);
        
        report.addBlock(searchWords);
        
        // FIXME: because this isn't an aggregator it can't be passed to
        // prepareStats; should we overload this method for use with this kind
        // of data?
        // prepare the average item views statistics
        if (avgItemViews > 0)
        {
            Statistics avg = new Statistics();
            avg.setSectionHeader("Averaging Information");

            Stat[] average = new Stat[1];
        
            average[0] = new Stat("Average views per item", avgItemViews);
            avg.add(average);
            report.addBlock(avg);
        }
      
        
        // prepare the log line level statistics
        // FIXME: at the moment we only know about warnings, but future versions
        // should aggregate all log line levels and display here
        Statistics levels = new Statistics("Level", "Number of lines");
        levels.setSectionHeader("Log Level Information");
        
        Stat[] level = new Stat[1];
        level[0] = new Stat("Warnings", warnings);
        
        levels.add(level);
        
        report.addBlock(levels);
        
        // get the display processing time information
        Calendar endTime = new GregorianCalendar();
        long timeInMillis = (endTime.getTimeInMillis() - startTime.getTimeInMillis());
        int outputProcessTime = (new Long(timeInMillis).intValue() / 1000);
        
        // prepare the processing information statistics
        Statistics process = new Statistics("Operation", "");
        process.setSectionHeader("Processing Information");
        
        Stat[] proc = new Stat[3];
        proc[0] = new Stat("Log Processing Time", processTime);
        proc[0].setUnits("seconds");
        proc[1] = new Stat("Output Processing Time", outputProcessTime);
        proc[1].setUnits("seconds");
        proc[2] = new Stat("Log File Lines Analysed", logLines);
        proc[2].setUnits("lines");
        
        process.add(proc);
        
        report.addBlock(process);
        
        // finally write the string into the output file
        try 
        {
        	FileOutputStream fos = new FileOutputStream(output);
            OutputStreamWriter osr = new OutputStreamWriter(fos, "UTF-8");
            PrintWriter out = new PrintWriter(osr);
            out.write(report.render());
            out.close();
        } 
        catch (IOException e) 
        {
            System.out.println("Unable to write to output file " + output);
            System.exit(0);
        }
        
        return;
    }
    
    
    /**
     * a standard stats block preparation method for use when an aggregator
     * has to be put out in its entirity.  This method will not be able to
     * deal with complex cases, although it will perform sorting by value and
     * translations as per the map file if requested
     *
     * @param   aggregator      the aggregator that should be converted
     * @param   sort            should the resulting stats be sorted by value
     * @param   translate       translate the stat name using the map file
     *
     * @return      a Statistics object containing all the relevant information
     */
    public static Statistics prepareStats(Map aggregator, boolean sort, boolean translate)
    {
        Stat[] stats = new Stat[aggregator.size()];
        if (aggregator.size() > 0)
        {
            Iterator keys = aggregator.keySet().iterator();
            int i = 0;
            while (keys.hasNext())
            {
                String key = (String) keys.next();
                int value = Integer.parseInt((String) aggregator.get(key));
                if (translate)
                {
                    stats[i] = new Stat(translate(key), value);
                }
                else
                {
                    stats[i] = new Stat(key, value);
                }
                i++;
            }
            
            if (sort)
            {
                Arrays.sort(stats);
            }
        }
        
        // add the results to the statistics object
        Statistics statistics = new Statistics();
        statistics.add(stats);
        
        return statistics;
    }
    
    
    /**
     * look the given text up in the action map table and return a translated
     * value if one exists.  If no translation exists the original text is
     * returned
     *
     * @param   text    the text to be translated
     *
     * @return      a string containing either the translated text or the original
     *              text
     */
    public static String translate(String text)
    {
        if (actionMap.containsKey(text))
        {
            return (String) actionMap.get(text);
        }
        else
        {
            return text;
        }
    }
    
    
    /**
     * read in the action map file which converts log file line actions into
     * actions which are more understandable to humans
     *
     * @param   map     the map file
     */
    public static void readMap(String map)
        throws IOException
    {
        FileReader fr = null;
        BufferedReader br = null;
        
        // read in the map file, printing a warning if none is found
        String record = null;
        try 
        {  
            fr = new FileReader(map);
            br = new BufferedReader(fr);
        } 
        catch (IOException e) 
        {  
            System.err.println("Failed to read map file: log file actions will be displayed without translation");
            return;
        } 
        
        // loop through the map file and read in the values
        while ((record = br.readLine()) != null) 
        {
            Matcher matchReal = real.matcher(record);
            
            // if the line is real then read it in
            if (matchReal.matches())
            {
                actionMap.put(matchReal.group(1).trim(), matchReal.group(2).trim());
            }
        }
    }
    
    
    /**
     * set the passed parameters up as global class variables.  This has to
     * be done in a separate method because the API permits for running from
     * the command line with args or calling the processReport method statically
     * from elsewhere
     *
     * @param   myFormat    the log file directory to be analysed
     * @param   myInput     regex for log file names
     * @param   myOutput    config file to use for dstat
     * @param   myMap       the action map file to use for translations
     */
    public static void setParameters(String myFormat, String myInput, 
                                    String myOutput, String myMap)
    {
        if (myFormat != null)
        {
            format = myFormat;
        }
        
        if (myInput != null)
        {
            input = myInput;
        }
        
        if (myOutput != null)
        {
            output = myOutput;
        }
        
        if (myMap != null)
        {
            map = myMap;
        }
        
        return;
    }
    
    
    /**
     * read the input file and populate all the class globals with the contents
     * The values that come from this file form the basis of the analysis report
     *
     * @param   input   the aggregator file
     */
    public static void readInput(String input)
        throws IOException, ParseException
    {
        FileReader fr = null;
        BufferedReader br = null;
        
        // read in the analysis information, throwing an error if we fail to open
        // the given file
        String record = null;
        try 
        {  
            fr = new FileReader(input);
            br = new BufferedReader(fr);
        } 
        catch (IOException e) 
        {  
            System.out.println("Failed to read input file");
            System.exit(0);
        } 
        
        // FIXME: although this works, it is not very elegant
        // loop through the aggregator file and read in the values
        while ((record = br.readLine()) != null) 
        {
            // match real lines
            Matcher matchReal = real.matcher(record);
            
            // pre-prepare our input strings
            String section = null;
            String key = null;
            String value = null;
            
            // temporary string to hold the left hand side of the equation
            String left = null;
            
            // match the line or skip this record
            if (matchReal.matches())
            {
                // lift the values out of the matcher's result groups
                left = matchReal.group(1).trim();
                value = matchReal.group(2).trim();
                
                // now analyse the left hand side, splitting by ".", taking the
                // first token as the section and the remainder of the string
                // as they key if it exists
                StringTokenizer tokens = new StringTokenizer(left, ".");
                int numTokens = tokens.countTokens();
                if (tokens.hasMoreTokens())
                {
                    section = tokens.nextToken();
                    if (numTokens > 1)
                    {
                        key = left.substring(section.length() + 1);
                    }
                    else
                    {
                        key = "";
                    }
                }
            }
            else
            {
                continue;
            }
            
            // if the line is real, then we carry on
            
            // first initialise a date format object to do our date processing
            // if necessary
            SimpleDateFormat sdf = new SimpleDateFormat("dd'/'MM'/'yyyy");
            
            // read the analysis contents in
            if (section.equals("archive"))
            {
                archiveStats.put(key, value);
            }
            
            if (section.equals("action"))
            {
                actionAggregator.put(key, value);
            }
            
            if (section.equals("user"))
            {
                userAggregator.put(key, value);
            }
            
            if (section.equals("search"))
            {
                searchAggregator.put(key, value);
            }
            
            if (section.equals("item"))
            {
                itemAggregator.put(key, value);
            }
            
            // read the config details used to make this report in
            if (section.equals("user_email"))
            {
                userEmail = value;
            }
            
            if (section.equals("item_floor"))
            {
                itemFloor = Integer.parseInt(value);
            }
            
            if (section.equals("search_floor"))
            {
                searchFloor = Integer.parseInt(value);
            }
            
            if (section.equals("host_url"))
            {
                url = value;
            }
            
            if (section.equals("item_lookup"))
            {
                itemLookup = Integer.parseInt(value);
            }
            
            if (section.equals("avg_item_views"))
            {
                try 
                {
                    avgItemViews = Integer.parseInt(value);
                }
                catch (NumberFormatException e)
                {
                    avgItemViews = 0;
                }
            }
            
            if (section.equals("server_name"))
            {
                serverName = value;
            }
            
            if (section.equals("service_name"))
            {
                name = value;
            }
             
            if (section.equals("start_date"))
            {
                startDate = sdf.parse(value);
            }
            
            if (section.equals("end_date"))
            {
                endDate = sdf.parse(value);
            }
            
            if (section.equals("analysis_process_time"))
            {
                processTime = Integer.parseInt(value);
            }
            
            if (section.equals("general_summary"))
            {
                generalSummary.add(value);
            }
            
            if (section.equals("log_lines"))
            {
                logLines = Integer.parseInt(value);
            }
            
            if (section.equals("warnings"))
            {
                warnings = Integer.parseInt(value);
            }
        }

        // close the inputs
        br.close();
        fr.close();
    }
    
    /**
     * get the information for the item with the given URI
     *
     * @param   context     the DSpace context we are operating under
     * @param   uri         the uri of the item being looked up, in the form
     *                      xyz:1234/567 and so forth
     *
     * @return      a string containing a reference (almost citation) to the
     *              article
     */
    public static String getItemInfo(Context context, String uri)
    {
        Item item = null;
        
        // ensure that the URI exists
        try 
        {
            ExternalIdentifier identifier = ExternalIdentifierService.parseCanonicalForm(context, uri);
            //ExternalIdentifierDAO identifierDAO = ExternalIdentifierDAOFactory.getInstance(context);
            //ExternalIdentifier identifier = identifierDAO.retrieve(uri);
            ObjectIdentifier oi = identifier.getObjectIdentifier();
            item = (Item) oi.getObject(context);
        } 
        catch (Exception e)
        {
            return null;
        }
        
        // if no URI that matches is found then also return null
        if (item == null)
        {
            return null;
        }
        
        // build the referece
        // FIXME: here we have blurred the line between content and presentation
        // and it should probably be un-blurred
        DCValue[] title = item.getDC("title", null, Item.ANY);
        DCValue[] author = item.getDC("contributor", "author", Item.ANY);
        
        StringBuffer authors = new StringBuffer();
        if (author.length > 0)
        {
            authors.append("(" + author[0].value);
        }
        if (author.length > 1)
        {
            authors.append(" et al");
        }
        if (author.length > 0)
        {
           authors.append(")");
        }
        
        String content = title[0].value + " " + authors.toString();
        
        return content;
    }
    
    
    /**
     * output the usage information to the terminal
     */
    public static void usage()
    {
        String usage = "Usage Information:\n" +
                        "ReportGenerator [options [parameters]]\n" +
                        "-format [output format]\n" +
                            "\tRequired\n" +
                            "\tSpecify the format that you would like the output in\n" +
                            "\tOptions:\n" +
                            "\t\thtml\n" +
                        "-in [aggregation file]\n" +
                            "\tRequired\n" +
                            "\tSpecify the aggregation data file to display\n" +
                        "-out [output file]\n" +
                            "\tOptional\n" +
                            "\tSpecify the file to output the report to\n" +
                            "\tDefault uses [dspace log directory]/report\n" +
                        "-map [map file]\n" +
                            "\tOptional\n" +
                            "\tSpecify the map file to translate log file actions into human readable actions\n" +
                            "\tDefault uses [dspace config directory]/dstat.map\n" +
                        "-help\n" +
                            "\tdisplay this usage information\n";
        
        System.out.println(usage);
    }
}
