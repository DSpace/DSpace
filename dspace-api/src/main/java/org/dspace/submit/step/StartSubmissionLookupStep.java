/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.step;

import gr.ekt.bte.core.Record;
import gr.ekt.bte.core.TransformationEngine;
import gr.ekt.bte.core.TransformationSpec;
import gr.ekt.bte.exceptions.BadTransformationSpec;
import gr.ekt.bte.exceptions.MalformedSourceException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.FormatIdentifier;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.submit.AbstractProcessingStep;
import org.dspace.submit.lookup.DSpaceWorkspaceItemOutputGenerator;
import org.dspace.submit.lookup.SubmissionItemDataLoader;
import org.dspace.submit.lookup.SubmissionLookupService;
import org.dspace.submit.util.ItemSubmissionLookupDTO;
import org.dspace.submit.util.SubmissionLookupDTO;
import org.dspace.submit.util.SubmissionLookupPublication;
import org.dspace.utils.DSpace;

/**
 * StartSubmissionLookupStep is used when you want enabled the user to auto fill
 * the item in submission with metadata retrieved from external bibliographic
 * services (like pubmed, arxiv, and so on...)
 * 
 * <p>
 * At the moment this step is only available for JSPUI
 * </p>
 * 
 * @see org.dspace.app.util.SubmissionConfig
 * @see org.dspace.app.util.SubmissionStepConfig
 * @see org.dspace.submit.AbstractProcessingStep
 * 
 * @author Andrea Bollini
 * @author Kostas Stamatis
 * @author Luigi Andrea Pascarelli
 * @author Panagiotis Koutsourakis
 * @version $Revision$
 */
public class StartSubmissionLookupStep extends AbstractProcessingStep
{
    /***************************************************************************
     * STATUS / ERROR FLAGS (returned by doProcessing() if an error occurs or
     * additional user interaction may be required)
     * 
     * (Do NOT use status of 0, since it corresponds to STATUS_COMPLETE flag
     * defined in the JSPStepManager class)
     **************************************************************************/
    // no collection was selected
    public static final int STATUS_NO_COLLECTION = 1;

    // invalid collection or error finding collection
    public static final int STATUS_INVALID_COLLECTION = 2;

    public static final int STATUS_NO_SUUID = 3;

    public static final int STATUS_SUBMISSION_EXPIRED = 4;

    private SubmissionLookupService slService = new DSpace()
            .getServiceManager().getServiceByName(
                    SubmissionLookupService.class.getCanonicalName(),
                    SubmissionLookupService.class);

    /** log4j logger */
    private static Logger log = Logger
            .getLogger(StartSubmissionLookupStep.class);

