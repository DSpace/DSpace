/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.statistics;

import java.io.File;
import java.io.FileInputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;

/**
 * This class allows the running of the DSpace statistic tools
 * 
 * Usage: {@code java CreateStatReport -r <statistic to run>}
 * Available: {@code <stat-initial> <stat-general> <stat-monthly> <stat-report-initial> 
 * 				<stat-report-general> <stat-report-monthly>}
 * 
 * @author Chris Yates
 *
 */

public class CreateStatReport {

	/**Current date and time*/
	private static Calendar calendar = null;
	
	/**Reporting start date and time*/
	private static Calendar reportStartDate = null;
	
	/**Path of log directory*/
	private static String outputLogDirectory = null;
	
	/**Path of reporting directory*/
	private static String outputReportDirectory = null;
	
	/**File suffix for log files*/
	private static String outputSuffix = ".dat";
	
	/**User context*/
	private static Context context;

    /** the config file from which to configure the analyser */
    private static String configFile = ConfigurationManager.getProperty("dspace.dir") +
                            File.separator + "config" + File.separator +
                            "dstat.cfg";

    /*
	 * Main method to be run from the command line executes individual statistic methods
	 * 
	 * Usage: java CreateStatReport -r <statistic to run> 
	 */
	public static void main(String[] argv) throws Exception {

        // Open the statistics config file
        FileInputStream fis = new java.io.FileInputStream(new File(configFile));
        Properties config = new Properties();
        config.load(fis);
        int startMonth = 0;
        int startYear = 2005;
        try
        {
            startYear = Integer.parseInt(config.getProperty("start.year", "1").trim());
        } catch (NumberFormatException nfe)
        {
            System.err.println("start.year is incorrectly set in dstat.cfg. Must be a number (e.g. 2005).");
            System.exit(0);
        }
        try
        {
            startMonth = Integer.parseInt(config.getProperty("start.month", "2005").trim());
        } catch (NumberFormatException nfe)
        {
            System.err.println("start.month is incorrectly set in dstat.cfg. Must be a number between 1 and 12.");
            System.exit(0);
        }
        reportStartDate = new GregorianCalendar(startYear, startMonth - 1, 1);
        calendar = new GregorianCalendar();
        
        // create context as super user
        context = new Context();
        context.turnOffAuthorisationSystem();
        
        //get paths to directories
        outputLogDirectory = ConfigurationManager.getProperty("log.report.dir") + File.separator;
        outputReportDirectory = ConfigurationManager.getProperty("report.dir") + File.separator;
        
        //read in command line variable to determine which statistic to run
		CommandLineParser parser = new PosixParser();
		Options options = new Options();
		options.addOption("r", "report", true, "report");
		CommandLine line = parser.parse(options, argv);
		
		String statAction = null;
		
		if(line.hasOption('r')) 
		{
			statAction = line.getOptionValue('r');
		}
		
		if (statAction == null) {
			usage();
			System.exit(0);
		}

		//call appropriate statistics method
		if(statAction.equals("stat-monthly")) {
			statMonthly();
		}
		
		if(statAction.equals("stat-general")) {
			statGeneral();
		}
		
		if(statAction.equals("stat-initial")) {
			statInitial();
		}
		
		if(statAction.equals("stat-report-general")) {
			statReportGeneral();
		}
		
		if(statAction.equals("stat-report-initial")) {
			statReportInitial();
		}
		
		if(statAction.equals("stat-report-monthly")) {
			statReportMonthly();
		}
	}
	
