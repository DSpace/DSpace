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
 * A cell of a {@link CrisLayoutRowRest}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CrisLayoutCellRest {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String style;

    private List<CrisLayoutBoxRest> boxes = new ArrayList<>();

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public List<CrisLayoutBoxRest> getBoxes() {
        return boxes;
    }

    public void setBoxes(List<CrisLayoutBoxRest> boxes) {
        this.boxes = boxes;
    }

}
