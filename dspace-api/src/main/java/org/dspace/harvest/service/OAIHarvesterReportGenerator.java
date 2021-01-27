/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest.service;

import java.io.InputStream;

import org.dspace.harvest.model.OAIHarvesterReport;

/**
 * Interface for classes that allow to generate a readable source starting from
 * the given OAI harvester report in a specific mime type.
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface OAIHarvesterReportGenerator {

    /**
     * Generate a report in a specific format and returns an inputstream to read it.
     *
     * @param  report the report to generate
     * @return        the input stream related to the generated report
     */
    InputStream generate(OAIHarvesterReport report);

    /**
     * Returns the mime type related to the generated report.
     *
     * @return the mime type
     */
    String getMimeType();

    /**
     * Returns the name of the generated report.
     *
     * @return the report's name
     */
    String getName();
}
