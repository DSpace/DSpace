/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.NotFoundException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.MethodNotAllowedException;
import org.dspace.app.rest.exception.UnprocessableEditException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.EditItemModeRest;
import org.dspace.app.rest.model.EditItemRest;
import org.dspace.app.rest.model.ErrorRest;
import org.dspace.app.rest.model.WorkspaceItemRest;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.rest.submit.AbstractProcessingStep;
import org.dspace.app.rest.submit.SubmissionService;
import org.dspace.app.rest.submit.UploadableStep;
import org.dspace.app.util.SubmissionConfig;
import org.dspace.app.util.SubmissionConfigReader;
import org.dspace.app.util.SubmissionConfigReaderException;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.content.edit.EditItem;
import org.dspace.content.edit.EditItemMode;
import org.dspace.content.edit.service.EditItemModeService;
import org.dspace.content.edit.service.EditItemService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.EPersonServiceImpl;
import org.dspace.validation.model.ValidationError;
import org.dspace.validation.service.ValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.data.rest.webmvc.json.patch.PatchException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 *  This is the repository responsible to manage EditItem Rest object
 *
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 */
@Component(EditItemRest.CATEGORY + "." + EditItemRest.NAME_PLURAL)
public class EditItemRestRepository extends DSpaceRestRepository<EditItemRest, String> {

    private static Logger log = LogManager.getLogger(EditItemRestRepository.class);

    public static final String OPERATION_PATH_SECTIONS = "sections";

    private SubmissionConfigReader submissionConfigReader;

    @Autowired
    EditItemService eis;

    @Autowired
    AuthorizeService authorizeService;

    @Autowired
    SubmissionService submissionService;

    @Autowired
    EPersonServiceImpl epersonService;

    @Autowired
    ItemService itemService;

    @Autowired
    BitstreamService bitstreamService;

    @Autowired
    private ValidationService validationService;

    @Autowired
    private EditItemModeService editItemModeService;

    /**
     * Constructor for EditItemRestRepository.
     * Initializes the submission configuration reader for processing edit item submissions.
     *
     * @throws SubmissionConfigReaderException if the submission configuration cannot be read
     */
    public EditItemRestRepository() throws SubmissionConfigReaderException {
        submissionConfigReader = new SubmissionConfigReader();
    }

