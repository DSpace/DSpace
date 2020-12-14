/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout;

import javax.persistence.Cacheable;
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
 * CrisLayoutBox and CrisLayoutMetric
 * 
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 *
 */
@Entity
@Table(name = "cris_layout_metric2box")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, include = "non-lazy")
public class CrisLayoutMetric2Box {

    @EmbeddedId
    private CrisLayoutMetric2BoxId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("id.crisLayoutBoxId")
    @JoinColumn(name = "cris_layout_box_id")
    private CrisLayoutBox box;

    @Column(name = "position")
    private Integer position;

    public CrisLayoutMetric2Box() {}

    public CrisLayoutMetric2Box(CrisLayoutBox box, String type, int position) {
        this.box = box;
        this.position = position;
        this.id = new CrisLayoutMetric2BoxId(box.getID(), type);
    }

    public CrisLayoutMetric2BoxId getId() {
        return id;
    }

    public void setId(CrisLayoutMetric2BoxId id) {
        this.id = id;
    }

    public CrisLayoutBox getBox() {
        return box;
    }

    public void setBox(CrisLayoutBox box) {
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
        CrisLayoutMetric2Box other = (CrisLayoutMetric2Box) obj;
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
        return id.getCrisLayoutMetricId();
    }

    public void setType(String type) {
        this.id.setCrisLayoutMetricId(type);
    }


}
