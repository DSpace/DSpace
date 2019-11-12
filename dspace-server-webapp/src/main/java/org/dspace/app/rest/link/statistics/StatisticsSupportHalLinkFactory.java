/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link.statistics;

import java.util.LinkedList;

import org.dspace.app.rest.StatisticsRestController;
import org.dspace.app.rest.link.HalLinkFactory;
import org.dspace.app.rest.model.hateoas.StatisticsSupportResource;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

@Component
public class StatisticsSupportHalLinkFactory
    extends HalLinkFactory<StatisticsSupportResource, StatisticsRestController> {
    protected void addLinks(StatisticsSupportResource halResource, Pageable pageable, LinkedList<Link> list)
        throws Exception {

        list.add(buildLink(Link.REL_SELF, getMethodOn().getStatisticsSupport()));
        list.add(buildLink("viewevents", getMethodOn().getViewEvents()));
        list.add(buildLink("searchevents", getMethodOn().getSearchEvents()));
    }

    protected Class<StatisticsRestController> getControllerClass() {
        return StatisticsRestController.class;
    }

    protected Class<StatisticsSupportResource> getResourceClass() {
        return StatisticsSupportResource.class;
    }
}
