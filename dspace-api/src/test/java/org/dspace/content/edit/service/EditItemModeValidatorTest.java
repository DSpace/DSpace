/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.edit.service;

import static java.util.List.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dspace.content.edit.EditItemMode;
import org.dspace.content.edit.service.impl.EditItemModeValidatorImpl;
import org.junit.Test;

/**
 * Unit tests for {@link EditItemModeValidatorImpl}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class EditItemModeValidatorTest {

    private EditItemModeValidator validator = new EditItemModeValidatorImpl();

    @Test
    public void testWithValidConfiguration() {

        List<EditItemMode> publicationModes = of(editItemMode("FULL"), editItemMode("OWNER"), editItemMode("TEST"));
        List<EditItemMode> personModes = of(editItemMode("FULL"), editItemMode("TEST"));
        List<EditItemMode> orgUnitModes = of(editItemMode("OWNER"));

        Map<String, List<EditItemMode>> configuration = Map.of("publication", publicationModes,
            "person", personModes, "orgunit", orgUnitModes);

        // no exception is thrown
        validator.validate(configuration);
    }

    @Test
    public void testWithSingleDuplication() {

        List<EditItemMode> publicationModes = of(editItemMode("FULL"), editItemMode("OWNER"), editItemMode("FULL"));
        List<EditItemMode> personModes = of(editItemMode("FULL"), editItemMode("TEST"));
        List<EditItemMode> orgUnitModes = of(editItemMode("OWNER"));

        Map<String, List<EditItemMode>> configuration = new LinkedHashMap<>();
        configuration.put("publication", publicationModes);
        configuration.put("person", personModes);
        configuration.put("orgunit", orgUnitModes);

        Exception exception = assertThrows(IllegalStateException.class, () -> validator.validate(configuration));
        assertThat(exception.getMessage(), is("Invalid Edit item mode configuration: "
            + "[Configuration with key 'publication' has the following duplicated edit modes: [FULL]]"));
    }

    @Test
    public void testWithManyDuplicationsOnSameConfigurationKey() {

        List<EditItemMode> publicationModes = of(editItemMode("FULL"), editItemMode("OWNER"), editItemMode("FULL"),
            editItemMode("OWNER"), editItemMode("FULL"));
        List<EditItemMode> personModes = of(editItemMode("FULL"), editItemMode("TEST"));
        List<EditItemMode> orgUnitModes = of(editItemMode("OWNER"));

        Map<String, List<EditItemMode>> configuration = new LinkedHashMap<>();
        configuration.put("publication", publicationModes);
        configuration.put("person", personModes);
        configuration.put("orgunit", orgUnitModes);

        Exception exception = assertThrows(IllegalStateException.class, () -> validator.validate(configuration));
        assertThat(exception.getMessage(), is("Invalid Edit item mode configuration: "
            + "[Configuration with key 'publication' has the following duplicated edit modes: [OWNER, FULL]]"));
    }

    @Test
    public void testWithManyDuplications() {

        List<EditItemMode> publicationModes = of(editItemMode("FULL"), editItemMode("OWNER"), editItemMode("FULL"));
        List<EditItemMode> personModes = of(editItemMode("FULL"), editItemMode("TEST"));
        List<EditItemMode> orgUnitModes = of(editItemMode("OWNER"), editItemMode("TEST"), editItemMode("TEST"));

        Map<String, List<EditItemMode>> configuration = new LinkedHashMap<>();
        configuration.put("publication", publicationModes);
        configuration.put("person", personModes);
        configuration.put("orgunit", orgUnitModes);

        Exception exception = assertThrows(IllegalStateException.class, () -> validator.validate(configuration));
        assertThat(exception.getMessage(), is("Invalid Edit item mode configuration: "
            + "[Configuration with key 'publication' has the following duplicated edit modes: [FULL],"
            + " Configuration with key 'orgunit' has the following duplicated edit modes: [TEST]]"));
    }

    private EditItemMode editItemMode(String name) {
        EditItemMode mode = new EditItemMode();
        mode.setName(name);
        return mode;
    }

}