    /**
     * Retrieves a single EditItem by its composite identifier.
     * The identifier must be in the format "UUID:MODE", where UUID is the item identifier
     * and MODE is the edit mode name. Security is checked internally in EditItemModeServiceImpl.hasAccess.
     *
     * @param context the DSpace context
     * @param data the composite identifier in format "UUID:MODE" (e.g., "123e4567-e89b-12d3-a456-426614174000:FULL")
     * @return the EditItemRest representation of the edit item, or null if not found
     * @throws DSpaceBadRequestException if the data parameter is not in the expected format
     * @throws ResourceNotFoundException if the item with the specified UUID does not exist or the mode configuration
     *                                   is not found
     * @throws AccessDeniedException if the current user does not have rights to edit in the specified mode
     * @throws RuntimeException if a database error occurs
     */
    @Override
    @PreAuthorize("isAuthenticated()")
    // Security is checked internally in EditItemModeServiceImpl.hasAccess
    public EditItemRest findOne(Context context, String data) {
        EditItem editItem = null;
        String uuid = null;
        String modeName = null;
        String[] values = data.split(":");
        if (values != null && values.length == 2) {
            uuid = values[0];
            modeName = values[1];
        } else {
            throw new DSpaceBadRequestException(
                    "Given parameters are incomplete. Expected <UUID-ITEM>:<MODE>, Received: " + data);
        }
        try {
            UUID itemUuid = UUID.fromString(uuid);
            Item item = itemService.find(context, itemUuid);
            if (item == null) {
                throw new ResourceNotFoundException("No such item with uuid : " + itemUuid);
            }
            editItem = eis.find(context, item, modeName);
        } catch (NotFoundException nfe) {
            throw new ResourceNotFoundException("No such mode configuration : " + modeName);
        } catch (AuthorizeException ae) {
            throw new AccessDeniedException("The current user does not have rights to edit mode <" + modeName + ">");
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (editItem == null) {
            return null;
        }
        return converter.toRest(editItem, utils.obtainProjection());
    }

    /**
     * Retrieves all edit items in the system with pagination support.
     * This method returns a paginated list of all edit items accessible in the repository.
     *
     * @param context the DSpace context
     * @param pageable the pagination information including page size and offset
     * @return a paginated list of EditItemRest objects
     * @throws RuntimeException if a database error occurs while retrieving edit items
     */
    @PreAuthorize("isAuthenticated()")
    @Override
    public Page<EditItemRest> findAll(Context context, Pageable pageable) {
        Iterator<EditItem> it = null;
        List<EditItem> items = new ArrayList<EditItem>();
        int total = 0;
        try {
            total = eis.countTotal(context);
            it = eis.findAll(context, pageable.getPageSize(), pageable.getOffset());
            while (it.hasNext()) {
                EditItem i = it.next();
                items.add(i);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return converter.toRestPage(items, pageable, total, utils.obtainProjection());
    }

    /**
     * Returns the domain class managed by this repository.
     * This method is required by the DSpaceRestRepository interface to identify
     * the REST resource type handled by this repository.
     *
     * @return the EditItemRest class object
     */
    @Override
    public Class<EditItemRest> getDomainClass() {
        return EditItemRest.class;
    }

    /**
     * Handles file upload for an edit item submission step.
     * This method processes file uploads for items in edit mode by delegating to the appropriate
     * uploadable submission step. The data parameter must be in the format "UUID:MODE".
     * The method validates permissions, finds the appropriate uploadable step in the submission
     * configuration, and processes the file upload.
     *
     * @param request the HTTP servlet request
     * @param apiCategory the API category (not used in this implementation)
     * @param model the model name (not used in this implementation)
     * @param data the composite identifier in format "UUID:MODE"
     * @param file the multipart file to upload
     * @return the updated EditItemRest representation with any upload errors included
     * @throws SQLException if a database error occurs
     * @throws DSpaceBadRequestException if the data parameter is not in the expected format
     * @throws ResourceNotFoundException if the item with the specified UUID does not exist
     * @throws AccessDeniedException if the current user does not have rights to edit in the specified mode
     * @throws MethodNotAllowedException if no uploadable step is defined for the given edit item mode
     */
    @Override
    public EditItemRest upload(HttpServletRequest request, String apiCategory, String model, String data,
                                    MultipartFile file) throws SQLException {

        String uuid = null;
        String modeName = null;
        String[] values = data.split(":");
        if (values != null && values.length == 2) {
            uuid = values[0];
            modeName = values[1];
        } else {
            throw new DSpaceBadRequestException(
                    "Given parameters are incomplete. Expected <UUID-ITEM>:<MODE>, Received: " + data);
        }
        Context context = obtainContext();

        UUID itemUuid = UUID.fromString(uuid);
        Item item = itemService.find(context, itemUuid);
        if (item == null) {
            throw new ResourceNotFoundException("No such item with uuid : " + itemUuid);
        }

        EditItemRest eir = findOne(context, data);
        EditItem source = null;
        try {
            source = eis.find(context, item, modeName);
        } catch (AuthorizeException e1) {
            throw new AccessDeniedException(e1.getMessage());
        }

        if (source != null && source.getMode() == null) {
            // The user is not allowed to give edit mode, return 403
            throw new AccessDeniedException(
                    "The current user does not have rights to edit mode <" + modeName + ">");
        }

        List<ErrorRest> errors = new ArrayList<ErrorRest>();
        SubmissionConfig submissionConfig =
            submissionConfigReader.getSubmissionConfigByName(source.getMode().getSubmissionDefinition());
        context.turnOffAuthorisationSystem();
        boolean hasUploadableStep = false;
        Bitstream newBitstream = null;
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
                    hasUploadableStep = true;
                    UploadableStep uploadableStep = (UploadableStep) stepInstance;
                    Pair<Bitstream, ErrorRest> pair = uploadableStep.upload(
                        context, submissionService, stepConfig, source, file
                    );
                    if (pair.getRight() != null) {
                        errors.add(pair.getRight());
                    } else {
                        newBitstream = pair.getLeft();
                    }
                }

            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }

        }

        // If replaceFile parameter (bitstream UUID) is provided and upload succeeded, replace the original bitstream.
        String replaceFileUuid = request.getParameter("replaceFile");
        if (replaceFileUuid != null && !replaceFileUuid.isBlank() && newBitstream != null && errors.isEmpty()) {
            try {
                Bitstream originalBitstream = bitstreamService.find(context, UUID.fromString(replaceFileUuid));
                if (originalBitstream != null) {
                    boolean replaceName = Boolean.parseBoolean(request.getParameter("replaceName"));
                    bitstreamService.replace(context, originalBitstream, newBitstream, replaceName);
                }
            } catch (Exception e) {
                log.error("Error replacing bitstream with UUID " + replaceFileUuid, e);
            }
        }

        if (!hasUploadableStep) {
            throw new MethodNotAllowedException("No uploadable step defined for the given edit item mode");
        }

        context.commit();
        context.reloadEntity(source.getItem());
        eir = converter.toRest(source, utils.obtainProjection());

        if (!errors.isEmpty()) {
            eir.getErrors().addAll(errors);
        }

        context.commit();
        context.restoreAuthSystemState();
        return eir;
    }

