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
import org.dspace.app.rest.model.UsageReportCategoryRest;
import org.dspace.app.rest.statistics.StatisticsReportsConfiguration;
import org.dspace.app.rest.utils.DSpaceObjectUtils;
import org.dspace.app.rest.utils.UsageReportUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component(StatisticsSupportRest.CATEGORY + "." + UsageReportCategoryRest.PLURAL_NAME)
public class StatisticsCategoryRestRepository extends DSpaceRestRepository<UsageReportCategoryRest, String> {

    @Autowired
    private DSpaceObjectUtils dspaceObjectUtil;

    @Autowired
    private UsageReportUtils usageReportUtils;

    @Autowired
    private StatisticsReportsConfiguration statsConfiguration;

    public StatisticsSupportRest getStatisticsSupport() {
        return new StatisticsSupportRest();
    }

    @Override
    @PreAuthorize("permitAll()")
    public UsageReportCategoryRest findOne(Context context, String categoryId) {
        UsageReportCategoryRest usageReportCategoryRest = statsConfiguration.getCategory(categoryId);
        if (usageReportCategoryRest != null) {
            return converter.toRest(usageReportCategoryRest, utils.obtainProjection());
        } else {
            return null;
        }
    }

    @PreAuthorize("permitAll()")
    @SearchRestMethod(name = "object")
    public Page<UsageReportCategoryRest> findByObject(@Parameter(value = "uri", required = true) String uri,
                                              Pageable pageable) {
        UUID uuid = UUID.fromString(StringUtils.substringAfterLast(uri, "/"));
        List<UsageReportCategoryRest> usageReportsCategoriesOfItem = null;
        try {
            Context context = obtainContext();
            DSpaceObject dso = dspaceObjectUtil.findDSpaceObject(context, uuid);
            if (dso == null) {
                throw new IllegalArgumentException("No DSO found with uuid: " + uuid);
            }
            usageReportsCategoriesOfItem = usageReportUtils.getUsageReportsCategoriesOfDSO(context, dso);
        } catch (SQLException | ParseException | SolrServerException | IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        return converter.toRestPage(usageReportsCategoriesOfItem, pageable, usageReportsCategoriesOfItem.size(),
                utils.obtainProjection());
    }

    @Override
    public Page<UsageReportCategoryRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not allowed!", "findAll");
    }

    @Override
    public Class<UsageReportCategoryRest> getDomainClass() {
        return UsageReportCategoryRest.class;
    }
}
