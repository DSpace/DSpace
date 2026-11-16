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
 * <p>It carries the value, its language, its authority and the authority confidence, and no
 * persistence or authorization data.</p>
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
