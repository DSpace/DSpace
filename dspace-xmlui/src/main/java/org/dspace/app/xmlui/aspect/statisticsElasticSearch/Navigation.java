/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.statisticsElasticSearch;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.element.Options;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.xml.sax.SAXException;

import java.io.Serializable;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Navigation Elements for viewing Elastic Statistics for Items.
 *
 */
public class Navigation extends AbstractDSpaceTransformer implements CacheableProcessingComponent {

    private static final Message T_statistics_head = message("xmlui.statistics.Navigation.title");
    private static final Message T_statistics_view = message("xmlui.statistics.Navigation.usage-Elasticsearch.view");

    public Serializable getKey() {
        //TODO: DO THIS
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
        if(dso != null && dso.getHandle() != null){
            statistics.setHead(T_statistics_head);
            statistics.addItemXref(contextPath + "/handle/" + dso.getHandle() + "/stats", T_statistics_view);

        }
    }
}