    /**
     * Do any processing of the information input by the user, and/or perform
     * step processing (if no user interaction required)
     * <P>
     * It is this method's job to save any data to the underlying database, as
     * necessary, and return error messages (if any) which can then be processed
     * by the appropriate user interface (JSP-UI or XML-UI)
     * <P>
     * NOTE: If this step is a non-interactive step (i.e. requires no UI), then
     * it should perform *all* of its processing in this method!
     * 
     * @param context
     *            current DSpace context
     * @param request
     *            current servlet request object
     * @param response
     *            current servlet response object
     * @param subInfo
     *            submission info object
     * @return Status or error flag which will be processed by
     *         doPostProcessing() below! (if STATUS_COMPLETE or 0 is returned,
     *         no errors occurred!)
     */
    public int doProcessing(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
    	context.turnOffItemWrapper();
        // First we find the collection which was selected
        int id = Util.getIntParameter(request, "collectionid");
        String titolo = request.getParameter("search_title");
        String date = request.getParameter("search_year");
        String autori = request.getParameter("search_authors");
        String uuidSubmission = request.getParameter("suuid");
        String uuidLookup = request.getParameter("iuuid");
        String fuuidLookup = request.getParameter("fuuid");
        String fPath = request.getParameter("filePath");
        String fName = StringUtils.isNotBlank(request.getParameter("filename") )? request.getParameter("filename"): StringUtils.substringAfterLast(fPath, FileSystems.getDefault().getSeparator());
        
        String uuid_batch = request.getParameter("iuuid_batch");
        
        boolean forceEmpty = Util.getBoolParameter(request, "forceEmpty");
        
        ItemSubmissionLookupDTO itemLookup = null;
        SubmissionLookupDTO submissionDTO = null;
        
        if (!forceEmpty) {
	        if (StringUtils.isBlank(uuidSubmission))
	        {
	            return STATUS_NO_SUUID;
	        }
	
	        submissionDTO = slService.getSubmissionLookupDTO(
	                request, uuidSubmission);
	
	        if (submissionDTO == null)
	        {
	            return STATUS_SUBMISSION_EXPIRED;
	        }
	
	        if (fuuidLookup == null || fuuidLookup.isEmpty())
	        {
	            if (StringUtils.isNotBlank(uuidLookup))
	            {
	                itemLookup = submissionDTO.getLookupItem(uuidLookup);
	                if (itemLookup == null)
	                {
	                    return STATUS_SUBMISSION_EXPIRED;
	                }
	            }
	        }
        }
        
        // if the user didn't select a collection,
        // send him/her back to "select a collection" page
        if (id < 0)
        {
            return STATUS_NO_COLLECTION;
        }

        // try to load the collection
        Collection col = Collection.find(context, id);

        // Show an error if the collection is invalid
        if (col == null)
        {
            return STATUS_INVALID_COLLECTION;
        }
        else
        {
            // create our new Workspace Item
            DCInputSet inputSet = null;
            try
            {
                inputSet = new DCInputsReader().getInputs(col.getHandle());
            }
            catch (Exception e)
            {
                log.error(e.getMessage(), e);
                throw new RuntimeException(e);
            }

            List<ItemSubmissionLookupDTO> dto = new ArrayList<ItemSubmissionLookupDTO>();

            if (itemLookup != null)
            {
                dto.add(itemLookup);
            }
            else if (fuuidLookup != null && !fuuidLookup.isEmpty())
            {
                String[] ss = fuuidLookup.split(",");
                for (String s : ss)
                {
                    itemLookup = submissionDTO.getLookupItem(s);
                    if (itemLookup == null)
                    {
                        return STATUS_SUBMISSION_EXPIRED;
                    }
                    dto.add(itemLookup);
                }
            }
            else
            {
                SubmissionLookupPublication manualPub = new SubmissionLookupPublication(
                        SubmissionLookupService.MANUAL_USER_INPUT);
                manualPub.add("title", titolo);
                manualPub.add("year", date);
                manualPub.add("allauthors", autori);

                Enumeration e = request.getParameterNames();

                while (e.hasMoreElements())
                {
                    String parameterName = (String) e.nextElement();
                    String parameterValue = request.getParameter(parameterName);

                    if (parameterName.startsWith("identifier_")
                            && StringUtils.isNotBlank(parameterValue))
                    {
                        manualPub
                                .add(parameterName.substring("identifier_"
                                        .length()), parameterValue);
                    }
                }
                List<Record> publications = new ArrayList<Record>();
                publications.add(manualPub);
                dto.add(new ItemSubmissionLookupDTO(publications));

            }

            List<WorkspaceItem> result = null;

            TransformationEngine transformationEngine = slService
                    .getPhase2TransformationEngine();
            if (transformationEngine != null)
            {
                SubmissionItemDataLoader dataLoader = (SubmissionItemDataLoader) transformationEngine
                        .getDataLoader();
                dataLoader.setDtoList(dto);
                // dataLoader.setProviders()

                DSpaceWorkspaceItemOutputGenerator outputGenerator = (DSpaceWorkspaceItemOutputGenerator) transformationEngine
                        .getOutputGenerator();
                outputGenerator.setCollection(col);
                outputGenerator.setContext(context);
                outputGenerator.setFormName(inputSet.getFormName());
                outputGenerator.setDto(dto.get(0));

                try
                {
                    transformationEngine.transform(new TransformationSpec());
                    result = outputGenerator.getWitems();
                }
                catch (BadTransformationSpec e1)
                {
                    e1.printStackTrace();
                }
                catch (MalformedSourceException e1)
                {
                    e1.printStackTrace();
                }
            }
            
            String[] uuids = StringUtils.split(uuid_batch, ";");
            List<ItemSubmissionLookupDTO> b_dto = new ArrayList<ItemSubmissionLookupDTO>();
            for(int i=0;i<uuids.length;i++){
            	ItemSubmissionLookupDTO isld = submissionDTO.getLookupItem(uuids[i]);
            	b_dto.add(isld);
            }
            if(!b_dto.isEmpty()){
            	
				Thread thread = new ThreadImporter(request, context.getCurrentUser().getID(), b_dto, id,inputSet);
				thread.start();
            	
            }
            
            if (result != null && result.size() > 0)
            {
                // update Submission Information with this Workspace Item
            	WorkspaceItem wsi= result.get(0);
            	if(StringUtils.isNotBlank(fPath) && result.size() == 1){
            		File file = new File(fPath);	
            		if (file.exists()) {
            			Item item = wsi.getItem();
            			Bitstream bit = item.createSingleBitstream(new FileInputStream(file),Constants.DEFAULT_BUNDLE_NAME);
            			bit.setName(fName);
            			bit.setSource(fPath);
            			bit.setFormat(FormatIdentifier.guessFormat(context, bit));
            			bit.update();            			
            			item.update();
                        // save this bitstream to the submission info, as the bitstream we're currently working with
            			subInfo.setBitstream(bit);
            			file.delete();
            		}
            	 }
                subInfo.setSubmissionItem(wsi);
            }

            // commit changes to database
            context.commit();

            // need to reload current submission process config,
            // since it is based on the Collection selected
            subInfo.reloadSubmissionConfig(request);
        }

        slService.invalidateDTOs(request, uuidSubmission);
        // no errors occurred
        return STATUS_COMPLETE;
    }

