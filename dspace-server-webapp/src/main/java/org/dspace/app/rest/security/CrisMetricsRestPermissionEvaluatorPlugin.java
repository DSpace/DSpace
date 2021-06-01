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
import org.dspace.app.rest.model.CrisMetricsRest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.metrics.CrisItemMetricsService;
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
    private CrisItemMetricsService crisItemMetricsService;

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

            Item item = itemFromMetricId(context, targetId.toString());

            if (Objects.isNull(item)) {
                // this is needed to allow 404 instead than 403
                return true;
            }

            return authorizeService.authorizeActionBoolean(context, item, Constants.READ);

        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

    private Item itemFromMetricId(Context context, String target) throws SQLException {

        if (crisItemMetricsService.isEmbeddableMetricId(target.toString())) {
            int indexOf = target.indexOf(AbstractEmbeddableMetricProvider.DYNAMIC_ID_SEPARATOR);
            if (indexOf != -1) {
                String uuid = target.substring(0, indexOf);
                return itemService.find(context, UUID.fromString(uuid));
            } else {
                return null;
            }

        } else {

            CrisMetrics metric = crisItemMetricsService.find(context, target.toString());
            return metric != null ? metric.getResource() : null;
        }
    }

}