/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dspace.app.xmlui.aspect.journal.landing;

import java.io.IOException;
import java.sql.SQLException;
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
import org.dspace.content.Item;
import org.dspace.discovery.SearchServiceException;
import org.xml.sax.SAXException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import org.datadryad.api.DryadDataFile;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.content.DCValue;
import org.dspace.handle.HandleManager;
import org.dspace.statistics.Dataset;
import org.dspace.statistics.content.DatasetDSpaceObjectGenerator;
import org.dspace.statistics.content.StatisticsDataVisits;
import org.dspace.statistics.content.StatisticsListing;
import org.dspace.statistics.content.filter.StatisticsFilter;
import java.util.GregorianCalendar;
import java.util.Locale;
import org.datadryad.api.DryadDataPackage;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.I18nUtil;

/**
 *
 * @author Nathan Day
 */
public class JournalLandingTabbedTransformer extends AbstractDSpaceTransformer {
    
    private static final Logger log = Logger.getLogger(JournalLandingTabbedTransformer.class);
    private int currentMonth;
    private String currentMonthStr;
    private int currentYear;
    Locale defaultLocale = org.dspace.core.I18nUtil.getDefaultLocale();

    // query parameters
    protected int itemType;
    protected int itemCountMax = 10;
    protected boolean itemAndPackageCountedSeparately = false;
    protected int namelength = -1;
   
    // cocoon parameters
    protected String journalName;
        
    // performSearch() values
    protected ArrayList<DSpaceObject> references;
    protected ArrayList<String> values;
    private String query;
    private String sortOption;
    private String sortFieldOption;
    private TabData currentTabData;
    
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
    private String queryDateFilter;

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
            queryDateFilter = t.dateFilter;
            
            references = new ArrayList<DSpaceObject>();
            values = new ArrayList<String>();
            try {
                performSearch(null);
            } catch (SearchServiceException e) {
                log.error(e.getMessage(), e);
            }
            if (references.size() == 0) {
                list.addItem(EMPTY_VAL);
            } else {
                for (DSpaceObject ref : references)
                    refs.addReference(ref);
                for (String s : values)
                    list.addItem().addContent(s);
            }
        }
    }

    private void performSearch(DSpaceObject object) throws SearchServiceException, UIException {

        DatasetDSpaceObjectGenerator dsoAxis = new DatasetDSpaceObjectGenerator();
        dsoAxis.addDsoChild(itemType, itemCountMax, itemAndPackageCountedSeparately, namelength);
        StatisticsListing statListing = new StatisticsListing(new StatisticsDataVisits());
        statListing.addDatasetGenerator(dsoAxis);
        if (queryDateFilter != null) {
            StatisticsFilter dateFilter = new StatisticsFilter() {
                @Override
                public String toQuery() {
                    return queryDateFilter;
                }
            };
            statListing.addFilter(dateFilter);
        }

        //Render the list as a table
        Dataset dataset = null;
        try {
            dataset = statListing.getDataset(context);
        } catch (Exception ex) {
            log.error(ex);
            return;
        }
        if (dataset != null) {
            String[][] matrix = dataset.getMatrixFormatted();
            java.util.List<ResultItem> resultList = null;
            try {
                resultList = retrieveResultList(dataset, matrix[0]);
            } catch (Exception ex) {
                log.error(ex);
                return;
            }
            if (resultList != null) {
                log.debug("Handling " + resultList.size() + " objects for journal " + journalName);
                for (ResultItem result : resultList) {
                    references.add(result.item);
                    values.add(result.value);
                }
            } else {
                log.debug("Not handling a null result list for journal " + journalName + " (" + this.getClass().getName() + ")");
            }
        }
    }

    private class ResultItem {
        public Item item;
        public String value;
        public ResultItem(Item item, String value) {
            this.item = item;
            this.value = value;
        }
    }
    
    private java.util.List<ResultItem> retrieveResultList(Dataset dataset, String[] strings) throws SQLException {
        java.util.List<ResultItem> values = new ArrayList<ResultItem>();
        java.util.List<Map<String, String>> urls = dataset.getColLabelsAttrs();
        int j=0;
        for (Map<String, String> map : urls) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                if(values.size() == itemCountMax) return values;
                String url = entry.getValue();
                if (url == null || url.length() == 0 || url.lastIndexOf("/handle/") == -1) continue;

                log.debug("Using url: " + url);
                String suffix = url.substring(url.lastIndexOf("/handle/10255/"));
                suffix = suffix.replace("/handle/10255/","");
                if (suffix.indexOf("/") != -1) {
                    String trailer = suffix.substring(suffix.indexOf("/"));
                    suffix = suffix.replace(trailer, "");
                }
                suffix = "10255/" + suffix;

                log.debug("Using handle identifier to resolve object: " + suffix);
                DSpaceObject dso = HandleManager.resolveToObject(context, suffix);
                if (dso != null && dso instanceof Item) {
                    log.debug(("Retrieving results for Item with handle: " + ((Item)dso).getHandle()));
                    DCValue[] dcvs = ((Item) dso).getMetadata("dc", "type", null, Item.ANY);
                    for (DCValue dcv : dcvs) {
                        if (dcv.value.equals("Dataset")) {
                            log.debug(("Item with handle '" + ((Item)dso).getHandle()) + "' is a dataset.");
                            DryadDataFile file = null;
                            DryadDataPackage dataPackage = null;
                            try {
                                file = new DryadDataFile(((Item) dso));
                                dataPackage = file.getDataPackage(context);
                                if (dataPackage != null) {
                                    if (dataPackage.getPublicationName().equals(this.journalName)) {
                                        DCValue[] vals = ((Item)dso).getMetadata("dc", "title", null, Item.ANY);
                                        if(vals != null && 0 < vals.length) {
                                            log.debug(("Item with handle '" + ((Item) dso).getHandle())
                                                     + "' to be displayed for journal " + this.journalName);
                                            values.add(new ResultItem((Item) dso, strings[j]));
                                        }
                                        j++;
                                    }
                                } else {
                                    log.error("Data package for handle '" + suffix + "' is null");
                                }
                            } catch (Exception e) {
                                log.error(e.getMessage());
                            }
                        }
                    }
                }
            }
        }
        return values;
    }    
}
