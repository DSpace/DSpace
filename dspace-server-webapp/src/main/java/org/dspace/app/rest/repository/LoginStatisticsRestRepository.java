/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import static java.lang.Math.toIntExact;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.LoginStatisticsRest;
import org.dspace.app.rest.model.StatisticsSupportRest;
import org.dspace.core.Context;
import org.dspace.statistics.LoginStatistics;
import org.dspace.statistics.service.LoginStatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Rest repository to retrieve Login statistics.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@Component(StatisticsSupportRest.CATEGORY + "." + LoginStatisticsRest.NAME_PLURAL)
public class LoginStatisticsRestRepository extends DSpaceRestRepository<LoginStatisticsRest, UUID> {

    public static final DateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd");

    @Autowired
    private LoginStatisticsService loginStatisticsService;

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public LoginStatisticsRest findOne(Context context, UUID id) {
        return loginStatisticsService.find(context, id.toString())
            .map(loginStatistics -> (LoginStatisticsRest) converter.toRest(loginStatistics, utils.obtainProjection()))
            .orElse(null);
    }

    @SearchRestMethod(name = "byDateRange")
    @PreAuthorize("hasAuthority('ADMIN')")
    public Page<LoginStatisticsRest> findByDateRange(Pageable pageable,
        @Parameter(value = "startDate") String startDate,
        @Parameter(value = "endDate") String endDate) {

        Context context = obtainContext();

        List<LoginStatistics> statistics = loginStatisticsService.findByDateRange(context,
            parseDate(startDate), parseDate(endDate), toIntExact(pageable.getPageSize()));

        return converter.toRestPage(statistics, pageable, statistics.size(), utils.obtainProjection());
    }

    @Override
    public Page<LoginStatisticsRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not allowed!", "findAll");
    }

    private Date parseDate(String date) {
        try {
            return isNotBlank(date) ? DATE_FORMATTER.parse(date) : null;
        } catch (ParseException e) {
            throw new UnprocessableEntityException("The provided date has not a valid format. Expected: yyyy-MM-dd", e);
        }
    }

    @Override
    public Class<LoginStatisticsRest> getDomainClass() {
        return LoginStatisticsRest.class;
    }

}
