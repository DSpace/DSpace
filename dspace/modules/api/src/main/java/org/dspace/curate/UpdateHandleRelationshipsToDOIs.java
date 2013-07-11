package org.dspace.curate;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;

/**
 *
 * @author dan.leehr@nescent.org
 */
@Distributive
public class UpdateHandleRelationshipsToDOIs extends AbstractCurationTask {

    private static Logger log = Logger.getLogger(UpdateHandleRelationshipsToDOIs.class);

    // used to detect handles in metadata fields
    private static final String[] HANDLE_PREFIXES = {
        "http://hdl.handle.net/10255/dryad",
        "http://datadryad.org/handle/10255"
    };

    // what to strip off before dereferencing handle
    private static final String[] HANDLE_PREFIXES_TO_STRIP = {
        "http://hdl.handle.net/",
        "http://datadryad.org/handle/"
    };

    private static final String DOI_PREFIX = "doi:";

    private static final String ITEM_ID_KEY = "item_id";
    private static final String[] RELATIONSHIP_MD_STRINGS = {
            "dc.relation.haspart",
            "dc.relation.ispartof"
    };
            
    private static final String RELATIONSHIP_KEY = "relationship";
    private static final String ORIGINAL_VALUE_KEY = "original_value";
    private static final String NEW_VALUE_KEY = "new_value";
    private static final String ORIGINAL_ELEMENT_KEY = "original_element";

    Map<Integer, Map> relationshipTable = new HashMap<Integer, Map>();

    // list of maps
    // [ item: <item_id>, relationship: <dc_haspart>, original_value: <http://hdl...>, new_value
    
    private Context dspaceContext;

    @Override
    public void init(Curator curator, String taskID) throws IOException{
        super.init(curator, taskID);
        // init dspace context to allow access to database
        try {
            dspaceContext = new Context();
        } catch (SQLException e1) {
            log.error("Exception instantiating Context", e1);
            return;
        }

    }

    private void formatResults() {
        StringBuilder sb = new StringBuilder();
        sb.append("Updating Items with relationships expressed as handles to DOIs: \n");
        sb.append("Item ID, Relationship, Handle, DOI \n");
        String orderedKeys[] = { ITEM_ID_KEY, RELATIONSHIP_KEY, ORIGINAL_VALUE_KEY, NEW_VALUE_KEY };
        for(Integer itemID : relationshipTable.keySet()) {
            Map<String, Object> map = relationshipTable.get(itemID);
            if(map != null) {
                for(String mdString : RELATIONSHIP_MD_STRINGS) {
                    List<Map<String, Object>> relationships = (List<Map<String, Object>>) map.get(mdString);
                    if(relationships != null) {
                        for(Map<String, Object> relationshipMap : relationships) {
                            Object values[] = new Object[relationshipMap.size()];
                            for(int i=0;i<orderedKeys.length;i++) {
                                values[i] = relationshipMap.get(orderedKeys[i]);
                            }
                            sb.append(StringUtils.join(values, ","));
                            sb.append("\n");
                        }
                    }
                }
            }
        }
        report(sb.toString());
    }

    @Override
    public int perform(DSpaceObject dso) throws IOException {
        log.info("Perform");
        relationshipTable.clear();
        distribute(dso);
        log.info("relationshipTable size: " + relationshipTable.size());
        formatResults();
        log.info("returning success");
        return Curator.CURATE_SUCCESS;
    }