	/**
	 * This method generates a report from the first of the current month to the end of the current month.
	 * 
	 * @throws Exception if error
	 */
	private static void statMonthly() throws Exception {
        
        //Output Prefix
        String outputPrefix = "dspace-log-monthly-";
		
		// set up our command line variables
        String myLogDir = null;
        String myFileTemplate = null;
        String myConfigFile = null;
        StringBuffer myOutFile = null;
        Date myStartDate = null;
        Date myEndDate = null;
        boolean myLookUp = false;       
     
        Calendar start = new GregorianCalendar( calendar.get(Calendar.YEAR),
        										calendar.get(Calendar.MONTH),
        										calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
        myStartDate = start.getTime();
        
        Calendar end = new GregorianCalendar( calendar.get(Calendar.YEAR),
											  calendar.get(Calendar.MONTH),
											  calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        myEndDate = end.getTime();
        
        myOutFile = new StringBuffer(outputLogDirectory);
        myOutFile.append(outputPrefix);
        myOutFile.append(calendar.get(Calendar.YEAR));
        myOutFile.append("-");
        myOutFile.append(calendar.get(Calendar.MONTH)+1);
        myOutFile.append(outputSuffix);        
                             
        LogAnalyser.processLogs(context, myLogDir, myFileTemplate, myConfigFile, myOutFile.toString(), myStartDate, myEndDate, myLookUp);
	}	
	
	/**
	 * This method generates a full report based on the full log period
	 * 
	 * @throws Exception if error
	 */
	private static void statGeneral() throws Exception {
		
		//Output Prefix
        String outputPrefix = "dspace-log-general-";
		
        // set up our command line variables
        String myLogDir = null;
        String myFileTemplate = null;
        String myConfigFile = null;
        StringBuffer myOutFile = null;
        Date myStartDate = null;
        Date myEndDate = null;
        boolean myLookUp = false; 
        
        myOutFile = new StringBuffer(outputLogDirectory);
        myOutFile.append(outputPrefix);
        myOutFile.append(calendar.get(Calendar.YEAR));
        myOutFile.append("-");
        myOutFile.append(calendar.get(Calendar.MONTH)+1);
        myOutFile.append("-");
        myOutFile.append(calendar.get(Calendar.DAY_OF_MONTH));
        myOutFile.append(outputSuffix); 
		
        LogAnalyser.processLogs(context, myLogDir, myFileTemplate, myConfigFile, myOutFile.toString(), myStartDate, myEndDate, myLookUp);
	}
	
	/**
	 * This script starts from the year and month specified below and loops each month until the current month
	 * generating a monthly aggregation files for the DStat system.
	 * 
	 * @throws Exception if error
	 */
	private static void statInitial() throws Exception {
				
		//Output Prefix
        String outputPrefix = "dspace-log-monthly-";
		
        // set up our command line variables
        String myLogDir = null;
        String myFileTemplate = null;
        String myConfigFile = null;
        StringBuffer myOutFile = null;
        Date myStartDate = null;
        Date myEndDate = null;
        boolean myLookUp = false; 
				
		Calendar reportEndDate = new GregorianCalendar( calendar.get(Calendar.YEAR),
				  										calendar.get(Calendar.MONTH),
				  										calendar.getActualMaximum(Calendar.DAY_OF_MONTH));

        Calendar currentMonth = (Calendar)reportStartDate.clone();
		while(currentMonth.before(reportEndDate)) {
									
			Calendar start = new GregorianCalendar( currentMonth.get(Calendar.YEAR),
													currentMonth.get(Calendar.MONTH),
													currentMonth.getActualMinimum(Calendar.DAY_OF_MONTH));
			myStartDate = start.getTime();

			Calendar end = new GregorianCalendar( currentMonth.get(Calendar.YEAR),
												  currentMonth.get(Calendar.MONTH),
												  currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH));
			myEndDate = end.getTime();
			
			myOutFile = new StringBuffer(outputLogDirectory);
	        myOutFile.append(outputPrefix);
	        myOutFile.append(currentMonth.get(Calendar.YEAR));
	        myOutFile.append("-");
	        myOutFile.append(currentMonth.get(Calendar.MONTH)+1);
	        myOutFile.append(outputSuffix); 
	        	        
	        LogAnalyser.processLogs(context, myLogDir, myFileTemplate, myConfigFile, myOutFile.toString(), myStartDate, myEndDate, myLookUp);
	        
			currentMonth.add(Calendar.MONTH, 1);
		}
	}
	
	/**
	 * This method generates a full report based on the full log period
	 * 
	 * @throws Exception if error
	 */
	private static void statReportGeneral() throws Exception {
		
		//Prefix
		String inputPrefix = "dspace-log-general-";
		String outputPrefix = "report-general-";        
		
		String myFormat = "html";
        StringBuffer myInput = null;
        StringBuffer myOutput = null;
        String myMap = null;
		
        myInput = new StringBuffer(outputLogDirectory);
        myInput.append(inputPrefix);
        myInput.append(calendar.get(Calendar.YEAR));
        myInput.append("-");
        myInput.append(calendar.get(Calendar.MONTH)+1);
        myInput.append("-");
        myInput.append(calendar.get(Calendar.DAY_OF_MONTH));
        myInput.append(outputSuffix); 
        
        myOutput = new StringBuffer(outputReportDirectory);
        myOutput.append(outputPrefix);
        myOutput.append(calendar.get(Calendar.YEAR));
        myOutput.append("-");
        myOutput.append(calendar.get(Calendar.MONTH)+1);
        myOutput.append("-");
        myOutput.append(calendar.get(Calendar.DAY_OF_MONTH));
        myOutput.append(".");
        myOutput.append(myFormat);
		
		ReportGenerator.processReport(context, myFormat, myInput.toString(), myOutput.toString(), myMap);
	}
	
	/**
	 * This script starts from the year and month specified below and loops each month until the current month
	 * generating monthly reports from the DStat aggregation files
	 * 
	 * @throws Exception if error
	 */
	private static void statReportInitial() throws Exception {
		
		//Prefix
		String inputPrefix = "dspace-log-monthly-";
		String outputPrefix = "report-";        
		
		String myFormat = "html";
        StringBuffer myInput = null;
        StringBuffer myOutput = null;
        String myMap = null;
				
		Calendar reportEndDate = new GregorianCalendar( calendar.get(Calendar.YEAR),
				  										calendar.get(Calendar.MONTH),
				  										calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
		
        Calendar currentMonth = (Calendar)reportStartDate.clone();

		while(currentMonth.before(reportEndDate)) {
			
			myInput = new StringBuffer(outputLogDirectory);
	        myInput.append(inputPrefix);
	        myInput.append(currentMonth.get(Calendar.YEAR));
	        myInput.append("-");
	        myInput.append(currentMonth.get(Calendar.MONTH)+1);
	        myInput.append(outputSuffix); 
	        
	        myOutput = new StringBuffer(outputReportDirectory);
	        myOutput.append(outputPrefix);
	        myOutput.append(currentMonth.get(Calendar.YEAR));
	        myOutput.append("-");
	        myOutput.append(currentMonth.get(Calendar.MONTH)+1);
	        myOutput.append(".");
	        myOutput.append(myFormat);			
			
			ReportGenerator.processReport(context, myFormat, myInput.toString(), myOutput.toString(), myMap);
			
			currentMonth.add(Calendar.MONTH, 1);
		}	
	}
	
	/**
	 * This method generates a report from the aggregation files which have been run for the most recent month
	 * 
	 * @throws Exception if error
	 */
	private static void statReportMonthly() throws Exception 
	{
		//Prefix
		String inputPrefix = "dspace-log-monthly-";
		String outputPrefix = "report-";        
		
		String myFormat = "html";
        StringBuffer myInput = null;
        StringBuffer myOutput = null;
        String myMap = null;
	
        myInput = new StringBuffer(outputLogDirectory);
        myInput.append(inputPrefix);
        myInput.append(calendar.get(Calendar.YEAR));
        myInput.append("-");
        myInput.append(calendar.get(Calendar.MONTH)+1);
        myInput.append(outputSuffix); 
        
        myOutput = new StringBuffer(outputReportDirectory);
        myOutput.append(outputPrefix);
        myOutput.append(calendar.get(Calendar.YEAR));
        myOutput.append("-");
        myOutput.append(calendar.get(Calendar.MONTH)+1);
        myOutput.append(".");
        myOutput.append(myFormat);
		
        ReportGenerator.processReport(context, myFormat, myInput.toString(), myOutput.toString(), myMap);
	}
	
	/*
	 * Output the usage information
	 */
	private static void usage() throws Exception {
	
		System.out.println("Usage: java CreateStatReport -r <statistic to run>");
		System.out.println("Available: <stat-initial> <stat-general> <stat-monthly> <stat-report-initial> <stat-report-general> <stat-report-monthly>");     
		return;		
	}
}
