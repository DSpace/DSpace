/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.ws;


import javax.servlet.http.HttpServletRequest;
import javax.xml.soap.SOAPException;

import org.dspace.app.cris.model.ws.User;
import org.dspace.app.cris.util.UtilsXSD;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.xpath.XPath;
import org.springframework.ws.transport.context.TransportContext;
import org.springframework.ws.transport.context.TransportContextHolder;
import org.springframework.ws.transport.http.HttpServletConnection;

public class WSTokenAuthService extends AWSAuthService
{

    private XPath tokenExpression;

    public WSTokenAuthService()
            throws JDOMException
    {
        Namespace namespace = Namespace.getNamespace(UtilsXSD.NAMESPACE_PREFIX_CRIS, 
        		UtilsXSD.NAMESPACE_CRIS);
        tokenExpression = XPath.newInstance("//cris:Token");
        tokenExpression.addNamespace(namespace);
    }

    @Override
    protected Element invokeInternal(Element arg0) throws Exception
    {
        
        TransportContext context = TransportContextHolder.getTransportContext();
        HttpServletConnection connection = (HttpServletConnection) context
                .getConnection();
        HttpServletRequest request = connection.getHttpServletRequest();
        String ipAddress = request.getRemoteAddr();

        String token = tokenExpression.valueOf(arg0);
        String type = typeExpression.valueOf(arg0);
        type = type.trim();
        User userWS = null;
        try
        {
            userWS = authenticationWS.authenticateToken(ipAddress, token);
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
        return buildResult(userWS, arg0, "TokenAuthQueryResponse");

    }

 
}
