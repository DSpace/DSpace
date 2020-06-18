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
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component(StatisticsSupportRest.CATEGORY + "." + UsageReportRest.NAME)
public class StatisticsRestRepository extends DSpaceRestRepository<UsageReportRest, String> {

    @Autowired
    private UsageReportService usageReportService;

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
            usageReportRest = usageReportService.createUsageReport(context, uuidObject, reportId);
        } catch (ParseException | SolrServerException | IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        return usageReportRest;
    }

    @PreAuthorize("hasPermission(#uri, 'usagereportsearch', 'READ')")
    @SearchRestMethod(name = "object")
    public Page<UsageReportRest> findByObject(@Parameter(value = "uri", required = true) String uri,
                                              Pageable pageable) {
        UUID uuid = UUID.fromString(StringUtils.substringAfterLast(uri, "/"));
        List<UsageReportRest> usageReportsOfItem = null;
        try {
            usageReportsOfItem = usageReportService.getUsageReportsOfDSO(obtainContext(), uuid);
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
