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
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
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

public class InitializeEntities {

    private final static Logger log = Logger.getLogger(InitializeEntities.class);

    private RelationshipTypeService relationshipTypeService;
    private RelationshipService relationshipService;
    private EntityTypeService entityTypeService;

    private InitializeEntities() {
        relationshipTypeService = ContentServiceFactory.getInstance().getRelationshipTypeService();
        relationshipService = ContentServiceFactory.getInstance().getRelationshipService();
        entityTypeService = ContentServiceFactory.getInstance().getEntityTypeService();

    }

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
        List<RelationshipType> relationshipTypes = this.parseXMLToRelations(context, fileLocation);
        this.saveRelations(context, relationshipTypes);
    }

    private void saveRelations(Context context, List<RelationshipType> list) throws SQLException, AuthorizeException {
        for (RelationshipType relationshipType : list) {
            RelationshipType relationshipTypeFromDb = relationshipTypeService.findbyTypesAndLabels(context,
                                                                               relationshipType.getLeftType(),
                                                                               relationshipType.getRightType(),
                                                                               relationshipType.getLeftLabel(),
                                                                               relationshipType.getRightLabel());
            if (relationshipTypeFromDb == null) {
                relationshipTypeService.create(context, relationshipType);
            } else {
                relationshipTypeFromDb.setLeftMinCardinality(relationshipType.getLeftMinCardinality());
                relationshipTypeFromDb.setLeftMaxCardinality(relationshipType.getLeftMaxCardinality());
                relationshipTypeFromDb.setRightMinCardinality(relationshipType.getRightMinCardinality());
                relationshipTypeFromDb.setRightMaxCardinality(relationshipType.getRightMaxCardinality());
                relationshipTypeService.update(context, relationshipTypeFromDb);
            }
        }
        context.commit();
        context.complete();
    }

    private List<RelationshipType> parseXMLToRelations(Context context, String fileLocation) throws AuthorizeException {
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
                    String leftLabel = eElement.getElementsByTagName("leftLabel").item(0).getTextContent();
                    String rightLabel = eElement.getElementsByTagName("rightLabel").item(0).getTextContent();


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
                    RelationshipType relationshipType = populateRelationshipType(context,leftType,rightType,leftLabel,
                                                                                 rightLabel,leftCardinalityMin,
                                                                                 leftCardinalityMax,
                                                                                 rightCardinalityMin,
                                                                                 rightCardinalityMax);


                    relationshipTypes.add(relationshipType);
                }
            }
            return relationshipTypes;
        } catch (ParserConfigurationException | SAXException | IOException | SQLException e) {
            log.error(e, e);
        }
        return null;
    }

    private String getString(String leftCardinalityMin,Element node, String minOrMax) {
        if (node.getElementsByTagName(minOrMax).getLength() > 0) {
            leftCardinalityMin = node.getElementsByTagName(minOrMax).item(0).getTextContent();
        }
        return leftCardinalityMin;
    }

    private RelationshipType populateRelationshipType(Context context,String leftType,String rightType,String leftLabel,
                                                      String rightLabel,String leftCardinalityMin,
                                                      String leftCardinalityMax,String rightCardinalityMin,
                                                      String rightCardinalityMax)
        throws SQLException, AuthorizeException {
        RelationshipType relationshipType = new RelationshipType();

        EntityType leftEntityType = entityTypeService.findByEntityType(context,leftType);
        if (leftEntityType == null) {
            leftEntityType = entityTypeService.create(context, leftType);
        }
        EntityType rightEntityType = entityTypeService.findByEntityType(context, rightType);
        if (rightEntityType == null) {
            rightEntityType = entityTypeService.create(context, rightType);
        }
        relationshipType.setLeftType(leftEntityType);
        relationshipType.setRightType(rightEntityType);
        relationshipType.setLeftLabel(leftLabel);
        relationshipType.setRightLabel(rightLabel);
        if (StringUtils.isNotBlank(leftCardinalityMin)) {
            relationshipType.setLeftMinCardinality(Integer.parseInt(leftCardinalityMin));
        } else {
            relationshipType.setLeftMinCardinality(Integer.MIN_VALUE);
        }
        if (StringUtils.isNotBlank(leftCardinalityMax)) {
            relationshipType.setLeftMaxCardinality(Integer.parseInt(leftCardinalityMax));
        } else {
            relationshipType.setLeftMaxCardinality(Integer.MAX_VALUE);
        }
        if (StringUtils.isNotBlank(rightCardinalityMin)) {
            relationshipType.setRightMinCardinality(Integer.parseInt(rightCardinalityMin));
        } else {
            relationshipType.setRightMinCardinality(Integer.MIN_VALUE);
        }
        if (StringUtils.isNotBlank(rightCardinalityMax)) {
            relationshipType.setRightMaxCardinality(Integer.parseInt(rightCardinalityMax));
        } else {
            relationshipType.setRightMaxCardinality(Integer.MAX_VALUE);
        }
        return relationshipType;
    }
}
