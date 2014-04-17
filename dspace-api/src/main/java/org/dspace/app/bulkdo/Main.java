package org.dspace.app.bulkdo;

import org.apache.commons.cli.*;
import org.apache.commons.lang.StringUtils;
import org.dspace.content.*;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.handle.HandleManager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by monikam on 4/2/14.
 */
public class Main {

    private static String VERSION = "asd";

    private static final String allToDos = "lAD";
    // l: list    selected DSPaceObjects, aka the ones contained in given handle of given type
    // A: add     authorization policy defined by access and group parameters to selected DSPaceObjects
    // D: delete  authorization policy defined by access and group parameters from selected DSPaceObjects

    public static Options createOptions() {
        Options options = new Options();

        options.addOption("o", "object", true, "handle");
        options.addOption("t", "type", true, "type: collection, item, bundle, or bitstream ");
        options.addOption("a", "action", false, "one of " + allToDos + "  (default: v)");
        options.addOption("A", "acccess", false, "READ, DEFAULT_ITEM_READ, ... (need for action A nd D)");
        options.addOption("g", "group", false, "authorization group (need for action A nd D)");
        options.addOption("h", "help", false, "help");
        return options;
    }

    public static void usage(Options options) {
        HelpFormatter myhelp = new HelpFormatter();
        myhelp.printHelp("Bulk Apply Action\n", options);
        System.out.println("\n");

        System.out.println("Available actions: " +   StringUtils.join(Constants.actionText, ","));
        //TODO explain actions
        System.exit(0);
    }


    Context c = null;
    DSpaceObject dobj = null;
    int myType = -1;
    List<Collection> collections = null;
    List<Item> items = null;
    List<Bundle> bundles = null;
    List<Bitstream> bitstreams = null;


    Group group = null;
    int dspaceAccessAction = -1;
    char toDo = 'v';


    Main(CommandLine line) throws ParseException, SQLException {
        try {
            c = new Context();
        } catch (SQLException e) {
            throw new ParseException("Could not access database");
        }

        toDo = 'l';
        if (line.hasOption('a')) {
            String modeValue = line.getOptionValue('a');
            if (modeValue != null && !modeValue.isEmpty()) {
                toDo = modeValue.charAt(0);
            }
            if (allToDos.indexOf(toDo) == -1) {
                System.out.println("'" + modeValue + "'");
                System.out.println(toDo);
                throw new ParseException("" + toDo + " not one of " + allToDos);
            }
        }

        String handle = line.getOptionValue('o');
        if (handle == null || handle.isEmpty())
            throw new ParseException("Missing object argument");

        String typeString = line.getOptionValue('t');
        if (typeString == null || typeString.isEmpty())
            throw new ParseException("Missing type argument");
        myType = Constants.getTypeID(typeString.toUpperCase());
        if (myType != Constants.COLLECTION &&
                myType != Constants.ITEM &&
                myType != Constants.BUNDLE &&
                myType != Constants.BITSTREAM) {
            throw new ParseException("type must be collection, item, bundle, or bitstream");
        }

        String dspaceAccessActionString = line.getOptionValue('a');
        String groupString = line.getOptionValue('g');
        if (toDo == 'A' || toDo == 'D') {
            if (dspaceAccessActionString == null || dspaceAccessActionString.isEmpty())
                throw new ParseException("Missing dspaceAcessAction argument");
            if (groupString == null || groupString.isEmpty())
                throw new ParseException("Missing group argument");
            dspaceAccessAction = Constants.getActionID(dspaceAccessActionString);
            if (dspaceAccessAction < 0) {
                throw new ParseException(dspaceAccessActionString + " no a valid dspaceAccessAction");
            }
            group = Group.findByName(c, groupString);
            if (group == null) {
                throw new ParseException(groupString + " is not a valid Group");
            }
        }

        dobj = HandleManager.resolveToObject(c, handle);
        if (dobj == null) {
            String[] splits = StringUtils.split(handle, '.');
            try {
                String type = splits[0];
                int id = Integer.parseInt(splits[1]);
                dobj = DSpaceObject.find(c, Constants.getTypeID(type), id);
            } catch (Exception e) {
                dobj = null;
            }
        }
        if (dobj == null) {
            throw new ParseException(handle + " is not a valid object descriptor");
        }
        if (dobj.getType() != Constants.COMMUNITY &&
                dobj.getType() != Constants.COLLECTION &&
                dobj.getType() != Constants.ITEM &&
                dobj.getType() != Constants.BUNDLE &&
                dobj.getType() != Constants.BITSTREAM) {
            throw new ParseException(dobj + " is not a community, collection, item, bundle or bitstream");
        }
        // HACK: relying on const values
        if (myType > dobj.getType()) {
            throw new ParseException(Constants.typeText[myType] + "s are not nested inside " +
                    Constants.typeText[dobj.getType()]);
        }


    }

