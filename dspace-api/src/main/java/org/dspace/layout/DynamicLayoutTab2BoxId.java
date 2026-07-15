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
public class DynamicLayoutTab2BoxId implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 6453272496828531598L;

    @Column(name = "dynamic_layout_tab_id")
    private Integer dynamicLayoutTabId;
    @Column(name = "dynamic_layout_box_id")
    private Integer dynamicLayoutBoxId;

    public DynamicLayoutTab2BoxId() {}

    public DynamicLayoutTab2BoxId(Integer tabId, Integer boxId) {
        this.dynamicLayoutTabId = tabId;
        this.dynamicLayoutBoxId = boxId;
    }
    public Integer getDynamicLayoutTabId() {
        return dynamicLayoutTabId;
    }
    public void setDynamicLayoutTabId(Integer tabId) {
        this.dynamicLayoutTabId = tabId;
    }
    public Integer getDynamicLayoutBoxId() {
        return dynamicLayoutBoxId;
    }
    public void setDynamicLayoutBoxId(Integer boxId) {
        this.dynamicLayoutBoxId = boxId;
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dynamicLayoutBoxId == null) ? 0 : dynamicLayoutBoxId.hashCode());
        result = prime * result + ((dynamicLayoutTabId == null) ? 0 : dynamicLayoutTabId.hashCode());
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
        DynamicLayoutTab2BoxId other = (DynamicLayoutTab2BoxId) obj;
        if (dynamicLayoutBoxId == null) {
            if (other.dynamicLayoutBoxId != null) {
                return false;
            }
        } else if (!dynamicLayoutBoxId.equals(other.dynamicLayoutBoxId)) {
            return false;
        }
        if (dynamicLayoutTabId == null) {
            if (other.dynamicLayoutTabId != null) {
                return false;
            }
        } else if (!dynamicLayoutTabId.equals(other.dynamicLayoutTabId)) {
            return false;
        }
        return true;
    }

}
