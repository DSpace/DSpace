/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.dspace.app.rest.ContentReportRestController;
import org.dspace.contentreport.FilteredCollections;

/**
 * This class serves as a REST representation of a Filtered Collections Report.
 * The name must match that of the associated resource class (FilteredCollectionsResource) except for
 * the suffix. This is why it is not named something like FilteredCollectionsReportRest.
 *
 * @author Jean-François Morin (Université Laval)
 */
public class FilteredCollectionsRest extends BaseObjectRest<String> {

    private static final long serialVersionUID = -1109226348211060786L;
    /** Type of instances of this class, used by the DSpace REST infrastructure */
    public static final String NAME = "filteredcollectionsreport";
    /** Category of instances of this class, used by the DSpace REST infrastructure */
    public static final String CATEGORY = RestModel.CONTENT_REPORT;

    /** Collections included in the report */
    private List<FilteredCollectionRest> collections = new ArrayList<>();
    /** Report summary */
    private FilteredCollectionRest summary;

    /**
     * Builds a FilteredCollectionsRest instance from a {@link FilteredCollections} instance.
     * Each underlying FilteredCollection is converted to a FilteredCollectionRest instance.
     * @param model the FilteredCollections instance that provides values to the
     * FilteredCollectionsRest instance to be created
     * @return a FilteredCollectionsRest instance built from the provided model object
     */
    public static FilteredCollectionsRest of(FilteredCollections model) {
        var colls = new FilteredCollectionsRest();
        Optional.ofNullable(model.getCollections()).ifPresent(cs ->
                cs.stream()
                        .map(FilteredCollectionRest::of)
                        .forEach(colls.collections::add));
        colls.summary = FilteredCollectionRest.of(model.getSummary());
        return colls;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public Class<?> getController() {
        return ContentReportRestController.class;
    }

    @Override
    public String getType() {
        return NAME;
    }

    @Override
    public String getTypePlural() {
        return getType();
    }

    public List<FilteredCollectionRest> getCollections() {
        return collections;
    }

    public FilteredCollectionRest getSummary() {
        return summary;
    }

}
