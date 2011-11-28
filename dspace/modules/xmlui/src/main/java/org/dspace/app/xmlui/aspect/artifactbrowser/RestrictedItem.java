package org.dspace.app.xmlui.aspect.artifactbrowser;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.Item;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.workflow.*;
import org.dspace.workflow.actions.WorkflowActionConfig;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: fabio.bolognesi
 * Date: 9/8/11
 * Time: 2:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class RestrictedItem extends AbstractDSpaceTransformer //implements CacheableProcessingComponent
{

    private static final Logger log = Logger.getLogger(RestrictedItem.class);
    /**
     * language strings
     */
    private static final Message T_title =
            message("xmlui.ArtifactBrowser.RestrictedItem.title");

    private static final Message T_dspace_home =
            message("xmlui.general.dspace_home");

    private static final Message T_trail =
            message("xmlui.ArtifactBrowser.RestrictedItem.trail");

    private static final Message T_head_resource =
            message("xmlui.ArtifactBrowser.RestrictedItem.head_resource");

    private static final Message T_head_community =
            message("xmlui.ArtifactBrowser.RestrictedItem.head_community");

    private static final Message T_head_collection =
            message("xmlui.ArtifactBrowser.RestrictedItem.head_collection");

    private static final Message T_head_item =
            message("xmlui.ArtifactBrowser.RestrictedItem.head_item");

    private static final Message T_head_bitstream =
            message("xmlui.ArtifactBrowser.RestrictedItem.head_bitstream");

    private static final Message T_para_resource =
            message("xmlui.ArtifactBrowser.RestrictedItem.para_resource");

    private static final Message T_para_community =
            message("xmlui.ArtifactBrowser.RestrictedItem.para_community");

    private static final Message T_para_collection =
            message("xmlui.ArtifactBrowser.RestrictedItem.para_collection");

    private static final Message T_para_item =
            message("xmlui.ArtifactBrowser.RestrictedItem.para_item");

    private static final Message T_para_bitstream =
            message("xmlui.ArtifactBrowser.RestrictedItem.para_bitstream");

    // Item states
    private static final Message T_para_item_in_archive =message("xmlui.ArtifactBrowser.RestrictedItem.para_item_in_archive");
    private static final Message T_para_item_in_submission =message("xmlui.ArtifactBrowser.RestrictedItem.para_item_in_submission");
    private static final Message T_para_item_in_workflow =message("xmlui.ArtifactBrowser.RestrictedItem.para_item_in_workflow");
    private static final Message T_para_item_withdrawn =message("xmlui.ArtifactBrowser.RestrictedItem.para_item_withdrawn");

    private static final Message T_email =message("xmlui.ArtifactBrowser.RestrictedItem.email");
    private static final Message T_email_help =message("xmlui.ArtifactBrowser.RestrictedItem.email_help");
    private static final Message T_comments = message("xmlui.ArtifactBrowser.RestrictedItem.comments");
    private static final Message T_submit =message("xmlui.ArtifactBrowser.RestrictedItem.submit");
    private static final Message T_head_item_new =message("xmlui.ArtifactBrowser.RestrictedItem.head_item_new");

    private static final Message T_head_item_questions =message("xmlui.ArtifactBrowser.RestrictedItem.questions");







    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException {
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

        pageMeta.addMetadata("title").addContent(T_head_item_new);

        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        if (dso != null)
            HandleUtil.buildHandleTrail(dso, pageMeta, contextPath);
        pageMeta.addTrail().addContent(T_trail);

    }


    public void addBody(Body body) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        Request request = ObjectModelHelper.getRequest(objectModel);
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

        if (dso == null) {
            Division unauthorized = body.addDivision("unauthorized-resource", "primary");
            unauthorized.setHead(T_head_resource);
            unauthorized.addPara(T_para_resource);
        } else if (dso instanceof Community) {
            Community community = (Community) dso;
            Division unauthorized = body.addDivision("unauthorized-resource", "primary");
            unauthorized.setHead(T_head_community);
            unauthorized.addPara(T_para_community.parameterize(community.getMetadata("name")));
        } else if (dso instanceof Collection) {
            Collection collection = (Collection) dso;
            Division unauthorized = body.addDivision("unauthorized-resource", "primary");
            unauthorized.setHead(T_head_collection);
            unauthorized.addPara(T_para_collection.parameterize(collection.getMetadata("name")));
        } else if (dso instanceof Item) {
            // The dso may be an item but it could still be an item's bitstream. So let's check for the parameter.
            if (request.getParameter("bitstreamId") != null) {
                String identifier = "unknown";

                try {
                    Bitstream bit = Bitstream.find(context, new Integer(request.getParameter("bitstreamId")));
                    if (bit != null) {
                        identifier = bit.getName();
                    }
                } catch (Exception e) {
                    // just forget it - and display the restricted message.
                    log.trace("Caught exception", e);
                }
                Division unauthorized = body.addDivision("unauthorized-resource", "primary");
                unauthorized.setHead(T_head_bitstream);
                unauthorized.addPara(T_para_bitstream.parameterize(identifier));
            }
            else {
                String identifier = "unknown";
                String handle = dso.getHandle();

                // display DOI || handle || ID
                String   doi = DOIIdentifierProvider.getDoiValue((Item)dso);
                if(doi!=null && !"".equals(doi)){
                    identifier = ".Identifier: " + doi;
                }
                else if (handle == null || "".equals(handle)) {
                    identifier = ".Internal ID: " + dso.getID();
                } else {
                    identifier = ".Hdl:" + handle;
                }

                Division unauthorized = body.addDivision(getStateDescription((Item) dso), "primary");
                unauthorized.setHead(T_head_item_new);
                unauthorized.addPara(T_para_item.parameterize(identifier));

                Message status = getState((Item) dso);
                unauthorized.addPara("item_status",status.getKey()).addContent(status);

                Division feedback = unauthorized.addInteractiveDivision("feedback-form", contextPath+"/feedback",Division.METHOD_POST,"primary");
                // create feedback form
                org.dspace.app.xmlui.wing.element.List form = feedback.addList("form", org.dspace.app.xmlui.wing.element.List.TYPE_FORM);


                form.addItem().addContent(T_head_item_questions);

                Text email = form.addItem().addText("email");
                email.setLabel(T_email);
                email.setHelp(T_email_help);
                email.setValue(parameters.getParameter("email",""));


                TextArea comments = form.addItem().addTextArea("comments");
                comments.setLabel(T_comments);
                comments.setValue(parameters.getParameter("comments",""));

                form.addItem().addButton("submit").setValue(T_submit);

                feedback.addHidden("page").setValue(parameters.getParameter("page","unknown"));

            }
        } else {
            // This case should not occure, but if it does just fall back to the resource message.
            Division unauthorized = body.addDivision("unauthorized-resource", "primary");
            unauthorized.setHead(T_head_resource);
            unauthorized.addPara(T_para_resource);
        }

    }

    private Message getState(Item item) throws AuthorizeException, IOException, SQLException {

        if (((Item)item).isWithdrawn())
                return T_para_item_withdrawn;

            WorkspaceItem wsi = WorkspaceItem.findByItemId(context, item.getID());
            if (wsi != null) return T_para_item_in_submission;

            WorkflowItem wfi = WorkflowItem.findByItemId(context, item.getID());
            if (wfi != null) {
                return T_para_item_in_workflow;
            }

            return T_para_item_in_archive;

    }

    private String getStateDescription(Item item) throws AuthorizeException, IOException, SQLException {

        if (((Item)item).isWithdrawn())
                return "withdrawn";

            WorkspaceItem wsi = WorkspaceItem.findByItemId(context, item.getID());
            if (wsi != null) return "submission";

            WorkflowItem wfi = WorkflowItem.findByItemId(context, item.getID());
            if (wfi != null) {

                List<PoolTask> tasks = PoolTask.find(context, wfi);
                String step = null;
                if (tasks != null && tasks.size() > 0)
                    step = tasks.get(0).getStepID();
                if (step != null)
                    return "workflow_" + step;

                return "workflow";
            }

            return "archive";

    }
}
