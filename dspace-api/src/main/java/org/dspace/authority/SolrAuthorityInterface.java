/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority;

import java.util.List;

/**
 * @author Jonas Van Goolen (jonas at atmire dot com)
 */
public interface SolrAuthorityInterface {

    List<AuthorityValue> queryAuthorities(String text, int max);

    AuthorityValue queryAuthorityID(String id);
}
