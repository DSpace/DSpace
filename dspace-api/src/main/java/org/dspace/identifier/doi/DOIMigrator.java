/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.identifier.doi;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
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
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;

public class DOIMigrator {
    private static final Pattern looksLikeDOI = Pattern
            .compile("^(?:https?://(?:dx\\.)?doi\\.org/|doi:|)(10\\..+/.+)$");
    private static final String[] csvColumns = { "ItemUUID", "FromFieldName", "FromValue", "ToFieldName", "ToValue" };
    private static final Logger LOG = LogManager.getLogger(DOIMigrator.class);
    private final Context context;
    private int batchSize = 100;

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
            System.exit(1);
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
        options.addOption("m", "rehydrate", false,
                "Read CSV (as if from --dry-run) to find which migrations to make and run them");

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
            return;
        }

        if (line.hasOption('h') || 0 == line.getOptions().length) {
            helpFormatter.printHelp("dspace doi-migrate", null, options, null, true);
            return;
        }

        Stream<DOIMigration> doiMigrations;
        if ((line.hasOption("to") && line.hasOption("from"))
                && (line.hasOption("only-prefix") ^ line.hasOption("except-prefix"))
                && !line.hasOption("rehydrate")) {
            Function<String, Boolean> shouldMigrateDOI;
            if (line.hasOption("only-prefix")) {
                String prefix = line.getOptionValue("only-prefix");
                shouldMigrateDOI = (doi) -> doi.startsWith(prefix + '/');
            } else {
                String prefix = line.getOptionValue("except-prefix");
                shouldMigrateDOI = (doi) -> !doi.startsWith(prefix + '/');
            }

            MetadataFieldName fromFieldName = new MetadataFieldName(line.getOptionValue("from"));
            Stream<Item> items;
            try {
                items = migrationItems(fromFieldName);
            } catch (Exception e) {
                LOG.fatal(e);
                System.exit(1);
                return;
            }
            doiMigrations = itemsToDOIMigrations(new MetadataFieldName(line.getOptionValue("from")),
                    new MetadataFieldName(line.getOptionValue("to")),
                    shouldMigrateDOI,
                    items);
        } else if (line.hasOption("rehydrate")
                && !(line.hasOption("only-prefix")
                        || line.hasOption("except-prefix")
                        || line.hasOption("to")
                        || line.hasOption("from"))) {
            Reader stdinReader = new InputStreamReader(System.in);
            doiMigrations = csvToDOIMigrations(stdinReader);
        } else {
            System.err.println("You must have one of either: "
                    + "(1) --to and --from, plus either --only-prefix or --except-prefix, or "
                    + "(2) --rehydrate");
            helpFormatter.printHelp("dspace doi-migrate", null, options, null, true);
            System.exit(1);
            return;
        }

        if (line.hasOption("dry-run")) {
            Writer stdoutWriter = new OutputStreamWriter(System.out);
            doiMigrationsToCSV(stdoutWriter, doiMigrations);
        } else {
            runDOIMigrations(doiMigrations);
        }
    }

    public Stream<Item> migrationItems(MetadataFieldName fromFieldName)
        throws SQLException, IOException, AuthorizeException {
        Iterator<Item> itemsIterator = itemService.findByMetadataField(context,
                fromFieldName.schema,
                fromFieldName.element,
                fromFieldName.qualifier,
                Item.ANY);
        // so:24511052
        Stream<Item> items = StreamSupport.stream(((Iterable<Item>) () -> itemsIterator).spliterator(), false);

        return items;
    }

    public Stream<DOIMigration> itemsToDOIMigrations(MetadataFieldName fromFieldName,
            MetadataFieldName toFieldName,
            Function<String, Boolean> shouldMigrateDOI,
            Stream<Item> items) {
        return items.flatMap((Item item) -> {
            Stream.Builder<DOIMigration> s = Stream.builder();
            List<MetadataValue> metadata = itemService.getMetadata(item,
                    fromFieldName.schema,
                    fromFieldName.element,
                    fromFieldName.qualifier,
                    Item.ANY);

            for (MetadataValue val : metadata) {
                Matcher m = looksLikeDOI.matcher(val.getValue());
                if (m.matches()) {
                    String doi = m.group(1);
                    if (shouldMigrateDOI.apply(doi)) {
                        s.add(new DOIMigration(item,
                                val,
                                toFieldName,
                                "https://doi.org/" + doi));
                    }
                }
            }

            return s.build();
        });
    }

    class CSVMigrationsFormatError extends RuntimeException {
    }

    public Stream<DOIMigration> csvToDOIMigrations(Reader input) {
        // there is no real resource leak here, because in practice
        // input is always stdin (from a pipe); it’s no danger to
        // leave it open
        CSVReader csvReader = new CSVReader(input);
        Stream<String[]> rows = StreamSupport.stream(csvReader.spliterator(), false);
        return rows.skip(1).map((String[] row) -> {
            if (row.length != csvColumns.length) {
                throw new CSVMigrationsFormatError();
            }
            Item item;
            try {
                item = itemService.find(context, UUID.fromString(row[0]));
            } catch (SQLException e) {
                LOG.warn("No item found with UUID {}, skipping", row[0]);
                return null;
            }
            MetadataFieldName fromFieldName = new MetadataFieldName(row[1]);
            MetadataValue val = null;
            List<MetadataValue> vals = itemService.getMetadata(item,
                    fromFieldName.schema,
                    fromFieldName.element,
                    fromFieldName.qualifier,
                    Item.ANY);
            for (MetadataValue maybeVal : vals) {
                if (maybeVal.getValue().equals(row[2])) {
                    val = maybeVal;
                    break;
                }
            }
            if (val == null) {
                LOG.warn("No metadata {} with value {} found on item {}, skipping", fromFieldName.toString(),
                        row[2], row[0]);
                return null;
            }

            return new DOIMigration(item, val, new MetadataFieldName(row[3]), row[4]);
        }).filter((x) -> x != null);
    }

    void doiMigrationsToCSV(Writer output, Stream<DOIMigration> doiMigrations)
            throws IOException {
        CSVWriter csvWriter = new CSVWriter(output);
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
    }

    void runDOIMigrations(Stream<DOIMigration> doiMigrations) {
        // dear javac: assignment conversion, have you heard of it??
        AtomicInteger nth = new AtomicInteger(0);
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
                    throw new RuntimeException(e);
                }
            }
            metadataValue.setValue(doiMigration.toValue);
            if (nth.incrementAndGet() == batchSize) {
                nth.set(0);
                try {
                    context.commit();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
}
