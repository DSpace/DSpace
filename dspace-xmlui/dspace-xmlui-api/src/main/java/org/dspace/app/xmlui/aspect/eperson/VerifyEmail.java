/*
 * VerifyEmail.java
 *
 * Version: $Revision: 1.3 $
 *
 * Date: $Date: 2006/06/02 21:37:46 $
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

package org.dspace.app.xmlui.aspect.eperson;

import java.io.IOException;
import java.util.Map;

import org.apache.avalon.framework.parameters.ParameterException;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.SourceResolver;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.xml.sax.SAXException;

/**
 * Display to the user that a verification email has been set.
 * 
 * There are two parameters this transformer expects:
 * 
 * email - The email of the to-be-verrified account.
 * 
 * forgot - A boolean value indicating that this is part of the forgotten password workflow.
 * 
 * @author Scott Phillips
 */

public class VerifyEmail extends AbstractDSpaceTransformer
{
    /** language strings */
    private final static Message T_title =
        message("xmlui.EPerson.VerifyEmail.title");
    
    private final static Message T_dspace_home =
        message("xmlui.general.dspace_home");
    
    private final static Message T_trail_forgot_password =
        message("xmlui.EPerson.trail_forgot_password");
    
    private final static Message T_trail_new_registration =
        message("xmlui.EPerson.trail_new_registration");
    
    private final static Message T_head =
        message("xmlui.EPerson.VerifyEmail.head");
    
    private final static Message T_para =
        message("xmlui.EPerson.VerifyEmail.para");
    

    /** The email address being verrified */
    private String email;

    /** Determine if this is part of the forgot password workflow */
    private boolean forgot;

    public void setup(SourceResolver resolver, Map objectModel, String src,
            Parameters parameters) throws ProcessingException, SAXException,
            IOException
    { 
        super.setup(resolver,objectModel,src,parameters);
        
        try
        {
            this.email = parameters.getParameter("email");
            this.forgot = parameters.getParameterAsBoolean("forgot");
        }
        catch (ParameterException pe)
        {
            throw new ProcessingException(pe);
        }
    }

    public void addPageMeta(PageMeta pageMeta) throws WingException 
    {
        // Set the page title
        pageMeta.addMetadata("title").addContent(T_title);

        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        if (forgot)
        {
            pageMeta.addTrail().addContent(T_trail_forgot_password);
        } 
        else 
        {
            pageMeta.addTrail().addContent(T_trail_new_registration);
        }
        
    }
    
    public void addBody(Body body) throws WingException
    {
        Division verify = body.addDivision("verify-email","primary");

        verify.setHead(T_head);
        
        if (forgot)
        {
            EPersonUtils.forgottProgressList(verify, 1);
        }
        else
        {
            EPersonUtils.registrationProgressList(verify,1);
        }
        
        verify.addPara(T_para.parameterize(email));
    }

    /**
     * Recycle
     */
    public void recycle() 
    {
        this.email = null;
        super.recycle();
    }
}
