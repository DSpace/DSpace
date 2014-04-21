package org.dspace.app.bulkdo;

import org.apache.commons.cli.*;
import org.apache.commons.lang.StringUtils;
import org.dspace.content.*;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.dspace.app.util.Util;

import java.sql.SQLException;

import static java.util.Arrays.deepToString;


/**
 * Created by monikam on 4/2/14.
 */
class Arguments {

    public static String ROOT = "r";
    public static String ROOT_LONG = "root";

    public static String TYPE = "t";
    public static String TYPE_LONG = "type";

    public static String HELP = "h";
    public static String HELP_LONG = "help";

    public static String FORMAT = "f";
    public static String FORMAT_LONG = "format";

    public static String KEYS = "i";
    public static String KEYS_LONG = "include";

    public static String VERBOSE = "v";
    public static String VERBOSE_LONG = "verbose";

    protected Options options = null;
    protected CommandLine line;

    private Context c = null;
    private DSpaceObject dobj = null;
    private int myType = -1;
    private int format = Printer.TXT_FORMAT;
    private static String[] defaultKeys = { "object", "handle", "parent", "name"};
    private String[] keys = defaultKeys;
    private boolean verbose;

    Arguments()  {
        options = new Options();

        options.addOption(ROOT, ROOT_LONG, true, "handle / type.ID");
        options.addOption(TYPE, TYPE_LONG, true, "type: collection, item, bundle, or bitstream ");
        options.addOption(FORMAT, FORMAT_LONG, true, "output format: tsv or txt");
        options.addOption(KEYS, KEYS_LONG, true, "include listed object keys/properties in output; give as comma separated list");
        options.addOption(VERBOSE, VERBOSE_LONG, false, "verbose");
        options.addOption(HELP, HELP_LONG, false, "help");
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

    public Options getOptions() {
        return options;
    }

    public boolean getVerbose() {
        return verbose;
    }

    public Printer getPrinter() {
        return Printer.create(System.out, getFormat(), keys);
    }

    public void usage() {
        HelpFormatter myhelp = new HelpFormatter();
        myhelp.printHelp("Bulk Apply ActionTarget\n", options);
        System.out.println("");

        System.out.println("OPTION " + KEYS_LONG + ": ");
        System.out.println("\tDefault: " + deepToString(defaultKeys) );
        optionExplainKeys();
        System.out.println("");

        System.out.println("TODO: ADD EXPLANATIONS");
    }

    protected void optionExplainKeys() {
        System.out.println("\tAvailable Keys depend on the type of object being printed" );
        System.out.println("\t\t" + Constants.typeText[Constants.COLLECTION] + ":" + deepToString(ActionTarget.availableKeys(Constants.COLLECTION)));
        System.out.println("\t\t" + Constants.typeText[Constants.ITEM] + ":" + deepToString(ActionTarget.availableKeys(Constants.ITEM)));
        System.out.println("\t\t" + Constants.typeText[Constants.BUNDLE] + ":" + deepToString(ActionTarget.availableKeys(Constants.BUNDLE)));
        System.out.println("\t\t" + Constants.typeText[Constants.BITSTREAM] + ":" + deepToString(ActionTarget.availableKeys(Constants.BITSTREAM)));
    }

    public Boolean parseArgs(String[] argv) throws ParseException, SQLException {
        CommandLineParser parser = new PosixParser();
        line = parser.parse(options, argv);
        if (line.hasOption(HELP)) {
            usage();
            return false;
        }

        verbose = line.hasOption(VERBOSE);

        try {
            c = new Context();
        } catch (SQLException e) {
            throw new ParseException("Could not access database");
        }

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

        // HACK: relying on const values
        if (myType > dobj.getType()) {
            throw new ParseException(Constants.typeText[myType] + "s are not nested inside " +
                    Constants.typeText[dobj.getType()]);
        }

        return true;
    }

    public String toString() {
        String root = Util.toString(dobj, "NULL");
        return ROOT_LONG + "=" + root + " " + TYPE_LONG + "=" + Constants.typeText[myType] +
                " " + FORMAT_LONG + "=" + Printer.formatText[format];
    }
}




