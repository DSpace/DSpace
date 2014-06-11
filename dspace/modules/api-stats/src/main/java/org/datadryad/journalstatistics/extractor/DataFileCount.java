/*
 */
package org.datadryad.journalstatistics.extractor;

import java.io.IOException;
import java.sql.SQLException;
import org.apache.log4j.Logger;
import org.datadryad.api.DryadDataFile;
import org.datadryad.api.DryadDataPackage;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.Context;

/**
 * Counts Dryad Data Files in the archive associated with a specified Journal
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class DataFileCount extends DatabaseExtractor<Integer> {
    private static Logger log = Logger.getLogger(DataFileCount.class);

    private static final String JOURNAL_SCHEMA = "prism";
    private static final String JOURNAL_ELEMENT = "publicationName";
    private static final String JOURNAL_QUALIFIER = null;

    public DataFileCount(Context context) {
        super(context);
    }

    @Override
    public Integer extract(final String journalName) {
        Context context = this.getContext();
        Collection dataFiles;
        Integer count = 0;
        try {
            /*
             * Initial implementation is to fetch items by metadata field ID
             * then filter on collection. This may be too slow since it will
             * result in a lot of queries and could be done with a single join
             *
             * Note: findByMetadataField only returns items in archive
             */
            ItemIterator itemsByJournal = Item.findByMetadataField(
                    this.getContext(),
                    JOURNAL_SCHEMA,
                    JOURNAL_ELEMENT,
                    JOURNAL_QUALIFIER,
                    journalName);

            dataFiles = DryadDataFile.getCollection(context);

            while(itemsByJournal.hasNext()) {
                Item item = itemsByJournal.next();
                if(item.isOwningCollection(dataFiles)) {
                    count++;
                }
            }

        } catch (AuthorizeException ex) {
            log.error("AuthorizeException getting data file count per journal", ex);
        } catch (IOException ex) {
            log.error("IOException getting data file count per journal", ex);
        } catch (SQLException ex) {
            log.error("SQLException getting data file count per journal", ex);
        }

        return count;
    }

}
