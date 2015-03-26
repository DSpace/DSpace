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
import org.dspace.app.xmlui.wing.element.Para;
import static org.dspace.app.xmlui.aspect.journal.landing.Const.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.SourceResolver;
import org.datadryad.api.DryadJournal;
import org.dspace.JournalUtils;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.authority.Concept;
import org.xml.sax.SAXException;

/**
 * Journal landing page transformer for top panel containing journal information.
 * 
 * @author Nathan Day
 */
public class Banner extends AbstractDSpaceTransformer {

    private static final Logger log = Logger.getLogger(Banner.class);

    private static final Message T_Member = message("xmlui.JournalLandingPage.Banner.mem");
    private static final Message T_sponsorship = message("xmlui.JournalLandingPage.Banner.spo");
    private static final Message T_Integration = message("xmlui.JournalLandingPage.Banner.int");
    private static final Message T_Authors = message("xmlui.JournalLandingPage.Banner.aut");
    private static final Message T_Embargo = message("xmlui.JournalLandingPage.Banner.dat");
    private static final Message T_Hidden = message("xmlui.JournalLandingPage.Banner.met");
    private static final Message T_packages = message("xmlui.JournalLandingPage.Banner.pac");

    private static final Message T_yes = message("xmlui.JournalLandingPage.Banner.yes");
    private static final Message T_no = message("xmlui.JournalLandingPage.Banner.no");
    private static final Message T_allowed = message("xmlui.JournalLandingPage.Banner.allowed");
    private static final Message T_notAllowed = message("xmlui.JournalLandingPage.Banner.notAllowed");
    private static final Message T_none = message("xmlui.JournalLandingPage.Banner.none");
    private static final Message T_acceptance = message("xmlui.JournalLandingPage.Banner.onAcceptance");
    private static final Message T_review = message("xmlui.JournalLandingPage.Banner.onReview");

    private String journalName;
    private DryadJournal dryadJournal;
    private Concept journalConcept;

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
        try {
            journalConcept = JournalUtils.getJournalConceptByName(context,journalName);
        } catch (SQLException ex) {
            throw(new ProcessingException("Error retrieving Concept for '" + journalName + "': " + ex.getMessage()));
        }
        if (journalConcept == null) {
            throw(new ProcessingException("Failed to retrieve Concept for " + journalName));
        }
    }

    @Override
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        Division outer = body.addDivision(BANNER_DIV_OUTER);
        Division inner = outer.addDivision(BANNER_DIV_INNER);

        // [JOURNAL FULL NAME]
        inner.setHead(journalName);

        // [Journal description]
        String journalDescr = JournalUtils.getDescription(journalConcept);
        if (journalDescr != null) {
            inner.addPara().addContent(journalDescr);
        }

        // Member: ___
        String memberName = JournalUtils.getMemberName(journalConcept);
        if (memberName != null && memberName.length() > 0) {
            Para pMem = inner.addPara(BANNER_MEM, BANNER_MEM);
            pMem.addHighlight(null).addContent(T_Member);
            pMem.addContent(": ");
            pMem.addContent(memberName);
        }

        // Sponsorship: ___
        Para pSpo = inner.addPara(BANNER_SPO, BANNER_SPO);
        pSpo.addHighlight(null).addContent(T_sponsorship);
        pSpo.addContent(": ");
        String sponsorName = JournalUtils.getSponsorName(journalConcept);
        if (sponsorName != null) {
            pSpo.addContent(sponsorName);
        } else {
            pSpo.addContent(T_none);
        }

        // Submission integration: ___
        Para pInt = inner.addPara(BANNER_INT, BANNER_INT);
        pInt.addHighlight(null).addContent(T_Integration);
        pInt.addContent(": ");
        if (JournalUtils.getBooleanIntegrated(journalConcept)) {
            pInt.addContent(T_yes);
        } else {
            pInt.addContent(T_no);
        }

        // When authors submit data: ___
        Para pAut = inner.addPara(BANNER_AUT, BANNER_AUT);
        pAut.addHighlight(null).addContent(T_Authors);
        pAut.addContent(": ");
        if (JournalUtils.getBooleanAllowReviewWorkflow(journalConcept)) {
            pAut.addContent(T_review);
        } else {
            pAut.addContent(T_acceptance);
        }

        // Data embargo: ___
        Para pDat = inner.addPara(BANNER_DAT, BANNER_DAT);
        pDat.addHighlight(null).addContent(T_Embargo);
        pDat.addContent(": ");
        if (JournalUtils.getBooleanEmbargoAllowed(journalConcept)) {
            pDat.addContent(T_allowed);
        } else {
            pDat.addContent(T_notAllowed);
        }

        // Metadata hidden until article publication: ___
        Para pMet = inner.addPara(BANNER_MET, BANNER_MET);
        pMet.addHighlight(null).addContent(T_Hidden);
        pMet.addContent(": ");
        if (JournalUtils.getBooleanPublicationBlackout(journalConcept)) {
            pMet.addContent(T_yes);
        } else {
            pMet.addContent(T_no);
        }

        // Total number of data packages: ___
        Para pPac = inner.addPara(BANNER_PAC, BANNER_PAC);
        pPac.addHighlight(null).addContent(T_packages);
        int archivedPackageCount = dryadJournal.getArchivedPackagesCount();
        pPac.addContent(": " + Integer.toString(archivedPackageCount));
    }
}
