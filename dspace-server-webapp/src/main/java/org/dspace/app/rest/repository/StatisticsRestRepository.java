/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.model.StatisticsSupportRest;
import org.dspace.app.rest.model.UsageReportRest;
import org.dspace.app.rest.utils.DSpaceObjectUtils;
import org.dspace.app.rest.utils.UsageReportUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component(StatisticsSupportRest.CATEGORY + "." + UsageReportRest.NAME)
public class StatisticsRestRepository extends DSpaceRestRepository<UsageReportRest, String> {

    @Autowired
    private DSpaceObjectUtils dspaceObjectUtil;

    @Autowired
    private UsageReportUtils usageReportUtils;

    public StatisticsSupportRest getStatisticsSupport() {
        return new StatisticsSupportRest();
    }

    @Override
    @PreAuthorize("hasPermission(#uuidObjectReportId, 'usagereport', 'READ')")
    public UsageReportRest findOne(Context context, String uuidObjectReportId) {
        UUID uuidObject = UUID.fromString(StringUtils.substringBefore(uuidObjectReportId, "_"));
        String reportId = StringUtils.substringAfter(uuidObjectReportId, "_");

        UsageReportRest usageReportRest = null;
        try {
            DSpaceObject dso = dspaceObjectUtil.findDSpaceObject(context, uuidObject);
            if (dso == null) {
                return null;
            }
            usageReportRest = usageReportUtils.createUsageReport(context, dso, reportId);

        } catch (ParseException | SolrServerException | IOException | SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return converter.toRest(usageReportRest, utils.obtainProjection());
    }

    @PreAuthorize("hasPermission(#uri, 'usagereportsearch', 'READ')")
    @SearchRestMethod(name = "object")
    public Page<UsageReportRest> findByObject(@Parameter(value = "uri", required = true) String uri,
            @Parameter(value = "category") String category, Pageable pageable,
                                              @Parameter(value = "startDate") String startDate,
                                              @Parameter(value = "endDate") String endDate) {
        UUID uuid = UUID.fromString(StringUtils.substringAfterLast(uri, "/"));
        List<UsageReportRest> usageReportsOfItem = null;
        try {
            Context context = obtainContext();
            DSpaceObject dso = dspaceObjectUtil.findDSpaceObject(context, uuid);
            if (dso == null) {
                return null;
            }
            if (category != null && !usageReportUtils.categoryExists(dso, category)) {
                throw new IllegalArgumentException("The specified category doesn't exists: " + category);
            }
            usageReportsOfItem = usageReportUtils.getUsageReportsOfDSO(context, dso, category, startDate, endDate);
        } catch (SQLException | ParseException | SolrServerException | IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        return converter.toRestPage(usageReportsOfItem, pageable, usageReportsOfItem.size(), utils.obtainProjection());
    }

    @Override
    public Page<UsageReportRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not allowed!", "findAll");
    }

    @Override
    public Class<UsageReportRest> getDomainClass() {
        return UsageReportRest.class;
    }
}
