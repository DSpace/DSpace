package ar.edu.unlp.sedici.aspect.extraSubmission.submit;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.LicenseUtils;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.license.CreativeCommons;
import org.dspace.submit.AbstractProcessingStep;

@Deprecated /* No se usa más desde #2183. Se deja como ejemplo de step*/
public class AutomaticIssuedMetadataStep extends AbstractProcessingStep {


    /** log4j logger */
    private static Logger log = Logger.getLogger(AutomaticIssuedMetadataStep.class);


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
        // get reference to item
        Item item = subInfo.getSubmissionItem().getItem();

        //Si no tiene cargado el metadato lo cargo
        if (item.getMetadata("dc", "date", "issued", null).length == 0){
        	
        	//recupero sedici.date.exposure
        	DCValue[] exposure=item.getMetadata("sedici", "date", "exposure", null);
        	//si exposure esta cargado lo copio en date issued
        	if (exposure.length !=0){
        		item.addMetadata("dc", "date", "issued", null, exposure[0].value);
        	};
        	
            // Save changes to database
            subInfo.getSubmissionItem().update();

            // gaurdo los cambios
            context.commit();
        };
        // completed without errors
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
        return 1;

    }

}

