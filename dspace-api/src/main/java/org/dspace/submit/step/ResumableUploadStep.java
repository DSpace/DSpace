package org.dspace.submit.step;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.core.Context;
import org.dspace.submit.AbstractProcessingStep;

public class ResumableUploadStep extends AbstractProcessingStep{
    
    private static Logger log = Logger.getLogger(ResumableUploadStep.class);

    @SuppressWarnings("unchecked")
    @Override
    public int doProcessing(Context context, HttpServletRequest request, HttpServletResponse response,
            SubmissionInfo subInfo) throws ServletException, IOException, SQLException, AuthorizeException {
        
        for (Enumeration<Object> e = request.getParameterNames(); e.hasMoreElements();){
            String key = e.nextElement().toString();
            log.info("---");
            log.info(key);
            
            if(key.startsWith("description-")){
                log.info("*");

                String val = request.getParameter(key.toString());
                if(val != null && val.length() > 0){
                    log.info(request.getParameter(val));
                    log.info(key.split("-")[1]);
                    int bistreamId = Integer.parseInt(key.split("-")[1]);

                    Bitstream b = Bitstream.find(context, bistreamId);
                    b.setDescription(val);

                    b.update();
                }
            }
        }

        return 0;
    }

    @Override
    public int getNumberOfPages(HttpServletRequest request, SubmissionInfo subInfo) throws ServletException {
        // TODO Auto-generated method stub
        return 1;
    }
}
