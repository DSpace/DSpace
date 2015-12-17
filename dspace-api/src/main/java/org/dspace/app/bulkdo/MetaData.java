/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkdo;

import org.apache.commons.cli.ParseException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
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

        ItemService itemService = ContentServiceFactory.getInstance().getItemService();
        Lister lister = new Lister(args.getContext(), args.getRoot(), args.getType());
        ArrayList<ActionTarget> targets = lister.getTargets(args.getType(), args.getWorkflowItemsOnly());
        for (ActionTarget at : targets) {
            Item item = (Item) at.getObject();
            List<MetadataValue> vals = itemService.getMetadata(item, args.schema, args.element, args.qualifier, null);
            at.put(beforeKey, MetadataValue.collectValues(vals));
            at.put(changedKey, false);
            switch (args.getAction()) {
                case Arguments.DO_ADD:
                    // check whether args.metaData_value exists
                    Boolean hasSameValue = false;
                    for (MetadataValue v : vals) {
                        hasSameValue = (v.getValue().equals(args.metaData_value));
                        if (hasSameValue) break;
                    }
                    if (!hasSameValue) {  // could not find value --> so set it
                        itemService.addMetadata(args.getContext(), (Item) at.getObject(), args.schema, args.element, args.qualifier, Item.ANY, args.metaData_value);
                        at.put(changedKey, true);
                    }
                    break;
                case Arguments.DO_DEL:
                    List<String> valList = new ArrayList<String>();
                    for (MetadataValue v : vals) {
                        if (v.getValue().equals(args.metaData_value)) {
                            // we will remove
                            at.put(changedKey, true);
                        } else {
                            // a keeper
                            valList.add(v.getValue());
                        }
                    }
                    itemService.clearMetadata(args.getContext(), (Item) at.getObject(), args.schema, args.element, args.qualifier, Item.ANY);
                    itemService.addMetadata(args.getContext(), (Item) at.getObject(), args.schema, args.element, args.qualifier, null,valList);
                    break;
                default:
                    throw new RuntimeException("Don't know how to " + args.getActionString());
            }
            if (!args.getDryRun()) {
                itemService.update(args.getContext(), item);
                args.getContext().complete();
            }
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
        } catch (AuthorizeException ae) {
            System.err.println("ERROR: " + ae.getMessage() + "\n");
            System.exit(1);
        } catch (ParseException pe) {
            System.err.println("ERROR: " + pe.getMessage() + "\n");
            args.usage();
            System.exit(1);
        }
    }
}

class MetaDataArguments extends  Arguments {
    String metaData_name;
    String metaData_value;
    String schema, qualifier, element;

    MetaDataArguments() {
        super(MetaData.class.getCanonicalName(), new char[]{Arguments.DO_ADD, Arguments.DO_DEL});
        options.addOption(Arguments.METADATA, Arguments.METADATA_LONG, true, "metadata setting of the form 'schema.ualifier.name=value'");
    }

    @Override
    public Boolean parseArgs(String[] argv) throws ParseException {
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

    @Override
    public void shortDescription() {
        System.out.println("ADD metaData value to or DELete value from all dspaceObjects of given type contained in root");
    }
}
