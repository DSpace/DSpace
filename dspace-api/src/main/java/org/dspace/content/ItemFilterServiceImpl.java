/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.dspace.content.logic.LogicalStatement;
import org.dspace.notifyservices.ItemFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/**
 * Service implementation for {@link ItemFilterService}
 *
 * @author Mohamd Eskander (mohamed.eskander at 4science.com)
 */
public class ItemFilterServiceImpl implements ItemFilterService {

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public List<ItemFilter> findAll() {
        Map<String, LogicalStatement> beans =
            applicationContext.getBeansOfType(LogicalStatement.class);

        return beans.keySet()
                    .stream()
                    .sorted()
                    .map(id -> new ItemFilter(id))
                    .collect(Collectors.toList());
    }
}