/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.ProcessRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.ProcessStatus;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.scripts.Process;
import org.dspace.scripts.ProcessQueryParameterContainer;
import org.dspace.scripts.Process_;
import org.dspace.scripts.service.ProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * The repository for the Process workload
 */
@Component(ProcessRest.CATEGORY + "." + ProcessRest.PLURAL_NAME)
public class ProcessRestRepository extends DSpaceRestRepository<ProcessRest, Integer> {

    private static final Logger log = LogManager.getLogger();

    @Autowired
    private ProcessService processService;

    @Autowired
    private ConverterService converterService;


    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private EPersonService epersonService;


    @Override
    @PreAuthorize("hasPermission(#id, 'PROCESS', 'READ')")
    public ProcessRest findOne(Context context, Integer id) {
        try {
            Process process = processService.find(context, id);
            if (process == null) {
                return null;
            }
            return converter.toRest(process, utils.obtainProjection());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public Page<ProcessRest> findAll(Context context, Pageable pageable) {
        try {
            int total = processService.countTotal(context);
            List<Process> processes = processService.findAll(context, pageable.getPageSize(),
                    Math.toIntExact(pageable.getOffset()));
            return converter.toRestPage(processes, pageable, total, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @SearchRestMethod(name = "own")
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    public Page<ProcessRest> findByCurrentUser(Pageable pageable) {

        try {
            Context context = obtainContext();
            long total = processService.countByUser(context, context.getCurrentUser());
            List<Process> processes = processService.findByUser(context, context.getCurrentUser(),
                                                                pageable.getPageSize(),
                                                                Math.toIntExact(pageable.getOffset()));
            return converter.toRestPage(processes, pageable, total, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Calls on the getBitstreams method to retrieve all the Bitstreams of this process
     * @param processId The processId of the Process to retrieve the Bitstreams for
     * @return          The list of Bitstreams of the given Process
     * @throws SQLException If something goes wrong
     * @throws AuthorizeException If something goes wrong
     */
    public List<BitstreamRest> getProcessBitstreams(Integer processId) throws SQLException, AuthorizeException {
        Context context = obtainContext();
        Process process = getProcess(processId, context);
        List<Bitstream> bitstreams = processService.getBitstreams(context, process);
        return bitstreams.stream()
                  .map(bitstream -> (BitstreamRest) converterService.toRest(bitstream, Projection.DEFAULT))
                  .collect(Collectors.toList());
    }

    private Process getProcess(Integer processId, Context context) throws SQLException, AuthorizeException {
        Process process = processService.find(context, processId);
        if (process == null) {
            throw new ResourceNotFoundException("Process with id " + processId + " was not found");
        }
        if ((context.getCurrentUser() == null) || (!context.getCurrentUser()
                                                           .equals(process.getEPerson()) && !authorizeService
            .isAdmin(context))) {
            throw new AuthorizeException("The current user is not eligible to view the process with id: " + processId);
        }
        return process;
    }

    /**
     * Retrieves the Bitstream in the given Process of a given type
     * @param processId The processId of the Process to be used
     * @param type      The type of bitstreams to be returned, if null it'll return all the bitstreams
     * @return          The bitstream for the given parameters
     * @throws SQLException If something goes wrong
     * @throws AuthorizeException If something goes wrong
     */
    public BitstreamRest getProcessBitstreamByType(Integer processId, String type)
        throws SQLException, AuthorizeException {
        Context context = obtainContext();
        Process process = getProcess(processId, context);
        Bitstream bitstream = processService.getBitstream(context, process, type);

        return converterService.toRest(bitstream, utils.obtainProjection());
    }

    @Override
    protected void delete(Context context, Integer integer)
        throws AuthorizeException, RepositoryMethodNotImplementedException {
        try {
            processService.delete(context, processService.find(context, integer));
        } catch (SQLException | IOException e) {
            log.error("Something went wrong trying to find Process with id: " + integer, e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Search method that will take Parameters and return a list of {@link ProcessRest} objects
     * based on the {@link Process} objects that were in the databank that adhere to these params
     * @param ePersonUuid   The UUID for the EPerson that started the Process
     * @param scriptName    The name of the Script for which the Process belongs to
     * @param processStatusString   The status of the Process
     * @param pageable      The pageable
     * @return              A page of {@link ProcessRest} objects adhering to the params
     * @throws SQLException If something goes wrong
     */
    @SearchRestMethod(name = "byProperty")
    @PreAuthorize("hasAuthority('ADMIN')")
    public Page<ProcessRest> findProcessesByProperty(@Parameter(value = "userId") UUID ePersonUuid,
                                                     @Parameter(value = "scriptName") String scriptName,
                                                     @Parameter(value = "processStatus") String processStatusString,
                                                     Pageable pageable)
        throws SQLException {
        if (StringUtils.isBlank(scriptName) && ePersonUuid == null && StringUtils.isBlank(processStatusString)) {
            throw new DSpaceBadRequestException("Either a name, user UUID or ProcessStatus should be provided");
        }

        Context context = obtainContext();
        EPerson ePerson = null;
        if (ePersonUuid != null) {
            ePerson = epersonService.find(context, ePersonUuid);
            if (ePerson == null) {
                throw new DSpaceBadRequestException("No EPerson with the given UUID is found");
            }
        }

        ProcessStatus processStatus = StringUtils.isBlank(processStatusString) ? null :
            ProcessStatus.valueOf(processStatusString);
        ProcessQueryParameterContainer processQueryParameterContainer = createProcessQueryParameterContainer(scriptName,
                                                                            ePerson, processStatus);
        handleSearchSort(pageable, processQueryParameterContainer);
        List<Process> processes = processService.search(context, processQueryParameterContainer, pageable.getPageSize(),
                                                        Math.toIntExact(pageable.getOffset()));
        return converterService.toRestPage(processes, pageable,
                                           processService.countSearch(context, processQueryParameterContainer),
                                           utils.obtainProjection());


    }

    /**
     * This method will retrieve the {@link Sort} from the given {@link Pageable} and it'll create the sortOrder and
     * sortProperty Strings on the {@link ProcessQueryParameterContainer} object so that we can store how the sorting
     * should be done
     * @param pageable                          The pageable object
     * @param processQueryParameterContainer    The object in which the sorting will be filled in
     */
    private void handleSearchSort(Pageable pageable, ProcessQueryParameterContainer processQueryParameterContainer) {
        Sort sort = pageable.getSort();
        if (sort != null) {
            Iterator<Sort.Order> iterator = sort.iterator();
            if (iterator.hasNext()) {
                Sort.Order order = iterator.next();
                if (StringUtils.equalsIgnoreCase(order.getProperty(), "startTime")) {
                    processQueryParameterContainer.setSortProperty(Process_.START_TIME);
                    processQueryParameterContainer.setSortOrder(order.getDirection().name());
                } else if (StringUtils.equalsIgnoreCase(order.getProperty(), "endTime")) {
                    processQueryParameterContainer.setSortProperty(Process_.FINISHED_TIME);
                    processQueryParameterContainer.setSortOrder(order.getDirection().name());
                } else if (StringUtils.equalsIgnoreCase(order.getProperty(), "creationTime")) {
                    processQueryParameterContainer.setSortProperty(Process_.CREATION_TIME);
                    processQueryParameterContainer.setSortOrder(order.getDirection().name());
                } else {
                    throw new DSpaceBadRequestException("The given sort option was invalid: " + order.getProperty());
                }
                if (iterator.hasNext()) {
                    throw new DSpaceBadRequestException("Only one sort method is supported, can't give multiples");
                }
            }
        }
    }

    /**
     * This method will create a new {@link ProcessQueryParameterContainer} object and return it.
     * This object will contain a map which is filled in with the database column reference as key and the value that
     * it should contain when searching as the value of the entry
     * @param scriptName    The name that the script of the process should have
     * @param ePerson       The eperson that the process should have
     * @param processStatus The status that the process should have
     * @return              The newly created {@link ProcessQueryParameterContainer}
     */
    private ProcessQueryParameterContainer createProcessQueryParameterContainer(String scriptName, EPerson ePerson,
                                                                                ProcessStatus processStatus) {
        ProcessQueryParameterContainer processQueryParameterContainer =
            new ProcessQueryParameterContainer();
        if (StringUtils.isNotBlank(scriptName)) {
            processQueryParameterContainer.addToQueryParameterMap(Process_.NAME, scriptName);
        }
        if (ePerson != null) {
            processQueryParameterContainer.addToQueryParameterMap(Process_.E_PERSON, ePerson);
        }
        if (processStatus != null) {
            processQueryParameterContainer.addToQueryParameterMap(Process_.PROCESS_STATUS, processStatus);
        }
        return processQueryParameterContainer;
    }

    @Override
    public Class<ProcessRest> getDomainClass() {
        return ProcessRest.class;
    }
}
