package org.dspace.authority;

import java.util.List;

/**
 * Created by jonas - jonas@atmire.com on 13/04/2018.
 */
public interface SolrAuthorityInterface {

    List<AuthorityValue> queryAuthorities(String text, int max);

    AuthorityValue queryAuthorityID(String id);
}
