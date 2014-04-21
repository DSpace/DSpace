package org.dspace.app.bulkdo;

import org.apache.commons.lang.ArrayUtils;
import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.core.Constants;

import java.sql.SQLException;
import java.util.*;

/**
 * Created by monikam on 4/11/14.
 */
public class ActionTarget {
    private DSpaceObject obj;
    private HashMap<String, Object> map;

    ActionTarget(DSpaceObject o) {
        assert (o != null);
        obj = o;
        map = null;
    }

    public DSpaceObject getObject() {
        return obj;
    }

    public static ActionTarget create(DSpaceObject obj) {
        assert (obj != null);
        switch (obj.getType()) {
            case Constants.BITSTREAM:
                return new BitstreamActionTarget(obj);
            case Constants.BUNDLE:
                return new BundleActionTarget(obj);
            case Constants.ITEM:
                return new ItemActionTarget(obj);
            case Constants.COLLECTION:
                return new CollectionActionTarget(obj);
            default:
                assert (false);
                throw new RuntimeException("should never try to create ActionTarget from " + obj.toString());
        }
    }

    static String theAvailableKeys[] = {"object", "id", "type", "handle", "exception"};

    public static String[] availableKeys(int type) {
        switch (type) {
            case Constants.BITSTREAM:
                return (String[]) ArrayUtils.addAll(theAvailableKeys, BitstreamActionTarget.theAvailableKeys);
            case Constants.BUNDLE:
                return (String[]) ArrayUtils.addAll(theAvailableKeys, BundleActionTarget.theAvailableKeys);
            case Constants.ITEM:
                return (String[]) ArrayUtils.addAll(theAvailableKeys, ItemActionTarget.theAvailableKeys);
            case Constants.COLLECTION:
                return (String[]) ArrayUtils.addAll(theAvailableKeys, CollectionActionTarget.theAvailableKeys);
            default:
                assert (false);
                return null;
        }
    }


    public static ActionTarget[] createArray(DSpaceObject[] objArr) {
        assert (objArr != null);
        ActionTarget[] arr = new ActionTarget[objArr.length];
        for (int i = 0; i < objArr.length; i++)
            arr[i] = create(objArr[i]);
        return arr;
    }

    public static ActionTarget[] createsArray(ArrayList<DSpaceObject> objArr) {
        assert (objArr != null);
        ActionTarget[] arr = new ActionTarget[objArr.size()];
        for (int i = 0; i < objArr.size(); i++)
            arr[i] = create(objArr.get(i));
        return arr;
    }

    public HashMap<String, Object> toHashMap() {
        if (map == null) {
            map = new HashMap<String, Object>();
            map.put("object", obj);
            if (obj != null) {
                try {
                    map.put("type", obj.getType());
                    map.put("id", obj.getID());
                    map.put("parent", obj.getParentObject());
                    map.put("handle", obj.getHandle());
                } catch (SQLException e) {
                    map.put("exception", e.getMessage());
                }
            }
        }
        return map;
    }
}

class CollectionActionTarget extends ActionTarget {
    Collection col;

    static String[] theAvailableKeys = {"name"};

    CollectionActionTarget(DSpaceObject o) {
        super(o);
        col = (Collection) o;
    }

    @Override
    public HashMap<String, Object> toHashMap() {
        HashMap<String, Object> map = super.toHashMap();
        map.put("name", col.getName());
        return map;
    }
}

class ItemActionTarget extends ActionTarget {
    Item itm;

    static String[] theAvailableKeys = {"isWithdrawn", "name"};

    ItemActionTarget(DSpaceObject o) {
        super(o);
        itm = (Item) o;
    }

    @Override
    public HashMap<String, Object> toHashMap() {
        HashMap<String, Object> map = super.toHashMap();
        map.put("isWithdrawn", itm.isWithdrawn());
        map.put("name", itm.getName());
        return map;
    }
}

class BundleActionTarget extends ActionTarget {
    Bundle bdl;

    static String[] theAvailableKeys = {"isWithdrawn", "isEmbargoed", "name"};

    BundleActionTarget(DSpaceObject o) {
        super(o);
        bdl = (Bundle) o;
    }

    @Override
    public HashMap<String, Object> toHashMap() {
        HashMap<String, Object> map = super.toHashMap();
        try {
            map.put("isEmbargoed", bdl.isEmbargoed());
        } catch (SQLException e) {
            map.put("isEmbargoed", e.getMessage());
        }
        map.put("name", bdl.getName());
        return map;
    }
}

class BitstreamActionTarget extends ActionTarget {
    Bitstream bit;

    static String[] theAvailableKeys = {"mimeType", "name", "size", "internalId", "checksum", "checksumAlgo"};

    BitstreamActionTarget(DSpaceObject o) {
        super(o);
        bit = (Bitstream) o;
    }

    @Override
    public HashMap<String, Object> toHashMap() {
        HashMap<String, Object> map = super.toHashMap();
        map.put("mimeType", bit.getFormat().getMIMEType());
        map.put("name", bit.getName());
        map.put("internalId", bit.getInternalId());
        map.put("size", bit.getSize());
        map.put("checksum", bit.getChecksum());
        map.put("checksumAlgo", bit.getChecksumAlgorithm());
        return map;
    }
}