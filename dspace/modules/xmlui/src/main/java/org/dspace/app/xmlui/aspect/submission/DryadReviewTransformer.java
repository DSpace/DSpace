package org.dspace.app.xmlui.aspect.submission;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
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
import org.dspace.workflow.DryadWorkflowUtils;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowRequirementsManager;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.dspace.workflow.ClaimedTask;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 18-aug-2010
 * Time: 11:27:27
 * <p/>
 * The user interface for the review step
 */
public class DryadReviewTransformer extends AbstractDSpaceTransformer {

    private static final Logger log = Logger.getLogger(DryadReviewTransformer.class);

    protected static final Message T_showfull = message("xmlui.Submission.general.showfull");
    protected static final Message T_showsimple = message("xmlui.Submission.general.showsimple");
    protected static final Message T_workflow_head = message("xmlui.Submission.general.workflow.head");
    protected static final Message T_workflow_trail = message("xmlui.Submission.general.workflow.trail");
    protected static final Message T_dspace_home = message("xmlui.general.dspace_home");
    protected static final Message T_workflow_title = message("xmlui.Submission.general.workflow.title");
    private static final Message T_head_has_part = message("xmlui.ArtifactBrowser.ItemViewer.head_hasPart");
    private static final Message T_in_workflow = message("xmlui.DryadItemSummary.in_workflow");


    private WorkflowItem wfItem;
    private boolean authorized;
    private boolean currentlyInReview;
    List<Item> dataFiles = new ArrayList<Item>();

    @Override
    public void setup(SourceResolver resolver, Map objectModel, String src, Parameters parameters) throws ProcessingException, SAXException, IOException {
        super.setup(resolver, objectModel, src, parameters);
        authorized = false;
        currentlyInReview = false;

        Request request = ObjectModelHelper.getRequest(objectModel);

        // Reviewers may access with either
        // 1. wfID + token
        // 2. provisional DOI (since it is not yet public)

        String requestDoi = request.getParameter("doi");
        if(requestDoi != null) {
            loadWFItemByDOI(requestDoi);
            if(wfItem == null) {
                // Not found
                return;
            }
            // DOI was found. Set authorized true and the reviewerToken for downloads
            authorized = true;
            String reviewerKey = getItemToken();
            request.getSession().setAttribute("reviewerToken", reviewerKey);
        } else {
            // DOI not present, require token
            String token = request.getParameter("token");
            if (token != null) {
                loadWFItem(request); // Looks up by wfID or itemId
                if(wfItem == null) {
                    // item not found
                    return;
                }
                // item lookup successful, make sure token matches
                String reviewerKey = getItemToken();
                authorized = token.equals(reviewerKey);
                if (authorized) {
                    request.getSession().setAttribute("reviewerToken", token);
                }
            }
        }

        // Check if the item is actually in review
        // taskowner has step_id=reviewStep, action_id=reviewAction, owner_id=submitter's eperson ID
        try {
            List<ClaimedTask> tasks = ClaimedTask.findByWorkflowId(context, wfItem.getID());
            // find a task with reviewStep
            for(ClaimedTask task : tasks) {
                if(task.getStepID().equals("reviewStep")) {
                    currentlyInReview = true;
                }
            }
        } catch (SQLException ex) {
            log.error("Exception checking for claimed task with reviewStep", ex);
            return;
        }
    }


    @Override
    public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        super.addPageMeta(pageMeta);

