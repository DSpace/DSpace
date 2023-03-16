package edu.umd.lib.dspace.content;

import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.opencsv.CSVWriter;
import edu.umd.lib.dspace.content.factory.DrumServiceFactory;
import edu.umd.lib.dspace.content.service.EmbargoDTOService;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;
import org.dspace.core.Context;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.utils.DSpace;

/**
 * Exporter for the batch export of embargoed items
 */
public class EmbargoListExport extends DSpaceRunnable<EmbargoListExportScriptConfiguration<EmbargoListExport>> {
    private static final String EXPORT_CSV = "exportCSV";
    private boolean help = false;

    private EPersonService ePersonService;

    private EmbargoDTOService embargoService;

    @Override
    @SuppressWarnings("unchecked")
    public EmbargoListExportScriptConfiguration<EmbargoListExport> getScriptConfiguration() {
        return new DSpace().getServiceManager()
                .getServiceByName("embargo-list-export", EmbargoListExportScriptConfiguration.class);
    }

    @Override
    public void setup() throws ParseException {
        ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
        embargoService = DrumServiceFactory.getInstance().getEmbargoDTOService();

        if (commandLine.hasOption('h')) {
            help = true;
            return;
        }
    }

    @Override
    public void internalRun() throws Exception {
        if (help) {
            loghelpinfo();
            printHelp();
            return;
        }
        handler.logDebug("starting embargo-list-export");

        Context context = new Context();
        context.setCurrentUser(ePersonService.find(context, this.getEpersonIdentifier()));
        List<EmbargoDTO> embargoes = embargoService.getEmbargoList(context);

        StringWriter stringWriter = new StringWriter();
        try (CSVWriter writer = new CSVWriter(stringWriter)) {
            writer.writeNext(new String[] {
                "Handle", "Item ID", "Bitstream ID", "Title", "Advisor",
                "Author", "Department", "Type", "End Date"
            });

            for (EmbargoDTO embargo : embargoes) {
                String[] entryData = new String[9];

                entryData[0] = embargo.getHandle();
                entryData[1] = embargo.getItemIdString();
                entryData[2] = embargo.getBitstreamIdString();
                entryData[3] = embargo.getTitle();
                entryData[4] = embargo.getAdvisor();
                entryData[5] = embargo.getAuthor();
                entryData[6] = embargo.getDepartment();
                entryData[7] = embargo.getType();
                entryData[8] = embargo.getEndDateString();
                writer.writeNext(entryData);
            }
            writer.flush();
        }
        stringWriter.close();

        InputStream inputStream = IOUtils.toInputStream(stringWriter.getBuffer(), StandardCharsets.UTF_8);
        handler.logDebug("writing to file " + getFileNameOrExportFile());
        handler.writeFilestream(context, getFileNameOrExportFile(), inputStream, EXPORT_CSV);
        context.restoreAuthSystemState();
        context.complete();
    }

    protected void loghelpinfo() {
        handler.logInfo("embargo-list-export");
    }

    protected String getFileNameOrExportFile() {
        return "embargo-list.csv";
    }
}
