/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

import java.util.LinkedList;

import org.atteo.evo.inflector.English;
import org.dspace.app.rest.RestResourceController;
import org.dspace.app.rest.model.SubmissionFormRest;
import org.dspace.app.rest.model.SubmissionSectionRest;
import org.dspace.app.rest.model.SubmissionUploadRest;
import org.dspace.app.rest.model.hateoas.SubmissionSectionResource;
import org.dspace.app.util.SubmissionStepConfig;
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

    protected void addLinks(final SubmissionSectionResource halResource, final Pageable pageable,
                            final LinkedList<Link> list) throws Exception {
        SubmissionSectionRest sd = halResource.getContent();

        if (SubmissionStepConfig.INPUT_FORM_STEP_NAME.equals(sd.getSectionType())) {
            UriComponentsBuilder uriComponentsBuilder = linkTo(
                getMethodOn(SubmissionFormRest.CATEGORY, SubmissionFormRest.NAME)
                    .findRel(null, null, SubmissionFormRest.CATEGORY, English.plural(SubmissionFormRest.NAME),
                            sd.getId(), "", null, null))
                .toUriComponentsBuilder();
            String uribuilder = uriComponentsBuilder.build().toString();
            list.add(
                buildLink(SubmissionFormRest.NAME_LINK_ON_PANEL, uribuilder.substring(0, uribuilder.lastIndexOf("/"))));
        }
        if (SubmissionStepConfig.UPLOAD_STEP_NAME.equals(sd.getSectionType())) {
            UriComponentsBuilder uriComponentsBuilder = linkTo(
                getMethodOn(RestResourceController.class, SubmissionUploadRest.CATEGORY, SubmissionUploadRest.NAME)
                    .findRel(null, null, SubmissionUploadRest.CATEGORY, English.plural(SubmissionUploadRest.NAME),
                           sd.getId(), "", null, null))
                .toUriComponentsBuilder();
            String uribuilder = uriComponentsBuilder.build().toString();
            list.add(
                buildLink(SubmissionFormRest.NAME_LINK_ON_PANEL, uribuilder.substring(0, uribuilder.lastIndexOf("/"))));
        }

    }

    protected Class<RestResourceController> getControllerClass() {
        return RestResourceController.class;
    }

    protected Class<SubmissionSectionResource> getResourceClass() {
        return SubmissionSectionResource.class;
    }

}

