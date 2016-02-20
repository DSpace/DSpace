/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package es.arvo.dspace.submit.step;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.core.ConfigurationManager;
import org.dspace.submit.AbstractProcessingStep;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.RevisionToken;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.handle.HandleManager;
import org.dspace.workflow.WorkflowManager;
import org.dspace.xmlworkflow.XmlWorkflowManager;

import com.ibm.icu.text.SimpleDateFormat;

/**
 * This is the class which defines what happens once a submission completes!
 * <P>
 * This class performs all the behind-the-scenes processing that
 * this particular step requires.  This class's methods are utilized 
 * by both the JSP-UI and the Manakin XML-UI
 * <P>
 * This step is non-interactive (i.e. no user interface), and simply performs
 * the processing that is necessary after a submission has been completed!
 * 
 * @see org.dspace.app.util.SubmissionConfig
 * @see org.dspace.app.util.SubmissionStepConfig
 * @see org.dspace.submit.AbstractProcessingStep
 * 
 * @author Tim Donohue
 * @version $Revision$
 */
public class CompleteStep extends AbstractProcessingStep
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(CompleteStep.class);

    private static SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
    
    public static String REVISED="yes ok 1 true";
    public static String NOT_REVISED="no ko 0 false";
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
        // The Submission is COMPLETE!!
        log.info(LogManager.getHeader(context, "submission_complete",
                "Completed submission with id="
                        + subInfo.getSubmissionItem().getID()));

        // Start the workflow for this Submission
        boolean success = false;
     
        String token=request.getParameter("tokenEvaluacion");
        String workspaceID=""+subInfo.getSubmissionItem().getID();
        RevisionToken revisionToken=RevisionToken.verificaTokenWorkspaceID(context, token, workspaceID);
        if(revisionToken!=null){
            context.turnOffAuthorisationSystem();
            EPerson eperson=EPerson.findByEmail(context, ConfigurationManager.getProperty("openaire.eperson.evaluaciones"));
            subInfo.getSubmissionItem().getItem().setSubmitter(eperson);
            context.setCurrentUser(eperson);
        }
        try
        {
            if(ConfigurationManager.getProperty("workflow","workflow.framework").equals("xmlworkflow")){
                try{
                    XmlWorkflowManager.start(context, (WorkspaceItem) subInfo.getSubmissionItem());
                }catch (Exception e){
                    log.error(LogManager.getHeader(context, "Error while starting xml workflow", "Item id: " + subInfo.getSubmissionItem().getItem().getID()), e);
                    throw new ServletException(e);
                }
            }else{
                WorkflowManager.start(context, (WorkspaceItem) subInfo.getSubmissionItem());
            }
            success = true;
        }
        catch (Exception e)
        {
            log.error("Caught exception in submission step: ",e);
            throw new ServletException(e);
        }
        finally
        {
        // commit changes to database
            if (success)
            {
        	//ARVO: Actualizamos el revision_token
        	if(revisionToken!=null){
        	    RevisionToken revToken=RevisionToken.find(context, token);
        	    revToken.setRevisionId(""+subInfo.getSubmissionItem().getItem().getID());
        	    revToken.update(context);
        	}
                context.commit();
            }
            else
            {
                context.getDBConnection().rollback();
            }
        }
     // ARVO: si hay token y estamos en el item correspondiente te deja subir archivos. 

        
        EPerson previousUser=context.getCurrentUser();
        
  
        if(revisionToken!=null){
            // Añadimos a la revision datos obligatorios sobre el item:
            // ADAN: Ñapa para que los enlaces sean clicakbles en la demo 
            Item itemRevisado=(Item) HandleManager.resolveToObject(context, revisionToken.getHandleRevisado());
            if(itemRevisado!=null && itemRevisado.getType()==Constants.ITEM){
        	if(revisionToken.getTipo().equalsIgnoreCase("R")){
        	    subInfo.getSubmissionItem().getItem().addMetadata("dc", "title", null,Item.ANY,(itemRevisado).getMetadata("dc.title")+" (Revisión de)");
        	    //                        subInfo.getSubmissionItem().getItem().addMetadata("dc", "relation", "isrelatedtoItem",Item.ANY, ConfigurationManager.getProperty("handle.canonical.prefix")+revisionToken.getHandleRevisado());
        	    //                        itemRevisado.addMetadata("dc", "relation", "isrelatedtoRevision",Item.ANY, ConfigurationManager.getProperty("handle.canonical.prefix")+ subInfo.getSubmissionItem().getItem().getHandle());
        	    subInfo.getSubmissionItem().getItem().addMetadata("dc", "relation", "isrelatedtoItem",Item.ANY, ConfigurationManager.getProperty("dspace.url")+"/"+revisionToken.getHandleRevisado());
        	    subInfo.getSubmissionItem().getItem().addMetadata("dc", "date", "issued",Item.ANY,sdf.format(new Date()));
        	    
        	    itemRevisado.addMetadata("dc", "relation", "isrelatedtoRevision",Item.ANY, ConfigurationManager.getProperty("dspace.url")+"/"+ subInfo.getSubmissionItem().getItem().getHandle());

        	    itemRevisado.clearMetadata("oprm", "item", "hasRevision", Item.ANY);
        	    itemRevisado.addMetadata("oprm", "item", "hasRevision",Item.ANY, REVISED);

        	}else if(revisionToken.getTipo().equalsIgnoreCase("J")){
        	    //            		subInfo.getSubmissionItem().getItem().addMetadata("dc", "title", null,Item.ANY,(itemRevisado).getMetadata("dc.title")+" (Juicio de)");
        	    //            		subInfo.getSubmissionItem().getItem().addMetadata("dc", "relation", "isrelatedtoRevision",Item.ANY, ConfigurationManager.getProperty("handle.canonical.prefix")+revisionToken.getHandleRevisado());
        	    subInfo.getSubmissionItem().getItem().addMetadata("dc", "title", null,Item.ANY,(itemRevisado).getMetadata("dc.title")+" (Juicio de)");
        	    subInfo.getSubmissionItem().getItem().addMetadata("dc", "relation", "isrelatedtoRevision",Item.ANY, ConfigurationManager.getProperty("dspace.url")+"/"+revisionToken.getHandleRevisado());
        	    subInfo.getSubmissionItem().getItem().addMetadata("dc", "date", "issued",Item.ANY,sdf.format(new Date()));
        	    itemRevisado.addMetadata("dc", "relation", "isrelatedtoJuicio",Item.ANY, ConfigurationManager.getProperty("dspace.url")+"/"+subInfo.getSubmissionItem().getItem().getHandle());
        	} 
        	itemRevisado.update();
        	subInfo.getSubmissionItem().getItem().update();
            }
        }else{
            subInfo.getSubmissionItem().getItem().clearMetadata("oprm", "item", "hasRevision", Item.ANY);
            subInfo.getSubmissionItem().getItem().addMetadata("oprm", "item", "hasRevision",Item.ANY, NOT_REVISED);
        }
      //ARVO: Restauramos la autorizacion
        if(revisionToken!=null){
            context.setCurrentUser(previousUser);
            context.restoreAuthSystemState();
        }
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
        // This class represents the non-interactive processing step
        // that occurs just *before* the final confirmation page!
        // (so it should only be processed once!)
        return 1;
    }
}
