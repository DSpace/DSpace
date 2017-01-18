/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.journal.landing;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.log4j.Logger;
import org.datadryad.api.DryadJournalConcept;
import org.datadryad.api.DryadJournalStats;
import org.dspace.JournalUtils;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.dspace.app.xmlui.aspect.journal.landing.Const.*;

/**
 * Add "Browse for data" div to journal landing page, containing a panel with
 * four tabs displaying recently published and most popular downloads.
 * 
 * @author Nathan Day
 */
public class JournalStats extends AbstractDSpaceTransformer
{

    private static final Logger log = Logger.getLogger(JournalStats.class);

    private static final String solrDatePastMonth = ConfigurationManager.getProperty("landing-page.stats.query.month");
    private static final String solrDatePastYear  = ConfigurationManager.getProperty("landing-page.stats.query.year");
    private static final String solrDateAllTime   = ConfigurationManager.getProperty("landing-page.stats.query.alltime");
    private static final String facetQueryField   = ConfigurationManager.getProperty("landing-page.stats.query.facet");

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

    private static final Message T_panel_head = message("xmlui.JournalLandingPage.JournalSearch.panel_head");

    private final static SimpleDateFormat fmt = new SimpleDateFormat(Const.fmtDateView);

    protected static final int displayCount = ConfigurationManager.getIntProperty("landing-page.stats.item-count");


    protected class DivData {
        public String n;
        public Message T_div_head;
        public int maxResults;
    }

    protected DivData divData;

    protected class TabData {
        public String n;
        public String rend;
        public Message buttonLabel;
        public Message refHead;
        public Message valHead;
        public String dateFilter;
        public String facetQueryField;
        public QueryType queryType;
    }

    protected java.util.List<TabData> tabData;

    protected DryadJournalConcept journalConcept;
    protected String journalISSN;
    protected String journalName;
    protected String journalAbbr;
    @Override
    public void setup(SourceResolver resolver, Map objectModel, String src,
            Parameters parameters) throws ProcessingException, SAXException,
            IOException
    {
        super.setup(resolver, objectModel, src, parameters);
        try {
            journalISSN = parameters.getParameter(Const.PARAM_JOURNAL_ISSN);
            journalName = parameters.getParameter(Const.PARAM_JOURNAL_NAME);
            journalAbbr = parameters.getParameter(Const.PARAM_JOURNAL_ABBR);
            journalConcept = JournalUtils.getJournalConceptByISSN(journalISSN);
        } catch (Exception ex) {
            log.error(ex);
            throw(new ProcessingException("Bad access of journal concept: " + ex.getMessage()));
        }
        if (journalConcept == null) {
            throw(new ProcessingException("Failed to retrieve journal for ISSN: " + journalISSN));
        }

        divData = new DivData();
        TabData tb1 = new TabData();
        TabData tb2 = new TabData();
        TabData tb3 = new TabData();
        TabData tb4 = new TabData();
        tabData = Arrays.asList(tb1, tb2, tb3, tb4);

        divData.n = JOURNAL_STATS;
        divData.T_div_head = T_div_head;
        divData.maxResults = displayCount;

        tb1.n = JOURNAL_STATS_DEPS;
        tb1.buttonLabel = T_btnRecPub;
        tb1.dateFilter = solrDatePastMonth;
        tb1.refHead = T_desc_RecPub;
        tb1.valHead = T_date;
        tb1.queryType = QueryType.DEPOSITS;

        tb2.n = JOURNAL_STATS_DOWN;
        tb2.rend = solrQueryMonth;
        tb2.buttonLabel = T_btn_month;
        tb2.dateFilter = solrDatePastMonth;
        tb2.refHead = T_desc_month;
        tb2.valHead = T_ref_head;
        tb2.queryType = QueryType.DOWNLOADS;
        tb2.facetQueryField = facetQueryField;

        tb3.n = JOURNAL_STATS_DOWN;
        tb3.rend = solrQueryYear;
        tb3.buttonLabel = T_btn_year;
        tb3.dateFilter = solrDatePastYear;
        tb3.refHead = T_desc_year;
        tb3.valHead = T_ref_head;
        tb3.queryType = QueryType.DOWNLOADS;
        tb3.facetQueryField = facetQueryField;

        tb4.n = JOURNAL_STATS_DOWN;
        tb4.rend = solrQueryAlltime;
        tb4.buttonLabel = T_btn_alltime;
        tb4.dateFilter = solrDateAllTime;
        tb4.refHead = T_desc_alltime;
        tb4.valHead = T_ref_head;
        tb4.queryType = QueryType.DOWNLOADS;
        tb4.facetQueryField = facetQueryField;
    }

