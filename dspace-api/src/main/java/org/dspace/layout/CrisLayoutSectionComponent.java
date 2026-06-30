/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout;

/**
 * Interface to mark classes as SectionComponent.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public interface CrisLayoutSectionComponent {

    /**
     * Returns the component style classes.
     *
     * @return the style as String
     */
    public String getStyle();

}
