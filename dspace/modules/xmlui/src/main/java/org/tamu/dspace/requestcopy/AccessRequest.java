package org.tamu.dspace.requestcopy;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Subscribe;
import org.dspace.harvest.HarvestedCollection;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;



public class AccessRequest {
    
    private Context context;
    private TableRow requestRow;
    /** log4j logger */
    private static Logger log = Logger.getLogger(AccessRequest.class);
    
    private static final String key="GXnS8YTi9kMm7t88vYYOrlJ9nwYeVEKAaJyC2FZNgvBZ9QL0BzIryn83n1UoytB";

    public static final int STATUS_ERROR = -1;
    public static final int STATUS_EXPIRED = 0;
    public static final int STATUS_SENT = 1;
    public static final int STATUS_DENIED = 2;
    public static final int STATUS_TEMPORARY_ACCESS = 3;
    public static final int STATUS_PERMANENT_ACCESS = 4;
    public static final int STATUS_OPEN_ACCESS = 5;

    /*
     * 	access_requests
     	request_id		| integer                  | not null
	requestor_id		| integer                  |
	author_id		| integer                  | 
	item_id			| integer                  |
	request_message		| varchar 
	request_status		| integer                  | 
	request_time		| timestamp with time zone |
	decision_time		| timestamp with time zone |
	
	access_requests2resourcepolicy
	request_id		| integer 
	policy_id		| integer
     */  
        
    
    private AccessRequest(Context c, TableRow tr) {
	this.context = c;
	this.requestRow = tr;	
	
	// Clean up expired polices while we are at it.
	this.cleanUp();
    }
    

    /**
     * Verify the existence of required tables and create them if necessary
     * @param c
     * @return
     * @throws SQLException
     */
    public static boolean tablesExist(Context c) throws SQLException 
    {
    	try {
	    DatabaseManager.queryTable(c, "access_request", "SELECT COUNT(*) FROM access_request");
	} catch (SQLException sqle) {
	    return false;
	}
	
	return true;
    }
    
    
    /**
     * Create the required table structure if necessary
     * @param c
     * @return
     * @throws SQLException
     */
    public static void createTables(Context c) throws SQLException 
    {
    	try {
	    DatabaseManager.updateQuery(c, "CREATE TABLE access_request" +
	    		"(" +
	    		"request_id INTEGER PRIMARY KEY, " +
	    		"requestor_id INTEGER REFERENCES eperson(eperson_id), " +
	    		"author_id INTEGER REFERENCES eperson(eperson_id), " +
	    		"item_id INTEGER REFERENCES item(item_id), " +
	    		"request_message VARCHAR, " +
	    		"email_hash VARCHAR, " +
	    		"request_status INTEGER, " +
	    		"request_time TIMESTAMP WITH TIME ZONE, " +
	    		"decision_time TIMESTAMP WITH TIME ZONE)");
	    DatabaseManager.updateQuery(c, "CREATE SEQUENCE access_request_seq");
	    c.commit();
	} 
    	catch (SQLException sqle) 
	{
	    sqle.getMessage();
	}
    } 
      
    
    
    public static AccessRequest create(Context c, Item item, String message, int status) throws SQLException 
    {
	Date rightNow = new Date();
	String seedValue = key + c.getCurrentUser().getID() + item.getSubmitter().getID() + item.getID() + status + rightNow;
	String hash = DigestUtils.shaHex(seedValue);
	
	TableRow tr = DatabaseManager.row("access_request");
    	tr.setColumn("requestor_id", c.getCurrentUser().getID());
    	tr.setColumn("author_id", item.getSubmitter().getID());
    	tr.setColumn("item_id", item.getID());
    	tr.setColumn("request_message", message);
    	tr.setColumn("email_hash", hash);
    	tr.setColumn("request_status", status);
    	tr.setColumn("request_time", rightNow);
    	//tr.setColumn("decision_time", "");
    	DatabaseManager.insert(c, tr);
    	
	return new AccessRequest(c, tr);
    }
    
    
    
    public static AccessRequest findById(Context c, int requestId) throws SQLException
    {
	TableRow tr = DatabaseManager.find(c, "access_request", requestId);
	
	if (tr == null)
	    return null;
	else
	    return new AccessRequest(c, tr);
    }
    
