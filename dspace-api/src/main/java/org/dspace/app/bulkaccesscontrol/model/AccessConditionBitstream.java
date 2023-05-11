/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkaccesscontrol.model;

import java.util.List;

/**
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 */
public class AccessConditionBitstream {

    private String mode;

    private Constraint constraint;

    private List<AccessCondition> accessConditions;

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Constraint getConstraint() {
        return constraint;
    }

    public void setConstraint(Constraint constraint) {
        this.constraint = constraint;
    }

    public List<AccessCondition> getAccessConditions() {
        return accessConditions;
    }

    public void setAccessConditions(List<AccessCondition> accessConditions) {
        this.accessConditions = accessConditions;
    }

    public class Constraint {

        private List<String> uuids;

        public List<String> getUuids() {
            return uuids;
        }

        public void setUuids(List<String> uuids) {
            this.uuids = uuids;
        }
    }

}
