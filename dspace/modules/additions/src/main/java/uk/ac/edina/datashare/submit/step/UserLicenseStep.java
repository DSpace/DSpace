package uk.ac.edina.datashare.submit.step;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.license.CreativeCommons;
import org.dspace.submit.step.LicenseStep;

import uk.ac.edina.datashare.utils.Consts;
import uk.ac.edina.datashare.utils.DSpaceUtils;
import uk.ac.edina.datashare.utils.MetaDataUtil;
import uk.ac.edina.datashare.utils.UserLicense;

/**
 * Process user license step. The actual processing in conditional of the type
 * of license the user selects.
 * 
 * 1) No License - ensure rights has been populated.
 * 2) Open Data License - ensure license has been accepted.
 * 3) ODC Attribution License - ensure license has been accepted.
 * 4) Creative Commons - ensure license has been chosen.
 */
public class UserLicenseStep extends LicenseStep
{
	//private Logger LOG = Logger.getLogger(UserLicenseStep.class);
	
    /*
     * (non-Javadoc)
     * @see org.dspace.submit.AbstractProcessingStep#doProcessing(org.dspace.core.Context, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, org.dspace.app.util.SubmissionInfo)
     */
    @Override
    public int doProcessing(
            Context context,
            HttpServletRequest request,
            HttpServletResponse response,
            SubmissionInfo subInfo) throws ServletException, IOException, SQLException, AuthorizeException
    {
        int retVal = 0; 
        
        Object ob = request.getParameter(Consts.USER_LICENSE_CONTROL);
        if(ob != null)
        {
            DSpaceUtils.setUserLicenseType(subInfo, ob.toString());
        }

        UserLicense license = DSpaceUtils.getUserLicenseType(subInfo);
        switch(license)
        {
            case NO_LICENSE:
            {
                // no license chosen - force rights statement
                retVal = doRightsStatement(context, request, subInfo);
                break;
            }
            case CREATIVE_COMMONS:
            {
                retVal = doCreativeCommons(context, request, subInfo);
                break;
            }
            case CREATIVE_COMMONS_BY:
            {
                retVal = doCreativeCommonsBy(context, request, subInfo);
                break;
            }
            case OPEN_DATA_COMMONS:
            {
                retVal = doOpenDataCommons(context, request, subInfo);
                break;
            }
            case ODC_ATTRIBUTION:
            {
                retVal = doOdcAttribution(context, request, subInfo);
                break;  
            }
            default:
            {
                throw new RuntimeException("Unknown license: " + license);
            }
        }
        
        subInfo.getSubmissionItem().update();
        context.commit();
        
        return retVal;
    }
    
    /**
     * Process rights statement when no license has been selected.
     * @param request HTTP request.
     * @param subInfo DSpace submission info object.
     * @return submission status.
     */
    private int doRightsStatement(
    		Context context,
            HttpServletRequest request,
            SubmissionInfo subInfo)
    {
        int retVal = STATUS_COMPLETE;
        String rightStatement = request.getParameter(Consts.RIGHTS_STATEMENT);
        String buttonPressed = Util.getSubmitButton(request, NEXT_BUTTON);
        
        if(buttonPressed.equals(NEXT_BUTTON) &&
                rightStatement.length() == 0)
        {
        	MetaDataUtil.setRights(subInfo, null);
            retVal = Consts.MANDITORY_FIELD;
        }
        else
        {
            // ok now populate the metadata
            MetaDataUtil.setRights(subInfo, rightStatement);
            Item item = subInfo.getSubmissionItem().getItem();
            
            try{
            	CreativeCommons.removeLicense(context, item);
            }
            catch(Exception ex){
            	throw new RuntimeException(ex);
            }
        }
        
        return retVal;
    }
    
