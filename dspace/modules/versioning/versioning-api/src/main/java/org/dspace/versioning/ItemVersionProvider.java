package org.dspace.versioning;

import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Created by IntelliJ IDEA.
 * User: fabio.bolognesi
 * Date: Mar 31, 2011
 * Time: 1:47:07 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ItemVersionProvider {
    public Item createNewItemAndAddItInWorkspace(Context c, Item item);
//    public Item createNewItemAndAddItInWorkspace(Context c, Item itemToCopy, Item itemToWire);
    public void deleteVersionedItem(Context c, Version versionToDelete, VersionHistory history);
//    public Item createNewItemAndAddItInWorkspace(Context c, Item itemToCopy, Item itemToWire, Version version);
    public boolean isResponsible();

    Item updateItemState(Context c, Item itemNew, Item previousItem);
}
