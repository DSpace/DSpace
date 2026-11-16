/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.enhancer.source;

import java.util.Objects;

/**
 * A read-only metadata value returned by a {@link MetadataSource}.
 *
 * <p>This type carries only the data needed by the relationship metadata generators: the value itself,
 * its language, its authority and the authority confidence. It deliberately has no persistence or
 * authorization fields, so it can be produced by any IO adapter (database or search index) without
 * fabricating state.</p>
 *
 * @param value      the metadata value, never {@code null}
 * @param language   the language of the value, may be {@code null}
 * @param authority  the authority key of the value, may be {@code null}
 * @param confidence the authority confidence, or a negative value when not applicable
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public record RawMetadataValue(String value, String language, String authority, int confidence) {

    /**
     * Create a raw metadata value, ensuring that the value itself is present.
     *
     * @param value      the metadata value, must not be {@code null}
     * @param language   the language of the value, may be {@code null}
     * @param authority  the authority key of the value, may be {@code null}
     * @param confidence the authority confidence, or a negative value when not applicable
     * @throws NullPointerException if {@code value} is {@code null}
     */
    public RawMetadataValue {
        Objects.requireNonNull(value, "A raw metadata value must have a non-null value");
    }

}
