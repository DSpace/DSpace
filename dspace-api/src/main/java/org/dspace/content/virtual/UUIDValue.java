package org.dspace.content.virtual;

import java.sql.SQLException;

import org.dspace.content.Item;
import org.dspace.core.Context;

public class UUIDValue implements VirtualBean {

    private boolean useForPlace;

    public void setUseForPlace(boolean useForPlace) {
        this.useForPlace = useForPlace;
    }

    public boolean getUseForPlace() {
        return useForPlace;
    }

    public String getValue(Context context, Item item) throws SQLException {
        return String.valueOf(item.getID());
    }
}
