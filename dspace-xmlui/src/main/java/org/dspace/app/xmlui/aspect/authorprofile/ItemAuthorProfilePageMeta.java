/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.authorprofile;


import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.discovery.*;
import org.dspace.discovery.configuration.DiscoveryConfigurationParameters;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 *
 * @author Roeland Dillen (roeland at atmire dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class ItemAuthorProfilePageMeta extends AbstractDSpaceTransformer {

    private static final Logger log = Logger.getLogger(ItemAuthorProfilePageMeta.class);

    @Override
    public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException, SQLException, IOException, AuthorizeException {
        try {
            Item dso = (Item) HandleUtil.obtainHandle(objectModel);
            SearchService search = SearchUtils.getSearchService();
            DiscoverQuery dq = new DiscoverQuery();
            DiscoverFacetField dff = new DiscoverFacetField("author", DiscoveryConfigurationParameters.TYPE_TEXT, -1, DiscoveryConfigurationParameters.SORT.VALUE);
            dq.addFacetField(dff);
            dq.setFacetMinCount(1);
            dq.setQuery("search.resourceid:" + dso.getID() + " AND search.resourcetype:" + dso.getType());
            DiscoverResult discoverResult = search.search(context, dq);
            List<DiscoverResult.FacetResult> authorFacets = AuthorProfileUtil.getAuthorFacets(context, discoverResult);
            for (DiscoverResult.FacetResult facetResult : authorFacets) {
                pageMeta.addMetadata("authorprofile", facetResult.getDisplayedValue(), null, false).addContent(facetResult.getAsFilterQuery().replace("author_filter:", ""));
            }
        } catch (SearchServiceException e) {
            log.error(e.getMessage(), e);
        }


    }
}
