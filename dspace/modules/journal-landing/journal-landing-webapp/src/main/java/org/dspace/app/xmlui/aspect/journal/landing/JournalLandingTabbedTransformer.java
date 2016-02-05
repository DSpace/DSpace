/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.journal.landing;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Map;
import org.apache.avalon.framework.parameters.ParameterException;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.log4j.Logger;
import static org.dspace.app.xmlui.aspect.journal.landing.Const.*;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.ReferenceSet;
import org.dspace.authorize.AuthorizeException;
import org.xml.sax.SAXException;
import java.util.LinkedHashMap;
import org.datadryad.api.DryadJournal;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.workflow.DryadWorkflowUtils;

/**
 * Cocoon/DSpace transformer to produce a panel for the journal landing page,
 * with multiple tabs. The DRI produced here is handled by the Mirage xsl
 * stylesheet lib/xsl/aspect/JournalLandingPage/main.xsl.
 * 
 * @author Nathan Day
 */
public abstract class JournalLandingTabbedTransformer extends AbstractDSpaceTransformer {

    private static final Logger log = Logger.getLogger(JournalLandingTabbedTransformer.class);
    private final static SimpleDateFormat fmt = new SimpleDateFormat(fmtDateView);

    private String journalName;
    private DryadJournal dryadJournal;

    protected class DivData {
        public String n;
        public Message T_div_head;
        public int maxResults;
    }
    protected DivData divData;
    protected class TabData {
        public String n;
        public Message buttonLabel;
        public Message refHead;
        public Message valHead;
        public String dateFilter;
        public String facetQueryField;
        public QueryType queryType;
    }
    protected java.util.List<TabData> tabData;

    @Override
    public void setup(SourceResolver resolver, Map objectModel, String src,
            Parameters parameters) throws ProcessingException, SAXException,
            IOException
    {
        super.setup(resolver, objectModel, src, parameters);
        try {
            journalName = parameters.getParameter(PARAM_JOURNAL_NAME);
        } catch (ParameterException ex) {
            log.error(ex);
            throw new ProcessingException(ex.getMessage());
        }
        dryadJournal = new DryadJournal(this.context, this.journalName);
    }

    /**
     * Method to add a div element with multiple tabs, each containing a listing
     * of Dryad references and an associated value, e.g., an accessioned date
     * or a download count.
     * @param body DRI body element
     * @throws SAXException
     * @throws WingException
     * @throws UIException
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException 
     */
    protected void addStatsLists(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        Division outer = body.addDivision(divData.n,divData.n);
        outer.setHead(divData.T_div_head);

        // tab buttons
        List tablist = outer.addList(TABLIST, List.TYPE_ORDERED, TABLIST);
        for(TabData t : tabData) {
            tablist.addItem(t.buttonLabel);
        }
        for(TabData t : tabData) {
            Division wrapper = outer.addDivision(t.n, t.n);
            // dspace referenceset or list to hold references or countries
            Division items = wrapper.addDivision(ITEMS);
            ReferenceSet itemsContainer = items.addReferenceSet(t.n, ReferenceSet.TYPE_SUMMARY_LIST);
            itemsContainer.setHead(t.refHead);
            // dspace value list, to hold counts
            Division vals = wrapper.addDivision(VALS);
            List valsList = vals.addList(t.n, List.TYPE_SIMPLE, t.n);
            valsList.setHead(t.valHead);
            if (t.queryType == QueryType.DOWNLOADS ) {
                doDownloadsQuery(itemsContainer, valsList, dryadJournal, divData, t);
            } else if (t.queryType == QueryType.DEPOSITS ) {
                doDepositsQuery(itemsContainer, valsList, dryadJournal, divData, t);
            }
        }
    }

    /**
     * Populate the given reference and values lists with data from Solr on 
     * download statistics.
     * @param itemsContainer DRI ReferenceSet element to contain retrieved Items
     * @param countList DRI List element to contain download counts
     * @param dryadJournal DryadJournal object for the given 
     * @param divData query parameters for the current div
     * @param t query parameters for the current tab
     */
    private void doDownloadsQuery(ReferenceSet itemsContainer, List countList, DryadJournal dryadJournal, DivData divData, TabData t) {
        LinkedHashMap<Item, String> results = dryadJournal.getRequestsPerJournal(
            t.facetQueryField, t.dateFilter, divData.maxResults
        );
        if (results != null) {
            for (Item item : results.keySet()) {
                Item dataPackage = DryadWorkflowUtils.getDataPackage(context, item);
                try {
                    itemsContainer.addReference(dataPackage);
                    countList.addItem().addContent(results.get(item));
                } catch (WingException ex) {
                    log.error(ex);
                }
            }
        }
    }

    /**
     * Populate the given reference and values lists with recent deposit data
     * from Postgres.
     * @param itemsContainer DRI ReferenceSet element to contain retrieved Items
     * @param countList DRI List element to contain download counts
     * @param dryadJournal DryadJournal object for the given 
     * @param divData query parameters for the current div
     * @param t query parameters for the current tab
     */
    private void doDepositsQuery(ReferenceSet itemsContainer, List countList, DryadJournal dryadJournal, DivData divData, TabData t) {
        java.util.List<Item> packages = null;
        try {
            packages = dryadJournal.getArchivedPackagesSortedRecent(divData.maxResults);
        } catch (SQLException ex) {
            log.error(ex.getMessage());
            return;
        }
        for (Item item : packages) {
            DCValue[] dateAccessioned = item.getMetadata(dcDateAccessioned);
            if (dateAccessioned.length >= 1) {
                String dateStr = dateAccessioned[0].value;
                //String date = fmt.format(dateStr);
                if (dateStr != null) {
                    try {
                        itemsContainer.addReference(item);
                        countList.addItem().addContent(fmt.format(fmt.parse(dateStr)));
                    } catch (Exception ex) {
                        log.error(ex);
                    }
                }
            }
        }
    }
}
