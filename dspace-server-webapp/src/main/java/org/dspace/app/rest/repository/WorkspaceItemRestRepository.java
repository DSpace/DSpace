/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import gr.ekt.bte.core.TransformationEngine;
import gr.ekt.bte.core.TransformationSpec;
import gr.ekt.bte.exceptions.BadTransformationSpec;
import gr.ekt.bte.exceptions.MalformedSourceException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.converter.WorkspaceItemConverter;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.RESTAuthorizationException;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.ErrorRest;
import org.dspace.app.rest.model.WorkspaceItemRest;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.repository.handler.service.UriListHandlerService;
import org.dspace.app.rest.submit.AbstractRestProcessingStep;
import org.dspace.app.rest.submit.SubmissionService;
import org.dspace.app.rest.submit.UploadableStep;
import org.dspace.app.rest.utils.Utils;
import org.dspace.app.util.SubmissionConfig;
import org.dspace.app.util.SubmissionConfigReader;
import org.dspace.app.util.SubmissionConfigReaderException;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.EPersonServiceImpl;
import org.dspace.event.Event;
import org.dspace.services.ConfigurationService;
import org.dspace.submit.AbstractProcessingStep;
import org.dspace.submit.lookup.DSpaceWorkspaceItemOutputGenerator;
import org.dspace.submit.lookup.MultipleSubmissionLookupDataLoader;
import org.dspace.submit.lookup.SubmissionItemDataLoader;
import org.dspace.submit.lookup.SubmissionLookupOutputGenerator;
import org.dspace.submit.lookup.SubmissionLookupService;
import org.dspace.submit.util.ItemSubmissionLookupDTO;
import org.dspace.util.UUIDUtils;
import org.dspace.versioning.ItemCorrectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.json.patch.PatchException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * This is the repository responsible to manage WorkspaceItem Rest object
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component(WorkspaceItemRest.CATEGORY + "." + WorkspaceItemRest.NAME)
public class WorkspaceItemRestRepository extends DSpaceRestRepository<WorkspaceItemRest, Integer> {

    public static final String OPERATION_PATH_SECTIONS = "sections";

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(WorkspaceItemRestRepository.class);

    @Autowired
    WorkspaceItemService wis;

    @Autowired
    ItemService itemService;

    @Autowired
    BitstreamService bitstreamService;

    @Autowired
    BitstreamFormatService bitstreamFormatService;

    @Autowired
    ConfigurationService configurationService;

    @Autowired
    ItemCorrectionService itemCorrectionService;

    @Autowired
    WorkspaceItemConverter workspaceItemConverter;

    @Autowired
    SubmissionService submissionService;

    @Autowired
    EPersonServiceImpl epersonService;

    @Autowired
    SubmissionLookupService submissionLookupService;

    @Autowired
    CollectionService collectionService;

    @Autowired
    private UriListHandlerService uriListHandlerService;

    private SubmissionConfigReader submissionConfigReader;

    public WorkspaceItemRestRepository() throws SubmissionConfigReaderException {
        submissionConfigReader = new SubmissionConfigReader();
    }

