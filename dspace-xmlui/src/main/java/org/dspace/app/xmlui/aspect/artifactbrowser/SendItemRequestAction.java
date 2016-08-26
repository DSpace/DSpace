/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.artifactbrowser;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.requestitem.RequestItemAuthor;
import org.dspace.app.requestitem.RequestItemAuthorExtractor;
import org.dspace.app.requestitem.factory.RequestItemServiceFactory;
import org.dspace.app.requestitem.service.RequestItemService;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.content.Bitstream;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EPerson;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;

 /**
 * This action will send a mail to request a item to administrator when all mandatory data is present.
 * It will record the request into the database.
 * 
 * Original Concept, JSPUI version:    Universidade do Minho   at www.uminho.pt
 * Sponsorship of XMLUI version:    Instituto Oceanográfico de España at www.ieo.es
 * 
 * @author Adán Román Ruiz at arvo.es (added request item support)
 */
public class SendItemRequestAction extends AbstractAction
{
    private static final Logger log = Logger.getLogger(SendItemRequestAction.class);

    protected HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
    protected RequestItemService requestItemService = RequestItemServiceFactory.getInstance().getRequestItemService();
    protected BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();

    @Override
    public Map act(Redirector redirector, SourceResolver resolver, Map objectModel,
            String source, Parameters parameters) throws Exception
    {
        Request request = ObjectModelHelper.getRequest(objectModel);
       
        String requesterName = request.getParameter("requesterName");
        String requesterEmail = request.getParameter("requesterEmail");
        String allFiles = request.getParameter("allFiles");
        String message = request.getParameter("message");
        String bitstreamId = request.getParameter("bitstreamId");
     
        // User email from context
        Context context = ContextUtil.obtainContext(objectModel);
        EPerson loggedin = context.getCurrentUser();
        String eperson = null;
        if (loggedin != null)
        {
            eperson = loggedin.getEmail();
        }

        // Check all data is there
        if (StringUtils.isEmpty(requesterName) || StringUtils.isEmpty(requesterEmail) || StringUtils.isEmpty(allFiles) || StringUtils.isEmpty(message))
        {
            // Either the user did not fill out the form or this is the
            // first time they are visiting the page.
            Map<String,String> map = new HashMap<>();
            map.put("bitstreamId",bitstreamId);

            if (StringUtils.isEmpty(requesterEmail))
            {
                map.put("requesterEmail", eperson);
            }
            else
            {
                map.put("requesterEmail", requesterEmail);
            }
            map.put("requesterName",requesterName);
            map.put("allFiles",allFiles);
            map.put("message",message);
            return map;
        }
    	DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if (!(dso instanceof Item))
        {
            throw new Exception("Invalid DspaceObject at ItemRequest.");
        }
        
        Item item = (Item) dso;
        String title = item.getName();
        
        title = StringUtils.isNotBlank(title) ? title : I18nUtil
                            .getMessage("jsp.general.untitled", context);
        Bitstream bitstream = bitstreamService.find(context, UUID.fromString(bitstreamId));

        RequestItemAuthor requestItemAuthor = DSpaceServicesFactory.getInstance().getServiceManager()
                .getServiceByName(
                        RequestItemAuthorExtractor.class.getName(),
                        RequestItemAuthorExtractor.class
                )
                .getRequestItemAuthor(context, item);

        String token = requestItemService.createRequest(context, bitstream, item, Boolean.valueOf(allFiles), requesterEmail, requesterName, message);

        // All data is there, send the email
        Email email = Email.getEmail(I18nUtil.getEmailFilename(context.getCurrentLocale(), "request_item.author"));
        email.addRecipient(requestItemAuthor.getEmail());

        email.addArgument(requesterName);    
        email.addArgument(requesterEmail);
        email.addArgument(allFiles.equals("true") ? I18nUtil.getMessage("itemRequest.all") : bitstream.getName());
        email.addArgument(handleService.getCanonicalForm(item.getHandle()));
        email.addArgument(title);    // request item title
        email.addArgument(message);   // message
        email.addArgument(getLinkTokenEmail(context,token));
        email.addArgument(requestItemAuthor.getFullName());    //   corresponding author name
        email.addArgument(requestItemAuthor.getEmail());    //   corresponding author email
        email.addArgument(DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("dspace.name"));
        email.addArgument(DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("mail.helpdesk"));

        email.setReplyTo(requesterEmail);
         
        email.send();
        // Finished, allow to pass.
        return null;
    }

    /**
     * Get the link to the author in RequestLink email.
     * @param context DSpace session context.
     * @param token token.
     * @return the link.
     * @throws SQLException passed through.
     */
    protected String getLinkTokenEmail(Context context, String token)
            throws SQLException
    {
        String base = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("dspace.url");

        String specialLink = new StringBuffer()
                .append(base)
                .append(base.endsWith("/") ? "" : "/")
                .append("itemRequestResponse/")
                .append(token)
                .toString()+"/";

        return specialLink;
    }

}
