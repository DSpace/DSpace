package org.dspace.app.bulkdo;

import org.apache.commons.cli.ParseException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.Constants;

import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by monikam on 5/6/14.
 */
public class MetaData {
    /**
     * log4j logger
     */
    private static Logger log = Logger.getLogger(MetaData.class);

    MetaDataArguments args;

    MetaData(MetaDataArguments arguments) {
        args = arguments;
        log.debug(this);
    }

    public void apply(Printer p, boolean dryRun) throws SQLException, AuthorizeException {
        int i;
        log.debug("apply: " + args.getActionString() + " dryRun=" + String.valueOf(dryRun));
        String beforeKey = "before:" + args.metaData_name;
        String changedKey = "changed";
        p.addKey(args.metaData_name);
        if (args.getVerbose())
            p.addKey(beforeKey);
        p.addKey(changedKey);

        Lister lister = new Lister(args.getContext(), args.getRoot(), args.getType());
        ArrayList<ActionTarget> targets = lister.getTargets(args.getType(), args.getWorkflowItemsOnly());
        for (ActionTarget at : targets) {
            Item item = (Item) at.getObject();
            DCValue[] vals = item.getMetadata(args.schema, args.element, args.qualifier, Item.ANY);
            at.put(beforeKey, DCValue.valuesFor(vals));
            at.put(changedKey, false);
            switch (args.getAction()) {
                case Arguments.DO_ADD:
                    for (i= 0; i < vals.length; i++) {
                        if (vals[i].value.equals(args.metaData_value)) {
                            break;
                        }
                    }
                    if (i == vals.length) {
                        item.addMetadata(args.schema, args.element, args.qualifier, null, args.metaData_value);
                        at.put(changedKey, true);
                    }
                    break;
                case Arguments.DO_DEL:
                    List<String> valList = new ArrayList<String>();
                    for (i= 0; i < vals.length; i++) {
                        if (!vals[i].value.equals(args.metaData_value)) {
                            valList.add(vals[i].value);
                        } else {
                            at.put(changedKey, true);
                        }
                    }
                    item.clearMetadata(args.schema, args.element, args.qualifier, Item.ANY);
                    String[] valArr = valList.toArray(new String[valList.size()]);
                    item.addMetadata(args.schema, args.element, args.qualifier, Item.ANY, valArr);
                    break;
                default:
                    throw new RuntimeException("Don't know how to " + args.getActionString());
            }
            item.update();
            if (!args.getDryRun())
                args.getContext().commit();
            p.println(at);
        }

    }

    public String toString()  {
        String me =  args.metaData_name + "=" +  args.metaData_value;
        return me + " > " + args.getRoot();
    }

    public static void main(String argv[]) {
        ConsoleAppender ca = new ConsoleAppender();
        ca.setWriter(new OutputStreamWriter(System.out));
        ca.setLayout(new PatternLayout("# %c: %m%n"));
        log.addAppender(ca);

        MetaDataArguments args = new MetaDataArguments();
        try {
            if (args.parseArgs(argv)) {
                MetaData actor = new MetaData(args);
                Printer p = args.getPrinter();
                actor.apply(p, args.getDryRun());
            }
        } catch (SQLException se) {
            System.err.println("ERROR: " + se.getMessage() + "\n");
            System.exit(1);
        } catch (ParseException pe) {
            System.err.println("ERROR: " + pe.getMessage() + "\n");
            args.usage();
            System.exit(1);
        } catch (AuthorizeException ae) {
            System.err.println("ERROR: " + ae.getMessage() + "\n");
            System.exit(1);
        }
    }
}

class MetaDataArguments extends  Arguments {
    String metaData_name;
    String metaData_value;
    String schema, qualifier, element;

    MetaDataArguments() {
        super(new char[]{Arguments.DO_ADD, Arguments.DO_DEL});
        options.addOption(Arguments.METADATA, Arguments.METADATA_LONG, true,
                "metadata setting of the form 'schema.ualifier.name=value'");
    }

    @Override
    public Boolean parseArgs(String[] argv) throws ParseException, SQLException {
        if (super.parseArgs(argv)) {
            if (!line.hasOption(Arguments.ACTION)) {
                throw new ParseException("Missing " + Arguments.ACTION_LONG + " argument");
            }
            if (!line.hasOption(Arguments.METADATA)) {
                throw new ParseException("Missing " + Arguments.METADATA_LONG + " argument");
            }
            if (!line.hasOption(Arguments.EPERSON)) {
                throw new ParseException("Missing " + Arguments.EPERSON_LONG + " argument");
            }
            String md = line.getOptionValue(Arguments.METADATA);
            int i = md.indexOf('=');
            try {
                metaData_name = md.substring(0, i);
                metaData_value = md.substring(i + 1);
            } catch (Exception e) {
                throw new ParseException("Malformed metadata setting '" + md + "'");
            }
            if (getType() != Constants.ITEM) {
                throw new ParseException("Can set metadata on ITEMs only");
            }

            try {
                String[] bits = metaData_name.split("\\.");
                schema = bits[0];
                if (bits.length == 3) {
                    qualifier = bits[2];
                    element = bits[1];
                } else {
                    element = bits[1];
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new ParseException("Improper metadata name " + metaData_name);
            }

        }
        return true;
    }

    @Override
    public  void  printArgs(PrintStream out, String prefix) {
        super.printArgs(out, prefix);
        out.println(prefix + " " + Arguments.METADATA_LONG + "=" + metaData_name + "(" +
                        schema + "." + element + "." + String.valueOf(qualifier) + ") " +
                "=" + metaData_value);
   }
}