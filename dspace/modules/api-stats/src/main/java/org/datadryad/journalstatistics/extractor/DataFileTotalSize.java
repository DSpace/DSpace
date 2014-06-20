/*
 */
package org.datadryad.journalstatistics.extractor;

import java.io.IOException;
import java.sql.SQLException;
import org.apache.log4j.Logger;
import org.datadryad.api.DryadDataFile;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.Context;

/**
 * Counts the total size (in bytes) of Dryad Data Files in the archive
 * associated with a specified Journal
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class DataFileTotalSize  extends DatabaseExtractor<Long> {
    private static Logger log = Logger.getLogger(DataItemCount.class);
    public DataFileTotalSize(Context context) {
        super(context);
    }

    @Override
    public Long extract(final String journalName) {
        Context context = this.getContext();
        Collection collection;
        Long totalSize = 0L;
        try {
            ItemIterator itemsByJournal = Item.findByMetadataField(this.getContext(), JOURNAL_SCHEMA, JOURNAL_ELEMENT, JOURNAL_QUALIFIER, journalName);
            collection = DryadDataFile.getCollection(context);
            while (itemsByJournal.hasNext()) {
                Item item = itemsByJournal.next();
                if(item.getOwningCollection().equals(collection)) {
                    DryadDataFile dataFile = new DryadDataFile(item);
                    totalSize += dataFile.getTotalStorageSize();
                }
            }
        } catch (AuthorizeException ex) {
            log.error("AuthorizeException getting total size per journal", ex);
        } catch (IOException ex) {
            log.error("IOException getting total size per journal", ex);
        } catch (SQLException ex) {
            log.error("SQLException getting total size per journal", ex);
        }
        return totalSize;
    }

}
