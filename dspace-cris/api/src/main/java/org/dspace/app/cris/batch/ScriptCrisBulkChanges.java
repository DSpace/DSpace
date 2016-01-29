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
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.importexport.ExcelBulkChangesService;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.OrganizationUnit;
import org.dspace.app.cris.model.Project;
import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.jdyna.ACrisNestedObject;
import org.dspace.app.cris.model.jdyna.DynamicObjectType;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.util.ImportExportUtils;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.utils.DSpace;

import it.cilea.osd.jdyna.model.ANestedPropertiesDefinition;
import it.cilea.osd.jdyna.model.ANestedProperty;
import it.cilea.osd.jdyna.model.ATypeNestedObject;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.model.Property;

public class ScriptCrisBulkChanges {
	/** log4j logger */
	private static Logger log = Logger.getLogger(ScriptCrisBulkChanges.class);

	/**
	 * Batch script to load data from XML or CSV or Excel. See the technical documentation for further details. 
	 */
	public static <ACO extends ACrisObject<P, TP, NP, NTP, ACNO, ATNO>, P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> void main(
			String[] args) throws ParseException, SQLException {
		// TODO move logic in ImportExportUtils
		log.info("#### START IMPORT: -----" + new Date() + " ----- ####");
		Context dspaceContext = new Context();
		dspaceContext.setIgnoreAuthorization(true);
		DSpace dspace = new DSpace();
		ApplicationService applicationService = dspace.getServiceManager().getServiceByName("applicationService",
				ApplicationService.class);

		String filePath = null;

		CommandLineParser parser = new PosixParser();

		Options options = new Options();
		options.addOption("h", "help", false, "help");

		options.addOption("f", "file", true, "File to import*");
//		options.addOption("t", "format", true, "The format input (XMLBulkChangesService, ExcelBulkChangesService, CSVBulkChangesService)");
		options.addOption("a", "active", false, "Set newly created objects as active");
		options.addOption("e", "entity", true, "The entity type to import (rp, ou, pj, others**)");

		// active or inactive for newly created epersons. Default is inactive.
		boolean status = false;

		CommandLine line = parser.parse(options, args);

		String entityType = "rp";
		if (line.hasOption("e")) {
			entityType = line.getOptionValue("e");
		}

		if (line.hasOption('h') || StringUtils.isEmpty(entityType)) {
			HelpFormatter myhelp = new HelpFormatter();
			myhelp.printHelp("ScriptCrisBulkChanges \n", options);
			System.out
					.println("\n\nUSAGE:\n ScriptCrisBulkChanges -e (rp|ou|pj|<others>) [-f path_file -a] -\n");
			System.out
					.println("* Please note: -f is not mandatory, if -f is not specified then default is : "
							+ ImportExportUtils.PATH_DEFAULT_XML);
			
			System.out.println("** \"others\" in this case is a placeholder for these types of dynamic entities installed on your repository, the follow are your:");
			for(DynamicObjectType dyno : applicationService.getList(DynamicObjectType.class)) {
		         System.out.println(dyno.getShortName());
			}
			
			System.exit(0);
		}

		if (!line.hasOption("f")) {
			filePath = ImportExportUtils.PATH_DEFAULT_XML;
		} else {
			filePath = line.getOptionValue("f");
		}

		String format = ExcelBulkChangesService.SERVICE_NAME;
//		if (!line.hasOption("t")) {
//		    //default CSV
//			format = ExcelBulkChangesService.SERVICE_NAME;
//		} else {
//			format = line.getOptionValue("t");
//		}

		if (line.hasOption("active")) {
			status = true;
		}

		ACO tmpCrisObject = null;
		try {
			if (StringUtils.equalsIgnoreCase("rp", entityType)) {
				tmpCrisObject = (ACO) ResearcherPage.class.newInstance();
			} else if (StringUtils.equalsIgnoreCase("ou", entityType)) {
				tmpCrisObject = (ACO) OrganizationUnit.class.newInstance();
			} else if (StringUtils.equalsIgnoreCase("pj", entityType)) {
				tmpCrisObject = (ACO) Project.class.newInstance();
			} else {
				ResearchObject tmp = ResearchObject.class.newInstance();
				tmp.setTypo(applicationService.findTypoByShortName(DynamicObjectType.class, entityType));
				tmpCrisObject = (ACO) tmp;
			}
		} catch (InstantiationException | IllegalAccessException e1) {
			System.out.println(e1.getMessage());
			System.exit(1);
		}

		String path = ConfigurationManager.getProperty(CrisConstants.CFG_MODULE, "file.import.path");		
		File dir = new File(path);
		if(!dir.exists()) {
			dir.mkdir();
		}
		
		
		try {
			ImportExportUtils.process(format, new FileInputStream(filePath), dir, applicationService, dspaceContext,
					status, tmpCrisObject.getClassPropertiesDefinition(), (Class<ACO>) tmpCrisObject.getClass(), tmpCrisObject,
					tmpCrisObject.getClassNested(), tmpCrisObject.getClassTypeNested(), tmpCrisObject.getClassNested().newInstance().getClassPropertiesDefinition());
		} catch (Exception e) {
			log.error(e.getMessage());
		}
		
		log.info("#### END IMPORT: -----" + new Date() + " ----- ####");
		dspaceContext.complete();
	}
}
