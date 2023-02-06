/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.step;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.step.DataIdentifiers;
import org.dspace.app.rest.submit.AbstractProcessingStep;
import org.dspace.app.rest.submit.SubmissionService;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.identifier.DOI;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.identifier.Handle;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.factory.IdentifierServiceFactory;
import org.dspace.identifier.service.IdentifierService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.services.model.Request;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Submission processing step to return identifier data for use in the 'identifiers' submission section component
 * in dspace-angular. For effective use, the "identifiers.submission.register" configuration property
 * in identifiers.cfg should be enabled so that the WorkspaceItemService will register identifiers for the new item
 * at the time of creation, and the DOI consumer will allow workspace and workflow items to have their DOIs minted
 * or deleted as per item filter results.
 *
 * This method can be extended to allow (if authorised) an operation to be sent which will
 * override an item filter and force reservation of an identifier.
 *
 * @author Kim Shepherd
 */
public class ShowIdentifiersStep extends AbstractProcessingStep {

    private static final Logger log = LogManager.getLogger(ShowIdentifiersStep.class);

    @Autowired(required = true)
    protected HandleService handleService;
    @Autowired(required = true)
    protected ContentServiceFactory contentServiceFactory;

    /**
     * Override DataProcessing.getData, return data identifiers from getIdentifierData()
     *
     * @param submissionService The submission service
     * @param obj               The workspace or workflow item
     * @param config            The submission step configuration
     * @return                  A simple DataIdentifiers bean containing doi, handle and list of other identifiers
     */
    @Override
    public DataIdentifiers getData(SubmissionService submissionService, InProgressSubmission obj,
                                   SubmissionStepConfig config) throws Exception {
        // If configured, WorkspaceItemService item creation will also call IdentifierService.register()
        // for the new item, and the DOI consumer (if also configured) will mint or delete DOI entries as appropriate
        // while the item is saved in the submission / workflow process

        // This step simply looks for existing identifier data and returns it in section data for rendering
        return getIdentifierData(obj);
    }

    /**
     * Get data about existing identifiers for this in-progress submission item - this method doesn't require
     * submissionService or step config, so can be more easily called from doPatchProcessing as well
     *
     * @param obj   The workspace or workflow item
     * @return      A simple DataIdentifiers bean containing doi, handle and list of other identifiers
     */
    private DataIdentifiers getIdentifierData(InProgressSubmission obj) {
        Context context = getContext();
        DataIdentifiers result = new DataIdentifiers();
        // Load identifier service
        IdentifierService identifierService =
                IdentifierServiceFactory.getInstance().getIdentifierService();
        // Attempt to look up handle and DOI identifiers for this item
        String[] defaultTypes = {"handle", "doi"};
        List<String> displayTypes = Arrays.asList(configurationService.getArrayProperty(
                "identifiers.submission.display",
                defaultTypes));
        result.setDisplayTypes(displayTypes);
        String handle = identifierService.lookup(context, obj.getItem(), Handle.class);
        DOI doi = null;
        String doiString = null;
        try {
            doi = IdentifierServiceFactory.getInstance().getDOIService().findDOIByDSpaceObject(context, obj.getItem());
            if (doi != null && !DOIIdentifierProvider.MINTED.equals(doi.getStatus())
                    && !DOIIdentifierProvider.DELETED.equals(doi.getStatus())) {
                doiString = doi.getDoi();
            }
        } catch (SQLException e) {
            log.error(e.getMessage());
        }

        // Other identifiers can be looked up / resolved through identifier service or
        // its own specific service here

        // If we got a DOI, format it to its external form
        if (StringUtils.isNotEmpty(doiString)) {
            try {
                doiString = IdentifierServiceFactory.getInstance().getDOIService().DOIToExternalForm(doiString);
            } catch (IdentifierException e) {
                log.error("Error formatting DOI: " + doi);
            }
        }
        // If we got a handle, format it to its canonical form
        if (StringUtils.isNotEmpty(handle)) {
            handle = HandleServiceFactory.getInstance().getHandleService().getCanonicalForm(handle);
        }

        // Populate bean with data and return, if the identifier type is configured for exposure
        result.addIdentifier("doi", doiString,
                doi != null ? DOIIdentifierProvider.statusText[doi.getStatus()] : null);
        result.addIdentifier("handle", handle, null);
        return result;
    }

    /**
     * Utility method to get DSpace context from the HTTP request
     * @return  DSpace context
     */
    private Context getContext() {
        Context context;
        Request currentRequest = DSpaceServicesFactory.getInstance().getRequestService().getCurrentRequest();
        if (currentRequest != null) {
            HttpServletRequest request = currentRequest.getHttpServletRequest();
            context = ContextUtil.obtainContext(request);
        } else {
            context = new Context();
        }

        return context;
    }

    /**
     * This step is currently just for displaying identifiers and does not take additional patch operations
     * @param context
     *            the DSpace context
     * @param currentRequest
     *            the http request
     * @param source
     *            the in progress submission
     * @param op
     *            the json patch operation
     * @param stepConf
     * @throws Exception
     */
    @Override
    public void doPatchProcessing(Context context, HttpServletRequest currentRequest, InProgressSubmission source,
                                  Operation op, SubmissionStepConfig stepConf) throws Exception {
        log.warn("Not implemented");
    }

}
