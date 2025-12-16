/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.util.Map;

import org.dspace.app.sherpa.v2.SHERPAJournal;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public interface SherpaExtraMetadataGenerator {

    public Map<String, String> build(SHERPAJournal journal);

}