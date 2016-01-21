/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.authority.indexer;

import org.dspace.authority.AuthorityValue;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;

import java.sql.SQLException;

/**
 *
 * @author Antoine Snyers (antoine at atmire.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public interface AuthorityIndexerInterface {

    public void init(Context context, Item item);

    public void init(Context context, boolean useCache);

    public void init(Context context);

    public AuthorityValue nextValue();

    public boolean hasMore() throws SQLException, AuthorizeException;

    public void close();

    public boolean isConfiguredProperly();
}
