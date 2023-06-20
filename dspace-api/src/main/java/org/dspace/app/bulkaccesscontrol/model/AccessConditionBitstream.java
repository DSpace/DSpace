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
 * Class that model the value of bitstream node
 * from json file of the {@link BulkAccessControl}
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 */
public class AccessConditionBitstream {

    private String mode;

    private Constraint constraints;

    private List<AccessCondition> accessConditions;

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Constraint getConstraints() {
        return constraints;
    }

    public void setConstraints(Constraint constraints) {
        this.constraints = constraints;
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

    public class Constraint {

        private List<String> uuid;

        public List<String> getUuid() {
            return uuid;
        }

        public void setUuid(List<String> uuid) {
            this.uuid = uuid;
        }
    }

}
