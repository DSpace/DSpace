/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.matcher;

import java.util.List;

import org.dspace.content.DSpaceObject;
import org.dspace.content.MetadataValue;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Implementation of {@link Matcher} for {@link DSpaceObject}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class DSpaceObjectMatcher extends TypeSafeMatcher<DSpaceObject> {

    private final Matcher<? super List<MetadataValue>> metadataValuesMatcher;

    private DSpaceObjectMatcher(Matcher<? super List<MetadataValue>> metadataValuesMatcher) {
        this.metadataValuesMatcher = metadataValuesMatcher;
    }

    public static DSpaceObjectMatcher withMetadata(Matcher<? super List<MetadataValue>> metadataValuesMatcher) {
        return new DSpaceObjectMatcher(metadataValuesMatcher);
    }

    @Override
    public void describeTo(Description description) {
        metadataValuesMatcher.describeTo(description);
    }

    @Override
    protected boolean matchesSafely(DSpaceObject dso) {
        return metadataValuesMatcher.matches(dso.getMetadata());
    }
}
