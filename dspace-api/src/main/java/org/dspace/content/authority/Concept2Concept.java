/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.NonUniqueMetadataException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Object that represents a relationship between concepts, which is
 * defined by a combination of "role", "incoming" Concept and
 * outgoing Concept internal database identifiers.
 *
 * @author Lantian Gai, Mark Diggory
 */
public class Concept2Concept
{
    private int relationID = 0;
    private int role_id = 0;
    private int incoming_id;
    private int outgoing_id;
    private Context context;
    private boolean modified = false;

    /** log4j logger */
    private static Logger log = Logger.getLogger(Concept2Concept.class);

    /** The row in the table representing this type */
    private TableRow row;

    // cache of relation by ID (Integer)
    private static Map<Integer, Concept2Concept> id2relation = null;


    /**
     * Default constructor.
     */
    public Concept2Concept()
    {
    }
    /**
     * Constructor creating a relation within a schema.
     *
     */
    public Concept2Concept(Concept2ConceptRole role)
    {
        this.role_id = role.getRelationID();
    }

    /**
     * Full constructor for new metadata relation elements.
     *


     */
    public Concept2Concept(Concept2ConceptRole role, int incoming_id,
                           int outgoing_id)
    {
        this.role_id = role.getRelationID();
        this.incoming_id = incoming_id;
        this.outgoing_id = outgoing_id;
    }

    /**
     * Full constructor for existing metadata relation elements.
     *
     * @param role_id schema to which the relation belongs
     * @param relationID database ID of relation.


     */
    public Concept2Concept(int role_id, int relationID, int incoming_id,
                           int outgoing_id)
    {
        this.role_id = role_id;
        this.relationID = relationID;
        this.incoming_id = incoming_id;
        this.outgoing_id = outgoing_id;

    }

    /**
     * Constructor to load the object from the database.
     *
     * @param row database row from which to populate object.
     */
    public Concept2Concept(TableRow row)
    {
        if (row != null)
        {
            this.relationID = row.getIntColumn("id");
            this.role_id = row.getIntColumn("role_id");
            this.incoming_id = row.getIntColumn("incoming_id");
            this.outgoing_id = row.getIntColumn("outgoing_id");
            this.row = row;
        }
    }

    /**
     * Get the element name.
     *
     * @return element name
     */
    public int getIncomingId()
    {
        return incoming_id;
    }

    /**
     * Set the element name.
     *

     */
    public void setIncomingId(int incoming_id)
    {
        this.incoming_id = incoming_id;
    }

    /**
     * Get the metadata relation id.
     *
     * @return metadata relation id
     */
    public int getRelationID()
    {
        return relationID;
    }

    /**
     * Get the qualifier.
     *
     * @return qualifier
     */
    public int getOutgoingId()
    {
        return outgoing_id;
    }

    /**
     * Set the qualifier.
     *
     */
    public void setOutgoingId(int outgoing_id)
    {
        this.outgoing_id = outgoing_id;
    }

    /**
     * Get the schema record key.
     *
     * @return schema record key
     */
    public int getRoleId()
    {
        return role_id;
    }

    /**
     * Set the schema record key.
     *
     * @param role_id new value for key
     */
    public void setRoleId(int role_id)
    {
        this.role_id = role_id;
    }

