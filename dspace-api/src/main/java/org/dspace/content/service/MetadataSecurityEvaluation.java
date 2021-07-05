package org.dspace.content.service;

import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.core.Context;

import java.sql.SQLException;

public interface MetadataSecurityEvaluation {
    public boolean allowMetadataFieldReturn(Context context, Item item, MetadataField metadataField) throws SQLException;
}
