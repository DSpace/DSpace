package org.dspace.app.bulkdo;

import org.apache.commons.lang.ArrayUtils;
import org.dspace.content.*;
import org.dspace.core.Constants;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by monikam on 4/11/14.
 */
public class ActionTarget  {

    private DSpaceObject obj;
    private HashMap<String, Object> map;
    private ActionTarget up;

    ActionTarget(ActionTarget container, DSpaceObject o) {
        assert (o != null);
        obj = o;
        up = container;
        map = null;
    }

    public DSpaceObject getObject() {
        return obj;
    }

    public static ActionTarget create(ActionTarget container, DSpaceObject obj) {
        assert (obj != null);
        switch (obj.getType()) {
            case Constants.BITSTREAM:
                return new BitstreamActionTarget(container, obj);
            case Constants.BUNDLE:
                return new BundleActionTarget(container, obj);
            case Constants.ITEM:
                return new ItemActionTarget(container, obj);
            case Constants.COLLECTION:
                return new CollectionActionTarget(container, obj);
            case Constants.COMMUNITY:
                return new ActionTarget(container, obj);
            default:
                assert (false);
                throw new RuntimeException("should never try to create ActionTarget from " + obj.toString());
        }
    }

    public static ActionTarget createUpFor(DSpaceObject obj) {
        assert (obj != null);
        try {
            DSpaceObject up = null;
            switch (obj.getType()) {
                case Constants.BITSTREAM:
                    up = ((Bitstream) obj).getBundles()[0];
                    break;
                case Constants.BUNDLE:
                    up = ((Bundle) obj).getItems()[0];
                    break;
                case Constants.ITEM:
                case Constants.COLLECTION:
                    up = obj.getParentObject();
                    break;
                case Constants.COMMUNITY:
                    up =null;
                    break;
                default:
                    throw new RuntimeException("should never try to create ActionTarget from " + obj.toString());
            }
            return create(null, up);
        } catch (SQLException e) {
            throw new RuntimeException("should never happen???:  " + e.getMessage());
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


    public static ArrayList<ActionTarget> createArray(ActionTarget up, DSpaceObject[] objArr) {
        assert (objArr != null);
        ArrayList<ActionTarget> arr = new ArrayList<ActionTarget>(objArr.length);
        for (int i = 0; i < objArr.length; i++)
            arr.add(create(up, objArr[i]));
        return arr;
    }

    public static ArrayList<ActionTarget> createsArray(ActionTarget up, ArrayList<DSpaceObject> objArr) {
        assert (objArr != null);
        ArrayList<ActionTarget> arr = new ArrayList<ActionTarget>(objArr.size());
        for (int i = 0; i < objArr.size(); i++)
            arr.add(create(up, objArr.get(i)));
        return arr;
    }

    public Object get(String key) {
        HashMap<String, Object> map = toHashMap();
        Object o = map.get(key);
        if (o == null) {
            int i = key.indexOf('.');
            if (i != -1) {
                String type = key.substring(0, i);
                int tId = Constants.getTypeID(type);
                if (tId != -1 && Arguments.typeIncludes(tId, getObject().getType())) {
                    key = key.substring(i+1, key.length());
                    ActionTarget look = this;
                    while  (look != null && look.getObject().getType() !=  tId) {
                        if (look.up == null && Arguments.typeIncludes(tId, look.getObject().getType())) {
                            look.up = ActionTarget.createUpFor(look.getObject());
                        }
                        look = look.up;
                    }
                    if (look != null) {
                        return look.toHashMap().get(key);
                    }
                }
            }
        }
        return o;
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
                    map.put("up", up);
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

    static String[] theAvailableKeys = {"name", "template"};

    CollectionActionTarget(ActionTarget up, DSpaceObject o) {
        super(up, o);
        col = (Collection) o;
    }

    @Override
    public HashMap<String, Object> toHashMap() {
        HashMap<String, Object> map = super.toHashMap();
        map.put("name", col.getName());
        try {
        map.put("template", col.getTemplateItem());
        } catch (SQLException e) {
            map.put("template", e.getMessage());
        }
        return map;
    }
}

class ItemActionTarget extends ActionTarget {
    Item itm;

    static String[] theAvailableKeys = {"isWithdrawn", "name"};

    ItemActionTarget(ActionTarget up, DSpaceObject o) {
        super(up, o);
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

    BundleActionTarget(ActionTarget up, DSpaceObject o) {
        super(up, o);
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

    BitstreamActionTarget(ActionTarget up, DSpaceObject o) {
        super(up, o);
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