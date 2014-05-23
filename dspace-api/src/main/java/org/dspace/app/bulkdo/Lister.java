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
    ArrayList<ActionTarget> actionTargets[] = new ArrayList[NTARGETTYPE];

    Lister(Context c, DSpaceObject root, int myType) throws SQLException {
        rootObject = root;
        context = c;
        if (!validType(myType)) {
            throw new RuntimeException("invalid type " + myType);
        }
    }

    public ArrayList<ActionTarget> getTargets(int type, boolean workflowItems) throws SQLException {
        assert (validType(type));
        if (null == actionTargets[type]) {
            switch (type) {
                case Constants.COLLECTION:
                    actionTargets[Constants.COLLECTION] = listCollections();
                    break;
                case Constants.ITEM:
                    getTargets(Constants.COLLECTION, workflowItems);
                    actionTargets[Constants.ITEM] = listItems(workflowItems);
                    break;
                case Constants.BUNDLE:
                    getTargets(Constants.ITEM, workflowItems);
                    actionTargets[Constants.BUNDLE] = listBundles();
                    break;
                case Constants.BITSTREAM:
                    getTargets(Constants.BUNDLE, workflowItems);
                    actionTargets[Constants.BITSTREAM] = listBitstreams();
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

    private ArrayList<ActionTarget>  listCollections() throws SQLException {
        ArrayList<ActionTarget>  targets = new ArrayList<ActionTarget>();
        if (rootObject.getType() == Constants.COMMUNITY) {
            Community comm = (Community) rootObject;
            Collection[] cols = comm.getCollections();
            targets = ActionTarget.createArray(context, new ActionTarget(context, null, comm), cols);
        } else if (rootObject.getType() == Constants.COLLECTION) {
            targets.add(CollectionActionTarget.create(context, null, rootObject));
        }
        log.info("Found " + targets.size() + " collections");
        return targets;
    }

    private ArrayList<ActionTarget> listItems(boolean workflowItem) throws SQLException {
        ArrayList<ActionTarget> targets = new ArrayList<ActionTarget>();
        if (rootObject.getType() == Constants.ITEM) {
             // TODO check whether  rootObj.is_workflowItem <==> workflowItem param
            targets.add(new ItemActionTarget(context, null, rootObject));
        } else {
            ArrayList<ActionTarget> cols = actionTargets[Constants.COLLECTION];
            if (workflowItem) {
                for (int i = 0; i < cols.size(); i++) {
                    WorkspaceItem[] wis = WorkspaceItem.findByCollection(context, (Collection) cols.get(i).getObject());
                    for (int j = 0; j < wis.length; j++) {
                        targets.add(ActionTarget.create(context, cols.get(i), wis[j].getItem()));
                    }
                }
            } else {
                for (int i = 0; i < cols.size(); i++) {
                    // items from collections and subcollections ???
                    ItemIterator iter = ((Collection) cols.get(i).getObject()).getAllItems();
                    while (iter.hasNext()) {
                        targets.add(ActionTarget.create(context, cols.get(i), iter.next()));
                    }
                }
            }
        }
        log.info("Found " + targets.size() + " items");
        return targets;
    }

    private ArrayList<ActionTarget> listBundles() throws SQLException {
        ArrayList<ActionTarget> targets = new ArrayList<ActionTarget>();
        if (rootObject.getType() == Constants.BUNDLE) {
            targets.add(new BundleActionTarget(context, null, rootObject));
        } else {
            ArrayList<ActionTarget> items = actionTargets[Constants.ITEM];
            // collect BUNDLES from items
            for (int i = 0; i < items.size(); i++) {
                Bundle[] bundles = ((Item) items.get(i).getObject()).getBundles();
                for (Bundle bdl : bundles) {
                    targets.add(new BundleActionTarget(context, items.get(i), bdl));
                }
            }
        }
        log.info("Found " + targets.size() + " bundles");
        return targets;
    }

    private ArrayList<ActionTarget> listBitstreams() throws SQLException {
        ArrayList<ActionTarget> targets = new ArrayList<ActionTarget>();
        if (rootObject.getType() == Constants.BITSTREAM) {
            targets.add(new BitstreamActionTarget(context, null, rootObject));
        } else {
            ArrayList<ActionTarget>  bundles = actionTargets[Constants.BUNDLE];
            // collect BITSTREAMS from bundles
            for (ActionTarget bdl : bundles) {
                Bitstream[] bits = ((Bundle) bdl.getObject()).getBitstreams();
                for (Bitstream bit : bits) {
                    targets.add(new BitstreamActionTarget(context, bdl, bit));
                }
            }
        }
        log.info("Found " + targets.size() + " bitstreams");
        return targets;
    }


    public static void main(String argv[]) {
        ConsoleAppender ca = new ConsoleAppender();
        ca.setWriter(new OutputStreamWriter(System.out));
        ca.setLayout(new PatternLayout("# %c: %m%n"));
        log.addAppender(ca);

        Arguments args = new ListArguments();
        try {
            if (args.parseArgs(argv)) {
                Lister lister = new Lister(args.getContext(), args.getRoot(), args.getType());
                ArrayList<ActionTarget> targets = lister.getTargets(args.getType(), args.getWorkflowItemsOnly());

                log.debug("# " + targets.size() + " type=" + args.getType());
                Printer p = args.getPrinter();
                for (int i = 0; i < targets.size(); i++)
                    p.println(targets.get(i));
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

class ListArguments extends  Arguments {
    public ListArguments() {
        super(Lister.class.getCanonicalName());
    }

    @Override
    public void shortDescription() {
        System.out.println("List dspaceObjects of given type contained in root, printing properties designated by include keys");
    }

}

