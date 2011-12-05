package org.dspace.app.xmlui.aspect.submission.submit;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.aspect.submission.AbstractSubmissionStep;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.*;
import org.dspace.handle.HandleManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.submit.AbstractProcessingStep;
import org.xml.sax.SAXException;

import java.sql.SQLException;
import java.io.IOException;
import java.util.ArrayList;

/**
 * User: @author kevinvandevelde (kevin at atmire.com)
 * Date: 27-jan-2010
 * Time: 15:07:05
 * XMLUI interface for a step that allows to search and select a publication
 */
public class SelectPublicationStep extends AbstractSubmissionStep {
    private static final Message T_HEAD = message("xmlui.submit.select.pub.head");
    private static final Message T_FORM_HEAD = message("xmlui.submit.select.pub.form_head");
    private static final Message T_PUB_HELP = message("xmlui.submit.select.pub.help");
    private static final Message T_PUB_HELP_NEW = message("xmlui.submit.select.pub.help_new");
    private static final Message T_PUB_SELECT_NEW = message("xmlui.submit.select.pub.form.option.new");
    private static final Message T_PUB_SELECT_EXISTING = message("xmlui.submit.select.pub.form.option.existing");
    private static final Message T_PUB_SELECT_HELP = message("xmlui.submit.select.pub.form.help");
    private static final Message T_PUB_SELECT_ERROR = message("xmlui.submit.select.pub.form.error");
    private static final Message T_PUB_LICENSE = message("xmlui.submit.select.pub.form.license");
    private static final Message T_PUB_LICENSE_ERROR = message("xmlui.submit.select.pub.form.license_error");
    private static final Message T_SELECT_LABEL = message("xmlui.submit.publication.journal.select.label");
    private static final Message T_SELECT_ERROR = message("xmlui.submit.publication.journal.select.error");
    private static final Message T_SELECT_HELP = message("xmlui.submit.publication.journal.select.help");
    private static final Message T_MANU_LABEL = message("xmlui.submit.publication.journal.manu.label");
    private static final Message T_MANU_HELP = message("xmlui.submit.publication.journal.manu.help");

    private static final Message T_PUB_MANU_ERROR = message("xmlui.submit.select.pub.form.manu_error");



    private static final Message T_MANU_ACC_LABEL = message("xmlui.submit.publication.journal.manu.acc.label");

    private static final Message T_SELECT_HELP_NOT_YET_SUBMITTED = message("xmlui.submit.publication.journal.help_not_yet_submitted");


    private static final Message T_article_status = message("xmlui.submit.publication.article_status");
    private static final Message T_article_status_help = message("xmlui.submit.publication.article_status_help");

    private static final Message T_article_status_published = message("xmlui_submit_publication_article_status_published");
    private static final Message T_article_status_accepted = message("xmlui_submit_publication_article_status_accepted");
    private static final Message T_article_status_in_review = message("xmlui_submit_publication_article_status_in_review");
    private static final Message T_article_status_not_yet_submitted = message("xmlui_submit_publication_article_status_not_yet_submitted");

    private static final Message T_enter_article_doi = message("xmlui.submit.publication.enter_article_doi");
    private static final Message T_unknown_doi = message("xmlui.submit.publication.unknown_doi");


    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, SQLException, IOException,
            AuthorizeException
    {
        pageMeta.addMetadata("title").addContent("Dryad Submission");

        pageMeta.addTrailLink(contextPath + "/","Dryad Home");
        pageMeta.addTrail().addContent("Submission");
    }


    public void addBody(Body body) throws SAXException, WingException, SQLException, IOException, AuthorizeException
    {
        Request request = ObjectModelHelper.getRequest(objectModel);
        Collection collection = submission.getCollection();
        String actionURL = contextPath + "/handle/"+collection.getHandle() + "/submit/" + knot.getId() + ".continue";

        Division div = body.addInteractiveDivision("submit-select-publication", actionURL, Division.METHOD_POST, "primary submission");
        addSubmissionProgressList(div);
        div.setHead(T_HEAD);

        List form = div.addList("submit-create-publication", List.TYPE_FORM);
        form.setHead(T_FORM_HEAD);
        Item content = form.addItem();
        boolean submitExisting = ConfigurationManager.getBooleanProperty("submit.dataset.existing-datasets", true);
        if(submitExisting)
            content.addContent(T_PUB_HELP);
        else
            content.addContent(T_PUB_HELP_NEW);


        addArticleStatusRadios(request, form);

        // case A (published selected): this fields will be displayed.
        addDOIField(form);

        //Get all the data required
        boolean pubIdError = this.errorFlag == org.dspace.submit.step.SelectPublicationStep.STATUS_INVALID_PUBLICATION_ID;
        Collection pubColl = (Collection) HandleManager.resolveToObject(context, ConfigurationManager.getProperty("submit.publications.collection"));
        String selectedJournalId = request.getParameter("journalID");


        //Start rendering our info
        Item newItem = form.addItem("select_publication_new", submitExisting ? "" : "odd");

        addRadioIfSubmitExisitng(content, submitExisting, pubIdError, pubColl, newItem);

        addJournalSelect(selectedJournalId, newItem);
        addJournalSelectStatusNotYetSubmitted(selectedJournalId, newItem);


        addManuscriptNumber(request, newItem);

        // add only if: status=accepted and journalID=integratedJournal
        addManuscriptNumberStatusAccepted(request, form);

        // add only if: status=accepted and journalID=!integratedJournal
        addManuscriptAcceptance(request, form);


        addPublicationNumberIfSubmitExisting(form, submitExisting, pubIdError, pubColl);


        addLicence(form);

        //add standard control/paging buttons
        addControlButtons(form);
    }

