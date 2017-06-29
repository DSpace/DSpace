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

    /**
     * Set the bitstream file access permission based on the provided file access string and start date.
     * @param context the current context
     * @param bitstream the bitstream the file access permission will apply to
     * @param fileAccess the file access string
     * @param startDate the start date String for the permission
     * @throws SQLException
     * @throws AuthorizeException
     */
    void setFileAccess(Context context, Bitstream bitstream, String fileAccess, String startDate) throws SQLException, AuthorizeException;

    /**
     * Set the bitstream file access permission based on the provided file access string and start date.
     * @param context the current context
     * @param bitstream the bitstream the file access permission will apply to
     * @param fileAccess the file access string
     * @param startDate the start date DCDate for the permission
     * @throws SQLException
     * @throws AuthorizeException
     */
    void setFileAccess(Context context, Bitstream bitstream, String fileAccess, DCDate startDate) throws SQLException, AuthorizeException;

    /**
     * retrieve the file access permission for the provided bitstream.
     * @param context the current context
     * @param bitstream the bitstream
     * @return The file access permission for the provided bitstream
     * @throws SQLException
     */
    ArticleAccess getFileAccess(Context context, Bitstream bitstream) throws SQLException;

    /**
     * retrieve the embargo date from the http request.
     * @param request
     * @return a DCDate object with the embargo date
     */
    DCDate getEmbargoDate(HttpServletRequest request);

    /**
     * check if the file access for the article is identical to the file access for the provided bitstream
     * @param context the current context
     * @param bitstream the bitstream
     * @return {@code true} if the file access for the article is identical to the file access for the provided bitstream
     * @throws SQLException
     */
    boolean fileAccessIdentical(Context context, Bitstream bitstream) throws SQLException;
}
