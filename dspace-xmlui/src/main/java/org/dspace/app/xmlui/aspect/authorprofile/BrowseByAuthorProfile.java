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
import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.excalibur.source.SourceValidity;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.aspect.discovery.AbstractDiscoveryTransformer;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.discovery.*;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationParameters;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Roeland Dillen (roeland at atmire dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class BrowseByAuthorProfile extends AbstractDiscoveryTransformer implements CacheableProcessingComponent {

    private static final Logger log = Logger.getLogger(BrowseByAuthorProfile.class);
    protected static final Message T_BROWSE_AUTHOR_HEAD = message("xmlui.authorprofile.artifactbrowser.AuthorProfile.list.head");
    protected static final Message T_starts_with = message("xmlui.authorprofile.artifactbrowser.AuthorProfile.starts_with");
    protected static final Message T_dspace_home = message("xmlui.general.dspace_home");
    protected static final Message T_AuthorProfile_help = message("xmlui.authorprofile.artifactbrowser.AuthorProfile.help");

    @Override
    protected String getURL() {
        return "browse-by";
    }

    private DSpaceValidity validity = null;

    protected void performSearch() {
        setupQueryArgs();
        try {
            queryResults = SearchUtils.getSearchService().search(context, queryArgs);

        } catch (SearchServiceException e) {
            log.error("Error while searching for author profiles to browse", e);

        }
    }

    @Override
    public void setup(SourceResolver resolver, Map objectModel, String src, Parameters parameters) throws ProcessingException, SAXException, IOException {
        super.setup(resolver, objectModel, src, parameters);
        performSearch();

    }

    /**
     * Add a page title and trail links
     */
    public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException, SQLException, IOException, AuthorizeException {
        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        pageMeta.addTrail().addContent(T_BROWSE_AUTHOR_HEAD);

        // Add the page title
        pageMeta.addMetadata("title").addContent(T_BROWSE_AUTHOR_HEAD);
    }

    @Override
    public void addBody(Body body) throws SAXException, WingException, SQLException, IOException, AuthorizeException, ProcessingException {
        Division main = body.addDivision("browse-by-author-wrapper");
        main.setHead(T_BROWSE_AUTHOR_HEAD);
        main.addPara(T_AuthorProfile_help);
        addJumpList(main);
        buildSearchControls(main);

        Division results = main.addDivision("browse-by-author-results","browse-results");
        addFacetResults(results);
    }

    protected void renderFacetField(String facetField, Table singleTable, List<String> filterQueries, DiscoverResult.FacetResult value) throws SQLException, WingException, UnsupportedEncodingException {
        String displayedValue = value.getDisplayedValue();
        String filterQuery = value.getAsFilterQuery();

        Cell cell = singleTable.addRow().addCell("","data","clearfix");


        //No use in selecting the same filter twice
        if (filterQueries.contains(filterQuery)) {
            cell.addContent(displayedValue + " (" + value.getCount() + ")");
        } else {
            //Add the basics
            Map<String, String> urlParams = new HashMap<String, String>();
            urlParams.putAll(getFilteredParameters());
            String url = "author-page?author=" + URLEncoder.encode(filterQuery.replace("author_filter:", ""), "UTF-8");
            cell.addXref(url, modifyValue(value));
            cell.addXref(url, " ", "author-profile");
        }
    }

    @Override
    protected void setupQueryArgs() {
        queryArgs = new DiscoverQuery();
        queryArgs.setQuery("search.resourcetype:" + Constants.AUTHOR_PROFILE);
        queryArgs.setDSpaceObjectFilter(Constants.AUTHOR_PROFILE);
        if (getFilteredParameters().containsKey(STARTS_WITH)) {
            queryArgs.addFacetField(new DiscoverFacetField("author", DiscoveryConfigurationParameters.TYPE_TEXT, getParameterRpp() + 1, DiscoveryConfigurationParameters.SORT.VALUE, getFilteredParameters().get(STARTS_WITH).toLowerCase()));
        } else {
            queryArgs.addFacetField(new DiscoverFacetField("author", DiscoveryConfigurationParameters.TYPE_TEXT, getParameterRpp() + 1, DiscoveryConfigurationParameters.SORT.VALUE));
        }
        queryArgs.setFacetMinCount(1);
        queryArgs.setMaxResults(0);
        if (getFilteredParameters().containsKey(OFFSET)) {
            queryArgs.setFacetOffset(Integer.valueOf(getFilteredParameters().get(OFFSET)));
        } else {
            queryArgs.setFacetOffset(0);
        }
    }

    @Override
    protected void buildOrderControl(Item controlsItem, DiscoveryConfiguration discoveryConfiguration, Message orderMessage) throws WingException {
        //suppress order control
    }

    @Override
    protected String getField() {
        return "author";
    }

    @Override
    protected String generateFacetURL(DiscoverResult.FacetResult value) throws UnsupportedEncodingException {
        return "discover";
    }

    @Override
    protected String modifyValue(DiscoverResult.FacetResult value) {
        return value.getDisplayedValue();
    }

    @Override
    protected void buildSortControl(Item controlsItem, DiscoveryConfiguration discoveryConfiguration, Message sortByMessage) throws WingException {
        //suppress sort control
    }

    @Override
    public void recycle() {
        super.recycle();
        queryResults = null;
        queryArgs = null;
        validity = null;
    }

    @Override
    public Serializable getKey() {
        return "browse-by-authorprofile";
    }

    @Override
    public SourceValidity getValidity() {
        if (this.validity == null) {
            DSpaceValidity val = new DSpaceValidity();
            performSearch();
            try {
                if(queryResults!=null){
                    for(DSpaceObject dso:queryResults.getDspaceObjects()){
                        val.add(dso);
                    }
                    for(DiscoverResult.FacetResult fr:queryResults.getFacetResult("author")){
                        val.add(fr.getDisplayedValue()+fr.getCount());
                    }
                }
                for(Map.Entry<String,String> param:getFilteredParameters().entrySet()){
                    val.add(param.getKey()+":"+param.getValue());
                }
                this.validity=val.complete();
            } catch (SQLException e) {
               return null;
            }
        }
        return this.validity;
    }

    @Override
    protected void addJumpList(Division div) throws SQLException, WingException, UnsupportedEncodingException {

        String action;

        action = contextPath + "/"+getURL();


        Division jump = div.addInteractiveDivision("filter-navigation", action,
                Division.METHOD_POST, "secondary navigation");


        // Add all the query parameters as hidden fields on the form
        for(Map.Entry<String, String> param : getFilteredParameters(new String[]{STARTS_WITH}).entrySet()){
            jump.addHidden(param.getKey()).setValue(param.getValue());
        }
        List<String> filterQueries = getParameterFilterQueries();
        for (String filterQuery : filterQueries) {
            jump.addHidden("fq").setValue(filterQuery);
        }

        //We cannot create a filter for dates
        // Create a clickable list of the alphabet
        org.dspace.app.xmlui.wing.element.List jumpList = jump.addList("jump-list", org.dspace.app.xmlui.wing.element.List.TYPE_SIMPLE, "alphabet");

        //Create our basic url
        String basicUrl = generateURL(getURL()+"?", getFilteredParameters(new String[]{STARTS_WITH, OFFSET}));
        //Add any filter queries
        basicUrl = addFilterQueriesToUrl(basicUrl);

//            jumpList.addItemXref(generateURL("browse", letterQuery), "0-9");
        for (char c = 'A'; c <= 'Z'; c++)
        {
            String linkUrl = basicUrl + "&" + STARTS_WITH +  "=" + Character.toString(c).toLowerCase();
            jumpList.addItemXref(linkUrl, Character
                    .toString(c));
        }

        // Create a free text field for the initial characters
        Para jumpForm = jump.addPara();
        jumpForm.addContent(T_starts_with);
        jumpForm.addText("starts_with").setHelp(T_starts_with_help);

        jumpForm.addButton("submit").setValue(T_go);
    }
}
