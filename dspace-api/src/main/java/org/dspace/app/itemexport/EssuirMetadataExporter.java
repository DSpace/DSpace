package org.dspace.app.itemexport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.apache.commons.cli.*;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.SpecialityDetailedInfo;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;


public class EssuirMetadataExporter {
    protected ItemService itemService;

    protected EssuirMetadataExporter() {
        itemService = ContentServiceFactory.getInstance().getItemService();
    }

    public static void main(String[] args) throws SQLException, IOException {
        CommandLineParser parser = new PosixParser();

        Options options = new Options();
        options.addOption("f", "file", true, "destination where you want file written (value like: out.csv)");

        CommandLine line = null;

        try {
            line = parser.parse(options, args);
        } catch (ParseException pe) {
            System.err.println("Error with commands.");

            System.exit(0);
        }

        if (!line.hasOption('f')) {
            System.err.println("Required parameter -f missing!");
            printHelp(options, 1);
        }

        String filename = line.getOptionValue('f');
        export(filename);
    }

    private static String fetchMetadataFieldValue(Item item, String element, String qualifier, String defaultValue) {
        return item.getItemService().getMetadata(item, MetadataSchema.DC_SCHEMA, element, qualifier, Item.ANY)
                .stream()
                .findFirst()
                .map(MetadataValue::getValue)
                .orElse(defaultValue);
    }

    private static ItemExportMetadata constructItemMetadata(Item item) throws JsonProcessingException {
        EPerson submitter = item.getSubmitter();
        String dateAvailable = fetchMetadataFieldValue(item, "date", "available", "Unknown date");
        String type = fetchMetadataFieldValue(item, "type", "*", "Unknown type");
        String speciality = fetchMetadataFieldValue(item, "speciality", "id", "[]");
        String presentationDate = fetchMetadataFieldValue(item, "date", "presentation", "");
        String publisher = fetchMetadataFieldValue(item, "publisher", null, "");
        List<SpecialityDetailedInfo> specialityDetailedInfoList = new ObjectMapper().readValue(speciality, new TypeReference<List<SpecialityDetailedInfo>>() {
        });

        ItemExportMetadata.Builder builder = new ItemExportMetadata.Builder()
                .withTitle(item.getName())
                .withHandle("https://essuir.sumdu.edu.ua/handle/" + item.getHandle())
                .withDateAvailable(dateAvailable)
                .withType(type)
                .withCollection(item.getOwningCollection().getName())
                .withSubmitterEmail(submitter.getEmail())
                .withSubmitterFirstName(submitter.getFirstName())
                .withSubmitterLastName(submitter.getLastName())
                .withPresentationDate(presentationDate)
                .withPublisher(publisher);

        if (specialityDetailedInfoList.size() == 3) {
            String facultyName = specialityDetailedInfoList.get(0).getName();
            String chairName = specialityDetailedInfoList.get(1).getName();
            String specialityName = specialityDetailedInfoList.get(2).getName();
            builder.withBachelorsPaperFaculty(facultyName)
                    .withBachelorsPaperChair(chairName)
                    .withBachelorsPaperSpeciality(specialityName);
        }

        if (submitter.getChair() != null) {
            builder.withChairName(String.format("%s (%d)", submitter.getChair().getName(), submitter.getChair().getId()));
            if (submitter.getChair().getFacultyEntity() != null) {
                builder.withFacultyName(submitter.getChair().getFacultyEntityName());
            }
        }
        return builder.build();
    }

    private static void export(String fileName) throws SQLException, IOException {
        Context context = new Context(Context.Mode.READ_ONLY);
        CsvSchema csvSchema = CsvSchema.builder()
                .setColumnSeparator(';')
                .addColumn("title")
                .addColumn("handle")
                .addColumn("collection")
                .addColumn("submitterEmail")
                .addColumn("submitterFirstName")
                .addColumn("submitterLastName")
                .addColumn("chairName")
                .addColumn("facultyName")
                .addColumn("dateAvailable")
                .addColumn("type")
                .addColumn("bachelorsPaperFaculty")
                .addColumn("bachelorsPaperChair")
                .addColumn("bachelorsPaperSpeciality")
                .addColumn("presentationDate")
                .addColumn("publisher")
                .build();
        CsvMapper csvMapper = new CsvMapper();

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), "Cp1251"));
        context.turnOffAuthorisationSystem();
        ContentServiceFactory contentServiceFactory = ContentServiceFactory.getInstance();
        ItemService itemService = contentServiceFactory.getItemService();
        Iterator<Item> items = itemService.findAll(context);
        writer.write("Paper Title;Handle;Collection;Submitter Email;Submitter First Name;Submitter Last Name;Chair Name;Faculty Name;Date Available;Type; Bachelors Paper Faculty; Bachelors Paper Chair; Bachelors Paper Speciality; Presentation Date; Publisher");
        writer.newLine();
        writer.flush();
        while (items.hasNext()) {
            Item item = items.next();
	    System.out.printf("%s -- %s\n", item.getName(), item.getHandle());
            String line = csvMapper.writerFor(ItemExportMetadata.class)
                    .with(csvSchema).writeValueAsString(constructItemMetadata(item));
            writer.write(line);
            writer.flush();
            context.uncacheEntity(item);
        }
        writer.close();
        context.restoreAuthSystemState();
        context.complete();
    }

    private static void printHelp(Options options, int exitCode) {
        HelpFormatter myhelp = new HelpFormatter();
        myhelp.printHelp("MetadataExport\n", options);
        System.out.println("\nfull export: metadataexport -f filename");
        System.out.println("partial export: metadataexport -i handle -f filename");
        System.exit(exitCode);
    }
}
