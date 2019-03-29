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
 * The DSpaceAuthorityIndexer will return a list of all authority values for a
 * given item. It will return an authority value for all metadata fields defined
 * in dspace.conf with 'authority.author.indexer.field'.
 * <p>
 * You have to call getAuthorityValues for every Item you want to index. But you
 * can supply an optional cache, to save the mapping from the metadata value to
 * the new authority values for metadata fields without an authority key.
 *
 * @author Antoine Snyers (antoine at atmire.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class DSpaceAuthorityIndexer implements AuthorityIndexerInterface, InitializingBean {

    private static final Logger log = Logger.getLogger(DSpaceAuthorityIndexer.class);

    /**
     * The list of metadata fields which are to be indexed *
     */
    protected List<String> metadataFields;

    @Autowired(required = true)
    protected AuthorityValueService authorityValueService;

    @Autowired(required = true)
    protected ItemService itemService;

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
    public List<AuthorityValue> getAuthorityValues(Context context, Item item)
            throws SQLException, AuthorizeException
    {
        return getAuthorityValues(context, item, null);
    }

    @Override
    public List<AuthorityValue> getAuthorityValues(Context context, Item item, Map<String, AuthorityValue> cache)
            throws SQLException, AuthorizeException
    {
        List<AuthorityValue> values = new ArrayList<>();

        for (String metadataField : metadataFields) {
            List<MetadataValue> metadataValues = itemService.getMetadataByMetadataString(item, metadataField);
            for (MetadataValue metadataValue : metadataValues) {
                String content = metadataValue.getValue();
                String authorityKey = metadataValue.getAuthority();

                // We only want to update our item IF our UUID is not present
                // or if we need to generate one.
                boolean requiresItemUpdate = StringUtils.isBlank(authorityKey) ||
                        StringUtils.startsWith(authorityKey, AuthorityValueService.GENERATE);

                AuthorityValue value = null;
                if (StringUtils.isBlank(authorityKey) && cache != null) {
                    // This is a value currently without an authority. So query
                    // the cache, if an authority is found for the exact value.
                    value = cache.get(content);
                }

                if (value == null) {
                    value = getAuthorityValue(context, metadataField, content,authorityKey);
                }

                if (value != null) {
                    if (requiresItemUpdate) {
                        value.updateItem(context, item, metadataValue);

                        try {
                            itemService.update(context, item);
                        }
                        catch (Exception e) {
                            log.error("Error creating a metadatavalue's authority", e);
                        }
                    }

                    if (cache != null) {
                        cache.put(content, value);
                    }

                    values.add(value);
                }
                else {
                    log.error("Error getting an authority value for " +
                            "the metadata value \"" + content + "\" " +
                            "in the field \"" + metadataField + "\" " +
                            "of the item " + item.getHandle());
                }
            }
        }

        return values;
    }

    /**
     * This method looks at the authority of a metadata value.
     * If the authority can be found in solr, that value is reused.
     * Otherwise a new authority value will be generated that will be indexed in solr.
     *
     * If the authority starts with AuthorityValueGenerator.GENERATE, a specific type of AuthorityValue will be generated.
     * Depending on the type this may involve querying an external REST service
     *
     * @param context Current DSpace context
     * @param metadataField Is one of the fields defined in dspace.cfg to be indexed.
     * @param metadataContent Content of the current metadata value.
     * @param metadataAuthorityKey Existing authority of the metadata value.
     */
    private AuthorityValue getAuthorityValue(Context context, String metadataField,
            String metadataContent, String metadataAuthorityKey)
    {
        if (StringUtils.isNotBlank(metadataAuthorityKey) &&
                !metadataAuthorityKey.startsWith(AuthorityValueService.GENERATE)) {
            // !uid.startsWith(AuthorityValueGenerator.GENERATE) is not strictly
            // necessary here but it prevents exceptions in solr

            AuthorityValue value = authorityValueService.findByUID(context, metadataAuthorityKey);
            if (value != null) {
                return value;
            }
        }

        return authorityValueService.generate(context, metadataAuthorityKey,
                metadataContent, metadataField.replaceAll("\\.", "_"));
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
