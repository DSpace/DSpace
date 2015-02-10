/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
import org.apache.avalon.framework.parameters.ParameterException;
import java.net.URLEncoder;

/**
 *
 * @author Nathan Day
 */
public class JournalSearch extends AbstractDSpaceTransformer {
    
    private static final Logger log = Logger.getLogger(JournalSearch.class);
    private static final Message T_panel_head = message("xmlui.JournalLandingPage.JournalSearch.panel_head"); 
    
    @Override
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        String journalName = null;
        try {
            journalName = parameters.getParameter(PARAM_JOURNAL_NAME);
        } catch (ParameterException ex) {
            log.error((ex));
        }
        if (journalName == null || journalName.length() == 0) return;

        // ------------------
        // Search data in Dryad associated with Journal X
        // 
        // ------------------
        Division searchDiv = body.addDivision(SEARCH_DIV, SEARCH_DIV);
        searchDiv.setHead(T_panel_head.parameterize(journalName));
    }
}