/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkaccesscontrol.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.dspace.app.bulkaccesscontrol.model.BulkAccessConditionConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Simple bean to manage different Bulk Access Condition configurations
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 */
public class BulkAccessConditionConfigurationService {

    @Autowired
    private List<BulkAccessConditionConfiguration> bulkAccessConditionConfigurations;

    public List<BulkAccessConditionConfiguration> getBulkAccessConditionConfigurations() {
        if (CollectionUtils.isEmpty(bulkAccessConditionConfigurations)) {
            return new ArrayList<>();
        }
        return bulkAccessConditionConfigurations;
    }

    public BulkAccessConditionConfiguration getBulkAccessConditionConfiguration(String name) {
        return getBulkAccessConditionConfigurations().stream()
                                                     .filter(x -> name.equals(x.getName()))
                                                     .findFirst()
                                                     .orElse(null);
    }

    public void setBulkAccessConditionConfigurations(
        List<BulkAccessConditionConfiguration> bulkAccessConditionConfigurations) {
        this.bulkAccessConditionConfigurations = bulkAccessConditionConfigurations;
    }
}