/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkdo;

import org.apache.commons.cli.*;
import org.apache.commons.lang.StringUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

import java.io.PrintStream;
import java.sql.SQLException;

import static java.util.Arrays.deepToString;


/**
 * Created by monikam on 4/2/14.
 */
class Arguments {

    // declare all option constants here to avoid accidental name clashes

    public static String ACTION = "a";
    public static String ACTION_LONG = "action";

    public static String BITSTREAM_FILE = "b";
    public static String BITSTREAM_FILE_LONG = "bitstream";

    public static String DSPACE_ACTION = "d";
    public static String DSPACE_ACTION_LONG = "dspace_action";

    public static String EPERSON = "e";
    public static String EPERSON_LONG = "eperson";

    public static String FORMAT = "f";
    public static String FORMAT_LONG = "format";

    public static String HELP = "h";
    public static String HELP_LONG = "help";

    public static String KEYS = "i";
    public static String KEYS_LONG = "include";

    public static String METADATA = "m";
    public static String METADATA_LONG = "meta_data";

    public static String ROOT = "r";
    public static String ROOT_LONG = "root";

    public static String TYPE = "c";
    public static String TYPE_LONG = "class";

    public static String VERBOSE = "v";
    public static String VERBOSE_LONG = "verbose";

    static String WORKFLOW_ITEM = "W";
    static String WORKFLOW_ITEM_LONG = "doWorkFlowItems";

    public static String WHO = "w";
    public static String WHO_LONG = "who";

    public static String DRYRUN  = "t";
    public static String DRYRUN_LONG = "test";

    public static final String[] actionText = {"ADD", "DEL", "REPLACE", "LIST"};
    public static final char DO_ADD = 'A';
    public static final char DO_DEL = 'D';
    public static final char DO_REPLACE = 'R';
    public static final char DO_LIST = 'L';

    protected Options options = null;
    protected CommandLine line;
    protected String mainClass;

    private Context c = null;
    private DSpaceObject dobj = null;
    private int myType = -1;
    private int format = Printer.TXT_FORMAT;
    private static String[] defaultKeys = {"object", "parent"};
    private String[] keys = defaultKeys;

    private boolean verbose;
    private boolean dryRun;
    private boolean doWorkflowItems;

    private char doAction;
    private char[] availableActions;

    Arguments(String mainClass) {
        this(mainClass, null);
    }

    Arguments(String mainCmd, char[] myAvailableActions) {
        mainClass = mainCmd;
        options = new Options();
        availableActions = myAvailableActions;
        doAction = '?';
        if (myAvailableActions != null && myAvailableActions.length > 0) {
            if (myAvailableActions.length > 1) {
                // there is actually a choice of what to do
                String availableActionStrings = "";
                for (int j = 0; j < myAvailableActions.length; j++) {
                    if (j > 0)
                        availableActionStrings += ", ";
                    availableActionStrings += getActionString(myAvailableActions[j]);
                }
                options.addOption(ACTION, ACTION_LONG, true, "what to do, available " + availableActionStrings);
            } else {
                doAction = myAvailableActions[0]; // its the oly option
            }
        } else {
            // no availableActions
            doAction = DO_LIST;
        }
        options.addOption(ROOT, ROOT_LONG, true, "handle / type.ID");
        options.addOption(TYPE, TYPE_LONG, true, "type: collection, item, bundle, or bitstream ");
        options.addOption(WORKFLOW_ITEM, WORKFLOW_ITEM_LONG, false, "list items in workflow");
        options.addOption(EPERSON, EPERSON_LONG, true, "dspace user account (email or netid) used for authorization to dspace app");
        options.addOption(FORMAT, FORMAT_LONG, true, "output format: tsv or txt");
        options.addOption(KEYS, KEYS_LONG, true, "include listed object keys/properties in output; give as comma separated list");
        options.addOption(VERBOSE, VERBOSE_LONG, false, "verbose");
        options.addOption(DRYRUN, DRYRUN_LONG, false, "dryrun - do not actually change anything; default is false");
        options.addOption(HELP, HELP_LONG, false, "help");
    }

    private String getActionString(char action) {
        for (String anActionText : actionText) {
            if (anActionText.charAt(0) == action) {
                return anActionText;
            }
        }
        return null;
    }

    public Context getContext() {
        return c;
    }

    public int getType() {
        return myType;
    }

    public int getFormat() {
        return format;
    }

    public DSpaceObject getRoot() {
        return dobj;
    }

    public char getAction() {
        return doAction;
    }

    public String getActionString() {
        return getActionString(doAction);
    }

    public boolean getWorkflowItemsOnly() { return doWorkflowItems; }

    public boolean getVerbose() {  return verbose;  }

    public boolean getDryRun() {
        return dryRun;
    }

    public Printer getPrinter() {
        Printer p =  Printer.create(System.out, getFormat(), keys);
        if (verbose)
            printArgs(p, "# ");
        return p;
    }

    public Options getOptions() {
        return options;
    }

    /**
     * determine whether objects of 'one' type include DSPaceObjects objects of 'other' type
     */
    /* TODO: relying on Constants values - move to Constants class ?? */
    static Boolean typeIncludes(int one, int other) {
        if (one == other)
            return true;
        if (one <= Constants.COMMUNITY) {
            return one > other;
        }
        if (one == Constants.GROUP) {
            return other == Constants.EPERSON;
        }
        return false; // not quite sure what to say about SITE
    }

    public void usage() {
        HelpFormatter myhelp = new HelpFormatter();
        myhelp.printHelp(mainClass + "\n", options);
        System.out.println("");

        shortDescription();
        System.out.println("");

        System.out.println("OPTION " + KEYS_LONG + ": ");
        System.out.println("\tDefault Print Keys: " + deepToString(defaultKeys));
        optionExplainKeys();
        System.out.println("");

        System.out.println("DESCRIPTION");
        description();
    }