    /**
     * Retrieves the number of pages that this "step" extends over. This method
     * is used to build the progress bar.
     * <P>
     * This method may just return 1 for most steps (since most steps consist of
     * a single page). But, it should return a number greater than 1 for any
     * "step" which spans across a number of HTML pages. For example, the
     * configurable "Describe" step (configured using input-forms.xml) overrides
     * this method to return the number of pages that are defined by its
     * configuration file.
     * <P>
     * Steps which are non-interactive (i.e. they do not display an interface to
     * the user) should return a value of 1, so that they are only processed
     * once!
     * 
     * @param request
     *            The HTTP Request
     * @param subInfo
     *            The current submission information object
     * 
     * @return the number of pages in this step
     */
    public int getNumberOfPages(HttpServletRequest request,
            SubmissionInfo subInfo) throws ServletException
    {
        // there is always just one page in the "select a collection" step!
        return 1;
    }
    
    class ThreadImporter extends Thread {
    	private HttpServletRequest request;

    	private int epersonid;

    	private int col;
    	
    	DCInputSet inputset;

    	private List<ItemSubmissionLookupDTO> dto;
    	/** log4j logger */
    	private Logger log = Logger.getLogger(ThreadImporter.class);

    	public ThreadImporter(HttpServletRequest request, int epersonid, List<ItemSubmissionLookupDTO> dto, int col, DCInputSet inputset) {
    		this.dto = dto;
    		this.epersonid = epersonid;
    		this.col = col;
    		this.inputset =inputset;
    		this.request = request;
    	}

    	public void run() {
    		Context context = null;
    		try {
    			context = new Context();
    			context.turnOffItemWrapper();
    			EPerson eperson = EPerson.find(context, epersonid);
    			context.setCurrentUser(eperson);
    			SubmissionLookupService slService = new DSpace().getServiceManager().getServiceByName(
    					SubmissionLookupService.class.getCanonicalName(), SubmissionLookupService.class);

    			TransformationEngine transformationEngine = slService.getPhase2TransformationEngine();
    			if (transformationEngine != null) {
    				SubmissionItemDataLoader dataLoader = (SubmissionItemDataLoader) transformationEngine.getDataLoader();
    				dataLoader.setDtoList(dto);
    				// dataLoader.setProviders()

    				DSpaceWorkspaceItemOutputGenerator outputGenerator = (DSpaceWorkspaceItemOutputGenerator) transformationEngine
    						.getOutputGenerator();
    				Collection collection = Collection.find(context, col);
    				outputGenerator.setCollection(collection);
    				outputGenerator.setContext(context);
    				outputGenerator.setFormName(inputset.getFormName());
    				outputGenerator.setDto(dto.get(0));

    				try {
    					transformationEngine.transform(new TransformationSpec());
    					List<WorkspaceItem> witems = outputGenerator.getWitems();
    					String uu = witems.get(0).getItem().getMetadata("dc.title");
    					context.commit();
    					context.complete();
    				} catch (BadTransformationSpec e1) {
    					log.error(e1.getMessage(), e1);
    				} catch (MalformedSourceException e1) {
    					log.error(e1.getMessage(), e1);
    				}
    			}
    		} catch (Exception e) {
    			try {
    				log.error(e.getMessage(), e);
    			} catch (Exception e1) {
    				log.error(e1.getMessage(), e1);
    			}
    		} finally {
    			if (context != null && context.isValid()) {
    				context.abort();
    			}
    		}
    	}
    }
    
}
