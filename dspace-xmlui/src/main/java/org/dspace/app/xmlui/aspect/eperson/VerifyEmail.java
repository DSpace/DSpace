/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
 * Notify the user that a verification email has been sent.
 * 
 * There are two parameters this transformer expects:
 * 
 * email - The email of the account to be verified.
 * 
 * forgot - A boolean value indicating whether this is part of the forgotten password workflow.
 * 
 * @author Scott Phillips
 */

public class VerifyEmail extends AbstractDSpaceTransformer
{
    /** language strings */
    private static final Message T_title =
        message("xmlui.EPerson.VerifyEmail.title");
    
    private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
    
    private static final Message T_trail_forgot_password =
        message("xmlui.EPerson.trail_forgot_password");
    
    private static final Message T_trail_new_registration =
        message("xmlui.EPerson.trail_new_registration");
    
    private static final Message T_head =
        message("xmlui.EPerson.VerifyEmail.head");
    
    private static final Message T_para =
        message("xmlui.EPerson.VerifyEmail.para");
    

    /** The email address being verified */
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
