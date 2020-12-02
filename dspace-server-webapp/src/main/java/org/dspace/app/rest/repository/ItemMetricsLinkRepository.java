/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;

import org.dspace.app.metrics.CrisMetrics;
import org.dspace.app.metrics.service.CrisMetricsService;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for "CrisMetrics" of an individual item.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
@Component(ItemRest.CATEGORY + "." + ItemRest.NAME + "." + ItemRest.METRICS)
public class ItemMetricsLinkRepository extends AbstractDSpaceRestRepository
                                       implements LinkRestRepository {

    @Autowired
    private CrisMetricsService crisMetricsService;

    @Autowired
    private ItemService itemService;

    @PreAuthorize("hasPermission(#itemUuid, 'ITEM', 'READ')")
    public Page<CrisMetrics> getMetrics(@Nullable HttpServletRequest request, @NotNull UUID itemUuid,
                                        @Nullable Pageable optionalPageable, Projection projection) {
        try {
            Context context = obtainContext();
            Item item = itemService.find(context, itemUuid);
            if (item == null) {
                throw new ResourceNotFoundException("No such item: " + itemUuid);
            }
            List<CrisMetrics>  metrics = crisMetricsService.findAllByItem(context, item);
            if (metrics == null) {
                throw new ResourceNotFoundException("No such metrics found!");
            }
            return converter.toRestPage(metrics, optionalPageable, projection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}