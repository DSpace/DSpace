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
/*
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Table;
*/
import static org.dspace.app.xmlui.aspect.journal.landing.Const.*;

import java.sql.SQLException;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.authorize.AuthorizeException;
import org.xml.sax.SAXException;
import java.io.IOException;

/**
 *
 * @author Nathan Day
 */
public class DryadInfo extends AbstractDSpaceTransformer {

    private static final Logger log = Logger.getLogger(DryadInfo.class);
    private static final Message T_Information = message("xmlui.JournalLandingPage.DryadInfo.inf");
    private static final Message T_Member = message("xmlui.JournalLandingPage.DryadInfo.mem");
    private static final Message T_Payment = message("xmlui.JournalLandingPage.DryadInfo.pay");
    private static final Message T_Embargo = message("xmlui.JournalLandingPage.DryadInfo.dat");
    private static final Message T_Hidden = message("xmlui.JournalLandingPage.DryadInfo.met");

    @Override
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {

        // ------------------
        // Information related to Dryad
        // Member:
        // Payment plan:
        // Data embargo:
        // Metadata hidden:
        // ------------------
        Division dryadInfoWrapper = body.addDivision(DRYAD_INFO_WRA);
        Division dryadInfo = dryadInfoWrapper.addDivision(DRYAD_INFO_INN);

        dryadInfo.setHead(T_Information);
        Para pMem = dryadInfo.addPara(DRYAD_INFO_MEM, DRYAD_INFO_MEM);
        pMem.addContent(T_Member);
        pMem.addContent(": " + "lorem ipsum");
        Para pPay = dryadInfo.addPara(DRYAD_INFO_PAY, DRYAD_INFO_PAY);
        pPay.addContent(T_Payment);
        pPay.addContent(": " + "lorem ipsum");
        Para pDat = dryadInfo.addPara(DRYAD_INFO_DAT, DRYAD_INFO_DAT);
        pDat.addContent(T_Embargo);
        pDat.addContent(": " + "lorem ipsum");
        Para pMet = dryadInfo.addPara(DRYAD_INFO_MET, DRYAD_INFO_MET);
        pMet.addContent(T_Hidden);
        pMet.addContent(": " + "lorem ipsum");
    }
}