    @Override
    public void addBody(Body body) throws WingException, SQLException
    {
        // BANNER --------------------------------------------------------------------------
        Division outer = body.addDivision(BANNER_DIV_OUTER);
        Division inner = outer.addDivision(BANNER_DIV_INNER);

        // [JOURNAL FULL NAME]
        // hidden-p rather than h1 to override default Mirage display
        inner.addHidden(journalName);

        // [Journal description]
        String journalDescr = journalConcept.getDescription();
        if (journalDescr != null && journalDescr.length() > 0) {
            inner.addPara().addContent(journalDescr);
        }

        // Member: ___
        String memberName = journalConcept.getMemberName();
        if (memberName != null && memberName.length() > 0) {
            Para pMem = inner.addPara(BANNER_MEM, BANNER_MEM);
            pMem.addHighlight(null).addContent(T_Member);
            pMem.addContent(BANNER_SEP);
            pMem.addContent(memberName);
        }

        // Sponsorship: ___
        String sponsorName = journalConcept.getSponsorName();
        if (sponsorName != null && sponsorName.length() > 0) {
            Para pSpo = inner.addPara(BANNER_SPO, BANNER_SPO);
            pSpo.addHighlight(null).addContent(T_sponsorship);
            pSpo.addContent(BANNER_SEP);
            pSpo.addContent(sponsorName);
        }

        // Submission integration: ___
        Para pInt = inner.addPara(BANNER_INT, BANNER_INT);
        pInt.addHighlight(null).addContent(T_Integration);
        pInt.addContent(BANNER_SEP);
        if (journalConcept.getIntegrated()) {
            pInt.addContent(T_yes);
        } else {
            pInt.addContent(T_no);
        }

        // When authors submit data: ___
        Para pAut = inner.addPara(BANNER_AUT, BANNER_AUT);
        pAut.addHighlight(null).addContent(T_Authors);
        pAut.addContent(BANNER_SEP);
        if (journalConcept.getAllowReviewWorkflow()) {
            pAut.addContent(T_review);
        } else {
            pAut.addContent(T_acceptance);
        }

        // Data embargo: ___
        Para pDat = inner.addPara(BANNER_DAT, BANNER_DAT);
        pDat.addHighlight(null).addContent(T_Embargo);
        pDat.addContent(BANNER_SEP);
        if (journalConcept.getAllowEmbargo()) {
            pDat.addContent(T_allowed);
        } else {
            pDat.addContent(T_notAllowed);
        }

        // Metadata hidden until article publication: ___
        Para pMet = inner.addPara(BANNER_MET, BANNER_MET);
        pMet.addHighlight(null).addContent(T_Hidden);
        pMet.addContent(BANNER_SEP);
        if (journalConcept.getPublicationBlackout()) {
            pMet.addContent(T_yes);
        } else {
            pMet.addContent(T_no);
        }

        // Total number of data packages: ___
        Para pPac = inner.addPara(BANNER_PAC, BANNER_PAC);
        pPac.addHighlight(null).addContent(T_packages);
        long archivedPackageCount = DryadJournalStats.getArchivedPackagesCount(context, journalName);
        pPac.addContent(BANNER_SEP + Long.toString(archivedPackageCount));

        // SEARCH -----------------------------------------------------------------------------------------
        Division searchDiv = body.addDivision(SEARCH_DIV, SEARCH_DIV);
        searchDiv.setHead(T_panel_head.parameterize(journalName));

        // JOURNAL STATS -----------------------------------------------------------------------------------------
        Division statsOuter = body.addDivision(divData.n, divData.n);
        statsOuter.setHead(divData.T_div_head);
        // tab buttons to page
        List tablist = statsOuter.addList(TABLIST, List.TYPE_ORDERED, TABLIST);
        for (TabData t : tabData) {
            tablist.addItem(t.buttonLabel);
            if (t.queryType == QueryType.DEPOSITS) {
                LinkedHashMap<Item, String> depositData =
                        DryadJournalStats.getArchivedPackagesSortedRecent(context, journalName, fmt, displayCount);
                addDepositTabData(statsOuter, t, depositData);
            } else if (t.queryType == QueryType.DOWNLOADS) {
                addDownloadsMarkup(statsOuter, t);
            }
        }
    }

