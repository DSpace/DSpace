package org.dspace.app.xmlui.aspect.submission.submit;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.log4j.Logger;
import org.datadryad.api.DryadJournalConcept;
import org.datadryad.rest.models.Manuscript;
import org.dspace.JournalUtils;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.xmlui.aspect.submission.AbstractSubmissionStep;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Collection;
import org.dspace.content.MetadataField;
import org.dspace.content.authority.*;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.handle.HandleManager;
import org.dspace.paymentsystem.PaymentSystemConfigurationManager;
import org.dspace.paymentsystem.PaymentSystemService;
import org.dspace.paymentsystem.ShoppingCart;
import org.dspace.submit.AbstractProcessingStep;
import org.dspace.utils.DSpace;
import org.dspace.workflow.WorkflowItem;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * User: @author kevinvandevelde (kevin at atmire.com)
 * Date: 27-jan-2010
 * Time: 15:07:05
 * XMLUI interface for a step that allows to search and select a publication
 *
 *
 * Modified by: Fabio
 *
 * Changed all the first submission page to control it with javascript to lead the user trough the process.
 *
 *
 */
public class SelectPublicationStep extends AbstractSubmissionStep {
    private static Logger log = Logger.getLogger(SelectPublicationStep.class);

    private static final Message T_HEAD = message("xmlui.submit.select.pub.head");
    private static final Message T_TRAIL = message("xmlui.submit.select.pub.trail");
    private static final Message T_HELP = message("xmlui.submit.select.pub.help");
    private static final Message T_FORM_HEAD = message("xmlui.submit.select.pub.form_head");
    //private static final Message T_PUB_HELP = message("xmlui.submit.select.pub.help");
    //private static final Message T_PUB_HELP_NEW = message("xmlui.submit.select.pub.help_new");
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
    private static final Message T_SELECT_LABEL_NEW = message("xmlui.submit.publication.journal.select.label.new");
    private static final Message T_MANU_LABEL_NEW = message("xmlui.submit.publication.journal.manu.label.new");
    private static final Message T_MANU_LABEL_REVIEW = message("xmlui.submit.publication.journal.manu.label.review");

    private static final Message T_PUB_MANU_ERROR = message("xmlui.submit.select.pub.form.manu_error");

    private static final Message T_MANU_ACC_LABEL = message("xmlui.submit.publication.journal.manu.acc.label");

    private static final Message T_SELECT_HELP_NOT_YET_SUBMITTED = message("xmlui.submit.publication.journal.help_not_yet_submitted");
    private static final Message T_SELECT_HELP_IN_REVIEW = message("xmlui.submit.publication.journal.help_in_review");

    protected static final Message T_Country= message("xmlui.PaymentSystem.shoppingcart.order.country");
    protected static final Message T_Country_head= message("xmlui.submit.select.country.head");
    protected static final Message T_Country_help= message("xmlui.submit.select.country.help");
    protected static final Message T_Country_error= message("xmlui.submit.select.country.error");

    private static final Message T_funding_head = message("xmlui.submit.select.funding.head");
    private static final Message T_funding_help = message("xmlui.submit.select.funding.help");
    private static final Message T_funding_desc1 = message("xmlui.submit.select.funding.desc1");
    private static final Message T_funding_desc2 = message("xmlui.submit.select.funding.desc2");
    private static final Message T_funding_status_yes = message("xmlui_submit_funding_status_yes");
    private static final Message T_funding_status_no = message("xmlui_submit_funding_status_no");
    private static final Message T_funding_error = message("xmlui.submit.select.funding.error");

    private static final Message T_select_yes = message("xmlui.Submission.submit.ReviewStep.yes");
    private static final Message T_select_no = message("xmlui.Submission.submit.ReviewStep.no");

    private static final Message T_article_status = message("xmlui.submit.publication.article_status");
    private static final Message T_article_status_help = message("xmlui.submit.publication.article_status_help");

