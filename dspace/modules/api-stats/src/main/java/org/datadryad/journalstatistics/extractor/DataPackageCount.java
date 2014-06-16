/*
 */
package org.datadryad.journalstatistics.extractor;

import java.sql.SQLException;
import org.datadryad.api.DryadDataPackage;
import org.datadryad.api.DryadObject;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Counts Dryad Data Packages in the archive associated with a specified Journal
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class DataPackageCount extends DataItemCount  {

    public DataPackageCount(Context context) {
        super(context);
    }

    @Override
    protected Collection getCollection() throws SQLException {
        return DryadDataPackage.getCollection(getContext());
    }

    @Override
    protected DryadObject makeDryadObject(Item item) {
        return new DryadDataPackage(item);
    }

}
