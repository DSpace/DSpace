/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.sql.SQLException;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.VersionHistoryRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.core.Context;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.dspace.versioning.VersionHistory;
import org.dspace.versioning.service.VersionHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the Converter that takes care of the conversion between {@link VersionHistory} and {@link VersionHistoryRest}
 */
@Component
public class VersionHistoryConverter implements DSpaceConverter<VersionHistory, VersionHistoryRest> {

    private static final Logger log = LogManager.getLogger(VersionHistoryConverter.class);

    @Autowired
    private RequestService requestService;

    @Autowired
    private VersionHistoryService versionHistoryService;

    @Override
    public VersionHistoryRest convert(VersionHistory modelObject, Projection projection) {
        Context context = getContext();
        VersionHistoryRest versionHistoryRest = new VersionHistoryRest();
        try {
            versionHistoryRest.setId(modelObject.getID());
            if (Objects.nonNull(context.getCurrentUser())) {
                if (versionHistoryService.canSeeDraftVersion(context, modelObject)) {
                    versionHistoryRest.setDraftVersion(modelObject.hasDraftVersion());
                }
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return versionHistoryRest;
    }

    @Override
    public Class<VersionHistory> getModelClass() {
        return VersionHistory.class;
    }

    /**
     * Retrieves the context from the request
     * If not request is found, will return null
     * @return  The context retrieved form the current request or null when no context
     */
    private Context getContext() {
        Request currentRequest = requestService.getCurrentRequest();
        if (currentRequest != null) {
            return ContextUtil.obtainContext(currentRequest.getHttpServletRequest());
        }
        return null;
    }

}
