package org.dspace.app.xmlui.aspect.submission.submit;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.aspect.submission.AbstractSubmissionStep;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.Item;

import org.dspace.submit.AbstractProcessingStep;
import org.dspace.workflow.DryadWorkflowUtils;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;

/**
 * User: @author kevinvandevelde (kevin at atmire.com)
 * Date: 27-jan-2010
 * Time: 16:34:24
 *
 * The describe class for the data file
 */
public class DescribeDatasetStep extends AbstractSubmissionStep {
    private static final Message T_HEAD = message("xmlui.submit.dataset.describe.head");
    private static final Message T_TRAIL = message("xmlui.submit.dataset.describe.trail");
    private static final Message T_HELP = message("xmlui.submit.dataset.describe.help");
    private static final Message T_FORM_PUB_HEAD = message("xmlui.submit.dataset.form.pub.head");
    private static final Message T_FORM_FILE_HEAD = message("xmlui.submit.dataset.form.file.head");
    private static final Message T_FORM_FILE_DETAILS = message("xmlui.submit.dataset.form.details");
    private static final Message T_FORM_DATA_REMOVE = message("xmlui.submit.dataset.form.dataset.remove");
    private static final Message T_FORM_DATA_FILE_ERROR = message("xmlui.submit.dataset.form.dataset.file.error");
    private static final Message T_FORM_DATA_FILE_REPO_ERROR = message("xmlui.submit.dataset.form.dataset.file-url.error");
    private static final Message T_FORM_DATA_HEAD = message("xmlui.submit.dataset.form.dataset.head");
    private static final Message T_FORM_DATA_LICENSE_HEAD = message("xmlui.submit.dataset.form.license.label");
    private static final Message T_FORM_DATA_LICENSE_CONTENT = message("xmlui.submit.dataset.license");
    private static final Message T_FORM_DATA_FILE_REPO_HELP = message("xmlui.submit.dataset.form.dataset.file-url.help");
    private static final Message T_FORM_REPO_NAME_HELP = message("xmlui.submit.dataset.form.dataset.repo-name.help");
    private static final Message T_cancel = message("xmlui.general.cancel_delete");


    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, SQLException, IOException,
            AuthorizeException {
        pageMeta.addMetadata("title").addContent("Dryad Submission");

        pageMeta.addTrailLink(contextPath + "/", "Dryad Home");
        pageMeta.addTrail().addContent("Submission");
    }

