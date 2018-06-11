package org.dspace.app.xmlui.aspect.submission.submit;

import org.apache.commons.io.FileUtils;
import org.datadryad.api.DryadDataFile;
import org.dspace.app.xmlui.aspect.submission.AbstractStep;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.app.xmlui.utils.XSLUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.handle.HandleManager;
import org.dspace.submit.AbstractProcessingStep;
import org.dspace.workflow.DryadWorkflowUtils;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowRequirementsManager;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 8-jun-2010
 * Time: 11:54:39
 * Page that displays an overview of a publication & it's datasets
 * On this page you can edit your publication or dataset(s)
 * You can delete dataset(s)
 * You can finish up the submission
 */
public class OverviewStep extends AbstractStep {
    private static final Message T_MAIN_HEAD = message("xmlui.Submission.submit.OverviewStep.head");
    private static final Message T_MAIN_HELP = message("xmlui.Submission.submit.OverviewStep.help");
    private static final Message T_TRAIL = message("xmlui.Submission.submit.OverviewStep.trail");
    private static final Message T_STEPS_HEAD_1 = message("xmlui.Submission.submit.OverviewStep.steps.1");
    private static final Message T_STEPS_HEAD_2 = message("xmlui.Submission.submit.OverviewStep.steps.2");
    private static final Message T_STEPS_HEAD_3 = message("xmlui.Submission.submit.OverviewStep.steps.3");
    private static final Message T_STEPS_HEAD_4 = message("xmlui.Submission.submit.OverviewStep.steps.4");
    private static final Message T_FINALIZE_HELP = message("xmlui.Submission.submit.OverviewStep.finalize.help");
    private static final Message T_FINALIZE_BUTTON = message("xmlui.Submission.submit.OverviewStep.button.finalize");
    private static final Message T_ERROR_ALL_FILES = message("xmlui.Submission.submit.OverviewStep.error.finalize.all-files");
    private static final Message T_ERROR_ONE_FILE = message("xmlui.Submission.submit.OverviewStep.error.finalize.one-file");

    private static final Message T_BUTTON_PUBLICATION_DELETE = message("xmlui.Submission.submit.OverviewStep.button.delete-datapackage");
    private static final Message T_BUTTON_PUBLICATION_EDIT = message("xmlui.Submission.submit.OverviewStep.button.edit-datapackage");
    private static final Message T_BUTTON_DATAFILE_ADD = message("xmlui.Submission.submit.OverviewStep.button.add-datafile");
    private static final Message T_BUTTON_DATAFILE_EDIT = message("xmlui.Submission.submit.OverviewStep.button.datafile.edit");
    private static final Message T_BUTTON_DATAFILE_DELETE = message("xmlui.Submission.submit.OverviewStep.button.datafile.delete");
    private static final Message T_BUTTON_DATAFILE_CONTINUE = message("xmlui.Submission.submit.OverviewStep.button.datafile.continue");
    private static final Message T_DUP_SUBMISSION = message("xmlui.submit.publication.describe.duplicatesubmission");

    private static final Message T_STEPS_HEAD_GENBANK = message("xmlui.Submission.submit.OverviewStep.headgenbank");
    private static final Message T_GENBANK_HELP = message("xmlui.Submission.submit.OverviewStep.genbankhelp");
    private static final Message T_GENBANK_BUTTON = message("xmlui.Submission.submit.OverviewStep.button.genbank");

    @Override
    public void addBody(Body body) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        Collection collection = submission.getCollection();
        String actionURL = contextPath + "/handle/" + collection.getHandle() + "/submit/" + knot.getId() + ".continue";
        org.dspace.content.Item publication = DryadWorkflowUtils.getDataPackage(context, submission.getItem());
        if(publication == null)
            publication = submission.getItem();
        
        body.addDivision("step-link","step-link").addPara(T_TRAIL);

        Division helpDivision = body.addDivision("general-help","general-help");
        helpDivision.setHead(T_MAIN_HEAD);
        helpDivision.addPara(T_MAIN_HELP);

        if (publication.checkForDuplicateItems(context)) {
            Division dupDivision = body.addDivision("duplicate-info", "duplicate-info");
            dupDivision.addPara(T_DUP_SUBMISSION);
        }

