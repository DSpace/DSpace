/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.script.service;

import java.util.List;

import org.apache.poi.ss.usermodel.Workbook;
import org.dspace.core.Context;
import org.dspace.layout.DynamicLayoutTab;

/**
 * Cris layout configuration tool parser.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface DynamicLayoutToolParser {

    /**
     * Parse the given workbook building a list of tabs.
     *
     * @param  context                  the DSpace context
     * @param  workbook                 the workbook to parse
     * @return                          the list of the extracted tabs
     * @throws IllegalArgumentException if the given workbook is not valid
     */
    List<DynamicLayoutTab> parse(Context context, Workbook workbook);
}
