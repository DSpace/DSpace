/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.identifier.doi;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.help.HelpFormatter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;
//import org.dspace.utils.DSpace;

public class DOIMigrator {
    private static final Pattern looksLikeDOI = Pattern
            .compile("^(?:https?://(?:dx\\.)?doi\\.org/|doi:|)(10\\..+/.+)$");
    private static final String[] csvColumns = { "ItemUUID", "FromFieldName", "FromValue", "ToFieldName", "ToValue" };
    private static final Logger LOG = LogManager.getLogger(DOIOrganiser.class);
    private final Context context;

    protected ItemService itemService;
    protected MetadataFieldService metadataFieldService;

    record DOIMigration(Item dspaceItem,
            MetadataValue from,
            MetadataFieldName toFieldName,
            String toValue) {
    }

    public DOIMigrator(Context context) {
        this.context = context;
        this.itemService = ContentServiceFactory.getInstance().getItemService();
        this.metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();
    }

    public static void main(String[] args) {
        // setup Context
        Context context = new Context();

        // Started from commandline, don't use the authentication system.
        context.turnOffAuthorisationSystem();

        DOIMigrator migrator = new DOIMigrator(context);
        // run command line interface
        try {
            migrator.runCLI(args);
        } catch (IOException ioe) {
            System.err.println("IO error: " + ioe.getMessage());
        }

        try {
            context.complete();
        } catch (SQLException sqle) {
            System.err.println("Cannot save changes to database: " + sqle.getMessage());
            System.exit(-1);
        }
    }

    public void runCLI(String[] args) throws IOException {
        Options options = new Options();

        options.addOption(null, "only-prefix", true,
                "Only move DOIs with the given prefix");
        options.addOption(null, "except-prefix", true,
                "Move all DOIs without the given prefix");
        options.addOption("f", "from", true,
                "Metadata field name to move DOIs from");
        options.addOption("t", "to", true,
                "Metadata field name to move DOIs into");
        options.addOption("n", "dry-run", false,
                "Don’t actually make the move, just write a CSV of what would be moved");

        options.addOption("h", "help", false,
                "Show help message");

        CommandLineParser parser = new DefaultParser();
        CommandLine line = null;

        HelpFormatter helpFormatter = HelpFormatter.builder().get();
        try {
            line = parser.parse(options, args);
        } catch (ParseException ex) {
            LOG.fatal(ex);
            System.exit(1);
        }

        if (line.hasOption('h') || 0 == line.getOptions().length) {
            helpFormatter.printHelp("dspace doi-migrate", null, options, null, true);
            return;
        }

        if (line.hasOption("only-prefix") && line.hasOption("except-prefix")) {
            System.err.println("--only-prefix and --except-prefix options are mutually exclusive");
            System.exit(1);
        } else if (!line.hasOption("only-prefix") && !line.hasOption("except-prefix")) {
            System.err.println("One of --only-prefix and --except-prefix options are required");
            System.exit(1);
        }
        Iterator<Item> itemsIterator;
        try {
            itemsIterator = itemService.findByMetadataField(context, "dc", "identifier", "doi", Item.ANY);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
            return;
        }
        Function<String, Boolean> shouldMigrateDOI;
        if (line.hasOption("only-prefix")) {
            String prefix = line.getOptionValue("only-prefix");
            shouldMigrateDOI = (doi) -> doi.startsWith(prefix + '/');
        } else {
            String prefix = line.getOptionValue("except-prefix");
            shouldMigrateDOI = (doi) -> !doi.startsWith(prefix + '/');
        }
        // so:24511052
        Stream<Item> items = StreamSupport.stream(((Iterable<Item>) () -> itemsIterator).spliterator(), false);
        Stream<DOIMigration> doiMigrations = itemDOIMigrations(new MetadataFieldName(line.getOptionValue("from")),
                new MetadataFieldName(line.getOptionValue("to")),
                shouldMigrateDOI,
                items);

        if (line.hasOption("dry-run")) {
            CSVWriter csvWriter = new CSVWriter(new OutputStreamWriter(System.out));
            try {
                csvWriter.writeNext(csvColumns, true);
                doiMigrations.forEach((DOIMigration doiMigration) -> {
                    String[] cells = new String[csvColumns.length];
                    cells[0] = doiMigration.dspaceItem.getID().toString();
                    cells[1] = doiMigration.from.getMetadataField().toString('.');
                    cells[2] = doiMigration.from.getValue();
                    cells[3] = doiMigration.toFieldName.toString();
                    cells[4] = doiMigration.toValue.toString();
                    csvWriter.writeNext(cells, true);
                });
            } finally {
                csvWriter.close();
            }
        } else {
            doiMigrations.forEach((DOIMigration doiMigration) -> {
                MetadataValue metadataValue = doiMigration.from;
                if (!doiMigration.from.getMetadataField()
                        .toString('.')
                        .equals(doiMigration.toFieldName.toString())) {
                    try {
                        MetadataField newField = this.metadataFieldService.findByString(this.context,
                                doiMigration.toFieldName.toString(), '.');
                        metadataValue.setMetadataField(newField);
                    } catch (SQLException e) {
                        System.err.println(e.getMessage());
                        System.exit(-1);
                    }
                }
                metadataValue.setValue(doiMigration.toValue);
            });
        }
    }

    public Stream<DOIMigration> itemDOIMigrations(MetadataFieldName fromField,
            MetadataFieldName toField,
            Function<String, Boolean> shouldMigrateDOI,
            Stream<Item> items) {
        return items.flatMap((Item item) -> {
            Stream.Builder<DOIMigration> s = Stream.builder();
            List<MetadataValue> metadata = itemService.getMetadata(item,
                    fromField.schema,
                    fromField.element,
                    fromField.qualifier,
                    Item.ANY);

            for (MetadataValue val : metadata) {
                Matcher m = looksLikeDOI.matcher(val.getValue());
                if (m.matches()) {
                    String doi = m.group(1);
                    if (shouldMigrateDOI.apply(doi)) {
                        s.add(new DOIMigration(item,
                                val,
                                toField,
                                "https://doi.org/" + doi));
                    }
                }
            }

            return s.build();
        });
    }
}
