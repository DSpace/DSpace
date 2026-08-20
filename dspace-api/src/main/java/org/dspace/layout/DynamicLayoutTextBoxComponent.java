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
 * Implementation of {@link DynamicLayoutSectionComponent} that holds and returns
 * a list of {@link DynamicLayoutTextRowComponent} so that their content can be
 * rendered
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
public class DynamicLayoutTextBoxComponent implements DynamicLayoutSectionComponent {

    private final List<DynamicLayoutTextRowComponent> textRows;
    private final String style;

    /**
     * Creates a text box component with the given rows and style.
     *
     * @param textRows the text rows of the component
     * @param style the CSS style of the component
     */
    public DynamicLayoutTextBoxComponent(List<DynamicLayoutTextRowComponent> textRows, String style) {
        this.textRows = textRows;
        this.style = style;
    }

    @Override
    public String getStyle() {
        return style;
    }

    /**
     * Returns the text rows.
     */
    public List<DynamicLayoutTextRowComponent> getTextRows() {
        return textRows;
    }
}