    @Override
    public void addOptions(Options options) throws SAXException, WingException,
            SQLException, IOException, AuthorizeException
    {
        options.addList("DryadSubmitData");
        options.addList("DryadConnect");
        options.addList("DryadMail");
    }

    @Override
    public void addPageMeta(PageMeta pageMeta) throws
            WingException, SQLException, SAXException, IOException, AuthorizeException
    {
        super.addPageMeta(pageMeta);
        // standard values
        Request request = ObjectModelHelper.getRequest(objectModel);
        pageMeta.addMetadata("contextPath").addContent(contextPath);
        pageMeta.addMetadata("request","queryString").addContent(request.getQueryString());
        pageMeta.addMetadata("request","scheme").addContent(request.getScheme());
        pageMeta.addMetadata("request","serverPort").addContent(request.getServerPort());
        pageMeta.addMetadata("request","serverName").addContent(request.getServerName());
        pageMeta.addMetadata("request","URI").addContent(request.getSitemapURI());

        // Get realPort and nodeName
        String port = ConfigurationManager.getProperty("dspace.port");
        String nodeName = ConfigurationManager.getProperty("dryad.home");

        // If we're using Apache, we may not have the real port in serverPort
        if (port != null) {
            pageMeta.addMetadata("request", "realServerPort").addContent(port);
        }
        else {
            pageMeta.addMetadata("request", "realServerPort").addContent("80");
        }
        if (nodeName != null) {
            pageMeta.addMetadata("dryad", "node").addContent(nodeName);
        }
        pageMeta.addMetadata("request","journalISSN").addContent(journalISSN);
        pageMeta.addMetadata("request","journalName").addContent(journalName);
        pageMeta.addMetadata("request","journalAbbr").addContent(journalAbbr);
        pageMeta.addMetadata("request","journalCover").addContent(journalConcept.getCoverImage());
        pageMeta.addMetadata("request","journalWebsite").addContent(journalConcept.getWebsite());

    }

    private void addDepositTabData(Division outer, TabData t, LinkedHashMap<Item, String> depositData) throws WingException
    {
        if (depositData.entrySet().size() == 0)
            return;
        Division wrapper = outer.addDivision(t.n, t.n);
        // dspace referenceset or list to hold references
        Division items = wrapper.addDivision(ITEMS);
        ReferenceSet itemsContainer = items.addReferenceSet(t.n, ReferenceSet.TYPE_SUMMARY_LIST);
        itemsContainer.setHead(t.refHead);
        // dspace value list, to hold counts
        Division vals = wrapper.addDivision(VALS);
        List valsList = vals.addList(t.n, List.TYPE_SIMPLE, t.n);
        valsList.setHead(t.valHead);
        for (Map.Entry<Item, String> e : depositData.entrySet())
        {
            try {
                itemsContainer.addReference(e.getKey());
                valsList.addItem().addContent(e.getValue());
            } catch (Exception ex) {
                log.error(ex.getMessage());
            }
        }
    }

    private void addDownloadsMarkup(Division outer, TabData t) throws WingException
    {
        Division wrapper = outer.addDivision(t.n, t.rend);
        Division items = wrapper.addDivision(ITEMS);
        ReferenceSet itemsContainer = items.addReferenceSet(t.n, ReferenceSet.TYPE_SUMMARY_LIST);
        // The header is actually set in JournalDownloads.java.
//        itemsContainer.setHead(t.refHead);
        Division vals = wrapper.addDivision(VALS);
        List valsList = vals.addList(t.n, List.TYPE_SIMPLE, t.n);
        valsList.setHead(t.valHead);
    }
}

