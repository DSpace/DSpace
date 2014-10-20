/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.authorprofile;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.aspect.discovery.SidebarFacetsTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.AuthorProfile;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.discovery.AuthorProfileUtil;
import org.dspace.discovery.DiscoverFilterQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;

/**
 *
 * @author Roeland Dillen (roeland at atmire dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class SidebarFacetsAuthorPageTransformer extends SidebarFacetsTransformer
{
    protected AuthorProfile authorProfile;
    private static final Logger log = Logger.getLogger(SidebarFacetsAuthorPageTransformer.class);

    @Override
    public void setup(SourceResolver resolver, Map objectModel, String src, Parameters parameters) throws ProcessingException, SAXException, IOException {
        super.setup(resolver, objectModel, src, parameters);
        //context.getCurrentUser()
        String synonym = parameters.getParameter("synonym", null);
        Request request = ObjectModelHelper.getRequest(objectModel);
        String firstName = request.getParameter("name.first");
        String lastName = request.getParameter("name.last");
        String author = request.getParameter("author");
        boolean firstLast = request.getParameters().containsKey("name.first") && request.getParameters().containsKey("name.last");
        try {
            if (firstLast) {
                authorProfile = AuthorProfileUtil.findAuthorProfile(context, firstName, lastName);
            } else if (synonym != null)
                authorProfile = AuthorProfileUtil.findAuthorProfileBySynonym(context, synonym);
            else

            {
                authorProfile = AuthorProfileUtil.findAuthorProfile(context, author.replaceAll("\\u000D\\u000A", "\\u000A"));
            }

        } catch (Exception e) {
            log.error(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        try {
            ArrayList<String> display = new ArrayList<String>();
            for (DCValue value : authorProfile.getMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "author", null, Item.ANY)) {
                display.add(value.value);
            }

            if (queryResults != null) {
                java.util.List<DiscoverResult.FacetResult> authorFacets = AuthorProfileUtil.getAuthorFacets(context, queryResults);
                for (DiscoverResult.FacetResult apFR : authorFacets) {
                    pageMeta.addMetadata("authorprofile", apFR.getDisplayedValue(), null, false).addContent(apFR.getAsFilterQuery().replace("author_filter:", ""));
                    if (display.contains(apFR.getDisplayedValue())) {
                        pageMeta.addMetadata("currentauthorprofile", apFR.getDisplayedValue(), null, false).addContent(apFR.getAsFilterQuery().replace("author_filter:", ""));
                    }
                }
            }

        } catch (SearchServiceException e) {
            log.error("Error while decorating authors names with author-profile link", e);
        }
    }

    @Override
    protected String getFacetFilterUrl(DSpaceObject dso, DiscoverySearchFilterFacet field, String filterQuery, String filterType, String paramsQuery) throws UIException, SQLException {
        StringBuilder path=new StringBuilder(contextPath +
                (dso == null ? "" : "/handle/" + dso.getHandle()) +
                "/discover?" +
                "filtertype_0=" + field.getIndexFieldName() +
                "&filter_relational_operator_0=" + filterType +
                "&filter_0=" + encodeForURL(filterQuery));
        int ac=1;
        for(DiscoverFilterQuery dsc: AuthorProfileUtil.toFilterQueryObjects(authorProfile, context)){
            path.append("&filtertype_").append(ac).append("=").append(dsc.getField())
                    .append("&filter_relational_operator_").append(ac).append("=").append("oneof")
                    .append("&filter_").append(ac).append("=").append(encodeForURL(dsc.getDisplayedValue()));
                    ac++;
        }
        return path.toString();

    }
}
