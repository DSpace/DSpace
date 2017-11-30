/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.codec.DecoderException;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.core.Utils;
import org.dspace.event.Event;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * Class representing an e-person.
 * 
 * @author David Stuve
 * @version $Revision$
 */
public class EPerson extends DSpaceObject
{
    /** The e-mail field (for sorting) */
    public static final int EMAIL = 1;

    /** The last name (for sorting) */
    public static final int LASTNAME = 2;

    /** The e-mail field (for sorting) */
    public static final int ID = 3;

    /** The netid field (for sorting) */
    public static final int NETID = 4;

    /** The e-mail field (for sorting) */
    public static final int LANGUAGE = 5;
    
    /** log4j logger */
    private static Logger log = Logger.getLogger(EPerson.class);

    /** Our context */
    private Context myContext;

    /** The row in the table representing this eperson */
    private TableRow myRow;

    /** Flag set when data is modified, for events */
    private boolean modified;

    /** Flag set when metadata is modified, for events */
    private boolean modifiedMetadata;

    /**
     * Construct an EPerson
     * 
     * @param context
     *            the context this object exists in
     * @param row
     *            the corresponding row in the table
     */
    EPerson(Context context, TableRow row)
    {
        myContext = context;
        myRow = row;

        // Cache ourselves
        context.cache(this, row.getIntColumn("eperson_id"));
        modified = false;
        modifiedMetadata = false;
        clearDetails();
    }

