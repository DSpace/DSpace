/*
 * PageNotFoundTransformer.java
 *
 * Version: $Revision: 1.2 $
 *
 * Date: $Date: 2006/06/02 21:38:18 $
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package org.dspace.app.xmlui.aspect.general;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
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
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.authorize.AuthorizeException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * This special comonent checks to see if the body element is empty (has no sub elements) and if
 * it is then displays some page not found text.
 * 
 * @author Scott Phillips
 */
public class PageNotFoundTransformer extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
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
        
        if (WingConstants.DRI.URI.equals(namespaceURI))
        {
            if (Body.E_BODY.equals(localName))
            {
                // Save the element and see if there is anything inside the body.
                this.bodyEvent = SAXEvent.startElement(namespaceURI,localName,qName,attributes);
                return;
            }
        }

       super.startElement(namespaceURI, localName, qName, attributes);
    }

    /**
     * Process the SAX event.
     * @see org.xml.sax.ContentHandler#endElement
     */
    public void endElement(String namespaceURI, String localName, String qName)
            throws SAXException
    {
        if (WingConstants.DRI.URI.equals(namespaceURI))
        {
            if (Body.E_BODY.equals(localName))
            {
                if (this.bodyEvent != null)
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
            }
        } 
        
      super.endElement(namespaceURI, localName, qName);
    } 
  
    
    /** What to add at the end of the body */
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        if (this.bodyEmpty)
        {
            Division notFound = body.addDivision("page-not-found","primary");
            
            notFound.setHead(T_head);
            
            notFound.addPara(T_para1); 
            
            notFound.addPara().addXref(contextPath,T_go_home);
        }
    }

    /** What page metadata to add to the document */
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
        if (this.bodyEmpty)
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
