/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.maxmind.geoip2.DatabaseReader;
import org.apache.commons.lang3.StringUtils;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service that handle the GeoIP database file.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class GeoIpService {

    @Autowired
    private ConfigurationService configurationService;

    /**
     * Returns an instance of {@link DatabaseReader} based on the configured db
     * file, if any.
     *
     * @return                       the Database reader
     * @throws IllegalStateException if the db file is not configured correctly
     */
    public DatabaseReader getDatabaseReader() throws IllegalStateException {
        String dbPath = configurationService.getProperty("usage-statistics.dbfile");
        if (StringUtils.isBlank(dbPath)) {
            throw new IllegalStateException("The required 'dbfile' configuration is missing in usage-statistics.cfg!");
        }

        try {
            File dbFile = new File(dbPath);
            return new DatabaseReader.Builder(dbFile).build();
        } catch (FileNotFoundException fe) {
            throw new IllegalStateException(
                "The GeoLite Database file is missing (" + dbPath + ")! Solr Statistics cannot generate location " +
                    "based reports! Please see the DSpace installation instructions for instructions to install " +
                    "this file.",fe);
        } catch (IOException e) {
            throw new IllegalStateException(
                "Unable to load GeoLite Database file (" + dbPath + ")! You may need to reinstall it. See the " +
                    "DSpace installation instructions for more details.", e);
        }
    }
}