    private void addPublicationNumberIfSubmitExisting(List form, boolean submitExisting, boolean pubIdError, Collection pubColl) throws WingException, SQLException {
        if(submitExisting){
            Item existItem = form.addItem("select_publication_exist", "");

            Radio publicationSelectRadio = existItem.addRadio("publication_select");

            //Make sure that the current user can submit in the publication collection
            if (AuthorizeManager.authorizeActionBoolean(context, pubColl, Constants.ADD))
            {
                publicationSelectRadio.addOption(pubIdError, "select").addContent(T_PUB_SELECT_EXISTING);

            } else {
                //We cannot create one so just disable it
                publicationSelectRadio.addOption(true, "select").addContent(T_PUB_SELECT_EXISTING);
            }

            Text publicationNr = existItem.addComposite("exits-options-comp").addText("publication_number");
            publicationNr.setLabel("Data package id");
            publicationNr.setHelp(T_PUB_SELECT_HELP);
            if(pubIdError)
                publicationNr.addError(T_PUB_SELECT_ERROR);
        }
    }

    private void addRadioIfSubmitExisitng(Item content, boolean submitExisting, boolean pubIdError, Collection pubColl, Item newItem) throws WingException, SQLException {
        if(submitExisting){
            //We need to add a radio
            Radio radio = newItem.addRadio("publication_select");
            if (AuthorizeManager.authorizeActionBoolean(context, pubColl, Constants.ADD))
            {
                radio.addOption(!pubIdError, "create").addContent(T_PUB_SELECT_NEW);
            }else{
                //We cannot create one so just disable it
                Option createOption = radio.addOption(false, "create");
                createOption.addContent(T_PUB_SELECT_NEW);
                radio.setDisabled(true);
                radio.addOption(true, "select").addContent(T_PUB_SELECT_EXISTING);
            }
        }else{
            //We cannot add a new data set so just add a hidden field to indicate that we want to create a new one
            content.addHidden("publication_select").setValue("create");
        }
    }

    private void addLicence(List form) throws WingException {
        CheckBox licensebox = form.addItem().addCheckBox("license_accept");
        licensebox.addOption(String.valueOf(Boolean.TRUE), T_PUB_LICENSE);
        if(this.errorFlag == org.dspace.submit.step.SelectPublicationStep.STATUS_LICENSE_NOT_ACCEPTED)
            licensebox.addError(T_PUB_LICENSE_ERROR);
    }

    private void addDOIField(List form) throws WingException {
        List doi = form.addList("doi");
        Text textArticleDOI = doi.addItem().addText("article_doi");
        textArticleDOI.setLabel(T_enter_article_doi);
        doi.addItem().addContent("OR");
        CheckBox cb = doi.addItem().addCheckBox("unknown_doi");
        cb.addOption(String.valueOf(Boolean.TRUE), T_unknown_doi);
    }

    private void addArticleStatusRadios(Request request, List form) throws WingException {
        // add "article status" radios
        Item articleStatus = form.addItem("article_status", "");
        Radio accessRadios = articleStatus.addRadio("article_status");
        accessRadios.setLabel(T_article_status);
        accessRadios.setHelp(T_article_status_help);
        accessRadios.addOption(org.dspace.submit.step.SelectPublicationStep.ARTICLE_STATUS_PUBLISHED, T_article_status_published);
        accessRadios.addOption(org.dspace.submit.step.SelectPublicationStep.ARTICLE_STATUS_ACCEPTED, T_article_status_accepted);
        accessRadios.addOption(org.dspace.submit.step.SelectPublicationStep.ARTICLE_STATUS_IN_REVIEW, T_article_status_in_review);
        accessRadios.addOption(org.dspace.submit.step.SelectPublicationStep.ARTICLE_STATUS_NOT_YET_SUBMITTED, T_article_status_not_yet_submitted);
        accessRadios.setOptionSelected(request.getParameter("article_status"));
    }



