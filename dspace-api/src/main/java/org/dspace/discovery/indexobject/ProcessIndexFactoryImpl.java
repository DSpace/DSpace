/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.indexobject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.dspace.scripts.Process;
import org.dspace.scripts.service.ProcessService;
import org.dspace.services.ConfigurationService;
import org.dspace.util.SolrUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation for indexing {@link Process} entities in Discovery.
 */
public class ProcessIndexFactoryImpl extends IndexFactoryImpl<IndexableProcess, Process> {

    public static final String PROCESS_ID = "process_id";
    public static final String PROCESS_NAME = "process_name";
    public static final String PROCESS_USER_NAME = "process_user_name";
    public static final String PROCESS_USER_EMAIL = "process_user_email";
    public static final String PROCESS_START_TIME = "process_start_time";
    public static final String PROCESS_FINISH_TIME = "process_finish_time";
    public static final String PROCESS_CREATION_TIME = "process_creation_time";
    public static final String PROCESS_STATUS = "process_status";
    public static final String PROCESS_ARGUMENTS = "process_arguments";
    public static final String PROCESS_OUTPUT_FILE = "process_output_file";
    public static final String PROCESS_SIZE_BYTES = "process_size_bytes";
    public static final String PROCESS_LOG = "process_log";

    @Autowired(required = true)
    private ProcessService processService;

    @Autowired(required = true)
    private BitstreamService bitstreamService;

    @Autowired(required = true)
    private ConfigurationService configurationService;

    @Override
    public Iterator<IndexableProcess> findAll(Context context) throws SQLException {
        return processService.findAll(context).stream()
                             .map(IndexableProcess::new)
                             .iterator();
    }

    @Override
    public String getType() {
        return IndexableProcess.TYPE;
    }

    @Override
    public Optional<IndexableProcess> findIndexableObject(Context context, String id) throws SQLException {
        Process process = processService.find(context, Integer.parseInt(id));
        return process == null ? Optional.empty() : Optional.of(new IndexableProcess(process));
    }

    @Override
    public boolean supports(Object object) {
        return object instanceof Process;
    }

    @Override
    public List<IndexableProcess> getIndexableObjects(Context context, Process object) {
        return List.of(new IndexableProcess(object));
    }

    @Override
    public SolrInputDocument buildDocument(Context context, IndexableProcess indexableObject)
        throws SQLException, IOException {
        SolrInputDocument doc = super.buildDocument(context, indexableObject);
        Process process = indexableObject.getIndexedObject();

        doc.addField(PROCESS_ID, process.getID());
        doc.addField(PROCESS_ID + "_i", process.getID());

        addTextAndFacet(doc, PROCESS_NAME, process.getName(), process.getName());
        indexUser(doc, process.getEPerson());
        indexDateField(doc, PROCESS_START_TIME, process.getStartTime());
        indexDateField(doc, PROCESS_FINISH_TIME, process.getFinishedTime());
        indexDateField(doc, PROCESS_CREATION_TIME, process.getCreationTime());

        if (process.getProcessStatus() != null) {
            String status = process.getProcessStatus().name();
            addTextAndFacet(doc, PROCESS_STATUS, status, status);
        }

        String arguments = processService.getParameters(process).stream()
                                         .map(DSpaceCommandLineParameter::toString)
                                         .collect(Collectors.joining(" "));
        if (StringUtils.isNotBlank(arguments)) {
            doc.addField(PROCESS_ARGUMENTS, arguments);
        }

        List<Bitstream> processFiles = ListUtils.emptyIfNull(processService.getBitstreams(context, process));
        long processSize = processFiles.stream().mapToLong(Bitstream::getSizeBytes).sum();
        doc.addField(PROCESS_SIZE_BYTES, processSize);
        doc.addField(PROCESS_SIZE_BYTES + "_l", processSize);
        doc.addField(PROCESS_SIZE_BYTES + "_sort", String.format(Locale.ROOT, "%020d", processSize));

        Bitstream outputFile = processService.getBitstream(context, process, Process.OUTPUT_TYPE);
        if (outputFile != null) {
            indexOutputFile(context, doc, outputFile);
        }

        return doc;
    }

    private void indexUser(SolrInputDocument doc, EPerson ePerson) {
        if (ePerson == null) {
            return;
        }

        String authority = ePerson.getID().toString();
        addTextAndFacet(doc, PROCESS_USER_NAME, authority, ePerson.getFullName());
        addTextAndFacet(doc, PROCESS_USER_EMAIL, authority, ePerson.getEmail());
    }

    private void addTextAndFacet(SolrInputDocument doc, String field, String authority, String value) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        doc.addField(field, value);
        addFacetIndex(doc, field, authority, value);
    }

    private void indexDateField(SolrInputDocument doc, String field, Instant instant) {
        if (instant == null) {
            return;
        }

        String date = DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDate.ofInstant(instant, ZoneOffset.UTC));
        addFacetIndex(doc, field, date, date);
        doc.addField(field, date);
        doc.addField(field + "_dt", SolrUtils.getDateFormatter().format(instant));
        doc.addField(field + "_min", date);
        doc.addField(field + "_min_sort", date);
        doc.addField(field + "_max", date);
        doc.addField(field + "_max_sort", date);
        doc.addField(field + ".year", instant.atZone(ZoneOffset.UTC).getYear());
        doc.addField(field + ".year_sort", instant.atZone(ZoneOffset.UTC).getYear());
    }

    private void indexOutputFile(Context context, SolrInputDocument doc, Bitstream outputFile)
        throws IOException, SQLException {
        String outputFileId = outputFile.getID().toString();
        String outputFileName = outputFile.getName();
        addTextAndFacet(doc, PROCESS_OUTPUT_FILE, outputFileId, outputFileName);
        doc.addField(PROCESS_OUTPUT_FILE + "_id", outputFileId);

        if (configurationService.getBooleanProperty("process.index.log", false)
            && Strings.CI.endsWith(outputFileName, ".log")) {
            try (InputStream inputStream = bitstreamService.retrieve(context, outputFile)) {
                doc.addField(PROCESS_LOG, IOUtils.toString(inputStream, StandardCharsets.UTF_8));
            } catch (AuthorizeException e) {
                throw new IOException("Unable to read the process log for indexing", e);
            }
        }
    }
}