    private void listCollections() throws SQLException {
        if (dobj.getType() == Constants.COMMUNITY) {
            Community comm = (Community) dobj;
            collections = new ArrayList<Collection>(Arrays.asList(comm.getCollections()));
        } else if (dobj.getType() == Constants.COLLECTION) {
            collections = new ArrayList<Collection>(1);
            collections.add((Collection) dobj);
        }
        if (collections != null)
            System.out.println("Found " + collections.size() + " collections");
    }

    private void listItems() throws SQLException {
        if (dobj.getType() == Constants.ITEM) {
            items = new ArrayList<Item>(1);
            items.add((Item) dobj);
        } else {
            if (collections != null) {
                items = new ArrayList<Item>();
                // collect ITEMS from collections  (only the true members / in_archive=1 )
                for (int i = 0; i < collections.size(); i++) {
                    ItemIterator iter = collections.get(i).getItems();
                    while (iter.hasNext()) {
                        items.add(iter.next());
                    }
                }
            }
        }
        if (items != null)
            System.out.println("Found " + items.size() + " items");
    }

    private void listBundles() throws SQLException {
        if (dobj.getType() == Constants.BUNDLE) {
            bundles = new ArrayList<Bundle>(1);
            bundles.add((Bundle) dobj);
        } else {
            if (items != null) {
                // collect BUNDLES from items
                bundles = new ArrayList<Bundle>(items.size());
                for (int i = 0; i < items.size(); i++) {
                    Bundle[] bs = items.get(i).getBundles("ORIGINAL");
                    Collections.addAll(bundles, bs);
                }
            }
        }
        if (bundles != null)
            System.out.println("Found " + bundles.size() + " bundles");
    }

    private void listBitstreams() throws SQLException {
        if (dobj.getType() == Constants.BITSTREAM) {
            bitstreams = new ArrayList<Bitstream>(1);
            bitstreams.add((Bitstream) dobj);
        } else {
            if (bundles != null) {
                bitstreams = new ArrayList<Bitstream>(bundles.size());
               // collect BISTTREAMS from bundles
                for (int i = 0; i < bundles.size(); i++) {
                    Bitstream[] bits = bundles.get(i).getBitstreams();
                    Collections.addAll(bitstreams, bits);
                }
            }
        }
        if (bitstreams != null)
            System.out.println("Found " + bitstreams.size() + " bitstreams");
    }

    void listObjects() throws SQLException {
        assert(dobj != null);
        assert(dobj.getType() >= myType);

        listCollections();
        if (myType == Constants.COLLECTION) {
            assert (collections != null);
            return;
        }

        listItems();
        if (myType == Constants.ITEM) {
            assert (items != null);
            return;
        }

        listBundles();
        if (myType == Constants.BUNDLE) {
            assert (bundles != null);
            return;
        }

        listBitstreams();
        if (myType == Constants.BITSTREAM) {
            assert (bitstreams != null);
            return;
        }
    }

    void applyAction() {
        switch (myType) {
            case Constants.COLLECTION:
                assert (collections != null);
                applyActionToList(collections);
                break;
            case Constants.ITEM:
                assert (items != null);
                applyActionToList(items);
                break;
            case Constants.BUNDLE:
                assert (bundles != null);
                applyActionToList(bundles);
                break;
            case Constants.BITSTREAM:
                assert (bitstreams != null);
                applyActionToList(bitstreams);
                break;
        }
    }

