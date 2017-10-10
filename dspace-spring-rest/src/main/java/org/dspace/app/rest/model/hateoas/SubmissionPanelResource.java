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
import org.dspace.app.rest.model.BrowseIndexRest;
import org.dspace.app.rest.model.InputFormRest;
import org.dspace.app.rest.model.SubmissionPanelRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * SubmissionDefinition Rest HAL Resource. The HAL Resource wraps the REST Resource
 * adding support for the links and embedded resources
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@RelNameDSpaceResource(SubmissionPanelRest.NAME)
public class SubmissionPanelResource extends ResourceSupport {
	public SubmissionPanelResource(SubmissionPanelRest sd, String... rels) {
		InputFormRest ifr = new InputFormRest();
		RestResourceController methodOn = methodOn(RestResourceController.class, ifr.getCategory(), ifr.getType());
		UriComponentsBuilder uriComponentsBuilder = linkTo(methodOn
				.findRel(null, ifr.getCategory(), English.plural(ifr.getType()), sd.getId(), InputFormRest.NAME, null, null, null))
				.toUriComponentsBuilder();
		Link link = new Link(uriComponentsBuilder.build().toString(), InputFormRest.NAME);
		add(link);
	}
}