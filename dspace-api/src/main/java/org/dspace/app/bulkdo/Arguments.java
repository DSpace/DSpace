package org.dspace.app.bulkdo;

import org.apache.commons.cli.*;
import org.apache.commons.lang.StringUtils;
import org.dspace.content.*;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.dspace.app.util.Util;

import java.sql.SQLException;


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

    protected Options options = null;
    protected CommandLine line;

    private Context c = null;
    private DSpaceObject dobj = null;
    private int myType = -1;
    private int format = Printer.TXT_FORMAT;

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

    Arguments()  {
        options = new Options();

        options.addOption(ROOT, ROOT_LONG, true, "handle / type.ID");
        options.addOption(TYPE, TYPE_LONG, true, "type: collection, item, bundle, or bitstream ");
        options.addOption(FORMAT, FORMAT_LONG, true, "output format: tsv or txt");
        options.addOption(HELP, HELP_LONG, false, "help");
    }

    public void usage() {
        HelpFormatter myhelp = new HelpFormatter();
        myhelp.printHelp("Bulk Apply ActionTarget\n", options);
        System.out.println("\n");
        System.out.println("TODO: ADD EXPLANATIONS");
    }

    public Boolean parseArgs(String[] argv) throws ParseException, SQLException  {
        CommandLineParser parser = new PosixParser();
        line = parser.parse(options, argv);
        if (line.hasOption(HELP)) {
            usage();
            return false;
        }

        try {
            c = new Context();
        } catch (SQLException e) {
            throw new ParseException("Could not access database");
        }

        String handle = line.getOptionValue(ROOT);
        if (handle == null || handle.isEmpty())
            throw new ParseException("Missing root object argument");

        String typeString = line.getOptionValue(TYPE);
        if (typeString == null || typeString.isEmpty())
            throw new ParseException("Missing type argument");

        myType = Constants.getTypeID(typeString.toUpperCase());
        if (myType != Constants.COLLECTION &&
                myType != Constants.ITEM &&
                myType != Constants.BUNDLE &&
                myType != Constants.BITSTREAM) {
            throw new ParseException("type must be collection, item, bundle, or bitstream");
        }

        if (line.hasOption(FORMAT)) {
            format = Printer.getFormat(line.getOptionValue(FORMAT).toUpperCase());
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

        return true;
    }

    public String toString() {
        String root = Util.toString(dobj, "NULL");
        return ROOT_LONG + "=" + root + " " + TYPE_LONG + "=" + Constants.typeText[myType] +
                " " + FORMAT_LONG + "=" + Printer.formatText[format];
    }
}




