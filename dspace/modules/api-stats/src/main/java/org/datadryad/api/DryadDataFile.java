/*
 */
package org.datadryad.api;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class DryadDataFile extends DryadObject {
    private static final String FILES_COLLECTION_HANDLE_KEY = "stats.datafiles.coll";

    // This is configured in dspace.cfg but replicated here.
    private static final String EMBARGO_TYPE_SCHEMA = "dc";
    private static final String EMBARGO_TYPE_ELEMENT = "type";
    private static final String EMBARGO_TYPE_QUALIFIER = "embargo";

    private static final String EMBARGO_DATE_SCHEMA = "dc";
    private static final String EMBARGO_DATE_ELEMENT = "date";
    private static final String EMBARGO_DATE_QUALIFIER = "embargoedUntil";

    private static final DateFormat EMBARGO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private DryadDataPackage dataPackage;
    private static Logger log = Logger.getLogger(DryadDataFile.class);

    public DryadDataFile(Item item) {
        super(item);
    }

    public static Collection getCollection(Context context) throws SQLException {
        String handle = ConfigurationManager.getProperty(FILES_COLLECTION_HANDLE_KEY);
        return DryadObject.collectionFromHandle(context, handle);
    }

    public static DryadDataFile create(Context context) throws SQLException {
        Collection collection = DryadDataFile.getCollection(context);
        DryadDataFile dataFile = null;
        try {
            WorkspaceItem wsi = WorkspaceItem.create(context, collection, true);
            Item item = wsi.getItem();
            dataFile = new DryadDataFile(item);
            dataFile.addToCollectionAndArchive(collection);
            wsi.deleteWrapper();
        } catch (AuthorizeException ex) {
            log.error("Authorize exception creating a Data File", ex);
        } catch (IOException ex) {
            log.error("IO exception creating a Data File", ex);
        }
        return dataFile;
    }

    public DryadDataPackage getDataPackage() {
        if(dataPackage == null) {
            // Find the data package for this file
            throw new RuntimeException("Not yet implemented");
        }
        return dataPackage;
    }

    static Date parseDate(String dateString) throws ParseException {
        return EMBARGO_DATE_FORMAT.parse(dateString);
    }

    static String formatDate(Date date) {
        return EMBARGO_DATE_FORMAT.format(date);
    }
    
    static Date[] extractDatesFromMetadata(DCValue[] values) {
        Date dates[] = new Date[values.length];
        for(int i=0;i<values.length;i++) {
            try {
                dates[i] = parseDate(values[i].value);
            } catch (ParseException ex) {
                log.error("Exception parsing date from '" + values[i].value + "'", ex);
            }
        }
        return dates;
    }

    static Date getEarliestDate(Date[] dates) {
        if(dates.length > 0) {
            Arrays.sort(dates);
            return dates[0];
        } else {
            return null;
        }
    }

    static Date getEarliestDate(DCValue values[]) {
        Date[] embargoDates = extractDatesFromMetadata(values);
        return getEarliestDate(embargoDates);
    }

    public boolean isEmbargoed() {
        boolean isEmbargoed = false;
        DCValue[] embargoLiftDateMetadata = getItem().getMetadata(EMBARGO_DATE_SCHEMA, EMBARGO_DATE_ELEMENT, EMBARGO_DATE_QUALIFIER, Item.ANY);
        if(embargoLiftDateMetadata.length > 0) {
            // has a lift date, compare to today
            Date today = new Date();
            Date embargoLiftDate = getEarliestDate(embargoLiftDateMetadata);
            // Embargoed if there is a lift date and it is in the future
            if(embargoLiftDate != null && (embargoLiftDate.compareTo(today) > 0)) {
                isEmbargoed = true;
            }
        }
        return isEmbargoed;
    }

    public void clearEmbargo() throws SQLException {
        getItem().clearMetadata(EMBARGO_TYPE_SCHEMA, EMBARGO_TYPE_ELEMENT, EMBARGO_TYPE_QUALIFIER, Item.ANY);
        getItem().clearMetadata(EMBARGO_DATE_SCHEMA, EMBARGO_DATE_ELEMENT, EMBARGO_DATE_QUALIFIER, Item.ANY);
        try {
            getItem().update();
        } catch (AuthorizeException ex) {
            log.error("Authorize exception clearing embargo", ex);
        }
    }

    public void setEmbargo(String embargoType, Date liftDate) throws SQLException {
        if(!DryadEmbargoTypes.validate(embargoType)) {
            throw new IllegalArgumentException("EmbargoType '"
                    + embargoType + "' is not valid");
        }
        if(liftDate == null) {
            throw new IllegalArgumentException("Unable to set embargo date with null liftDate");
        }

        else {
            try {
                clearEmbargo();
                String liftDateString = formatDate(liftDate);
                getItem().addMetadata(EMBARGO_TYPE_SCHEMA, EMBARGO_TYPE_ELEMENT, EMBARGO_TYPE_QUALIFIER, null, embargoType);
                getItem().addMetadata(EMBARGO_DATE_SCHEMA, EMBARGO_DATE_ELEMENT, EMBARGO_DATE_QUALIFIER, null, liftDateString);
                getItem().update();
            } catch (AuthorizeException ex) {
                log.error("Authorize exception setting embargo", ex);
            }
        }
    }
}
