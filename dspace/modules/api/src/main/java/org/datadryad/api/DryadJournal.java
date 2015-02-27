
package org.datadryad.api;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * Convenience class for querying for data related to a given journal
 * @author Nathan Day <nday@datadryad.org>
 */
public class DryadJournal {

    private static Logger log = Logger.getLogger(DryadJournal.class);

    private Context context = null;
    private String journalName = null;
    
    public DryadJournal(Context context, String journalName) throws IllegalArgumentException {
        if (context == null) {
            throw new IllegalArgumentException("Illegal null Context.");
        } else if (journalName == null || journalName.length() == 0) {
            throw new IllegalArgumentException("Illegal null or empty journal name.");
        }
        this.context = context;
        this.journalName = journalName;
    }

    // Params are collection id and publication name
    private final static String ARCHIVED_DATAFILE_QUERY =
    " -- item.item_id for data file                                                                                     " +
    " SELECT mdv_df.item_id                                                                                             " +
    "  FROM metadatavalue mdv_df                                                                                        " +
    "   JOIN metadatafieldregistry mdfr_df ON mdv_df.metadata_field_id=mdfr_df.metadata_field_id                        " +
    "   JOIN metadataschemaregistry mdsr_df ON mdsr_df.metadata_schema_id=mdfr_df.metadata_schema_id                    " +
    "  WHERE mdsr_df.short_id='dc'                                                                                      " +
    "    AND mdfr_df.element='relation'                                                                                 " +
    "    AND mdfr_df.qualifier='ispartof'                                                                               " +
    "    AND mdv_df.text_value IN                                                                                       " +
    "   -- doi for data packages for provided journal                                                                   " +
    "   (SELECT mdv_p_doi.text_value                                                                                    " +
    "      FROM  metadatavalue mdv_p_doi                                                                                " +
    "      JOIN  metadatafieldregistry mdfr_p_doi ON mdv_p_doi.metadata_field_id=mdfr_p_doi.metadata_field_id           " +
    "      JOIN  metadataschemaregistry mdsr_p_doi ON mdfr_p_doi.metadata_schema_id=mdsr_p_doi.metadata_schema_id       " +
    "     WHERE  mdsr_p_doi.short_id='dc'                                                                               " +
    "       AND  mdfr_p_doi.element='identifier'                                                                        " +
    "       AND  mdfr_p_doi.qualifier IS NULL                                                                           " +
    "       AND  mdv_p_doi.item_id IN                                                                                   " +
    "     -- item_id for data packages for provided journal                                                             " +
    "     (SELECT mdv_p_pub.item_id                                                                                     " +
    "          FROM  metadatavalue mdv_p_pub                                                                            " +
    "          JOIN  metadatafieldregistry mdfr_p_pub  ON mdv_p_pub.metadata_field_id=mdfr_p_pub.metadata_field_id      " +
    "          JOIN  metadataschemaregistry mdsr_p_pub ON mdfr_p_pub.metadata_schema_id=mdsr_p_pub.metadata_schema_id   " +
    "         WHERE  mdsr_p_pub.short_id='prism'                                                                        " +
    "           AND  mdfr_p_pub.element='publicationName'                                                               " +
    "           AND  mdv_p_pub.text_value='%'                                                                           " + // % : journal name
    "    ));                                                                                                            ";

    /**
     * Executes the SQL query to get archived data file item-ids for a given journal,
     * returning the item ids
     * @return a List of {@link Integer}
     * @throws SQLException
     */
    private List<Integer> getArchivedDataFiles() throws SQLException {
        TableRowIterator tri = DatabaseManager.query(this.context, ARCHIVED_DATAFILE_QUERY, this.journalName);
        List<Integer> dataFiles = new ArrayList<Integer>();
        while(tri.hasNext()) {
            TableRow row = tri.next();
            int itemId = row.getIntColumn("item_id");
            dataFiles.add(itemId);
        }
        return dataFiles;
    }
}
