/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Utility methods for tomcat clustering.
 */
public class ClusteringUtil {

    private ClusteringUtil() {
    }

    static private final ConfigurationService configurationService =
        DSpaceServicesFactory.getInstance().getConfigurationService();

    /**
     * Retrieves the current clustering instance uuid, or a randomly generated uuid if it does not exist.
     * Stores any generated uuid in {@code {dspace.dir}/config/clustering-identifier.txt}.
     */
    static public UUID createOrGetClusteringUuid() {
        String path =
            String.format("%s/config/clustering-identifier.txt", configurationService.getProperty("dspace.dir"));
        File file = new File(path);
        UUID uuid;
        try {
            if (!file.createNewFile()) {
                // File already exists
                String uuidString = new String(Files.readAllBytes(Path.of(path)));
                if (!uuidString.isEmpty()) {
                    // And contains uuid
                    return UUID.fromString(uuidString.split("\n", 2)[0]);
                }
            }
            // No file exists, or file does not contain uuid
            // Generate a new uuid
            uuid = UUID.randomUUID();
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
            out.write(uuid.toString().getBytes());
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return uuid;
    }
}
