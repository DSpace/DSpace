/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkaccesscontrol.service;

import java.util.Map;

import org.dspace.app.bulkaccesscontrol.model.BulkAccessConditionConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Simple bean to manage different Bulk Access Condition configurations
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 */
public class BulkAccessConditionConfigurationService {

    @Autowired
    private Map<String, BulkAccessConditionConfiguration> BulkAccessConditionConfigurations;

    public Map<String, BulkAccessConditionConfiguration> getBulkAccessConditionConfigurations() {
        return BulkAccessConditionConfigurations;
    }

    public void setBulkAccessConditionConfigurations(
        Map<String, BulkAccessConditionConfiguration> bulkAccessConditionConfigurations) {
        BulkAccessConditionConfigurations = bulkAccessConditionConfigurations;
    }
}