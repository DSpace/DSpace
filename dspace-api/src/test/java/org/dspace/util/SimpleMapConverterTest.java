/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.dspace.services.ConfigurationService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link SimpleMapConverter}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class SimpleMapConverterTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Mock
    private ConfigurationService configurationService;

    private File dspaceDir;

    private File crosswalksDir;

    @Before
    public void before() throws IOException {
        dspaceDir = folder.getRoot();
        crosswalksDir = folder.newFolder("config", "crosswalks");
    }

    @Test
    public void testPropertiesParsing() throws IOException {

        when(configurationService.getProperty("dspace.dir")).thenReturn(dspaceDir.getAbsolutePath());
        createFileInFolder(crosswalksDir, "test.properties", "key1=value1\nkey2=value2\nkey3=value3");

        SimpleMapConverter simpleMapConverter = new SimpleMapConverter();
        simpleMapConverter.setConfigurationService(configurationService);
        simpleMapConverter.setConverterNameFile("test.properties");

        simpleMapConverter.init();

        assertThat(simpleMapConverter.getValue("key1"), is("value1"));
        assertThat(simpleMapConverter.getValue("key2"), is("value2"));
        assertThat(simpleMapConverter.getValue("key3"), is("value3"));
        assertThat(simpleMapConverter.getValue(""), is(""));
        assertThat(simpleMapConverter.getValue(null), nullValue());

        assertThat(simpleMapConverter.getValue("key4"), is("key4"));

    }

    @Test
    public void testPropertiesParsingWithDefaultValue() throws IOException {

        when(configurationService.getProperty("dspace.dir")).thenReturn(dspaceDir.getAbsolutePath());
        createFileInFolder(crosswalksDir, "test.properties", "key1=value1\nkey2=value2\nkey3=value3");

        SimpleMapConverter simpleMapConverter = new SimpleMapConverter();
        simpleMapConverter.setConfigurationService(configurationService);
        simpleMapConverter.setConverterNameFile("test.properties");
        simpleMapConverter.setDefaultValue("default");

        simpleMapConverter.init();

        assertThat(simpleMapConverter.getValue("key1"), is("value1"));
        assertThat(simpleMapConverter.getValue("key2"), is("value2"));
        assertThat(simpleMapConverter.getValue("key3"), is("value3"));
        assertThat(simpleMapConverter.getValue(""), is("default"));
        assertThat(simpleMapConverter.getValue(null), is("default"));

        assertThat(simpleMapConverter.getValue("key4"), is("default"));

    }

    @Test
    public void testPropertiesParsingWithAnUnexistingFile() throws IOException {

        when(configurationService.getProperty("dspace.dir")).thenReturn(dspaceDir.getAbsolutePath());

        SimpleMapConverter simpleMapConverter = new SimpleMapConverter();
        simpleMapConverter.setConfigurationService(configurationService);
        simpleMapConverter.setConverterNameFile("test.properties");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> simpleMapConverter.init());

        // Get path separator used for this platform (eg. / for Linux, \ for Windows)
        String separator = File.separator;

        assertThat(exception.getMessage(),
            is("An error occurs parsing " + dspaceDir.getAbsolutePath() + separator + "config" + separator
                    + "crosswalks" + separator + "test.properties"));

        Throwable cause = exception.getCause();
        assertThat(cause, notNullValue());
        assertThat(cause, instanceOf(FileNotFoundException.class));

    }

    @Test
    public void testPropertiesParsingWithCorruptedFile() throws IOException {

        when(configurationService.getProperty("dspace.dir")).thenReturn(dspaceDir.getAbsolutePath());
        createFileInFolder(crosswalksDir, "test.properties", "key1=value1\nkey2\nkey3=value3");

        SimpleMapConverter simpleMapConverter = new SimpleMapConverter();
        simpleMapConverter.setConfigurationService(configurationService);
        simpleMapConverter.setConverterNameFile("test.properties");

        simpleMapConverter.init();

        assertThat(simpleMapConverter.getValue("key1"), is("value1"));
        assertThat(simpleMapConverter.getValue("key2"), is("key2"));
        assertThat(simpleMapConverter.getValue("key3"), is("value3"));

        assertThat(simpleMapConverter.getValue("key4"), is("key4"));


    }

    @Test
    public void testPropertiesParsingWithEmptyFile() throws IOException {

        when(configurationService.getProperty("dspace.dir")).thenReturn(dspaceDir.getAbsolutePath());
        createFileInFolder(crosswalksDir, "test.properties", "");

        SimpleMapConverter simpleMapConverter = new SimpleMapConverter();
        simpleMapConverter.setConfigurationService(configurationService);
        simpleMapConverter.setConverterNameFile("test.properties");

        simpleMapConverter.init();

        assertThat(simpleMapConverter.getValue("key1"), is("key1"));
        assertThat(simpleMapConverter.getValue("key2"), is("key2"));

    }

    private void createFileInFolder(File folder, String name, String content) throws IOException {
        File file = new File(folder, name);
        FileUtils.write(file, content, StandardCharsets.UTF_8);
    }

}
