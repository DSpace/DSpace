/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.dspace.AbstractDSpaceTest;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Default-locale authority lookups must work even when that locale is not enabled in the UI.
 */
public class DCInputAuthorityTest extends AbstractDSpaceTest {

    private DCInputAuthority authority;

    /**
     * Configure a default locale that is absent from the supported UI locales.
     */
    @BeforeEach
    public void configureLocales() {
        ConfigurationService configuration = DSpaceServicesFactory.getInstance().getConfigurationService();
        configuration.setProperty("default.locale", "en");
        configuration.setProperty("webui.supported.locales", new String[] {"it", "uk"});
        DCInputAuthority.reset();
        DCInputAuthority.getPluginNames();
        authority = new DCInputAuthority();
        authority.setPluginInstanceName("common_iso_languages");
    }

    /**
     * Restore configuration and cached value pairs after each test.
     */
    @AfterEach
    public void restoreLocales() {
        DSpaceServicesFactory.getInstance().getConfigurationService().reloadConfig();
        DCInputAuthority.reset();
        DCInputAuthority.getPluginNames();
    }

    /**
     * Resolve an exact stored value using the default locale.
     */
    @Test
    public void bestMatchUsesDefaultLocale() {
        Choices choices = authority.getBestMatch("en", null);
        assertEquals(1, choices.values.length);
        assertEquals("en", choices.values[0].value);
        assertEquals("English", choices.values[0].label);
    }

    /**
     * Find suggestions using the default locale when no locale is supplied.
     */
    @Test
    public void matchesUseDefaultLocale() {
        assertTrue(authority.getMatches("English", 0, 10, null).values.length > 0);
    }

    /**
     * Resolve labels by stored values and retain the unknown-key fallback.
     */
    @Test
    public void labelIsLookedUpByStoredValue() {
        assertEquals("English", authority.getLabel("en", null));
        assertEquals("UNKNOWN KEY unknown", authority.getLabel("unknown", null));
    }
}
