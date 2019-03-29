/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.submission.submit;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.SourceResolver;
import org.dspace.app.util.Util;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.*;

/**
 * This class manages the upload step with embargo fields during the submission.
 * Edit submission.xml to enable it.
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 *
 */
public class UploadWithEmbargoStep extends UploadStep
{
	/** Language Strings for Uploading **/
    protected static final Message T_head = 
        message("xmlui.Submission.submit.UploadWithEmbargoStep.head");
    protected static final Message T_file = 
        message("xmlui.Submission.submit.UploadWithEmbargoStep.file");
    protected static final Message T_file_help = 
        message("xmlui.Submission.submit.UploadWithEmbargoStep.file_help");
    protected static final Message T_file_error = 
        message("xmlui.Submission.submit.UploadWithEmbargoStep.file_error");
    protected static final Message T_upload_error =
        message("xmlui.Submission.submit.UploadWithEmbargoStep.upload_error");

    protected static final Message T_virus_checker_error =
        message("xmlui.Submission.submit.UploadWithEmbargoStep.virus_checker_error");
    protected static final Message T_virus_error =
        message("xmlui.Submission.submit.UploadWithEmbargoStep.virus_error");

    protected static final Message T_description = 
        message("xmlui.Submission.submit.UploadWithEmbargoStep.description");
    protected static final Message T_description_help = 
        message("xmlui.Submission.submit.UploadWithEmbargoStep.description_help");
    protected static final Message T_submit_upload = 
        message("xmlui.Submission.submit.UploadWithEmbargoStep.submit_upload");
    protected static final Message T_head2 = 
        message("xmlui.Submission.submit.UploadWithEmbargoStep.head2");
    protected static final Message T_column0 = 
        message("xmlui.Submission.submit.UploadWithEmbargoStep.column0");
    protected static final Message T_column1 = 
        message("xmlui.Submission.submit.UploadWithEmbargoStep.column1");
    protected static final Message T_column2 = 
        message("xmlui.Submission.submit.UploadWithEmbargoStep.column2");
    protected static final Message T_column3 = 
        message("xmlui.Submission.submit.UploadWithEmbargoStep.column3");
    protected static final Message T_column4 = 
        message("xmlui.Submission.submit.UploadWithEmbargoStep.column4");
    protected static final Message T_column5 = 
        message("xmlui.Submission.submit.UploadWithEmbargoStep.column5");
    protected static final Message T_column6 = 
        message("xmlui.Submission.submit.UploadWithEmbargoStep.column6");
    protected static final Message T_unknown_name = 
        message("xmlui.Submission.submit.UploadWithEmbargoStep.unknown_name");
    protected static final Message T_unknown_format = 
        message("xmlui.Submission.submit.UploadWithEmbargoStep.unknown_format");
    protected static final Message T_supported = 
        message("xmlui.Submission.submit.UploadWithEmbargoStep.supported");
    protected static final Message T_known = 
        message("xmlui.Submission.submit.UploadWithEmbargoStep.known");
    protected static final Message T_unsupported = 
        message("xmlui.Submission.submit.UploadWithEmbargoStep.unsupported");
    protected static final Message T_submit_edit =
        message("xmlui.Submission.submit.UploadWithEmbargoStep.submit_edit");

    protected static final Message T_submit_policy =
            message("xmlui.Submission.submit.UploadWithEmbargoStep.submit_policy");

    protected static final Message T_checksum = 
        message("xmlui.Submission.submit.UploadWithEmbargoStep.checksum");
    protected static final Message T_submit_remove = 
        message("xmlui.Submission.submit.UploadWithEmbargoStep.submit_remove");

    /**
     * Global reference to edit file page
     * (this is used when a user requests to edit a bitstream)
     **/
    private EditFileStep editFile = null;

    private EditBitstreamPolicies editBitstreamPolicies = null;

    private EditPolicyStep editPolicy = null;

    private boolean isAdvancedFormEnabled=true;
    
    /**
	 * Establish our required parameters, abstractStep will enforce these.
	 */
	public UploadWithEmbargoStep()
	{
		this.requireSubmission = true;
		this.requireStep = true;
	}


