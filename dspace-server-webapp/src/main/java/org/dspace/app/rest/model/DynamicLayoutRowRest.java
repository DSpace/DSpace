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
 * A row of a {@link DynamicLayoutTabRest} resource.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class DynamicLayoutRowRest {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String style;

    private List<DynamicLayoutCellRest> cells = new ArrayList<>();

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public List<DynamicLayoutCellRest> getCells() {
        return cells;
    }

    public void setCells(List<DynamicLayoutCellRest> cells) {
        this.cells = cells;
    }

}
