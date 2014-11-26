package org.dspace.app.xmlui.aspect.submission.submit;

import org.dspace.app.xmlui.aspect.submission.AbstractStep;
import org.dspace.app.xmlui.aspect.submission.AbstractSubmissionStep;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.util.DCInputsReader;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.submit.AbstractProcessingStep;
import org.xml.sax.SAXException;

import javax.servlet.ServletException;
import java.sql.SQLException;
import java.io.IOException;

/**
 * User: @author kevinvandevelde (kevin at atmire.com)
 * Date: 29-jan-2010
 * Time: 16:17:03
 *
 * The describe metadata class for a data package
 */
public class DescribePublicationStep extends AbstractSubmissionStep {
    private static final Message T_HEAD = message("xmlui.submit.publication.describe.head");
    private static final Message T_TRAIL = message("xmlui.submit.publication.describe.trail");
    private static final Message T_HELP = message("xmlui.submit.publication.describe.help");
    private static final Message T_FORM_HEAD = message("xmlui.submit.publication.describe.form.help");
    private static final Message T_complete_dataset = message("xmlui.Submission.general.submission.complete.datapackage");
    private static final Message T_complete_publication = message("xmlui.Submission.general.submission.complete.publication");

    public List addReviewSection(List reviewList) throws SAXException, WingException, SQLException, IOException, AuthorizeException {
        //There is no review section.
        return null;
    }


    @Override
    public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException, SQLException, IOException, AuthorizeException {
        pageMeta.addMetadata("title").addContent("Dryad Submission");

        pageMeta.addTrailLink(contextPath + "/","Dryad Home");
        pageMeta.addTrail().addContent("Submission");
        pageMeta.addMetadata("stylesheet", "screen", "datatables", true).addContent("../../static/Datatables/DataTables-1.8.0/media/css/datatables.css");
//            pageMeta.addMetadata("stylesheet", "screen", null, true).addContent("../../themes/AtmireModules/lib/css/datatables-overrides.css");
        pageMeta.addMetadata("stylesheet", "screen", "person-lookup", true).addContent("lib/css/person-lookup.css");
        pageMeta.addMetadata("javascript", null, "person-lookup", true).addContent("lib/js/person-lookup.js");
        pageMeta.addMetadata("javascript", null, "dryad-submisison-reorder-edit", true).addContent("/lib/js/dryad-submisison-reorder-edit.js");
    }

    @Override
    public void addBody(Body body) throws SAXException, WingException, SQLException, IOException, AuthorizeException {
        org.dspace.content.Item item = submission.getItem();
        Collection collection = submission.getCollection();
        String actionURL = contextPath + "/handle/" + collection.getHandle() + "/submit/" + knot.getId() + ".continue";

        body.addDivision("step-link","step-link").addPara(T_TRAIL);

        Division helpDivision = body.addDivision("general-help","general-help");
        helpDivision.setHead(T_HEAD);
        helpDivision.addPara(T_HELP);

        Division div = body.addInteractiveDivision("submit-describe-publication", actionURL, Division.METHOD_MULTIPART, "primary submission");

        List pubList = div.addList("submit-select-publication", List.TYPE_FORM);
        pubList.setHead(T_FORM_HEAD);


        this.errorFields = DescribeStepUtils.renderFormList(context, contextPath,  pubList, getPage(), errorFields, submissionInfo, item, collection);

        //add standard control/paging buttons
        addControlButtons(pubList);
    }

    /**
     * Adds the "<-Previous", "Save/Cancel" and "Next->" buttons
     * to a given form.  This method ensures that the same
     * default control/paging buttons appear on each submission page.
     * <P>
     * Note: A given step may define its own buttons as necessary,
     * and not call this method (since it must be explicitly envoked by
     * the step's addBody() method)
     *
     * @param controls
     *          The List which will contain all control buttons
     */
    @Override
    public void addControlButtons(List controls)
        throws WingException
    {
        Item actions = controls.addItem();

        //only have "<-Previous" button if not first step
        if(!isFirstStep())
            // dec-2011: removed previous button
            //actions.addButton(AbstractProcessingStep.PREVIOUS_BUTTON).setValue(T_previous);

        //always show "Save/Cancel"
        actions.addButton(AbstractProcessingStep.CANCEL_BUTTON).setValue(T_save);

        //If last step, show "Complete Submission"
        if(isLastStep()){
            Button button = actions.addButton(AbstractProcessingStep.NEXT_BUTTON);
            //Only if we have a handle can we 
            if(submission.getItem().getHandle() == null)
                button.setValue(T_complete_dataset);
            else
                button.setValue(T_complete_publication);
        }
        else //otherwise, show "Next->"
            actions.addButton(AbstractProcessingStep.NEXT_BUTTON).setValue(T_next);
    }

}
