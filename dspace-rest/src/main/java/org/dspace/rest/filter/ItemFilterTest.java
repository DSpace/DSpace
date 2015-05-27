package org.dspace.rest.filter;

import org.dspace.content.Item;
import org.dspace.core.Context;

public interface ItemFilterTest {
    public String getName();
    public String getTitle();
    public String getDescription();
    public boolean testItem(Context context, Item i);
}
