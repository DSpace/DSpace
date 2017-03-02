package edu.tamu.dspace.proxylicense;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.LicenseUtils;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.submit.step.LicenseStep;

public class ProxyLicenseStep extends LicenseStep 
{
    // user rejected the license
    public static final int STATUS_LICENSE_REJECTED = 1;
    
    public static final int STATUS_BAD_PROXY_FILE = 2;

    protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
	protected BitstreamFormatService bitstreamFormatService = ContentServiceFactory.getInstance().getBitstreamFormatService();
    
    /** log4j logger */
    private static Logger log = Logger.getLogger(LicenseStep.class);    
    
    /**
     * Process the input from the license page
     * 
     * @param context
     *            current DSpace context
     * @param request
     *            current servlet request object
     * @param response
     *            current servlet response object
     * @param subInfo
     *            submission info object
     * 
     * @return Status or error flag which will be processed by
     *         UI-related code! (if STATUS_COMPLETE or 0 is returned,
     *         no errors occurred!)
     */
    @Override
    public int doProcessing(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        String buttonPressed = Util.getSubmitButton(request, CANCEL_BUTTON);

        // For Manakin:
        // Add the license to the item
        Item item = subInfo.getSubmissionItem().getItem();
        EPerson submitter = context.getCurrentUser();
        
        // Get the full text for the actual licenses
	String defaultLicenseText = LicenseUtils.getLicenseText(context.getCurrentLocale(), subInfo.getSubmissionItem().getCollection(), item, submitter);
	String proxyLicenseText = XmluiProxyLicenseStep.loadLicense("proxy.license");
	String openLicenseText = XmluiProxyLicenseStep.loadLicense("open.license");;
        
        // Accepting the license means checking one of the boxes and clicking Next
	String decision = request.getParameter("decision");
        String licenseType = request.getParameter("license-selector");
        
        // Load the file's path and input stream and description
        String filePath = (String) request.getAttribute("file-path");
        InputStream fileInputStream = (InputStream) request.getAttribute("file-inputstream");
        
	// If the user somehow clicked more than one box, or didn't click any, throw back an error
	if (buttonPressed.equals(NEXT_BUTTON) && 
		(decision == null || licenseType == null || request.getParameterValues("decision").length > 1)) 
	{
	    return STATUS_LICENSE_REJECTED; 
	}
//	This block makes the proxy statement required	
//	else if ((buttonPressed.equals(NEXT_BUTTON) && licenseType.equalsIgnoreCase("proxy")) && 
//		(filePath == null || fileInputStream == null)) {
//	    return STATUS_BAD_PROXY_FILE;
//	}
	else 
	{
	    // Otherwise, it's accepted; it's just a matter of which one
	    log.info(LogManager.getHeader(context, "accept_license", subInfo.getSubmissionLogInfo()));
            // remove any existing DSpace license (just in case the user accepted it previously)
            itemService.removeDSpaceLicense(context, item);
	}
	
	if (buttonPressed.equals(NEXT_BUTTON) && licenseType.equalsIgnoreCase("default")) {
            LicenseUtils.grantLicense(context, item, defaultLicenseText);
            context.commit();
        }
	else if (buttonPressed.equals(NEXT_BUTTON) && licenseType.equalsIgnoreCase("proxy")) 
	{
            LicenseUtils.grantLicense(context, item, proxyLicenseText);
            
            // If Proxy, create the extra file if we have one
            if (filePath != null && fileInputStream != null) 
            {
                List<Bundle> bundles = itemService.getBundles(item,"LICENSE");
                Bitstream bs = bitstreamService.create(context, bundles.get(0),fileInputStream);
                
                List<String> pieces = Arrays.asList(filePath.split("\\."));
                if (pieces.size() == 1)
                    bs.setName(context,"PERMISSION.license");
                else
                    bs.setName(context,"PERMISSION." + pieces.get(pieces.size()-1));
                
                bs.setSource(context,filePath);
                bs.setDescription(context,"Proxy license");
                
                // Identify the format
                BitstreamFormat bf = bitstreamFormatService.guessFormat(context, bs);
                bs.setFormat(context,bf);
                
                // FIXME: this does not seem to work for some reason. Maybe you can't change policies on pre-archived items?
                // FIXME: The previous line may or may not still be true, as we have now switched to an AuthorizeService with a different removal mechanism for 6x+
            	authorizeService.removePoliciesActionFilter(context, bs, Constants.READ);
            }
            context.commit();
	}
	else if (buttonPressed.equals(NEXT_BUTTON) && licenseType.equalsIgnoreCase("open")) {
	    LicenseUtils.grantLicense(context, item, openLicenseText);
	    context.commit();
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
        return 1;

    }

}
