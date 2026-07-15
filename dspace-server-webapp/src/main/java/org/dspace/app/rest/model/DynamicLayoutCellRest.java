/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A cell of a {@link DynamicLayoutRowRest}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class DynamicLayoutCellRest {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String style;

    private List<DynamicLayoutBoxRest> boxes = new ArrayList<>();

    /**
     * Returns the style.
     */
    public String getStyle() {
        return style;
    }

    /**
     * Sets the style.
     */
    public void setStyle(String style) {
        this.style = style;
    }

    /**
     * Returns the boxes.
     */
    public List<DynamicLayoutBoxRest> getBoxes() {
        return boxes;
    }

    /**
     * Sets the boxes.
     */
    public void setBoxes(List<DynamicLayoutBoxRest> boxes) {
        this.boxes = boxes;
    }

}