    public void shortDescription() {
        System.out.println("TODO - add short description");
    }

    public void description() {
        // TODO - add more detail
        // for now see README file
    }

    protected void optionExplainKeys() {
        System.out.println("\tAvailable Keys depend on the type of object being printed");
        System.out.println("\t\t" + Constants.typeText[Constants.COLLECTION] + ":" + deepToString(ActionTarget.availableKeys(Constants.COLLECTION)));
        System.out.println("\t\t" + Constants.typeText[Constants.ITEM] + ":" + deepToString(ActionTarget.availableKeys(Constants.ITEM)) + "any metadafield, POLICY.dspace_action");
        System.out.println("\t\t" + Constants.typeText[Constants.BUNDLE] + ":" + deepToString(ActionTarget.availableKeys(Constants.BUNDLE)));
        System.out.println("\t\t" + Constants.typeText[Constants.BITSTREAM] + ":" + deepToString(ActionTarget.availableKeys(Constants.BITSTREAM)));
        System.out.println("\t\t" + "where dspace_action may be one of:  " + deepToString(Constants.actionText));
    }

    public Boolean parseArgs(String[] argv) throws ParseException {
        try {
            CommandLineParser parser = new PosixParser();
            line = parser.parse(options, argv);
            if (line.hasOption(HELP)) {
                usage();
                return false;
            }

            verbose = line.hasOption(VERBOSE);
            dryRun = line.hasOption(DRYRUN);
            doWorkflowItems = line.hasOption(WORKFLOW_ITEM);

            c = new Context();

            String rootObj = line.getOptionValue(ROOT);
            if (rootObj == null || rootObj.isEmpty())
                throw new ParseException("Missing root object argument");
            dobj = DSpaceObject.fromString(c, rootObj);
            if (dobj == null)
                throw new ParseException(rootObj + " is not a valid DSpaceObject");

            if (dobj.getType() != Constants.COMMUNITY &&
                    dobj.getType() != Constants.COLLECTION &&
                    dobj.getType() != Constants.ITEM &&
                    dobj.getType() != Constants.BUNDLE &&
                    dobj.getType() != Constants.BITSTREAM) {
                throw new ParseException(dobj + " is not a community, collection, item, bundle or bitstream");
            }

            if (line.hasOption(TYPE)) {
                String typeString = line.getOptionValue(TYPE);
                if (typeString == null || typeString.isEmpty())
                    throw new ParseException("Missing type argument");
                myType = Constants.getTypeID(typeString.toUpperCase());
            } else {
                // default to type of root arg
                myType = dobj.getType();
            }

            if (myType != Constants.COLLECTION &&
                    myType != Constants.ITEM &&
                    myType != Constants.BUNDLE &&
                    myType != Constants.BITSTREAM)
                throw new ParseException("type must be collection, item, bundle, or bitstream");

            if (!typeIncludes(dobj.getType(), myType)) {
                throw new ParseException(Constants.typeText[myType] + "s are not nested inside " +
                        Constants.typeText[dobj.getType()]);
            }

            if (line.hasOption(FORMAT)) {
                format = Printer.getFormat(line.getOptionValue(FORMAT).toUpperCase());
            }

            if (line.hasOption(KEYS)) {
                String[] keyList = StringUtils.split(line.getOptionValue(KEYS), ",");
                if (keyList.length > 0) {
                    for (int i = 0; i < keyList.length; i++) {
                        keyList[i] = keyList[i].trim();
                    }
                    keys = keyList;
                } else {
                    throw new ParseException("Invalid " + KEYS_LONG + " option: " + line.getOptionValue(KEYS));
                }
            }

            if (line.hasOption(ACTION)) {
                String actionStr = line.getOptionValue(ACTION).toUpperCase();
                int i = -1;
                if (actionStr == null || actionStr.isEmpty()) {
                    actionStr = ""; // make it nice for error message
                } else {
                    char action = actionStr.charAt(0);
                    for (i = 0; i < availableActions.length; i++) {
                        if (availableActions[i] == action)
                            break;
                    }
                }
                if (i < 0 || i == availableActions.length) {
                    throw new ParseException("No such action: '" + actionStr + "'");
                }
                doAction = availableActions[i];
            }

            if (line.hasOption(Arguments.EPERSON)) {
                String person = line.getOptionValue(EPERSON);
                EPerson user = (EPerson) DSpaceObject.fromString(getContext(), "EPerson." + person);
                if (user == null) {
                    throw new ParseException("No such EPerson: " + person);
                }
                getContext().setCurrentUser(user);
            }
            return true;
        } catch (SQLException e) {
            throw new ParseException("Configuration error: " + e.getMessage());
        }
    }

    public void printArgs(PrintStream out, String prefix) {
        out.println(prefix + " " + ROOT_LONG + "=" + dobj);
        out.println(prefix + " " + TYPE_LONG + "=" + Constants.typeText[myType]);
        out.println(prefix + " " + ACTION_LONG + "=" + getAction() + " " + getActionString());
        out.println(prefix + " " + WORKFLOW_ITEM_LONG + "=" + String.valueOf(doWorkflowItems));
        out.println(prefix + " " + FORMAT_LONG + "=" + getFormat());
        out.println(prefix + " " + KEYS_LONG + "=" + deepToString(keys));
        out.println(prefix + " " + EPERSON_LONG + "=" + line.getOptionValue(EPERSON));
        out.println(prefix + " " + DRYRUN_LONG + "=" + dryRun);
    }
}




