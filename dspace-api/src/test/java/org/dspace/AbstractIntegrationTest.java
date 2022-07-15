/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.dspace.utils.DSpace;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;

/**
 * This is the base class for Integration Tests. It inherits from the class
 * AbstractUnitTest the structure (database, file system) required by DSpace to
 * run tests.
 *
 * It also contains some generic mocks and utilities that are needed by the
 * integration tests developed for DSpace
 *
 * @author pvillega
 */
@Ignore
public class AbstractIntegrationTest extends AbstractUnitTest {

    /**
     * this is the amount of time that guarantee us that changes to the configuration files are picked up.
     * The actual refresh rate is defined in <code>dspace/config/config-definition.xml</code>
     */
    private static final int CONFIG_RELOAD_TIME = 5500;

    /**
     * holds the size of the local.cfg file, see {@link #cleanExtraConfigurations()}
     **/
    private long initialLocalCfgSize;

    /**
     * set to true if the local cfg has been manipulated
     */
    private boolean localCfgChanged = false;

    @Override
    @Before
    /**
     * Extend the {@link AbstractUnitTest#init} method to deal with extra
     * configuration that can be manipulated at runtime during the Integration Test
     */
    public void init() {
        super.init();
        String extraConfPath = getLocalConfigurationFilePath();
        FileChannel fileOpen;
        try {
            fileOpen = FileChannel.open(Paths.get(extraConfPath), StandardOpenOption.READ);
            initialLocalCfgSize = fileOpen.size();
            fileOpen.close();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    @After
    /**
     * Extend the {@link AbstractUnitTest#destroy} method to deal with extra
     * configurations that can be manipulated at runtime during the Integration Test
     */
    public void destroy() {
        super.destroy();
        cleanExtraConfigurations();
    }

    /**
     * Restore the local.cfg file to its initial size
     */
    protected void cleanExtraConfigurations() {
        if (!localCfgChanged) {
            // return immediately as no changes have been applied so we can avoid to wait
            // for configuration reload
            return;
        }
        String extraConfPath = getLocalConfigurationFilePath();
        try {
            FileChannel.open(Paths.get(extraConfPath), StandardOpenOption.WRITE)
                .truncate(initialLocalCfgSize).close();
            localCfgChanged = false;
            // sleep to give the time to the configuration to note the change
            Thread.sleep(CONFIG_RELOAD_TIME);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * 
     * @return the full path to the in use local.cfg file
     */
    private String getLocalConfigurationFilePath() {
        return new DSpace().getConfigurationService()
                .getProperty("dspace.dir") + "/config/local.cfg";
    }

    /**
     * Append the input text to the current local.cfg file assuring
     * that the new text goes in a new line and sleep enough time to allow the
     * configuration reload
     * 
     * @param textToAppend
     */
    protected void appendToLocalConfiguration(String textToAppend) {
        String extraConfPath = getLocalConfigurationFilePath();
        try (Writer output = new BufferedWriter(
                new FileWriter(extraConfPath, StandardCharsets.UTF_8, true))) {
            output.append("\n");
            output.append(textToAppend);
            output.flush();
            output.close();
            localCfgChanged = true;
            // sleep to give the time to the configuration to note the change
            Thread.sleep(CONFIG_RELOAD_TIME);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
