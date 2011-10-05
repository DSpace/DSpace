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

        //Get all the data required
        boolean pubIdError = this.errorFlag == org.dspace.submit.step.SelectPublicationStep.STATUS_INVALID_PUBLICATION_ID;
        Collection pubColl = (Collection) HandleManager.resolveToObject(context, ConfigurationManager.getProperty("submit.publications.collection"));
        String selectedJournalId = request.getParameter("journalID");


        //Start rendering our info
        Item newItem = form.addItem("select_publication_new", submitExisting ? "" : "odd");
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
        Composite optionsList = newItem.addComposite("new-options-comp");

        Select journalID = optionsList.addSelect("journalID");
//        journalID.addOption("other".equals(selectedJournalId), "other","(please select a journal)");
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



        CheckBox licensebox = form.addItem().addCheckBox("license_accept");
        licensebox.addOption(String.valueOf(Boolean.TRUE), T_PUB_LICENSE);
        if(this.errorFlag == org.dspace.submit.step.SelectPublicationStep.STATUS_LICENSE_NOT_ACCEPTED)
            licensebox.addError(T_PUB_LICENSE_ERROR);


        //add standard control/paging buttons
        addControlButtons(form);
    }

    public List addReviewSection(List list) throws SAXException, WingException, SQLException, IOException, AuthorizeException {
        return null;
    }
}
