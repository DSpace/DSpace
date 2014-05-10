/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.requestitem;

import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;

import java.sql.SQLException;
import java.util.Date;

/**
 * Object representing an Item Request
 */
public class RequestItem {
    private static Logger log = Logger.getLogger(RequestItem.class);

    private int bitstreamId, itemID;
    private String reqEmail;
    private String reqName;
    private String reqMessage;
    private String token;
    private boolean allfiles;
    private Date decision_date;
    private boolean accept_request;

    public RequestItem(int itemID, int bitstreamId, String reqEmail, String reqName, String reqMessage, boolean allfiles){
        this.itemID = itemID;
        this.bitstreamId = bitstreamId;
        this.reqEmail = reqEmail;
        this.reqName = reqName;
        this.reqMessage = reqMessage;
        this.allfiles = allfiles;
    }

    private RequestItem(TableRow record) {
        this.itemID = record.getIntColumn("item_id");
        this.bitstreamId = record.getIntColumn("bitstream_id");
        this.token = record.getStringColumn("token");
        this.reqEmail = record.getStringColumn("request_email");
        this.reqName = record.getStringColumn("request_name");
        this.reqMessage = record.getStringColumn("request_message");
        this.allfiles = record.getBooleanColumn("allfiles");
        this.decision_date = record.getDateColumn("decision_date");
        this.accept_request = record.getBooleanColumn("accept_request");
    }

    public static RequestItem findByToken(Context context, String token) {
        try {
            TableRow requestItem = DatabaseManager.findByUnique(context, "requestitem", "token", token);
            return new RequestItem(requestItem);
        } catch (SQLException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    /**
     * Save updates to the record. Only accept_request, and decision_date are set-able.
     * @param context
     */
    public void update(Context context) {
        try {
            TableRow record = DatabaseManager.findByUnique(context, "requestitem", "token", token);

            record.setColumn("accept_request", accept_request);
            record.setColumn("decision_date", decision_date);

            DatabaseManager.update(context, record);

        } catch (SQLException e) {
            log.error(e.getMessage());
        }

    }

    /**
     * Generate a unique id of the request and put it into the DB
     * @param context
     * @return
     * @throws java.sql.SQLException
     */
    public String getNewToken(Context context) throws SQLException
    {
        TableRow record = DatabaseManager.create(context, "requestitem");
        record.setColumn("token", Utils.generateHexKey());
        record.setColumn("bitstream_id", bitstreamId);
        record.setColumn("item_id", itemID);
        record.setColumn("allfiles", allfiles);
        record.setColumn("request_email", reqEmail);
        record.setColumn("request_name", reqName);
        record.setColumn("request_message", reqMessage);
        record.setColumnNull("accept_request");
        record.setColumn("request_date", new Date());
        record.setColumnNull("decision_date");
        record.setColumnNull("expires");

        DatabaseManager.update(context, record);

        if (log.isDebugEnabled())
        {
            log.debug("Created requestitem_token " + record.getIntColumn("requestitem_id")
                    + " with token " + record.getStringColumn("token") +  "\"");
        }
        return record.getStringColumn("token");

    }

    public boolean isAllfiles() {
        return allfiles;
    }

    public String getReqMessage() {
        return reqMessage;
    }

    public String getReqName() {
        return reqName;
    }

    public String getReqEmail() {
        return reqEmail;
    }

    public String getToken() {
        return token;
    }

    public int getItemID() {
        return itemID;
    }

    public int getBitstreamId() {
        return bitstreamId;
    }

    public Date getDecision_date() {
        return decision_date;
    }

    public void setDecision_date(Date decision_date) {
        this.decision_date = decision_date;
    }

    public boolean isAccept_request() {
        return accept_request;
    }

    public void setAccept_request(boolean accept_request) {
        this.accept_request = accept_request;
    }
}