    /**
     * Check if user has requested to edit information about an
     * uploaded file
     */
    public void setup(SourceResolver resolver, Map objectModel, String src, Parameters parameters) 
    throws ProcessingException, SAXException, IOException
    { 
        super.setup(resolver,objectModel,src,parameters);
               
        // If this page for editing an uploaded file's information
        // was requested, then we need to load EditFileStep instead!
        if(this.errorFlag==org.dspace.submit.step.UploadWithEmbargoStep.STATUS_EDIT_BITSTREAM)
        {
            this.editFile = new EditFileStep();
            this.editFile.setup(resolver, objectModel, src, parameters);   
        }
        else if(this.errorFlag==org.dspace.submit.step.UploadWithEmbargoStep.STATUS_EDIT_POLICIES
                || this.errorFlag==org.dspace.submit.step.UploadWithEmbargoStep.STATUS_EDIT_POLICIES_ERROR_SELECT_GROUP
                  || this.errorFlag==org.dspace.submit.step.UploadWithEmbargoStep.STATUS_EDIT_POLICIES_DUPLICATED_POLICY){
            this.editBitstreamPolicies = new EditBitstreamPolicies();
            this.editBitstreamPolicies.setup(resolver, objectModel, src, parameters);
        }
        else if(this.errorFlag==org.dspace.submit.step.AccessStep.STATUS_EDIT_POLICY
                || this.errorFlag==org.dspace.submit.step.UploadWithEmbargoStep.STATUS_EDIT_POLICY_ERROR_SELECT_GROUP
                 || this.errorFlag==org.dspace.submit.step.UploadWithEmbargoStep.STATUS_EDIT_POLICY_DUPLICATED_POLICY){
            this.editPolicy = new EditPolicyStep();
            this.editPolicy.setup(resolver, objectModel, src, parameters);
        }
        else
        {
            this.editFile = null;
            editBitstreamPolicies = null;
            editPolicy=null;
        }

        isAdvancedFormEnabled=DSpaceServicesFactory.getInstance().getConfigurationService().getBooleanProperty("webui.submission.restrictstep.enableAdvancedForm", false);
    }

    public void addPageMeta(PageMeta pageMeta) throws WingException, AuthorizeException, IOException, SAXException, SQLException {
        super.addPageMeta(pageMeta);
        pageMeta.addMetadata("javascript", "static").addContent("static/js/accessFormUtil.js");
    }
    
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        // If we are actually editing information of an uploaded file,
        // then display that body instead!
    	if(this.editFile!=null)
        {
            editFile.addBody(body);
            return;
        }
        else if(editBitstreamPolicies!=null && isAdvancedFormEnabled){
            editBitstreamPolicies.addBody(body);
            return;
        }
        else if(editPolicy!=null && isAdvancedFormEnabled){
            editPolicy.addBody(body);
            return;
        }
        
        // Get a list of all files in the original bundle
		Item item = submission.getItem();
		Collection collection = submission.getCollection();
		String actionURL = contextPath + "/handle/"+collection.getHandle() + "/submit/" + knot.getId() + ".continue";
		boolean disableFileEditing = (submissionInfo.isInWorkflow()) && !DSpaceServicesFactory.getInstance().getConfigurationService().getBooleanProperty("workflow.reviewer.file-edit");
        java.util.List<Bundle> bundles = itemService.getBundles(item, "ORIGINAL");
        java.util.List<Bitstream> bitstreams = new ArrayList<>();
		if (bundles.size() > 0)
		{
			bitstreams = bundles.get(0).getBitstreams();
		}
		
