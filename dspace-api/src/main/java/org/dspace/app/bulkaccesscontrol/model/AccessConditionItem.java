/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkaccesscontrol.model;

import java.util.ArrayList;
import java.util.List;

import org.dspace.app.bulkaccesscontrol.BulkAccessControl;

/**
 * Class that model the value of item node
 * from json file of the {@link BulkAccessControl}
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 */
public class AccessConditionItem {

    String mode;

    List<AccessCondition> accessConditions;

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public List<AccessCondition> getAccessConditions() {
        if (accessConditions == null) {
            return new ArrayList<>();
        }
        return accessConditions;
    }

    public void setAccessConditions(List<AccessCondition> accessConditions) {
        this.accessConditions = accessConditions;
    }
}