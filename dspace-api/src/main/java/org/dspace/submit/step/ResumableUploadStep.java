package org.dspace.submit.step;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

public class ResumableUploadStep extends UploadStep{
    private static Logger log = Logger.getLogger(ResumableUploadStep.class);
    private static final boolean fileRequired = 
            ConfigurationManager.getBooleanProperty("webui.submit.upload.required", true);
    
    @SuppressWarnings("unchecked")
    @Override
    public int doProcessing(
            Context context,
            HttpServletRequest request,
            HttpServletResponse response,
            SubmissionInfo subInfo) throws ServletException, IOException, SQLException, AuthorizeException {
        int status = UploadStep.STATUS_COMPLETE;
        
        if(request.getParameter("resumable") != null && Boolean.parseBoolean(request.getParameter("resumable"))){ 
            for (Enumeration<Object> e = request.getParameterNames(); e.hasMoreElements();){
                String key = e.nextElement().toString();
                if(key.startsWith("description-")){
                    String val = request.getParameter(key.toString()); 
                    log.info(key + " : " + val);
                    if(val != null && val.length() > 0){
                        int bistreamId = Integer.parseInt(key.split("-")[1]);

                        Bitstream b = Bitstream.find(context, bistreamId);
                        b.setDescription(val);
                        b.update();
                    }
                }
                else if(key.equals("primary_bitstream_id")){
                    Item item = subInfo.getSubmissionItem().getItem();
                    Bundle[] bundles = item.getBundles("ORIGINAL");
                    if (bundles.length > 0){
                        bundles[0].setPrimaryBitstreamID(Integer.valueOf(request.getParameter(key.toString())).intValue());
                        bundles[0].update();
                    }
                }
            }

            String buttonPressed = Util.getSubmitButton(request, NEXT_BUTTON);
            Item item = subInfo.getSubmissionItem().getItem();
            if (fileRequired && !item.hasUploadedFiles()
                    && !buttonPressed.equals(UploadStep.SUBMIT_MORE_BUTTON))
            {
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
