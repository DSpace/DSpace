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
import org.dspace.app.xmlui.wing.element.Para;
import static org.dspace.app.xmlui.aspect.journal.landing.Const.*;

import java.io.IOException;
import java.sql.SQLException;
import org.apache.avalon.framework.parameters.ParameterException;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.submit.utils.DryadJournalSubmissionUtils;
import org.xml.sax.SAXException;

/**
 *
 * @author Nathan Day
 */
public class Banner extends AbstractDSpaceTransformer {

    private static final Logger log = Logger.getLogger(Banner.class);

    private static final Message T_pub = message("xmlui.JournalLandingPage.Banner.pub");
    private static final Message T_soc = message("xmlui.JournalLandingPage.Banner.soc");
    private static final Message T_edi = message("xmlui.JournalLandingPage.Banner.edi");

    @Override
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        // ------------------
        // Journal X
        // 1 sentence scope
        // Publisher:
        // Society: 
        // Editorial review:
        // ------------------
        String journalName;
        try {
            journalName = this.parameters.getParameter(PARAM_JOURNAL_NAME);
        } catch (ParameterException ex) {
            log.error("Failed to retrieve journal metadata from parameters");
            return;
        }

        Division outer = body.addDivision(BANNER_DIV_OUTER);
        Division inner = outer.addDivision(BANNER_DIV_INNER);
        inner.setHead(journalName);

        inner.addPara().addContent("1 sentence scope");

        Para pub = inner.addPara(BANNER_PUB, BANNER_PUB);
        pub.addContent(T_pub);
        pub.addContent(": " + "lorem ipsum");
        Para soc = inner.addPara(BANNER_SOC, BANNER_SOC);
        soc.addContent(T_soc);
        soc.addContent(": " + "lorem ipsum");
        Para edi = inner.addPara(BANNER_EDI, BANNER_EDI);
        edi.addContent(T_edi);
        edi.addContent(": " + "lorem ipsum");
    }
}