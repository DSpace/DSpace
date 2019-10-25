package org.ssu.entity;

import org.dspace.app.util.GoogleMetadata;
import org.dspace.content.Item;
import org.dspace.core.Context;

import java.sql.SQLException;
import java.util.List;

public class GoogleMetadataTagGenerator extends GoogleMetadata {
    /**
     * Wrap the item, parse all configured fields and generate metadata field
     * values.
     *
     * @param context context
     * @param item    The item being viewed to extract metadata from
     * @throws SQLException if database error
     */
    public GoogleMetadataTagGenerator(Context context, Item item) throws SQLException {
        super(context, item);
    }

    public void setAuthors(List<String> authors) {
        metadataMappings.removeAll(AUTHORS);
        metadataMappings.putAll(AUTHORS, authors);
    }
}
