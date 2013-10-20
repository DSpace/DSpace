/*
 * RequestItemManager.java
 *
 * Created on 27 de Marco de 2006, 17:12 by Arnaldo Dantas
 *
 */

package pt.uminho.sdum.dspace.requestItem.util;

import java.io.IOException;
import java.sql.SQLException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.util.Date;
import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import pt.uminho.sdum.dspace.requestItem.servlet.RequestItemServlet;

/**
 *
 * @author Arnaldo Dantas
 */
public class RequestItemManager {
    
    /** log4j log */
    private static Logger log = Logger.getLogger(RequestItemManager.class);
    
    /* tablerow of requestitem table*/
    TableRow requestitem;
    
    /** Creates a new instance of RequestItemManager */
    public RequestItemManager(){}

    /**
     * Return the e-mail address referred to by a token, or null if email
     * address can't be found ignores expiration of token
     * 
     * @param context
     *            DSpace context
     * @param token
     *            Account token
     * @return The email address corresponding to token, or null.
     */
    public static TableRow getRequestbyToken(Context context, String token)
            throws SQLException
    {
        TableRow rd = DatabaseManager.findByUnique(context, "requestitem",
                "token", token);

        if (rd == null)
        {
            return null;
        }

        /*
         * ignore the expiration date on tokens Date expires =
         * rd.getDateColumn("expires"); if (expires != null) { if ((new
         * java.util.Date()).after(expires)) return null; }
         */
        return rd;
    }
    
    /*
     *
     */
    protected static String getNewToken(Context context, int bitstreamId
            , int itemID, String reqEmail, String reqName, boolean allfiles) throws SQLException
    {
        TableRow rd = DatabaseManager.create(context, "requestitem");
        rd.setColumn("token", Utils.generateHexKey());
        rd.setColumn("bitstream_id", bitstreamId);
        rd.setColumn("item_id",itemID);
        rd.setColumn("allfiles", allfiles);
        rd.setColumn("request_email", reqEmail);
        rd.setColumn("request_name", reqName);
        rd.setColumnNull("accept_request");
        rd.setColumn("request_date", new Date());
        rd.setColumnNull("decision_date");
        rd.setColumnNull("expires");
        // don't set expiration date any more
        //rd.setColumn("expires", getDefaultExpirationDate());
        DatabaseManager.update(context, rd);

        // This is a potential problem -- if we create the callback
        // and then crash, registration will get SNAFU-ed.
        // So FIRST leave some breadcrumbs
        if (log.isDebugEnabled())
        {
            log.debug("Created requestitem_token "
                    + rd.getIntColumn("requestitem_id")
                    + " with token " + rd.getStringColumn("token")
                    +  "\"");
        }
        return rd.getStringColumn("token");
         
    }

    /**
     * Get the link to the author in RequestLink email.
     * 
     * @param email
     *            The email address to mail to
     *
     * @exception SQLExeption
     *
     */
    public static String getLinkTokenEmail(Context context, String bitstreamId
            , int itemID, String reqEmail, String reqName, boolean allfiles)
            throws SQLException
    {
        String base = ConfigurationManager.getProperty("dspace.baseUrl");

        String specialLink = (new StringBuffer()).append(base).append(
                base.endsWith("/") ? "" : "/").append(
                "request-item").append("?step=" + RequestItemServlet.ENTER_TOKEN)
                .append("&token=")
                .append(getNewToken(context, Integer.parseInt(bitstreamId), itemID, reqEmail, reqName, allfiles))
                .toString();
        
        return specialLink;
    }
    
     /**
     * Get email by template.
     * 
     * @param template
     *            The template email 
     *
     * @exception SQLExeption
     *
     */
    public static ReqEmail getEmail(String template) throws IOException
    {
        String subject = "";
        StringBuffer contentBuffer = new StringBuffer();

        // Read in template
        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader(new FileReader(
                    ConfigurationManager.getProperty("dspace.dir") +
                    File.separator + "config" + File.separator + "emails"
                            + File.separator + template));

            boolean more = true;
            while (more)
            {
                String line = reader.readLine();

                if (line == null)
                {
                    more = false;
                } else if (line.toLowerCase().startsWith("subject:"))
                {
                    // Extract the first subject line - everything to the right
                    // of the colon, trimmed of whitespace
                    subject = line.substring(8).trim();
                } else if (!line.startsWith("#"))
                {
                    // Add non-comment lines to the content
                    contentBuffer.append(line);
                    contentBuffer.append("\n");
                }
            }
        }
        finally
        {
            if (reader != null)
            {
                reader.close();
            }
        }
        return getEmail(subject, contentBuffer.toString());
    }

     /**
     * Get email.
     * 
     * @param subject
     *            The subject of email 
     * @param contentText
     *            The text of email 
     *
     * @exception SQLExeption
     *
     */
    public static ReqEmail getEmail(String subject, String contentText) throws IOException
    {
        ReqEmail email = new ReqEmail();
        email.setField(email.FIELD_MESSAGE, contentText);
        email.setField(email.FIELD_SUBJECT, subject);
        return email;
    }
    
    public static boolean isRestricted(Context context, DSpaceObject o) throws SQLException
    {
        TableRowIterator tri = DatabaseManager.query(context,
        "SELECT * FROM resourcepolicy WHERE " + "resource_type_id="
                + o.getType() + " AND " + "resource_id=" + o.getID()
                + " AND " + "action_id=" + Constants.READ
                + " AND epersongroup_id = '0';");
        if(tri.hasNext())
            return false;
        return true;
    }
    
    
}
