/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.foresite;

import java.io.InputStream;
import java.util.Properties;
import java.net.URI;

/**
 * @Author Richard Jones
 */
public interface OREParser
{
    ResourceMap parse(InputStream is) throws OREParserException;

	ResourceMap parse(InputStream is, URI uri) throws OREParserException;

	void configure(Properties properties);
}
