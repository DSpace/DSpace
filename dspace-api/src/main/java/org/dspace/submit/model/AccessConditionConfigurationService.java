/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.model;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Simple bean to manage different Access Condition configurations
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class AccessConditionConfigurationService {

    @Autowired
    private List<AccessConditionConfiguration> accessConditionConfigurations;

    public AccessConditionConfiguration getAccessConfigurationById(String name) {
        return accessConditionConfigurations.stream().filter(x -> name.equals(x.getName())).findFirst().get();
    }

}