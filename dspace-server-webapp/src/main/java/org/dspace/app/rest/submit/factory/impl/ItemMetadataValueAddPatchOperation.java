/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.model.patch.LateObjectEvaluator;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipMetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

/**
 * Submission "add" PATCH operation.
 *
 * Path used to add a new value to an <b>existent metadata</b>:
 * "/sections/<:name-of-the-form>/<:metadata>/-"
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/submission/workspaceitems/<:id-workspaceitem> -H "
 * Content-Type: application/json" -d '[{ "op": "add", "path": "
 * /sections/traditionalpageone/dc.title/-", "value": {"value": "Add new
 * title"}}]'
 * </code>
 *
 * Path used to insert the new metadata value in a <b>specific position</b>:
 * "/sections/<:name-of-the-form>/<:metadata>/<:idx-zero-based>"
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/submission/workspaceitems/<:id-workspaceitem> -H "
 * Content-Type: application/json" -d '[{ "op": "add", "path": "
 * /sections/traditionalpageone/dc.title/1", "value": {"value": "Add new
 * title"}}]'
 * </code>
 *
 * Path used to <b>initialize or replace</b> the whole metadata values:
 * "/sections/<:name-of-the-form>/<:metadata>"
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/submission/workspaceitems/<:id-workspaceitem> -H "
 * Content-Type: application/json" -d '[{ "op": "add", "path": "
 * /sections/traditionalpageone/dc.title", "value": [{"value": "Add new first
 * title"}, {"value": "Add new second title"}]}]'
 * </code>
 *
 * Please note that according to the JSON Patch specification RFC6902 to
 * initialize a new metadata in the section the add operation must receive an
 * array of values and it is not possible to add a single value to the not yet
 * initialized "/sections/<:name-of-the-form>/<:metadata>/-" path.
 *
 * NOTE: If the target location specifies an object member that does exist, that
 * member's value is replaced.
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public class ItemMetadataValueAddPatchOperation extends MetadataValueAddPatchOperation<Item> {

    /**
     * log4j category
     */
    private static final Logger log =
            org.apache.logging.log4j.LogManager.getLogger(ItemMetadataValueAddPatchOperation.class);

    @Autowired
    ItemService itemService;

    @Autowired
    RelationshipService relationshipService;

    @Override
    void add(Context context, HttpServletRequest currentRequest, InProgressSubmission source, String path, Object value)
            throws SQLException {
        String[] split = getAbsolutePath(path).split("/");
        // if split size is one so we have a call to initialize or replace
        if (split.length == 1) {
            List<MetadataValueRest> list = evaluateArrayObject((LateObjectEvaluator) value);
            replaceValue(context, source.getItem(), split[0], list);

        } else {
            // call with "-" or "index-based" we should receive only single
            // object member
            MetadataValueRest object = evaluateSingleObject((LateObjectEvaluator) value);
            // check if is not empty
            List<MetadataValue> metadataByMetadataString = itemService.getMetadataByMetadataString(source.getItem(),
                                                                                                   split[0]);
            Assert.notEmpty(metadataByMetadataString);
            if (split.length > 1) {
                String controlChar = split[1];
                switch (controlChar) {
                    case "-":
                        addValue(context, source.getItem(), split[0], object, -1);
                        break;
                    default:
                        // index based

                        int index = Integer.parseInt(controlChar);
                        if (index > metadataByMetadataString.size()) {
                            throw new IllegalArgumentException(
                                "The specified index MUST NOT be greater than the number of elements in the array");
                        }
                        addValue(context, source.getItem(), split[0], object, index);

                        break;
                }
            }
        }

    }

    protected void replaceValue(Context context, Item source, String target, List<MetadataValueRest> list)
            throws SQLException {
        String[] metadata = Utils.tokenize(target);

        // fetch pre-existent metadata
        List<MetadataValue> preExistentMetadata =
                getDSpaceObjectService().getMetadata(source, metadata[0], metadata[1], metadata[2], Item.ANY);

        // fetch pre-existent relationships
        Map<Integer, Relationship> preExistentRelationships = preExistentRelationships(context, preExistentMetadata);

        // clear all plain metadata
        getDSpaceObjectService().clearMetadata(context, source, metadata[0], metadata[1], metadata[2], Item.ANY);
        // remove all deleted relationships
        for (Relationship rel : preExistentRelationships.values()) {
            try {
                Optional<MetadataValueRest> stillPresent = list.stream()
                    .filter(ll -> ll.getAuthority() != null &&  rel.getID().equals(getRelId(ll.getAuthority())))
                    .findAny();
                if (stillPresent.isEmpty()) {
                    relationshipService.delete(context, rel);
                }
            } catch (AuthorizeException e) {
                e.printStackTrace();
                throw new RuntimeException("Authorize Exception during relationship deletion.");
            }
        }

        // create plain metadata / move relationships in the list order

        // if a virtual value is present in the list, it must be present in preExistentRelationships too.
        // (with this operator virtual value can only be moved or deleted).
        int idx = 0;
        for (MetadataValueRest ll : list) {
            if (StringUtils.startsWith(ll.getAuthority(), Constants.VIRTUAL_AUTHORITY_PREFIX)) {

                Optional<MetadataValue> preExistentMv = preExistentMetadata.stream().filter(mvr ->
                    StringUtils.equals(ll.getAuthority(), mvr.getAuthority())).findFirst();

                if (!preExistentMv.isPresent()) {
                    throw new UnprocessableEntityException(
                            "Relationship with authority=" + ll.getAuthority() + " not found");
                }

                final RelationshipMetadataValue rmv = (RelationshipMetadataValue) preExistentMv.get();
                final Relationship rel = preExistentRelationships.get(rmv.getRelationshipId());
                this.updateRelationshipPlace(context, source, idx, rel);

            } else {
                getDSpaceObjectService()
                        .addMetadata(context, source, metadata[0], metadata[1], metadata[2],
                                ll.getLanguage(), ll.getValue(), ll.getAuthority(), ll.getConfidence(), idx);
            }
            idx++;
        }
    }

    /**
     * Retrieve Relationship Objects from a List of MetadataValue.
     */
    private Map<Integer, Relationship> preExistentRelationships(Context context,
            List<MetadataValue> preExistentMetadata) throws SQLException {
        Map<Integer, Relationship> relationshipsMap = new HashMap<Integer, Relationship>();
        for (MetadataValue ll : preExistentMetadata) {
            if (ll instanceof RelationshipMetadataValue) {
                Relationship relationship = relationshipService
                        .find(context, ((RelationshipMetadataValue) ll).getRelationshipId());
                if (relationship != null) {
                    relationshipsMap.put(relationship.getID(), relationship);
                }
            }
        }
        return relationshipsMap;
    }

    private Integer getRelId(String authority) {
        final int relId = Integer.parseInt(authority.split(Constants.VIRTUAL_AUTHORITY_PREFIX)[1]);
        return relId;
    }

    private void updateRelationshipPlace(Context context, Item dso, int place, Relationship rs) {

        try {
            if (rs.getLeftItem().equals(dso)) {
                rs.setLeftPlace(place);
            } else {
                rs.setRightPlace(place);
            }
            relationshipService.update(context, rs);
        } catch (Exception e) {
            //should not occur, otherwise metadata can't be updated either
            log.error("An error occurred while moving " + rs.getID() + " for item " + dso.getID(), e);
        }

    }

    @Override
    protected ItemService getDSpaceObjectService() {
        return itemService;
    }
}
