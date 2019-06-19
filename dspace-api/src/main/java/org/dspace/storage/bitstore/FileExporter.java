package org.dspace.storage.bitstore;

import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.Context;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.function.BiPredicate;
import java.util.function.Function;

public class FileExporter {
    private static Function<Item, LocalDate> extractDateAvailable = (item) -> Arrays.stream(item.getMetadata("dc", "date", "available", null))
            .findAny()
            .map(metadatum -> metadatum.value)
            .map(value -> LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")).toLocalDate())
            .orElse(LocalDate.now());
    private static BiPredicate<LocalDate, Pair<LocalDate, LocalDate>> isDateInRange = (date, range) -> date.isAfter(range.getLeft().minusDays(1)) && date.isBefore(range.getRight().plusDays(1));

    public static void main(String[] args) throws SQLException {
        Context context = new Context();
        context.turnOffAuthorisationSystem();
        CommandLine line = null;
        String usage = "org.dspace.storage.bitstore.FileExporter -o <output directory>";

        HelpFormatter formatter = new HelpFormatter();

        Options options = new Options();
        options.addOption(Option.builder("o")
                .required()
                .argName("output directory")
                .hasArg(true)
                .desc("Output directory for files")
                .build());


        options.addOption(Option.builder("f")
                .argName("start date")
                .hasArg(true)
                .desc("Start date")
                .build());

        options.addOption(Option.builder("t")
                .argName("end date")
                .hasArg(true)
                .desc("End date")
                .build());


        try {
            line = new PosixParser().parse(options, args);
        } catch (Exception e) {
            formatter.printHelp(usage, e.getMessage(), options, "");
            System.exit(1);
        }


        LocalDate from = LocalDate.parse(line.getOptionValue("f", LocalDate.MIN.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))), DateTimeFormatter.ofPattern("dd.MM.yyyy")).minusDays(1L);
        LocalDate to = LocalDate.parse(line.getOptionValue("t", LocalDate.MAX.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))), DateTimeFormatter.ofPattern("dd.MM.yyyy")).plusDays(1L);
        String outputDirectory = line.getOptionValue("o");


        int totalProcessed = 0;
        ItemIterator all = Item.findAll(context);
        while (all.hasNext()) {
            Item item = all.next();
            LocalDate uploadDate = extractDateAvailable.apply(item);

            if (isDateInRange.test(uploadDate, Pair.of(from, to))) {
                System.out.printf("Processing item: %s uploaded at: %s.\n", item.getName(), uploadDate.toString());
                Arrays.stream(item.getBundles())
                        .flatMap(bundle -> Arrays.stream(bundle.getBitstreams()))
                        .filter(bitstream -> !bitstream.getFormat().isInternal())
                        .forEach(bitstream -> processBitstream(bitstream, outputDirectory));
                totalProcessed++;
                System.gc();
            }
        }

        System.out.printf("Processed %d files.\n", totalProcessed);
        System.out.println("Files has been written to " + outputDirectory);
    }

    private static void processBitstream(Bitstream bitstream, String folder) {
        System.out.printf("Processing file %s with id %d.\n", bitstream.getName(), bitstream.getID());
        File outputFile = null;
        try {
            outputFile = new File(folder + "/" + bitstream.getName());
            FileUtils.copyInputStreamToFile(bitstream.retrieve(), outputFile);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (AuthorizeException e) {
            e.printStackTrace();
        }

    }

}
