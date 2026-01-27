/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.EntityType;
import org.dspace.content.RelationshipType;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Context;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This script is used to initialize the database with a set of relationship types that are written
 * in an xml file that is given to this script.
 * This XML file needs to have a proper XML structure and needs to define the variables of the RelationshipType object.
 */
public class InitializeEntities {

    private final static Logger log = LogManager.getLogger();

    private final RelationshipTypeService relationshipTypeService;
    private final EntityTypeService entityTypeService;


    private InitializeEntities() {
        relationshipTypeService = ContentServiceFactory.getInstance().getRelationshipTypeService();
        entityTypeService = ContentServiceFactory.getInstance().getEntityTypeService();
    }

    /**
     * The main method for this script
     *
     * @param argv  The command line arguments given with this command
     * @throws SQLException         If something goes wrong with the database
     * @throws AuthorizeException   If something goes wrong with permissions
     * @throws ParseException       If something goes wrong with the parsing
     */
    public static void main(String[] argv) throws SQLException, AuthorizeException, ParseException {
        InitializeEntities initializeEntities = new InitializeEntities();
        // Set up command-line options and parse arguments
        CommandLineParser parser = new DefaultParser();
        Options options = createCommandLineOptions();
        CommandLine line = parser.parse(options,argv);
        // First of all, check if the help option was entered or a required argument is missing
        checkHelpEntered(options, line);
        // Get the file location from the command line
        String fileLocation = getFileLocationFromCommandLine(line);
        // Run the script
        initializeEntities.run(fileLocation);
    }

    /**
     * Check if the help option was entered or a required argument is missing. If so, print help and exit.
     * @param options the defined command-line options
     * @param line the parsed command-line arguments
     */
    private static void checkHelpEntered(Options options, CommandLine line) {
        if (line.hasOption("h") || !line.hasOption("f")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Initialize Entities", options);
            System.exit(0);
        }
    }

    /**
     * Get the file path from the command-line argument. Exits with exit code 1 if no file argument was entered.
     * @param line the parsed command-line arguments
     * @return the file path
     */
    private static String getFileLocationFromCommandLine(CommandLine line) {
        String query = line.getOptionValue("f");
        if (StringUtils.isEmpty(query)) {
            System.out.println("No file location was entered");
            log.info("No file location was entered");
            System.exit(1);
        }
        return query;
    }

    /**
     * Create the command-line options
     * @return the command-line options
     */
    protected static Options createCommandLineOptions() {
        Options options = new Options();
        options.addOption("f", "file", true, "the path to the file containing the " +
                "relationship definitions (e.g. ${dspace.dir}/config/entities/relationship-types.xml)");
        options.addOption("h", "help", false, "print this message");

        return options;
    }

    /**
     * Run the script for the given file location
     * @param fileLocation the file location
     * @throws SQLException If something goes wrong initializing context or inserting relationship types
     * @throws AuthorizeException  If the script user fails to authorize while inserting relationship types
     */
    private void run(String fileLocation) throws SQLException, AuthorizeException {
        Context context = new Context();
        context.turnOffAuthorisationSystem();
        this.parseXMLToRelations(context, fileLocation);
        context.complete();
    }