    private static final Message T_article_status_published = message("xmlui_submit_publication_article_status_published");
    private static final Message T_article_status_accepted = message("xmlui_submit_publication_article_status_accepted");
    private static final Message T_article_status_in_review = message("xmlui_submit_publication_article_status_in_review");
    private static final Message T_article_status_not_yet_submitted = message("xmlui_submit_publication_article_status_not_yet_submitted");

    private static final Message T_enter_article_doi = message("xmlui.submit.publication.enter_article_doi");

    private static final Message T_asterisk_explanation = message("xmlui.submit.publication.journal.manu.acc.asterisk_explanation");

    protected static final Message T_license_head = message("xmlui.submit.select.license.head");

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

        body.addDivision("step-link","step-link").addPara(T_TRAIL);

        Division helpDivision = body.addDivision("general-help","general-help");
        helpDivision.setHead(T_HEAD);
        helpDivision.addPara(T_HELP);

        Division div = body.addInteractiveDivision("submit-select-publication", actionURL, Division.METHOD_POST, "primary submission");
        addSubmissionProgressList(div);

        List form = div.addList("submit-create-publication", List.TYPE_FORM);

        boolean submitExisting = ConfigurationManager.getBooleanProperty("submit.dataset.existing-datasets", true);

        // retrieve request parameters: journalID, manuscriptNumber
        String selectedJournalId = request.getParameter("journalID");

        String manuscriptNumber = request.getParameter("manu");
        log.debug("initializing submission UI for journal " + selectedJournalId + ", manu " + manuscriptNumber);
	    Manuscript manuscript = null;
        String selectedJournalName = null;
        // get journal status and name
        if (selectedJournalId!=null) {
            try {
                DryadJournalConcept journalConcept = JournalUtils.getJournalConceptByJournalID(selectedJournalId);
                selectedJournalName = journalConcept.getFullName();
                if (journalConcept == null) {
                    throw new Exception("no journalID " + selectedJournalId);
                }
                manuscript = JournalUtils.getManuscriptFromManuscriptStorage(manuscriptNumber, journalConcept);

                if (manuscript == null) {
                    manuscript = new Manuscript(journalConcept);
                    if (!"".equals(manuscriptNumber)) {
                        manuscript.setManuscriptId(manuscriptNumber);
                    }
                }
            } catch (Exception e) {
                 //invalid journalID
                this.errorFlag = org.dspace.submit.step.SelectPublicationStep.ERROR_INVALID_JOURNAL;
		        log.error("Error getting parameters for invalid JournalID: " + selectedJournalId, e);
            }
        }

        // add radios: Accepted, In Review, Published, Not Yet Submitted
        addArticleStatusRadios(request, form, manuscript);

        //Get all the data required
        boolean pubIdError = this.errorFlag == org.dspace.submit.step.SelectPublicationStep.STATUS_INVALID_PUBLICATION_ID;
        Collection pubColl = (Collection) HandleManager.resolveToObject(context, ConfigurationManager.getProperty("submit.publications.collection"));

        //Start rendering our info
        Item newItem = form.addItem("select_publication_new", submitExisting ? "" : "odd");
        addRadioIfSubmitExisting(submitExisting, pubIdError, pubColl, newItem);

        // case A: (radio selected ==> published)
        addFieldsStatusPublished(request, newItem);

        // case B: (radio selected ==> accepted)
        addfieldsStatusAccepted(newItem, request, manuscript);

        // case D: (radio selected ==>  In Review)
        addJournalSelectStatusInReview(newItem, manuscript, request);

        // hidden select fields that populate integrated journals
        addJournalSelectStatusIntegrated(selectedJournalName, newItem);

        addPublicationNumberIfSubmitExisting(form, submitExisting, pubIdError, pubColl);

        addManuscriptNumber(request, newItem, manuscriptNumber);