    /**
     * Applies a JSON Patch to an edit item.
     * This method processes patch operations on edit item sections, validating that the number
     * of validation errors does not increase after applying the patch. All operations must
     * target paths starting with "/sections/". The method enforces validation by comparing
     * error counts before and after the patch is applied.
     *
     * @param context the DSpace context
     * @param request the HTTP servlet request
     * @param apiCategory the API category (not used in this implementation)
     * @param model the model name (not used in this implementation)
     * @param data the composite identifier in format "UUID:MODE"
     * @param patch the JSON Patch containing operations to apply
     * @throws SQLException if a database error occurs
     * @throws AuthorizeException if the user is not authorized to perform the operation
     * @throws DSpaceBadRequestException if the data parameter is not in the expected format or patch path is invalid
     * @throws ResourceNotFoundException if the item with the specified UUID does not exist
     * @throws AccessDeniedException if the current user does not have rights to edit in the specified mode
     * @throws UnprocessableEditException if applying the patch increases the number of validation errors
     * @throws UnprocessableEntityException if the specified section does not exist in the submission configuration
     */
    @Override
    public void patch(Context context, HttpServletRequest request, String apiCategory, String model, String data,
                      Patch patch) throws SQLException, AuthorizeException {
        List<Operation> operations = patch.getOperations();

        String uuid = null;
        String modeName = null;
        String[] values = data.split(":");
        if (values != null && values.length > 0) {
            uuid = values[0];
            modeName = values[1];
        } else {
            throw new DSpaceBadRequestException(
                    "Data: " + data);
        }
        UUID itemUuid = UUID.fromString(uuid);
        Item item = itemService.find(context, itemUuid);
        if (item == null) {
            throw new ResourceNotFoundException("No such item with uuid : " + itemUuid);
        }
        EditItem source = eis.find(context, item, modeName);
        if (source != null && source.getMode() == null) {
            // The user is not allowed to give edit mode, return 403
            throw new AccessDeniedException(
                    "The current user does not have rights to edit mode <" + modeName + ">");
        }
        context.turnOffAuthorisationSystem();
        List<ValidationError> initialErrors = validationService.validate(context, source);
        int numInitialErrors = calculateErrors(initialErrors);
        EditItemRest eir = findOne(context, data);
        for (Operation op : operations) {
            // the value in the position 0 is a null value
            String[] path = op.getPath().substring(1).split("/", 3);
            if (OPERATION_PATH_SECTIONS.equals(path[0])) {
                String section = path[1];
                evaluatePatch(context, request, source, eir, section, op);
            } else {
                throw new DSpaceBadRequestException(
                    "Patch path operation need to starts with '" + OPERATION_PATH_SECTIONS + "'");
            }
        }

        List<ValidationError> errors = validationService.validate(context, source);
        int editErrors = calculateErrors(errors);
        if (numInitialErrors < editErrors) {
            throw new UnprocessableEditException(errors, "The number of validation errors in the item increase from "
                    + numInitialErrors + " to " + editErrors);
        }

        eis.update(context, source);
        context.restoreAuthSystemState();
    }

