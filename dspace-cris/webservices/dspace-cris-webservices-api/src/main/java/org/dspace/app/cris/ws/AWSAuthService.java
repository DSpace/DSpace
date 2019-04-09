/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.ws;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.discovery.CrisSearchService;
import org.dspace.app.cris.model.ws.User;
import org.dspace.app.cris.util.UtilsXSD;
import org.dspace.discovery.SearchServiceException;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.xpath.XPath;
import org.springframework.ws.server.endpoint.AbstractJDomPayloadEndpoint;

public abstract class AWSAuthService extends AbstractJDomPayloadEndpoint
{

    private static Logger log = Logger.getLogger(AWSAuthService.class);

    protected Map<String, IWSService> types2ws;

    protected XPath queryExpression;

    protected XPath projectionExpression;

    protected XPath typeExpression;

    protected XPath paginationStartExpression;

    protected XPath paginationLimitExpression;

    protected XPath xsdRPExpression;

    protected XPath sortExpression;
    
    protected XPath sortOrderExpression;
    
    protected XPath parentCRISIDExpression;
    
    protected CrisSearchService searchService;

    protected AuthenticationWS authenticationWS;

    public AWSAuthService() throws JDOMException
    {
        Namespace namespace = Namespace.getNamespace(
                UtilsXSD.NAMESPACE_PREFIX_CRIS, UtilsXSD.NAMESPACE_CRIS);
        queryExpression = XPath.newInstance("//cris:Query");
        queryExpression.addNamespace(namespace);
        projectionExpression = XPath.newInstance("//cris:Projection");
        projectionExpression.addNamespace(namespace);
        typeExpression = XPath.newInstance("//cris:Type");
        typeExpression.addNamespace(namespace);
        paginationStartExpression = XPath.newInstance("//cris:PaginationStart");
        paginationStartExpression.addNamespace(namespace);
        paginationLimitExpression = XPath.newInstance("//cris:PaginationRows");
        paginationLimitExpression.addNamespace(namespace);
        sortExpression = XPath.newInstance("//cris:Sort");
        sortExpression.addNamespace(namespace);
        sortOrderExpression = XPath.newInstance("//cris:Sort/@SortOrder");
        sortOrderExpression.addNamespace(namespace);
        parentCRISIDExpression = XPath.newInstance("//cris:ParentCrisID");
        parentCRISIDExpression.addNamespace(namespace);
    }

    public void setSearchService(CrisSearchService searchService)
    {
        this.searchService = searchService;
    }

    public CrisSearchService getSearchService()
    {
        return searchService;
    }

    protected Element buildResult(User userWS, Element arg0, String nameRoot)
            throws JDOMException, IOException, SearchServiceException
    {
        String query = queryExpression.valueOf(arg0);
        String projection = projectionExpression.valueOf(arg0);
        String paginationStart = paginationStartExpression.valueOf(arg0);
        String paginationLimit = paginationLimitExpression.valueOf(arg0);
        String sort = sortExpression.valueOf(arg0);
        String sortOrder = sortOrderExpression.valueOf(arg0);
        String parent = parentCRISIDExpression.valueOf(arg0); 

        String[] splitProjection = projection.trim().split(" ");

        List<String> stringList = new ArrayList<String>();

        for (String string : splitProjection)
        {
            if (string != null)
            {
                string = string.trim();
                if (string.length() > 0)
                {
                    stringList.add(string);
                }
            }
        }

        splitProjection = stringList.toArray(new String[stringList.size()]);

        String type = typeExpression.valueOf(arg0);
        type = type.trim();
        if(type.endsWith("nested")) {
        	if(stringList.size()>1) {
        		throw new IOException("Nested endpoint require a single value on projection");
        	}
        	if(StringUtils.isBlank(parent)) {
        		throw new IOException("ParentCrisID is mandatory for the nested endpoint");
        	}
        	else {
        		if("*:*".equals(query)) {
        			query = "";
        		}
            }
        }
        String typeCapitalizedFirst = (type.substring(0, 1).toUpperCase() + type
                .substring(1)).trim();

        IWSService plugin = types2ws.get(type);

        Element root = null;

        root = plugin.marshall(query, paginationStart, paginationLimit,
                splitProjection, type, root, userWS, nameRoot, sort, sortOrder, parent);

        return root;

    }

    public void setTypes2ws(Map<String, IWSService> types2ws)
    {
        this.types2ws = types2ws;
    }

    public Map<String, IWSService> getTypes2ws()
    {
        return types2ws;
    }

    public void setAuthenticationWS(AuthenticationWS authenticationWS)
    {
        this.authenticationWS = authenticationWS;
    }

    public AuthenticationWS getAuthenticationWS()
    {
        return authenticationWS;
    }
}
