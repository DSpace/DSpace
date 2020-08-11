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
import java.util.LinkedList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.EntityType;
import org.dspace.content.RelationshipType;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Context;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This script is used to initialize the database with a set of relationshiptypes that are written
 * in an xml file that is given to this script.
 * This XML file needs to have a proper XML structure and needs to define the variables of the RelationshipType object
 */
public class InitializeEntities {

    private final static Logger log = LogManager.getLogger();

    private RelationshipTypeService relationshipTypeService;
    private RelationshipService relationshipService;
    private EntityTypeService entityTypeService;


    private InitializeEntities() {
        relationshipTypeService = ContentServiceFactory.getInstance().getRelationshipTypeService();
        relationshipService = ContentServiceFactory.getInstance().getRelationshipService();
        entityTypeService = ContentServiceFactory.getInstance().getEntityTypeService();
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
        InitializeEntities initializeEntities = new InitializeEntities();
        CommandLineParser parser = new PosixParser();
        Options options = createCommandLineOptions();
        CommandLine line = parser.parse(options,argv);
        String fileLocation = getFileLocationFromCommandLine(line);
        checkHelpEntered(options, line);
        initializeEntities.run(fileLocation);
    }
    private static void checkHelpEntered(Options options, CommandLine line) {
        if (line.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Intialize Entities", options);
            System.exit(0);
        }
    }
    private static String getFileLocationFromCommandLine(CommandLine line) {
        String query = line.getOptionValue("f");
        if (StringUtils.isEmpty(query)) {
            System.out.println("No file location was entered");
            log.info("No file location was entered");
            System.exit(1);
        }
        return query;
    }

    protected static Options createCommandLineOptions() {
        Options options = new Options();
        options.addOption("f", "file", true, "the location for the file containing the xml data");

        return options;
    }

    private void run(String fileLocation) throws SQLException, AuthorizeException {
        Context context = new Context();
        context.turnOffAuthorisationSystem();
        this.parseXMLToRelations(context, fileLocation);
        context.complete();
    }

    private void parseXMLToRelations(Context context, String fileLocation) throws AuthorizeException {
        try {
            File fXmlFile = new File(fileLocation);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = null;
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);

            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("type");
            List<RelationshipType> relationshipTypes = new LinkedList<>();
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

                    NodeList leftCardinalityList = eElement.getElementsByTagName("leftCardinality");
                    NodeList rightCardinalityList = eElement.getElementsByTagName("rightCardinality");

                    String leftCardinalityMin = "";
                    String leftCardinalityMax = "";

                    String rightCardinalityMin = "";
                    String rightCardinalityMax = "";

                    for (int j = 0; j < leftCardinalityList.getLength(); j++) {
                        Node node = leftCardinalityList.item(j);
                        leftCardinalityMin = getString(leftCardinalityMin,(Element) node, "min");
                        leftCardinalityMax = getString(leftCardinalityMax,(Element) node, "max");

                    }

                    for (int j = 0; j < rightCardinalityList.getLength(); j++) {
                        Node node = rightCardinalityList.item(j);
                        rightCardinalityMin = getString(rightCardinalityMin,(Element) node, "min");
                        rightCardinalityMax = getString(rightCardinalityMax,(Element) node, "max");

                    }
                    populateRelationshipType(context, leftType, rightType, leftwardType, rightwardType,
                                             leftCardinalityMin, leftCardinalityMax,
                                             rightCardinalityMin, rightCardinalityMax, copyToLeft, copyToRight);


                }
            }
        } catch (ParserConfigurationException | SAXException | IOException | SQLException e) {
            log.error("An error occurred while parsing the XML file to relations", e);
        }
    }

    private String getString(String leftCardinalityMin,Element node, String minOrMax) {
        if (node.getElementsByTagName(minOrMax).getLength() > 0) {
            leftCardinalityMin = node.getElementsByTagName(minOrMax).item(0).getTextContent();
        }
        return leftCardinalityMin;
    }

    private void populateRelationshipType(Context context, String leftType, String rightType, String leftwardType,
                                          String rightwardType, String leftCardinalityMin, String leftCardinalityMax,
                                          String rightCardinalityMin, String rightCardinalityMax,
                                          Boolean copyToLeft, Boolean copyToRight)
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
                                           copyToLeft, copyToRight);
        } else {
            relationshipType.setCopyToLeft(copyToLeft);
            relationshipType.setCopyToRight(copyToRight);
            relationshipType.setLeftMinCardinality(leftCardinalityMinInteger);
            relationshipType.setLeftMaxCardinality(leftCardinalityMaxInteger);
            relationshipType.setRightMinCardinality(rightCardinalityMinInteger);
            relationshipType.setRightMaxCardinality(rightCardinalityMaxInteger);
            relationshipTypeService.update(context, relationshipType);
        }
    }
}
