/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.shell;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
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
 * java -jar /dspace/bin/dspace.jar -Ddspace.dir=/dspace
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
            scanBasePackages = {
                "org.dspace.shell"
            },
            exclude = { DataSourceAutoConfiguration.class }
)
@CommandScan
public class DSpaceShellApplication {

    private static final Logger log = LoggerFactory.getLogger(DSpaceShellApplication.class);

    public static void main(String[] args) {
        configureDSpaceDirectory();
        // Start Spring Boot (Spring Shell) application
        SpringApplication.run(DSpaceShellApplication.class, args);
    }

    private static void configureDSpaceDirectory() {
        if (System.getProperty("dspace.dir") != null) {
            return;
        }

        // Check environment variable first
        String envHome = System.getenv("DSPACE_HOME");

        if (envHome != null) {
            System.setProperty("dspace.dir", envHome);
            log.debug("Using DSPACE_HOME as dspace.dir={}", envHome);
            return;
        }

        // Try to detect from JAR location
        try {
            // Locate the running JAR
            String jarPath = new File(
                    DSpaceShellApplication.class
                            .getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()
            ).getAbsolutePath();

            File jarFile = new File(jarPath);
            File binDir = jarFile.getParentFile();   // .../dspace/bin
            File dspaceDir = binDir.getParentFile(); // .../dspace

            if (new File(dspaceDir, "config").exists()) {
                System.setProperty("dspace.dir", dspaceDir.getAbsolutePath());
                log.info("Auto-detected dspace.dir={}",
                        dspaceDir.getAbsolutePath());
                return;
            }

        } catch (Exception e) {
            log.error(e.getMessage());
        }

        String fallback = new File(".").getAbsolutePath();
        System.setProperty("dspace.dir", fallback);

        log.warn("Could not determine DSpace directory. Using {}", fallback);

    }
}
