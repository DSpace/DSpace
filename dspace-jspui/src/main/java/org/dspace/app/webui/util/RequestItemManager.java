/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
/*
 * RequestItemManager.java
 *
 * Created on 27 de Marco de 2006, 17:12 by Arnaldo Dantas
 *
 */

package org.dspace.app.webui.util;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.app.webui.servlet.RequestItemServlet;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;

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
        String base = ConfigurationManager.getProperty("dspace.url");

        String specialLink = (new StringBuffer()).append(base).append(
                base.endsWith("/") ? "" : "/").append(
                "request-item").append("?step=" + RequestItemServlet.ENTER_TOKEN)
                .append("&token=")
                .append(getNewToken(context, Integer.parseInt(bitstreamId), itemID, reqEmail, reqName, allfiles))
                .toString();
        
        return specialLink;
    }
    
     public static boolean isRestricted(Context context, DSpaceObject o) throws SQLException
    {
		List<ResourcePolicy> policies = AuthorizeManager
				.getPoliciesActionFilter(context, o, Constants.READ);
		for (ResourcePolicy rp : policies)
		{
			if (rp.isDateValid())
			{
				return false;
			}
		}
        return true;
    }
    
    
}
