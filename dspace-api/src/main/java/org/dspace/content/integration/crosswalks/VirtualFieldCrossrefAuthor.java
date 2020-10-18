/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;

/**
 * Implements virtual field processing for generate an xml element to insert on
 * crosserf xml deposit file.
 * 
 * @author pascarelli
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class VirtualFieldCrossrefAuthor implements VirtualFieldDisseminator, VirtualFieldIngester {

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    public String[] getMetadata(Item item, Map<String, String> fieldCache, String fieldName) {
        // Check to see if the virtual field is already in the cache
        // - processing is quite intensive, so we generate all the values on
        // first request
        if (fieldCache.containsKey(fieldName)) {
            return new String[] { fieldCache.get(fieldName) };
        }
        List<MetadataValue> md = itemService.getMetadata(item, "dc", "contributor", "author", Item.ANY);

        String element = "<given_name>{0}</given_name><surname>{1}</surname>";
        String firstname = "";
        String lastname = "";

        String[] author = md.get(0).getValue().split(",");
        if (author.length > 2) {
            firstname = author[2];
            lastname = author[0] + "," + author[1];
        } else {
            if (author.length > 1) {
                firstname = author[1];
                lastname = author[0];
            }
        }

        element = MessageFormat.format(element, firstname.trim(), lastname.trim());

        fieldCache.put("virtual.crossrefauthor", element);
        // Return the value of the virtual field (if any)
        if (fieldCache.containsKey(fieldName)) {
            return new String[] { fieldCache.get(fieldName) };
        }
        return null;
    }

    public boolean addMetadata(Item item, Map<String, String> fieldCache, String fieldName, String value) {
        // NOOP - we won't add any metadata yet, we'll pick it up when we
        // finalise the item
        return true;
    }

    public boolean finalizeItem(Item item, Map<String, String> fieldCache) {
        return false;
    }
}