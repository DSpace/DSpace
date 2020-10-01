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
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.EditItemRest;
import org.dspace.app.rest.model.ErrorRest;
import org.dspace.app.rest.model.WorkspaceItemRest;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.rest.submit.AbstractRestProcessingStep;
import org.dspace.app.rest.submit.SubmissionService;
import org.dspace.app.rest.submit.UploadableStep;
import org.dspace.app.util.SubmissionConfig;
import org.dspace.app.util.SubmissionConfigReader;
import org.dspace.app.util.SubmissionConfigReaderException;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.edit.EditItem;
import org.dspace.content.edit.EditItemMode;
import org.dspace.content.edit.service.EditItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.EPersonServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 *  This is the repository responsible to manage EditItem Rest object
 *
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 */
@Component(EditItemRest.CATEGORY + "." + EditItemRest.NAME)
public class EditItemRestRepository extends DSpaceRestRepository<EditItemRest, String> {

    private static final Logger log = Logger.getLogger(EditItemRestRepository.class);

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

    public EditItemRestRepository() throws SubmissionConfigReaderException {
        submissionConfigReader = new SubmissionConfigReader();
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.DSpaceRestRepository#findOne(org.dspace.core.Context, java.io.Serializable)
     */
    @Override
    public EditItemRest findOne(Context context, String data) {
        EditItem editItem = null;
        try {
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
            editItem = eis.find(
                    context,
                    UUID.fromString(uuid),
                    modeName);
            if (editItem != null
                    && !modeName.equalsIgnoreCase(EditItemMode.NONE)
                    && editItem.getMode() == null) {
                // The user is not allowed to give edit mode, return 403
                throw new AccessDeniedException(
                        "The current user does not have rights to edit mode <" + modeName + ">");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (editItem == null) {
            return null;
        }
        return converter.toRest(editItem, utils.obtainProjection());
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.DSpaceRestRepository#
     * findAll(org.dspace.core.Context, org.springframework.data.domain.Pageable)
     */
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

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.DSpaceRestRepository#getDomainClass()
     */
    @Override
    public Class<EditItemRest> getDomainClass() {
        return EditItemRest.class;
    }

    @Override
    protected void delete(Context context, String data) {
        EditItem source = null;
        try {
            String uuid = null;
            String[] values = data.split(":");
            if (values != null && values.length > 0) {
                uuid = values[0];
            } else {
                throw new DSpaceBadRequestException(
                        "Data: " + data);
            }
            source = eis.find(context, UUID.fromString(uuid));
            if (!authorizeService.isAdmin(context) && !eis.getItemService().canEdit(context, source.getItem())) {
                throw new AuthorizeException("Unauthorized attempt to edit ItemID " + source.getItem().getID());
            }
            context.turnOffAuthorisationSystem();
            eis.deleteWrapper(context, source);
        } catch (SQLException | AuthorizeException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @PreAuthorize("hasPermission(#id, 'WORKSPACEITEM', 'WRITE')")
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
        EditItemRest eir = findOne(context, data);
        EditItem source = eis.find(context, UUID.fromString(uuid), modeName);

        if (source != null && source.getMode() == null) {
            // The user is not allowed to give edit mode, return 403
            throw new AccessDeniedException(
                    "The current user does not have rights to edit mode <" + modeName + ">");
        }

        List<ErrorRest> errors = new ArrayList<ErrorRest>();
        SubmissionConfig submissionConfig =
            submissionConfigReader.getSubmissionConfigByName(source.getMode().getSubmissionDefinition());
        context.turnOffAuthorisationSystem();
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
        EditItem source = eis.find(context, UUID.fromString(uuid));
        if (source != null && source.getMode() == null) {
            // The user is not allowed to give edit mode, return 403
            throw new AccessDeniedException(
                    "The current user does not have rights to edit mode <" + modeName + ">");
        }
        context.turnOffAuthorisationSystem();

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
        eis.update(context, source);
        context.restoreAuthSystemState();
    }

    private void evaluatePatch(Context context, HttpServletRequest request, EditItem source, EditItemRest eir,
            String section, Operation op) {
        SubmissionConfig submissionConfig =
                submissionConfigReader.getSubmissionConfigByName(eir.getSubmissionDefinition().getName());
        for (int stepNum = 0; stepNum < submissionConfig.getNumberOfSteps(); stepNum++) {

            SubmissionStepConfig stepConfig = submissionConfig.getStep(stepNum);

            if (section.equals(stepConfig.getId())) {
                /*
                * First, load the step processing class (using the current class loader)
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
                        stepProcessing.doPatchProcessing(
                                context, getRequestService().getCurrentRequest(), source, op, stepConfig);
                        stepProcessing.doPostProcessing(context, source);
                    } else {
                        throw new DSpaceBadRequestException(
                             "The submission step class specified by '" + stepConfig.getProcessingClassName() +
                             "' does not extend the class org.dspace.submit.AbstractProcessingStep!" +
                             " Therefore it cannot be used by the Configurable Submission as the <processing-class>!");
                    }

                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
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
}
