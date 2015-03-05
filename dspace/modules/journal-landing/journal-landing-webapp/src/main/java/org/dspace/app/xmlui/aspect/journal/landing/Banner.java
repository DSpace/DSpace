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
import java.util.Map;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.SourceResolver;
import org.datadryad.api.DryadJournal;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.authorize.AuthorizeException;
import org.xml.sax.SAXException;

/**
 *
 * @author Nathan Day
 */
public class Banner extends AbstractDSpaceTransformer {

    private static final Logger log = Logger.getLogger(Banner.class);

    private static final Message T_Member = message("xmlui.JournalLandingPage.Banner.mem");
    private static final Message T_Payment = message("xmlui.JournalLandingPage.Banner.pay");
    private static final Message T_Integration = message("xmlui.JournalLandingPage.Banner.int");
    private static final Message T_Authors = message("xmlui.JournalLandingPage.Banner.aut");
    private static final Message T_Embargo = message("xmlui.JournalLandingPage.Banner.dat");
    private static final Message T_Hidden = message("xmlui.JournalLandingPage.Banner.met");
    private static final Message T_packages = message("xmlui.JournalLandingPage.Banner.pac");

    private String journalName;
    private DryadJournal dryadJournal;

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
        try {
            dryadJournal = new DryadJournal(context, journalName);
        } catch (Exception ex) {
            log.error(ex);
            throw(new ProcessingException("Failed to make handler for " + journalName));
        }
    }

    @Override
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        Division outer = body.addDivision(BANNER_DIV_OUTER);
        Division inner = outer.addDivision(BANNER_DIV_INNER);
        inner.setHead(journalName);

        String journalDescr = "Journal description";
        inner.addPara().addContent(journalDescr);

        Para pMem = inner.addPara(BANNER_MEM, BANNER_MEM);
        pMem.addHighlight(null).addContent(T_Member);
        String journalMemberInfo = "lorem ipsum";
        pMem.addContent(": " + journalMemberInfo);

        Para pPay = inner.addPara(BANNER_PAY, BANNER_PAY);
        pPay.addHighlight(null).addContent(T_Payment);
        String journalPaymentPlanInfo = "lorem ipsum";
        pPay.addContent(": " + journalPaymentPlanInfo);

        Para pInt = inner.addPara(BANNER_INT, BANNER_INT);
        pInt.addHighlight(null).addContent(T_Integration);
        String journalIntegrationInfo = "lorem ipsum";
        pInt.addContent(": " + journalIntegrationInfo);

        Para pAut = inner.addPara(BANNER_AUT, BANNER_AUT);
        pAut.addHighlight(null).addContent(T_Authors);
        String journalAuthorSubmit = "lorem ipsum";
        pAut.addContent(": " + journalAuthorSubmit);

        Para pDat = inner.addPara(BANNER_DAT, BANNER_DAT);
        pDat.addHighlight(null).addContent(T_Embargo);
        String journalDataEmbargo = "lorem ipsum";
        pDat.addContent(": " + journalDataEmbargo);

        Para pMet = inner.addPara(BANNER_MET, BANNER_MET);
        pMet.addHighlight(null).addContent(T_Hidden);
        String journalMetadataInfo = "lorem ipsum";
        pMet.addContent(": " + journalMetadataInfo);

        Para pPac = inner.addPara(BANNER_PAC, BANNER_PAC);
        pPac.addHighlight(null).addContent(T_packages);
        int archivedPackageCount = dryadJournal.getArchivedPackagesCount();
        pPac.addContent(": " + Integer.toString(archivedPackageCount));
    }
}
