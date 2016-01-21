/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.cocoon;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.generation.AbstractGenerator;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.content.Collection;
import org.dspace.content.authority.ChoiceAuthorityServiceImpl;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.ChoicesXMLGenerator;

import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.xml.sax.SAXException;

import org.apache.log4j.Logger;

/**
 * Generate XML description of Choice values to be used by
 * AJAX UI client.
 *
 * @author Larry Stone
 */
public class AJAXMenuGenerator extends AbstractGenerator
{
    private static Logger log = Logger.getLogger(AJAXMenuGenerator.class);

    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected ChoiceAuthorityService choiceAuthorityService = ContentAuthorityServiceFactory.getInstance().getChoiceAuthorityService();

    /**
     * Generate the AJAX response Document.
     *
     * Looks for request paraemters:
     *  field - MD field key, i.e. form key, REQUIRED.
     *  start - index to start from, default 0.
     *  limit - max number of lines, default 1000.
     *  format - opt. result XML/XHTML format: "select", "ul", "xml"(default)
     *  locale - explicit locale, pass to choice plugin
     */
    public void generate()
        throws IOException, SAXException, ProcessingException
    {
        try {
            int start = 0;
            int limit = 1000;
            Collection collection = null;
            String format = parameters.getParameter("format",null);
            String field = parameters.getParameter("field",null);
            String sstart = parameters.getParameter("start",null);
            if (sstart != null && sstart.length() > 0)
            {
                start = Integer.parseInt(sstart);
            }
            String slimit = parameters.getParameter("limit",null);
            if (slimit != null && slimit.length() > 0)
            {
                limit = Integer.parseInt(slimit);
            }
            String scoll = parameters.getParameter("collection",null);
            if (scoll != null && scoll.length() > 0)
            {
                collection = collectionService.find(ContextUtil.obtainContext(ObjectModelHelper.getRequest(objectModel)), UUID.fromString(scoll));
            }
            String query = parameters.getParameter("query",null);

            // localization
            String locale = parameters.getParameter("locale",null);
            log.debug("AJAX menu generator: field="+field+", query="+query+", start="+sstart+", limit="+slimit+", format="+format+", field="+field+", query="+query+", start="+sstart+", limit="+slimit+", format="+format+", locale = "+locale);

            Choices result = choiceAuthorityService.getMatches(field, query, collection, start, limit, locale, true);

            log.debug("Result count = "+result.values.length+", default="+result.defaultSelected);

            ChoicesXMLGenerator.generate(result, format, contentHandler);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
    }
}
