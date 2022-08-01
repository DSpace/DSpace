/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.model.SubmissionCCLicenseUrlRest;
import org.dspace.app.rest.model.wrapper.SubmissionCCLicenseUrl;
import org.dspace.app.rest.repository.DSpaceRestRepository;
import org.dspace.app.rest.utils.Utils;
import org.dspace.core.Context;
import org.dspace.license.service.CreativeCommonsService;
import org.dspace.services.RequestService;
import org.dspace.utils.DSpace;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.Link;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This Repository is responsible for handling the CC License URIs.
 * It only supports a search method
 */

@Component(SubmissionCCLicenseUrlRest.CATEGORY + "." + SubmissionCCLicenseUrlRest.NAME)
public class SubmissionCCLicenseUrlRepository extends DSpaceRestRepository<SubmissionCCLicenseUrlRest, String>
                                              implements InitializingBean {

    @Autowired
    protected Utils utils;

    @Autowired
    protected CreativeCommonsService creativeCommonsService;

    @Autowired
    protected ConverterService converter;

    protected RequestService requestService = new DSpace().getRequestService();

    @Autowired
    DiscoverableEndpointsService discoverableEndpointsService;

    /**
     * Retrieves the CC License URI based on the license ID and answers in the field questions, provided as parameters
     * to this request
     *
     * @return the CC License URI as a SubmissionCCLicenseUrlRest
     */
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @SearchRestMethod(name = "rightsByQuestions")
    public SubmissionCCLicenseUrlRest findByRightsByQuestions() {
        ServletRequest servletRequest = requestService.getCurrentRequest()
                                                      .getServletRequest();
        Map<String, String[]> requestParameterMap = servletRequest
                .getParameterMap();
        Map<String, String> parameterMap = new HashMap<>();
        String licenseId = servletRequest.getParameter("license");
        if (StringUtils.isBlank(licenseId)) {
            throw new DSpaceBadRequestException(
                    "A \"license\" parameter needs to be provided.");
        }

        // Loop through parameters to find answer parameters, adding them to the parameterMap. Zero or more answers
        // may exist, as some CC licenses do not require answers
        for (String parameter : requestParameterMap.keySet()) {
            if (StringUtils.startsWith(parameter, "answer_")) {
                String field = StringUtils.substringAfter(parameter, "answer_");
                String answer = "";
                if (requestParameterMap.get(parameter).length > 0) {
                    answer = requestParameterMap.get(parameter)[0];
                }
                parameterMap.put(field, answer);
            }
        }

        Map<String, String> fullParamMap = creativeCommonsService.retrieveFullAnswerMap(licenseId, parameterMap);
        if (fullParamMap == null) {
            throw new ResourceNotFoundException("No CC License could be matched on the provided ID: " + licenseId);
        }
        boolean licenseContainsCorrectInfo = creativeCommonsService.verifyLicenseInformation(licenseId, fullParamMap);
        if (!licenseContainsCorrectInfo) {
            throw new DSpaceBadRequestException(
                    "The provided answers do not match the required fields for the provided license.");
        }

        String licenseUri = creativeCommonsService.retrieveLicenseUri(licenseId, fullParamMap);

        SubmissionCCLicenseUrl submissionCCLicenseUrl = new SubmissionCCLicenseUrl(licenseUri, licenseUri);
        if (StringUtils.isBlank(licenseUri)) {
            throw new ResourceNotFoundException("No CC License URI could be found for ID: " + licenseId);
        }

        return converter.toRest(submissionCCLicenseUrl, utils.obtainProjection());

    }

    /**
     * The findOne method is not supported in this repository
     */
    @PreAuthorize("permitAll()")
    public SubmissionCCLicenseUrlRest findOne(final Context context, final String s) {
        throw new RepositoryMethodNotImplementedException(SubmissionCCLicenseUrlRest.NAME, "findOne");
    }

    /**
     * The findAll method is not supported in this repository
     */
    public Page<SubmissionCCLicenseUrlRest> findAll(final Context context, final Pageable pageable) {
        throw new RepositoryMethodNotImplementedException(SubmissionCCLicenseUrlRest.NAME, "findAll");
    }

    public Class<SubmissionCCLicenseUrlRest> getDomainClass() {
        return SubmissionCCLicenseUrlRest.class;
    }

    @Override
    public void afterPropertiesSet() {
        discoverableEndpointsService.register(this, Arrays.asList(
                Link.of("/api/" + SubmissionCCLicenseUrlRest.CATEGORY + "/" +
                        SubmissionCCLicenseUrlRest.PLURAL + "/search",
                        SubmissionCCLicenseUrlRest.PLURAL + "-search")));
    }

}
