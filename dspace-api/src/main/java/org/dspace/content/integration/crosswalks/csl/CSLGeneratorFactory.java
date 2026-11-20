/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.csl;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory for instances of {@link CSLGenerator}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CSLGeneratorFactory {

    @Autowired
    private CSLNestedGenerator nestedGenerator;

    @Autowired
    private CSLWebServiceGenerator webServiceGenerator;

    public CSLGenerator getCSLGenerator() {
        return webServiceGenerator.isWebServiceAvailable() ? webServiceGenerator : nestedGenerator;
    }

}
