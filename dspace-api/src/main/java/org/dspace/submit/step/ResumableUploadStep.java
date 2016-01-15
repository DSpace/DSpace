package org.dspace.submit.step;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.submit.AbstractProcessingStep;

public class ResumableUploadStep extends AbstractProcessingStep{
    
    private static Logger log = Logger.getLogger(ResumableUploadStep.class);

    @Override
    public int doProcessing(Context context, HttpServletRequest request, HttpServletResponse response,
            SubmissionInfo subInfo) throws ServletException, IOException, SQLException, AuthorizeException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getNumberOfPages(HttpServletRequest request, SubmissionInfo subInfo) throws ServletException {
        // TODO Auto-generated method stub
        return 1;
    }
}
