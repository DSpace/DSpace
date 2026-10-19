/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app;

import org.dspace.app.rest.WebApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring boot application for integration tests.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@SpringBootApplication(scanBasePackageClasses = WebApplication.class)
public class TestApplication {

}
