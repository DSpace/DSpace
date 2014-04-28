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
public class ActionTarget {

    private DSpaceObject obj;
    private HashMap<String, Object> map;
    private ActionTarget up;

    ActionTarget(ActionTarget container, DSpaceObject o) {
        System.out.println("Create ActionTarget " + o);
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
                    up = null;
                    break;
                default:
                    throw new RuntimeException("should never try to create ActionTarget from " + obj.toString());
            }
            return create(null, up);
        } catch (SQLException e) {
            throw new RuntimeException("should never happen???:  " + e.getMessage());
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
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

    /**
     *
     *
     * @param key
     * @return
     */

    private Object getFromUp(String key) {
        int i = key.indexOf('.');
        if (i != -1) {

            String type = key.substring(0, i);
            int tId = Constants.getTypeID(type);
            if (tId != -1 && Arguments.typeIncludes(tId, getObject().getType())) {
                key = key.substring(i + 1, key.length());
                ActionTarget look = this;
                while (look != null && look.getObject().getType() != tId) {
                    if (look.up == null && Arguments.typeIncludes(tId, look.getObject().getType())) {
                        look.up = ActionTarget.createUpFor(look.getObject());
                    }
                    look = look.up;
                }
                if (look != null) {
                    return look.get(key);
                }
            }
        }
        return null;
    }

    private Object getMetadateValue(String field) {
        if (obj.getType() == Constants.ITEM) {
            DCValue[] values =  ((Item) obj).getMetadata(field);
            if (values.length == 0) {
                return null;
            }
            if (values.length ==1 ) {
                return values[0].value;
            }
            String vals[] = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                vals[i] = values[i].value;
            }
            return vals;
        }
        return null;
    }

    public Object get(String key) {
        toHashMap();
        Object o = map.get(key);
        if (o == null) {
            o = getFromUp(key);
        }
        if (o == null) {
            o = getMetadateValue(key);
        }
        return o;
    }

    public void put(String key, Object obj) {
        toHashMap();
        map.put(key, obj);
    }

    protected boolean toHashMap() {
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
            return true;
        }
        return false;
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
    protected boolean toHashMap() {
        boolean create = super.toHashMap();
        if (create) {
            put("name", col.getName());
            try {
                put("template", col.getTemplateItem());
            } catch (SQLException e) {
                put("template", e.getMessage());
            }
        }
        return create;
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
    protected boolean toHashMap() {
        boolean create = super.toHashMap();
        if (create) {
            put("isWithdrawn", itm.isWithdrawn());
            put("name", itm.getName());
        }
        return create;
    }
}

class BundleActionTarget extends ActionTarget {
    Bundle bdl;

    static String[] theAvailableKeys = {"isWithdrawn", "isEmbargoed", "name"};

    BundleActionTarget(ActionTarget up, DSpaceObject o) {
        super(up, o);
        bdl = (Bundle) o;
    }

    protected boolean toHashMap() {
        boolean create = super.toHashMap();
        if (create) {
            try {
                put("isEmbargoed", bdl.isEmbargoed());
            } catch (SQLException e) {
                put("isEmbargoed", e.getMessage());
            }
            put("name", bdl.getName());
        }
        return create;
    }
}

class BitstreamActionTarget extends ActionTarget {
    Bitstream bit;

    static String[] theAvailableKeys = {"mimeType", "name", "size", "internalId", "checksum", "checksumAlgo"};

    BitstreamActionTarget(ActionTarget up, DSpaceObject o) {
        super(up, o);
        bit = (Bitstream) o;
    }

    protected boolean toHashMap() {
        boolean create = super.toHashMap();
        if (create) {
            put("mimeType", bit.getFormat().getMIMEType());
            put("name", bit.getName());
            put("internalId", bit.getInternalId());
            put("size", bit.getSize());
            put("checksum", bit.getChecksum());
            put("checksumAlgo", bit.getChecksumAlgorithm());
        }
        return create;
    }
}