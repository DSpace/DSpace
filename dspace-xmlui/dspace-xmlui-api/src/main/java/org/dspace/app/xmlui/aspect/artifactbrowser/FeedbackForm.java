/*
 * FeedbackForm.java
 *
 * Version: $Revision: 1.4 $
 *
 * Date: $Date: 2006/07/27 18:24:34 $
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
package org.dspace.app.xmlui.aspect.artifactbrowser;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.app.xmlui.wing.element.TextArea;
import org.dspace.authorize.AuthorizeException;
import org.xml.sax.SAXException;

/**
 * Display to the user a simple form letting the user give feedback.
 * 
 * @author Scott Phillips
 */
public class FeedbackForm extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
    /** Language Strings */
    private static final Message T_title =
        message("xmlui.ArtifactBrowser.FeedbackForm.title");
    
    private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
    
    private static final Message T_trail =
        message("xmlui.ArtifactBrowser.FeedbackForm.trail");
    
    private static final Message T_head = 
        message("xmlui.ArtifactBrowser.FeedbackForm.head");
    
    private static final Message T_para1 =
        message("xmlui.ArtifactBrowser.FeedbackForm.para1");
    
    private static final Message T_email =
        message("xmlui.ArtifactBrowser.FeedbackForm.email");

    private static final Message T_email_help =
        message("xmlui.ArtifactBrowser.FeedbackForm.email_help");
    
    private static final Message T_comments = 
        message("xmlui.ArtifactBrowser.FeedbackForm.comments");
    
    private static final Message T_submit =
        message("xmlui.ArtifactBrowser.FeedbackForm.submit");
    
    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey() {
        
        String email = parameters.getParameter("email","");
        String comments = parameters.getParameter("comments","");
        String page = parameters.getParameter("page","unknown");
        
       return HashUtil.hash(email + "-" + comments + "-" + page);
    }

    /**
     * Generate the cache validity object.
     */
    public SourceValidity getValidity() 
    {
        return NOPValidity.SHARED_INSTANCE;
    }
    
    
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {       
        pageMeta.addMetadata("title").addContent(T_title);
 
        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        pageMeta.addTrail().addContent(T_trail);
    }

    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {

        // Build the item viewer division.
        Division feedback = body.addInteractiveDivision("feedback-form",
                contextPath+"/feedback",Division.METHOD_POST,"primary");
        
        feedback.setHead(T_head);
        
        feedback.addPara(T_para1);
        
        List form = feedback.addList("form",List.TYPE_FORM);
        
        Text email = form.addItem().addText("email");
        email.setLabel(T_email);
        email.setHelp(T_email_help);
        email.setValue(parameters.getParameter("email",""));
        
        TextArea comments = form.addItem().addTextArea("comments");
        comments.setLabel(T_comments);
        comments.setValue(parameters.getParameter("comments",""));
        
        form.addItem().addButton("submit").setValue(T_submit);
        
        feedback.addHidden("page").setValue(parameters.getParameter("page","unknown"));
    }
}
