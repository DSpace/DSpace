/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * This class map the join table for relation beetwen the objects
 * DynamicLayoutBox and DynamicLayoutMetric
 * 
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 *
 */
@Entity
@Table(name = "dynamic_layout_metric2box")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, include = "non-lazy")
public class DynamicLayoutMetric2Box {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dynamic_layout_metric2box_id_seq")
    @SequenceGenerator(name = "dynamic_layout_metric2box_id_seq", sequenceName = "dynamic_layout_metric2box_id_seq",
        allocationSize = 1)
    @Column(name = "id", unique = true, nullable = false, insertable = true, updatable = false)
    private Integer id;

    @Column(name = "metric_type")
    private String type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dynamic_layout_box_id")
    private DynamicLayoutBox box;

    @Column(name = "position")
    private Integer position;

    public DynamicLayoutMetric2Box() {}

    public DynamicLayoutMetric2Box(DynamicLayoutBox box, String type, int position) {
        this.box = box;
        this.position = position;
        this.type = type;
        this.box.addMetric2box(this);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public DynamicLayoutBox getBox() {
        return box;
    }

    public void setBox(DynamicLayoutBox box) {
        this.box = box;
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
        DynamicLayoutMetric2Box other = (DynamicLayoutMetric2Box) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
