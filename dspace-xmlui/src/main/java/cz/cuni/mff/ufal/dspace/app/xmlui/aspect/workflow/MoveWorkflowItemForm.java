/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.workflow;

import java.sql.SQLException;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.content.Collection;
import org.dspace.core.Constants;
import org.dspace.workflow.WorkflowItem;


/**
 * This page displays collections to which the user can move a workflow item.
 *
 * @author Michal Josífko
 * modified for LINDAT/CLARIN
 */
public class MoveWorkflowItemForm extends AbstractDSpaceTransformer {

    /** Language strings */
    private static final Message T_dspace_home = message("xmlui.general.dspace_home");
    private static final Message T_workflow_title = message("xmlui.Submission.general.workflow.title");
    private static final Message T_submit_cancel = message("xmlui.general.cancel");

    private static final Message T_title = message("xmlui.administrative.item.MoveItemForm.title");
    private static final Message T_trail = message("xmlui.administrative.item.MoveItemForm.trail");
    private static final Message T_head1 = message("xmlui.administrative.item.MoveItemForm.head1");
    private static final Message T_collection = message("xmlui.administrative.item.MoveItemForm.collection");
    private static final Message T_collection_help = message("xmlui.administrative.item.MoveItemForm.collection_help");
    private static final Message T_collection_default = message("xmlui.administrative.item.MoveItemForm.collection_default");
    private static final Message T_submit_move = message("xmlui.administrative.item.MoveItemForm.submit_move");


    public void addPageMeta(PageMeta pageMeta) throws WingException, SQLException
    {
        pageMeta.addMetadata("title").addContent(T_title);

        String handle = parameters.getParameter("handle","");
        int workflowID = Integer.parseInt(parameters.getParameter("workflowID","").substring(1));
        String actionURL = contextPath + "/handle/"+ handle + "/workflow?workflowID="+workflowID;

        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(actionURL, T_workflow_title);
        pageMeta.addTrail().addContent(T_trail);
    }

    public void addBody(Body body) throws WingException, SQLException
    {
        // Get our parameters and state
        String handle = parameters.getParameter("handle","");
        int workflowID = Integer.parseInt(parameters.getParameter("workflowID","").substring(1));
        WorkflowItem wfi = WorkflowItem.find(context, workflowID);
        Collection workflowCollection = wfi.getCollection();
        String actionURL = contextPath + "/handle/"+ handle + "/workflow?workflowID="+workflowID;

        // DIVISION: Main
        Division main = body.addInteractiveDivision("move-item", actionURL, Division.METHOD_POST, "primary administrative item");
        main.setHead(T_head1);

        Collection[] collections = Collection.findAuthorized(context, null, Constants.ADD);

        List list = main.addList("select-collection", List.TYPE_FORM);
        Select select = list.addItem().addSelect("collectionID");
        select.setLabel(T_collection);
        select.setHelp(T_collection_help);

        if (workflowCollection == null) {
            select.addOption("",T_collection_default);
        }

        for (Collection collection : collections)
        {
            String name = collection.getMetadata("name");
            if (name.length() > 50)
            {
                name = name.substring(0, 47) + "...";
            }

            // Only add the item if it isn't already the owner
            if (collection.getID() != workflowCollection.getID())
            {
                select.addOption(collection.equals(workflowCollection), collection.getID(), name);
            }
        }

        org.dspace.app.xmlui.wing.element.Item actions = list.addItem();
        actions.addButton("submit_move").setValue(T_submit_move);
        actions.addButton("submit_cancel").setValue(T_submit_cancel);

        main.addHidden("submission-continue").setValue(knot.getId());
    }
}
