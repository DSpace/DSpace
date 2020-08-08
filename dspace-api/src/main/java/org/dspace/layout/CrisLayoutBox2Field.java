/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout;

import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;

import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * This class map the join table for relation beetwen the objects
 * CrisLayoutBox and CrisLayoutField
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@Entity
@Table(name = "cris_layout_box2field")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, include = "non-lazy")
public class CrisLayoutBox2Field {

    @EmbeddedId
    private CrisLayoutBox2FieldId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("id.crisLayoutBoxId")
    @JoinColumn(name = "cris_layout_box_id")
    private CrisLayoutBox box;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @MapsId("id.crisLayoutFieldId")
    @JoinColumn(name = "cris_layout_field_id")
    private CrisLayoutField field;

    @Column(name = "position")
    private Integer position;

    public CrisLayoutBox2Field() {}

    public CrisLayoutBox2Field(CrisLayoutBox box, CrisLayoutField field, int position) {
        this.box = box;
        this.field = field;
        this.position = position;
        this.id = new CrisLayoutBox2FieldId(box.getID(), field.getID());
    }

    public CrisLayoutBox2FieldId getId() {
        return id;
    }

    public void setId(CrisLayoutBox2FieldId id) {
        this.id = id;
    }

    public CrisLayoutBox getBox() {
        return box;
    }

    public void setBox(CrisLayoutBox box) {
        this.box = box;
    }

    public CrisLayoutField getField() {
        return field;
    }

    public void setField(CrisLayoutField field) {
        this.field = field;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
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
        CrisLayoutBox2Field other = (CrisLayoutBox2Field) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }

}
