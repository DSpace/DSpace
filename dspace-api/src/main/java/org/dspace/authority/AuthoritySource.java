package org.dspace.authority;

import java.util.List;

/**
 * User: lantian @ atmire . com
 * Date: 9/8/14
 * Time: 3:32 PM
 */
public interface AuthoritySource {

    public List<AuthorityValue> queryAuthorities(String text, int max);

    public AuthorityValue queryAuthorityID(String id);
}
