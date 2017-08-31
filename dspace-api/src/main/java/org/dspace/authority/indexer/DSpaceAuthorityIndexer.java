/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.indexer;

import org.apache.commons.collections.CollectionUtils;
import org.dspace.authority.AuthorityValue;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.authority.service.AuthorityValueService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.MetadataValue;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.util.*;

/**
 * DSpaceAuthorityIndexer is used in IndexClient, which is called by the AuthorityConsumer and the indexing-script.
 * <p>
 * An instance of DSpaceAuthorityIndexer is bound to an item.
 * <p>
 * DSpaceAuthorityIndexer lets you iterate over each metadata value
 * for each metadata field defined in dspace.cfg with 'authority.author.indexer.field'
 * for each item in the list.
 * <p>
 *
 * @author Antoine Snyers (antoine at atmire.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class DSpaceAuthorityIndexer implements AuthorityIndexerInterface, InitializingBean {

    private static final Logger log = Logger.getLogger(DSpaceAuthorityIndexer.class);

    protected Item item;
    /**
     * The list of metadata fields which are to be indexed *
     */
    protected List<String> metadataFields;
    protected int currentFieldIndex;
    protected int currentMetadataIndex;
    protected AuthorityValue nextValue;
    protected Context context;

    @Autowired(required = true)
    protected AuthorityValueService authorityValueService;

    @Autowired(required = true)
    protected ItemService itemService;
    protected boolean useCache;
    protected Map<String, AuthorityValue> cache;

    @Autowired(required = true)
    protected ConfigurationService configurationService;

    @Override
    public void afterPropertiesSet() throws Exception {
        int counter = 1;
        String field;
        metadataFields = new ArrayList<String>();
        while ((field = configurationService.getProperty("authority.author.indexer.field." + counter)) != null) {
            metadataFields.add(field);
            counter++;
        }
    }

    @Override
    public void init(Context context, Item item) {
        this.context = context;
        this.item = item;

        currentFieldIndex = 0;
        currentMetadataIndex = 0;
        useCache = false;
        cache = new HashMap<>();
    }

    @Override
    public AuthorityValue nextValue() {
        return nextValue;
    }

    @Override
    public boolean hasMore() throws SQLException, AuthorizeException {
        // 1. iterate over the metadata values

        String metadataField = metadataFields.get(currentFieldIndex);
        List<MetadataValue> values = itemService.getMetadataByMetadataString(item, metadataField);
        if (currentMetadataIndex < values.size()) {
            prepareNextValue(metadataField, values.get(currentMetadataIndex));

            currentMetadataIndex++;
            return true;
        } else {

            // 2. iterate over the metadata fields

            if ((currentFieldIndex + 1) < metadataFields.size()) {
                currentFieldIndex++;
                //Reset our current metadata index since we are moving to another field
                currentMetadataIndex = 0;
                return hasMore();
            } else {
                return false;
            }
        }
    }

    /**
     * This method looks at the authority of a metadata.
     * If the authority can be found in solr, that value is reused.
     * Otherwise a new authority value will be generated that will be indexed in solr.
     *
     * If the authority starts with AuthorityValueGenerator.GENERATE, a specific type of AuthorityValue will be generated.
     * Depending on the type this may involve querying an external REST service
     *
     * @param metadataField Is one of the fields defined in dspace.cfg to be indexed.
     * @param value         Is one of the values of the given metadataField in one of the items being indexed.
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    protected void prepareNextValue(String metadataField, MetadataValue value) throws SQLException, AuthorizeException {

        nextValue = null;

        String content = value.getValue();
        String authorityKey = value.getAuthority();
        //We only want to update our item IF our UUID is not present or if we need to generate one.
        boolean requiresItemUpdate = StringUtils.isBlank(authorityKey) || StringUtils.startsWith(authorityKey, AuthorityValueService.GENERATE);

        if (StringUtils.isNotBlank(authorityKey) && !authorityKey.startsWith(AuthorityValueService.GENERATE)) {
            // !uid.startsWith(AuthorityValueGenerator.GENERATE) is not strictly necessary here but it prevents exceptions in solr
            nextValue = authorityValueService.findByUID(context, authorityKey);
        }
        if (nextValue == null && StringUtils.isBlank(authorityKey) && useCache) {
            // A metadata without authority is being indexed
            // If there is an exact match in the cache, reuse it rather than adding a new one.
            AuthorityValue cachedAuthorityValue = cache.get(content);
            if (cachedAuthorityValue != null) {
                nextValue = cachedAuthorityValue;
            }
        }
        if (nextValue == null) {
            nextValue = authorityValueService.generate(context, authorityKey, content, metadataField.replaceAll("\\.", "_"));
        }
        if (nextValue != null && requiresItemUpdate) {
            nextValue.updateItem(context, item, value);
            try {
                itemService.update(context, item);
            } catch (Exception e) {
                log.error("Error creating a metadatavalue's authority", e);
            }
        }
        if (useCache) {
            cache.put(content, nextValue);
        }
    }

    @Override
    public void close() {
        cache.clear();
    }

    @Override
    public boolean isConfiguredProperly() {
        boolean isConfiguredProperly = true;
        if(CollectionUtils.isEmpty(metadataFields)){
            log.warn("Authority indexer not properly configured, no metadata fields configured for indexing. Check the \"authority.author.indexer.field\" properties.");
            isConfiguredProperly = false;
        }
        return isConfiguredProperly;
    }
}