    /**
     * Process creative commons license.
     * @param context The DSpace context.
     * @param request HTTP request.
     * @param subInfo DSpace submission info object.
     * @return submission status.
     */
    private int doCreativeCommons(
            Context context,
            HttpServletRequest request,
            SubmissionInfo subInfo)
    {
        int retVal = STATUS_COMPLETE;
        
        String buttonPressed = Util.getSubmitButton(request, NEXT_BUTTON);
        
        try
        {
            Item item = subInfo.getSubmissionItem().getItem();
            
            // do DSpace creative commons step
            CreateCommonsStep step = new CreateCommonsStep();
            step.doCreativeCommons(context, request, subInfo);
            
            // if next is selected then there must be a license attached
            if(buttonPressed.equals(NEXT_BUTTON) &&
                    (!CreativeCommons.hasLicense(context, item)))
            {
                retVal = Consts.NO_LICENSE;
            }
        }
        catch(Exception ex)
        {
            throw new RuntimeException(ex);
        }
        
        return retVal;
    }
    
    /**
     * Process ODC Attribution license.
     * @param context The DSpace context.
     * @param request HTTP request.
     * @param subInfo DSpace submission info object.
     * @return submission status.
     */
    private int doOdcAttribution(
            Context context,
            HttpServletRequest request,
            SubmissionInfo subInfo)
    {
        return doOpenLicense(context, request, subInfo, Consts.ODC_ATTRIBUTION_LICENCE);
    }
    
    /**
     * Process create commons by attribution license.
     * @param context The DSpace context.
     * @param request HTTP request.
     * @param subInfo DSpace submission info object.
     * @return submission status.
     */
    private int doCreativeCommonsBy(
            Context context,
            HttpServletRequest request,
            SubmissionInfo subInfo)
    {
        return doOpenLicense(context, request, subInfo, Consts.CREATIVE_COMMONS_BY_LICENCE);
    }
    
    /**
     * Process open data license.
     * @param context The DSpace context.
     * @param request HTTP request.
     * @param subInfo DSpace submission info object.
     * @return submission status.
     */
    private int doOpenDataCommons(
            Context context,
            HttpServletRequest request,
            SubmissionInfo subInfo)
    {
        return doOpenLicense(context, request, subInfo, Consts.OPEN_DATA_LICENCE);
    }
    
    /**
     * Process generic open data license.
     * @param context The DSpace context.
     * @param request HTTP request.
     * @param subInfo DSpace submission info object.
     * @param licenseFile The full path string of the license text.
     * @return submission status.
     */
    private int doOpenLicense(
            Context context,
            HttpServletRequest request,
            SubmissionInfo subInfo,
            String licenseFile)
    {
        int retVal = STATUS_COMPLETE;
        
        String buttonPressed = Util.getSubmitButton(request, NEXT_BUTTON);
        
        if(buttonPressed.equals(NEXT_BUTTON))
        {
        	try
        	{
        		Item item = subInfo.getSubmissionItem().getItem();
                    
        		// make sure there are no other licenses
        		CreativeCommons.removeLicense(context, item);
        		MetaDataUtil.clearRights(item);
        		
        		// now insert license
        		CreativeCommons.setLicense(
        				context,
        				item,
        				new FileInputStream(licenseFile),
        				"text/plain");
        	}
        	catch(Exception ex)
        	{
        		throw new RuntimeException(ex);
        	}
        }
        
        return retVal; 
    }
    
    /*
     * (non-Javadoc)
     * @see org.dspace.submit.AbstractProcessingStep#getNumberOfPages(javax.servlet.http.HttpServletRequest, org.dspace.app.util.SubmissionInfo)
     */
    @Override
    public int getNumberOfPages(HttpServletRequest request, SubmissionInfo subInfo) throws ServletException
    {
        return 1;
    }
    
    /**
     * Local License step implementation.
     */
    private class CreateCommonsStep extends org.dspace.submit.step.CCLicenseStep
    {
        public int doCreativeCommons(
                Context context,
                HttpServletRequest request,
                SubmissionInfo subInfo) throws ServletException, IOException, SQLException,
                AuthorizeException
        {
            return this.processCC(context, request, null, subInfo);
        }
    }
}
