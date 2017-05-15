/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.fileaccess;

import java.sql.*;
import javax.servlet.http.*;
import org.dspace.authorize.*;
import org.dspace.content.*;
import org.dspace.core.*;
import org.dspace.fileaccess.service.*;
import org.dspace.importer.external.elsevier.entitlement.*;

/**
 * Created by Philip Vissenaekens (philip at atmire dot com)
 * Date: 12/11/15
 * Time: 13:49
 */
public class FileAccessFromMetadataServiceImpl implements FileAccessFromMetadataService {
    @Override
    public void setFileAccess(Context context, Bitstream bitstream, String fileAccess, String startDate) throws SQLException, AuthorizeException {

    }

    @Override
    public void setFileAccess(Context context, Bitstream bitstream, String fileAccess, DCDate startDate) throws SQLException, AuthorizeException {

    }

    @Override
    public ArticleAccess getFileAccess(Context context, Bitstream bitstream) throws SQLException {
        return null;
    }

    @Override
    public DCDate getEmbargoDate(HttpServletRequest request) {
        return null;
    }

    @Override
    public boolean fileAccessIdentical(Context context, Bitstream bitstream) throws SQLException {
        return false;
    }
}