    //TODO @PreAuthorize("hasPermission(#id, 'WORKSPACEITEM', 'READ')")
    @Override
    public WorkspaceItemRest findOne(Context context, Integer id) {
        WorkspaceItem witem = null;
        try {
            witem = wis.find(context, id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (witem == null) {
            return null;
        }
        return converter.toRest(witem, utils.obtainProjection());
    }

    //TODO @PreAuthorize("hasAuthority('ADMIN')")
    @Override
    public Page<WorkspaceItemRest> findAll(Context context, Pageable pageable) {
        try {
            long total = wis.countTotal(context);
            List<WorkspaceItem> witems = wis.findAll(context, pageable.getPageSize(),
                    Math.toIntExact(pageable.getOffset()));
            return converter.toRestPage(witems, pageable, total, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    //TODO @PreAuthorize("hasPermission(#submitterID, 'EPERSON', 'READ')")
    @SearchRestMethod(name = "findBySubmitter")
    public Page<WorkspaceItemRest> findBySubmitter(@Parameter(value = "uuid", required = true) UUID submitterID,
            Pageable pageable) {
        try {
            Context context = obtainContext();
            EPerson ep = epersonService.find(context, submitterID);
            long total = wis.countByEPerson(context, ep);
            List<WorkspaceItem> witems = wis.findByEPerson(context, ep, pageable.getPageSize(),
                    Math.toIntExact(pageable.getOffset()));
            return converter.toRestPage(witems, pageable, total, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    protected WorkspaceItemRest createAndReturn(Context context) throws SQLException, AuthorizeException {
        WorkspaceItem source;
        String itemUUID = getRequestService().getCurrentRequest().getHttpServletRequest().getParameter("item");
        String relationship = getRequestService().getCurrentRequest().getHttpServletRequest()
                .getParameter("relationship");

        if ((StringUtils.isNotBlank(itemUUID) && StringUtils.isNotBlank(relationship))
                || StringUtils.isNotBlank(relationship)) {
            try {
                source = itemCorrectionService.createWorkspaceItemAndRelationshipByItem(context,
                        getRequestService().getCurrentRequest(), UUIDUtils.fromString(itemUUID), relationship);
            } catch (AuthorizeException e) {
                throw new RESTAuthorizationException(e);
            } catch (Exception e) {
                throw new UnprocessableEntityException(e.getMessage());
            }
        } else if (StringUtils.isNotBlank(itemUUID)) {
            try {
                source = itemCorrectionService.createWorkspaceItemByItem(context,
                        getRequestService().getCurrentRequest(), UUIDUtils.fromString(itemUUID));
            } catch (Exception e) {
                throw new UnprocessableEntityException(e.getMessage());
            }
        } else if (StringUtils.isNotBlank(relationship)) {
            throw new UnprocessableEntityException("Cannot create a relationship without a given item");
        } else {
            source = submissionService.createWorkspaceItem(context, getRequestService().getCurrentRequest());
        }

        return converter.toRest(source, converter.getProjection("full"));
    }

    @Override
    protected WorkspaceItemRest save(Context context, WorkspaceItemRest wsi) {
        SubmissionConfig submissionConfig = submissionConfigReader
            .getSubmissionConfigByName(submissionConfigReader.getDefaultSubmissionConfigName());
        WorkspaceItem source = workspaceItemConverter.toModel(wsi);
        for (int stepNum = 0; stepNum < submissionConfig.getNumberOfSteps(); stepNum++) {

            SubmissionStepConfig stepConfig = submissionConfig.getStep(stepNum);
            /*
             * First, load the step processing class (using the current
             * class loader)
             */
            ClassLoader loader = this.getClass().getClassLoader();
            Class stepClass;
            try {
                stepClass = loader.loadClass(stepConfig.getProcessingClassName());

                Object stepInstance = stepClass.newInstance();

                if (stepInstance instanceof AbstractProcessingStep) {
                    // load the JSPStep interface for this step
                    AbstractProcessingStep stepProcessing = (AbstractProcessingStep) stepClass
                        .newInstance();
                    stepProcessing.doPreProcessing(context, source);
                } else {
                    throw new Exception("The submission step class specified by '"
                                            + stepConfig.getProcessingClassName()
                                            + "' does not extend the class org.dspace.submit.AbstractProcessingStep!"
                                            + " Therefore it cannot be used by the Configurable Submission as the " +
                                            "<processing-class>!");
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        submissionService.saveWorkspaceItem(context, source);
        return wsi;
    }

    @Override
    public Class<WorkspaceItemRest> getDomainClass() {
        return WorkspaceItemRest.class;
    }

    //TODO @PreAuthorize("hasPermission(#id, 'WORKSPACEITEM', 'WRITE')")
    @Override
    public WorkspaceItemRest upload(HttpServletRequest request, String apiCategory, String model, Integer id,
                                    MultipartFile file) throws Exception {

        Context context = obtainContext();
        WorkspaceItemRest wsi = findOne(context, id);
        WorkspaceItem source = wis.find(context, id);
        List<ErrorRest> errors = new ArrayList<ErrorRest>();
        SubmissionConfig submissionConfig =
            submissionConfigReader.getSubmissionConfigByName(wsi.getSubmissionDefinition().getName());
        for (int i = 0; i < submissionConfig.getNumberOfSteps(); i++) {
            SubmissionStepConfig stepConfig = submissionConfig.getStep(i);

            /*
             * First, load the step processing class (using the current
             * class loader)
             */
            ClassLoader loader = this.getClass().getClassLoader();
            Class stepClass;
            try {
                stepClass = loader.loadClass(stepConfig.getProcessingClassName());

                Object stepInstance = stepClass.newInstance();
                if (UploadableStep.class.isAssignableFrom(stepClass)) {
                    UploadableStep uploadableStep = (UploadableStep) stepInstance;
                    uploadableStep.doPreProcessing(context, source);
                    ErrorRest err =
                        uploadableStep.upload(context, submissionService, stepConfig, source, file);
                    uploadableStep.doPostProcessing(context, source);
                    if (err != null) {
                        errors.add(err);
                    }
                }

            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }

        }
        wsi = converter.toRest(source, Projection.DEFAULT);

        if (!errors.isEmpty()) {
            wsi.getErrors().addAll(errors);
        }

        context.commit();
        return wsi;
    }

    //TODO @PreAuthorize("hasPermission(#id, 'WORKSPACEITEM', 'WRITE')")
    @Override
    public void patch(Context context, HttpServletRequest request, String apiCategory, String model, Integer id,
                      Patch patch) throws SQLException, AuthorizeException {
        List<Operation> operations = patch.getOperations();
        WorkspaceItemRest wsi = findOne(context, id);
        WorkspaceItem source = wis.find(context, id);
        for (Operation op : operations) {
            //the value in the position 0 is a null value
            String[] path = op.getPath().substring(1).split("/", 3);
            if (OPERATION_PATH_SECTIONS.equals(path[0])) {
                String section = path[1];
                evaluatePatch(context, request, source, wsi, section, op);
            } else {
                throw new DSpaceBadRequestException(
                    "Patch path operation need to starts with '" + OPERATION_PATH_SECTIONS + "'");
            }
        }
        wis.update(context, source);
    }

    private void evaluatePatch(Context context, HttpServletRequest request, WorkspaceItem source, WorkspaceItemRest wsi,
                               String section, Operation op) {
        SubmissionConfig submissionConfig = submissionConfigReader
            .getSubmissionConfigByName(wsi.getSubmissionDefinition().getName());
        for (int stepNum = 0; stepNum < submissionConfig.getNumberOfSteps(); stepNum++) {

            SubmissionStepConfig stepConfig = submissionConfig.getStep(stepNum);

            if (section.equals(stepConfig.getId())) {
                /*
                 * First, load the step processing class (using the current
                 * class loader)
                 */
                ClassLoader loader = this.getClass().getClassLoader();
                Class stepClass;
                try {
                    stepClass = loader.loadClass(stepConfig.getProcessingClassName());

                    Object stepInstance = stepClass.newInstance();

                    if (stepInstance instanceof AbstractRestProcessingStep) {
                        // load the JSPStep interface for this step
                        AbstractRestProcessingStep stepProcessing =
                            (AbstractRestProcessingStep) stepClass.newInstance();
                        stepProcessing.doPreProcessing(context, source);
                        stepProcessing.doPatchProcessing(context, getRequestService().getCurrentRequest(), source, op);
                        stepProcessing.doPostProcessing(context, source);
                    } else {
                        throw new DSpaceBadRequestException(
                            "The submission step class specified by '" + stepConfig.getProcessingClassName() +
                            "' does not extend the class org.dspace.submit.AbstractProcessingStep!" +
                            " Therefore it cannot be used by the Configurable Submission as the <processing-class>!");
                    }

                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    throw new PatchException("Error processing the patch request", e);
                }
            }
        }
    }

    //TODO @PreAuthorize("hasPermission(#id, 'WORKSPACEITEM', 'DELETE')")
    @Override
    protected void delete(Context context, Integer id) throws AuthorizeException {
        WorkspaceItem witem = null;
        try {
            witem = wis.find(context, id);
            wis.deleteAll(context, witem);
            context.addEvent(new Event(Event.DELETE, Constants.ITEM, witem.getItem().getID(), null,
                itemService.getIdentifiers(context, witem.getItem())));
        } catch (SQLException | IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public Iterable<WorkspaceItemRest> upload(Context context, HttpServletRequest request,
            MultipartFile uploadfile)
        throws SQLException, FileNotFoundException, IOException, AuthorizeException {
        File file = Utils.getFile(uploadfile, "upload-loader", "filedataloader");
        List<WorkspaceItemRest> results = new ArrayList<>();

        try {
            String uuid = request.getParameter("collection");
            if (StringUtils.isBlank(uuid)) {
                uuid = configurationService.getProperty("submission.default.collection");
            }

            Collection collection = null;
            if (StringUtils.isNotBlank(uuid)) {
                collection = collectionService.find(context, UUID.fromString(uuid));
            } else {
                collection = collectionService.findAuthorizedOptimized(context, Constants.ADD).get(0);
            }

            SubmissionConfig submissionConfig =
                submissionConfigReader.getSubmissionConfigByCollection(collection.getHandle());


            List<ItemSubmissionLookupDTO> tmpResult = new ArrayList<ItemSubmissionLookupDTO>();

            TransformationEngine transformationEngine1 = submissionLookupService.getPhase1TransformationEngine();
            TransformationSpec spec = new TransformationSpec();
            // FIXME this is mostly due to the need to test. The BTE framework has an assert statement that check if the
            // number of found record is less than the requested and treat 0 as is, instead, the implementation assume
            // 0=unlimited this lead to test failure.
            // It is unclear if BTE really respect values other than 0/MAX allowing us to put a protection against heavy
            // load
            spec.setNumberOfRecords(Integer.MAX_VALUE);
            if (transformationEngine1 != null) {
                MultipleSubmissionLookupDataLoader dataLoader =
                    (MultipleSubmissionLookupDataLoader) transformationEngine1.getDataLoader();

                List<String> fileDataLoaders = submissionLookupService.getFileProviders();
                for (String fileDataLoader : fileDataLoaders) {
                    dataLoader.setFile(file.getAbsolutePath(), fileDataLoader);

                    try {
                        SubmissionLookupOutputGenerator outputGenerator =
                            (SubmissionLookupOutputGenerator) transformationEngine1.getOutputGenerator();
                        outputGenerator.setDtoList(new ArrayList<ItemSubmissionLookupDTO>());
                        log.debug("BTE transformation is about to start!");
                        transformationEngine1.transform(spec);
                        log.debug("BTE transformation finished!");
                        tmpResult.addAll(outputGenerator.getDtoList());
                        if (!tmpResult.isEmpty()) {
                            //exit with the results founded on the first data provided
                            break;
                        }
                    } catch (BadTransformationSpec e1) {
                        log.error(e1.getMessage(), e1);
                    } catch (MalformedSourceException e1) {
                        log.error(e1.getMessage(), e1);
                    }
                }
            }

            List<WorkspaceItem> result = null;

            //try to ingest workspaceitems
            if (!tmpResult.isEmpty()) {
                TransformationEngine transformationEngine2 = submissionLookupService.getPhase2TransformationEngine();
                if (transformationEngine2 != null) {
                    SubmissionItemDataLoader dataLoader =
                        (SubmissionItemDataLoader) transformationEngine2.getDataLoader();
                    dataLoader.setDtoList(tmpResult);
                    // dataLoader.setProviders()

                    DSpaceWorkspaceItemOutputGenerator outputGenerator =
                        (DSpaceWorkspaceItemOutputGenerator) transformationEngine2.getOutputGenerator();
                    outputGenerator.setCollection(collection);
                    outputGenerator.setContext(context);
                    outputGenerator.setFormName(submissionConfig.getSubmissionName());
                    outputGenerator.setDto(tmpResult.get(0));

                    try {
                        transformationEngine2.transform(spec);
                        result = outputGenerator.getWitems();
                    } catch (BadTransformationSpec e1) {
                        e1.printStackTrace();
                    } catch (MalformedSourceException e1) {
                        e1.printStackTrace();
                    }
                }
            }

            //we have to create the workspaceitem to push the file also if nothing found before
            if (result == null) {
                WorkspaceItem source =
                    submissionService.createWorkspaceItem(context, getRequestService().getCurrentRequest());
                result = new ArrayList<>();
                result.add(source);
            }

            //perform upload of bitstream if there is exact one result and convert workspaceitem to entity rest
            if (result != null && !result.isEmpty()) {
                for (WorkspaceItem wi : result) {

                    List<ErrorRest> errors = new ArrayList<ErrorRest>();

                    //load bitstream into bundle ORIGINAL only if there is one result (approximately this is the
                    // right behaviour for pdf file but not for other bibliographic format e.g. bibtex)
                    if (result.size() == 1) {

                        for (int i = 0; i < submissionConfig.getNumberOfSteps(); i++) {
                            SubmissionStepConfig stepConfig = submissionConfig.getStep(i);

                            ClassLoader loader = this.getClass().getClassLoader();
                            Class stepClass;
                            try {
                                stepClass = loader.loadClass(stepConfig.getProcessingClassName());

                                Object stepInstance = stepClass.newInstance();
                                if (UploadableStep.class.isAssignableFrom(stepClass)) {
                                    UploadableStep uploadableStep = (UploadableStep) stepInstance;
                                    ErrorRest err = uploadableStep.upload(context, submissionService, stepConfig, wi,
                                            uploadfile);
                                    if (err != null) {
                                        errors.add(err);
                                    }
                                }

                            } catch (Exception e) {
                                log.error(e.getMessage(), e);
                            }
                        }
                    }
                    WorkspaceItemRest wsi = converter.toRest(wi, Projection.DEFAULT);
                    if (result.size() == 1) {
                        if (!errors.isEmpty()) {
                            wsi.getErrors().addAll(errors);
                        }
                    }
                    results.add(wsi);
                }
            }
        } finally {
            file.delete();
        }
        return results;
    }

    @Override
    protected WorkspaceItemRest createAndReturn(Context context, List<String> stringList)
        throws AuthorizeException, SQLException, RepositoryMethodNotImplementedException {

        HttpServletRequest req = getRequestService().getCurrentRequest().getHttpServletRequest();
        WorkspaceItem workspaceItem = uriListHandlerService.handle(context, req, stringList, WorkspaceItem.class);
        return converter.toRest(workspaceItem, Projection.DEFAULT);
    }


}