    /**
     * Parse the XML file at fileLocation to create relationship types in the database
     * @param context DSpace context
     * @param fileLocation the full or relative file path to the relationship types XML
     * @throws AuthorizeException If the script user fails to authorize while inserting relationship types
     */
    private void parseXMLToRelations(Context context, String fileLocation) throws AuthorizeException {
        try {
            File fXmlFile = new File(fileLocation);
            // This XML builder will allow external entities, so the relationship types XML should
            // be considered trusted by administrators
            DocumentBuilder dBuilder = XMLUtils.getTrustedDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);

            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("type");
            for (int i = 0; i < nList.getLength(); i++) {
                Node nNode = nList.item(i);

                if (nNode.getNodeType() == Node.ELEMENT_NODE) {

                    Element eElement = (Element) nNode;

                    String leftType =  eElement.getElementsByTagName("leftType").item(0).getTextContent();
                    String rightType = eElement.getElementsByTagName("rightType").item(0).getTextContent();
                    String leftwardType = eElement.getElementsByTagName("leftwardType").item(0).getTextContent();
                    String rightwardType = eElement.getElementsByTagName("rightwardType").item(0).getTextContent();
                    Node copyToLeftNode = eElement.getElementsByTagName("copyToLeft").item(0);
                    Boolean copyToLeft;
                    if (copyToLeftNode == null) {
                        copyToLeft = false;
                    } else {
                        copyToLeft = Boolean.valueOf(copyToLeftNode.getTextContent());

                    }
                    Node copyToRightNode = eElement.getElementsByTagName("copyToRight").item(0);
                    Boolean copyToRight;
                    if (copyToRightNode == null) {
                        copyToRight = false;
                    } else {
                        copyToRight = Boolean.valueOf(copyToRightNode.getTextContent());
                    }

                    Node tiltedNode = eElement.getElementsByTagName("tilted").item(0);
                    RelationshipType.Tilted tilted;
                    if (tiltedNode == null) {
                        tilted = RelationshipType.Tilted.NONE;
                    } else {
                        tilted = RelationshipType.Tilted.valueOf(tiltedNode.getTextContent().toUpperCase());
                    }

                    NodeList leftCardinalityList = eElement.getElementsByTagName("leftCardinality");
                    NodeList rightCardinalityList = eElement.getElementsByTagName("rightCardinality");

                    String leftCardinalityMin = "";
                    String leftCardinalityMax = "";

                    String rightCardinalityMin = "";
                    String rightCardinalityMax = "";

                    for (int j = 0; j < leftCardinalityList.getLength(); j++) {
                        Node node = leftCardinalityList.item(j);
                        leftCardinalityMin = getCardinalityMinString(leftCardinalityMin,(Element) node, "min");
                        leftCardinalityMax = getCardinalityMinString(leftCardinalityMax,(Element) node, "max");

                    }

                    for (int j = 0; j < rightCardinalityList.getLength(); j++) {
                        Node node = rightCardinalityList.item(j);
                        rightCardinalityMin = getCardinalityMinString(rightCardinalityMin,(Element) node, "min");
                        rightCardinalityMax = getCardinalityMinString(rightCardinalityMax,(Element) node, "max");

                    }
                    populateRelationshipType(context, leftType, rightType, leftwardType, rightwardType,
                                             leftCardinalityMin, leftCardinalityMax,
                                             rightCardinalityMin, rightCardinalityMax, copyToLeft, copyToRight,
                                             tilted);


                }
            }
        } catch (ParserConfigurationException | SAXException | IOException | SQLException e) {
            log.error("An error occurred while parsing the XML file to relations", e);
        }
    }

    /**
     * Extract the min or max value for the left or right cardinality from the node text content
     * @param leftCardinalityMin current left cardinality min
     * @param node node to extract the min or max value from
     * @param minOrMax element tag name to parse
     * @return final left cardinality min
     */
    private String getCardinalityMinString(String leftCardinalityMin, Element node, String minOrMax) {
        if (node.getElementsByTagName(minOrMax).getLength() > 0) {
            leftCardinalityMin = node.getElementsByTagName(minOrMax).item(0).getTextContent();
        }
        return leftCardinalityMin;
    }

    /**
     * Populate the relationship type based on values parsed from the XML relationship types configuration
     *
     * @param context DSpace context
     * @param leftType left relationship type (e.g. "Publication").
     * @param rightType right relationship type (e.g. "Journal").
     * @param leftwardType leftward relationship type (e.g. "isAuthorOfPublication").
     * @param rightwardType rightward relationship type (e.g. "isPublicationOfAuthor").
     * @param leftCardinalityMin left cardinality min
     * @param leftCardinalityMax left cardinality max
     * @param rightCardinalityMin right cardinality min
     * @param rightCardinalityMax right cardinality max
     * @param copyToLeft copy metadata values to left if right side is deleted
     * @param copyToRight copy metadata values to right if left side is deleted
     * @param tilted set a tilted relationship side (left or right) if there are many relationships going one way
     *               to help performance (e.g. authors with 1000s of publications)
     * @throws SQLException if database error occurs while saving the relationship type
     * @throws AuthorizeException if authorization error occurs while saving the relationship type
     */
    private void populateRelationshipType(Context context, String leftType, String rightType, String leftwardType,
                                          String rightwardType, String leftCardinalityMin, String leftCardinalityMax,
                                          String rightCardinalityMin, String rightCardinalityMax,
                                          Boolean copyToLeft, Boolean copyToRight, RelationshipType.Tilted tilted)
        throws SQLException, AuthorizeException {

        EntityType leftEntityType = entityTypeService.findByEntityType(context,leftType);
        if (leftEntityType == null) {
            leftEntityType = entityTypeService.create(context, leftType);
        }
        EntityType rightEntityType = entityTypeService.findByEntityType(context, rightType);
        if (rightEntityType == null) {
            rightEntityType = entityTypeService.create(context, rightType);
        }
        Integer leftCardinalityMinInteger;
        Integer leftCardinalityMaxInteger;
        Integer rightCardinalityMinInteger;
        Integer rightCardinalityMaxInteger;
        if (StringUtils.isNotBlank(leftCardinalityMin)) {
            leftCardinalityMinInteger = Integer.parseInt(leftCardinalityMin);
        } else {
            leftCardinalityMinInteger = null;
        }
        if (StringUtils.isNotBlank(leftCardinalityMax)) {
            leftCardinalityMaxInteger = Integer.parseInt(leftCardinalityMax);
        } else {
            leftCardinalityMaxInteger = null;
        }
        if (StringUtils.isNotBlank(rightCardinalityMin)) {
            rightCardinalityMinInteger = Integer.parseInt(rightCardinalityMin);
        } else {
            rightCardinalityMinInteger = null;
        }
        if (StringUtils.isNotBlank(rightCardinalityMax)) {
            rightCardinalityMaxInteger = Integer.parseInt(rightCardinalityMax);
        } else {
            rightCardinalityMaxInteger = null;
        }
        RelationshipType relationshipType = relationshipTypeService
            .findbyTypesAndTypeName(context, leftEntityType, rightEntityType, leftwardType, rightwardType);
        if (relationshipType == null) {
            relationshipTypeService.create(context, leftEntityType, rightEntityType, leftwardType, rightwardType,
                                           leftCardinalityMinInteger, leftCardinalityMaxInteger,
                                           rightCardinalityMinInteger, rightCardinalityMaxInteger,
                                           copyToLeft, copyToRight, tilted);
        } else {
            relationshipType.setCopyToLeft(copyToLeft);
            relationshipType.setCopyToRight(copyToRight);
            relationshipType.setTilted(tilted);
            relationshipType.setLeftMinCardinality(leftCardinalityMinInteger);
            relationshipType.setLeftMaxCardinality(leftCardinalityMaxInteger);
            relationshipType.setRightMinCardinality(rightCardinalityMinInteger);
            relationshipType.setRightMaxCardinality(rightCardinalityMaxInteger);
            relationshipTypeService.update(context, relationshipType);
        }
    }
}