        Division mainDiv = body.addInteractiveDivision("submit-completed-dataset", actionURL, Division.METHOD_POST, "primary submission");

        Division actionsDiv = mainDiv.addDivision("submit-completed-overview");

        //First of all add all the publication info
        Division pubDiv = actionsDiv.addDivision("puboverviewdivision", "odd subdiv");

        pubDiv.setHead(T_STEPS_HEAD_1);
        //TODO: expand this !

        // display a formatted reference to the data package
        ReferenceSet refSet = pubDiv.addReferenceSet("submission", ReferenceSet.TYPE_SUMMARY_VIEW);
        refSet.addReference(publication);

        //add an edit button for the publication (if we aren't archived)
        if(!publication.isArchived()){
            Para actionsPara = pubDiv.addPara();
            actionsPara.addButton("submit_edit_publication").setValue(T_BUTTON_PUBLICATION_EDIT);
            if(submission instanceof WorkflowItem){
                //For a curator add an edit metadata button
                actionsPara.addButton("submit_edit_metadata_" + submission.getID()).setValue(message("xmlui.Submission.Submissions.OverviewStep.edit-metadata-pub"));
            }
        }


        //A boolean set to true if we have dataset that is not fully submitted, BUT has been linked to the publication
        boolean submissionNotFinished = false;
        boolean noDatasets = false;
        org.dspace.content.Item[] datasets = DryadWorkflowUtils.getDataFiles(context, publication);
        //Second add info & edit/delete buttons for all our data files
        {
            Division dataDiv = actionsDiv.addDivision("dataoverviewdivision", "even subdiv");

            //First of all add the current one !
            dataDiv.setHead(T_STEPS_HEAD_2);

            Table dataSetList = dataDiv.addTable("datasets", 1, 3, "datasets");
            Cell addCell = dataSetList.addRow().addCell("add_file", null, 1, 3, "add_file");
            addCell.addButton("submit_adddataset").setValue(T_BUTTON_DATAFILE_ADD);

            if(datasets.length == 0)
                noDatasets = true;

            if(publication.isArchived()){
                //Add the current dataset, since our publication is archived this will probally be the only one !
                submissionNotFinished = renderDatasetItem(submissionNotFinished, dataSetList, submission.getItem(), (WorkspaceItem) submission);
            }

            for (org.dspace.content.Item dataset : datasets) {
                //Our current item has already been added
                InProgressSubmission wsDataset;
                if(submissionInfo.getSubmissionItem() instanceof WorkspaceItem)
                    wsDataset = WorkspaceItem.findByItemId(context, dataset.getID());
                else
                    wsDataset = WorkflowItem.findByItemId(context, dataset.getID());
                //Only add stuff IF we have a workspaceitem
                if(wsDataset != null){
                    submissionNotFinished = renderDatasetItem(submissionNotFinished, dataSetList, dataset, wsDataset);
                }
            }

        }
        //Thirdly add the the upload data files to external repos (this is optional)
	// Commented out because there is a bug somewhere in this code -- Clicking the checkbox for TreeBASE upload causes the Tomcat server to crash!
	/*
        {
            if(submission instanceof WorkspaceItem){

                java.util.List<Bitstream> toUploadFiles = new ArrayList<Bitstream>();
                for (org.dspace.content.Item dataset : datasets) {
                    Bundle[] orignalBundles = dataset.getBundles("ORIGINAL");
                    if(orignalBundles != null){
                        for (Bundle bundle : orignalBundles) {
                            if(bundle != null){
                                Bitstream[] bits = bundle.getBitstreams();
                                for (Bitstream bit : bits) {
                                    toUploadFiles.add(bit);
                                }
                            }
                        }
                    }
                }

                if(0 < toUploadFiles.size()){
                    Division uploadDiv = actionsDiv.addDivision("uploadexternaldivision", "odd subdiv");
                    uploadDiv.addPara("data-label", "bold").addContent(T_STEPS_HEAD_3);
                    List fileList = uploadDiv.addList("upload-external-list");
                    for (Bitstream bitstream : toUploadFiles) {
                        org.dspace.content.Item item = bitstream.getBundles()[0].getItems()[0];
                        Item externalItem = fileList.addItem();
                        externalItem.addCheckBox("export_item").addOption(false, item.getHandle(), "");

                        String identifier;
                        if (item.getHandle() != null)
                            identifier = "handle/"+item.getHandle();
                        else if (item != null)
                            identifier = "item/"+item.getID();
                        else
                            identifier = "id/"+bitstream.getID();


                        String url = contextPath + "/bitstream/"+identifier+"/";

                        // If we can put the pretty name of the bitstream on the end of the URL
                        try
                        {
                            if (bitstream.getName() != null)
                                url += Util.encodeBitstreamName(bitstream.getName(), "UTF-8");
                        }
                        catch (UnsupportedEncodingException uee)
                        {
                            // just ignore it, we don't have to have a pretty
                            // name on the end of the url because the sequence id will
                            // locate it. However it means that links in this file might
                            // not work....
                        }

                        url += "?sequence="+bitstream.getSequenceID();

                        externalItem.addXref(url, bitstream.getName());


                        Select repoSelect = externalItem.addSelect("repo_name_" + item.getHandle());
                        for(String repo : DCRepositoryFile.repoConfigLocations)
                            repoSelect.addOption(repo, repo);

                    }
                }
            }
        }
	*/

