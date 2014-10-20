/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.objectmanager;

import org.dspace.content.AuthorProfile;
import org.dspace.core.Context;

import java.sql.SQLException;

/**
 *
 * @author Roeland Dillen (roeland at atmire dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public interface AuthorProfileMetadataFilter {

    public boolean includeMetadatum(Context context, AuthorProfile ap, String element, String qualifier) throws SQLException;
}
