/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.crosswalk;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.jdom2.Element;



/**
 * XOAI ingestion crosswalk
 * <p>
 * Processes raw XOAI metadata in element/field nested nodes and populates the given DSO with
 * the parsed metadata. This is useful for DSpace-DSpace harvests where metadata can be
 * effectively copied using the source system's metadata schema.
 *
 * @author Kim Shepherd
 */
public class XOAIIngestionCrosswalk implements IngestionCrosswalk {

    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private CrosswalkMetadataValidator metadataValidator = new CrosswalkMetadataValidator();
    private static Logger log = LogManager.getLogger(XOAIIngestionCrosswalk.class);

    /**
     * Ingest metadata from a list of XML elements into a DSpace object.
     * @param context                     DSpace context.
     * @param dso                         DSpace Object (Item, Bitstream, etc) to which new metadata gets attached.
     * @param metadata                    List of XML Elements of metadata
     * @param createMissingMetadataFields whether to create missing fields
     * @throws CrosswalkException
     * @throws IOException
     * @throws SQLException
     * @throws AuthorizeException
     */
    @Override
    public void ingest(Context context, DSpaceObject dso, List<Element> metadata, boolean createMissingMetadataFields)
            throws CrosswalkException, IOException, SQLException, AuthorizeException {
        // As with other crosswalks, wrap this in a root element before parsing
        Element root = new Element("wrap", metadata.get(0).getNamespace());
        root.addContent(metadata);
        ingest(context, dso, root, createMissingMetadataFields);
    }

    /**
     * Ingest XOAI metadata.
     * @param context                     DSpace context.
     * @param dso                         DSpace Object (usually an Item) to which new metadata gets attached.
     * @param root                        root Element of metadata document.
     * @param createMissingMetadataFields whether to create missing fields
     * @throws CrosswalkException
     * @throws IOException
     * @throws SQLException
     * @throws AuthorizeException
     */
    @Override
    public void ingest(Context context, DSpaceObject dso, Element root, boolean createMissingMetadataFields)
            throws CrosswalkException, IOException, SQLException, AuthorizeException {

        if (root == null) {
            log.error("The element received by ingest was null");
            return;
        }
        // Only ingest if the DSO is an Item
        if (dso.getType() != Constants.ITEM) {
            throw new CrosswalkObjectNotSupported("XOAIIngestionCrosswalk can only crosswalk an Item.");
        }
        Item item = (Item)dso;



        List<Element> schemaNodes = root.getChildren();

        // First level: SCHEMA (or special node)
        for (Element schemaNode : schemaNodes) {
            // Check if this node name is a reserved string for a different type
            // eg bundles, others, repository
            String schema = schemaNode.getAttributeValue("name");
            if ("bundles".equals(schema)) {
                // We could ingest bitstreams here if we wanted to implement ORE capability
                continue;
            } else if ("others".equals(schema)) {
                // contains handle, identifier, lastModifyDate
                continue;
            } else if ("repository".equals(schema)) {
                // contains name and mail
                continue;
            }

            // Second level: ELEMENT
            List<Element> elementNodes = schemaNode.getChildren();
            for (Element elementNode : elementNodes) {
                String element = elementNode.getAttributeValue("name");

                // Third level: QUALIFIER
                // Values of "none" are crosswalked to NULL
                List<Element> thirdLevelNodes = elementNode.getChildren();
                for (Element thirdLevelNode : thirdLevelNodes) {

                    String qualifier = thirdLevelNode.getAttributeValue("name");
                    qualifier = ("none".equals(qualifier) ? null : qualifier);

                    // Test to see how deep remaining tree is, to test whether qualifier node
                    // actually does exist. If not, this node is actually language, not qualifier.
                    List<Element> fourthLevelNodes = thirdLevelNode.getChildren();
                    if (!fourthLevelNodes.isEmpty()
                            && !fourthLevelNodes.get(0).getChildren().isEmpty()) {
                        // Fourth level: LANGUAGE
                        // Values of "none are crosswalked to NULL
                        for (Element fourthLevelNode : fourthLevelNodes) {
                            String language = fourthLevelNode.getAttributeValue("name");
                            language = ("none".equals(language) ? null : language);

                            // Fifth level: FIELD VALUE
                            List<Element> fifthLevelNodes = fourthLevelNode.getChildren();
                            for (Element fifthLevelNode : fifthLevelNodes) {
                                String value = fifthLevelNode.getText();
                                MetadataField metadataField = metadataValidator.checkMetadata(context,
                                        schema,
                                        element,
                                        qualifier,
                                        createMissingMetadataFields);
                                itemService.addMetadata(context, item, metadataField, language, value);

                            }
                        }
                    } else {
                        // Third level: LANGUAGE
                        String language = qualifier;

                        // Fourth level: FIELD VALUE
                        List<Element> fieldNodes = thirdLevelNode.getChildren();
                        for (Element fieldNode : fieldNodes) {
                            String value = fieldNode.getText();
                            MetadataField metadataField = metadataValidator.checkMetadata(context,
                                    schema,
                                    element,
                                    null,
                                    createMissingMetadataFields);
                            itemService.addMetadata(context, item, metadataField, language, value);
                        }
                    }

                }
            }
        }
    }
}