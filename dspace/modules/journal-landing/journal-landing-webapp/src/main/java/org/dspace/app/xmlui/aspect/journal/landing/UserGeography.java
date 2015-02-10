/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dspace.app.xmlui.aspect.journal.landing;

import org.apache.log4j.Logger;

import static org.dspace.app.xmlui.aspect.journal.landing.Const.*;

import org.xml.sax.SAXException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.SourceResolver;
import org.dspace.app.xmlui.utils.UIException;
import static org.dspace.app.xmlui.wing.AbstractWingTransformer.message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.discovery.SearchServiceException;

/**
 *
 * @author Nathan Day
 */
public class UserGeography extends JournalLandingTabbedTransformer {
    
    private static final Logger log = Logger.getLogger(UserGeography.class);
    
    @Override
    public void setup(SourceResolver resolver, Map objectModel, String src,
            Parameters parameters) throws ProcessingException, SAXException,
            IOException
    {
        super.setup(resolver, objectModel, src, parameters);
        
        divData = new DivData();
        divData.n = USER_GEO;
        divData.T_div_head = message("xmlui.JournalLandingPage.UserGeography.panel_head");

        tabData = new ArrayList<TabData>(2);
        TabData tb1 = new TabData();
        tb1.n = USER_GEO_VIEWS;
        tb1.buttonLabel = message("xmlui.JournalLandingPage.UserGeography.tab-views");
        tb1.refHead = message("xmlui.JournalLandingPage.UserGeography.item_head");
        tb1.valHead = message("xmlui.JournalLandingPage.UserGeography.tab-views");
        tabData.add(tb1);

        TabData tb2 = new TabData();
        tb2.n = USER_GEO_DOWNLOADS;
        tb2.buttonLabel = message("xmlui.JournalLandingPage.UserGeography.tab-downloads");
        tb2.refHead = message("xmlui.JournalLandingPage.UserGeography.item_head");
        tb2.valHead = message("xmlui.JournalLandingPage.UserGeography.tab-downloads");
        tabData.add(tb2);        
    }

    @Override
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        super.addBody(body);
    }

    @Override
    protected void performSearch(DSpaceObject object) throws SearchServiceException, UIException {
        /*
        JournalUserGeo journalUserGeo = new JournalUserGeo(this.context);
        String responseDownload = journalUserGeo.getResponseDownload();
        String getResponseView = journalUserGeo.getResponseView();
        */
    }
}
