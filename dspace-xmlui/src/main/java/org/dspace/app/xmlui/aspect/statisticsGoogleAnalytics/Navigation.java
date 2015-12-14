/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.statisticsGoogleAnalytics;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Options;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Constants;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;

/**
 * User: Robin Taylor
 * Date: 11/07/2014
 * Time: 13:23
 *
 * Navigation Elements for viewing Google Analytics Statistics for Items.
 *
 */
public class Navigation extends AbstractDSpaceTransformer implements CacheableProcessingComponent {

    private static final Message T_statistics_head = message("xmlui.statisticsGoogleAnalytics.Navigation.title");
    private static final Message T_statistics_view = message("xmlui.statisticsGoogleAnalytics.Navigation.usage.view");

    protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();

    public Serializable getKey() {
        return null;
    }

    /**
     * Generate the cache validity object.
     *
     * The cache is always valid.
     */
    public SourceValidity getValidity() {
        return NOPValidity.SHARED_INSTANCE;
    }


    /**
     * Add the statistics aspect navigational options.
     */
    public void addOptions(Options options) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException
    {
        /* Create skeleton menu structure to ensure consistent order between aspects,
         * even if they are never used
         */
        options.addList("browse");
        options.addList("account");
        options.addList("context");
        options.addList("administrative");
        List statistics = options.addList("statistics");

        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        boolean displayUsageStats = displayStatsType(context, dso);

        if (dso != null && dso.getHandle() != null) {
            if (dso.getType() == Constants.ITEM) {
                if (displayUsageStats) {
                    statistics.setHead(T_statistics_head);
                    statistics.addItemXref(contextPath + "/handle/" + dso.getHandle() + "/google-stats", T_statistics_view);
                }
            }
        }

    }

    protected boolean displayStatsType(Context context, DSpaceObject dso) throws SQLException {
        ConfigurationService cs = DSpaceServicesFactory.getInstance().getConfigurationService();
        return !cs.getPropertyAsType("google-analytics.authorization.admin.usage", Boolean.TRUE) || authorizeService.isAdmin(context, dso);

    }
}

