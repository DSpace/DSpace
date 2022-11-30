/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link.contentreports;

import java.util.LinkedList;

import org.dspace.app.rest.ContentReportsRestController;
import org.dspace.app.rest.link.HalLinkFactory;
import org.dspace.app.rest.model.hateoas.ContentReportsSupportResource;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

/**
 * This class adds the self and report links to the ContentReportsSupportResource.
 * @author Jean-François Morin (Université Laval)
 */
@Component
public class ContentReportsSupportHalLinkFactory
    extends HalLinkFactory<ContentReportsSupportResource, ContentReportsRestController> {

    @Override
    protected void addLinks(ContentReportsSupportResource halResource, Pageable pageable, LinkedList<Link> list)
        throws Exception {

        list.add(buildLink(IanaLinkRelations.SELF.value(), getMethodOn().getContentReportsSupport()));
        list.add(buildLink("filteredcollections", getMethodOn().getFilteredCollections("", null, null)));
        list.add(buildLink("filtereditems", getMethodOn().getFilteredItems()));
    }

    @Override
    protected Class<ContentReportsRestController> getControllerClass() {
        return ContentReportsRestController.class;
    }

    @Override
    protected Class<ContentReportsSupportResource> getResourceClass() {
        return ContentReportsSupportResource.class;
    }
}
