package org.dspace.app.bulkdo;

import org.apache.commons.cli.ParseException;
import org.dspace.app.util.SyndicationFeed;
import org.dspace.content.*;
import org.dspace.core.Constants;
import org.dspace.core.Context;


import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by monikam on 4/2/14.
 */
public class Lister {

    Context c = null;
    DSpaceObject dobj;

    // HACK
    public static final int NTARGETTYPE = 8;
    ActionTarget[][] actionTargets = new ActionTarget[NTARGETTYPE][];

    Lister(Context c, DSpaceObject root, int myType) throws SQLException {
        dobj = root;
        if (!validType(myType)) {
            throw new RuntimeException("invalid type " + myType);
        }
        getTargets(myType);
    }

    public ActionTarget[] getTargets(int type) throws SQLException {
        assert (validType(type));
        if (null == actionTargets[type]) {
            switch (type) {
                case Constants.COLLECTION:
                    actionTargets[Constants.COLLECTION] = ActionTarget.createArray(listCollections());
                    break;
                case Constants.ITEM:
                    getTargets(Constants.COLLECTION);
                    actionTargets[Constants.ITEM] = ActionTarget.createsArray(listItems());
                    break;
                case Constants.BUNDLE:
                    getTargets(Constants.ITEM);
                    actionTargets[Constants.BUNDLE] = ActionTarget.createsArray(listBundles());
                    break;
                case Constants.BITSTREAM:
                    getTargets(Constants.BUNDLE);
                    actionTargets[Constants.BITSTREAM] = ActionTarget.createsArray(listBitstreams());
                    break;
                default:
                    throw new RuntimeException("should never get here");
            }
        }
        return actionTargets[type];
    }

    public static boolean validType(int type) {
        return (type == Constants.COLLECTION) || (type == Constants.ITEM) ||
                (type == Constants.BUNDLE) || (type == Constants.BITSTREAM);

    }

    private Collection[] listCollections() throws SQLException {
        Collection[] cols = null;
        if (dobj.getType() == Constants.COMMUNITY) {
            Community comm = (Community) dobj;
            cols = comm.getCollections();
        } else if (dobj.getType() == Constants.COLLECTION) {
            cols = new Collection[1];
            cols[0] = (Collection) dobj;
        } else {
            cols = new Collection[0];
        }
        System.out.println("Found " + cols.length + " collections");
        return cols;
    }

    private ArrayList<DSpaceObject> listItems() throws SQLException {
        ArrayList<DSpaceObject> items = null;
        if (dobj.getType() == Constants.ITEM) {
            items = new ArrayList<DSpaceObject>(1);
            items.add((Item) dobj);
        } else {
            ActionTarget[] cols = actionTargets[Constants.COLLECTION];
            // collect ITEMS from collections  (only the true members / in_archive=1 )
            items = new ArrayList<DSpaceObject>(cols.length * 4);
            for (int i = 0; i < cols.length; i++) {
                ItemIterator iter = ((Collection) cols[i].geObject()).getItems();
                while (iter.hasNext()) {
                    items.add(iter.next());
                }
            }
        }
        System.out.println("Found " + items.size() + " items");
        return items;
    }

    private ArrayList<DSpaceObject> listBundles() throws SQLException {
        ArrayList<DSpaceObject> bundles = null;
        if (dobj.getType() == Constants.BUNDLE) {
            bundles = new ArrayList<DSpaceObject>(1);
            bundles.add((Bundle) dobj);
        } else {
            ActionTarget[] items =actionTargets[Constants.ITEM];
            // collect BUNDLES from items
            bundles = new ArrayList<DSpaceObject>(items.length);
            for (int i = 0; i < items.length; i++) {
                Bundle[] bs = ((Item) items[i].geObject()).getBundles();
                Collections.addAll(bundles, bs);
            }
        }
        System.out.println("Found " + bundles.size() + " bundles");
        return bundles;
    }

    private ArrayList<DSpaceObject> listBitstreams() throws SQLException {
        ArrayList<DSpaceObject> bitstreams = null;
        if (dobj.getType() == Constants.BITSTREAM) {
            bitstreams = new ArrayList<DSpaceObject>(1);
            bitstreams.add((Bitstream) dobj);
        } else {
            ActionTarget[] bundles = actionTargets[Constants.BUNDLE];
            bitstreams = new ArrayList<DSpaceObject>(bundles.length);
            // collect BITSTREAMS from bundles
            for (int i = 0; i < bundles.length; i++) {
                Bitstream[] bits = ((Bundle) bundles[i].geObject()).getBitstreams();
                Collections.addAll(bitstreams, bits);
            }
        }
        System.out.println("Found " + bitstreams.size() + " bitstreams");
        return bitstreams;
    }


    public static void main(String argv[]) {
        Arguments args = new Arguments();
        try {
            if (args.parseArgs(argv)) {
                Lister lister = new Lister(args.getContext(), args.getRoot(), args.getType());
                ActionTarget[] targets = lister.getTargets(args.getType());

                System.out.println("# " + targets.length + " type=" + args.getType());
                Printer p = Printer.create(System.out, args.getFormat());
                for (int i = 0; i < targets.length; i++)
                    p.println(targets[i]);
           }
        } catch (SQLException se) {
            System.err.println("ERROR: " + se.getMessage() + "\n");
            System.exit(1);
        } catch (ParseException pe) {
            System.err.println("ERROR: " + pe.getMessage() + "\n");
            args.usage();
            System.exit(1);
        }
    }

}