/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout;

import jakarta.persistence.Cacheable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * This class map the join table for relation beetwen the objects
 * DynamicLayoutBox and DynamicLayoutField
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@Entity
@Table(name = "dynamic_layout_box2field")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, include = "non-lazy")
public class DynamicLayoutBox2Field {

    @EmbeddedId
    private DynamicLayoutBox2FieldId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("id.dynamicLayoutBoxId")
    @JoinColumn(name = "dynamic_layout_box_id")
    private DynamicLayoutBox box;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @MapsId("id.dynamicLayoutFieldId")
    @JoinColumn(name = "dynamic_layout_field_id")
    private DynamicLayoutField field;

    @Column(name = "position")
    private Integer position;

    /**
     * Default constructor.
     */
    public DynamicLayoutBox2Field() {}

    /**
     * Creates a box-to-field association for the given box, field and position.
     *
     * @param box the layout box
     * @param field the layout field
     * @param position the position of the field within the box
     */
    public DynamicLayoutBox2Field(DynamicLayoutBox box, DynamicLayoutField field, int position) {
        this.box = box;
        this.field = field;
        this.position = position;
        this.id = new DynamicLayoutBox2FieldId(box.getID(), field.getID());
    }

    /**
     * Returns the id.
     */
    public DynamicLayoutBox2FieldId getId() {
        return id;
    }

    /**
     * Sets the id.
     */
    public void setId(DynamicLayoutBox2FieldId id) {
        this.id = id;
    }

    /**
     * Returns the box.
     */
    public DynamicLayoutBox getBox() {
        return box;
    }

    /**
     * Sets the box.
     */
    public void setBox(DynamicLayoutBox box) {
        this.box = box;
    }

    /**
     * Returns the field.
     */
    public DynamicLayoutField getField() {
        return field;
    }

    /**
     * Sets the field.
     */
    public void setField(DynamicLayoutField field) {
        this.field = field;
    }

    /**
     * Returns the position.
     */
    public Integer getPosition() {
        return position;
    }

    /**
     * Sets the position.
     */
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
        DynamicLayoutBox2Field other = (DynamicLayoutBox2Field) obj;
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
