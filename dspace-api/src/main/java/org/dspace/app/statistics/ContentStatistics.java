/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.statistics;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import org.dspace.core.Context;
import org.dspace.storage.rdbms.*;

/**
 * Expose some simple measures of the repository's size as an XML document via a
 * web service.
 *
 * <p><em>NOTE WELL:</em>  we go straight to the database for much of this
 * information.  This could break if there are significant changes in the
 * schema.  The object model doesn't provide these statistics, though.</p>
 *
 * @author Mark H. Wood
 */
public class ContentStatistics extends HttpServlet
{
	private static final TimeZone utcZone = TimeZone.getTimeZone("UTC");

	protected static final Logger log = Logger.getLogger(ContentStatistics.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException
    {
        log.debug("Entering ContentStatistics.doGet");
        Context dsContext = null;
        TableRow row;

        // Response header
        resp.setContentType("text/xml; encoding='UTF-8'");
        resp.setStatus(HttpServletResponse.SC_OK);

        // Response body
        PrintWriter responseWriter = resp.getWriter();
        responseWriter.print("<?xml version='1.0' encoding='UTF-8'?>");

        responseWriter.print("<dspace-repository-statistics date='");
        log.debug("Ready to write date");
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
        df.setTimeZone(utcZone);
        SimpleDateFormat tf = new SimpleDateFormat("HHmmss");
        tf.setTimeZone(utcZone);
        Date now = new Date();
        responseWriter.print(df.format(now));
        responseWriter.print('T');
        responseWriter.print(tf.format(now));
        responseWriter.print("Z'>");
        log.debug("Wrote the date");

        try
        {
            dsContext = new Context();

            row = DatabaseManager.querySingle(dsContext,
                    "SELECT count(community_id) AS communities FROM community;");
            if (null != row)
            {
                responseWriter.printf(
                        " <statistic name='communities'>%d</statistic>",
                        row.getLongColumn("communities"));
            }

            row = DatabaseManager.querySingle(dsContext,
                    "SELECT count(collection_id) AS collections FROM collection;");
            if (null != row)
            {
                responseWriter.printf(
                        " <statistic name='collections'>%d</statistic>",
                        row.getLongColumn("collections"));
            }

            row = DatabaseManager.querySingle(dsContext,
                    "SELECT count(item_id) AS items FROM item WHERE NOT withdrawn;");
            if (null != row)
            {
                responseWriter.printf(
                        " <statistic name='items'>%d</statistic>",
                        row.getLongColumn("items"));
            }

            log.debug("Counting, summing bitstreams");
            row = DatabaseManager.querySingle(dsContext,
                    "SELECT count(bitstream_id) AS bitstreams," +
                            " sum(size_bytes) AS totalBytes" +
                            " FROM bitstream" +
                                "  JOIN bundle2bitstream USING(bitstream_id)" +
                                "  JOIN bundle USING(bundle_id)" +
                                "  JOIN item2bundle USING(bundle_id)" +
                                "  JOIN item USING(item_id)" +
                                " WHERE NOT withdrawn" +
                                "  AND NOT deleted" +
                                "  AND bundle.name = 'ORIGINAL';");
            if (null != row)
            {
                log.debug("Writing count");
                responseWriter.printf(" <statistic name='bitstreams'>%d</statistic>",
                        row.getLongColumn("bitstreams"));
                log.debug("Writing total size");
                responseWriter.printf(" <statistic name='totalBytes'>%d</statistic>",
                        row.getLongColumn("totalBytes"));
                log.debug("Completed writing count, size");
            }

            log.debug("Counting, summing image bitstreams");
            row = DatabaseManager.querySingle(dsContext,
                    "SELECT count(bitstream_id) AS images," +
                    " sum(size_bytes) AS imageBytes" +
                    " FROM bitstream" +
                    " JOIN bitstreamformatregistry USING(bitstream_format_id)" +
                    " JOIN bundle2bitstream USING(bitstream_id)" +
                    " JOIN bundle USING(bundle_id)" +
                    " JOIN item2bundle USING(bundle_id)" +
                    " JOIN item USING(item_id)" +
                    " WHERE bundle.name = 'ORIGINAL'" +
                    "  AND mimetype LIKE 'image/%'" +
                    "  AND NOT deleted" +
                    "  AND NOT withdrawn;"
                    );
            if (null != row)
            {
                responseWriter.printf(" <statistic name='images'>%d</statistic>",
                        row.getLongColumn("images"));
                responseWriter.printf(" <statistic name='imageBytes'>%d</statistic>",
                        row.getLongColumn("imageBytes"));
            }

            dsContext.abort();	// nothing to commit
        }
        catch (SQLException e)
        {
            log.debug("caught SQLException");
            if (null != dsContext)
            {
                dsContext.abort();
            }
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    e.getMessage());
        }

        responseWriter.print("</dspace-repository-statistics>");
        log.debug("Finished report");
    }

    /** HttpServlet implements Serializable for some strange reason */
    private static final long serialVersionUID = -98582768658080267L;
}
