/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.step;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

/**
 * Resumable Upload step for DSpace. Processes the actual upload of files for an
 * item being submitted into DSpace.
 */
public class ResumableUploadStep extends UploadStep{
    private static final boolean fileRequired = 
            ConfigurationManager.getBooleanProperty("webui.submit.upload.required", true);
    public static final String RESUMABLE_PARAM = "resumable";
    
    /*
     * (non-Javadoc)
     * @see org.dspace.submit.step.UploadStep#doProcessing(org.dspace.core.Context, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, org.dspace.app.util.SubmissionInfo)
     */
    @SuppressWarnings("unchecked")
    @Override
    public int doProcessing(
            Context context,
            HttpServletRequest request,
            HttpServletResponse response,
            SubmissionInfo subInfo) throws ServletException, IOException, SQLException, AuthorizeException
    {
        int status = UploadStep.STATUS_COMPLETE;
        boolean next = false;
                
        String resumable = request.getParameter(RESUMABLE_PARAM);
        if(resumable != null && Boolean.parseBoolean(resumable))
        { 
            for (Enumeration<Object> e = request.getParameterNames(); e.hasMoreElements();)
            {
                String key = e.nextElement().toString();
                if(key.startsWith("description-"))
                {
                    String val = request.getParameter(key.toString()); 
                    if(val != null && val.length() > 0)
                    {
                        int bistreamId = Integer.parseInt(key.split("-")[1]);

                        Bitstream b = Bitstream.find(context, bistreamId);
                        b.setDescription(val);
                        b.update();
                    }
                }
                else if(key.equals("primary_bitstream_id"))
                {
                    Item item = subInfo.getSubmissionItem().getItem();
                    Bundle[] bundles = item.getBundles("ORIGINAL");
                    if (bundles.length > 0)
                    {
                        bundles[0].setPrimaryBitstreamID(Integer.valueOf(request.getParameter(key.toString())).intValue());
                        bundles[0].update();
                    }
                }
                else if(key.equals(NEXT_BUTTON)){
                    next = true;
                }
            }

            Item item = subInfo.getSubmissionItem().getItem();
            if (fileRequired && next && !item.hasUploadedFiles()){
                // if next has been chosen check files have been uploaded
                status = UploadStep.STATUS_NO_FILES_ERROR;
            }
        }
        else{
            // client is using traditional upload form, pass request onto base class
            status = super.doProcessing(context, request, response, subInfo);
        }
        
        return status;
    }
}
