package org.dspace.app.xmlui.aspect.submission.workflow.actions.processingaction;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.aspect.submission.workflow.AbstractXMLUIAction;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.identifier.IdentifierNotFoundException;
import org.dspace.identifier.IdentifierNotResolvableException;
import org.dspace.utils.DSpace;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 18-aug-2010
 * Time: 10:42:33
 *
 * The user interface for the review stage
 */
public class DryadReviewActionXMLUI extends AbstractXMLUIAction {

    private static final Message T_BUTTON_DATAFILE_ADD = message("xmlui.Submission.submit.OverviewStep.button.add-datafile");
    private static final Message T_ADD_DATAFILE_help = message("xmlui.Submission.submit.OverviewStep.help.add-datafile");

    protected static final Message T_info1= message("xmlui.Submission.workflow.DryadReviewActionXMLUI.info1");

    protected static final Message T_cancel_submit = message("xmlui.general.cancel");
    private static final Message T_head_has_part = message("xmlui.ArtifactBrowser.ItemViewer.head_hasPart");


    @Override
     public void addBody(Body body) throws SAXException, WingException, SQLException, IOException, AuthorizeException {
        Item item = workflowItem.getItem();
        Collection collection = workflowItem.getCollection();
        Request request = ObjectModelHelper.getRequest(objectModel);

        String actionURL = contextPath + "/handle/"+collection.getHandle() + "/workflow_new";

        //Retrieve our pagenumber
        // Generate a from asking the user two questions: multiple
        // titles & published before.
    	Division div = body.addInteractiveDivision("perform-task", actionURL, Division.METHOD_POST, "primary workflow");
        //TODO: add to msgs.props
        div.setHead("Review submission");

        //addWorkflowItemInformation(div, item, request);

         // Add datafile list
        String showfull = request.getParameter("submit_full_item_info");

        // if the user selected showsimple, remove showfull.
        if (showfull != null && request.getParameter("submit_simple_item_info") != null)
            showfull = null;

        ReferenceSet referenceSet;
        if (showfull == null){
            referenceSet = div.addReferenceSet("collection-viewer", ReferenceSet.TYPE_DETAIL_VIEW);
        } else {
            referenceSet = div.addReferenceSet("collection-viewer", ReferenceSet.TYPE_SUMMARY_VIEW);
        }
        org.dspace.app.xmlui.wing.element.Reference itemRef = referenceSet.addReference(item);
        if (item.getMetadata("dc.relation.haspart").length > 0) {
            ReferenceSet hasParts;
            hasParts = itemRef.addReferenceSet("embeddedView", null, "hasPart");
            hasParts.setHead(T_head_has_part);

            for (Item obj : retrieveDataFiles(item)) {
                hasParts.addReference(obj);
            }
        }


        Table table = div.addTable("workflow-actions", 1, 1);
        table.setHead(T_info1);



        // Approve task
        Row row = table.addRow();
        row.addCellContent(T_ADD_DATAFILE_help);
        row.addCell().addButton("submit_adddataset").setValue(T_BUTTON_DATAFILE_ADD);

        // Everyone can just cancel
        row = table.addRow();
        row.addCell(0, 2).addButton("submit_leave").setValue(T_cancel_submit);


        div.addHidden("submission-continue").setValue(knot.getId());
    }

    private List<Item> retrieveDataFiles(Item item) throws SQLException {
        java.util.List<Item> dataFiles = new ArrayList<Item>();
        DOIIdentifierProvider dis = new DSpace().getSingletonService(DOIIdentifierProvider.class);

        if (item.getMetadata("dc.relation.haspart").length > 0) {

            for (DCValue value : item.getMetadata("dc.relation.haspart")) {

                DSpaceObject obj = null;
                try {
                    obj = dis.resolve(context, value.value);
                } catch (IdentifierNotFoundException e) {
                    // just keep going
                } catch (IdentifierNotResolvableException e) {
                    // just keep going
                }
                if (obj != null) dataFiles.add((Item) obj);
            }
        }
        return dataFiles;
    }
}
