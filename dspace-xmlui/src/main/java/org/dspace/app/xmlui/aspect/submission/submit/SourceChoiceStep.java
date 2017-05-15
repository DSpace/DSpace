/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.submission.submit;

import java.io.*;
import java.sql.*;
import java.util.*;
import org.dspace.app.xmlui.aspect.submission.*;
import org.dspace.app.xmlui.utils.*;
import org.dspace.app.xmlui.wing.*;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.authorize.*;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.factory.*;
import org.dspace.content.service.*;
import org.dspace.importer.external.service.*;
import org.dspace.submit.*;
import org.dspace.utils.*;
import org.xml.sax.*;

/**
 * @author lotte.hofstede at atmire.com
 */
public class SourceChoiceStep extends AbstractSubmissionStep {
    private static final Message T_title =
            message("xmlui.Submission.submit.SourceChoiceStep.title");
    private static final Message T_admin_help =
            message("xmlui.Submission.submit.SourceChoiceStep.admin_help");
    private Map<String, AbstractImportMetadataSourceService> sources = new DSpace().getServiceManager().getServiceByName("ImportServices", HashMap.class);
    protected static final Message T_skip =
            message("xmlui.Submission.general.submission.skip");
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();

    public List addReviewSection(List reviewList) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        return null;
    }

    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException {
                Collection collection = submission.getCollection();
        String actionURL = contextPath + "/handle/"+collection.getHandle() + "/submit/" + knot.getId() + ".continue";
        Division div = body.addInteractiveDivision("SourceChoiceStep", actionURL, Division.METHOD_POST, "primary submission");
        div.setHead(T_submission_head);
        addSubmissionProgressList(div);

        List form = div.addList("submit-lookup", List.TYPE_FORM);

        form.setHead(T_title);

        if (authorizeService.isAdmin(context) && sources.size() == 1) {
            Division notice = body.addDivision("general-message", "notice neutral");
            Para p = notice.addPara();
            p.addContent(T_admin_help);
        }

        Select select = form.addItem().addSelect("source", "ImportSourceSelect");

        for (Map.Entry<String, AbstractImportMetadataSourceService> source : sources.entrySet()) {
            select.addOption(source.getKey(), source.getValue().getName());
        }

        String lastSource = itemService.getMetadata(submission.getItem(), "workflow.import.source");
        select.setOptionSelected(lastSource);

        Division statusDivision = div.addDivision("statusDivision");
        List statusList = statusDivision.addList("statusList", List.TYPE_FORM);
        addControlButtons(statusList);
    }

    /**
     * Adds the "<-Previous", "Save/Cancel" and "Next->" buttons
     * to a given form.  This method ensures that the same
     * default control/paging buttons appear on each submission page.
     * <p>
     * Note: A given step may define its own buttons as necessary,
     * and not call this method (since it must be explicitly invoked by
     * the step's addBody() method)
     *
     * @param controls The List which will contain all control buttons
     */
    public void addControlButtons(List controls)
            throws WingException {
        org.dspace.app.xmlui.wing.element.Item actions = controls.addItem();

        // only have "<-Previous" button if not first step
        if (!isFirstStep()) {
            actions.addButton(AbstractProcessingStep.PREVIOUS_BUTTON).setValue(T_previous);
        }

        // always show "Save/Cancel"
        actions.addButton(AbstractProcessingStep.CANCEL_BUTTON).setValue(T_save);

        // If last step, show "Complete Submission"
        if (isLastStep()) {
            actions.addButton(AbstractProcessingStep.NEXT_BUTTON).setValue(T_complete);
        } else { // otherwise, show "Next->"
            actions.addButton(org.dspace.submit.step.SourceChoiceStep.CONDITIONAL_NEXT_IMPORT).setValue(T_next);
        }

        actions.addButton(AbstractProcessingStep.PROGRESS_BAR_PREFIX + new StepAndPage(getStep() + 2, 1)).setValue(T_skip);
    }

}
