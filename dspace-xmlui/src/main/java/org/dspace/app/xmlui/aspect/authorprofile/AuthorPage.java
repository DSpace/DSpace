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
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.aspect.discovery.AbstractSearch;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Collection;
import org.dspace.content.*;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.discovery.*;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoverySortFieldConfiguration;
import org.dspace.utils.DSpace;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 *
 * @author Roeland Dillen (roeland at atmire dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class AuthorPage extends AbstractSearch{

    private static final Message T_dspace_home = message("xmlui.general.dspace_home");
    private static final Message T_trail = message("xmlui.AuthorProfile.trail");
    private AuthorProfile authorProfile = null;
    private String firstName;
    private String lastName;
    private String fullName;

    private SearchService searchService = null;
    private String author = null;

    private static final Logger log = Logger.getLogger(AuthorPage.class);

    @Override
    protected DSpaceObject getScope() throws SQLException {
        return authorProfile;
    }

    @Override
    public void recycle() {
        super.recycle();

        authorProfile = null;
        firstName = null;
        lastName = null;
        fullName = null;
        searchService = null;
        author = null;
    }

    private static final Message T_PRIVATE_PROFILE = message("xmlui.ArtifactBrowser.RestrictedItem.para_resource");
    private static final Message T_PROFILE_HEAD = message("xmlui.authorprofile.artifactbrowser.AuthorProfile.head");
    private static final Message T_HEAD_RESOURCE = message("xmlui.ArtifactBrowser.RestrictedItem.head_resource");

    @Override
    public void setup(SourceResolver resolver, Map objectModel, String src, Parameters parameters) throws ProcessingException, SAXException, IOException {
        super.setup(resolver, objectModel, src, parameters);
        String synonym = parameters.getParameter("synonym", null);
        Request request = ObjectModelHelper.getRequest(objectModel);
        firstName = request.getParameter("name.first");
        lastName = request.getParameter("name.last");
        author = request.getParameter("author");
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

            lastName = authorProfile.getMetadataFirstValue(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "name", "last", Item.ANY);
            firstName = authorProfile.getMetadataFirstValue(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "name", "first", Item.ANY);
            fullName = lastName + ", " + firstName;
        } catch (Exception e) {
            log.error(e);
            fullName = "";
            throw new RuntimeException(e);
        }
    }


    protected SearchService getSearchService() {
        if (searchService == null) {
            DSpace dspace = new DSpace();

            org.dspace.kernel.ServiceManager manager = dspace.getServiceManager();

            searchService = manager.getServiceByName(SearchService.class.getName(), SearchService.class);
        }
        return searchService;

    }


    @Override
    public void addBody(Body body) throws SAXException, WingException, SQLException, IOException, AuthorizeException {
        if (AuthorizeManager.authorizeActionBoolean(context, authorProfile, Constants.READ)) {
            showBody(body);
        } else {
            Division division = body.addDivision("authorization");
            division.setHead(T_HEAD_RESOURCE);
            division.addPara(T_PRIVATE_PROFILE);
        }
    }

    @Override
    protected String getBasicUrl() throws SQLException {
        return null;
    }

    @Override
    protected void addResults(Division results) throws WingException {
        ReferenceSet referenceSet = null;

        for (DSpaceObject resultDso : queryResults.getDspaceObjects()) {
            if (resultDso instanceof Community || resultDso instanceof Collection) {
                if (referenceSet == null) {
                    referenceSet = results.addReferenceSet("search-results-repository",
                            ReferenceSet.TYPE_SUMMARY_LIST, null, "repository-search-results");
                    // Set a heading showing that we will be listing containers that matched:
                    referenceSet.setHead(T_result_head_2);
                }
                if (resultDso != null) {
                    referenceSet.addReference(resultDso);
                }
            }
        }


        // Put in palce top level referenceset
        referenceSet = results.addReferenceSet("search-results-repository",
                ReferenceSet.TYPE_SUMMARY_LIST, null, "repository-search-results");

        List stats = results.addList("search-results-stats");
        for (DSpaceObject resultDso : queryResults.getDspaceObjects()) {
            if (resultDso instanceof Item) {
                referenceSet.addReference(resultDso);

            }
        }
    }

    @Override
    protected void setResultHead(Division results) throws SQLException, WingException {
        //results.setHead(T_RESULTS_HEAD.parameterize(fullName));
    }

    @Override
    protected String getParameterSortBy() {
        String s = ObjectModelHelper.getRequest(objectModel).getParameter("sort_by");
        return s;
    }

    public void showBody(Body body) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {

        Request request = ObjectModelHelper.getRequest(objectModel);
        Division wrapper = body.addDivision("author-profile-wrapper", " clearfix");
        Division div = wrapper.addDivision("author-profile", " clearfix");
        div.setHead(fullName);
//        div.setHead(T_PROFILE_HEAD.parameterize(fullName));

        Division search = body.addDivision("search", "primary");
        search.setHead(T_PROFILE_HEAD.parameterize(fullName));

        div.addReferenceSet("authors", ReferenceSet.TYPE_SUMMARY_VIEW).addReference(authorProfile);


        if (AuthorizeManager.isAdmin(context)) {
            div.addPara().addXref(contextPath + "/admin/authorprofile?authorProfileId=" + authorProfile.getID(), message("xmlui.authorprofile.artifactbrowser.AuthorProfile.edit"));
        }
        buildMainForm(search);

        try {
            buildSearchResultsDivision(search);
        } catch (SearchServiceException e) {

            log.error(e.getMessage(), e);
        }
    }

    protected Division buildMainForm(Division searchDiv) throws WingException, SQLException {
        Request request = ObjectModelHelper.getRequest(objectModel);

        //We set our action to context path, since the eventual action will depend on which url we click on
        Division mainForm = searchDiv.addInteractiveDivision("main-form", getBasicUrl(), Division.METHOD_GET, "discovery-main-form");

        if (request.getParameter("name.first") != null && request.getParameter("name.last") != null) {
            mainForm.addHidden("name.first").setValue(request.getParameter("name.first"));
            mainForm.addHidden("name.last").setValue(request.getParameter("name.last"));
        } else {
            mainForm.addHidden("author").setValue(request.getParameter("author"));
        }

        String query = getQuery();
        mainForm.addHidden("query").setValue(query);

        Map<String, String[]> fqs = getParameterFilterQueries();
        if (fqs != null)
        {
            for (String parameter : fqs.keySet())
            {
                String[] values = fqs.get(parameter);
                if(values != null)
                {
                    for (String value : values)
                    {
                        mainForm.addHidden(parameter).setValue(value);
                    }
                }
            }
        }

        mainForm.addHidden("rpp").setValue(getParameterRpp());
        Hidden sort_by = mainForm.addHidden("sort_by");
        if(!StringUtils.isBlank(request.getParameter("sort_by")))
        {
            sort_by.setValue(request.getParameter("sort_by"));
        }else{
            DiscoverySortFieldConfiguration defaultSortConfig = getConfiguration().getSearchSortConfiguration().getDefaultSort();
            sort_by.setValue(SearchUtils.getSearchService().toSortFieldIndex(defaultSortConfig.getMetadataField(), defaultSortConfig.getType()));
        }

        Hidden order = mainForm.addHidden("order");
        if(getParameterOrder() != null)
        {
            order.setValue(request.getParameter("order"));
        }else{
            order.setValue(getConfiguration().getSearchSortConfiguration().getDefaultSortOrder().toString());
        }
        if(!StringUtils.isBlank(request.getParameter("page")))
        {
            mainForm.addHidden("page").setValue(request.getParameter("page"));
        }
        //Optional redirect url !
        mainForm.addHidden("redirectUrl");


        return mainForm;
    }

    @Override
    protected String getQuery() {
        StringBuilder sb = new StringBuilder();
        java.util.List<String> qs = AuthorProfileUtil.toFilterQuery(context, authorProfile);
        int i = 0;
        for (String q : qs) {
            sb.append(q);
            if (i < (qs.size() - 1)) {
                sb.append(" AND ");
            }
            i++;
        }
        return sb.toString();
    }

    /**
     * Generate a url to the simple search url.
     */
    protected String generateURL(Map<String, String> parameters)
            throws UIException {
        String query = getQuery();
        if (!"".equals(query)) {
            parameters.put("query", encodeForURL(query));
        }

        if (parameters.get("name.first") == null) {
            parameters.put("name.first", firstName);
        }
        if (parameters.get("name.last") == null) {
            parameters.put("name.last", lastName);
        }

        if (parameters.get("page") == null) {
            parameters.put("page", String.valueOf(getParameterPage()));
        }

        if (parameters.get("rpp") == null) {
            parameters.put("rpp", String.valueOf(getParameterRpp()));
        }


        if (parameters.get("group_by") == null) {
            parameters.put("group_by", String.valueOf(this.getParameterGroup()));
        }

        if (parameters.get("sort_by") == null && getParameterSortBy() != null) {
            parameters.put("sort_by", String.valueOf(getParameterSortBy()));
        }

        if (parameters.get("order") == null && getParameterOrder() != null) {
            parameters.put("order", getParameterOrder());
        }

        if (parameters.get("etal") == null) {
            parameters.put("etal", String.valueOf(getParameterEtAl()));
        }
        if (parameters.get("scope") == null && getParameterScope() != null) {
            parameters.put("scope", getParameterScope());
        }

        return AbstractDSpaceTransformer.generateURL("author-page", parameters);
    }

    @Override
    protected void addToValidity(DSpaceValidity validity) throws Exception{
        super.addToValidity(validity);

        HashSet<String> display = new HashSet<String>();
        for (DCValue value : authorProfile.getMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "author", null, Item.ANY)) {
            display.add(value.value);
        }

        for (DiscoverResult.FacetResult apFR : AuthorProfileUtil.getAuthorFacets(context, queryResults)) {
            validity.add("authorprofile"+ apFR.getDisplayedValue()+apFR.getAsFilterQuery().replace("author_filter:", ""));
            if (display.contains(apFR.getDisplayedValue())) {
                validity.add("currentauthorprofile"+apFR.getDisplayedValue()+apFR.getAsFilterQuery().replace("author_filter:", ""));
            }
        }
    }

    @Override
    public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        super.addPageMeta(pageMeta);
        if (AuthorizeManager.authorizeActionBoolean(context, authorProfile, Constants.READ)) {
            pageMeta.addMetadata("title").addContent(lastName + ", " + firstName);
        } else {
            pageMeta.addMetadata("title").addContent(message("xmlui.ArtifactBrowser.RestrictedItem.title"));
        }

        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrail().addContent(T_trail);
    }

    @Override
    protected DiscoveryConfiguration getConfiguration() throws SQLException {
        return SearchUtils.getDiscoveryConfiguration(authorProfile);
    }
}
