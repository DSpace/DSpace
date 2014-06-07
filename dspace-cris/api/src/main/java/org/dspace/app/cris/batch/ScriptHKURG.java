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

public class ScriptHKURG {
	/** log4j logger */
	private static Logger log = Logger.getLogger(ScriptHKURG.class);

	/**
	 * Batch script to load the grant data in the RGs database from RPs. See the
	 * technical documentation for further details.
	 */
	public static void main(String[] args) throws ParseException, SQLException {
		// TODO move logic in ImportExportUtils
		log.info("#### START IMPORT: -----" + new Date() + " ----- ####");
		Context dspaceContext = new Context();
		dspaceContext.setIgnoreAuthorization(true);
		DSpace dspace = new DSpace();
		ApplicationService applicationService = dspace.getServiceManager()
				.getServiceByName("applicationService",
						ApplicationService.class);
		CommandLineParser parser = new PosixParser();

		String xmlFilePath = null;

		Options options = new Options();
		options.addOption("rpmode", "rpmode", false, "if this check exist then import from internal database");
		options.addOption("f", "file", true, "File xml to import");
		options.addOption("h", "help", false, "help");

		options.addOption("status", "status", false, "Get active RPs only");
		options.addOption("active", "active", false,
				"Set active true newly created grants public");
		options.addOption("newly", "newly", false,
				"Get only newly grants find on rps");

		// RPs status flag. Default is get public RPs
		boolean status = false;
		// RGs active flag. Default is inactive.
		boolean active = false;
		// Import only newly grants (with new grantCode)
		boolean newly = false;

		CommandLine line = parser.parse(options, args);

		if (line.hasOption('h')) {
			HelpFormatter myhelp = new HelpFormatter();
			myhelp.printHelp("ScriptHRURP \n", options);
			System.out
					.println("\n\nUSAGE:\n ScriptHKURG [<-status> <-rpmode mode>|<-f file>] [<-active> <-newly>] \n");
			System.out
					.println("Please note: Script import in database mode (-rpmode) or file mode (-f), default is status false and active false and newly false (get only RPs with status false and from its projects create inactive grants and all grants finded on rps)");
			System.exit(0);
		}

		if (!line.hasOption("rpmode")) {
			if (!line.hasOption("f")) {
				xmlFilePath = ImportExportUtils.GRANT_PATH_DEFAULT_XML;
			} else {
				xmlFilePath = line.getOptionValue("f");
			}
		} else {
			if (line.hasOption("status")) {
				status = true;
			} else {
				status = false;
			}
			if (line.hasOption("newly")) {
				newly = true;
			} else {
				newly = false;
			}
		}
		
		if (line.hasOption("active")) {
			active = true;
		} else {
			active = false;
		}

		try {
			if (xmlFilePath != null) {
				String path = ConfigurationManager
						.getProperty(CrisConstants.CFG_MODULE, "researchergrant.file.import.path");
				File dir = new File(path);
				ImportExportUtils.importGrantsXML(new FileInputStream(
						xmlFilePath), dir, applicationService, dspaceContext,
						active);
			} else {
				ImportExportUtils.importGrants(applicationService,
						dspaceContext, status, active, newly);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		log.info("#### END IMPORT: -----" + new Date() + " ----- ####");
		dspaceContext.complete();
	}
}
