/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.cocoon;

import java.io.*;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.List;
import java.util.Date;
import java.util.Map;

import javax.mail.internet.MimeUtility;
import javax.servlet.http.HttpServletResponse;

import org.apache.avalon.excalibur.pool.Recyclable;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.ResourceNotFoundException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.Response;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.cocoon.environment.http.HttpResponse;
import org.apache.cocoon.reading.AbstractReader;
import org.apache.cocoon.util.ByteRange;
import org.apache.commons.lang.StringUtils;
import org.dspace.app.xmlui.utils.AuthenticationUtil;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.*;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.disseminate.factory.DisseminateServiceFactory;
import org.dspace.disseminate.service.CitationDocumentService;
import org.dspace.eperson.Group;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.usage.UsageEvent;
import org.xml.sax.SAXException;

import org.apache.log4j.Logger;
import org.dspace.core.LogManager;

/**
 * The BitstreamReader will query DSpace for a particular bitstream and transmit
 * it to the user. There are several methods of specifying the bitstream to be
 * delivered. You may reference a bitstream by its id or attempt to
 * resolve the bitstream's name.
 *
 *  <p>/bitstream/{handle}/{sequence}/{name}
 *
 *  <pre>{@code
 *  <map:read type="BitstreamReader">
 *    <map:parameter name="handle" value="{1}/{2}"/>
 *    <map:parameter name="sequence" value="{3}"/>
 *    <map:parameter name="name" value="{4}"/>
 *  </map:read>
 * }</pre>
 *
 *  When no handle is assigned yet, you can access a bitstream
 *  using its internal ID.
 *
 *  <p>/bitstream/id/{bitstreamID}/{sequence}/{name}
 *
 * <pre>{@code
 *  <map:read type="BitstreamReader">
 *    <map:parameter name="bitstreamID" value="{1}"/>
 *    <map:parameter name="sequence" value="{2}"/>
 *  </map:read>
 * }</pre>
 *
 *  Alternatively, you can access the bitstream via a name instead
 *  of directly through its sequence.
 *
 *  <p>/html/{handle}/{name}
 *
 * <pre>{@code
 *  <map:read type="BitstreamReader">
 *    <map:parameter name="handle" value="{1}/{2}"/>
 *    <map:parameter name="name" value="{3}"/>
 *  </map:read>
 * }</pre>
 *
 *  Again when no handle is available you can also access it
 *  via an internal itemID and name.
 *
 *  <p>/html/id/{itemID}/{name}
 *
 * <pre>{@code
 *  <map:read type="BitstreamReader">
 *    <map:parameter name="itemID" value="{1}"/>
 *    <map:parameter name="name" value="{2}"/>
 *  </map:read>
 * }</pre>
 *
 * <p>
 * Added request-item support.<br>
 * Original Concept, JSPUI version:    Universidade do Minho   at www.uminho.pt<br>
 * Sponsorship of XMLUI version:    Instituto Oceanográfico de España at www.ieo.es
 * 
 * @author Scott Phillips
 * @author Adán Román Ruiz at arvo.es (added request item support)
 */

public class BitstreamReader extends AbstractReader implements Recyclable
{
    private static Logger log = Logger.getLogger(BitstreamReader.class);
        
    /**
     * Messages to be sent when the user is not authorized to view
     * a particular bitstream. They will be redirected to the login
     * where this message will be displayed.
     */
    private static final String AUTH_REQUIRED_HEADER = "xmlui.BitstreamReader.auth_header";
    private static final String AUTH_REQUIRED_MESSAGE = "xmlui.BitstreamReader.auth_message";
        
    /**
     * How big a buffer should we use when reading from the bitstream before
     * writing to the HTTP response?
     */
    protected static final int BUFFER_SIZE = 8192;

    /**
     * When should a bitstream expire in milliseconds. This should be set to
     * some low value just to prevent someone hiting DSpace repeatedy from
     * killing the server. Note: there are 1000 milliseconds in a second.
     *
     * Format: minutes * seconds * milliseconds
     *  60 * 60 * 1000 == 1 hour
     */
    protected static final int expires = 60 * 60 * 1000;

