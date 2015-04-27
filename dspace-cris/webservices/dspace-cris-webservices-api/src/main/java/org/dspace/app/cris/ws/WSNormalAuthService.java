/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.ws;


import javax.xml.soap.SOAPException;

import org.dspace.app.cris.model.ws.User;
import org.dspace.app.cris.util.UtilsXSD;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.xpath.XPath;

public class WSNormalAuthService extends AWSAuthService
{

    /**
     * XPath to read username on soap message
     */
    private XPath usernameExpression;

    /**
     * XPath to read password on soap message
     */
    private XPath passwordExpression;

    public WSNormalAuthService()
            throws JDOMException
    {
        Namespace namespace = Namespace.getNamespace(UtilsXSD.NAMESPACE_PREFIX_CRIS, 
        		UtilsXSD.NAMESPACE_CRIS);
        usernameExpression = XPath.newInstance("//cris:Username");
        usernameExpression.addNamespace(namespace);
        passwordExpression = XPath.newInstance("//cris:Password");
        passwordExpression.addNamespace(namespace);

    }

    @Override
    protected Element invokeInternal(Element arg0) throws Exception
    {

        String username = usernameExpression.valueOf(arg0);
        String password = passwordExpression.valueOf(arg0);
        String type = typeExpression.valueOf(arg0);
        type = type.trim();
        User userWS = null;
        try
        {
            userWS = authenticationWS.authenticateNormal(username, password);
        }
        catch (RuntimeException e)
        {
            throw new SOAPException(e.getMessage());
        }
        if (userWS == null)
        {
            throw new RuntimeException("User not found!");
        }

        if (!userWS.isEnabled())
        {
            throw new RuntimeException(
                    "User disabled! Please Contact Admnistrator");
        }
        
        if(!AuthorizationWS.authorize(userWS, type)) {
            throw new SOAPException("User not allowed to retrieve those informations. Contact Administrator");
        }
        return buildResult(userWS, arg0, "NormalAuthQueryResponse");
    }

}