		// Part A: 
		//  First ask the user if they would like to upload a new file (may be the first one)
    	Division div = body.addInteractiveDivision("submit-upload", actionURL, Division.METHOD_MULTIPART, "primary submission");
    	div.setHead(T_submission_head);
    	addSubmissionProgressList(div);
    	
    	
    	List upload = null;
    	if (!disableFileEditing)
    	{
    		// Only add the upload capabilities for new item submissions
	    	upload = div.addList("submit-upload-new", List.TYPE_FORM);
	        upload.setHead(T_head);    
	        
	        File file = upload.addItem().addFile("file");
	        file.setLabel(T_file);
	        file.setHelp(T_file_help);
	        file.setRequired();
	        
	        // if no files found error was thrown by processing class, display it!
	        if (this.errorFlag==org.dspace.submit.step.UploadWithEmbargoStep.STATUS_NO_FILES_ERROR)
	        {
                file.addError(T_file_error);
            }

            // if an upload error was thrown by processing class, display it!
            if (this.errorFlag == org.dspace.submit.step.UploadWithEmbargoStep.STATUS_UPLOAD_ERROR)
            {
                file.addError(T_upload_error);
            }

            // if virus checking was attempted and failed in error then let the user know
            if (this.errorFlag == org.dspace.submit.step.UploadWithEmbargoStep.STATUS_VIRUS_CHECKER_UNAVAILABLE)
            {
                file.addError(T_virus_checker_error);
            }

             // if virus checking was attempted and a virus found then let the user know
            if (this.errorFlag == org.dspace.submit.step.UploadWithEmbargoStep.STATUS_CONTAINS_VIRUS)
            {
                file.addError(T_virus_error);
            }
	        	
	        Text description = upload.addItem().addText("description");
	        description.setLabel(T_description);
	        description.setHelp(T_description_help);


            // if AdvancedAccessPolicy=false: add simpleForm in UploadWithEmbargoStep
            if(!isAdvancedFormEnabled){
                AccessStepUtil asu = new AccessStepUtil(context);
                // if the item is embargoed default value will be displayed.
                asu.addEmbargoDateSimpleForm(item, upload, errorFlag);
                asu.addReason(null, upload, errorFlag);
            }

	        Button uploadSubmit = upload.addItem().addButton("submit_upload");
	        uploadSubmit.setValue(T_submit_upload);

    	}

        make_sherpaRomeo_submission(item, div);
        
        // Part B:
        //  If the user has already uploaded files provide a list for the user.
        if (bitstreams.size() > 0 || disableFileEditing)
		{
	        Table summary = div.addTable("submit-upload-summary",(bitstreams.size() * 2) + 2,7);
	        summary.setHead(T_head2);
	        
	        Row header = summary.addRow(Row.ROLE_HEADER);
	        header.addCellContent(T_column0); // primary bitstream
	        header.addCellContent(T_column1); // select checkbox
	        header.addCellContent(T_column2); // file name
	        header.addCellContent(T_column3); // size
	        header.addCellContent(T_column4); // description
	        header.addCellContent(T_column5); // format
	        header.addCellContent(T_column6); // edit button
	        
	        for (Bitstream bitstream : bitstreams)
	        {
                UUID id = bitstream.getID();
	        	String name = bitstream.getName();
	        	String url = makeBitstreamLink(item, bitstream);
	        	long bytes = bitstream.getSizeBytes();
	        	String desc = bitstream.getDescription();
	        	String algorithm = bitstream.getChecksumAlgorithm();
	        	String checksum = bitstream.getChecksum();

	        	
	        	Row row = summary.addRow();

	            // Add radio-button to select this as the primary bitstream
                Radio primary = row.addCell().addRadio("primary_bitstream_id");
                primary.addOption(String.valueOf(id));
                
                // If this bitstream is already marked as the primary bitstream
                // mark it as such.
                if(bundles.get(0).getPrimaryBitstream() != null && bundles.get(0).getPrimaryBitstream().getID().equals(id)) {
                    primary.setOptionSelected(String.valueOf(id));
                }
	        	
	        	if (!disableFileEditing)
	        	{
	        		// Workflow users can not remove files.
		            CheckBox remove = row.addCell().addCheckBox("remove");
		            remove.setLabel("remove");
		            remove.addOption(id.toString());
	        	}
	        	else
	        	{
	        		row.addCell();
	        	}
	        	
	            row.addCell().addXref(url,name);
	            row.addCellContent(bytes + " bytes");
	            if (desc == null || desc.length() == 0)
                {
                    row.addCellContent(T_unknown_name);
                }
	            else
                {
                    row.addCellContent(desc);
                }
	            
                BitstreamFormat format = bitstream.getFormat(context);
	            if (format == null)
	            {
	            	row.addCellContent(T_unknown_format);
	            }
	            else
	            {
                    int support = format.getSupportLevel();
	            	Cell cell = row.addCell();
	            	cell.addContent(format.getMIMEType());
	            	cell.addContent(" ");
	            	switch (support)
	            	{
	            	case 1:
	            		cell.addContent(T_supported);
	            		break;
	            	case 2:
	            		cell.addContent(T_known);
	            		break;
	            	case 3:
	            		cell.addContent(T_unsupported);
	            		break;
	            	}
	            }
	            
	            Button edit = row.addCell().addButton("submit_edit_"+id);
	            edit.setValue(T_submit_edit);

                if(isAdvancedFormEnabled){
                    Button policy = row.addCell().addButton("submit_editPolicy_"+id);
                    policy.setValue(T_submit_policy);
                }

                Row checksumRow = summary.addRow();
	            checksumRow.addCell();
	            Cell checksumCell = checksumRow.addCell(null, null, 0, 6, null);
	            checksumCell.addHighlight("bold").addContent(T_checksum);
	            checksumCell.addContent(" ");
	            checksumCell.addContent(algorithm + ":" + checksum);
	        }
	        
	        if (!disableFileEditing)
	        {
	        	// Workflow users can not remove files.
		        Row actionRow = summary.addRow();
		        actionRow.addCell();
		        Button removeSeleceted = actionRow.addCell(null, null, 0, 6, null).addButton("submit_remove_selected");
		        removeSeleceted.setValue(T_submit_remove);
	        }
	        
	        upload = div.addList("submit-upload-new-part2", List.TYPE_FORM);

		}