        generateCountryList(form,request);
        generateFundingInfo(form,request);

        // add License checkbox.
        addLicense(form);

        //add "Next" button
	    Item actions = form.addItem();
	    actions.addButton(AbstractProcessingStep.NEXT_BUTTON).setValue(T_next);
    }




    private void addArticleStatusRadios(Request request, List form, Manuscript manuscript) throws WingException {
        // add "article status" radios
        Item articleStatus = form.addItem("jquery_radios", "");
        articleStatus.addContent(T_article_status);
        Radio accessRadios = articleStatus.addRadio("article_status");
        accessRadios.setHelp(T_article_status_help);
        accessRadios.addOption(org.dspace.submit.step.SelectPublicationStep.ARTICLE_STATUS_PUBLISHED, T_article_status_published);
        accessRadios.addOption(org.dspace.submit.step.SelectPublicationStep.ARTICLE_STATUS_ACCEPTED, T_article_status_accepted);
        accessRadios.addOption(org.dspace.submit.step.SelectPublicationStep.ARTICLE_STATUS_IN_REVIEW, T_article_status_in_review);
        //accessRadios.addOption(org.dspace.submit.step.SelectPublicationStep.ARTICLE_STATUS_NOT_YET_SUBMITTED, T_article_status_not_yet_submitted);


        if (manuscript!=null && manuscript.isAccepted()){
            accessRadios.setOptionSelected(org.dspace.submit.step.SelectPublicationStep.ARTICLE_STATUS_ACCEPTED);
        } else if (manuscript!=null && manuscript.isSubmitted()) {
            accessRadios.setOptionSelected(org.dspace.submit.step.SelectPublicationStep.ARTICLE_STATUS_IN_REVIEW);
        } else if(request.getParameter("journalID")!=null&&!request.getParameter("journalID").equals("")) {
            accessRadios.setOptionSelected(org.dspace.submit.step.SelectPublicationStep.ARTICLE_STATUS_IN_REVIEW);
        } else {
            accessRadios.setOptionSelected(request.getParameter("article_status"));
        }
    }


    private void addManuscriptNumber(Request request, Item newItem, String manuscriptNumber) throws WingException {
        Composite optionsList;
        optionsList = newItem.addComposite("new-manu-comp");
        Text manuText = optionsList.addText("manu");

        if(manuscriptNumber != null){
                manuText.setValue(manuscriptNumber);
        }

        manuText.setLabel(T_MANU_LABEL_REVIEW);

        // Add an error message in case manuscript number is invalid
        if(this.errorFlag == org.dspace.submit.step.SelectPublicationStep.ERROR_SELECT_JOURNAL){
            //Show the error coming from our manuscript !
            manuText.addError(String.valueOf(request.getSession().getAttribute("submit_error")));
            //We are done clear it
            request.getSession().setAttribute("submit_error", null);
        }
    }

    private void addFieldsStatusPublished(Request request, Item newItem) throws WingException {
        Composite doi = newItem.addComposite("doi");
        Text textArticleDOI = doi.addText("article_doi");
        textArticleDOI.setLabel(T_enter_article_doi);
        if(request.getParameter("article_doi") != null)
            textArticleDOI.setValue(request.getParameter("article_doi"));
        if(this.errorFlag == org.dspace.submit.step.SelectPublicationStep.ERROR_PUBMED_DOI){
            textArticleDOI.addError("Invalid Identifier.");
        }
        if(this.errorFlag == org.dspace.submit.step.SelectPublicationStep.ERROR_PUBMED_NAME){
            textArticleDOI.addError("No journal name.");
        }


    }

    private void addfieldsStatusAccepted(Item newItem, Request request, Manuscript manuscript) throws WingException {
        // JOURNAL ID
        Composite optionsList = newItem.addComposite("new-options-comp");
        Text journalField = addJournalAuthorityControlled(optionsList, "prism_publicationName");
	    journalField.setHelp(T_asterisk_explanation);
        journalField.setLabel(T_SELECT_LABEL_NEW);


        if (manuscript!=null && manuscript.isAccepted()) {
            journalField.setValue(manuscript.getJournalName());
        } else {
            journalField.setValue(request.getParameter("prism_publicationName"));
        }
    }

    private void addJournalSelectStatusInReview(Item newItem, Manuscript manuscript, Request request) throws WingException,SQLException {
        Composite optionsList = newItem.addComposite("journalID_status_in_review");
        Select journalID = optionsList.addSelect("journalIDStatusInReview");
        journalID.addOption("", "Please select a valid journal");
        java.util.List<DryadJournalConcept> journalConcepts = Arrays.asList((DryadJournalConcept[]) JournalUtils.getAllJournalConcepts());
        for (DryadJournalConcept journalConcept : journalConcepts) {
            String val = journalConcept.getJournalID();
            String name = journalConcept.getFullName();
            // add only journal with allowReviewWorkflow=true;
            if (journalConcept.getAllowReviewWorkflow() && journalConcept.getIntegrated()) {
                journalID.addOption(val, name);
            }
        }
        String selectedJournalID = request.getParameter("journalIDStatusInReview");
        if (selectedJournalID == null && manuscript != null) {
            selectedJournalID = manuscript.getJournalConcept().getJournalID();
        }
        log.error("selected journal is " + selectedJournalID);
        if (!"".equals(selectedJournalID)) {
            journalID.setOptionSelected(selectedJournalID);
        }
        journalID.setLabel(T_SELECT_LABEL);
        journalID.setHelp(T_SELECT_HELP_IN_REVIEW);
        if(this.errorFlag == org.dspace.submit.step.SelectPublicationStep.ERROR_INVALID_JOURNAL)
            journalID.addError(T_SELECT_ERROR);
    }


    private void addJournalSelectStatusIntegrated(String selectedJournalName, Item newItem) throws WingException,SQLException {
        Composite optionsList = newItem.addComposite("journalID_status_integrated");
        Select journalID = optionsList.addSelect("journalIDStatusIntegrated");
        if (selectedJournalName == null || "".equals(selectedJournalName)) {
            java.util.List<DryadJournalConcept> journalConcepts = Arrays.asList((DryadJournalConcept[]) JournalUtils.getAllJournalConcepts());
            for (DryadJournalConcept journalConcept : journalConcepts) {
                if (journalConcept.getIntegrated()) {
                    String val = journalConcept.getFullName();
                    journalID.addOption(val.equals(selectedJournalName), val, val);
                }
            }
        } else {
            log.error("journal was " + selectedJournalName);
            journalID.setOptionSelected(selectedJournalName);
        }
    }

    public List addReviewSection(List list) throws SAXException, WingException, SQLException, IOException, AuthorizeException {
        return null;
    }


    private Text addJournalAuthorityControlled(Composite comp, String fieldName) throws WingException {
        String fieldkey = MetadataField.formKey("prism", "publicationName", null);
        Text journal = comp.addText(fieldName);
        journal.setAuthorityControlled();
        journal.setChoices(fieldkey);
        journal.setChoicesPresentation(ConfigurationManager.getProperty("choices.presentation.prism.publicationName"));
        journal.setChoicesClosed(ChoiceAuthorityManager.getManager().isClosed(fieldkey));
        if(this.errorFlag == org.dspace.submit.step.SelectPublicationStep.ERROR_INVALID_JOURNAL)
            journal.addError(T_SELECT_ERROR);

        return journal;

    }

    // NOTE: this method is always turned off in our current layout.
    // If it is enabled again, CHECK THE FORMATTING in utils.js and Mirage.xsl.
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

    private void addRadioIfSubmitExisting(boolean submitExisting, boolean pubIdError, Collection pubColl, Item newItem) throws WingException, SQLException {
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
           newItem.addHidden("publication_select").setValue("create");
       }
    }


    private void addLicense(List form) throws WingException {
        Item licenseItem = form.addItem("license_accepted","license_accepted");
        licenseItem.addContent(T_license_head);
        CheckBox licensebox = licenseItem.addCheckBox("license_accept");
        licensebox.addOption(String.valueOf(Boolean.TRUE), T_PUB_LICENSE);
        if(this.errorFlag == org.dspace.submit.step.SelectPublicationStep.STATUS_LICENSE_NOT_ACCEPTED)
            licensebox.addError(T_PUB_LICENSE_ERROR);
    }

    private void generateCountryList(org.dspace.app.xmlui.wing.element.List info,Request request) throws WingException{
        java.util.List<String> countryArray = PaymentSystemConfigurationManager.getSortedCountry();
        try{
	org.dspace.app.xmlui.wing.element.Item countryItem = info.addItem("country-help","country-help");
	countryItem.addContent(T_Country_head);
	countryItem.addContent(T_Country_help);

        Select countryList = countryItem.addSelect("country");
        countryList.addOption("","Select a fee-waiver country");
        String selectedCountry = request.getParameter("country");
        PaymentSystemService paymentSystemService = new DSpace().getSingletonService(PaymentSystemService.class);
        SubmissionInfo submissionInfo=(SubmissionInfo)request.getAttribute("dspace.submission.info");
        org.dspace.content.Item item = null;

        if(submissionInfo==null)
        {
            String workflowId = request.getParameter("workflowID");
            if(workflowId==null) {
                // item is no longer in submission OR workflow, probably archived, so we don't need shopping cart info
                return;
            }
            WorkflowItem workflowItem = WorkflowItem.find(context,Integer.parseInt(workflowId));
            item = workflowItem.getItem();
        }
        else
        {
            item = submissionInfo.getSubmissionItem().getItem();
        }
        ShoppingCart shoppingCart = paymentSystemService.getShoppingCartByItemId(context,item.getID());
        org.dspace.app.xmlui.wing.element.List hiddenList = info.addList("transaction");
        hiddenList.addItem().addHidden("transactionId").setValue(Integer.toString(shoppingCart.getID()));
        hiddenList.addItem().addHidden("baseUrl").setValue(request.getContextPath());


        if(selectedCountry==null)
        {
            if(shoppingCart!=null){
                selectedCountry = shoppingCart.getCountry();
            }
        }
            else
        {
            if(shoppingCart!=null)
            {
                shoppingCart.setCountry(selectedCountry);
            }
        }
        for(String temp:countryArray){
            {
                String[] countryTemp = temp.split(":");
                if(selectedCountry!=null&&selectedCountry.equals(countryTemp[0]))
                {
                    countryList.addOption(true,countryTemp[0],countryTemp[0]);
                }
                else
                {
                    countryList.addOption(false,countryTemp[0],countryTemp[0]);
                }
            }
        }
        }catch (Exception e)
        {}
    }

    private void generateFundingInfo(List form, Request request) throws WingException {
        Item fundingInfo = form.addItem("funding-info","");
        fundingInfo.addContent(T_funding_head);
        fundingInfo.addContent(T_funding_help);
//
//        Radio fundingRadio = fundingInfo.addRadio("funding-status");
//        fundingRadio.addOption("1", T_funding_status_yes);
//        fundingRadio.addOption("0", T_funding_status_no);
//
        Text grantInfoText = form.addItem("grant-info","grant-info").addText("grant-info");
        grantInfoText.setLabel(T_funding_desc1);
//        if (this.errorFlag == org.dspace.submit.step.SelectPublicationStep.ERROR_INVALID_GRANT) {
//            fundingRadio.setOptionSelected("1");
//            grantInfoText.setValue(request.getParameter("grant-info"));
//            grantInfoText.addError(T_funding_error);
//        }
    }
}
