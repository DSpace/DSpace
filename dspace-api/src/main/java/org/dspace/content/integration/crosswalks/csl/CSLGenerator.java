/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.csl;

/**
 * CSL generator to create citations in the specified style and format.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface CSLGenerator {

    /**
     * Returns the citations in the given style and format for the provided items.
     *
     * @param  itemDataProvider the items to handle
     * @param  style            the citation style
     * @param  format           the citation format
     * @return                  the generated citations
     */
    CSLResult generate(DSpaceListItemDataProvider itemDataProvider, String style, String format);

}
