package org.dspace.app.bulkdo;

import org.apache.commons.cli.ParseException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.dspace.content.*;
import org.dspace.core.Constants;
import org.dspace.core.Context;


import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;

import static java.util.Arrays.deepToString;

/**
 * Created by monikam on 4/2/14.
 */
public class Lister {

    /** log4j logger */
    private static Logger log = Logger.getLogger(Lister.class);

    Context context = null;
    DSpaceObject rootObject;

    // HACK
    public static final int NTARGETTYPE = 8;
    ActionTarget[][] actionTargets = new ActionTarget[NTARGETTYPE][];

    Lister(Context c, DSpaceObject root, int myType) throws SQLException {
        rootObject = root;
        context = c;
        if (!validType(myType)) {
            throw new RuntimeException("invalid type " + myType);
        }
    }

    public ActionTarget[] getTargets(int type, boolean workflowItems) throws SQLException {
        assert (validType(type));
        if (null == actionTargets[type]) {
            switch (type) {
                case Constants.COLLECTION:
                    actionTargets[Constants.COLLECTION] = ActionTarget.createArray(listCollections());
                    break;
                case Constants.ITEM:
                    getTargets(Constants.COLLECTION, workflowItems);
                    actionTargets[Constants.ITEM] = ActionTarget.createsArray(listItems(workflowItems));
                    break;
                case Constants.BUNDLE:
                    getTargets(Constants.ITEM, workflowItems);
                    actionTargets[Constants.BUNDLE] = ActionTarget.createsArray(listBundles());
                    break;
                case Constants.BITSTREAM:
                    getTargets(Constants.BUNDLE, workflowItems);
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
        if (rootObject.getType() == Constants.COMMUNITY) {
            Community comm = (Community) rootObject;
            cols = comm.getCollections();
        } else if (rootObject.getType() == Constants.COLLECTION) {
            cols = new Collection[1];
            cols[0] = (Collection) rootObject;
        } else {
            cols = new Collection[0];
        }
        log.info("Found " + cols.length + " collections");
        return cols;
    }

    private ArrayList<DSpaceObject> listItems(boolean workflowItem) throws SQLException {
        ArrayList<DSpaceObject> items = null;
        if (rootObject.getType() == Constants.ITEM) {
             // TODO check whether  rootObj.is_workflowItem <==> workflowItem param
            items = new ArrayList<DSpaceObject>(1);
            items.add((Item) rootObject);
        } else {
            ActionTarget[] cols = actionTargets[Constants.COLLECTION];
            items = new ArrayList<DSpaceObject>(cols.length * 4);
            if (workflowItem) {
                for (int i = 0; i < cols.length; i++) {
                    WorkspaceItem[] wis = WorkspaceItem.findByCollection(context, (Collection) cols[i].getObject());
                    for (int j = 0; j < wis.length; j++) {
                        items.add(wis[j].getItem());
                    }
                }
            } else {
                for (int i = 0; i < cols.length; i++) {
                    // items from collections and subcollections ???
                    ItemIterator iter = ((Collection) cols[i].getObject()).getAllItems();
                    while (iter.hasNext()) {
                        items.add(iter.next());
                    }
                }
            }
        }
        log.info("Found " + items.size() + " items");
        return items;
    }

    private ArrayList<DSpaceObject> listBundles() throws SQLException {
        ArrayList<DSpaceObject> bundles = null;
        if (rootObject.getType() == Constants.BUNDLE) {
            bundles = new ArrayList<DSpaceObject>(1);
            bundles.add((Bundle) rootObject);
        } else {
            ActionTarget[] items =actionTargets[Constants.ITEM];
            // collect BUNDLES from items
            bundles = new ArrayList<DSpaceObject>(items.length);
            for (int i = 0; i < items.length; i++) {
                Bundle[] bs = ((Item) items[i].getObject()).getBundles();
                Collections.addAll(bundles, bs);
            }
        }
        log.info("Found " + bundles.size() + " bundles");
        return bundles;
    }

    private ArrayList<DSpaceObject> listBitstreams() throws SQLException {
        ArrayList<DSpaceObject> bitstreams = null;
        if (rootObject.getType() == Constants.BITSTREAM) {
            bitstreams = new ArrayList<DSpaceObject>(1);
            bitstreams.add((Bitstream) rootObject);
        } else {
            ActionTarget[] bundles = actionTargets[Constants.BUNDLE];
            bitstreams = new ArrayList<DSpaceObject>(bundles.length);
            // collect BITSTREAMS from bundles
            for (int i = 0; i < bundles.length; i++) {
                Bitstream[] bits = ((Bundle) bundles[i].getObject()).getBitstreams();
                Collections.addAll(bitstreams, bits);
            }
        }
        log.info("Found " + bitstreams.size() + " bitstreams");
        return bitstreams;
    }


    public static void main(String argv[]) {
        ConsoleAppender ca = new ConsoleAppender();
        ca.setWriter(new OutputStreamWriter(System.out));
        ca.setLayout(new PatternLayout("%c: %m%n"));
        log.addAppender(ca);

        ListerArguments args = new ListerArguments();
        try {
            if (args.parseArgs(argv)) {
                Lister lister = new Lister(args.getContext(), args.getRoot(), args.getType());
                ActionTarget[] targets = lister.getTargets(args.getType(), args.doWorkflowItems);

                log.debug("# " + targets.length + " type=" + args.getType());
                Printer p = args.getPrinter();
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

class ListerArguments extends Arguments {
    static String WORKFLOW_ITEM = "w";
    static String WORKFLOW_ITEM_LONG = "doWorkSpaceItems";

    boolean doWorkflowItems = false;

    ListerArguments() {
        super();
        options.addOption(WORKFLOW_ITEM, WORKFLOW_ITEM_LONG, false, "list items in workflow");
    }

    public Boolean parseArgs(String[] argv) throws ParseException, SQLException {
        if (super.parseArgs(argv)) {
            doWorkflowItems = line.hasOption(WORKFLOW_ITEM);
            return true;
        }
        return false;
    }
}
