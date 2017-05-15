/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.fileaccess.service;

import java.sql.*;
import javax.servlet.http.*;
import org.dspace.authorize.*;
import org.dspace.content.*;
import org.dspace.core.*;
import org.dspace.importer.external.elsevier.entitlement.*;

/**
 * Created by Philip Vissenaekens (philip at atmire dot com)
 * Date: 26/10/15
 * Time: 11:55
 */
public interface FileAccessFromMetadataService {

    void setFileAccess(Context context, Bitstream bitstream, String fileAccess, String startDate) throws SQLException, AuthorizeException;

    void setFileAccess(Context context, Bitstream bitstream, String fileAccess, DCDate startDate) throws SQLException, AuthorizeException;

        ArticleAccess getFileAccess(Context context, Bitstream bitstream) throws SQLException;

    DCDate getEmbargoDate(HttpServletRequest request);

    boolean fileAccessIdentical(Context context, Bitstream bitstream) throws SQLException;
}
