/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.rest.common;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * JAX-RS holder for a Long value.
 *
 * @author mwood
 */
@XmlRootElement(name = "long")
public class Long
{
    private final long value;

    public Long() { value = 0; }

    public Long(long longValue) { value = longValue; }

    @XmlAttribute(name = "value", required=true)
    public long getValue() { return value; }
}
