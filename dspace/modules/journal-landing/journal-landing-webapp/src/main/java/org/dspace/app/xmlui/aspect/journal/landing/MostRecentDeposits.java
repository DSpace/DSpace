/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dspace.app.xmlui.aspect.journal.landing;

import java.io.IOException;
import java.io.Serializable;
import org.apache.log4j.Logger;

import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import static org.dspace.app.xmlui.aspect.journal.landing.Const.*;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import org.apache.avalon.framework.parameters.ParameterException;
import org.xml.sax.SAXException;
import org.datadryad.api.DryadJournal;
import org.dspace.app.xmlui.aspect.discovery.AbstractFiltersTransformer;
import org.dspace.app.xmlui.utils.UIException;
import static org.dspace.app.xmlui.wing.AbstractWingTransformer.message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.ReferenceSet;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.discovery.SearchServiceException;

/**
 *
 * @author Nathan Day
 */
public class MostRecentDeposits extends AbstractFiltersTransformer {

    private static final Logger log = Logger.getLogger(MostRecentDeposits.class);
    private final static SimpleDateFormat fmt = new SimpleDateFormat(fmtDateView);

    private static final Message T_mostRecent = message("xmlui.JournalLandingPage.MostRecentDeposits.panel_head");
    private static final Message T_date = message("xmlui.JournalLandingPage.MostRecentDeposits.date");

    private ArrayList<Item> references;
    private ArrayList<String> dates;
    private String journalName;

    @Override
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        try {
            this.journalName = parameters.getParameter(PARAM_JOURNAL_NAME);
        } catch (ParameterException ex) {
            log.error(ex);
        }
        if (journalName == null || journalName.length() == 0) return;

        Division mostRecent = body.addDivision(MOST_RECENT_DEPOSITS_DIV);
        mostRecent.setHead(T_mostRecent);

        Division items = mostRecent.addDivision(ITEMS);
        ReferenceSet refs = items.addReferenceSet(MOST_RECENT_DEPOSITS_REFS, "summaryList");

        Division count = mostRecent.addDivision(VALS);
        List vals = count.addList("date-count", List.TYPE_SIMPLE, "date-count");
        vals.setHead(T_date);

        references = new ArrayList<Item>();
        dates = new ArrayList<String>();
        try {
            performSearch(null);
        } catch (SearchServiceException e) {
            log.error(e.getMessage(), e);
        }
        if (references.size() > 0) {
            for (DSpaceObject ref : references)
                refs.addReference(ref);
            for (String s : dates)
                vals.addItem(s);
        }
        references = null;
        dates = null;
    }

    /**
     * Search for recently accessioned data packages
     * @param object: unused
     * @throws org.dspace.discovery.SearchServiceException
     * @throws org.dspace.app.xmlui.utils.UIException
     */
    @Override
    public void performSearch(DSpaceObject object) throws SearchServiceException, UIException {
        DryadJournal dryadJournal = new DryadJournal(this.context, this.journalName);
        java.util.List<Item> packages = null;
        try {
            packages = dryadJournal.getArchivedPackagesSortedRecent(displayCount);
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
                    references.add(item);
                    dates.add(dateStr);                
                }
            }
        }
    }
    @Override
    public String getView() {
        return "site";
    }
    @Override
    public Serializable getKey() {
        // do not allow this to be cached
        return null;
    }
}
