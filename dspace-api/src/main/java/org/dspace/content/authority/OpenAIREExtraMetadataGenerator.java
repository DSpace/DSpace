/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.util.Map;

/**
 * 
 * @author Mykhaylo Boychuk (4science.it)
 */
public interface OpenAIREExtraMetadataGenerator {

    Map<String, String> build(String value);

}