        // Part C:
        // add standard control/paging buttons
        addControlButtons(upload);
    }
    
    /** 
     * Each submission step must define its own information to be reviewed
     * during the final Review/Verify Step in the submission process.
     * <P>
     * The information to review should be tacked onto the passed in 
     * List object.
     * <P>
     * NOTE: To remain consistent across all Steps, you should first
     * add a sub-List object (with this step's name as the heading),
     * by using a call to reviewList.addList().  This sublist is
     * the list you return from this method!
     * 
     * @param reviewList
     *      The List to which all reviewable information should be added
     * @return 
     *      The new sub-List object created by this step, which contains
     *      all the reviewable information.  If this step has nothing to
     *      review, then return null!   
     */
    public List addReviewSection(List reviewList) throws SAXException,
        WingException, UIException, SQLException, IOException,
        AuthorizeException
    {
        // Create a new list section for this step (and set its heading)
        List uploadSection = reviewList.addList("submit-review-" + this.stepAndPage, List.TYPE_FORM);
        uploadSection.setHead(T_head);
        
        // Review all uploaded files
        Item item = submission.getItem();
        java.util.List<Bundle> bundles = itemService.getBundles(item, "ORIGINAL");
        java.util.List<Bitstream> bitstreams = new ArrayList<>();
        if (bundles.size() > 0)
        {
            bitstreams = bundles.get(0).getBitstreams();
        }
        
        for (Bitstream bitstream : bitstreams)
        {
            BitstreamFormat bitstreamFormat = bitstream.getFormat(context);
            
            String name = bitstream.getName();
            String url = makeBitstreamLink(item, bitstream);
            String format = bitstreamFormat.getShortDescription();
            Message support = ReviewStep.T_unknown;
            if (bitstreamFormat.getSupportLevel() == BitstreamFormat.KNOWN)
            {
                support = T_known;
            }
            else if (bitstreamFormat.getSupportLevel() == BitstreamFormat.SUPPORTED)
            {
                support = T_supported;
            }
            
            org.dspace.app.xmlui.wing.element.Item file = uploadSection.addItem();
            file.addXref(url,name);
            file.addContent(" - "+ format + " ");
            file.addContent(support);
            
        }
        
        // return this new "upload" section
        return uploadSection;
    }

    /**
     * Returns canonical link to a bitstream in the item.
     *
     * @param item The DSpace Item that the bitstream is part of
     * @param bitstream The bitstream to link to
     * @returns a String link to the bitstream
     */
    private String makeBitstreamLink(Item item, Bitstream bitstream)
    {
        String name = bitstream.getName();
        StringBuilder result = new StringBuilder(contextPath);
        result.append("/bitstream/item/").append(String.valueOf(item.getID()));
        // append name although it isn't strictly necessary
        try
        {
            if (name != null)
            {
                result.append("/").append(Util.encodeBitstreamName(name, "UTF-8"));
            }
        }
        catch (UnsupportedEncodingException uee)
        {
            // just ignore it, we don't have to have a pretty
            // name on the end of the url because the sequence id will
            // locate it. However it means that links in this file might
            // not work....
        }
        result.append("?sequence=").append(String.valueOf(bitstream.getSequenceID()));
        return result.toString();
    }
}
        
