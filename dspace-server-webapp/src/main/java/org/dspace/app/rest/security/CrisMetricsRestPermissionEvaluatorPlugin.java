/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.metrics.CrisMetrics;
import org.dspace.app.metrics.service.CrisMetricsService;
import org.dspace.app.rest.model.CrisMetricsRest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.metrics.embeddable.impl.AbstractEmbeddableMetricProvider;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * {@link RestPermissionEvaluatorPlugin} class that evaluate READ, WRITE and DELETE permissions over a CrisMetrics
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
@Component
public class CrisMetricsRestPermissionEvaluatorPlugin extends RestObjectPermissionEvaluatorPlugin {

    private static final Logger log = LoggerFactory.getLogger(CrisMetricsRestPermissionEvaluatorPlugin.class);

    @Autowired
    AuthorizeService authorizeService;

    @Autowired
    private RequestService requestService;

    @Autowired
    private EPersonService ePersonService;

    @Autowired
    private CrisMetricsService crisMetricsService;

    @Autowired
    private ItemService itemService;

    @Override
    public boolean hasDSpacePermission(Authentication authentication, Serializable targetId, String targetType,
            DSpaceRestPermission permission) {

        DSpaceRestPermission restPermission = DSpaceRestPermission.convert(permission);
        if (!DSpaceRestPermission.READ.equals(restPermission)
            || !StringUtils.equalsIgnoreCase(targetType, CrisMetricsRest.NAME)) {
            return false;
        }

        Request request = requestService.getCurrentRequest();
        Context context = ContextUtil.obtainContext(request.getServletRequest());
        EPerson ePerson = null;

        try {
            ePerson = ePersonService.findByEmail(context, (String) authentication.getPrincipal());

            // anonymous user
            if (ePerson == null) {
                return false;
            }

            String target = targetId.toString();

            if (isEmbedded(target)) {
                return evaluateEmbeddedMetric(target, context);
            }
            return evaluateStoredMetric(target, context);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

    private boolean evaluateEmbeddedMetric(String target, Context context) throws SQLException {
        String uuid = target.substring(0, target.indexOf(AbstractEmbeddableMetricProvider.DYNAMIC_ID_SEPARATOR));
        Item item = itemService.find(context, UUID.fromString(uuid));

        if (Objects.isNull(item)) {
            // this is needed to allow 404 instead than 403
            return true;
        }
        return authorizeService.authorizeActionBoolean(context, item, Constants.READ);
    }

    private boolean isEmbedded(Serializable targetId) {
        return targetId.toString().contains(AbstractEmbeddableMetricProvider.DYNAMIC_ID_SEPARATOR);
    }

    private boolean evaluateStoredMetric(String target, Context context) throws SQLException {
        Integer crisMetricId = Integer.parseInt(target);
        CrisMetrics metric = crisMetricsService.find(context, crisMetricId);
        if (Objects.isNull(metric)) {
            // this is needed to allow 404 instead than 403
            return true;
        }
        return authorizeService.authorizeActionBoolean(context, metric.getResource(), Constants.READ);
    }

}