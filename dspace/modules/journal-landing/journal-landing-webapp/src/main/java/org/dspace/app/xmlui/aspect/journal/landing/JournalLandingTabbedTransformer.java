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
import org.xml.sax.SAXException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.Locale;
import org.datadryad.api.DryadJournal;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.content.Item;
import org.dspace.workflow.DryadWorkflowUtils;

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
    private DryadJournal dryadJournal;

    // container for data pertaining to entire div
    protected class DivData {
        public String n;
        public Message T_div_head;
        public String facetQueryField;
        public int maxResults;
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
        dryadJournal = new DryadJournal(this.context, this.journalName);
    }

    protected void addStatsTable(Body body) throws SAXException, WingException,
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
            Division counts = wrapper.addDivision(VALS);
            List countList = counts.addList(t.n, List.TYPE_SIMPLE, t.n);
            countList.setHead(t.valHead);
            LinkedHashMap<Item, String> results = dryadJournal.getRequestsPerJournal(
                divData.facetQueryField, t.dateFilter, divData.maxResults
            );
            if (results != null) {
                for (Item item : results.keySet()) {
                    Item dataPackage = DryadWorkflowUtils.getDataPackage(context, item);
                    itemsContainer.addReference(dataPackage);
                    countList.addItem().addContent(results.get(item));
                }
            }
        }
    }
}
