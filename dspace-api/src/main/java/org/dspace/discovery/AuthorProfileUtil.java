/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import org.apache.solr.client.solrj.util.ClientUtils;
import org.dspace.content.AuthorProfile;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.configuration.DiscoveryConfigurationParameters;
import org.dspace.utils.DSpace;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Class that can be used as a bean to use a certain metadata field in spring
 *
 * @author Roeland Dillen (roeland at atmire dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class AuthorProfileUtil {


    public static long countAuthorProfiles(Context context) throws SearchServiceException {
        DiscoverQuery dq = new DiscoverQuery();
        dq.setDSpaceObjectFilter(Constants.AUTHOR_PROFILE);

        return SearchUtils.getSearchService().search(context, dq, true).getTotalSearchResults();
    }

    public static AuthorProfile findAuthorProfile(Context context, String authorFilter) throws Exception {
        AuthorProfile authorProfile;

        DiscoverQuery dq = new DiscoverQuery();

        dq.setQuery("author_filter:(" +  ClientUtils.escapeQueryChars(com.ibm.icu.text.Normalizer.normalize(authorFilter.toLowerCase(), com.ibm.icu.text.Normalizer.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "") + SolrServiceImpl.FILTER_SEPARATOR + authorFilter)+")");
        dq.setDSpaceObjectFilter(Constants.AUTHOR_PROFILE);

        DiscoverResult discoverResult = SearchUtils.getSearchService().search(context, dq, true);
        if (discoverResult.getDspaceObjects().size() > 0) {
            authorProfile = (AuthorProfile) discoverResult.getDspaceObjects().get(0);
        } else throw new NullPointerException("author profile is null");
        return authorProfile;
    }

    public static AuthorProfile findAuthorProfileByVariant(Context context, String authorFilterIn) throws Exception {
        AuthorProfile authorProfile = null;
        String authorFilter=authorFilterIn.replaceAll("\\s+"," ");
        DiscoverQuery dq = new DiscoverQuery();
        dq.setQuery("author_filter:" + ClientUtils.escapeQueryChars(authorFilter.toLowerCase()+SolrServiceImpl.FILTER_SEPARATOR+authorFilter));
        dq.setDSpaceObjectFilter(Constants.AUTHOR_PROFILE);

        DiscoverResult discoverResult = SearchUtils.getSearchService().search(context, dq, true);
        if (discoverResult.getDspaceObjects().size() > 0) {
            authorProfile = (AuthorProfile) discoverResult.getDspaceObjects().get(0);
        }
        return authorProfile;
    }




    public static AuthorProfile findAuthorProfileBySynonym(Context context, String authorFilter) throws Exception {
        AuthorProfile authorProfile;

        DiscoverQuery dq = new DiscoverQuery();
        dq.setQuery("name.synonym:" + authorFilter);
        dq.setDSpaceObjectFilter(Constants.AUTHOR_PROFILE);

        DiscoverResult discoverResult = SearchUtils.getSearchService().search(context, dq, true);
        if (discoverResult.getDspaceObjects().size() > 0) {
            authorProfile = (AuthorProfile) discoverResult.getDspaceObjects().get(0);
        } else throw new NullPointerException("author profile is null");
        return authorProfile;
    }

    public static AuthorProfile findAuthorProfile(Context context, String firstname, String lastName) throws Exception {
        AuthorProfile authorProfile;

        DiscoverQuery dq = new DiscoverQuery();
        dq.setQuery("name.first:" + ClientUtils.escapeQueryChars(firstname) + " AND name.last:" + ClientUtils.escapeQueryChars(lastName));
        dq.setDSpaceObjectFilter(Constants.AUTHOR_PROFILE);

        DiscoverResult discoverResult = SearchUtils.getSearchService().search(context, dq, true);
        if (discoverResult.getDspaceObjects().size() > 0) {
            authorProfile = (AuthorProfile) discoverResult.getDspaceObjects().get(0);
        } else throw new IllegalArgumentException("No author profile found for: " + lastName +", " + firstname);
        return authorProfile;
    }

    public static List<String> toFilterQuery(Context context, AuthorProfile authorProfile) {
        LinkedList<String> queries=new LinkedList<String>();
        StringBuilder filterQueries = new StringBuilder();
        String lastName = authorProfile.getMetadataFirstValue(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "name", "last", Item.ANY);
        String firstName = authorProfile.getMetadataFirstValue(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "name", "first", Item.ANY);
        String fullName = lastName + ", " + firstName;

        filterQueries.append("author_filter:(");
        //filterQueries.append("\"");
        filterQueries.append(ClientUtils.escapeQueryChars(com.ibm.icu.text.Normalizer.normalize(lastName.toLowerCase() + ", " + firstName.toLowerCase(), com.ibm.icu.text.Normalizer.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "")+SolrServiceImpl.FILTER_SEPARATOR+fullName));
        //filterQueries.append("\"");

        for (DCValue dcValue : authorProfile.getMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "name", "variant", Item.ANY)) {

            if(!fullName.equals(dcValue.value))
            {
                filterQueries.append(" OR ");
                //filterQueries.append("\"");
                filterQueries.append(ClientUtils.escapeQueryChars(com.ibm.icu.text.Normalizer.normalize(dcValue.value.toLowerCase().replaceAll(" "," "), com.ibm.icu.text.Normalizer.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "")+ SolrServiceImpl.FILTER_SEPARATOR+dcValue.value.replaceAll(" "," ")));
            }
                //filterQueries.append("\"");
        }
        filterQueries.append(")");
        queries.add(filterQueries.toString());
        queries.add("search.resourcetype:2");


        return queries;

    }

    public static List<DiscoverFilterQuery> toFilterQueryObjects(AuthorProfile authorProfile,Context context) throws SQLException {
        DSpace dspace = new DSpace();

        org.dspace.kernel.ServiceManager manager = dspace.getServiceManager();

        SearchService searchService = manager.getServiceByName(SearchService.class.getName(), SearchService.class);
        StringBuilder filterQueries = new StringBuilder();
        StringBuilder display=new StringBuilder();
        String lastName = authorProfile.getMetadataFirstValue(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "name", "last", Item.ANY);
        String firstName = authorProfile.getMetadataFirstValue(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "name", "first", Item.ANY);
        String fullName = lastName + ", " + firstName;

        filterQueries.append("");
        //filterQueries.append("\"");
        filterQueries.append(ClientUtils.escapeQueryChars(fullName));
        display.append(fullName);
        //filterQueries.append("\"");
        int i = 0;

        for (DCValue dcValue : authorProfile.getMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "name", "variant", Item.ANY)) {

            if(!fullName.equals(dcValue.value))
            {
                filterQueries.append(" OR ");
                display.append(" OR ");
                //filterQueries.append("\"");
                filterQueries.append(ClientUtils.escapeQueryChars(dcValue.value.replaceAll(" "," ")));
                display.append(dcValue.value.replaceAll(" ", " "));
            }
            //filterQueries.append("\"");
            i++;
        }
        filterQueries.append("");
        DiscoverFilterQuery fq=searchService.toFilterQuery(context,"author","oneof",display.toString());
        return Arrays.asList(fq);
    }

    public static List<DiscoverResult.FacetResult> getAuthorFacets(Context context, DiscoverResult queryResults) throws SQLException,SearchServiceException {
        LinkedList<DiscoverResult.FacetResult> result=new LinkedList<DiscoverResult.FacetResult>();
        for (DiscoverResult.FacetResult fr : queryResults.getFacetResult("author")) {

            DiscoverQuery dq = new DiscoverQuery();
            dq.setQuery("search.resourcetype:" + Constants.AUTHOR_PROFILE + " AND author:" + fr.getDisplayedValue().replace(" ", "\\ "));
            dq.addFacetField( new DiscoverFacetField("author", DiscoveryConfigurationParameters.TYPE_TEXT, -1, DiscoveryConfigurationParameters.SORT.VALUE));
            dq.setFacetMinCount(1);
            DiscoverResult apResult = SearchUtils.getSearchService().search(context, dq, true);
            for (DiscoverResult.FacetResult apFR : apResult.getFacetResult("author")){
                if (fr.getDisplayedValue().equals(apFR.getDisplayedValue())){
                    result.add(fr);
                }
            }

        }
        return result;
    }
}
