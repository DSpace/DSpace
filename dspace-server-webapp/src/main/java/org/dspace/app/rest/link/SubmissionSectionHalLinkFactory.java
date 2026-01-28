/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link;
import static org.dspace.app.rest.model.SubmissionFormRest.NAME_LINK_ON_PANEL;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.util.LinkedList;

import org.apache.log4j.Logger;
import org.dspace.app.rest.RestResourceController;
import org.dspace.app.rest.model.SubmissionAccessOptionRest;
import org.dspace.app.rest.model.SubmissionFormRest;
import org.dspace.app.rest.model.SubmissionSectionRest;
import org.dspace.app.rest.model.SubmissionUploadRest;
import org.dspace.app.rest.model.hateoas.SubmissionSectionResource;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.submit.model.UploadConfiguration;
import org.dspace.submit.model.UploadConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * This class' purpose is to provide a factory to add links to the AuthorityEntryResource. The addLinks factory will
 * be called
 * from the HalLinkService class addLinks method.
 */
@Component
public class SubmissionSectionHalLinkFactory extends HalLinkFactory<SubmissionSectionResource, RestResourceController> {
    /* Log4j logger */
    private static final Logger log = Logger.getLogger(SubmissionSectionHalLinkFactory.class);

    @Autowired
    private UploadConfigurationService uploadConfigurationService;

    protected void addLinks(final SubmissionSectionResource halResource, final Pageable pageable,
                            final LinkedList<Link> list) throws Exception {
        SubmissionSectionRest sd = halResource.getContent();

        if (sd != null) {
            if (sd.supportsType(SubmissionStepConfig.INPUT_FORM_STEP_NAME)) {
                buildLink(list, sd.getId(), SubmissionFormRest.CATEGORY, SubmissionFormRest.NAME,
                        SubmissionFormRest.PLURAL_NAME);
            }
            if (sd.supportsType(SubmissionStepConfig.UPLOAD_STEP_NAME)) {
                UploadConfiguration uploadConfiguration = uploadConfigurationService.getMap().get(sd.getId());
                if (uploadConfiguration != null) {
                    buildLink(list, uploadConfiguration.getName(), SubmissionUploadRest.CATEGORY,
                            SubmissionUploadRest.NAME, SubmissionUploadRest.PLURAL_NAME);
                } else {
                    log.warn("No UploadConfiguration configured for submission step ID " + sd.getType());
                }
            }
            if (sd.supportsType(SubmissionStepConfig.ACCESS_CONDITION_STEP_NAME)) {
                buildLink(list, sd.getId(), SubmissionAccessOptionRest.CATEGORY, SubmissionAccessOptionRest.NAME,
                      SubmissionAccessOptionRest.PLURAL_NAME);
            }
        }
    }

    private void buildLink(final LinkedList<Link> list, String sectionId, String category, String name,
                           String plural) {
        UriComponentsBuilder uriComponentsBuilder = linkTo(getMethodOn(category, name)
                    .findRel(null, null, category, plural, sectionId, "", null, null))
                    .toUriComponentsBuilder();
        String uribuilder = uriComponentsBuilder.build().toString();
        list.add(buildLink(NAME_LINK_ON_PANEL, uribuilder.substring(0, uribuilder.lastIndexOf("/"))));
    }

    protected Class<RestResourceController> getControllerClass() {
        return RestResourceController.class;
    }

    protected Class<SubmissionSectionResource> getResourceClass() {
        return SubmissionSectionResource.class;
    }

}