    Concept2Concept(Context context, TableRow row) throws SQLException
    {
        this.context = context;
        this.row = row;
        this.modified = false;

        // Cache ourselves
        context.cache(this, row.getIntColumn("id"));
    }
    /**
     * Creates a new metadata relation.
     *
     * @param context
     *            DSpace context object
     * @throws java.io.IOException
     * @throws org.dspace.authorize.AuthorizeException
     * @throws java.sql.SQLException
     * @throws org.dspace.content.NonUniqueMetadataException
     */
    public static Concept2Concept create(Context context) throws  AuthorizeException,
            SQLException
    {
        // authorized?
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "You must be an admin to create an Concept2Concept");
        }

        // Create a table row
        TableRow row = DatabaseManager.create(context, "Concept2Concept");

        Concept2Concept e = new Concept2Concept(context, row);

        log.info(LogManager.getHeader(context, "create_Concept2Concept", "Concept2Concept_id="
                + e.getRelationID()));

        //context.addEvent(new Event(Event.CREATE, Constants.EPERSON, e.getID(), null));

        return e;
    }


    /**
     * Retrieves the metadata relation from the database.
     *
     * @param context dspace context
     * @param role_id schema by ID

     * @return recalled metadata relation
     * @throws java.sql.SQLException
     * @throws org.dspace.authorize.AuthorizeException
     */
    public static Concept2Concept findByElement(Context context, int role_id,
                                                int incoming_id, int outgoing_id) throws SQLException,
            AuthorizeException
    {
        // Grab rows from DB
        TableRowIterator tri;

        tri = DatabaseManager.queryTable(context,"Concept2Concept",
                "SELECT * FROM Concept2Concept WHERE role_id= ? " +
                        "AND incoming_id= ?  AND outgoing_id= ? ",
                role_id, incoming_id, outgoing_id);


        TableRow row = null;
        try
        {
            if (tri.hasNext())
            {
                row = tri.next();
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

        if (row == null)
        {
            return null;
        }
        else
        {
            return new Concept2Concept(row);
        }
    }

    /**
     * Retrieve all Dublin Core types from the registry
     *
     * @param context dspace context
     * @return an array of all the Dublin Core types
     * @throws java.sql.SQLException
     */
    public static Concept2Concept[] findAll(Context context) throws SQLException
    {
        List<Concept2Concept> relations = new ArrayList<Concept2Concept>();

        // Get all the metadatarelationregistry rows
        TableRowIterator tri = DatabaseManager.queryTable(context, "Concept2Concept",
                "SELECT mfr.* FROM Concept2Concept mfr ORDER BY msr.role_id,  mfr.incoming_id, mfr.outgoing_id");

        try
        {
            // Make into DC Type objects
            while (tri.hasNext())
            {
                relations.add(new Concept2Concept(tri.next()));
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

        // Convert list into an array
        Concept2Concept[] typeArray = new Concept2Concept[relations.size()];
        return (Concept2Concept[]) relations.toArray(typeArray);
    }

    /**
     * Return all metadata relations that are found in a given schema.
     *
     * @param context dspace context
     * @param role_id schema by db ID
     * @return array of metadata relations
     * @throws java.sql.SQLException
     */
    public static Concept2Concept[] findAllByRole(Context context, int role_id)
            throws SQLException
    {
        List<Concept2Concept> relations = new ArrayList<Concept2Concept>();

        // Get all the metadatarelationregistry rows
        TableRowIterator tri = DatabaseManager.queryTable(context,"Concept2Concept",
                "SELECT * FROM Concept2Concept WHERE role_id= ? " +
                        " ORDER BY incoming_id, outgoing_id", role_id);

        try
        {
            // Make into DC Type objects
            while (tri.hasNext())
            {
                relations.add(new Concept2Concept(tri.next()));
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

        // Convert list into an array
        Concept2Concept[] typeArray = new Concept2Concept[relations.size()];
        return (Concept2Concept[]) relations.toArray(typeArray);
    }

    /**
     * Update the metadata relation in the database.
     *
     * @param context dspace context
     * @throws java.sql.SQLException
     * @throws org.dspace.authorize.AuthorizeException
     * @throws org.dspace.content.NonUniqueMetadataException
     * @throws java.io.IOException
     */
    public void update(Context context) throws SQLException,
            AuthorizeException, NonUniqueMetadataException, IOException
    {
        // Check authorisation: Only admins may update the metadata registry
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "Only administrators may modiffy the Dublin Core registry");
        }

        // Check to see if the schema ID was altered. If is was then we will
        // query to ensure that there is not already a duplicate name relation.
        if (row.getIntColumn("role_id") != role_id)
        {
            if (Concept2Concept.hasElement(context, role_id, incoming_id, outgoing_id))
            {
                throw new NonUniqueMetadataException(
                        "Duplcate relation name found in target schema");
            }
        }

        // Ensure the element and qualifier are unique within a given schema.
        if (!unique(context, role_id, incoming_id, outgoing_id))
        {
            throw new NonUniqueMetadataException("Please make " + incoming_id + "."
                    + outgoing_id);
        }

        row.setColumn("role_id", role_id);
        row.setColumn("incoming_id", incoming_id);
        row.setColumn("outgoing_id", outgoing_id);
        DatabaseManager.update(context, row);
        decache();

        log.info(LogManager.getHeader(context, "update_Concept2Concept",
                "Concept2Concept_id=" + getRelationID() + "incoming_id=" + incoming_id
                        + "outgoing_id=" + outgoing_id));
    }

    /**
     * Return true if and only if the schema has a relation with the given element
     * and qualifier pair.
     *
     * @param context dspace context
     * @param role_id schema by ID

     * @return true if the relation exists
     * @throws java.sql.SQLException
     * @throws org.dspace.authorize.AuthorizeException
     */
    private static boolean hasElement(Context context, int role_id,
                                      int incoming_id, int outgoing_id) throws SQLException,
            AuthorizeException
    {
        return Concept2Concept.findByElement(context, role_id, incoming_id,
                outgoing_id) != null;
    }

    /**
     * Delete the metadata relation.
     *
     * @param context dspace context
     * @throws java.sql.SQLException
     * @throws org.dspace.authorize.AuthorizeException
     */
    public void delete(Context context) throws SQLException, AuthorizeException
    {
        // Check authorisation: Only admins may create DC types
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "Only administrators may modify the incoming registry");
        }

        log.info(LogManager.getHeader(context, "delete_Concept2Concept",
                "metadata_incoming_relation_id=" + getRelationID()));

        DatabaseManager.delete(context, row);
        decache();
    }

    /**
     * A sanity check that ensures a given element and qualifier are unique
     * within a given schema. The check happens in code as we cannot use a
     * database constraint.
     *
     * @param context dspace context
     * @param role_id

     * @return true if unique
     * @throws org.dspace.authorize.AuthorizeException
     * @throws java.sql.SQLException
     * @throws java.io.IOException
     */
    private boolean unique(Context context, int role_id, int incoming_id,
                           int outgoing_id) throws IOException, SQLException,
            AuthorizeException
    {
        int count = 0;
        Connection con = null;
        PreparedStatement statement = null;
        ResultSet rs = null;

        try
        {
            con = context.getDBConnection();
            TableRow reg = DatabaseManager.row("Concept2Concept");

            String query = "SELECT COUNT(*) FROM " + reg.getTable()
                    + " WHERE role_id= ? "
                    + " and incoming_id= ? " + "and outgoing_id = ?";

            statement = con.prepareStatement(query);
            statement.setInt(1,role_id);
            statement.setInt(2,incoming_id);
            statement.setInt(3,outgoing_id);


            rs = statement.executeQuery();

            if (rs.next())
            {
                count = rs.getInt(1);
            }
        }
        finally
        {
            if (rs != null)
            {
                try { rs.close(); } catch (SQLException sqle) { }
            }

            if (statement != null)
            {
                try { statement.close(); } catch (SQLException sqle) { }
            }
        }

        return (count == 0);
    }

    /**
     * Return the HTML FORM key for the given relation.
     *
     * @return HTML FORM key
     */
    public static String formKey(int role_id, int incoming_id, int outgoing_id)
    {
        return role_id+"_"+incoming_id+"_"+outgoing_id;
    }

    /**
     * Find the relation corresponding to the given numeric ID.  The ID is
     * a database key internal to DSpace.
     *
     * @param context
     *            context, in case we need to read it in from DB
     * @param id
     *            the metadata relation ID
     * @return the metadata relation object
     * @throws java.sql.SQLException
     */
    public static Concept2Concept find(Context context, int id)
            throws SQLException
    {
        if (!isCacheInitialized())
        {
            initCache(context);
        }

        // 'sanity check' first.
        Integer iid = Integer.valueOf(id);
        if (!id2relation.containsKey(iid))
        {
            return null;
        }

        return id2relation.get(iid);
    }

    // invalidate the cache e.g. after something modifies DB state.
    private static void decache()
    {
        id2relation = null;
    }

    private static boolean isCacheInitialized()
    {
        return id2relation != null;
    }

    // load caches if necessary
    private static synchronized void initCache(Context context) throws SQLException
    {
        if (!isCacheInitialized())
        {
            Map<Integer, Concept2Concept> new_id2relation = new HashMap<Integer, Concept2Concept>();
            log.info("Loading Concept2Concept elements into cache.");

            // Grab rows from DB
            TableRowIterator tri = DatabaseManager.queryTable(context,"Concept2Concept",
                    "SELECT * from Concept2Concept");

            try
            {
                while (tri.hasNext())
                {
                    TableRow row = tri.next();
                    int relationID = row.getIntColumn("metadata_incoming_relation_id");
                    new_id2relation.put(Integer.valueOf(relationID), new Concept2Concept(row));
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

            id2relation = new_id2relation;
        }
    }

    /**
     * Return <code>true</code> if <code>other</code> is the same MetadataRelation
     * as this object, <code>false</code> otherwise
     *
     * @param obj
     *            object to compare to
     *
     * @return <code>true</code> if object passed in represents the same
     *         MetadataRelation as this object
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
        final Concept2Concept other = (Concept2Concept) obj;
        if (this.relationID != other.relationID)
        {
            return false;
        }
        if (this.role_id != other.role_id)
        {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 47 * hash + this.relationID;
        hash = 47 * hash + this.role_id;
        return hash;
    }
    public static Concept2Concept[] search(Context context, String query)
            throws SQLException
    {
        return search(context, query, -1, -1);
    }


    public static Concept2Concept[] search(Context context, String query, int offset, int limit)
            throws SQLException
    {
        String params = "%"+query.toLowerCase()+"%";
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append("SELECT count(*) as outgoingcount FROM incoming2outgoing,incoming2outgoingrole WHERE  incoming2outgoing.id = incoming2outgoingrole.id AND (incoming2outgoing.id = ? OR ");
        queryBuf.append("incoming2outgoing.role_id = ? OR incoming2outgoing.incoming_id = ? OR incoming2outgoing.outgoing_id = ? OR incoming2outgoing.role like LOWER(?) ORDER BY  incoming2outgoing.id desc");

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
        Object[] paramArr = new Object[] {int_param,int_param,int_param,int_param,params};
        if (limit > 0 && offset > 0)
        {
            paramArr = new Object[]{int_param, int_param,int_param,int_param,params,limit, offset};
        }
        else if (limit > 0)
        {
            paramArr = new Object[]{int_param,int_param,int_param,int_param,params,  limit};
        }
        else if (offset > 0)
        {
            paramArr = new Object[]{int_param,int_param,int_param,int_param,params,  offset};
        }

        // Get all the epeople that match the query
        TableRowIterator rows = DatabaseManager.query(context,
                dbquery, paramArr);
        try
        {
            List<TableRow> incoming2ConceptsRows = rows.toList();
            Concept2Concept[] incoming2Concepts = new Concept2Concept[incoming2ConceptsRows.size()];

            for (int i = 0; i < incoming2ConceptsRows.size(); i++)
            {
                TableRow row = (TableRow) incoming2ConceptsRows.get(i);

                // First check the cache
                Concept2Concept fromCache = (Concept2Concept) context.fromCache(Concept2Concept.class, row
                        .getIntColumn("id"));

                if (fromCache != null)
                {
                    incoming2Concepts[i] = fromCache;
                }
                else
                {
                    incoming2Concepts[i] = new Concept2Concept(row);
                }
            }

            return incoming2Concepts;
        }
        finally
        {
            if (rows != null)
            {
                rows.close();
            }
        }
    }

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
                "SELECT count(*) as outgoingcount FROM incoming2outgoing,incoming2outgoingrole WHERE  incoming2outgoing.role_id = incoming2outgoingrole.id AND (incoming2outgoing.id = ? OR " +
                        "incoming2outgoing.role_id = ? OR incoming2outgoing.incoming_id = ? OR incoming2outgoing.outgoing_id = ? OR incoming2outgoingrole.role like LOWER(?) ) ",
                new Object[] {int_param,int_param,int_param,int_param,dbquery});

        // use getIntColumn for Oracle count data
        if ("oracle".equals(ConfigurationManager.getProperty("db.name")))
        {
            count = Long.valueOf(row.getIntColumn("outgoingcount"));
        }
        else  //getLongColumn works for postgres
        {
            count = Long.valueOf(row.getLongColumn("outgoingcount"));
        }

        return count.intValue();
    }


    public static Concept2Concept findByConceptAndConcept(Context context,Integer incoming_id,Integer outgoing_id)
            throws SQLException, AuthorizeException
    {
        if (incoming_id == null||outgoing_id == null||incoming_id<0||outgoing_id<0)
        {
            return null;
        }

        Object[] paramArr = new Object[] {incoming_id,outgoing_id};
        // All email addresses are stored as lowercase, so ensure that the email address is lowercased for the lookup
        TableRowIterator rows = DatabaseManager.query(context,
                "SELECT * FROM incoming2outgoing WHERE concpet_id = ? AND outgoing_id = ? ", paramArr);

        try
        {
            List<TableRow> incoming2ConceptsRows = rows.toList();
            Concept2Concept[] incoming2Concepts = new Concept2Concept[incoming2ConceptsRows.size()];

            for (int i = 0; i < incoming2ConceptsRows.size(); i++)
            {
                TableRow row = (TableRow) incoming2ConceptsRows.get(i);

                // First check the cache
                Concept2Concept fromCache = (Concept2Concept) context.fromCache(Concept2Concept.class, row
                        .getIntColumn("id"));

                if (fromCache != null)
                {
                    incoming2Concepts[i] = fromCache;
                }
                else
                {
                    incoming2Concepts[i] = new Concept2Concept(row);
                }
            }

            return incoming2Concepts[0];
        }
        finally
        {
            if (rows != null)
            {
                rows.close();
            }
        }
    }


    public static Concept2Concept[] findByParentAndRole(Context context,Integer incoming_id,Integer role_id)
            throws SQLException, AuthorizeException
    {
        if (incoming_id == null||role_id == null||incoming_id<0||role_id<0)
        {
            return null;
        }

        Object[] paramArr = new Object[] {incoming_id,role_id};
        // All email addresses are stored as lowercase, so ensure that the email address is lowercased for the lookup
        TableRowIterator rows = DatabaseManager.query(context,
                "SELECT * FROM Concept2Concept WHERE incoming_id = ? AND role_id = ? ", paramArr);

        try
        {
            List<TableRow> incoming2ConceptsRows = rows.toList();
            Concept2Concept[] incoming2Concepts = new Concept2Concept[incoming2ConceptsRows.size()];

            for (int i = 0; i < incoming2ConceptsRows.size(); i++)
            {
                TableRow row = (TableRow) incoming2ConceptsRows.get(i);

                // First check the cache
                Concept2Concept fromCache = (Concept2Concept) context.fromCache(Concept2Concept.class, row
                        .getIntColumn("id"));

                if (fromCache != null)
                {
                    incoming2Concepts[i] = fromCache;
                }
                else
                {
                    incoming2Concepts[i] = new Concept2Concept(row);
                }
            }

            return incoming2Concepts;
        }
        finally
        {
            if (rows != null)
            {
                rows.close();
            }
        }

    }
    public static Concept2Concept[] findByChildAndRole(Context context,Integer outgoing_id,Integer role_id)
            throws SQLException, AuthorizeException
    {
        if (outgoing_id == null||role_id == null||outgoing_id<0||role_id<0)
        {
            return null;
        }

        Object[] paramArr = new Object[] {outgoing_id,outgoing_id};
        // All email addresses are stored as lowercase, so ensure that the email address is lowercased for the lookup
        TableRowIterator rows = DatabaseManager.query(context,
                "SELECT * FROM Concept2Concept WHERE outgoing_id = ? AND role_id = ? ", paramArr);

        try
        {
            List<TableRow> incoming2ConceptsRows = rows.toList();
            Concept2Concept[] incoming2Concepts = new Concept2Concept[incoming2ConceptsRows.size()];

            for (int i = 0; i < incoming2ConceptsRows.size(); i++)
            {
                TableRow row = (TableRow) incoming2ConceptsRows.get(i);

                // First check the cache
                Concept2Concept fromCache = (Concept2Concept) context.fromCache(Concept2Concept.class, row
                        .getIntColumn("id"));

                if (fromCache != null)
                {
                    incoming2Concepts[i] = fromCache;
                }
                else
                {
                    incoming2Concepts[i] = new Concept2Concept(row);
                }
            }

            return incoming2Concepts;
        }
        finally
        {
            if (rows != null)
            {
                rows.close();
            }
        }
    }

    public static Concept2Concept[] findByChild(Context context,Integer outgoing_id)
            throws SQLException, AuthorizeException
    {
        if (outgoing_id == null||outgoing_id<0)
        {
            return null;
        }

        Object[] paramArr = new Object[] {outgoing_id};
        // All email addresses are stored as lowercase, so ensure that the email address is lowercased for the lookup
        TableRowIterator rows = DatabaseManager.query(context,
                "SELECT * FROM Concept2Concept WHERE outgoing_id = ? ORDER BY role_id asc", paramArr);

        try
        {
            List<TableRow> incoming2ConceptsRows = rows.toList();
            Concept2Concept[] incoming2Concepts = new Concept2Concept[incoming2ConceptsRows.size()];

            for (int i = 0; i < incoming2ConceptsRows.size(); i++)
            {
                TableRow row = (TableRow) incoming2ConceptsRows.get(i);

                // First check the cache
                Concept2Concept fromCache = (Concept2Concept) context.fromCache(Concept2Concept.class, row
                        .getIntColumn("id"));

                if (fromCache != null)
                {
                    incoming2Concepts[i] = fromCache;
                }
                else
                {
                    incoming2Concepts[i] = new Concept2Concept(row);
                }
            }

            return incoming2Concepts;
        }
        finally
        {
            if (rows != null)
            {
                rows.close();
            }
        }
    }

    public static Concept2Concept[] findByParent(Context context,Integer incoming_id)
            throws SQLException, AuthorizeException
    {
        if (incoming_id == null||incoming_id<0)
        {
            return null;
        }

        Object[] paramArr = new Object[] {incoming_id};
        // All email addresses are stored as lowercase, so ensure that the email address is lowercased for the lookup
        TableRowIterator rows = DatabaseManager.query(context,
                "SELECT * FROM Concept2Concept WHERE incoming_id = ? ORDER BY role_id asc", paramArr);

        try
        {
            List<TableRow> incoming2ConceptsRows = rows.toList();
            Concept2Concept[] incoming2Concepts = new Concept2Concept[incoming2ConceptsRows.size()];

            for (int i = 0; i < incoming2ConceptsRows.size(); i++)
            {
                TableRow row = (TableRow) incoming2ConceptsRows.get(i);

                // First check the cache
                Concept2Concept fromCache = (Concept2Concept) context.fromCache(Concept2Concept.class, row
                        .getIntColumn("id"));

                if (fromCache != null)
                {
                    incoming2Concepts[i] = fromCache;
                }
                else
                {
                    incoming2Concepts[i] = new Concept2Concept(row);
                }
            }

            return incoming2Concepts;
        }
        finally
        {
            if (rows != null)
            {
                rows.close();
            }
        }
    }

}