    private void addManuscriptNumber(Request request, Item newItem) throws WingException {
        Composite optionsList;
        optionsList = newItem.addComposite("new-manu-comp");
        Text manuText = optionsList.addText("manu");
        if(request.getParameter("manu") != null)
            manuText.setValue(request.getParameter("manu"));
        manuText.setLabel(T_MANU_LABEL);

        //Add an error message should our manuscript be invalid
        if(this.errorFlag == org.dspace.submit.step.SelectPublicationStep.ERROR_SELECT_JOURNAL){
            //Show the error coming from our bean !
            manuText.addError(String.valueOf(request.getSession().getAttribute("submit_error")));
            //We are done clear it
            request.getSession().setAttribute("submit_error", null);
        }
    }


    private void addManuscriptNumberStatusAccepted(Request request, List form) throws WingException {
        if(errorFlag==org.dspace.submit.step.SelectPublicationStep.DISPLAY_MANUSCRIPT_NUMBER || errorFlag==org.dspace.submit.step.SelectPublicationStep.ENTER_MANUSCRIPT_NUMBER){
            Item item  = form.addItem("manu-number-status-accepted", "");
            Text manuText = item.addText("manu-number-status-accepted");
            if(request.getParameter("manu-number-status-accepted") != null)
                manuText.setValue(request.getParameter("manu-number-status-accepted"));
            manuText.setLabel(T_MANU_LABEL);
            manuText.setHelp(T_MANU_HELP);

            //Add an error message should our manuscript be invalid
            if(this.errorFlag == org.dspace.submit.step.SelectPublicationStep.ENTER_MANUSCRIPT_NUMBER){
                //Show the error coming from our bean !
                manuText.addError(String.valueOf(request.getSession().getAttribute("submit_error")));
                //We are done clear it
                request.getSession().setAttribute("submit_error", null);
            }

//            if(errorFlag==org.dspace.submit.step.SelectPublicationStep.ENTER_MANUSCRIPT_NUMBER){
//                manuText.addError(T_PUB_MANU_ERROR);
//            }
        }
    }

    private void addManuscriptAcceptance(Request request, List form) throws WingException {
        if(errorFlag==org.dspace.submit.step.SelectPublicationStep.DISPLAY_CONFIRM_MANUSCRIPT_ACCEPTANCE){
            Item item = form.addItem("manu_accepted-cb", "");
            CheckBox checkBox = item.addCheckBox("manu_acc");
            checkBox.addOption(String.valueOf(Boolean.TRUE), T_MANU_ACC_LABEL);
        }
    }

    private void addJournalSelect(String selectedJournalId, Item newItem) throws WingException {
        Composite optionsList = newItem.addComposite("new-options-comp");
        Select journalID = optionsList.addSelect("journalID");
        java.util.List<String> journalVals = org.dspace.submit.step.SelectPublicationStep.journalVals;
        java.util.List<String> journalNames = org.dspace.submit.step.SelectPublicationStep.journalNames;

        for (int i = 0; i < journalVals.size(); i++) {
            String val =  journalVals.get(i);
            String name =  journalNames.get(i);
            if(org.dspace.submit.step.SelectPublicationStep.integratedJournals.contains(val))
                name += "*";
            journalID.addOption(val.equals(selectedJournalId), val, name);
        }

        journalID.setLabel(T_SELECT_LABEL);
        journalID.setHelp(T_SELECT_HELP);
        if(this.errorFlag == org.dspace.submit.step.SelectPublicationStep.ERROR_INVALID_JOURNAL)
            journalID.addError(T_SELECT_ERROR);
    }

    private void addJournalSelectStatusNotYetSubmitted(String selectedJournalId, Item newItem) throws WingException {
        Composite optionsList = newItem.addComposite("journalID_status_not_yet_submitted");
        Select journalID = optionsList.addSelect("journalIDStatusNotYetSubmitted");
        java.util.List<String> journalVals = org.dspace.submit.step.SelectPublicationStep.journalVals;
        java.util.List<String> journalNames = org.dspace.submit.step.SelectPublicationStep.journalNames;

        for (int i = 0; i < journalVals.size(); i++) {
            String val =  journalVals.get(i);
            String name =  journalNames.get(i);
            if(org.dspace.submit.step.SelectPublicationStep.integratedJournals.contains(val))
                name += "*";
            journalID.addOption(val.equals(selectedJournalId), val, name);
        }

        journalID.setLabel(T_SELECT_LABEL);
        journalID.setHelp(T_SELECT_HELP_NOT_YET_SUBMITTED);
        if(this.errorFlag == org.dspace.submit.step.SelectPublicationStep.ERROR_INVALID_JOURNAL)
            journalID.addError(T_SELECT_ERROR);
    }

    public List addReviewSection(List list) throws SAXException, WingException, SQLException, IOException, AuthorizeException {
        return null;
    }
}
