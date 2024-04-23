/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.util.List;

import org.dspace.app.ldn.ItemFilter;

/**
 * Service interface class for the Item Filter Object
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public interface ItemFilterService {

    /**
     * @param id the bean name of item filter
     * @return one logical item filter by id
     * defined in item-filter.xml
     */
    public ItemFilter findOne(String id);

    /**
     * @return all logical item filters
     * defined in item-filter.xml
     */
    public List<ItemFilter> findAll();
}
