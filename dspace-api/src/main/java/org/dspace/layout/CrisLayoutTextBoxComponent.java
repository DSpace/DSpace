/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.layout;

import java.util.List;

/**
 * Implementation of {@link CrisLayoutSectionComponent} that holds and returns
 * a list of {@link CrisLayoutTextRowComponent} so that their content can be
 * rendered
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
public class CrisLayoutTextBoxComponent implements CrisLayoutSectionComponent {

    private final List<CrisLayoutTextRowComponent> textRows;
    private final String style;

    public CrisLayoutTextBoxComponent(List<CrisLayoutTextRowComponent> textRows, String style) {
        this.textRows = textRows;
        this.style = style;
    }

    @Override
    public String getStyle() {
        return style;
    }

    public List<CrisLayoutTextRowComponent> getTextRows() {
        return textRows;
    }
}
