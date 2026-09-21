/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.service.impl;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;

import java.util.List;

import org.dspace.layout.DynamicLayoutSection;
import org.junit.Test;

/**
 * Unit tests for {@link DynamicLayoutSectionServiceImpl}, focused on the
 * visibility filtering applied to the top-bar sections (including nested
 * sections).
 *
 * @author DSpace
 */
public class DynamicLayoutSectionServiceImplTest {

    private DynamicLayoutSectionServiceImpl buildService(List<DynamicLayoutSection> sections) {
        DynamicLayoutSectionServiceImpl service = new DynamicLayoutSectionServiceImpl();
        service.setComponents(sections);
        return service;
    }

    @Test
    public void flatVisibleSectionsAreReturnedAsIs() {
        DynamicLayoutSection visible = new DynamicLayoutSection("visible", true, emptyList());
        DynamicLayoutSection hidden = new DynamicLayoutSection("hidden", false, emptyList());

        DynamicLayoutSectionServiceImpl service = buildService(List.of(visible, hidden));

        List<DynamicLayoutSection> result = service.findAllVisibleSectionsInTopBar();

        assertThat(result, hasSize(1));
        assertThat(result.get(0).getId(), is("visible"));
    }

    @Test
    public void hiddenParentIsExcludedEntirely() {
        DynamicLayoutSection visibleChild = new DynamicLayoutSection("visibleChild", true, emptyList());
        DynamicLayoutSection hiddenParent = new DynamicLayoutSection("hiddenParent", false);
        hiddenParent.setNestedSections(List.of(visibleChild));

        DynamicLayoutSectionServiceImpl service = buildService(List.of(hiddenParent));

        assertThat(service.findAllVisibleSectionsInTopBar(), hasSize(0));
    }

    @Test
    public void hiddenNestedSectionsAreFilteredOutOfVisibleParent() {
        DynamicLayoutSection visibleChild = new DynamicLayoutSection("visibleChild", true, emptyList());
        DynamicLayoutSection hiddenChild = new DynamicLayoutSection("hiddenChild", false, emptyList());
        DynamicLayoutSection visibleParent = new DynamicLayoutSection("visibleParent", true);
        visibleParent.setNestedSections(List.of(visibleChild, hiddenChild));

        DynamicLayoutSectionServiceImpl service = buildService(List.of(visibleParent));

        List<DynamicLayoutSection> result = service.findAllVisibleSectionsInTopBar();

        assertThat(result, hasSize(1));
        DynamicLayoutSection parent = result.get(0);
        assertThat(parent.getId(), is("visibleParent"));
        assertThat(parent.getNestedSections(), contains(
            org.hamcrest.Matchers.hasProperty("id", is("visibleChild"))));
    }

    @Test
    public void nestedVisibilityIsFilteredRecursively() {
        DynamicLayoutSection visibleLeaf = new DynamicLayoutSection("visibleLeaf", true, emptyList());
        DynamicLayoutSection hiddenLeaf = new DynamicLayoutSection("hiddenLeaf", false, emptyList());

        DynamicLayoutSection intermediate = new DynamicLayoutSection("intermediate", true);
        intermediate.setNestedSections(List.of(visibleLeaf, hiddenLeaf));

        DynamicLayoutSection root = new DynamicLayoutSection("root", true);
        root.setNestedSections(List.of(intermediate));

        DynamicLayoutSectionServiceImpl service = buildService(List.of(root));

        List<DynamicLayoutSection> result = service.findAllVisibleSectionsInTopBar();

        assertThat(result, hasSize(1));
        DynamicLayoutSection rootResult = result.get(0);
        assertThat(rootResult.getNestedSections(), hasSize(1));
        DynamicLayoutSection intermediateResult = rootResult.getNestedSections().get(0);
        assertThat(intermediateResult.getId(), is("intermediate"));
        assertThat(intermediateResult.getNestedSections(), contains(
            org.hamcrest.Matchers.hasProperty("id", is("visibleLeaf"))));
    }

    @Test
    public void countVisibleSectionsInTopBarReflectsTopLevelVisibleSections() {
        DynamicLayoutSection visibleOne = new DynamicLayoutSection("visibleOne", true, emptyList());
        DynamicLayoutSection hidden = new DynamicLayoutSection("hidden", false, emptyList());
        DynamicLayoutSection visibleTwo = new DynamicLayoutSection("visibleTwo", true, emptyList());

        DynamicLayoutSectionServiceImpl service = buildService(List.of(visibleOne, hidden, visibleTwo));

        assertThat(service.countVisibleSectionsInTopBar(), is(2));
    }

}
