/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.cris.batch;

import java.sql.SQLException;
import java.util.Date;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.util.UnsubscribeEPersionUtils;
import org.dspace.core.Context;

public class ScriptCrisUnsubscribeEPerson {
	/** log4j logger */
	private static Logger log = Logger.getLogger(ScriptCrisUnsubscribeEPerson.class);

	/**
	 * Batch script to disable notification. See the technical documentation for further details. 
	 */
	public static void main(
			String[] args) throws ParseException, SQLException {
		log.info("#### START UNSUBSCRIBE EPERSON: -----" + new Date() + " ----- ####");
		Context dspaceContext = new Context();
		dspaceContext.turnOffAuthorisationSystem();
		dspaceContext.turnOffItemWrapper();
		CommandLineParser parser = new PosixParser();

		Options options = new Options();
		options.addOption("h", "help", false, "help");
		options.addOption("e", "email", true, "Email of ePerson *");
		options.addOption("q", "quiet", false, "Skip sending the summary email");
		
		CommandLine line = parser.parse(options, args);
		String eperson = "";
		if (line.hasOption("e")) {
			eperson = line.getOptionValue("e");
		}
		boolean skipEmail = line.hasOption('q');

		if (line.hasOption('h') || StringUtils.isEmpty(eperson)) {
			HelpFormatter myhelp = new HelpFormatter();
			myhelp.printHelp("ScriptCrisBulkChanges \n", options);
			System.out
					.println("\n\nUSAGE:\n ScriptCrisBulkChanges -e email -\n");
			System.out
					.println("* Please note: -e is mandatory and is the email of an ePerson");
			
			System.exit(0);
		}

		try {
			UnsubscribeEPersionUtils.process(dspaceContext, eperson, skipEmail);
		} catch (Exception e) {
			log.error(e.getMessage());
		}
		
		log.info("#### END UNSUBSCRIBE EPERSON: -----" + new Date() + " ----- ####");
		// Finish off and tidy up
		dspaceContext.restoreAuthSystemState();
        dspaceContext.restoreItemWrapperState();
		dspaceContext.complete();
	}
}
