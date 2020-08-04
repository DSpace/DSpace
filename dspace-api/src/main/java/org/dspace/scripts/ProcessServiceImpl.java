/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.scripts;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.ProcessStatus;
import org.dspace.content.dao.ProcessDAO;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.scripts.service.ProcessService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The implementation for the {@link ProcessService} class
 */
public class ProcessServiceImpl implements ProcessService {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(ProcessService.class);

    @Autowired
    private ProcessDAO processDAO;

    @Autowired
    private BitstreamService bitstreamService;

    @Autowired
    private BitstreamFormatService bitstreamFormatService;

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private MetadataFieldService metadataFieldService;

    @Override
    public Process create(Context context, EPerson ePerson, String scriptName,
                          List<DSpaceCommandLineParameter> parameters) throws SQLException {

        Process process = new Process();
        process.setEPerson(ePerson);
        process.setName(scriptName);
        process.setParameters(DSpaceCommandLineParameter.concatenate(parameters));
        process.setCreationTime(new Date());
        Process createdProcess = processDAO.create(context, process);
        log.info(LogManager.getHeader(context, "process_create",
                                      "Process has been created for eperson with email " + ePerson.getEmail()
                                          + " with ID " + createdProcess.getID() + " and scriptName " +
                                          scriptName + " and parameters " + parameters));
        return createdProcess;
    }

    @Override
    public Process find(Context context, int processId) throws SQLException {
        return processDAO.findByID(context, Process.class, processId);
    }

    @Override
    public List<Process> findAll(Context context) throws SQLException {
        return processDAO.findAll(context, Process.class);
    }

    @Override
    public List<Process> findAll(Context context, int limit, int offset) throws SQLException {
        return processDAO.findAll(context, limit, offset);
    }

    @Override
    public List<Process> findAllSortByScript(Context context) throws SQLException {
        return processDAO.findAllSortByScript(context);
    }

    @Override
    public List<Process> findAllSortByStartTime(Context context) throws SQLException {
        List<Process> processes = findAll(context);
        Comparator<Process> comparing = Comparator
            .comparing(Process::getStartTime, Comparator.nullsLast(Comparator.naturalOrder()));
        comparing = comparing.thenComparing(Process::getID);
        processes.sort(comparing);
        return processes;
    }

    @Override
    public void start(Context context, Process process) throws SQLException {
        process.setProcessStatus(ProcessStatus.RUNNING);
        process.setStartTime(new Date());
        update(context, process);
        log.info(LogManager.getHeader(context, "process_start", "Process with ID " + process.getID()
            + " and name " + process.getName() + " has started"));

    }

    @Override
    public void fail(Context context, Process process) throws SQLException {
        process.setProcessStatus(ProcessStatus.FAILED);
        process.setFinishedTime(new Date());
        update(context, process);
        log.info(LogManager.getHeader(context, "process_fail", "Process with ID " + process.getID()
            + " and name " + process.getName() + " has failed"));

    }

    @Override
    public void complete(Context context, Process process) throws SQLException {
        process.setProcessStatus(ProcessStatus.COMPLETED);
        process.setFinishedTime(new Date());
        update(context, process);
        log.info(LogManager.getHeader(context, "process_complete", "Process with ID " + process.getID()
            + " and name " + process.getName() + " has been completed"));

    }

    @Override
    public void appendFile(Context context, Process process, InputStream is, String type, String fileName)
        throws IOException, SQLException, AuthorizeException {
        Bitstream bitstream = bitstreamService.create(context, is);
        if (getBitstream(context, process, type) != null) {
            throw new IllegalArgumentException("Cannot create another file of type: " + type + " for this process" +
                                                   " with id: " + process.getID());
        }
        bitstream.setName(context, fileName);
        bitstreamService.setFormat(context, bitstream, bitstreamFormatService.guessFormat(context, bitstream));
        MetadataField dspaceProcessFileTypeField = metadataFieldService
            .findByString(context, Process.BITSTREAM_TYPE_METADATAFIELD, '.');
        bitstreamService.addMetadata(context, bitstream, dspaceProcessFileTypeField, null, type);
        authorizeService.addPolicy(context, bitstream, Constants.READ, context.getCurrentUser());
        authorizeService.addPolicy(context, bitstream, Constants.WRITE, context.getCurrentUser());
        authorizeService.addPolicy(context, bitstream, Constants.DELETE, context.getCurrentUser());
        bitstreamService.update(context, bitstream);
        process.addBitstream(bitstream);
        update(context, process);
    }

    @Override
    public void delete(Context context, Process process) throws SQLException, IOException, AuthorizeException {

        for (Bitstream bitstream : ListUtils.emptyIfNull(process.getBitstreams())) {
            bitstreamService.delete(context, bitstream);
        }
        processDAO.delete(context, process);
        log.info(LogManager.getHeader(context, "process_delete", "Process with ID " + process.getID()
            + " and name " + process.getName() + " has been deleted"));
    }

    @Override
    public void update(Context context, Process process) throws SQLException {
        processDAO.save(context, process);
    }

    @Override
    public List<DSpaceCommandLineParameter> getParameters(Process process) {
        if (StringUtils.isBlank(process.getParameters())) {
            return Collections.emptyList();
        }

        String[] parameterArray = process.getParameters().split(Pattern.quote(DSpaceCommandLineParameter.SEPARATOR));
        List<DSpaceCommandLineParameter> parameterList = new ArrayList<>();

        for (String parameter : parameterArray) {
            parameterList.add(new DSpaceCommandLineParameter(parameter));
        }

        return parameterList;
    }

    @Override
    public Bitstream getBitstreamByName(Context context, Process process, String bitstreamName) {
        for (Bitstream bitstream : getBitstreams(context, process)) {
            if (StringUtils.equals(bitstream.getName(), bitstreamName)) {
                return bitstream;
            }
        }

        return null;
    }

    @Override
    public Bitstream getBitstream(Context context, Process process, String type) {
        List<Bitstream> allBitstreams = process.getBitstreams();

        if (type == null) {
            return null;
        } else {
            if (allBitstreams != null) {
                for (Bitstream bitstream : allBitstreams) {
                    if (StringUtils.equals(bitstreamService.getMetadata(bitstream,
                                                                        Process.BITSTREAM_TYPE_METADATAFIELD), type)) {
                        return bitstream;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public List<Bitstream> getBitstreams(Context context, Process process) {
        return process.getBitstreams();
    }

    public int countTotal(Context context) throws SQLException {
        return processDAO.countRows(context);
    }

    @Override
    public List<String> getFileTypesForProcessBitstreams(Context context, Process process) {
        List<Bitstream> list = getBitstreams(context, process);
        Set<String> fileTypesSet = new HashSet<>();
        for (Bitstream bitstream : list) {
            List<MetadataValue> metadata = bitstreamService.getMetadata(bitstream,
                                                                        Process.BITSTREAM_TYPE_METADATAFIELD, Item.ANY);
            if (metadata != null && !metadata.isEmpty()) {
                fileTypesSet.add(metadata.get(0).getValue());
            }
        }
        return new ArrayList<>(fileTypesSet);
    }

}
