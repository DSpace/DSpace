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
import java.util.regex.Pattern;

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
    @Autowired
    ItemService itemService;
    @Autowired
    RequestService requestService;
    private String keyId;
    private String schema;
    private String element;
    private String qualifier;
    // use with aggregate mode
    private boolean singleResultOnAggregate = true;
    // limit the generator to a specific authority
    private String authorityName;
    private boolean useForDisplay = true;
    private boolean useAsData = true;

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
                    choiceList.add(new Choice((String) document.getFieldValue("search.resourceid"),
                                              title, title, extras));
                }
            }
        }
        return choiceList;
    }

    protected void buildSingleExtraByMetadata(MetadataValueDTO metadataValue, Map<String, String> extras) {
        if (metadataValue == null) {
            return;
        }
        if (isDuplicatedValue(metadataValue, extras)) {
            return;
        }
        if (StringUtils.isNotBlank(metadataValue.getAuthority())) {
            putValueInExtras(extras, metadataValue.getValue() + "::" + metadataValue.getAuthority());
        } else {
            putValueInExtras(extras, metadataValue.getValue());
        }
    }

    private boolean isDuplicatedValue(MetadataValueDTO metadataValue, Map<String, String> extras) {
        String key = keyId;
        if (useAsData) {
            key = "data-" + keyId;
        }
        String values = extras.get(key);
        if (values == null) {
            return false;
        }

        String valueToFind = StringUtils.isNotBlank(metadataValue.getAuthority())
            ? metadataValue.getValue() + "::" + metadataValue.getAuthority()
            : metadataValue.getValue();

        if (values.contains("|||")) {
            // so it's multivalues
            String[] splittedVals = values.split(Pattern.quote("|||"));
            return ArrayUtils.contains(splittedVals, valueToFind);
        }
        return values.equals(valueToFind);

    }

    protected void buildSingleExtraByRP(SolrDocument solrDocument, Map<String, String> extras) {
        List<MetadataValueDTO> metadataValues = getMetadataValueDTOsFromSolr(schema, element, qualifier, solrDocument);
        if (metadataValues.isEmpty()) {
            buildSingleExtraByMetadata(null, extras);
        } else {
            metadataValues.forEach(metadataValue -> {
                buildSingleExtraByMetadata(metadataValue, extras);
            });
        }
    }

    private void putValueInExtras(Map<String, String> extras, String value) {
        if (useAsData) {
            String key = "data-" + keyId;
            if (extras.containsKey(key)) {
                extras.put(key, extras.get(key) + "|||" + value);
            } else {
                extras.put(key, value);
            }
        }
        if (useForDisplay) {
            if (extras.containsKey(keyId)) {
                extras.put(keyId, extras.get(keyId) + "|||" + value);
            } else {
                extras.put(keyId, value);
            }
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
        List<MetadataValueDTO> metadataValues = new ArrayList<MetadataValueDTO>();
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

    public void setRelatedInputformMetadata(String keyId) {
        log.warn("Used deprecated setRelatedInputformMetadata to set keyId on "
                     + "ItemSimpleAuthorityMetadataGenerator, setKeyId should be preferred");
        this.keyId = keyId;
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

    public String getAuthorityName() {
        return authorityName;
    }

    public void setAuthorityName(String authorityName) {
        this.authorityName = authorityName;
    }

    public boolean isUseForDisplay() {
        return useForDisplay;
    }

    public void setUseForDisplay(boolean useForDisplay) {
        this.useForDisplay = useForDisplay;
    }

    public boolean isUseAsData() {
        return useAsData;
    }

    public void setUseAsData(boolean useAsData) {
        this.useAsData = useAsData;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

}