    private int calculateErrors(List<ValidationError> errors) {
        if (errors == null || errors.isEmpty()) {
            return 0;
        } else {
            return errors.stream().mapToInt(e -> e.getPaths().size()).sum();
        }
    }

    private void evaluatePatch(Context context, HttpServletRequest request, EditItem source, EditItemRest eir,
            String section, Operation op) {
        boolean sectionExist = false;
        SubmissionConfig submissionConfig =
                submissionConfigReader.getSubmissionConfigByName(eir.getSubmissionDefinition().getName());
        for (int stepNum = 0; stepNum < submissionConfig.getNumberOfSteps(); stepNum++) {

            SubmissionStepConfig stepConfig = submissionConfig.getStep(stepNum);

            if (section.equals(stepConfig.getId())) {
                sectionExist = true;
                /*
                * First, load the step processing class (using the current class loader)
                */
                ClassLoader loader = this.getClass().getClassLoader();
                Class stepClass;
                try {
                    stepClass = loader.loadClass(stepConfig.getProcessingClassName());

                    Object stepInstance = stepClass.newInstance();

                    if (stepInstance instanceof AbstractProcessingStep) {
                         // load the JSPStep interface for this step
                        AbstractProcessingStep stepProcessing = (AbstractProcessingStep) stepClass.newInstance();
                        stepProcessing.doPatchProcessing(context, request, source, op, stepConfig);
                    } else {
                        throw new DSpaceBadRequestException(
                             "The submission step class specified by '" + stepConfig.getProcessingClassName() +
                             "' does not extend the class org.dspace.submit.AbstractProcessingStep!" +
                             " Therefore it cannot be used by the Configurable Submission as the <processing-class>!");
                    }

                } catch (UnprocessableEntityException e) {
                    log.error(e.getMessage(), e);
                    throw e;
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    throw new PatchException("Error processing the patch request", e);
                }
            }
        }
        if (!sectionExist) {
            throw new UnprocessableEntityException("The section with name " + section +
                                                   " does not exist in this submission!");
        }
    }

    @PreAuthorize("hasPermission(#submitterID, 'EPERSON', 'READ')")
    @SearchRestMethod(name = "findBySubmitter")
    public Page<WorkspaceItemRest> findBySubmitter(@Parameter(value = "uuid", required = true) UUID submitterID,
            Pageable pageable) {
        try {
            Context context = obtainContext();
            EPerson ep = epersonService.find(context, submitterID);
            long total = eis.countBySubmitter(context, ep);
            List<EditItem> witems = eis.findBySubmitter(context, ep, pageable.getPageSize(),
                    Math.toIntExact(pageable.getOffset()));
            return converter.toRestPage(witems, pageable, total, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @PreAuthorize("isAuthenticated()")
    @SearchRestMethod(name = "findModesById")
    public Page<EditItemModeRest> findModesById(@Parameter(value = "uuid", required = true) UUID id,
                                                Pageable pageable) {

        Context context = obtainContext();
        List<EditItemMode> modes = null;
        try {
            modes = editItemModeService.findModes(context, id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (modes == null) {
            return null;
        }
        return converter.toRestPage(modes, pageable, modes.size(), utils.obtainProjection());
    }

}
