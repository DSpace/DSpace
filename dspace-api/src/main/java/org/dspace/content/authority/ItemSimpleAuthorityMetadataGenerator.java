/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.dspace.content.Item;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.content.service.ItemService;
import org.dspace.discovery.indexobject.ItemIndexFactoryImpl;
import org.dspace.services.RequestService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Generic generator to work on nested/simple metadata
 * 
 * @author Mykhaylo Boychuk (4Science.it)
 *
 */
public class ItemSimpleAuthorityMetadataGenerator implements ItemAuthorityExtraMetadataGenerator {

    private static final Logger log = LoggerFactory.getLogger(ItemSimpleAuthorityMetadataGenerator.class);

    private final String storedSeparatorSplit = java.util.regex.Pattern.quote(ItemIndexFactoryImpl.STORE_SEPARATOR);

    private String relatedInputformMetadata;

    private String schema;

    private String element;

    private String qualifier;

    // use with aggregate mode
    private boolean singleResultOnAggregate = true;

    // limit the generator to a specific authority
    private String authorityName;

    @Autowired
    ItemService itemService;

    @Autowired
    RequestService requestService;

    @Override
    public Map<String, String> build(String checkAuthorityName, SolrDocument document) {
        Map<String, String> extras = new HashMap<String, String>();
        if (StringUtils.isBlank(authorityName) || StringUtils.equals(authorityName, checkAuthorityName)) {
            buildSingleExtraByRP(document, extras);
        }
        return extras;
    }

    @Override
    public List<Choice> buildAggregate(String checkAuthorityName, SolrDocument document) {
        List<Choice> choiceList = new LinkedList<Choice>();
        String title = ((ArrayList<String>) document.getFieldValue("dc.title")).get(0);
        if (StringUtils.isNotBlank(authorityName) && !StringUtils.equals(authorityName, checkAuthorityName)) {
            choiceList.add(
                    new Choice((String) document.getFieldValue("search.resourceid"),
                            title, title, new HashMap<String, String>()));
        } else {
            if (isSingleResultOnAggregate()) {
                Map<String, String> extras = new HashMap<String, String>();
                buildSingleExtraByRP(document, extras);
                choiceList.add(new Choice((String) document.getFieldValue("search.resourceid"),
                        title, title, extras));
            } else {
                List<MetadataValueDTO> metadataValues = getMetadataValueDTOsFromSolr(schema, element, qualifier,
                        document);
                if (metadataValues != null && metadataValues.size() != 0) {
                    for (MetadataValueDTO metadataValue : metadataValues) {
                        Map<String, String> extras = new HashMap<String, String>();
                        buildSingleExtraByMetadata(metadataValue, extras);
                        choiceList.add(new Choice((String) document.getFieldValue("search.resourceid"),
                                title + "(" + metadataValue.getValue() + ")", title, extras));
                    }
                } else {
                    Map<String, String> extras = new HashMap<String, String>();
                    extras.put("data-" + getRelatedInputformMetadata(), "");
                    choiceList.add(new Choice((String) document.getFieldValue("search.resourceid"),
                            title, title, extras));
                }
            }
        }
        return choiceList;
    }

    protected void buildSingleExtraByMetadata(MetadataValueDTO metadataValue, Map<String, String> extras) {
        if (metadataValue == null) {
            extras.put("data-" + getRelatedInputformMetadata(), "");
        } else {
            if (StringUtils.isNotBlank(metadataValue.getAuthority())) {
                extras.put("data-" + getRelatedInputformMetadata(),
                        metadataValue.getValue() + "::" + metadataValue.getAuthority());
            } else {
                extras.put("data-" + getRelatedInputformMetadata(), metadataValue.getValue());
            }
        }
    }

    protected void buildSingleExtraByRP(SolrDocument solrDocument, Map<String, String> extras) {
        List<MetadataValueDTO> metadataValues =  getMetadataValueDTOsFromSolr(schema, element, qualifier, solrDocument);
        if (metadataValues.isEmpty()) {
            buildSingleExtraByMetadata(null, extras);
        } else {
            buildSingleExtraByMetadata(metadataValues.get(0), extras);
        }
    }

    private List<MetadataValueDTO> getMetadataValueDTOsFromSolr(String schema, String element, String qualifier,
            SolrDocument solrDocument) {
        if (!projectionFieldsContain(schema, element, qualifier)) {
            log.error("the metadata " + getMetadata(schema, element, qualifier)
                    + " is not configured to be stored in solr,"
                    + " check the discovery.index.projection properties in the discovery.cfg");
            return Collections.EMPTY_LIST;
        }
        String metadata = getMetadata(schema, element, qualifier) + "_stored";
        List<MetadataValueDTO> metadataValues =  new ArrayList<MetadataValueDTO>();
        ArrayList<String> fieldValue = (ArrayList<String>) solrDocument.getFieldValue(metadata);
        if (fieldValue != null) {
            for (String storedValue : fieldValue) {
                MetadataValueDTO dto = new MetadataValueDTO();
                dto.setSchema(schema);
                dto.setElement(element);
                dto.setQualifier(qualifier);
                String[] split = storedValue.split(storedSeparatorSplit);
                dto.setValue(nullOrValue(split[0]));
                dto.setAuthority(nullOrValue(split[3]));
                dto.setLanguage(nullOrValue(split[4]));
                metadataValues.add(dto);
            }
        }
        return metadataValues;
    }

    private String nullOrValue(String string) {
        if (StringUtils.equals(string, "null")) {
            return null;
        } else {
            return string;
        }
    }

    private String getMetadata(String schema, String element, String qualifier) {
        if (StringUtils.isNotBlank(qualifier)) {
            return StringUtils.join(new String[] {schema, element, qualifier}, ".");
        } else {
            return StringUtils.join(new String[] {schema, element}, ".");
        }
    }

    private boolean projectionFieldsContain(String schema, String element, String qualifier) {
        String[] projectionFields = DSpaceServicesFactory.getInstance().getConfigurationService()
                .getArrayProperty("discovery.index.projection");
        return ArrayUtils.contains(projectionFields, getMetadata(schema, element, qualifier)) ||
                ArrayUtils.contains(projectionFields, getMetadata(schema, element, Item.ANY));
    }

    public String getRelatedInputformMetadata() {
        return relatedInputformMetadata;
    }

    public void setRelatedInputformMetadata(String relatedInputformMetadata) {
        this.relatedInputformMetadata = relatedInputformMetadata;
    }

    public String getElement() {
        return element;
    }

    public void setElement(String element) {
        this.element = element;
    }

    public String getQualifier() {
        return qualifier;
    }

    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public boolean isSingleResultOnAggregate() {
        return singleResultOnAggregate;
    }

    public void setSingleResultOnAggregate(boolean singleResultOnAggregate) {
        this.singleResultOnAggregate = singleResultOnAggregate;
    }

    public void setAuthorityName(String authorityName) {
        this.authorityName = authorityName;
    }

    public String getAuthorityName() {
        return authorityName;
    }

}