    private void applyActionToList(List objs) {
        System.out.println("Apply to " + objs.size() + " " + Constants.typeText[myType]);
        for (int i = 0; i < objs.size(); i++) {
            Action a = Action.create((DSpaceObject) objs.get(i));
            switch (toDo) {
                case 'l': System.out.println(a.describe());
                          break;
                case 'A':
                case 'D':
                    System.out.println(a.describe() + " " +
                            dspaceAccessAction + " " + Constants.actionText[dspaceAccessAction] + " "  +
                            group.getName());
                    break;
                default:
                    assert(false);
            }
        }
    }


    public static void main(String argv[]) {
        Options options = createOptions();
        try {
            CommandLineParser parser = new PosixParser();
            CommandLine line = parser.parse(options, argv);
            if (line.hasOption("h")) {
                Main.usage(options);
                return;
            }
            Main me = new Main(line);

            System.out.println("Work on all " + Constants.typeText[me.myType] + "s in " +
                    me.dobj + " " + Action.getString(me.dobj.getHandle(), ""));
            System.out.println("Edit-Mode: " + me.toDo);
            System.out.println("\n");

            me.listObjects();
            System.out.println("\n");

            me.applyAction();


        } catch (SQLException e) {
            System.err.println("WTF - is the database corrupted ???");
            System.exit(1);
        } catch (ParseException e) {
            System.err.println("ERROR: " + e.getMessage() + "\n");
            usage(options);
            System.exit(1);
        } finally {
            System.out.println(VERSION);
        }
    }
}

class Action {
    DSpaceObject obj;

    public static Action create(DSpaceObject obj) {
        assert(obj != null);
        switch (obj.getType()) {
            case Constants.BITSTREAM:
                return new BitstreamAction(obj);
            case Constants.BUNDLE:
                return new BundleAction(obj);
            case Constants.ITEM:
                return new ItemAction(obj);
            case Constants.COLLECTION:
                return new CollectionAction(obj);
            default: assert(false);
                    throw new RuntimeException("should never try to create Action from " + obj.toString());
        }
    }

    Action(DSpaceObject o)  {
        assert(o != null);
        obj = o;
    }

    public String describe() {
        String parent = "";
        try {
            parent = obj.getParentObject().toString();
        } catch (SQLException e) {
            parent = e.getMessage();
        } catch (NullPointerException ne) {
            parent = "NONE";
        }
        String[] vals = { obj.toString(), getString(obj.getHandle(), "undefined"), "parent=" + parent };
        return StringUtils.join(vals, " ");
    }

    public static String getString(String strOrNull, String undefined) {
        String ret = strOrNull;
        if (strOrNull == null)
            ret =  undefined;
        return ret.replaceAll("\\s","");
    }
}

class  BitstreamAction extends  Action {
    Bitstream bit;

    BitstreamAction(DSpaceObject o) {
        super(o);
        bit = (Bitstream) obj;
    }

    @Override
    public String describe() {
        String[] vals = {super.describe(),
                getString(bit.getFormat().getShortDescription(), "UNKNOWN_FORMAT"),
                getString(bit.getName(), ""),
                bit.getInternalId()};
        return StringUtils.join(vals, " ");
    }
}

class  BundleAction extends  Action {
    Bundle bdl;

    BundleAction(DSpaceObject o) {
        super(o);
        bdl = (Bundle) obj;
    }

    @Override
    public String describe() {
        String isEmbargoed = "";
        try {
            isEmbargoed = "isEmbargoed=" + String.valueOf(bdl.isEmbargoed());
        } catch (SQLException e) {
            isEmbargoed = e.getMessage();
        }
        String[] vals = {super.describe(), isEmbargoed, getString(bdl.getName(), "") };
        return StringUtils.join(vals, " ");
    }
}

class  ItemAction extends  Action {
    Item itm;

    ItemAction(DSpaceObject o) {
        super(o);
        itm = (Item) obj;
    }

    @Override
    public String describe() {
        String isWithdrawn = "";
        isWithdrawn = "isWithdrawn=" + String.valueOf(itm.isWithdrawn());
        String[] vals = {super.describe(), isWithdrawn, getString(itm.getName(), "") };
        return StringUtils.join(vals, " ");
    }
}

class  CollectionAction extends  Action {
    Collection col;

    CollectionAction(DSpaceObject o) {
        super(o);
        col = (Collection) obj;
    }

    @Override
    public String describe() {
        String[] vals = {super.describe(), getString(col.getName(), "") };
        return StringUtils.join(vals, " ");
    }
}