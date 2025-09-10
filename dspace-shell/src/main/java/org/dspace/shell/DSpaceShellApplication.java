/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.shell;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.shell.command.annotation.CommandScan;

/**
 * Main entry point for the DSpace Spring Shell application.
 * <p>
 * This class bootstraps a Spring Boot application with Spring Shell integration.
 * It scans for commands under the {@code org.dspace.shell} package and excludes
 * the default {@link org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration}
 * to avoid Spring Boot attempting to configure its own DataSource.
 * </p>
 * <p>
 * When executed, this application provides a modular command-line interface (CLI)
 * for DSpace, allowing execution of custom commands defined in the shell module.
 * </p>
 *
 * <p><b>Usage:</b></p>
 * <pre>{@code
 * java -jar /dspace/bin/dspace.jar
 * }</pre>
 *
 * <p>The {@code @CommandScan} annotation ensures that all available Spring Shell
 * commands are discovered and registered automatically at startup.</p>
 *
 * @author Paulo Graça
 * @since DSpace 10.0
 */
@SuppressWarnings({ "checkstyle:hideutilityclassconstructor" })
@SpringBootApplication(
        scanBasePackages = {"org.dspace.shell"},
        exclude = { DataSourceAutoConfiguration.class }
    )
@CommandScan
public class DSpaceShellApplication {
    public static void main(String[] args) {
        // Always ensure dspace.dir is set BEFORE starting Spring
        if (System.getProperty("dspace.dir") == null) {
            try {
                // Locate the running JAR
                String jarPath = new java.io.File(
                        DSpaceShellApplication.class
                                .getProtectionDomain()
                                .getCodeSource()
                                .getLocation()
                                .toURI()
                ).getAbsolutePath();

                java.io.File jarFile = new java.io.File(jarPath);
                java.io.File binDir = jarFile.getParentFile();   // .../dspace/bin
                java.io.File dspaceDir = binDir.getParentFile(); // .../dspace

                System.setProperty("dspace.dir", dspaceDir.getAbsolutePath());
                System.out.println("Auto-detected dspace.dir=" + dspaceDir.getAbsolutePath());
            } catch (Exception e) {
                System.err.println("Failed to auto-detect dspace.dir, using current directory");
                System.setProperty("dspace.dir", new java.io.File(".").getAbsolutePath());
            }
        }
        // Start Spring Boot (Spring Shell) application
        SpringApplication.run(DSpaceShellApplication.class, args);
    }
}