    @Override
    public void addBody(Body body) throws SAXException, WingException, SQLException, IOException, AuthorizeException {
        org.dspace.content.Item item = submission.getItem();
        Collection collection = submission.getCollection();
        Request request = ObjectModelHelper.getRequest(objectModel);
        String actionURL = contextPath + "/handle/" + collection.getHandle() + "/submit/" + knot.getId() + ".continue";

        body.addDivision("step-link","step-link").addPara(T_TRAIL);

        Division helpDivision = body.addDivision("general-help","general-help");
        helpDivision.setHead(T_HEAD);
        helpDivision.addPara(T_HELP);

        Division div = body.addInteractiveDivision("submit-describe-dataset", actionURL, Division.METHOD_MULTIPART, "primary submission");
        //addSubmissionProgressList(div);


        List pubList = div.addList("submit-select-publication", List.TYPE_FORM);
        pubList.setHead(T_FORM_PUB_HEAD);
        String pubSummary = getPublicationSummary();
        pubList.addItem().addContent(pubSummary);


        //Find out if we have a bitstream fitting
        Bitstream fileFound = null;
        Bundle[] bundles = item.getBundles("ORIGINAL");
        if(bundles != null && 0 < bundles.length){
            Bitstream[] bits = bundles[0].getBitstreams();
            for (Bitstream bit : bits) {
                if("dataset-file".equals(bit.getDescription())){
                    fileFound = bit;
                }
            }
        }
        DCRepositoryFile externalFile = null;
        DCValue[] externalVals = item.getMetadata("dryad", "externalIdentifier", null, Item.ANY);
        for (DCValue externalVal : externalVals) {
            if(externalVal.value != null && !externalVal.value.equals(""))
                externalFile = new DCRepositoryFile(externalVal.value);
        }


        if(fileFound != null){
            List displayFileList = div.addList("submit-overview-file", List.TYPE_FORM);
            displayFileList.setHead(T_FORM_FILE_HEAD);

            org.dspace.app.xmlui.wing.element.Item detailsItem = displayFileList.addItem("data-upload-details" , "");
            detailsItem.addContent(T_FORM_FILE_DETAILS);
            
            org.dspace.app.xmlui.wing.element.Item fileItem = displayFileList.addItem("bitstream-item", "");

            String url = DescribeStepUtils.makeBitstreamLink(contextPath, item, fileFound);
            fileItem.addHighlight("head").addContent(message("xmlui.Submission.submit.UploadStep.column2"));
            fileItem.addHighlight("head").addContent(message("xmlui.Submission.submit.UploadStep.column3"));
            fileItem.addHighlight("head").addContent(message("xmlui.Submission.submit.UploadStep.column5"));
            fileItem.addHighlight("head").addContent(message("xmlui.Submission.submit.UploadStep.column7"));

            fileItem.addHighlight("content").addXref(url, fileFound.getName());
            fileItem.addHighlight("content").addContent((fileFound.getSize() / 1000) + "Kb");
            fileItem.addHighlight("content").addContent(fileFound.getFormat().getDescription());
            fileItem.addHidden("remove_dataset_id").setValue("" + fileFound.getID());
            fileItem.addHidden("dataset_id_present").setValue("" + fileFound.getID());
            //Also create a remove button
            fileItem.addHighlight("content").addButton("submit_remove_dataset").setValue(T_FORM_DATA_REMOVE);
        }else
        if(externalFile != null){
            List displayFileList = div.addList("submit-overview-url", List.TYPE_FORM);
            displayFileList.setHead(T_FORM_FILE_HEAD);

            org.dspace.app.xmlui.wing.element.Item detailsItem = displayFileList.addItem("data-upload-details" , "");
            detailsItem.addContent(T_FORM_FILE_DETAILS);
            
            String externalUrl = externalFile.toUrl();

            org.dspace.app.xmlui.wing.element.Item externalFileItem = displayFileList.addItem("external-item", "");
            if(externalUrl != null)
                externalFileItem.addXref(externalUrl, externalUrl);
            else
                externalFileItem.addHighlight("").addContent(externalFile.toString());
            externalFileItem.addButton("submit_remove_external").setValue(T_FORM_DATA_REMOVE);
            //This is needed to disable our embargo field (javascript will take care of this)
            externalFileItem.addHidden("disabled-embargo").setValue(Boolean.TRUE.toString());
        }else{
            List fileList = div.addList("submit-upload-file", List.TYPE_FORM);
            fileList.setHead(T_FORM_FILE_HEAD);

            org.dspace.app.xmlui.wing.element.Item detailsItem = fileList.addItem("data-upload-details" , "");
            detailsItem.addContent(T_FORM_FILE_DETAILS);

            org.dspace.app.xmlui.wing.element.Item fileItem = fileList.addItem("dataset-item", "");

            boolean fileSelected = request.getParameter("datafile_type") == null || request.getParameter("datafile_type").equals("file");

            fileItem.addRadio("datafile_type").addOption(fileSelected, "file");
            File file = fileItem.addFile("dataset-file");
            fileItem.addHidden("dataset-file-description").setValue("dataset-file");
            if(errorFields.contains("dataset-file")){
                file.addError(T_FORM_DATA_FILE_ERROR);
            }

            org.dspace.app.xmlui.wing.element.Item fileUrlItem = fileList.addItem("dataset-identifier", "");
            fileUrlItem.addRadio("datafile_type").addOption(!fileSelected, "identifier");
            if(!fileSelected)
                fileUrlItem.addHidden("disabled-embargo").setValue(Boolean.TRUE.toString());


            Composite dataComp = fileUrlItem.addComposite("data_file_repo");
            Text dataIdenTxt = dataComp.addText("datafile_identifier");
            dataIdenTxt.setValue(request.getParameter("datafile_identifier") == null ? "" : request.getParameter("datafile_identifier"));
            /* TODO: Find allowed method to set these two attributes
             * dataIdenTxt.setAttribute('placeholder', T_FORM_DATA_FILE_REPO_HELP);
             * dataIdenTxt.setAttribute('title', T_FORM_DATA_FILE_REPO_HELP);
             */
            Select datafileRepo = dataComp.addSelect("datafile_repo");
            datafileRepo.addOption("select-repo", "(please select a repository)");

            for(String repo : DCRepositoryFile.repositoryLocations.keySet())
                datafileRepo.addOption(repo, repo);

            datafileRepo.addOption("other", "OTHER REPOSITORY");
            if(errorFields.contains("dataset-file-url")){
                dataComp.addError(T_FORM_DATA_FILE_REPO_ERROR);
            }
            Text repoNameTxt = dataComp.addText("other_repo_name");
            repoNameTxt.setHelp(T_FORM_REPO_NAME_HELP);
        }



        //Start adding the metadata fields
        List form = div.addList("submit-describe-dataset", List.TYPE_FORM);
        form.setHead(T_FORM_DATA_HEAD);

        this.errorFields = DescribeStepUtils.renderFormList(context, contextPath,  form, getPage(), errorFields, submissionInfo, item, collection);

        //Add the license stuff
        form.addLabel(T_FORM_DATA_LICENSE_HEAD);
        form.addItem().addContent(T_FORM_DATA_LICENSE_CONTENT);

        //add standard control/paging buttons
        addControlButtons(form);
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
    public void addControlButtons(List controls)
        throws WingException
    {
        org.dspace.app.xmlui.wing.element.Item actions = controls.addItem();

        //We do NOT add a previous button when describing the datasetstep
        //only have "<-Previous" button if not first step
//        if(!isFirstStep())
//            actions.addButton(AbstractProcessingStep.PREVIOUS_BUTTON).setValue(T_previous);

        //always show "Save/Cancel"
        //actions.addButton(AbstractProcessingStep.CANCEL_BUTTON).setValue(T_save);

        // customized cancel button for DRYAD: when a user click this button; it has to delete eventually the files added
        // and go to overview step
        actions.addButton(AbstractProcessingStep.CANCEL_BUTTON).setValue(T_cancel);

        //If last step, show "Complete Submission"
        if(isLastStep())
            actions.addButton(AbstractProcessingStep.NEXT_BUTTON).setValue(T_complete);
        else //otherwise, show "Next->"
            actions.addButton(AbstractProcessingStep.NEXT_BUTTON).setValue(T_next);
    }

    public void addOptions(Options options) throws SAXException, WingException,
            SQLException, IOException, AuthorizeException
    {
        options.addList("human-subjects");
        options.addList("large-data-packages");
    }


    public List addReviewSection(List list) throws SAXException, WingException, SQLException, IOException, AuthorizeException {
        //We do not have a review step
        return null;
    }

    private String getPublicationSummary() throws SQLException {
        //Time to get our summary
        //Don't worry we ALWAYS have a publication when we get here
        org.dspace.content.Item publication = DryadWorkflowUtils.getDataPackage(context, submission.getItem());

        if (publication == null) throw new RuntimeException("Cannot resolve the Item identifier.");
        //The summary is the title + "," + authors + journal name
        String title = publication.getName();
        if (title == null)
            title = "Untitled";

        String authorString = "";
        DCValue[] creators = publication.getMetadata("dc", "creator", null, Item.ANY);
        for (DCValue creator : creators)
            authorString += creator.value + ", ";

        DCValue[] authors = publication.getMetadata("dc", "contributor", Item.ANY, Item.ANY);
        for (DCValue author : authors)
            if(!"correspondingAuthor".equals(author.qualifier))
            authorString += author.value + ", ";

        //Get the journal name
        //TODO: get the journal name what metadata type is this ?

        //Make sure that we do not have an authorstring that ends with a comma
        if(authorString.endsWith(", "))
            authorString = authorString.substring(0, authorString.lastIndexOf(", "));

        return title + ", " + authorString;
    }

    @Override
    public void recycle() {
        //Clear out our error fields
        this.errorFields = null;
        super.recycle();
    }
}
