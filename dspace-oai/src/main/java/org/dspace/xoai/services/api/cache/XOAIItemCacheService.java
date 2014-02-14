/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.api.cache;

import com.lyncode.xoai.dataprovider.xml.xoai.Metadata;
import org.dspace.content.Item;

import java.io.IOException;


public interface XOAIItemCacheService {
    boolean hasCache (Item item);
    Metadata get (Item item) throws IOException;
    void put (Item item, Metadata metadata) throws IOException;
    void delete (Item item);
    void deleteAll() throws IOException;
}
