/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.servlet.ServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
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
        String resourceId = (String) document.getFieldValue("search.resourceid");

        //FIXME: lookup via solar instead of itemService. see https://4science.atlassian.net/browse/CSTPER-375
        Map<String, String> extras = new HashMap<String, String>();
        Item item = null;
        try {
            item = itemService.find(context(), UUID.fromString(resourceId));
        } catch (SQLException e) {
            log.error("error while retrieving item: {}", e.getMessage(), e);
            return extras;
        }
        if (StringUtils.isBlank(authorityName) || StringUtils.equals(authorityName, checkAuthorityName)) {
            buildSingleExtraByRP(item, extras);
        }
        return extras;
    }

    @Override
    public List<Choice> buildAggregate(String checkAuthorityName, SolrDocument document) {

        String resourceId = (String) document.getFieldValue("search.resourceid");
        //FIXME: lookup via solar instead of itemService. see https://4science.atlassian.net/browse/CSTPER-375
        Item item = null;
        try {
            item = itemService.find(context(), UUID.fromString(resourceId));
        } catch (SQLException e) {
            log.error("error while retrieving item: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
        List<Choice> choiceList = new LinkedList<Choice>();
        if (StringUtils.isNotBlank(authorityName) && !StringUtils.equals(authorityName, checkAuthorityName)) {
            choiceList.add(
                    new Choice(item.getID().toString(), item.getName(), item.getName(), new HashMap<String, String>()));
        } else {
            if (isSingleResultOnAggregate()) {
                Map<String, String> extras = new HashMap<String, String>();
                buildSingleExtraByRP(item, extras);
                choiceList.add(new Choice(item.getID().toString(), item.getName(), item.getName(), extras));
            } else {
                List<MetadataValue> metadataValues =  itemService.getMetadata(item,
                                                                  getSchema(), getElement(), getQualifier(), Item.ANY);
                for (MetadataValue metadataValue : metadataValues) {
                    Map<String, String> extras = new HashMap<String, String>();
                    buildSingleExtraByMetadata(metadataValue, extras);
                    choiceList.add(new Choice(item.getID().toString(), item.getName() + "(" + metadataValue.getValue()
                                              + ")", item.getName(), extras));
                }
                if (metadataValues == null || metadataValues.size() == 0) {
                    Map<String, String> extras = new HashMap<String, String>();
                    extras.put("data-" + getRelatedInputformMetadata(), "");
                    choiceList.add(new Choice(item.getID().toString(), item.getName(), item.getName(), extras));
                }
            }
        }
        return choiceList;
    }

    protected void buildSingleExtraByMetadata(MetadataValue metadataValue, Map<String, String> extras) {
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

    protected void buildSingleExtraByRP(Item item, Map<String, String> extras) {
        List<MetadataValue> metadataValues =  itemService.getMetadata(item,
                                                  getSchema(), getElement(), getQualifier(), Item.ANY);
        if (metadataValues.isEmpty()) {
            buildSingleExtraByMetadata(null, extras);
        } else {
            buildSingleExtraByMetadata(metadataValues.get(0), extras);
        }
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

    private Context context() {
        Request currentRequest = requestService.getCurrentRequest();
        ServletRequest servletRequest = currentRequest.getServletRequest();
        Context context = (Context) servletRequest.getAttribute("dspace.context");
        return context;
    }
}