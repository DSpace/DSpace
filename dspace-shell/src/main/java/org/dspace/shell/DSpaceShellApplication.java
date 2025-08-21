/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.shell;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.shell.command.annotation.CommandScan;

@SpringBootApplication(scanBasePackages = {"org.dspace.shell", "org.dspace"})
@CommandScan
public class DSpaceShellApplication {
    public static void main(String[] args) {
        SpringApplication.run(DSpaceShellApplication.class, args);
    }
}
