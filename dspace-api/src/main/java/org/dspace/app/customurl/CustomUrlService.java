/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.customurl;

import java.util.List;
import java.util.Optional;

import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * The service to handle item's custom urls
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface CustomUrlService {

    /**
     * Returns the current custom url for the given item, if any.
     *
     * @param item the item
     * @return the custom url
     */
    Optional<String> getCustomUrl(Item item);

    /**
     * Returns all the old custom url related to the given item.
     *
     * @param item the item
     * @return the old custom urls list
     */
    List<String> getOldCustomUrls(Item item);

    /**
     * Returns all the custom url (current or old) related to the given item.
     *
     * @param item the item
     * @return the custom urls list
     */
    List<String> getAllCustomUrls(Item item);

    /**
     * Replace the custom url of the given item with the provided newUrl. If the
     * given item has no custom url, the new url is simply set on it.
     *
     * @param context the DSpace context.
     * @param item    the item
     * @param newUrl  the new custom url to set
     */
    void replaceCustomUrl(Context context, Item item, String newUrl);

    /**
     * Remove any old custom urls that is equals to the given custom url.
     *
     * @param context   the DSpace context.
     * @param item      the item
     * @param customUrl the custom url to match
     */
    void deleteAnyOldCustomUrlEqualsTo(Context context, Item item, String customUrl);

    /**
     * Add the given url on the old custom url of the given item.
     *
     * @param context the DSpace context.
     * @param item    the item
     * @param url     the old custom url to add
     */
    void addOldCustomUrl(Context context, Item item, String url);

    /**
     * Remove the custom url from the given item.
     *
     * @param context the DSpace context.
     * @param item    the item
     */
    void deleteCustomUrl(Context context, Item item);

    /**
     * Remove all the old custom urls from the given item.
     *
     * @param context the DSpace context.
     * @param item    the item
     */
    void deleteAllOldCustomUrls(Context context, Item item);

    /**
     * Remove the old custom url with the provided index from the given item.
     *
     * @param context the DSpace context.
     * @param item    the item
     * @param index   the index of the custom url to delete
     * @throws IllegalArgumentException if the provided index is not consistent with
     *                                  the cardinality of the old custom urls
     */
    void deleteOldCustomUrlByIndex(Context context, Item item, int index);

    /**
     * Find a single item with the given customUrl, if any.
     *
     * @param context   the DSpace context.
     * @param customUrl the custom url to search for
     * @return the item related to the provided custom url, if
     * any
     * @throws IllegalStateException if multiple items with the given customUrl are
     *                               found
     */
    Optional<Item> findItemByCustomUrl(Context context, String customUrl);

}
