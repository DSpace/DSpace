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
import org.dspace.layout.CrisLayoutTab;

/**
 * Cris layout configuration tool converter.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 *
 */
public interface CrisLayoutToolConverter {

    /**
     * convert the given list of tabs to workbook.
     *
     * @param  tabs                     the tabs to convert
     * @return                          the workbook
     */
    Workbook convert(List<CrisLayoutTab> tabs);
}
