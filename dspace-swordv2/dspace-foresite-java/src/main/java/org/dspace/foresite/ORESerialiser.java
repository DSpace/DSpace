/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.foresite;

import java.util.Properties;

/**
 * @Author Richard Jones
 */
public interface ORESerialiser
{
    ResourceMapDocument serialise(ResourceMap rem) throws ORESerialiserException;

	ResourceMapDocument serialiseRaw(ResourceMap rem) throws ORESerialiserException;

	void configure(Properties properties);
}
