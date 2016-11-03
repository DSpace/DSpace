package org.dspace.app.xmlui.aspect.journal.landing;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.log4j.Logger;
import org.datadryad.api.DryadJournalStats;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.ReferenceSet;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Returns download data for journal for given timeframe.
 * @author Nathan Day
 */
public class JournalDownloads extends AbstractDSpaceTransformer
{

    private static final Logger log = Logger.getLogger(JournalDownloads.class);

    private String downloadsRange;
    private String solrRangeExp;
    List<Integer> archivedDataFiles;

    @Override
    public void setup(SourceResolver resolver, Map objectModel, String src,
                      Parameters parameters)
            throws ProcessingException, SAXException, IOException
    {
        super.setup(resolver, objectModel, src, parameters);
        try {
            String journalName = parameters.getParameter(Const.PARAM_JOURNAL_NAME);
            downloadsRange = parameters.getParameter(Const.PARAM_DOWNLOAD_DURATION);
            archivedDataFiles = DryadJournalStats.getArchivedDataFiles(context, journalName);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new ProcessingException(e.getMessage());
        }
        if (Const.solrQueryToDuration.containsKey(downloadsRange)) {
            solrRangeExp = Const.solrQueryToDuration.get(downloadsRange);
        } else {
            throw new ProcessingException("Bad download query range.");
        }
    }

    @Override
    public void addBody(Body body) throws WingException, SQLException, SAXException, IOException, AuthorizeException
    {
        if (archivedDataFiles == null || archivedDataFiles.size() == 0)
            return;
        super.addBody(body);
        LinkedHashMap<Item, Long> refs = DryadJournalStats.getDownloadCounts(this.context, archivedDataFiles, solrRangeExp);
        Division outer = body.addDivision(Const.JOURNAL_STATS, Const.JOURNAL_STATS);
        Division inner = outer.addDivision(downloadsRange, Const.JOURNAL_STATS_DOWN);
        Division items = inner.addDivision(Const.ITEMS);
        Division vals = inner.addDivision(Const.VALS);
        ReferenceSet itemsContainer = items.addReferenceSet(Const.ITEMS, ReferenceSet.TYPE_SUMMARY_LIST);
        if (downloadsRange.equals(Const.solrQueryMonth)) {
            itemsContainer.setHead(Const.T_desc_month);
        } else if (downloadsRange.equals(Const.solrQueryYear)) {
            itemsContainer.setHead(Const.T_desc_year);
        } else if (downloadsRange.equals(Const.solrQueryAlltime)) {
            itemsContainer.setHead(Const.T_desc_alltime);
        }
        org.dspace.app.xmlui.wing.element.List countList = vals.addList(Const.ITEMS, org.dspace.app.xmlui.wing.element.List.TYPE_SIMPLE, Const.ITEMS);
        countList.setHead(Const.T_ref_head);
        for(Item item : refs.keySet()) {
            itemsContainer.addReference(item);
            countList.addItem(refs.get(item).toString());
        }
    }
}
