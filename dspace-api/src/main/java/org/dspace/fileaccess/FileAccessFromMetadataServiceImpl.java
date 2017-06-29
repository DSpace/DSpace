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
 * This stub can be used to replace the Elsevier specific ElsevierFileAccessFromMetadataService.
 * comment out the ElsevierFileAccessFromMetadataService bean in config/spring/api/core-services.xml to disable it
 * uncomment the FileAccessFromMetadataServiceImpl bean in the same spring file to enable it.
 *
 * @author Created by Philip Vissenaekens (philip at atmire dot com)
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
