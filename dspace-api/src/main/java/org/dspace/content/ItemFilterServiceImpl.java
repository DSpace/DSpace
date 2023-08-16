/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.dspace.app.ldn.ItemFilter;
import org.dspace.content.logic.LogicalStatement;
import org.dspace.kernel.ServiceManager;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for {@link ItemFilterService}
 *
 * @author Mohamd Eskander (mohamed.eskander at 4science.com)
 */
public class ItemFilterServiceImpl implements ItemFilterService {

    @Autowired
    private ServiceManager serviceManager;

    @Override
    public ItemFilter findOne(String id) {
        return findAll()
            .stream()
            .filter(itemFilter ->
                itemFilter.getId().equals(id))
            .findFirst()
            .orElse(null);
    }

    @Override
    public List<ItemFilter> findAll() {
        return serviceManager.getServicesNames()
                             .stream()
                             .filter(id -> isLogicalStatement(id))
                             .map(id -> new ItemFilter(id))
                             .collect(Collectors.toList());
    }

    private boolean isLogicalStatement(String id) {
        return Objects.nonNull(
            serviceManager.getServiceByName(id, LogicalStatement.class)
        );
    }
}