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
 * This class defines the Id for the relation beetwen the objects CrisLayoutBox and CrisLayoutBox
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@Embeddable
public class CrisLayoutBox2FieldId implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -2649635188820470846L;

    @Column(name = "cris_layout_box_id")
    private Integer crisLayoutBoxId;
    @Column(name = "cris_layout_field_id")
    private Integer crisLayoutFieldId;

    public CrisLayoutBox2FieldId() {}

    public CrisLayoutBox2FieldId(Integer boxId, Integer fieldId) {
        this.crisLayoutFieldId = fieldId;
        this.crisLayoutBoxId = boxId;
    }

    public Integer getCrisLayoutBoxId() {
        return crisLayoutBoxId;
    }

    public void setCrisLayoutBoxId(Integer boxId) {
        this.crisLayoutBoxId = boxId;
    }

    public Integer getCrisLayoutFieldId() {
        return crisLayoutFieldId;
    }

    public void setCrisLayoutFieldId(Integer fieldId) {
        this.crisLayoutFieldId = fieldId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((crisLayoutBoxId == null) ? 0 : crisLayoutBoxId.hashCode());
        result = prime * result + ((crisLayoutFieldId == null) ? 0 : crisLayoutFieldId.hashCode());
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
        CrisLayoutBox2FieldId other = (CrisLayoutBox2FieldId) obj;
        if (crisLayoutBoxId == null) {
            if (other.crisLayoutBoxId != null) {
                return false;
            }
        } else if (!crisLayoutBoxId.equals(other.crisLayoutBoxId)) {
            return false;
        }
        if (crisLayoutFieldId == null) {
            if (other.crisLayoutFieldId != null) {
                return false;
            }
        } else if (!crisLayoutFieldId.equals(other.crisLayoutFieldId)) {
            return false;
        }
        return true;
    }

}