        // Add GenBank button
//        Division genBankDiv = actionsDiv.addDivision("genbankdivision", "odd subdiv");
//        if(submission instanceof WorkspaceItem){
//            genBankDiv.addPara("data-label", "bold").addContent(T_STEPS_HEAD_GENBANK);
//            genBankDiv.addPara().addContent(T_GENBANK_HELP);
//
//            String token = generateTokenAndAddToMetadata();
//
//            String url= ConfigurationManager.getProperty("genbank.url") + "/?tool=genbank&dryadID=" + publication.getMetadata("dc.identifier")[0].value +  "&ticket=" + token;
//            genBankDiv.addPara().addHidden("genbank_url").setValue(url);
//            genBankDiv.addPara().addButton("submit_genbank").setValue(T_GENBANK_BUTTON);
//        }

        //Lastly add the finalize submission button
        Division finDiv = actionsDiv.addDivision("finalizedivision", (submission instanceof WorkspaceItem ? "even" : "odd") + " subdiv");

        if(submission instanceof WorkspaceItem){
            finDiv.setHead(T_STEPS_HEAD_4);
            finDiv.addPara().addContent(T_FINALIZE_HELP);

            // add Delete and Continue to Checkout buttons
            Para bottomButtonPara = finDiv.addPara();
            if (!publication.isArchived() && submission instanceof WorkspaceItem) {
                WorkspaceItem pubWsItem = WorkspaceItem.findByItemId(context, publication.getID());
                bottomButtonPara.addButton("submit_delete_datapack_" + pubWsItem.getID()).setValue(T_BUTTON_PUBLICATION_DELETE);
            }

            Button finishButton = bottomButtonPara.addButton(AbstractProcessingStep.NEXT_BUTTON);
            finishButton.setValue(T_FINALIZE_BUTTON);
            if(submissionNotFinished || noDatasets){
                finishButton.setDisabled(true);
                if(submissionNotFinished)
                    finishButton.addError(T_ERROR_ALL_FILES);
                else
                    finishButton.addError(T_ERROR_ONE_FILE);
            }
        } else {
            //
            finDiv.addPara().addButton("submit_cancel").setValue(message("xmlui.general.return"));
        }
    }

    private String generateTokenAndAddToMetadata() throws SQLException, AuthorizeException {
        String randomKey = null;

        DCValue[] values = submission.getItem().getMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA + ".genbank.token");
        if(values!=null && values.length > 0){
            randomKey = values[0].value;
        }
        else{
            UUID uuid = UUID.randomUUID();
            submission.getItem().addMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA, "genbank", "token", null, uuid.toString());
            submission.getItem().update();
            randomKey = uuid.toString();
        }
        return randomKey;
    }

    private boolean renderDatasetItem(boolean submissionNotFinished, Table table, org.dspace.content.Item dataset, InProgressSubmission wsDataset) throws WingException, SQLException {
        // first row: buttons, title
        Row row = table.addRow("dataset-row-top", null, "dataset-row-top");
        Cell actionCell = row.addCell("dataset-action",null, 0, 0, "dataset-action");
        Cell labelCell = row.addCell("dataset-label",null, 0, 0, "dataset-label");
        Cell dataCell = row.addCell("dataset-label",null, 0, 0, "dataset-label");
        Button editButton = actionCell.addButton("submit_edit_dataset_" + wsDataset.getID());
        //To determine which name our button is getting check if we are through submission with this
        if (dataset.getMetadata("internal", "workflow", "submitted", org.dspace.content.Item.ANY).length == 0 && (wsDataset instanceof WorkspaceItem)) {
            editButton.setValue(T_BUTTON_DATAFILE_CONTINUE);
            submissionNotFinished = true;
        } else {
            editButton.setValue(T_BUTTON_DATAFILE_EDIT);
        }
        actionCell.addButton("submit_delete_dataset_" + wsDataset.getID()).setValue(T_BUTTON_DATAFILE_DELETE);

        DryadDataFile dryadDataFile = new DryadDataFile(dataset);
        String datasetTitle = XSLUtils.getShortFileName(wsDataset.getItem().getName(), 50);

        labelCell.addContent("Title");
        dataCell.addContent(datasetTitle);

        // filename
        row = table.addRow("dataset-row", null, "dataset-row");
        actionCell = row.addCell("dataset-action",null, 0, 0, "dataset-action");
        labelCell = row.addCell("dataset-label",null, 0, 0, "dataset-label");
        dataCell = row.addCell();

        String fileInfo;
        try {
            Bitstream mainFile = dryadDataFile.getFirstBitstream();
            fileInfo = mainFile.getName() + " (" + FileUtils.byteCountToDisplaySize(mainFile.getSize()) + ")";
        } catch (Exception e) {
            fileInfo = dryadDataFile.getItem().getName();
        }
        labelCell.addContent("Filename");
        dataCell.addXref(HandleManager.resolveToURL(context, dataset.getHandle()), fileInfo);

        // readme
        Bitstream readme = dryadDataFile.getREADME();
        if (readme != null) {
            row = table.addRow("dataset-row", null, "dataset-row");
            actionCell = row.addCell("dataset-action",null, 0, 0, "dataset-action");
            labelCell = row.addCell("dataset-label",null, 0, 0, "dataset-label");
            dataCell = row.addCell();

            String readmeFileInfo = readme.getName() + " (" + FileUtils.byteCountToDisplaySize(readme.getSize()) + ")";
            labelCell.addContent("README");
            dataCell.addContent(readmeFileInfo);
        }

        // description
        DCValue[] descriptions = dataset.getMetadata("dc", "description", null, org.dspace.content.Item.ANY);
        if (descriptions.length > 0) {
            row = table.addRow("dataset-row", null, "dataset-row");
            actionCell = row.addCell("dataset-action",null, 0, 0, "dataset-action");
            labelCell = row.addCell("dataset-label",null, 0, 0, "dataset-label");
            dataCell = row.addCell("dataset-description",null, 0, 0, "dataset-description");

            labelCell.addContent("Description");
            dataCell.addHighlight("dataset-description").addContent(descriptions[0].value);
        }

        // date added
        DCValue[] provenances = dataset.getMetadata("dc", "description", "provenance", org.dspace.content.Item.ANY);
        for (DCValue provenance : provenances) {
            Matcher uploadMatcher = Pattern.compile("File was uploaded at (.+)").matcher(provenance.value);
            if (uploadMatcher.matches()) {
                String uploadDate = uploadMatcher.group(1);
                row = table.addRow("dataset-row", null, "dataset-row");
                actionCell = row.addCell("dataset-action",null, 0, 0, "dataset-action");
                labelCell = row.addCell("dataset-label",null, 0, 0, "dataset-label");
                dataCell = row.addCell();

                labelCell.addContent("Date added");
                dataCell.addHighlight("dataset-date").addContent(uploadDate);
                break;
            }
        }


        return submissionNotFinished;
    }
}
