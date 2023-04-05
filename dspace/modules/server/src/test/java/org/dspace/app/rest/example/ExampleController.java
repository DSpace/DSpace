/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.example;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * This Controller serves as an example of how & where to add local customizations to the DSpace REST API.
 * See {@link ExampleControllerIT} for the integration tests for this controller.
 */
@RestController
@RequestMapping("example")
public class ExampleController {

    @RequestMapping("")
    public String test() {
        return "Hello world";
    }
}
