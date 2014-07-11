/*
 */
package org.datadryad.journalstatistics.extractor;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.datadryad.api.DryadDataPackage;
import org.dspace.content.Collection;
import org.dspace.content.DCDate;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * Gets the data packages by journal that are unpublished (Still in workflow), returns
 * the number counted by month of submission. Extracts dates out of the provenance field
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class DataPackageUnpublishedCount extends DatabaseExtractor<Map<String, Integer>> {

    private static Logger log = Logger.getLogger(DataPackageUnpublishedCount.class);

    private static String PROVENANCE_DATE_START_TOKEN = ") on ";
    private static String PROVENANCE_DATE_END_TOKEN = " workflow start";

    // Params are collection id and publication name
    private final static String SQL_QUERY =
            "select  " +
            "  item.item_id as item_id, " +
            "  mdv_pubname.text_value as publication_name, " +
            "  mdv_provenance.text_value as provenance_submitted " +
            "from  " +
            "  item item " +
            "  join metadatavalue mdv_pubname on item.item_id = mdv_pubname.item_id " +
            "  join metadatafieldregistry mfr_pubname on mdv_pubname.metadata_field_id = mfr_pubname.metadata_field_id " +
            "  join metadatavalue mdv_provenance on item.item_id = mdv_provenance.item_id " +
            "  join metadatafieldregistry mfr_provenance on mdv_provenance.metadata_field_id = mfr_provenance.metadata_field_id " +
            "  join workflowitem wfi on item.item_id = wfi.item_id " +
            "  inner join ( " +
            "    select mdv_provenance_inner.metadata_field_id, mdv_provenance_inner.item_id, max(mdv_provenance_inner.place) as place " +
            "    from metadatavalue mdv_provenance_inner " +
            "      join metadatafieldregistry mfr_provenance_inner on mfr_provenance_inner.metadata_field_id = mdv_provenance_inner.metadata_field_id " +
            "    where  " +
            "      mdv_provenance_inner.text_value like 'Submitted by %' and " +
            "      mfr_provenance_inner.element = 'description' and  " +
            "      mfr_provenance_inner.qualifier = 'provenance'  " +
            "    group by  " +
            "      mdv_provenance_inner.metadata_field_id,  " +
            "      mdv_provenance_inner.item_id " +
            "    ) ss  " +
            "      on ss.metadata_field_id = mdv_provenance.metadata_field_id and  " +
            "         ss.place = mdv_provenance.place and  " +
            "         ss.item_id = mdv_provenance.item_id " +
            "where  " +
            "  wfi.collection_id = ? and " +
            "  mdv_pubname.text_value = ? and  " +
            "  mfr_pubname.element = 'publicationName' and " +
            "  mfr_provenance.element = 'description' and  " +
            "  mfr_provenance.qualifier = 'provenance' and  " +
            "  mdv_provenance.text_value like 'Submitted by %' " +
            "order by " +
            "  item.item_id; ";

    @Override
    public Map<String, Integer> extract(String journalName) {
        // Should return a map of Year/month to integers
        Map<String, Integer> results = new HashMap<String, Integer>();
        try {
            List<DateItem> unpublishedItems = getUnpublishedItems(journalName);
            for(DateItem dateItem : unpublishedItems) {
                if(passesDateFilter(dateItem.date)) {
                    String bucket = bucketForDate(dateItem.date);
                    if(!results.containsKey(bucket)) {
                        results.put(bucket, 0);
                    }
                    Integer count = results.get(bucket);
                    count++;
                    results.put(bucket, count);
                }
            }
        } catch (SQLException ex) {
            log.error("SQLException getting unpublished items size per journal", ex);
        }
        return results;
    }



    public static Integer getCountOrZero(final Map<String, Integer> map, final String key) {
        if(map != null && map.containsKey(key)) {
            return map.get(key);
        } else {
            return 0;
        }
    }

    public DataPackageUnpublishedCount(Context context) {
        super(context);
    }

    static DateFormat YEAR_MONTH = new SimpleDateFormat("yyyy-MM");
    static String bucketForDate(Date date) {
        return YEAR_MONTH.format(date);
    }

    class DateItem {
        Date date;
        int item_id;
        public DateItem(Date date, int item_id) {
            this.date = date;
            this.item_id = item_id;
        }
        public String bucketName() {
            return bucketForDate(date);
        }
    }

    public static String extractDateStringFromProvenance(String provenance) throws ParseException {
        // The provenance string is constructed by WorkflowManager
        // Must begin with "Submitted by" and have ") on " - "workflow start"

        int startTokenPosition = provenance.indexOf(PROVENANCE_DATE_START_TOKEN);
        if(startTokenPosition == -1) {
            throw new ParseException("Unable to find start token: '" +
                    PROVENANCE_DATE_START_TOKEN +
                    "' in the provenance string. Cannot extract date submitted", 0);
        }

        int endTokenPosition = provenance.indexOf(PROVENANCE_DATE_END_TOKEN);
        if(endTokenPosition == -1) {
            throw new ParseException("Unable to find end token:'" +
                    PROVENANCE_DATE_END_TOKEN +
                    "' in the provenance string. Cannot extract date submitted", 0);
        }

        int startDatePosition = startTokenPosition + PROVENANCE_DATE_START_TOKEN.length();
        int endDatePosition = endTokenPosition;
        String dateString = provenance.substring(startDatePosition, endDatePosition);
        return dateString;
    }

    /**
     * Executes the SQL query to get unpublished items for a given journal,
     * returning the item ids and submission dates
     * @param journalName
     * @return a List of {@link DateItem} objects
     * @throws SQLException
     */
    private List<DateItem> getUnpublishedItems(String journalName) throws SQLException {
        Collection c = DryadDataPackage.getCollection(this.getContext());
        TableRowIterator tri = DatabaseManager.query(this.getContext(), SQL_QUERY, c.getID(), journalName);
        List<DateItem> dateItems = new ArrayList<DateItem>();
        while(tri.hasNext()) {
            TableRow row = tri.next();
            int itemId = row.getIntColumn("item_id");
            String provenance = row.getStringColumn("provenance_submitted");
            String submittedDateString;
            try {
                submittedDateString = extractDateStringFromProvenance(provenance);
            } catch (ParseException ex) {
                log.error("Exception extracting date string from provenance, skipping", ex);
                continue;
            }
            DCDate submittedDate = new DCDate(submittedDateString);
            DateItem dateItem = new DateItem(submittedDate.toDate(), itemId);
            dateItems.add(dateItem);
        }
        return dateItems;
    }

}