    public static AccessRequest findByRequestorAndItem(Context c, int requestorId, int itemId) throws SQLException 
    {
	TableRow tr = DatabaseManager.querySingle(c, "SELECT * FROM access_request WHERE requestor_id=? AND item_id=?", requestorId, itemId);
	
	if (tr == null)
	    return null;
	else
	    return new AccessRequest(c, tr);
    }
    
        

    // Getters
    public int getRequestId() {
    	return requestRow.getIntColumn("request_id");
    }
    
    public int getRequestorId() {
    	return requestRow.getIntColumn("requestor_id");
    }
    
    public int getAuthorId() {
    	return requestRow.getIntColumn("author_id");
    }
    
    public int getItemId() {
    	return requestRow.getIntColumn("item_id");
    }
    
    public String getRequestMessage () {
	return requestRow.getStringColumn("request_message");
    }
    
    public String getEmailHash () {
	return requestRow.getStringColumn("email_hash");
    }
    
    public int getStatus() {
    	return requestRow.getIntColumn("request_status");
    }
    
    public Date getRequestDate() {
	return requestRow.getDateColumn("request_time");
    }
    
    public Date getDecisionDate() {
	return requestRow.getDateColumn("decision_time");
    }
    
    
    // Setters
    public void setRequestorId(int rid) {
    	requestRow.setColumn("requestor_id", rid);
    }
    
    public void setAuthorId(int aid) {
    	requestRow.setColumn("author_id", aid);
    }
    
    public void setItemId(int rid) {
    	requestRow.setColumn("item_id", rid);
    }
    
    public void setRequestMessage(String message) {
    	if (message == null) {
    	    requestRow.setColumnNull("request_message");
    	} else {
    	    requestRow.setColumn("request_message", message);
    	}
    }
    
    public void setStatus(int status) {
    	requestRow.setColumn("request_status", status);
    }
    
    public void makeDecision(int status) {
    	requestRow.setColumn("request_status", status);
    	requestRow.setColumn("decision_time", new Date());
    }
    
    public void setRequestDate(Date date) {
    	if (date == null) {
    	    requestRow.setColumnNull("request_time");
    	} else {
    	    requestRow.setColumn("request_time", date);
    	}
    }
    
    public void setDecisionDate(Date date) {
    	if (date == null) {
    	    requestRow.setColumnNull("decision_time");
    	} else {
    	    requestRow.setColumn("decision_time", date);
    	}
    }
    
    
    
    public void update() throws SQLException, IOException, AuthorizeException
    {
        DatabaseManager.update(context, requestRow);

    }
    
    
    /**
     * Return a human-readable interpretation of the request 
     * @param status
     * @return
     */
    public static String statusToString(int status)
    {
	switch (status) {
	    case STATUS_EXPIRED: return "This request has expired.";
	    case STATUS_SENT: return "This request for access was sent to the submitter.";
	    case STATUS_DENIED: return "This access request was denied.";
	    case STATUS_TEMPORARY_ACCESS: return "The requestor has been granted temporary access to the item's contents.";
	    case STATUS_PERMANENT_ACCESS: return "The requestor has been granted permanent access to the item's contents.";
	    case STATUS_OPEN_ACCESS: return "This item has been switched to an open-access model.";
	    case STATUS_ERROR: default: return "An error occurred while the access request was being processed.";
	}
    }
        
    /**
     * Clean up expired policies and requests
     */
    // FIXME: Uses Postgre-specific syntax
    private void cleanUp() 
    {
	try 
	{
	    context.turnOffAuthorisationSystem();

	    // 1. Drop expired requests (7 days past the request submit)
	    int accessRequests = DatabaseManager.updateQuery(this.context, "DELETE FROM access_request WHERE request_time < clock_timestamp() - interval '7 days'");
	    if (accessRequests > 0)
		log.info("Removed " + accessRequests + " expired access requests from the 'access_request' table.");

	    // 2. Remove all expired policies
	    int policies = DatabaseManager.updateQuery(this.context, "DELETE FROM resourcepolicy WHERE end_date <= current_date");
	    if (policies > 0)
		log.info("Removed " + policies + " expired access policies from the 'resourcepolicy' table.");
	    
	    if (accessRequests == 0 && policies == 0)
		log.info("Access request database clean up complete. No expired entries.");
	}
	catch (SQLException e) 
	{
	    log.error("Failure while removing expired access requests.", e);
	}
	finally {
	    context.restoreAuthSystemState();
	}
    }

}
