/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package cz.cuni.mff.ufal;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;

import org.apache.cocoon.ResourceNotFoundException;
import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.components.flow.InvalidContinuationException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingConstants;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.authorize.AuthorizeException;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import org.dspace.core.ConfigurationManager;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.List;

/**
 * This special comonent checks to see if the body element is empty (has no sub elements) and if
 * it is then displays some page not found text.
 * 
 * based on class by Scott Phillips and Kim Shepherd
 * modified for LINDAT/CLARIN
 */
public class InvalidContinuationTransformer extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
    private static final org.apache.log4j.Logger log = Logger.getLogger(InvalidContinuationTransformer.class);
    
    /** Language Strings */
    private static final Message T_title =
        message("xmlui.SessionExpired.title");
    
    private static final Message T_head =
        message("xmlui.SessionExpired.head");
    
    private static final Message T_para1 =
        message("xmlui.SessionExpired.para1");
    
    private static final Message T_go_home =
        message("xmlui.general.go_home");
    
    private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");

    private static final Message T_site_admin =
            message("xmlui.error.site_administrator");

    private static final Message T_here =
            message("xmlui.general.here");

    
    /** Where the body element is stored while we wait to see if it is empty */
    private SAXEvent bodyEvent;
    
    /** Have we determined that the body is empty, and hence a we should generate a page not found. */
    private boolean bodyEmpty;
    
    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey() 
    {
        Request request = ObjectModelHelper.getRequest(objectModel);
        
        return HashUtil.hash(request.getSitemapURI());
    }

    /**
     * Generate the cache validity object.
     * 
     * The cache is always valid.
     */
    public SourceValidity getValidity() {
        return NOPValidity.SHARED_INSTANCE;
    }
    
    
    /**
     * Receive notification of the beginning of a document.
     */
    @Override
    public void startDocument() throws SAXException
    {
        // Reset our parameters before starting a new document.
        this.bodyEvent = null;
        this.bodyEmpty = false;
        super.startDocument();
    }

    /**
     * Process the SAX event.
     * @see org.xml.sax.ContentHandler#startElement
     */
    @Override
    public void startElement(String namespaceURI, String localName,
            String qName, Attributes attributes) throws SAXException
    {          
        if (this.bodyEvent != null)
        {
            // If we have recorded the startElement for body and we are
            // recieving another start event, then there must be something 
            // inside the body element, so we send the held body and carry
            // on as normal.
            sendEvent(this.bodyEvent);
            this.bodyEvent = null;
        }
        
        if (WingConstants.DRI.URI.equals(namespaceURI) && Body.E_BODY.equals(localName))
        {
            // Save the element and see if there is anything inside the body.
            this.bodyEvent = SAXEvent.startElement(namespaceURI,localName,qName,attributes);
            return;
        }

       super.startElement(namespaceURI, localName, qName, attributes);
    }

    /**
     * Process the SAX event.
     * @see org.xml.sax.ContentHandler#endElement
     */
    @Override
    public void endElement(String namespaceURI, String localName, String qName)
            throws SAXException
    {
        if (this.bodyEvent != null && WingConstants.DRI.URI.equals(namespaceURI) && Body.E_BODY.equals(localName))
        {
            // If we are recieving an endElement event for body while we
            // still have a startElement body event recorded then
            // the body element must have been empty. In this case, record
            // that the body is empty, and send both the start and end body events.

            this.bodyEmpty = true;
            // Sending the body will trigger the Wing framework to ask
            // us if we want to add a body to the page.
            sendEvent(this.bodyEvent);
            this.bodyEvent = null;
        }

        super.endElement(namespaceURI, localName, qName);
    } 
  
    
    /** What to add at the end of the body 
     * @throws InvalidContinuationException */
    @Override
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException, ResourceNotFoundException, InvalidContinuationException
    {
        Division notFound = body.addDivision("session-expired","alert alert-error");
        
        notFound.setHead(T_title);
        
        String path = contextPath+"/"+ sitemapURI;
        Throwable t = new InvalidContinuationException( "Page " + this.sitemapURI + " needs authenticated user." );
        
        List list = notFound.addList("session-expired-message", List.TYPE_FORM);
        
        String login = contextPath+"/login";
        Item item = list.addItem();
        item.addHighlight("").addContent(T_head);
        item.addXref(login, T_here, "signon");
                
        String contact = contextPath+"/contact";
        item = list.addItem();
        item.addHighlight("").addContent(T_para1);
        item.addXref(contact, T_site_admin);
        
        list.addItem(null, "fa fa-warning fa-5x hangright").addContent(" ");
        
        notFound.addPara().addXref(contextPath + "/",T_go_home);
        
        Division stw = body.addDivision("stack-trace");
        List stack = stw.addList("stack");
        stack.addItemXref("#", "Stack trace");
        List trace = stack.addList("trace");        
        for(StackTraceElement ste : t.getStackTrace()){
        	trace.addItem(ste.toString());
        }        
                    
        log.warn("Not Found" + path);
    }

    /** What page metadata to add to the document */
    @Override
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
        // Set the page title
        pageMeta.addMetadata("title").addContent(T_title);
        
        // Give theme a base trail
        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
    }
    

    /**
     * Send the given recorded sax event.
     */
    public void sendEvent(SAXEvent event) throws SAXException
    {
        if (event.type == SAXEvent.EventType.START)
        {
            super.startElement(event.namespaceURI,event.localName,event.qName,event.attributes);
        }
        else if (event.type == SAXEvent.EventType.END)
        {
            super.endElement(event.namespaceURI,event.localName,event.qName);
        }
    }
    
    
    /**
     * This private class remembers start and end element SAX events.
     */
    private static class SAXEvent {
        
        public enum EventType { START, END };
        
        protected EventType type = null;
        protected String namespaceURI = null;
        protected String localName = null;
        protected String qName = null;
        protected Attributes attributes = null;
        
        /**
         * Create a new StartElement recorded sax event.
         */
        public static SAXEvent startElement(String namespaceURI, String localName, String qName, Attributes attributes) 
        {
            SAXEvent event = new SAXEvent();
            event.type = EventType.START;
            event.namespaceURI = namespaceURI;
            event.localName = localName;
            event.qName = qName;
            event.attributes = attributes;
            return event;
        }
        
        /**
         * Create a new EndElement recorded sax event.
         */
        public static SAXEvent endElement(String namespaceURI, String localName, String qName) 
        {
            SAXEvent event = new SAXEvent();
            event.type = EventType.END;
            event.namespaceURI = namespaceURI;
            event.localName = localName;
            event.qName = qName;
            return event;
        }
    }
    
}

