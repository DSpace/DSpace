/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.batch;


import java.io.File;
import java.io.FileInputStream;
import java.sql.SQLException;
import java.util.Date;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.util.ImportExportUtils;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.utils.DSpace;

public class ScriptHKURP {
	/** log4j logger */
	private static Logger log = Logger.getLogger(ScriptHKURP.class);

	/**
	 * Batch script to load the contact data in the RPs database from XML. See
	 * the technical documentation for further details.
	 */
	public static void main(String[] args) throws ParseException, SQLException {
		// TODO move logic in ImportExportUtils
		log.info("#### START IMPORT: -----" + new Date() + " ----- ####");
		Context dspaceContext = new Context();
		dspaceContext.setIgnoreAuthorization(true);
		DSpace dspace = new DSpace();
        ApplicationService applicationService = dspace.getServiceManager().getServiceByName(
                "applicationService", ApplicationService.class);
        
		String excelFilePath = null;

		CommandLineParser parser = new PosixParser();

		Options options = new Options();
		options.addOption("h", "help", false, "help");

		options.addOption("f", "file", true, "File xml to import");

		// allen's added argument
		options.addOption("active", "active", false,
				"Set newly created epersons active");
		options.addOption("inactive", "inactive", false,
				"Set newly created epersons inactive");
			
		// active or inactive for newly created epersons. Default is inactive.
		boolean status = false;

		CommandLine line = parser.parse(options, args);

		if (line.hasOption('h')) {
			HelpFormatter myhelp = new HelpFormatter();
			myhelp.printHelp("ScriptHRURP \n", options);
			System.out
					.println("\n\nUSAGE:\n ScriptHKURP <-f path_file_xml> \n");
			System.out
					.println("Please note: -f is not mandatory, if -f is not specified then default path_file_xml is : "
							+ ImportExportUtils.PATH_DEFAULT_XML);
			System.exit(0);
		}

		if (!line.hasOption("f")) {
			excelFilePath = ImportExportUtils.PATH_DEFAULT_XML;
		} else {
			excelFilePath = line.getOptionValue("f");
		}

		// allen: use -active to make newly created eperson active.
		// allen: -inactive is optional. Default is already inactive.
		if (line.hasOption("active")) {
			status = true;
		} else {
			status = false;
		}
		
		String path = ConfigurationManager
				.getProperty(CrisConstants.CFG_MODULE, "researcherpage.file.import.path");
		File dir = new File(path);
		try {
			ImportExportUtils.importResearchersXML(new FileInputStream(excelFilePath),
					dir, applicationService, dspaceContext, status);
		} catch (Exception e) {
			log.error(e.getMessage());
		} 
		log.info("#### END IMPORT: -----" + new Date() + " ----- ####");
		dspaceContext.complete();
	}
}
