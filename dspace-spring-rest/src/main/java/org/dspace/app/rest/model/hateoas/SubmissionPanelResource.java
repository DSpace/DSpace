/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import org.atteo.evo.inflector.English;
import org.dspace.app.rest.RestResourceController;
import org.dspace.app.rest.model.InputFormRest;
import org.dspace.app.rest.model.SubmissionPanelRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;
import org.springframework.hateoas.Link;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * SubmissionPanel Rest HAL Resource. The HAL Resource wraps the REST Resource
 * adding support for the links and embedded resources
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 *
 */
@RelNameDSpaceResource(SubmissionPanelRest.NAME)
public class SubmissionPanelResource extends DSpaceResource<SubmissionPanelRest> {

	public SubmissionPanelResource(SubmissionPanelRest sd, Utils utils, String... rels) {
		super(sd, utils, rels);	
		if("input-form".equals(sd.getPanelType())) {
			RestResourceController methodOn = methodOn(RestResourceController.class, InputFormRest.CATEGORY, InputFormRest.NAME);
			UriComponentsBuilder uriComponentsBuilder = linkTo(methodOn
					.findRel(null, InputFormRest.CATEGORY, English.plural(InputFormRest.NAME), sd.getId(), "", null, null, null))
					.toUriComponentsBuilder();
			String uribuilder = uriComponentsBuilder.build().toString();
			Link link = new Link(uribuilder.substring(0, uribuilder.lastIndexOf("/")), InputFormRest.NAME);
			add(link);	
		}		
	}
}