    private void findHandleRelationships(Item item, String mdString) throws IOException {
        DCValue[] relationshipMetadataValues = item.getMetadata(mdString);
        for(DCValue relationshipValue : relationshipMetadataValues) {
            String handlePrefix = null;
            for(int i=0;i<HANDLE_PREFIXES.length;i++) {
                if(relationshipValue.value.startsWith(HANDLE_PREFIXES[i])) {
                    handlePrefix = HANDLE_PREFIXES_TO_STRIP[i];
                }
            }
            if(handlePrefix != null) {
                log.log(Level.INFO, "Found handle prefix in metadata value "
                        + relationshipValue.schema + "."
                        + relationshipValue.element + "."
                        + relationshipValue.qualifier + ": "
                        + relationshipValue.value);

                // this hasPartValue references a handle.
                // Figure out what item it is and update to a DOI
                String handleOnly = relationshipValue.value.substring(handlePrefix.length());
                DSpaceObject referencedObject = dereference(dspaceContext, handleOnly);
                if(referencedObject == null) {
                    log.error("Unable to find referenced object with handle: " + handleOnly);
                } else if(referencedObject.getType() == Constants.ITEM) {
                    // get the DOI identifier for referenced object if it is an item
                    Item referencedItem = (Item)referencedObject;
                    DCValue referencedItemMetadata[] = referencedItem.getMetadata("dc.identifier");
                    // find the DOI identifier
                    String referencedDOI = null;
                    for(DCValue identifierMetadataValue : referencedItemMetadata) {
                        if(identifierMetadataValue.value.startsWith(DOI_PREFIX)) {
                            referencedDOI = identifierMetadataValue.value;
                            break;
                        }
                    }
                    if(referencedDOI == null) {
                        log.log(Level.ERROR, "No DOI found for referenced item: " + referencedItem.getID());
                    } else {
                        log.log(Level.INFO, "Found DOI " + referencedDOI + " for referenced item:" + referencedItem.getID());
                        Map<String, Object> map = relationshipTable.get(item.getID());
                        if(map == null) {
                            map = new HashMap<String, Object>();
                        }
                        List<Map<String, Object>> relationships = (List<Map<String, Object>>) map.get(mdString);
                        if(relationships == null) {
                            relationships = new ArrayList<Map<String, Object> >();
                            map.put(mdString, relationships);
                        }
                        {
                            Map<String, Object> relationship = new HashMap<String, Object>();
                            relationship.put(ITEM_ID_KEY, item.getID());
                            relationship.put(RELATIONSHIP_KEY, relationshipValue.qualifier);
                            relationship.put(ORIGINAL_VALUE_KEY, relationshipValue.value);
                            relationship.put(NEW_VALUE_KEY, referencedDOI);
                            relationship.put(ORIGINAL_ELEMENT_KEY, relationshipValue);
                            relationships.add(relationship); // now back into map
                        }
                        relationshipTable.put(item.getID(), map);
                    }
                }
            }
        }
    }

    private void updateHandleRelationships(Item item, String mdString) {
        Map<String, Object> map = relationshipTable.get(item.getID());
        // Item may not have an entry in the map
        if(map != null) {
            DCValue[] metadata = item.getMetadata(mdString);
            List<Map<String, Object>> relationships = (List<Map<String, Object>>) map.get(mdString);
            if(relationships != null) {
                // have an array of original metadata
                for(int i=0;i<metadata.length;i++) {
                    DCValue dcValue = metadata[i];
                    // now find if this value is in the list of changes
                    for(Map<String, Object> relationshipMap : relationships) {
                        DCValue originalDcElement = (DCValue) relationshipMap.get(ORIGINAL_ELEMENT_KEY);
                        // Must have schema, element, qualifier, and matching value
                        if(originalDcElement != null &&
                                originalDcElement.schema.equals(dcValue.schema) &&
                                originalDcElement.element.equals(dcValue.element) &&
                                originalDcElement.qualifier.equals(dcValue.qualifier) &&
                                originalDcElement.value.equals(dcValue.value)) {
                            // same, update them
                            dcValue.value = (String) relationshipMap.get(NEW_VALUE_KEY);
                        }
                    }
                }
                String[] split = mdString.split("\\.");
                item.clearMetadata(split[0], split[1], split[2], Item.ANY);
                for(DCValue dcValue : metadata) {
                    item.addMetadata(dcValue.schema, dcValue.element, dcValue.qualifier, dcValue.language, dcValue.value);
                }
                try {
                    item.update();
                } catch (Exception ex) {
                    log.error("Exception updating item", ex);
                }
            }
        }
    }

    @Override
    protected void performItem(Item item) throws SQLException, IOException {
        // Check if the item has a handle prefix
        log.info("Updating handle relationships for item " + item.getID());
        for(String mdString : RELATIONSHIP_MD_STRINGS) {
            findHandleRelationships(item, mdString);
            updateHandleRelationships(item, mdString);
        }
    }

}
