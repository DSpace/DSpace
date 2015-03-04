/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dspace.app.xmlui.aspect.journal.landing;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import org.dspace.content.DSpaceObject;
import org.dspace.discovery.SearchServiceException;
import org.xml.sax.SAXException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Locale;
import org.datadryad.api.DryadJournal.*;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.content.Item;

/**
 *
 * @author Nathan Day
 */
public class JournalLandingTabbedTransformer extends AbstractDSpaceTransformer {
    
    private static final Logger log = Logger.getLogger(JournalLandingTabbedTransformer.class);
    private final static SimpleDateFormat fmt = new SimpleDateFormat(fmtDateView);
    private int currentMonth;
    private String currentMonthStr;
    private int currentYear;
    Locale defaultLocale = org.dspace.core.I18nUtil.getDefaultLocale();

    // cocoon parameters
    protected String journalName;

    // performSearch() values
    protected ArrayList<Item> references;
    protected ArrayList<String> values;

    // container for data pertaining to entire div
    protected class DivData {
        public String n;
        public Message T_div_head;
    }
    protected DivData divData;
    protected class TabData {
        public String n;
        public Message buttonLabel;
        public Message refHead;
        public Message valHead;
        public String dateFilter;
    }
    protected ArrayList<TabData> tabData;

    protected int getCurrentMonth() {
        return currentMonth;
    }
    protected int getCurrentYear() {
        return currentYear;
    }
    protected String getCurrentMonthStr() {
        return currentMonthStr;
    }

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
        Calendar cal = new GregorianCalendar();
        Date now = new Date();
        cal.setTime(now);
        currentMonth = cal.get(Calendar.MONTH);
        currentYear = cal.get(Calendar.YEAR);
        currentMonthStr = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, defaultLocale);
    }

    @Override
    public void addBody(Body body) throws SAXException, WingException,
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
            Division items = wrapper.addDivision(ITEMS);
            // reference list
            ReferenceSet refs = items.addReferenceSet(t.n, ReferenceSet.TYPE_SUMMARY_LIST);
            refs.setHead(t.refHead);
            // dspace item value list
            Division count = wrapper.addDivision(VALS);
            List list = count.addList(t.n, List.TYPE_SIMPLE, t.n);
            list.setHead(t.valHead);
            
            references = new ArrayList<Item>();
            values = new ArrayList<String>();
            try {
                retrieveItemResults();
            } catch (SearchServiceException e) {
                log.error(e.getMessage(), e);
            }
            if (references.size() > 0) {
                for (DSpaceObject ref : references)
                    refs.addReference(ref);
                for (String s : values)
                    list.addItem().addContent(s);
            }
        }
    }

    public void retrieveItemResults() throws SearchServiceException, UIException, SQLException {
        org.datadryad.api.DryadJournal dryadJournal = new org.datadryad.api.DryadJournal(this.context, this.journalName);
        SortedQueryResults sortedItems = dryadJournal.getRequestsPerJournal("site", "owningItem", "[* TO NOW]", 10);
        if (sortedItems.items.size() != sortedItems.values.size()) {
            log.error("Mismatch in data returned by getRequestsPerJournal()");
            return;
        }
        Iterator<Item> itIt = sortedItems.items.iterator();
        Iterator<String> valIt = sortedItems.values.iterator();
        while (itIt.hasNext() && valIt.hasNext() && references.size() < displayCount) {
            references.add(itIt.next());
            values.add(valIt.next());
        }
    }
}
