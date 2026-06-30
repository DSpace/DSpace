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
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@Embeddable
public class CrisLayoutTab2BoxId implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 6453272496828531598L;

    @Column(name = "cris_layout_tab_id")
    private Integer crisLayoutTabId;
    @Column(name = "cris_layout_box_id")
    private Integer crisLayoutBoxId;

    public CrisLayoutTab2BoxId() {}

    public CrisLayoutTab2BoxId(Integer tabId, Integer boxId) {
        this.crisLayoutTabId = tabId;
        this.crisLayoutBoxId = boxId;
    }
    public Integer getCrisLayoutTabId() {
        return crisLayoutTabId;
    }
    public void setCrisLayoutTabId(Integer tabId) {
        this.crisLayoutTabId = tabId;
    }
    public Integer getCrisLayoutBoxId() {
        return crisLayoutBoxId;
    }
    public void setCrisLayoutBoxId(Integer boxId) {
        this.crisLayoutBoxId = boxId;
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((crisLayoutBoxId == null) ? 0 : crisLayoutBoxId.hashCode());
        result = prime * result + ((crisLayoutTabId == null) ? 0 : crisLayoutTabId.hashCode());
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
        CrisLayoutTab2BoxId other = (CrisLayoutTab2BoxId) obj;
        if (crisLayoutBoxId == null) {
            if (other.crisLayoutBoxId != null) {
                return false;
            }
        } else if (!crisLayoutBoxId.equals(other.crisLayoutBoxId)) {
            return false;
        }
        if (crisLayoutTabId == null) {
            if (other.crisLayoutTabId != null) {
                return false;
            }
        } else if (!crisLayoutTabId.equals(other.crisLayoutTabId)) {
            return false;
        }
        return true;
    }

}
