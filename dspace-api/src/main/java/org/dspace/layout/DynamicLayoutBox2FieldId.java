/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * This class defines the Id for the relation beetwen the objects DynamicLayoutBox and DynamicLayoutBox
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@Embeddable
public class DynamicLayoutBox2FieldId implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -2649635188820470846L;

    @Column(name = "dynamic_layout_box_id")
    private Integer dynamicLayoutBoxId;
    @Column(name = "dynamic_layout_field_id")
    private Integer dynamicLayoutFieldId;

    /**
     * Default constructor.
     */
    public DynamicLayoutBox2FieldId() {}

    /**
     * Creates a composite identifier for the given box and field ids.
     *
     * @param boxId the box identifier
     * @param fieldId the field identifier
     */
    public DynamicLayoutBox2FieldId(Integer boxId, Integer fieldId) {
        this.dynamicLayoutFieldId = fieldId;
        this.dynamicLayoutBoxId = boxId;
    }

    /**
     * Returns the dynamic layout box id.
     */
    public Integer getDynamicLayoutBoxId() {
        return dynamicLayoutBoxId;
    }

    /**
     * Sets the dynamic layout box id.
     */
    public void setDynamicLayoutBoxId(Integer boxId) {
        this.dynamicLayoutBoxId = boxId;
    }

    /**
     * Returns the dynamic layout field id.
     */
    public Integer getDynamicLayoutFieldId() {
        return dynamicLayoutFieldId;
    }

    /**
     * Sets the dynamic layout field id.
     */
    public void setDynamicLayoutFieldId(Integer fieldId) {
        this.dynamicLayoutFieldId = fieldId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dynamicLayoutBoxId == null) ? 0 : dynamicLayoutBoxId.hashCode());
        result = prime * result + ((dynamicLayoutFieldId == null) ? 0 : dynamicLayoutFieldId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        DynamicLayoutBox2FieldId other = (DynamicLayoutBox2FieldId) obj;
        if (dynamicLayoutBoxId == null) {
            if (other.dynamicLayoutBoxId != null) {
                return false;
            }
        } else if (!dynamicLayoutBoxId.equals(other.dynamicLayoutBoxId)) {
            return false;
        }
        if (dynamicLayoutFieldId == null) {
            if (other.dynamicLayoutFieldId != null) {
                return false;
            }
        } else if (!dynamicLayoutFieldId.equals(other.dynamicLayoutFieldId)) {
            return false;
        }
        return true;
    }

}