        if (wfItem != null) {

            String title = "";
            DCValue[] vals = wfItem.getItem().getMetadata("dc.title");
            if (vals != null && vals[0] != null)
                title = vals[0].value;

            pageMeta.addMetadata("title").addContent("Reviewing " + title);
            Collection collection = wfItem.getCollection();

            pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
            HandleUtil.buildHandleTrail(collection, pageMeta, contextPath);
            pageMeta.addTrail().addContent(T_workflow_trail);

            Item dataPackage = DryadWorkflowUtils.getDataPackage(context, wfItem.getItem());
            if (dataPackage != null) {
                //We have a data package, indicating that we are viewing a data file
                //Add the review key to the page meta so we can use that
                DCValue[] reviewerKeys = dataPackage.getMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA, "step", "reviewerKey", Item.ANY);
                if (0 < reviewerKeys.length)
                    pageMeta.addMetadata("identifier", "reviewerKey").addContent(reviewerKeys[0].value);
            }
        }
    }

    @Override
    public void addBody(Body body) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        if (!authorized) {
            throw new AuthorizeException("You are not authorized to review the submission");
        }
        if (!currentlyInReview) {
            throw new AuthorizeException("The submission is not currently in review");
        }
        Request request = ObjectModelHelper.getRequest(objectModel);

        Division div = body.addInteractiveDivision("main-div", contextPath + "/review", Division.METHOD_POST, "");

        // Adding message for withdrawn or workflow item
        addWarningMessage(wfItem.getItem(), div);

        //Add an overview of the item in question
        String showfull = request.getParameter("submit_full_item_info");

        // if the user selected showsimple, remove showfull.
        if (showfull != null && request.getParameter("submit_simple_item_info") != null)
            showfull = null;


        DCValue[] vals = wfItem.getItem().getMetadata("dc.title");
        ReferenceSet referenceSet = null;

        if (showfull == null) {
            referenceSet = div.addReferenceSet("narf", ReferenceSet.TYPE_SUMMARY_VIEW);
            if (vals != null && vals[0] != null)
                referenceSet.setHead(vals[0].value);
            else
                referenceSet.setHead(T_workflow_head);
            div.addPara().addButton("submit_full_item_info").setValue(T_showfull);
        } else {
            referenceSet = div.addReferenceSet("narf", ReferenceSet.TYPE_DETAIL_VIEW);
            if (vals != null && vals[0] != null)
                referenceSet.setHead(vals[0].value);
            else
                referenceSet.setHead(T_workflow_head);
            div.addPara().addButton("submit_simple_item_info").setValue(T_showsimple);

            div.addHidden("submit_full_item_info").setValue("true");
        }



        // adding the dataFile
        org.dspace.app.xmlui.wing.element.Reference itemRef = referenceSet.addReference(wfItem.getItem());
        if (wfItem.getItem().getMetadata("dc.relation.haspart").length > 0) {
            ReferenceSet hasParts;
            hasParts = itemRef.addReferenceSet("embeddedView", null, "hasPart");
            hasParts.setHead(T_head_has_part);

            if (dataFiles.size() == 0) retrieveDataFiles(wfItem.getItem());

            for (Item obj : dataFiles) {
                hasParts.addReference(obj);
            }
        }

        div.addHidden("token").setValue(request.getParameter("token"));
        div.addHidden("wfID").setValue(String.valueOf(wfItem.getID()));
    }

    private void loadWFItemByDOI(String doi) throws IOException {
        wfItem = null;
        DOIIdentifierProvider dis = new DSpace().getSingletonService(DOIIdentifierProvider.class);
        try {
            DSpaceObject obj = dis.resolve(context, doi);
            if (obj instanceof Item) {
                wfItem = WorkflowItem.findByItemId(context, obj.getID());
            }
        } catch (IdentifierNotFoundException e) {
            log.error(e);
        } catch (IdentifierNotResolvableException e) {
            log.error(e);
        } catch (SQLException e) {
            log.error(e);
        } catch (AuthorizeException e) {
            log.error(e);
        }
    }

    private void
    loadWFItem(Request request) throws IOException {
        int wfItemId;
        wfItem = null;
        try {
            if (request.getParameter("wfID") != null) {
                wfItemId = Integer.parseInt(request.getParameter("wfID"));
                wfItem = WorkflowItem.find(context, wfItemId);
            }
            else if (request.getParameter("itemID") != null) {
                Item item = Item.find(context, Integer.parseInt(request.getParameter("itemID")));
                wfItem = WorkflowItem.findByItemId(context, item.getID());
            }
        } catch (SQLException e) {
            log.error(e);
        } catch (AuthorizeException e) {
            log.error(e);
        }
    }

    private String getItemToken() {
        Item datapackage = wfItem.getItem();
        //Check for a data file
        if (DryadWorkflowUtils.getDataPackage(context, wfItem.getItem()) != null) {
            //We have a data file, get our data package so we can check its reviewer key
            datapackage = DryadWorkflowUtils.getDataPackage(context, wfItem.getItem());
        }
        DCValue[] values = datapackage.getMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA, "step", "reviewerKey", Item.ANY);
        if (values!=null && values.length > 0)
            return values[0].value;

        return null;
    }


    private void retrieveDataFiles(Item item) throws SQLException {
        DOIIdentifierProvider dis = new DSpace().getSingletonService(DOIIdentifierProvider.class);

        if (item.getMetadata("dc.relation.haspart").length > 0) {
            dataFiles = new ArrayList<Item>();
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
    }


    private void addWarningMessage(Item item, Division division) throws WingException, SQLException, AuthorizeException, IOException {

        log.warn("InternalItemTransformer - addWarningMessage");

        WorkflowItem wfi = WorkflowItem.findByItemId(context, item.getID());

        log.warn("InternalItemTransformer - addWarningMessage() wfi: " + wfi);

        if (wfi != null) {
            DCValue[] values = item.getMetadata("workflow.step.reviewerKey");

            log.warn("InternalItemTransformer - addWarningMessage() values: " + values);

            if(values!=null && values.length > 0){
                addMessage(division, T_in_workflow, null, null);
            }
        }
    }

    private void addMessage(Division main, Message message, String link, Message linkMessage) throws WingException {
        Division div = main.addDivision("notice", "notice");
        Para p = div.addPara();
        p.addContent(message);
        if (link != null)  //avoid adding worthless links to "/"
	        p.addXref(link, linkMessage);

    }


    /**
     * recycle
     */
    public void recycle()
    {
        this.wfItem = null;
	this.authorized = false;
	this.dataFiles=new ArrayList<Item>();
        super.recycle();
    }

}
