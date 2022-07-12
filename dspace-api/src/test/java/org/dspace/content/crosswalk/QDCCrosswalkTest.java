/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.crosswalk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.dspace.AbstractDSpaceTest;
import org.dspace.core.service.PluginService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.jdom2.Namespace;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author mwood
 */
public class QDCCrosswalkTest
        extends AbstractDSpaceTest {
    private static final String PLUGIN_NAME = "qdctest";

    private static final String NAMESPACE_PREFIX = "dc";

    private static final String NAMESPACE_PROPERTY
            = "crosswalk.qdc.namespace." + PLUGIN_NAME + "." + NAMESPACE_PREFIX;

    private static final String NAMESPACE_URI = "http://purl.org/dc/elements/1.1/";

    private static final String SCHEMALOCATION_KEY = "crosswalk.qdc.schemaLocation." + PLUGIN_NAME;

    private static final String SCHEMALOCATION = "http://purl.org/dc/terms/"
            + " http://dublincore.org/schemas/xmls/qdc/2006/01/06/dcterms.xsd";

    private static final String PROPERTIES_PREFIX = "crosswalk.qdc.properties.";

    private static final String PROPERTIES_KEY = PROPERTIES_PREFIX + PLUGIN_NAME;

    private static final String PROPERTIES = "crosswalks/QDC.properties";

    private static PluginService pluginService;

    @BeforeClass
    public static void setUpClass() {
        DSpaceServicesFactory dsf = DSpaceServicesFactory.getInstance();

        pluginService = dsf.getServiceManager()
                .getServiceByName(null, PluginService.class);

        ConfigurationService cfg = dsf.getConfigurationService();

        cfg.setProperty("crosswalk.selfnamed." + IngestionCrosswalk.class.getName(),
                QDCCrosswalk.class.getName());
        cfg.setProperty(NAMESPACE_PROPERTY, NAMESPACE_URI);
        cfg.setProperty(SCHEMALOCATION_KEY, SCHEMALOCATION);
        // Clear out other aliases.  Magical:  see plugin for details.
        for (String property : cfg.getPropertyKeys(PROPERTIES_PREFIX)) {
            cfg.setProperty(property, null);
        }
        cfg.setProperty(PROPERTIES_KEY, PROPERTIES);
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        // make sure that the config properties set in @BeforeClass are picked up
        QDCCrosswalk.initStatic();
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getPluginNames method, of class QDCCrosswalk.
     */
    @Test
    public void testGetPluginNames() {
        String[] names = QDCCrosswalk.getPluginNames();
        assertEquals("Wrong number of plugin names.", 1, names.length);
        assertEquals("Plugin should be named '" + PLUGIN_NAME + "'.", PLUGIN_NAME, names[0]);
    }

    /**
     * Test of getNamespaces method, of class QDCCrosswalk.
     */
    @Test
    public void testGetNamespaces() {
        QDCCrosswalk instance = (QDCCrosswalk) pluginService.getNamedPlugin(
                IngestionCrosswalk.class, PLUGIN_NAME);
        Namespace[] namespaces = instance.getNamespaces();
        boolean found_prefix = false;
        for (Namespace namespace : namespaces) {
            found_prefix |= namespace.getPrefix().equals(NAMESPACE_PREFIX);
        }
        assertTrue("Should know namespace " + NAMESPACE_PREFIX + '.', found_prefix);
    }

    /**
     * Test of getSchemaLocation method, of class QDCCrosswalk.
     */
    @Test
    public void testGetSchemaLocation() {
        QDCCrosswalk instance = (QDCCrosswalk) pluginService.getNamedPlugin(
                IngestionCrosswalk.class, PLUGIN_NAME);
        String schemaLocation = instance.getSchemaLocation();
        System.out.println(schemaLocation);
        assertEquals("SchemaLocation did not match.", SCHEMALOCATION, schemaLocation);
    }

    /**
     * Test of disseminateList method, of class QDCCrosswalk.
     * @throws java.lang.Exception passed through.
     */
    @Ignore
    @Test
    public void testDisseminateList() throws Exception {
    }

    /**
     * Test of disseminateElement method, of class QDCCrosswalk.
     * @throws java.lang.Exception passed through.
     */
    @Ignore
    @Test
    public void testDisseminateElement() throws Exception {
    }

    /**
     * Test of canDisseminate method, of class QDCCrosswalk.
     */
    @Ignore
    @Test
    public void testCanDisseminate() {
    }

    /**
     * Test of ingest method, of class QDCCrosswalk.
     * @throws java.lang.Exception passed through.
     */
    @Ignore
    @Test
    public void testIngest_4args_1() throws Exception {
    }

    /**
     * Test of ingest method, of class QDCCrosswalk.
     * @throws java.lang.Exception passed through.
     */
    @Ignore
    @Test
    public void testIngest_4args_2() throws Exception {
    }

    /**
     * Test of preferList method, of class QDCCrosswalk.
     */
    @Test
    public void testPreferList() {
        QDCCrosswalk instance = (QDCCrosswalk) pluginService.getNamedPlugin(
                IngestionCrosswalk.class, PLUGIN_NAME);
        assertTrue("QDC crosswalk should prefer list.", instance.preferList());
    }
}