    /**
     * Return true if this object equals obj, false otherwise.
     * 
     * @param obj
     * @return true if ResourcePolicy objects are equal
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final EPerson other = (EPerson) obj;
        if (this.getID() != other.getID())
        {
            return false;
        }
        if (!this.getEmail().equals(other.getEmail()))
        {
            return false;
        }
        if (!this.getFullName().equals(other.getFullName()))
        {
            return false;
        }
        return true;
    }

    /**
     * Return a hash code for this object.
     *
     * @return int hash of object
     */
    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 89 * hash + this.getID();
        hash = 89 * hash + (this.getEmail() != null? this.getEmail().hashCode():0);
        hash = 89 * hash + (this.getFullName() != null? this.getFullName().hashCode():0);
        return hash;
    }



    /**
     * Get an EPerson from the database.
     * 
     * @param context
     *            DSpace context object
     * @param id
     *            ID of the EPerson
     * 
     * @return the EPerson format, or null if the ID is invalid.
     */
    public static EPerson find(Context context, int id) throws SQLException
    {
        // First check the cache
        EPerson fromCache = (EPerson) context.fromCache(EPerson.class, id);

        if (fromCache != null)
        {
            return fromCache;
        }

        TableRow row = DatabaseManager.find(context, "eperson", id);

        if (row == null)
        {
            return null;
        }
        else
        {
            return new EPerson(context, row);
        }
    }

    /**
     * Find the eperson by their email address.
     * 
     * @return EPerson, or {@code null} if none such exists.
     */
    public static EPerson findByEmail(Context context, String email)
            throws SQLException, AuthorizeException
    {
        if (email == null)
        {
            return null;
        }
        
        // All email addresses are stored as lowercase, so ensure that the email address is lowercased for the lookup 
        TableRow row = DatabaseManager.findByUnique(context, "eperson",
                "email", email.toLowerCase());

        if (row == null)
        {
            return null;
        }
        else
        {
            // First check the cache
            EPerson fromCache = (EPerson) context.fromCache(EPerson.class, row
                    .getIntColumn("eperson_id"));

            if (fromCache != null)
            {
                return fromCache;
            }
            else
            {
                return new EPerson(context, row);
            }
        }
    }

    /**
     * Find the eperson by their netid.
     * 
     * @param context
     *            DSpace context
     * @param netid
     *            Network ID
     * 
     * @return corresponding EPerson, or <code>null</code>
     */
    public static EPerson findByNetid(Context context, String netid)
            throws SQLException
    {
        if (netid == null)
        {
            return null;
        }

        TableRow row = DatabaseManager.findByUnique(context, "eperson", "netid", netid);

        if (row == null)
        {
            return null;
        }
        else
        {
            // First check the cache
            EPerson fromCache = (EPerson) context.fromCache(EPerson.class, row
                    .getIntColumn("eperson_id"));

            if (fromCache != null)
            {
                return fromCache;
            }
            else
            {
                return new EPerson(context, row);
            }
        }
    }

    /**
     * Find the epeople that match the search query across firstname, lastname or email.
     * 
     * @param context
     *            DSpace context
     * @param query
     *            The search string
     * 
     * @return array of EPerson objects
     */
    public static EPerson[] search(Context context, String query)
            throws SQLException
    {
        return search(context, query, -1, -1);
    }
    
    /**
     * Find the epeople that match the search query across firstname, lastname or email. 
     * This method also allows offsets and limits for pagination purposes. 
     * 
     * @param context
     *            DSpace context
     * @param query
     *            The search string
     * @param offset
     *            Inclusive offset 
     * @param limit
     *            Maximum number of matches returned
     * 
     * @return array of EPerson objects
     */
    public static EPerson[] search(Context context, String query, int offset, int limit) 
    		throws SQLException
	{
		String params = "%"+query.toLowerCase()+"%";
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append("SELECT * FROM eperson WHERE eperson_id = ? OR ");
        queryBuf.append("LOWER(firstname) LIKE LOWER(?) OR LOWER(lastname) LIKE LOWER(?) OR LOWER(email) LIKE LOWER(?) ORDER BY lastname, firstname ASC ");

        // Add offset and limit restrictions - Oracle requires special code
        if ("oracle".equals(ConfigurationManager.getProperty("db.name")))
        {
            // First prepare the query to generate row numbers
            if (limit > 0 || offset > 0)
            {
                queryBuf.insert(0, "SELECT /*+ FIRST_ROWS(n) */ rec.*, ROWNUM rnum  FROM (");
                queryBuf.append(") ");
            }

            // Restrict the number of rows returned based on the limit
            if (limit > 0)
            {
                queryBuf.append("rec WHERE rownum<=? ");
                // If we also have an offset, then convert the limit into the maximum row number
                if (offset > 0)
                {
                    limit += offset;
                }
            }

            // Return only the records after the specified offset (row number)
            if (offset > 0)
            {
                queryBuf.insert(0, "SELECT * FROM (");
                queryBuf.append(") WHERE rnum>?");
            }
        }
        else
        {
            if (limit > 0)
            {
                queryBuf.append(" LIMIT ? ");
            }

            if (offset > 0)
            {
                queryBuf.append(" OFFSET ? ");
            }
        }

        String dbquery = queryBuf.toString();

        // When checking against the eperson-id, make sure the query can be made into a number
		Integer int_param;
		try {
			int_param = Integer.valueOf(query);
		}
		catch (NumberFormatException e) {
			int_param = Integer.valueOf(-1);
		}

        // Create the parameter array, including limit and offset if part of the query
        Object[] paramArr = new Object[] {int_param,params,params,params};
        if (limit > 0 && offset > 0)
        {
            paramArr = new Object[]{int_param, params, params, params, limit, offset};
        }
        else if (limit > 0)
        {
            paramArr = new Object[]{int_param, params, params, params, limit};
        }
        else if (offset > 0)
        {
            paramArr = new Object[]{int_param, params, params, params, offset};
        }

        // Get all the epeople that match the query
		TableRowIterator rows = DatabaseManager.query(context, 
		        dbquery, paramArr);
		try
        {
            List<TableRow> epeopleRows = rows.toList();
            EPerson[] epeople = new EPerson[epeopleRows.size()];

            for (int i = 0; i < epeopleRows.size(); i++)
            {
                TableRow row = (TableRow) epeopleRows.get(i);

                // First check the cache
                EPerson fromCache = (EPerson) context.fromCache(EPerson.class, row
                        .getIntColumn("eperson_id"));

                if (fromCache != null)
                {
                    epeople[i] = fromCache;
                }
                else
                {
                    epeople[i] = new EPerson(context, row);
                }
            }

            return epeople;
        }
        finally
        {
            if (rows != null)
            {
                rows.close();
            }
        }
    }

    /**
     * Returns the total number of epeople returned by a specific query, without the overhead 
     * of creating the EPerson objects to store the results.
     * 
     * @param context
     *            DSpace context
     * @param query
     *            The search string
     * 
     * @return the number of epeople matching the query
     */
    public static int searchResultCount(Context context, String query)
    	throws SQLException
	{
		String dbquery = "%"+query.toLowerCase()+"%";
		Long count;
		
		// When checking against the eperson-id, make sure the query can be made into a number
		Integer int_param;
		try {
			int_param = Integer.valueOf(query);
		}
		catch (NumberFormatException e) {
			int_param = Integer.valueOf(-1);
		}
		
		// Get all the epeople that match the query
		TableRow row = DatabaseManager.querySingle(context,
		        "SELECT count(*) as epcount FROM eperson WHERE eperson_id = ? OR " +
		        "LOWER(firstname) LIKE LOWER(?) OR LOWER(lastname) LIKE LOWER(?) OR LOWER(email) LIKE LOWER(?)",
		        new Object[] {int_param,dbquery,dbquery,dbquery});
				
		// use getIntColumn for Oracle count data
        if ("oracle".equals(ConfigurationManager.getProperty("db.name")))
        {
            count = Long.valueOf(row.getIntColumn("epcount"));
        }
        else  //getLongColumn works for postgres
        {
            count = Long.valueOf(row.getLongColumn("epcount"));
        }
        
		return count.intValue();
	}
    
    
    
    /**
     * Find all the epeople that match a particular query
     * <ul>
     * <li><code>ID</code></li>
     * <li><code>LASTNAME</code></li>
     * <li><code>EMAIL</code></li>
     * <li><code>NETID</code></li>
     * </ul>
     * 
     * @return array of EPerson objects
     */
    public static EPerson[] findAll(Context context, int sortField)
            throws SQLException
    {
        String s;

        switch (sortField)
        {
        case ID:
            s = "eperson_id";
            break;

        case EMAIL:
            s = "email";
            break;

        case LANGUAGE:
            s = "language";
            break;
        case NETID:
            s = "netid";
            break;

        default:
            s = "lastname";
        }

        // NOTE: The use of 's' in the order by clause can not cause an SQL 
        // injection because the string is derived from constant values above.
        TableRowIterator rows = DatabaseManager.query(context, 
                "SELECT * FROM eperson ORDER BY "+s);

        try
        {
            List<TableRow> epeopleRows = rows.toList();

            EPerson[] epeople = new EPerson[epeopleRows.size()];

            for (int i = 0; i < epeopleRows.size(); i++)
            {
                TableRow row = (TableRow) epeopleRows.get(i);

                // First check the cache
                EPerson fromCache = (EPerson) context.fromCache(EPerson.class, row
                        .getIntColumn("eperson_id"));

                if (fromCache != null)
                {
                    epeople[i] = fromCache;
                }
                else
                {
                    epeople[i] = new EPerson(context, row);
                }
            }

            return epeople;
        }
        finally
        {
            if (rows != null)
            {
                rows.close();
            }
        }
    }

    /**
     * Create a new eperson
     * 
     * @param context
     *            DSpace context object
     */
    public static EPerson create(Context context) throws SQLException,
            AuthorizeException
    {
        // authorized?
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "You must be an admin to create an EPerson");
        }

        // Create a table row
        TableRow row = DatabaseManager.create(context, "eperson");

        EPerson e = new EPerson(context, row);

        log.info(LogManager.getHeader(context, "create_eperson", "eperson_id="
                + e.getID()));

        context.addEvent(new Event(Event.CREATE, Constants.EPERSON, e.getID(), null));

        return e;
    }

    /**
     * Delete an eperson
     * 
     */
    public void delete() throws SQLException, AuthorizeException,
            EPersonDeletionException
    {
        // authorized?
        if (!AuthorizeManager.isAdmin(myContext))
        {
            throw new AuthorizeException(
                    "You must be an admin to delete an EPerson");
        }

        // check for presence of eperson in tables that
        // have constraints on eperson_id
        List<String> constraintList = getDeleteConstraints();

        // if eperson exists in tables that have constraints
        // on eperson, throw an exception
        if (constraintList.size() > 0)
        {
            throw new EPersonDeletionException(constraintList);
        }

        myContext.addEvent(new Event(Event.DELETE, Constants.EPERSON, getID(), getEmail()));

        // Remove from cache
        myContext.removeCached(this, getID());

        // XXX FIXME: This sidesteps the object model code so it won't
        // generate  REMOVE events on the affected Groups.

        // Remove any group memberships first
        DatabaseManager.updateQuery(myContext,
                "DELETE FROM EPersonGroup2EPerson WHERE eperson_id= ? ",
                getID());

        // Remove any subscriptions
        DatabaseManager.updateQuery(myContext,
                "DELETE FROM subscription WHERE eperson_id= ? ",
                getID());

        // Remove ourself
        DatabaseManager.delete(myContext, myRow);

        log.info(LogManager.getHeader(myContext, "delete_eperson",
                "eperson_id=" + getID()));
    }

    /**
     * Get the e-person's internal identifier
     * 
     * @return the internal identifier
     */
    public int getID()
    {
        return myRow.getIntColumn("eperson_id");
    }
    
    /**
     * Get the e-person's language
     * 
     * @return language code (or null if the column is an SQL NULL)
     */
     public String getLanguage()
     {
         return myRow.getStringColumn("language");
     }
     
     /**
     * Set the EPerson's language.  Value is expected to be a Unix/POSIX
     * Locale specification of the form {language} or {language}_{territory},
     * e.g. "en", "en_US", "pt_BR" (the latter is Brazilian Portugese).
     * 
     * @param language
     *            language code
     */
     public void setLanguage(String language)
     {
         myRow.setColumn("language", language);
     }
  

    /**
     * Get the e-person's handle
     * 
     * @return current implementation always returns null
     */
    public String getHandle()
    {
        // No Handles for e-people
        return null;
    }

    /**
     * Get the e-person's email address
     * 
     * @return their email address (or null if the column is an SQL NULL)
     */
    public String getEmail()
    {
        return myRow.getStringColumn("email");
    }

    /**
     * Set the EPerson's email
     * 
     * @param s
     *            the new email
     */
    public void setEmail(String s)
    {
        if (s != null)
        {
            s = s.toLowerCase();
        }

        myRow.setColumn("email", s);
        modified = true;
    }

    /**
     * Get the e-person's netid
     * 
     * @return their netid (DB constraints ensure it's never NULL)
     */
    public String getNetid()
    {
        return myRow.getStringColumn("netid");
    }

    /**
     * Set the EPerson's netid
     * 
     * @param s
     *            the new netid
     */
    public void setNetid(String s)
    {
        myRow.setColumn("netid", s);
        modified = true;
    }

    /**
     * Get the e-person's full name, combining first and last name in a
     * displayable string.
     * 
     * @return their full name (first + last name; if both are NULL, returns email)
     */
    public String getFullName()
    {
        String f = myRow.getStringColumn("firstname");
        String l = myRow.getStringColumn("lastname");

        if ((l == null) && (f == null))
        {
            return getEmail();
        }
        else if (f == null)
        {
            return l;
        }
        else
        {
            return (f + " " + l);
        }
    }

    /**
     * Get the eperson's first name.
     * 
     * @return their first name (or null if the column is an SQL NULL)
     */
    public String getFirstName()
    {
        return myRow.getStringColumn("firstname");
    }

    /**
     * Set the eperson's first name
     * 
     * @param firstname
     *            the person's first name
     */
    public void setFirstName(String firstname)
    {
        myRow.setColumn("firstname", firstname);
        modified = true;
    }

    /**
     * Get the eperson's last name.
     * 
     * @return their last name (or null if the column is an SQL NULL)
     */
    public String getLastName()
    {
        return myRow.getStringColumn("lastname");
    }

    /**
     * Set the eperson's last name
     * 
     * @param lastname
     *            the person's last name
     */
    public void setLastName(String lastname)
    {
        myRow.setColumn("lastname", lastname);
        modified = true;
    }

    /**
     * Indicate whether the user can log in
     * 
     * @param login
     *            boolean yes/no
     */
    public void setCanLogIn(boolean login)
    {
        myRow.setColumn("can_log_in", login);
        modified = true;
    }

    /**
     * Can the user log in?
     * 
     * @return boolean, yes/no
     */
    public boolean canLogIn()
    {
        return myRow.getBooleanColumn("can_log_in");
    }

    /**
     * Set require cert yes/no
     * 
     * @param isrequired
     *            boolean yes/no
     */
    public void setRequireCertificate(boolean isrequired)
    {
        myRow.setColumn("require_certificate", isrequired);
        modified = true;
    }

    /**
     * Get require certificate or not
     * 
     * @return boolean, yes/no (or false if the column is an SQL NULL)
     */
    public boolean getRequireCertificate()
    {
        return myRow.getBooleanColumn("require_certificate");
    }

    /**
     * Indicate whether the user self-registered
     * 
     * @param sr
     *            boolean yes/no
     */
    public void setSelfRegistered(boolean sr)
    {
        myRow.setColumn("self_registered", sr);
        modified = true;
    }

    /**
     * Can the user log in?
     * 
     * @return boolean, yes/no (or false if the column is an SQL NULL)
     */
    public boolean getSelfRegistered()
    {
        return myRow.getBooleanColumn("self_registered");
    }

    /**
     * Get the value of a metadata field
     * 
     * @param field
     *            the name of the metadata field to get
     * 
     * @return the value of the metadata field (or null if the column is an SQL NULL)
     * 
     * @exception IllegalArgumentException
     *                if the requested metadata field doesn't exist
     */
    public String getMetadata(String field)
    {
        return myRow.getStringColumn(field);
    }

    /**
     * Set a metadata value
     * 
     * @param field
     *            the name of the metadata field to set
     * @param value
     *            value to set the field to
     * 
     * @exception IllegalArgumentException
     *                if the requested metadata field doesn't exist
     */
    public void setMetadata(String field, String value)
    {
        myRow.setColumn(field, value);
        modifiedMetadata = true;
        addDetails(field);
    }

    /**
     * Set the EPerson's password.
     * 
     * @param s
     *            the new password.
     */
    public void setPassword(String s)
    {
        PasswordHash hash = new PasswordHash(s);
        myRow.setColumn("password", Utils.toHex(hash.getHash()));
        myRow.setColumn("salt", Utils.toHex(hash.getSalt()));
        myRow.setColumn("digest_algorithm", hash.getAlgorithm());
        modified = true;
    }

    /**
     * Set the EPerson's password hash.
     * 
     * @param password
     *          hashed password, or null to set row data to NULL.
     */
    public void setPasswordHash(PasswordHash password)
    {
        if (null == password)
        {
            myRow.setColumnNull("digest_algorithm");
            myRow.setColumnNull("salt");
            myRow.setColumnNull("password");
        }
        else
        {
            myRow.setColumn("digest_algorithm", password.getAlgorithm());
            myRow.setColumn("salt", password.getSaltString());
            myRow.setColumn("password", password.getHashString());
        }
        modified = true;
    }

    /**
     * Return the EPerson's password hash.
     *
     * @return hash of the password
     */
    public PasswordHash getPasswordHash()
    {
        PasswordHash hash = null;
        try {
            hash = new PasswordHash(myRow.getStringColumn("digest_algorithm"),
                    myRow.getStringColumn("salt"),
                    myRow.getStringColumn("password"));
        } catch (DecoderException ex) {
            log.error("Problem decoding stored salt or hash:  " + ex.getMessage());
        }
        return hash;
    }

    /**
     * Check EPerson's password.  Side effect:  original unsalted MD5 hashes are
     * converted using the current algorithm.
     * 
     * @param attempt
     *            the password attempt
     * @return boolean successful/unsuccessful
     */
    public boolean checkPassword(String attempt)
    {
        PasswordHash myHash;
        try
        {
            myHash = new PasswordHash(
                    myRow.getStringColumn("digest_algorithm"),
                    myRow.getStringColumn("salt"),
                    myRow.getStringColumn("password"));
        } catch (DecoderException ex)
        {
            log.error(ex.getMessage());
            return false;
        }
        boolean answer = myHash.matches(attempt);

        // If using the old unsalted hash, and this password is correct, update to a new hash
        if (answer && (null == myRow.getStringColumn("digest_algorithm")))
        {
            log.info("Upgrading password hash for EPerson " + getID());
            setPassword(attempt);
            try {
                myContext.turnOffAuthorisationSystem();
                update();
            } catch (SQLException ex) {
                log.error("Could not update password hash", ex);
            } catch (AuthorizeException ex) {
                log.error("Could not update password hash", ex);
            } finally {
                myContext.restoreAuthSystemState();
            }
        }

        return answer;
    }

    /**
     * Update the EPerson
     */
    public void update() throws SQLException, AuthorizeException
    {
        // Check authorisation - if you're not the eperson
        // see if the authorization system says you can
        if (!myContext.ignoreAuthorization()
                && ((myContext.getCurrentUser() == null) || (getID() != myContext
                        .getCurrentUser().getID())))
        {
            AuthorizeManager.authorizeAction(myContext, this, Constants.WRITE);
        }

        DatabaseManager.update(myContext, myRow);

        log.info(LogManager.getHeader(myContext, "update_eperson",
                "eperson_id=" + getID()));

        if (modified)
        {
            myContext.addEvent(new Event(Event.MODIFY, Constants.EPERSON, getID(), null));
            modified = false;
        }
        if (modifiedMetadata)
        {
            myContext.addEvent(new Event(Event.MODIFY_METADATA, Constants.EPERSON, getID(), getDetails()));
            modifiedMetadata = false;
            clearDetails();
        }
    }

    /**
     * return type found in Constants
     */
    public int getType()
    {
        return Constants.EPERSON;
    }

    /**
     * Check for presence of EPerson in tables that have constraints on
     * EPersons. Called by delete() to determine whether the eperson can
     * actually be deleted.
     * 
     * An EPerson cannot be deleted if it exists in the item, workflowitem, or
     * tasklistitem tables.
     * 
     * @return List of tables that contain a reference to the eperson.
     */
    public List<String> getDeleteConstraints() throws SQLException
    {
        List<String> tableList = new ArrayList<String>();

        // check for eperson in item table
        TableRowIterator tri = DatabaseManager.query(myContext, 
                "SELECT * from item where submitter_id= ? ",
                getID());

        try
        {
            if (tri.hasNext())
            {
                tableList.add("item");
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
        }

        if(ConfigurationManager.getProperty("workflow","workflow.framework").equals("xmlworkflow")){
            getXMLWorkflowConstraints(tableList);
        }else{
            getOriginalWorkflowConstraints(tableList);

        }
        // the list of tables can be used to construct an error message
        // explaining to the user why the eperson cannot be deleted.
        return tableList;
    }

    private void getXMLWorkflowConstraints(List<String> tableList) throws SQLException {
         TableRowIterator tri;
        // check for eperson in claimtask table
        tri = DatabaseManager.queryTable(myContext, "cwf_claimtask",
                "SELECT * from cwf_claimtask where owner_id= ? ",
                getID());

        try
        {
            if (tri.hasNext())
            {
                tableList.add("cwf_claimtask");
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
        }

        // check for eperson in pooltask table
        tri = DatabaseManager.queryTable(myContext, "cwf_pooltask",
                "SELECT * from cwf_pooltask where eperson_id= ? ",
                getID());

        try
        {
            if (tri.hasNext())
            {
                tableList.add("cwf_pooltask");
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
        }

        // check for eperson in workflowitemrole table
        tri = DatabaseManager.queryTable(myContext, "cwf_workflowitemrole",
                "SELECT * from cwf_workflowitemrole where eperson_id= ? ",
                getID());

        try
        {
            if (tri.hasNext())
            {
                tableList.add("cwf_workflowitemrole");
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
        }

    }

    private void getOriginalWorkflowConstraints(List<String> tableList) throws SQLException {
        TableRowIterator tri;
        // check for eperson in workflowitem table
        tri = DatabaseManager.query(myContext, 
                "SELECT * from workflowitem where owner= ? ",
                getID());

        try
        {
            if (tri.hasNext())
            {
                tableList.add("workflowitem");
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
        }

        // check for eperson in tasklistitem table
        tri = DatabaseManager.query(myContext, 
                "SELECT * from tasklistitem where eperson_id= ? ",
                getID());

        try
        {
            if (tri.hasNext())
            {
                tableList.add("tasklistitem");
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
        }
    }

    public String getName()
    {
        return getEmail();
    }

    @Override
    public void updateLastModified()
    {

    }

}
