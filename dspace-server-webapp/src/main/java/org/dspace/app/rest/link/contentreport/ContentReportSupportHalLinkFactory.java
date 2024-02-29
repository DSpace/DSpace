/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link.contentreport;

import java.util.LinkedList;

import org.dspace.app.rest.ContentReportRestController;
import org.dspace.app.rest.link.HalLinkFactory;
import org.dspace.app.rest.model.hateoas.ContentReportSupportResource;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

/**
 * This class adds the self and report links to the ContentReportSupportResource.
 * @author Jean-François Morin (Université Laval)
 */
@Component
public class ContentReportSupportHalLinkFactory
    extends HalLinkFactory<ContentReportSupportResource, ContentReportRestController> {

    @Override
    protected void addLinks(ContentReportSupportResource halResource, Pageable pageable, LinkedList<Link> list)
        throws Exception {

        list.add(buildLink(IanaLinkRelations.SELF.value(), getMethodOn().getContentReportSupport()));
        list.add(buildLink("filteredcollections", getMethodOn().getFilteredCollections(null, null, null)));
        list.add(buildLink("filtereditems", getMethodOn()
                .getFilteredItems(null, null, null, null, null, null, null, null, null)));
    }

    @Override
    protected Class<ContentReportRestController> getControllerClass() {
        return ContentReportRestController.class;
    }

    @Override
    protected Class<ContentReportSupportResource> getResourceClass() {
        return ContentReportSupportResource.class;
    }
}
