/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.MetadataValueWrapper;
import org.dspace.app.rest.model.MetadataValueWrapperRest;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataValueService;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.indexobject.IndexableItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage MetadataValueWrapper Rest object.
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
@Component(MetadataValueWrapperRest.CATEGORY + "." + MetadataValueWrapperRest.NAME)
public class MetadataValueRestRepository extends DSpaceRestRepository<MetadataValueWrapperRest, Integer> {

    /**
     * log4j logger
     */
    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(MetadataValueRestRepository.class);

    @Autowired
    MetadataValueService metadataValueService;

    @Autowired
    MetadataFieldService metadataFieldService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private ItemService itemService;

    private static final String UNDEFINED = "undefined";

    /**
     * Endpoint for the search in the {@link MetadataValue} objects by the metadataField and various values.
     *
     * @param schemaName    an exact match of the prefix of the metadata schema (e.g. "dc", "dcterms", "eperson")
     * @param elementName   an exact match of the field's element (e.g. "contributor", "title")
     * @param qualifierName an exact match of the field's qualifier (e.g. "author", "alternative")
     * @param searchValue   searching value in the {@link MetadataValue} object
     * @param pageable      the pagination options
     * @return List of {@link MetadataValueWrapperRest} objects representing all {@link MetadataValue} objects
     * that match the given params
     */
    @SearchRestMethod(name = "byValue")
    public Page<MetadataValueWrapperRest> findByValue(@Parameter(value = "schema", required = true) String schemaName,
                                                   @Parameter(value = "element", required = true) String elementName,
                                                   @Parameter(value = "qualifier", required = false)
                                                                  String qualifierName,
                                                   @Parameter(value = "searchValue", required = false) String
                                                                  searchValue,
                                                   Pageable pageable) {
        if (StringUtils.isBlank(searchValue)) {
            throw new DSpaceBadRequestException("searchValue cannot be null!");
        }

        Context context = obtainContext();

        String separator = ".";
        String metadataField = StringUtils.isNotBlank(schemaName) ? schemaName + separator : "";
        metadataField += StringUtils.isNotBlank(elementName) ? elementName : "";
        metadataField += StringUtils.isNotBlank(qualifierName) && !StringUtils.equals(UNDEFINED, qualifierName) ?
                separator + qualifierName : "";

        List<String> metadata = List.of(metadataField.split("\\."));
        // metadataField validation
        if (StringUtils.isNotBlank(metadataField)) {
            if (metadata.size() > 3) {
                throw new IllegalArgumentException("Query param should not contain more than 2 dot (.) separators, " +
                        "forming schema.element.qualifier");
            }
        }

        if (searchValue.contains(":")) {
            searchValue = searchValue.replace(":", "");
        }

        // Find matches in Solr Search core
        DiscoverQuery discoverQuery =
                this.createDiscoverQuery(metadataField, searchValue, pageable);

        List<MetadataValueWrapper> metadataValueWrappers = new ArrayList<>();
        try {
            DiscoverResult searchResult = searchService.search(context, null, discoverQuery);
            for (IndexableObject object : searchResult.getIndexableObjects()) {
                if (object instanceof IndexableItem) {
                    // get metadata values of the item
                    List<MetadataValue> metadataValues = itemService.getMetadataByMetadataString(
                            ((IndexableItem) object).getIndexedObject(), metadataField);

                    // convert metadata values to the wrapper
                    List<MetadataValueWrapper> metadataValueWrapperList =
                            this.convertMetadataValuesToWrappers(metadataValues);
                    metadataValueWrappers.addAll(metadataValueWrapperList);
                }
            }
        } catch (SearchServiceException e) {
            log.error("Error while searching with Discovery", e);
            throw new IllegalArgumentException("Error while searching with Discovery: " + e.getMessage());
        }

        // filter eu sponsor -> do not return eu sponsor suggestions for items where eu sponsor is used.
        // openAIRE API
        if (StringUtils.equals(schemaName, "local") && StringUtils.equals(elementName, "sponsor")) {
            metadataValueWrappers = filterEUSponsors(metadataValueWrappers);
        }
        metadataValueWrappers = distinctMetadataValues(metadataValueWrappers);

        return converter.toRestPage(metadataValueWrappers, pageable, utils.obtainProjection());
    }

    public List<MetadataValueWrapper> filterEUSponsors(List<MetadataValueWrapper> metadataWrappers) {
        return metadataWrappers.stream().filter(m -> !m.getMetadataValue().getValue().contains("info:eu-repo"))
                .collect(Collectors.toList());
    }

    public List<MetadataValueWrapper> distinctMetadataValues(List<MetadataValueWrapper> metadataWrappers) {
        return metadataWrappers.stream().filter(
                distinctByKey(metadataValueWrapper -> metadataValueWrapper.getMetadataValue().getValue()) )
                .collect( Collectors.toList() );
    }

    private DiscoverQuery createDiscoverQuery(String metadataField, String searchValue, Pageable pageable) {
        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setQuery(metadataField + ":" + "*" + searchValue + "*");
        discoverQuery.setStart(Math.toIntExact(pageable.getOffset()));
        discoverQuery.setMaxResults(pageable.getPageSize());
        // return only metadata field values
        discoverQuery.addSearchField(metadataField);

        return discoverQuery;
    }

    private List<MetadataValueWrapper> convertMetadataValuesToWrappers(List<MetadataValue> metadataValueList) {
        List<MetadataValueWrapper> metadataValueWrapperList = new ArrayList<>();
        for (MetadataValue metadataValue : metadataValueList) {
            MetadataValueWrapper metadataValueWrapper = new MetadataValueWrapper();
            metadataValueWrapper.setMetadataValue(metadataValue);
            metadataValueWrapperList.add(metadataValueWrapper);
        }
        return metadataValueWrapperList;
    }

    /**
     * Filter unique values from the list and return list with unique values
     * @return List with unique values
     */
    public static <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor) {
        Map<Object, Boolean> map = new ConcurrentHashMap<>();
        return t -> map.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    @Override
    @PreAuthorize("permitAll()")
    public MetadataValueWrapperRest findOne(Context context, Integer id) {
        MetadataValueWrapper metadataValueWrapper = new MetadataValueWrapper();
        try {
            metadataValueWrapper.setMetadataValue(metadataValueService.find(context, id));
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
        if (metadataValueWrapper.getMetadataValue() == null) {
            return null;
        }
        return converter.toRest(metadataValueWrapper, utils.obtainProjection());
    }

    @Override
    public Page<MetadataValueWrapperRest> findAll(Context context, Pageable pageable) {
        List<MetadataValueWrapper> metadataValueWrappers = new ArrayList<>();
        try {
            List<MetadataField> metadataFields = metadataFieldService.findAll(context);
            for (MetadataField metadataField : metadataFields) {
                metadataValueWrappers.addAll(this.convertMetadataValuesToWrappers(
                        this.metadataValueService.findByField(context, metadataField)));
            }
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        return converter.toRestPage(metadataValueWrappers, pageable, utils.obtainProjection());
    }

    @Override
    public Class<MetadataValueWrapperRest> getDomainClass() {
        return MetadataValueWrapperRest.class;
    }
}
