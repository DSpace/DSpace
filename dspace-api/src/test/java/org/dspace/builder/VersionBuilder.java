/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.builder;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.versioning.Version;
import org.dspace.versioning.service.VersioningService;

/**
 * Builder to construct Version objects
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class VersionBuilder extends AbstractBuilder<Version, VersioningService> {

    private static final Logger log = LogManager.getLogger(VersionBuilder.class);

    private Version version;

    protected VersionBuilder(Context context) {
        super(context);
    }

    public static VersionBuilder createVersion(Context context, Item item, String summary) {
        VersionBuilder builder = new VersionBuilder(context);
        return builder.create(context, item, summary);
    }

    private VersionBuilder create(Context context, Item item, String summary) {
        try {
            this.context = context;
            if (StringUtils.isNotBlank(summary)) {
                this.version = getService().createNewVersion(context, item, summary);
            } else {
                this.version = getService().createNewVersion(context, item);
            }
        } catch (Exception e) {
            log.error("Error in VersionBuilder.create(..), error: ", e);
        }
        return this;
    }

    @Override
    public Version build() throws SQLException, AuthorizeException {
        try {
            getService().update(context, version);
            context.dispatchEvents();
            indexingService.commit();
        } catch (Exception e) {
            log.error("Error in VersionBuilder.build(), error: ", e);
        }
        return version;
    }

    @Override
    public void delete(Context context, Version version) throws Exception {
        if (Objects.nonNull(version)) {
            getService().delete(context, version);
        }
    }

    @Override
    protected VersioningService getService() {
        return versioningService;
    }

    @Override
    public void cleanup() throws Exception {
        delete(version);
    }

    public void delete(Version version) throws Exception {
        try (Context context = new Context()) {
            context.turnOffAuthorisationSystem();
            context.setDispatcher("noindex");
            Version attachedTab = context.reloadEntity(version);
            if (attachedTab != null) {
                getService().delete(context, attachedTab);
            }
            context.complete();
        }
        indexingService.commit();
    }

    public static void delete(Integer id)
            throws SQLException, IOException, SearchServiceException {
        try (Context context = new Context()) {
            context.turnOffAuthorisationSystem();
            Version version = versioningService.getVersion(context, id);
            if (version != null) {
                versioningService.delete(context, version);
            }
            context.complete();
        }
        indexingService.commit();
    }

}