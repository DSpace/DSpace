package org.dspace.content.virtual;

import java.sql.SQLException;

import org.dspace.content.Item;
import org.dspace.core.Context;

public interface VirtualBean {

    String getValue(Context context, Item item) throws SQLException;
}
