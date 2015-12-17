/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.general;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;

import javax.servlet.http.HttpServletResponse;

import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.Response;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
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

import cz.cuni.mff.ufal.InvalidContinuationTransformer;

/**
 * This special component checks to see if the body element is empty (has no sub elements) and if
 * it is then displays some page not found text.
 * 
 * based on class by Scott Phillips and Kim Shepherd
 * modified for LINDAT/CLARIN
 */
public class PageNotFoundTransformer extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
    private static final org.apache.log4j.Logger log = Logger.getLogger(PageNotFoundTransformer.class);
    /** Language Strings */
    private static final Message T_title =
        message("xmlui.PageNotFound.title");
    
    private static final Message T_head =
        message("xmlui.PageNotFound.head");
    
    private static final Message T_para1 =
        message("xmlui.PageNotFound.para1");
    
    private static final Message T_go_home =
        message("xmlui.general.go_home");
    
    private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
    
    
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
            // receiving another start event, then there must be something 
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
            // If we are receiving an endElement event for body while we
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
  
    
    /** What to add at the end of the body */
    @Override
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException, ProcessingException
    {
    	
        Request request = ObjectModelHelper.getRequest(objectModel);
    	
        if ( this.eperson == null &&  this.sitemapURI.endsWith(".continue") ) {
        	InvalidContinuationTransformer ic = new InvalidContinuationTransformer();
        	ic.setup(null, objectModel, null, parameters);
        	ic.addBody(body);
        	return;
        }
    	
	    final String ignore = ConfigurationManager.getProperty("lr", "lr.ignore.notFound");
	    String[] ignorePaths = null;
	    if(ignore != null && !ignore.equals("")){
		ignorePaths = ignore.split(" ");
	    }

	    boolean found = false;
	    for(String path : ignorePaths){
		if(sitemapURI.contains(path)){
			found = true;
			break;
		}
	    }

        if (this.bodyEmpty && !found)
        {
            Division notFound = body.addDivision("page-not-found","alert alert-error");
            
            notFound.setHead(T_head);
            
            String path = request.getRequestURI();
            
            Throwable t = new Throwable("Page cannot be found");
            List list = notFound.addList("not-found", List.TYPE_FORM);
            Item mess = list.addItem();
            mess.addHighlight("").addContent("Sorry, we couldn't find the page you've requested ("+path+"), if you think it should exist please contact our ");
            String support = ConfigurationManager.getProperty("lr.help.mail");
            mess.addHighlight("").addXref("mailto:" + support, "Help Desk.", null, null);
            list.addItem(null, "fa fa-warning fa-5x hangright").addContent(" ");

            notFound.addPara().addXref(contextPath + "/",T_go_home);

            Division stw = body.addDivision("stack-trace");
            List stack = stw.addList("stack");
            stack.addItemXref("#", "Stack trace");
            List trace = stack.addList("trace");        
            for(StackTraceElement ste : t.getStackTrace()){
            	trace.addItem(ste.toString());
            }

            //This is here to generate 404 but doesn't work.
            //check ./sources/dspace-xmlui/dspace-xmlui-webapp/src/main/webapp/sitemap.xmap and the relevant dspace bug referenced in our #421
            // special case

/*
            if ( this.eperson == null &&  this.sitemapURI.endsWith(".continue") ) {
            	throw new InvalidContinuationException( "Page " + this.sitemapURI + " needs authenticated user." );
            }
*/            
            //throw new ResourceNotFoundException("Page cannot be found");

			HttpServletResponse httpResponse = (HttpServletResponse) objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);
			httpResponse.setStatus(Response.SC_NOT_FOUND);

        }
    }

    /** What page metadata to add to the document */
    @Override
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
    	if ( this.eperson == null &&  this.sitemapURI.endsWith(".continue") ) {
        	InvalidContinuationTransformer ic = new InvalidContinuationTransformer();
        	try {
				ic.setup(null, objectModel, null, parameters);
			} catch (ProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	ic.addPageMeta(pageMeta);
    	}
    	else if (this.bodyEmpty)
        {
            // Set the page title
            pageMeta.addMetadata("title").addContent(T_title);
            
            // Give theme a base trail
            pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        }
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
