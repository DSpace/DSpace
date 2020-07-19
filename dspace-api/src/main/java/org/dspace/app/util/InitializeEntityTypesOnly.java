/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import java.sql.SQLException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.EntityType;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.EntityTypeService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * This script is used to initialize the database with the needed entity types that are written
 * in the cris.cfg entity-types property and/or passed to the script.
 */
public class InitializeEntityTypesOnly {

    private final static Logger log = LogManager.getLogger();

    private EntityTypeService entityTypeService;

    private ConfigurationService configurationService;

    private InitializeEntityTypesOnly() {
        entityTypeService = ContentServiceFactory.getInstance().getEntityTypeService();
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    }

    /**
     * The main method for this script
     *
     * @param argv  The commandline arguments given with this command
     * @throws SQLException         If something goes wrong with the database
     * @throws AuthorizeException   If something goes wrong with permissions
     * @throws ParseException       If something goes wrong with the parsing
     */
    public static void main(String[] argv) throws SQLException, AuthorizeException, ParseException {
        InitializeEntityTypesOnly initializeEntities = new InitializeEntityTypesOnly();
        CommandLineParser parser = new PosixParser();
        Options options = createCommandLineOptions();
        CommandLine line = parser.parse(options, argv);
        checkHelpEntered(options, line);
        String[] entities = ArrayUtils.addAll(ArrayUtils.nullToEmpty(line.getOptionValues('e')),
                ArrayUtils.nullToEmpty(initializeEntities.getDefaultEntities()));
        Context context = new Context();
        context.turnOffAuthorisationSystem();
        initializeEntities.assureExists(context, entities);
        context.restoreAuthSystemState();
        context.complete();
    }

    private static void checkHelpEntered(Options options, CommandLine line) {
        if (line.hasOption("h") || (!line.hasOption('e') && !line.hasOption('d'))) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Intialize Entity types only", options);
            System.exit(0);
        }
    }

    protected static Options createCommandLineOptions() {
        Options options = new Options();
        options.addOption("e", "entity", true, "the label of an entity type to enforce");
        options.addOption("d", "default", false,
                "enforce the entity types defined in the cris.entity-types property (see cris.cfg)");
        return options;
    }

    private String[] getDefaultEntities() {
        return configurationService.getArrayProperty("cris.entity-type");
    }

    private void assureExists(Context context, String[] entities) throws SQLException, AuthorizeException {
        for (String entity : entities) {
            EntityType entityType = entityTypeService.findByEntityType(context, entity);
            if (entityType == null) {
                entityType = entityTypeService.create(context, entity);
            }
        }
    }
}
