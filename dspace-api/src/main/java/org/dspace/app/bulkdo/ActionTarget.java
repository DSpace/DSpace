/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkdo;

import org.apache.commons.lang.ArrayUtils;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.*;
import org.dspace.core.Constants;
import org.dspace.core.Context;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;


class ActionTarget {

    private Context context;
    private DSpaceObject obj;
    private HashMap<String, Object> map;
    private ActionTarget up;

    public static final String POLICY = "POLICY";

    ActionTarget(Context contxt, ActionTarget container, DSpaceObject o) {
        assert (o != null);
        context = contxt;
        obj = o;
        up = container;
        map = null;
    }

    public DSpaceObject getObject() {
        return obj;
    }

    public static ActionTarget create(Context context, ActionTarget container, DSpaceObject obj) {
        assert (obj != null);
        switch (obj.getType()) {
            case Constants.BITSTREAM:
                return new BitstreamActionTarget(context, container, obj);
            case Constants.BUNDLE:
                return new BundleActionTarget(context, container, obj);
            case Constants.ITEM:
                return new ItemActionTarget(context, container, obj);
            case Constants.COLLECTION:
                return new CollectionActionTarget(context, container, obj);
            case Constants.COMMUNITY:
                return new ActionTarget(context, container, obj);
            default:
                assert (false);
                throw new RuntimeException("should never try to create ActionTarget from " + obj);
        }
    }

    public static ActionTarget createUpFor(Context context, DSpaceObject obj) {
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
            if (up == null)
                return null;
            return create(context, null, up);
        } catch (SQLException e) {
            throw new RuntimeException("should never happen???:  " + e.getMessage());
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }


    static String theAvailableKeys[] = {"object", "id", "type", "exception"};

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


    public static ArrayList<ActionTarget> createArray(Context context, ActionTarget up, DSpaceObject[] objArr) {
        assert (objArr != null);
        ArrayList<ActionTarget> arr = new ArrayList<ActionTarget>(objArr.length);
        for (int i = 0; i < objArr.length; i++)
            arr.add(create(context, up, objArr[i]));
        return arr;
    }

    private static ArrayList<ActionTarget> createsArray(Context context, ActionTarget up, ArrayList<DSpaceObject> objArr) {
        assert (objArr != null);
        ArrayList<ActionTarget> arr = new ArrayList<ActionTarget>(objArr.size());
        for (int i = 0; i < objArr.size(); i++)
            arr.add(create(context, up, objArr.get(i)));
        return arr;
    }

    private Object getFromUp(int tId, String key) {
        if (Arguments.typeIncludes(tId, getObject().getType())) {
            ActionTarget look = this;
            while (look != null && look.getObject().getType() != tId) {
                if (look.up == null && Arguments.typeIncludes(tId, look.getObject().getType())) {
                    look.up = ActionTarget.createUpFor(context, look.getObject());
                }
                look = look.up;
            }
            if (look != null) {
                return look.get(key);
            }
        }
        return null;
    }

    private String[] getMetadateValue(String field) {
        if (obj.getType() == Constants.ITEM) {
            Metadatum[] values =  ((Item) obj).getMetadataByMetadataString(field);
            return Metadatum.valuesFor(values);
        }
        return null;
    }

    private Object getPolicy(String whatPol) throws SQLException {
        int pol = Constants.getActionID(whatPol);
        if (pol != -1) {
            return AuthorizeManager.getPoliciesActionFilter(context, obj, pol);
        }
        return null;
    }

    public Object get(String key) {
        toHashMap();
        Object o = map.get(key);
        if (o != null)
            return o;

        int i = key.indexOf('.');
        if (i != -1) {
            String kind = key.substring(0, i);
            String keyForKind = key.substring(i + 1, key.length());
            int tId = Constants.getTypeID(kind);
            if (tId != -1) {
                // its a DSpaceObject type - look up
                o = getFromUp(tId, keyForKind);
            } else {
                try {
                    if (kind.equals(POLICY)) {
                        o = getPolicy(keyForKind);
                    }
                } catch (SQLException e) {
                    return "ERROR:" + e.getMessage();
                }
            }
        }
        if (o == null) {
            o = getMetadateValue(key);
        }
        return o;
    }

    public void put(String key, Object put) {
        toHashMap();
        map.put(key, put);
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

    static String[] theAvailableKeys = {"name", "handle", "template"};

    CollectionActionTarget(Context context, ActionTarget up, DSpaceObject o) {
        super(context, up, o);
        col = (Collection) o;
    }

    @Override
    protected boolean toHashMap() {
        boolean create = super.toHashMap();
        if (create) {
            put("name", col.getName());
            put("handle", getObject().getHandle());
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

    static String[] theAvailableKeys = {"isWithdrawn", "handle", "name"};

    ItemActionTarget(Context context, ActionTarget up, DSpaceObject o) {
        super(context, up, o);
        itm = (Item) o;
    }

    @Override
    protected boolean toHashMap() {
        boolean create = super.toHashMap();
        if (create) {
            put("isWithdrawn", itm.isWithdrawn());
            put("name", itm.getName());
            put("handle", getObject().getHandle());
        }
        return create;
    }
}

class BundleActionTarget extends ActionTarget {
    Bundle bdl;

    static String[] theAvailableKeys = {"isEmbargoed", "name"};

    BundleActionTarget(Context context, ActionTarget up, DSpaceObject o) {
        super(context, up, o);
        bdl = (Bundle) o;
    }

    // TODO - figure out how to determine embargo state
    protected boolean toHashMap() {
        boolean create = super.toHashMap();
        if (create) {
            put("isEmbargoed", "we have no idea");
            put("name", bdl.getName());
        }
        return create;
    }
}

