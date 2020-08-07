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
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@Entity
@Table(name = "cris_layout_tab2box")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, include = "non-lazy")
public class CrisLayoutTab2Box {

    @EmbeddedId
    private CrisLayoutTab2BoxId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("id.crisLayoutTabId")
    @JoinColumn(name = "cris_layout_tab_id")
    private CrisLayoutTab tab;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("id.crisLayoutBoxId")
    @JoinColumn(name = "cris_layout_box_id")
    private CrisLayoutBox box;

    @Column(name = "position")
    private Integer position;

    public CrisLayoutTab2Box() {}

    public CrisLayoutTab2Box(CrisLayoutTab tab, CrisLayoutBox box, int position) {
        this.tab = tab;
        this.box = box;
        this.position = position;
        this.id = new CrisLayoutTab2BoxId(tab.getID(), box.getID());
    }

    public CrisLayoutTab2BoxId getId() {
        return id;
    }

    public void setId(CrisLayoutTab2BoxId id) {
        this.id = id;
    }

    public CrisLayoutTab getTab() {
        return tab;
    }

    public void setTab(CrisLayoutTab tab) {
        this.tab = tab;
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
        result = prime * result + ((box == null) ? 0 : box.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((position == null) ? 0 : position.hashCode());
        result = prime * result + ((tab == null) ? 0 : tab.hashCode());
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
        CrisLayoutTab2Box other = (CrisLayoutTab2Box) obj;
        if (box == null) {
            if (other.box != null) {
                return false;
            }
        } else if (!box.equals(other.box)) {
            return false;
        }
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (position == null) {
            if (other.position != null) {
                return false;
            }
        } else if (!position.equals(other.position)) {
            return false;
        }
        if (tab == null) {
            if (other.tab != null) {
                return false;
            }
        } else if (!tab.equals(other.tab)) {
            return false;
        }
        return true;
    }

}
