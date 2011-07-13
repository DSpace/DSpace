/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.dspace.utils.DSpace;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Util methods used by discovery
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class SearchUtils {

    private static final Logger log = Logger.getLogger(SearchUtils.class);

    private static ExtendedProperties props = null;

    private static Map<String, FacetFieldConfig[]> fieldFacets = new HashMap<String, FacetFieldConfig[]>();

    private static List<String> allFacets = new ArrayList<String>();

    private static List<String> searchFilters = new ArrayList<String>();

    private static List<String> searchFiltersFullAutocomplete = new ArrayList<String>();

    private static List<String> sortFields = new ArrayList<String>();

    private static List<String> dateIndexableFields = new ArrayList<String>();

    public static final String FILTER_SEPARATOR = "|||";


    /** Cached search service **/
    private static SearchService searchService;

    static {

        log.debug("loading configuration");
        //Method that will retrieve all the possible configs we have

        props = ExtendedProperties
                .convertProperties(ConfigurationManager.getProperties());

        try {
            File config = new File(props.getProperty("dspace.dir") + "/config/dspace-solr-search.cfg");
            if (config.exists()) {
                props.combine(new ExtendedProperties(config.getAbsolutePath()));
            } else {
                InputStream is = null;
                try {
                    is = SearchUtils.class.getResourceAsStream("dspace-solr-search.cfg");
                    ExtendedProperties defaults = new ExtendedProperties();
                    defaults.load(is);
                    props.combine(defaults);
                } finally {
                    if (is != null) {
                        is.close();
                    }
                }
            }

            log.debug("combined configuration");
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

        try {
            Iterator allPropsIt = props.getKeys();
            while (allPropsIt.hasNext()) {
                String propName = String.valueOf(allPropsIt.next());
                if (propName.startsWith("solr.facets.")) {
                    String[] propVals = props.getStringArray(propName);

                    log.info("loading scope, " + propName);

                    allFacets.addAll(Arrays.asList(propVals));
                    List<FacetFieldConfig> facets = new ArrayList<FacetFieldConfig>();
                    for (String propVal : propVals) {
                        if (propVal.endsWith("_dt") || propVal.endsWith(".year")) {
                            facets.add(new FacetFieldConfig(propVal.replace("_dt", ".year"), true));

                            log.info("value, " + propVal);

                        } else {
                            facets.add(new FacetFieldConfig(propVal + "_filter", false));

                            log.info("value, " + propVal);
                        }
                    }

                    //All the values are split into date & facetfields, so now store em
                    fieldFacets.put(propName.replace("solr.facets.", ""), facets.toArray(new FacetFieldConfig[facets.size()]));

                    log.info("fieldFacets size: " + fieldFacets.size());
                }
            }

            String[] filterFieldsProps = SearchUtils.getConfig().getStringArray("solr.search.filters");
            if (filterFieldsProps != null) {
                for (String filterProp : filterFieldsProps) {
                    if(filterProp.endsWith(":full")){
                        String field = filterProp.substring(0, filterProp.lastIndexOf(":"));
                        searchFiltersFullAutocomplete.add(field);
                        searchFilters.add(field);
                    }else{
                        searchFilters.add(filterProp);
                    }

                }
            }

            String[] sortFieldProps = SearchUtils.getConfig().getStringArray("solr.search.sort");
            if (sortFieldProps != null) {
                sortFields.addAll(Arrays.asList(sortFieldProps));
            }


        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IllegalStateException(e.getMessage(), e);
        }

    }

    public static ExtendedProperties getConfig() {

        return props;
    }

    public static FacetFieldConfig[] getFacetsForType(String type) {
        return fieldFacets.get(type);
    }

    public static List<String> getAllFacets() {
        return allFacets;
    }

    public static List<String> getSearchFilters() {
        return searchFilters;
    }

    public static boolean isNonTokenizedSearchFilter(String field){
        return searchFiltersFullAutocomplete.contains(field);
    }

    public static List<String> getSortFields() {
        return sortFields;
    }



    public static String[] getDefaultFilters(String scope) {
        List<String> result = new ArrayList<String>();
        // Check (and add) any default filters which may be configured
        String defaultFilters = getConfig().getString("solr.default.filterQuery");
        if (defaultFilters != null)
        {
            result.addAll(Arrays.asList(defaultFilters.split(",")));
        }

        if (scope != null) {
            String scopeDefaultFilters = SearchUtils.getConfig().getString("solr." + scope + ".default.filterQuery");
            if (scopeDefaultFilters != null)
            {
                result.addAll(Arrays.asList(scopeDefaultFilters.split(",")));
            }
        }
        return result.toArray(new String[result.size()]);
    }

    public static List<String> getDateIndexableFields() {
        String[] dateFieldsProps = SearchUtils.getConfig().getStringArray("solr.index.type.date");
        if (dateFieldsProps != null) {
            for (String dateField : dateFieldsProps) {
                dateIndexableFields.add(dateField.trim());
            }
        }
        return dateIndexableFields;
    }

    public static SearchService getSearchService()
    {
        if(searchService ==  null){
            DSpace dspace = new DSpace();
            org.dspace.kernel.ServiceManager manager = dspace.getServiceManager() ;
            searchService = manager.getServiceByName(SearchService.class.getName(),SearchService.class);
        }
        return searchService;
    }

}
