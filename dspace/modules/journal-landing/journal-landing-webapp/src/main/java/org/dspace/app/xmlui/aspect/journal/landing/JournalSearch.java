/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.journal.landing;

import org.apache.log4j.Logger;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;

import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import static org.dspace.app.xmlui.aspect.journal.landing.Const.*;

import java.sql.SQLException;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.authorize.AuthorizeException;
import org.xml.sax.SAXException;
import java.io.IOException;
import java.util.Map;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.SourceResolver;

/**
 * Add "Search data in Dryad associated with ..." by-journal search field
 * to the journal-landing page.
 * 
 * @author Nathan Day
 */
public class JournalSearch extends AbstractDSpaceTransformer {
    
    private static final Logger log = Logger.getLogger(JournalSearch.class);
    private static final Message T_panel_head = message("xmlui.JournalLandingPage.JournalSearch.panel_head"); 
    
    private String journalName;
    
    @Override
    public void setup(SourceResolver resolver, Map objectModel, String src,
            Parameters parameters) throws ProcessingException, SAXException,
            IOException
    {
        super.setup(resolver, objectModel, src, parameters);
        try {
            journalName = parameters.getParameter(PARAM_JOURNAL_NAME);
        } catch (Exception ex) {
            log.error(ex);
            throw(new ProcessingException("Bad access of journal name"));
        }
    }
    
    @Override
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        Division searchDiv = body.addDivision(SEARCH_DIV, SEARCH_DIV);
        searchDiv.setHead(T_panel_head.parameterize(journalName));
    }
}