package org.dspace.xmlworkflow.state.actions.processingaction.cristin;

import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.state.actions.processingaction.ProcessingAction;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;

/**
 * <p>Processing action to handle the assignment of an item to multiple collections</p>
 *
 * <p>This presents the user (via the related XmlUICollectionAssignmentUI) with a
 * full list of the collections in the reposiory, and options to check/un-check those
 * collections to which the item should be mapped.</p>
 *
 * <p><strong>Configuration</strong></p>
 *
 * <p>In the spring/api/workflow-actions.xml definition file add a new bean for this class:</p>
 *
 */
public class XmlUICollectionAssignment extends ProcessingAction {

    private static final CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();

    @Override
    public void activate(Context context, XmlWorkflowItem xmlWorkflowItem)
            throws SQLException, IOException, AuthorizeException {
        // no need to do anything here
    }

    @Override
    public ActionResult execute(Context context, XmlWorkflowItem wfi, Step step, HttpServletRequest request)
            throws SQLException, AuthorizeException, IOException {
        String button = Util.getSubmitButton(request, "submit_leave");

        if ("submit_save".equals(button)) {
            this.save(context, wfi, request);
        } else if ("submit_finished".equals(button)) {
            this.save(context, wfi, request);
            return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
        }

        return new ActionResult(ActionResult.TYPE.TYPE_CANCEL);
    }

    private void save(Context context, XmlWorkflowItem wfi, HttpServletRequest request)
            throws SQLException, AuthorizeException, IOException {
        // first get a list of the uuids to map to
        List<String> mapTo = new ArrayList<>();

        Enumeration e = request.getParameterNames();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            if (key.startsWith("mapped_collection_")) {
                mapTo.add(request.getParameter(key));
            }
        }

        // now pass through the item and add it to/remove it from the relevant collections
        Item item = wfi.getItem();
        List<Collection> collections = item.getCollections();
        Collection owner = wfi.getCollection();

        // remove it from existing collections (or record that the item is
        // already in a collection it is supposed to me in)
        List<String> alreadyIn = new ArrayList<>();
        for (Collection col : collections) {
            // if the collection's id is not in the mapTo list and is not the owning
            // collection then delete the item from that collection
            if (!mapTo.contains(col.getID().toString()) && col.getID() != owner.getID()) {
                collectionService.removeItem(context, col, item);
                collectionService.update(context, col);
            } else if (mapTo.contains(col.getID().toString())) {
                alreadyIn.add(col.getID().toString());
            }
        }

        // add the item to all the necessary collections that it is not
        // already added to
        for (String colID : mapTo) {
            // if it is already in the desired collection, just carry on
            if (alreadyIn.contains(colID)) {
                continue;
            }
            Collection col = collectionService.find(context, UUID.fromString(colID));
            collectionService.removeItem(context, col, item);
            collectionService.update(context, col);
        }
    }
}
