/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.indexer;

import org.dspace.authority.AuthorityValue;
import org.dspace.authority.AuthorityValueFinder;
import org.dspace.authority.AuthorityValueGenerator;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DSpaceAuthorityIndexer is used in IndexClient, which is called by the AuthorityConsumer and the indexing-script.
 * <p/>
 * An instance of DSpaceAuthorityIndexer is bound to a list of items.
 * This can be one item or all items too depending on the init() method.
 * <p/>
 * DSpaceAuthorityIndexer lets you iterate over each metadata value
 * for each metadata field defined in dspace.cfg with 'authority.author.indexer.field'
 * for each item in the list.
 * <p/>
 * <p/>
 *
 * @author Antoine Snyers (antoine at atmire.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class DSpaceAuthorityIndexer implements AuthorityIndexerInterface {

    private static final Logger log = Logger.getLogger(DSpaceAuthorityIndexer.class);

    private ItemIterator itemIterator;
    private Item currentItem;
    /**
     * The list of metadata fields which are to be indexed *
     */
    private List<String> metadataFields;
    private int currentFieldIndex;
    private int currentMetadataIndex;
    private AuthorityValue nextValue;
    private Context context;
    private AuthorityValueFinder authorityValueFinder;


    public void init(Context context, Item item) {
        ArrayList<Integer> itemList = new ArrayList<Integer>();
        itemList.add(item.getID());
        this.itemIterator = new ItemIterator(context, itemList);
        try {
            currentItem = this.itemIterator.next();
        } catch (SQLException e) {
            log.error("Error while retrieving an item in the metadata indexer");
        }
        initialize(context);
    }

    public void init(Context context) {
        try {
            this.itemIterator = Item.findAll(context);
            currentItem = this.itemIterator.next();
        } catch (SQLException e) {
            log.error("Error while retrieving all items in the metadata indexer");
        }
        initialize(context);
    }

    private void initialize(Context context) {
        this.context = context;
        this.authorityValueFinder = new AuthorityValueFinder();

        currentFieldIndex = 0;
        currentMetadataIndex = 0;

        int counter = 1;
        String field;
        metadataFields = new ArrayList<String>();
        while ((field = ConfigurationManager.getProperty("authority.author.indexer.field." + counter)) != null) {
            metadataFields.add(field);
            counter++;
        }
    }

    public AuthorityValue nextValue() {
        return nextValue;
    }


    public boolean hasMore() {
        if (currentItem == null) {
            return false;
        }

        // 1. iterate over the metadata values

        String metadataField = metadataFields.get(currentFieldIndex);
        DCValue[] values = currentItem.getMetadata(metadataField);
        if (currentMetadataIndex < values.length) {
            prepareNextValue(metadataField, values[currentMetadataIndex]);

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

                // 3. iterate over the items

                try {
                    if (itemIterator.hasNext()) {
                        currentItem = itemIterator.next();
                        //Reset our current field index
                        currentFieldIndex = 0;
                        //Reset our current metadata index
                        currentMetadataIndex = 0;
                    } else {
                        currentItem = null;
                    }
                    return hasMore();
                } catch (SQLException e) {
                    currentItem = null;
                    log.error("Error while retrieving next item in the author indexer",e);
                    return false;
                }
            }
        }
    }

    /**
     * This method looks at the authority of a metadata.
     * If the authority can be found in solr, that value is reused.
     * Otherwise a new authority value will be generated that will be indexed in solr.
     * If the authority starts with AuthorityValueGenerator.GENERATE, a specific type of AuthorityValue will be generated.
     * Depending on the type this may involve querying an external REST service
     *
     * @param metadataField Is one of the fields defined in dspace.cfg to be indexed.
     * @param value         Is one of the values of the given metadataField in one of the items being indexed.
     */
    private void prepareNextValue(String metadataField, DCValue value) {

        nextValue = null;

        String content = value.value;
        String uid = value.authority;

        if (StringUtils.isNotBlank(uid) && !uid.startsWith(AuthorityValueGenerator.GENERATE)) {
            // !uid.startsWith(AuthorityValueGenerator.GENERATE) is not strictly necessary here but it prevents exceptions in solr
            nextValue = authorityValueFinder.findByUID(context, uid);
        }
        if (nextValue == null) {
            nextValue = AuthorityValueGenerator.generate(uid, content, metadataField.replaceAll("\\.", "_"));
        }
        if (nextValue != null) {
            nextValue.updateItem(currentItem, value);
        }

        try {
            currentItem.update();
        } catch (Exception e) {
            log.error("Error creating a metadatavalue's authority", e);
        }

    }

    public void close() {
        itemIterator.close();
        itemIterator = null;
    }
}
