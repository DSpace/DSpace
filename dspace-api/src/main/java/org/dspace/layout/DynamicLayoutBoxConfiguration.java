/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout;

/**
 * This is a wrapper class around the DynamicLayoutBox needed by the REST layer as
 * we need to have a correspondence between the api and the rest model
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class DynamicLayoutBoxConfiguration {
    private DynamicLayoutBox box;

    public DynamicLayoutBoxConfiguration(DynamicLayoutBox box) {
        this.box = box;
    }

    public DynamicLayoutBox getLayoutBox() {
        return box;
    }
}