    /** The Cocoon response */
    protected Response response;

    /** The Cocoon request */
    protected Request request;

    /** The bitstream file */
    protected InputStream bitstreamInputStream;
    
    /** The bitstream's reported size */
    protected long bitstreamSize;
    
    /** The bitstream's mime-type */
    protected String bitstreamMimeType;
    
    /** The bitstream's name */
    protected String bitstreamName;
    
    /** True if bitstream is readable by anonymous users */
    protected boolean isAnonymouslyReadable;

    /** The last modified date of the item containing the bitstream */
    private Date itemLastModified = null;

    /** True if user agent making this request was identified as spider. */
    private boolean isSpider = false;

    /** TEMP file for citation PDF. We will save here, so we can delete the temp file when done.  */
    private File tempFile;

    protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
    protected BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    protected HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected CitationDocumentService citationDocumentService = DisseminateServiceFactory.getInstance().getCitationDocumentService();


    /**
     * Set up the bitstream reader.
     *
     * See the class description for information on configuration options.
     * @param resolver source resolver.
     * @param objectModel Cocoon object model.
     * @param src source to read.
     * @param par Reader parameters.
     * @throws org.apache.cocoon.ProcessingException passed through.
     * @throws org.xml.sax.SAXException passed through.
     * @throws java.io.IOException passed through.
     */
    @Override
    public void setup(SourceResolver resolver, Map objectModel, String src,
            Parameters par)
            throws ProcessingException, SAXException, IOException
    {
        super.setup(resolver, objectModel, src, par);

        try
        {
            this.request = ObjectModelHelper.getRequest(objectModel);
            this.response = ObjectModelHelper.getResponse(objectModel);

            Item item = null;

            // Check to see if a context already exists or not. We may
            // have been aggregated into an http request by the XSL document
            // pulling in an XML-based bitstream. In this case the context has
            // already been created and we should leave it open because the
            // normal processes will close it.
            boolean BitstreamReaderOpenedContext = !ContextUtil.isContextAvailable(objectModel);
            Context context = ContextUtil.obtainContext(objectModel);
            
            // Get our parameters that identify the bitstream
            String itemID = par.getParameter("itemID", null);
            String bitstreamID = par.getParameter("bitstreamID", null);
            String handle = par.getParameter("handle", null);
            
            int sequence = par.getParameterAsInteger("sequence", -1);
            String name = par.getParameter("name", null);
        
            this.isSpider = par.getParameter("userAgent", "").equals("spider");

            // Resolve the bitstream
            Bitstream bitstream = null;
            DSpaceObject dso = null;
            
            if (bitstreamID != null)
            {
                // Direct reference to the individual bitstream ID.
                bitstream = bitstreamService.findByIdOrLegacyId(context, bitstreamID);
            }
            else if (itemID != null)
            {
                // Referenced by internal itemID
                item = itemService.findByIdOrLegacyId(context, itemID);
                
                if (sequence > -1)
                {
                        bitstream = findBitstreamBySequence(item, sequence);
                }
                else if (name != null)
                {
                        bitstream = findBitstreamByName(item, name);
                }
            }
            else if (handle != null)
            {
                // Reference by an item's handle.
                dso = handleService.resolveToObject(context, handle);

                if (dso instanceof Item)
                {
                    item = (Item)dso;

                    if (sequence > -1)
                    {
                        bitstream = findBitstreamBySequence(item,sequence);
                    }
                    else if (name != null)
                    {
                        bitstream = findBitstreamByName(item,name);
                    }
                }
            }

            if (item != null) {
                itemLastModified = item.getLastModified();
            }

            // if initial search was by sequence number and found nothing,
            // then try to find bitstream by name (assuming we have a file name)
            if((sequence > -1 && bitstream==null) && name!=null)
            {
                bitstream = findBitstreamByName(item,name);

                // if we found bitstream by name, send a redirect to its new sequence number location
                if(bitstream!=null)
                {
                    String redirectURL = "";

                    // build redirect URL based on whether item has a handle assigned yet
                    if(item.getHandle()!=null && item.getHandle().length()>0)
                    {
                        redirectURL = request.getContextPath() + "/bitstream/handle/" + item.getHandle();
                    }
                    else
                    {
                        redirectURL = request.getContextPath() + "/bitstream/item/" + item.getID();
                    }

                        redirectURL += "/" + name + "?sequence=" + bitstream.getSequenceID();

                        HttpServletResponse httpResponse = (HttpServletResponse)
                        objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);
                        httpResponse.sendRedirect(redirectURL);
                        return;
                }
            }

            // Was a bitstream found?
            if (bitstream == null)
            {
                throw new ResourceNotFoundException("Unable to locate bitstream");
            }

            // Is there a User logged in and does the user have access to read it?
            boolean isAuthorized = authorizeService.authorizeActionBoolean(context, bitstream, Constants.READ);
            if (item != null && item.isWithdrawn() && !authorizeService.isAdmin(context))
            {
                isAuthorized = false;
                log.info(LogManager.getHeader(context, "view_bitstream", "handle=" + item.getHandle() + ",withdrawn=true"));
            }
            // It item-request is enabled to all request we redirect to restricted-resource immediately without login request  
            String requestItemType = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("request.item.type");
            if (!isAuthorized)
            {
                if(context.getCurrentUser() != null || StringUtils.equalsIgnoreCase("all", requestItemType)){
                        // A user is logged in, but they are not authorized to read this bitstream,
                        // instead of asking them to login again we'll point them to a friendly error
                        // message that tells them the bitstream is restricted.
                        String redictURL = request.getContextPath() + "/handle/";
                        if (item!=null){
                                redictURL += item.getHandle();
                        }
                        else if(dso!=null){
                                redictURL += dso.getHandle();
                        }
                        redictURL += "/restricted-resource?bitstreamId=" + bitstream.getID();

                        HttpServletResponse httpResponse = (HttpServletResponse)
                        objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);
                        httpResponse.sendRedirect(redictURL);
                        return;
                }
                else{
                	if(StringUtils.isBlank(DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("request.item.type")) ||
                			                			DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("request.item.type").equalsIgnoreCase("logged")){
                        // The user does not have read access to this bitstream. Interrupt this current request
                        // and then forward them to the login page so that they can be authenticated. Once that is
                        // successful, their request will be resumed.
                        AuthenticationUtil.interruptRequest(objectModel, AUTH_REQUIRED_HEADER, AUTH_REQUIRED_MESSAGE, null);

                        // Redirect
                        String redictURL = request.getContextPath() + "/login";

                        HttpServletResponse httpResponse = (HttpServletResponse)
                        objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);
                        httpResponse.sendRedirect(redictURL);
                        return;
                	}
                }
            }

            // Success, bitstream found and the user has access to read it.
            // Store these for later retrieval:

            // Intercepting views to the original bitstream to instead show a citation altered version of the object
            // We need to check if this resource falls under the "show watermarked alternative" umbrella.
            // At which time we will not return the "bitstream", but will instead on-the-fly generate the citation rendition.

            // What will trigger a redirect/intercept?
            // 1) Intercepting Enabled
            // 2) This User is not an admin
            // 3) This object is citation-able
            if (citationDocumentService.isCitationEnabledForBitstream(bitstream, context)) {
                // on-the-fly citation generator
                log.info(item.getHandle() + " - " + bitstream.getName() + " is citable.");

                FileInputStream fileInputStream = null;

                try {
                    //Create the cited document
                    tempFile = citationDocumentService.makeCitedDocument(context, bitstream);
                    if(tempFile == null) {
                        log.error("CitedDocument was null");
                    } else {
                        log.info("CitedDocument was ok," + tempFile.getAbsolutePath());
                    }


                    fileInputStream = new FileInputStream(tempFile);
                    if(fileInputStream == null) {
                        log.error("Error opening fileInputStream: ");
                    }

                    this.bitstreamInputStream = fileInputStream;
                    this.bitstreamSize = tempFile.length();

                } catch (Exception e) {
                    log.error("Caught an error with intercepting the citation document:" + e.getMessage());
                }

                //End of CitationDocument
            } else {
                this.bitstreamInputStream = bitstreamService.retrieve(context, bitstream);
                this.bitstreamSize = bitstream.getSize();
            }

            this.bitstreamMimeType = bitstream.getFormat(context).getMIMEType();
            this.bitstreamName = bitstream.getName();
            if (context.getCurrentUser() == null)
            {
                this.isAnonymouslyReadable = true;
            }
            else
            {
                this.isAnonymouslyReadable = false;
                for (ResourcePolicy rp : authorizeService.getPoliciesActionFilter(context, bitstream, Constants.READ))
                {
                    if (rp.getGroup() != null && rp.getGroup().getName().equals(Group.ANONYMOUS))
                    {
                        this.isAnonymouslyReadable = true;
                    }
                }
            }

            // Trim any path information from the bitstream
            if (bitstreamName != null && bitstreamName.length() >0 )
            {
                        int finalSlashIndex = bitstreamName.lastIndexOf('/');
                        if (finalSlashIndex > 0)
                        {
                                bitstreamName = bitstreamName.substring(finalSlashIndex+1);
                        }
            }
            else
            {
                // In-case there is no bitstream name...
                if(name != null && name.length() > 0) {
                    bitstreamName = name;
                    if(name.endsWith(".jpg")) {
                        bitstreamMimeType = "image/jpeg";
                    } else if(name.endsWith(".png")) {
                        bitstreamMimeType = "image/png";
                    }
                } else {
                    bitstreamName = "bitstream";
                }
            }
            
            // Log that the bitstream has been viewed, this is non-cached and the complexity
            // of adding it to the sitemap for every possible bitstream uri is not very tractable
            DSpaceServicesFactory.getInstance().getEventService().fireEvent(
                                new UsageEvent(
                                                UsageEvent.Action.VIEW,
                                                ObjectModelHelper.getRequest(objectModel),
                                                ContextUtil.obtainContext(ObjectModelHelper.getRequest(objectModel)),
                                                bitstream));
            
            // If we created the database connection close it, otherwise leave it open.
            if (BitstreamReaderOpenedContext)
            	context.complete();
        }
        catch (SQLException sqle)
        {
            throw new ProcessingException("Unable to read bitstream.",sqle);
        }
        catch (AuthorizeException ae)
        {
            throw new ProcessingException("Unable to read bitstream.",ae);
        }
    }

    
    
    
    
    /**
     * Find the bitstream identified by a sequence number on this item.
     *
     * @param item A DSpace item
     * @param sequence The sequence of the bitstream
     * @return The bitstream or null if none found.
     */
    private Bitstream findBitstreamBySequence(Item item, int sequence) throws SQLException
    {
        if (item == null)
        {
            return null;
        }
        
        List<Bundle> bundles = item.getBundles();
        for (Bundle bundle : bundles)
        {
            List<Bitstream> bitstreams = bundle.getBitstreams();

            for (Bitstream bitstream : bitstreams)
            {
                if (bitstream.getSequenceID() == sequence)
                {
                    return bitstream;
                }
            }
        }
        return null;
    }
    
    /**
     * Return the bitstream from the given item that is identified by the
     * given name. If the name has prepended directories they will be removed
     * one at a time until a bitstream is found. Note that if two bitstreams
     * have the same name then the first bitstream will be returned.
     *
     * @param item A DSpace item
     * @param name The name of the bitstream
     * @return The bitstream or null if none found.
     */
    private Bitstream findBitstreamByName(Item item, String name) throws SQLException
    {
        if (name == null || item == null)
        {
            return null;
        }
    
        // Determine our the maximum number of directories that will be removed for a path.
        int maxDepthPathSearch = 3;
        if (DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("xmlui.html.max-depth-guess") != null)
        {
            maxDepthPathSearch = DSpaceServicesFactory.getInstance().getConfigurationService().getIntProperty("xmlui.html.max-depth-guess");
        }
        
        // Search for the named bitstream on this item. Each time through the loop
        // a directory is removed from the name until either our maximum depth is
        // reached or the bitstream is found. Note: an extra pass is added on to the
        // loop for a last ditch effort where all directory paths will be removed.
        for (int i = 0; i < maxDepthPathSearch+1; i++)
        {
                // Search through all the bitstreams and see
                // if the name can be found
                List<Bundle> bundles = item.getBundles();
                for (Bundle bundle : bundles)
                {
                    List<Bitstream> bitstreams = bundle.getBitstreams();
        
                    for (Bitstream bitstream : bitstreams)
                    {
                        if (name.equals(bitstream.getName()))
                        {
                            return bitstream;
                        }
                    }
                }
                
                // The bitstream was not found, so try removing a directory
                // off of the name and see if we lost some path information.
                int indexOfSlash = name.indexOf('/');
                
                if (indexOfSlash < 0)
                {
                    // No more directories to remove from the path, so return null for no
                    // bitstream found.
                    return null;
                }
               
                name = name.substring(indexOfSlash+1);
                
                // If this is our next to last time through the loop then
                // trim everything and only use the trailing filename.
                if (i == maxDepthPathSearch-1)
                {
                        int indexOfLastSlash = name.lastIndexOf('/');
                        if (indexOfLastSlash > -1)
                        {
                            name = name.substring(indexOfLastSlash + 1);
                        }
                }
                
        }
        
        // The named bitstream was not found and we exhausted the maximum path depth that
        // we search.
        return null;
    }
    
    
    /**
         * Write the actual data out to the response.
         *
         * Some implementation notes:
         *
         * 1) We set a short expiration time just in the hopes of preventing someone
         * from overloading the server by clicking reload a bunch of times. I
         * Realize that this is nowhere near 100% effective but it may help in some
         * cases and shouldn't hurt anything.
         *
         * 2) We accept partial downloads, thus if you lose a connection halfway
         * through most web browser will enable you to resume downloading the
         * bitstream.
     * @throws java.io.IOException passed through.
     * @throws org.xml.sax.SAXException passed through.
     * @throws org.apache.cocoon.ProcessingException passed through.
         */
    @Override
    public void generate() throws IOException, SAXException,
            ProcessingException
    {
        if (this.bitstreamInputStream == null)
        {
            return;
        }
        
        // Only allow If-Modified-Since protocol if request is from a spider
        // since response headers would encourage a browser to cache results
        // that might change with different authentication.
        if (isSpider)
        {
            // Check for if-modified-since header -- ONLY if not authenticated
            long modSince = request.getDateHeader("If-Modified-Since");
            if (modSince != -1 && itemLastModified != null && itemLastModified.getTime() < modSince)
            {
                // Item has not been modified since requested date,
                // hence bitstream has not been, either; return 304
                response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                return;
            }
        }

        // Only set Last-Modified: header for spiders or anonymous
        // access, since it might encourage browse to cache the result
        // which might leave a result only available to authenticated
        // users in the cache for a response later to anonymous user.
        try
        {
            if (itemLastModified != null && (isSpider || ContextUtil.obtainContext(request).getCurrentUser() == null))
            {
                // TODO:  Currently just borrow the date of the item, since
                // we don't have last-mod dates for Bitstreams
                response.setDateHeader("Last-Modified", itemLastModified.getTime());
            }
        }
        catch (SQLException e)
        {
            throw new ProcessingException(e);
        }

        byte[] buffer = new byte[BUFFER_SIZE];
        int length = -1;

        // Only encourage caching if this is not a restricted resource, i.e.
        // if it is accessed anonymously or is readable by Anonymous:
        if (isAnonymouslyReadable)
        {
            response.setDateHeader("Expires", System.currentTimeMillis() + expires);
        }
        
        // If this is a large bitstream then tell the browser it should treat it as a download.
        int threshold = DSpaceServicesFactory.getInstance().getConfigurationService().getIntProperty("xmlui.content_disposition_threshold");
        if (bitstreamSize > threshold && threshold != 0)
        {
                String name  = bitstreamName;
                
                // Try and make the download file name formatted for each browser.
                try {
                        String agent = request.getHeader("USER-AGENT");
                        if (agent != null && agent.contains("MSIE"))
                        {
                            name = URLEncoder.encode(name, "UTF8");
                        }
                        else if (agent != null && agent.contains("Mozilla"))
                        {
                            name = MimeUtility.encodeText(name, "UTF8", "B");
                        }
                }
                catch (UnsupportedEncodingException see)
                {
                        // do nothing
                }
                response.setHeader("Content-Disposition", "attachment;filename=" + '"' + name + '"');
        }

        ByteRange byteRange = null;

        // Turn off partial downloads, they cause problems
        // and are only rarely used. Specifically some windows pdf
        // viewers are incapable of handling this request. You can
        // uncomment the following lines to turn this feature back on.

//        response.setHeader("Accept-Ranges", "bytes");
//        String ranges = request.getHeader("Range");
//        if (ranges != null)
//        {
//            try
//            {
//                ranges = ranges.substring(ranges.indexOf('=') + 1);
//                byteRange = new ByteRange(ranges);
//            }
//            catch (NumberFormatException e)
//            {
//                byteRange = null;
//                if (response instanceof HttpResponse)
//                {
//                    // Respond with status 416 (Request range not
//                    // satisfiable)
//                    response.setStatus(416);
//                }
//            }
//        }

        try
        {
            if (byteRange != null)
            {
                String entityLength;
                String entityRange;
                if (this.bitstreamSize != -1)
                {
                    entityLength = "" + this.bitstreamSize;
                    entityRange = byteRange.intersection(
                            new ByteRange(0, this.bitstreamSize)).toString();
                }
                else
                {
                    entityLength = "*";
                    entityRange = byteRange.toString();
                }

                response.setHeader("Content-Range", entityRange + "/" + entityLength);
                if (response instanceof HttpResponse)
                {
                    // Response with status 206 (Partial content)
                    response.setStatus(206);
                }

                int pos = 0;
                int posEnd;
                while ((length = this.bitstreamInputStream.read(buffer)) > -1)
                {
                    posEnd = pos + length - 1;
                    ByteRange intersection = byteRange.intersection(new ByteRange(pos, posEnd));
                    if (intersection != null)
                    {
                        out.write(buffer, (int) intersection.getStart() - pos, (int) intersection.length());
                    }
                    pos += length;
                }
            }
            else
            {
                response.setHeader("Content-Length", String.valueOf(this.bitstreamSize));

                while ((length = this.bitstreamInputStream.read(buffer)) > -1)
                {
                    out.write(buffer, 0, length);
                }
                out.flush();
            }
        }
        finally
        {
            try
            {
                // Close the bitstream input stream so that we don't leak a file descriptor
                this.bitstreamInputStream.close();
                
                // Close the output stream as per Cocoon docs: http://cocoon.apache.org/2.2/core-modules/core/2.2/681_1_1.html
                out.close();
            } 
            catch (IOException ioe)
            {
                // Closing the stream threw an IOException but do we want this to propagate up to Cocoon?
                // No point since the user has already got the bitstream contents.
                log.warn("Caught IO exception when closing a stream: " + ioe.getMessage());
            }
        }

    }

    /**
     * Returns the mime-type of the bitstream.
     * @return the type.
     */
    @Override
    public String getMimeType()
    {
        return this.bitstreamMimeType;
    }
    
    /**
         * Recycle
         */
    @Override
    public void recycle() {
        this.response = null;
        this.request = null;
        this.bitstreamInputStream = null;
        this.bitstreamSize = 0;
        this.bitstreamMimeType = null;
        this.bitstreamName = null;
        this.itemLastModified = null;
        this.tempFile = null;
        super.recycle();
    }


}
