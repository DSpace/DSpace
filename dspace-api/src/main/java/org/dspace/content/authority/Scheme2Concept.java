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
 * Class representing a DSpace Scheme2Concept Mapping.
 *
 * @author Lantian Gai, Mark Diggory
 */
public class Scheme2Concept
{
    private int relationID = 0;
    private int scheme_id;
    private int concept_id;
    private Context context;
    private boolean modified = false;

    /** log4j logger */
    private static Logger log = Logger.getLogger(Scheme2Concept.class);

    /** The row in the table representing this type */
    private TableRow row;

    // cache of relation by ID (Integer)
    private static Map<Integer, Scheme2Concept> id2relation = null;


    /**
     * Default constructor.
     */
    public Scheme2Concept()
    {
    }
    /**
     * Constructor creating a relation within a schema.
     *
     */


    /**
     * Full constructor for new metadata relation elements.
     *


     */
    public Scheme2Concept(int scheme_id,
                          int concept_id)
    {
        this.scheme_id = scheme_id;
        this.concept_id = concept_id;
    }

    /**
     * Full constructor for existing metadata relation elements.
     *
     * @param relationID database ID of relation.


     */
    public Scheme2Concept(int relationID, int scheme_id,
                          int concept_id)
    {
        this.relationID = relationID;
        this.scheme_id = scheme_id;
        this.concept_id = concept_id;

    }

    /**
     * Constructor to load the object from the database.
     *
     * @param row database row from which to populate object.
     */
    public Scheme2Concept(TableRow row)
    {
        if (row != null)
        {
            this.relationID = row.getIntColumn("id");
            this.scheme_id = row.getIntColumn("scheme_id");
            this.concept_id = row.getIntColumn("concept_id");
            this.row = row;
        }
    }

    /**
     * Get the element name.
     *
     * @return element name
     */
    public int getSchemeId()
    {
        return scheme_id;
    }

    /**
     * Set the element name.
     *

     */
    public void setSchemeId(int scheme_id)
    {
        this.scheme_id = scheme_id;
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
    public int getConceptId()
    {
        return concept_id;
    }

    /**
     * Set the qualifier.
     *
     */
    public void setConceptId(int concept_id)
    {
        this.concept_id = concept_id;
    }


    Scheme2Concept(Context context, TableRow row) throws SQLException
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
    public static Scheme2Concept create(Context context) throws  AuthorizeException,
            SQLException
    {
        // authorized?
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "You must be an admin to create an Scheme2Concept");
        }

        // Create a table row
        TableRow row = DatabaseManager.create(context, "Scheme2Concept");

        Scheme2Concept e = new Scheme2Concept(context, row);

        log.info(LogManager.getHeader(context, "create_Scheme2Concept", "Scheme2Concept_id="
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
    public static Scheme2Concept findByElement(Context context, int role_id,
                                               int scheme_id, int concept_id) throws SQLException,
            AuthorizeException
    {
        // Grab rows from DB
        TableRowIterator tri;

        tri = DatabaseManager.queryTable(context,"Scheme2Concept",
                "SELECT * FROM Scheme2Concept WHERE role_id= ? " +
                        "AND scheme_id= ?  AND concept_id= ? ",
                role_id, scheme_id, concept_id);


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
            return new Scheme2Concept(row);
        }
    }

    /**
     * Retrieve all Dublin Core types from the registry
     *
     * @param context dspace context
     * @return an array of all the Dublin Core types
     * @throws java.sql.SQLException
     */
    public static Scheme2Concept[] findAll(Context context) throws SQLException
    {
        List<Scheme2Concept> relations = new ArrayList<Scheme2Concept>();

        // Get all the metadatarelationregistry rows
        TableRowIterator tri = DatabaseManager.queryTable(context, "Scheme2Concept",
                "SELECT mfr.* FROM Scheme2Concept mfr ORDER BY msr.role_id,  mfr.scheme_id, mfr.concept_id");

        try
        {
            // Make into DC Type objects
            while (tri.hasNext())
            {
                relations.add(new Scheme2Concept(tri.next()));
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
        Scheme2Concept[] typeArray = new Scheme2Concept[relations.size()];
        return (Scheme2Concept[]) relations.toArray(typeArray);
    }

    /**
     * Return all metadata relations that are found in a given schema.
     *
     * @param context dspace context
     * @param role_id schema by db ID
     * @return array of metadata relations
     * @throws java.sql.SQLException
     */
    public static Scheme2Concept[] findAllByRole(Context context, int role_id)
            throws SQLException
    {
        List<Scheme2Concept> relations = new ArrayList<Scheme2Concept>();

        // Get all the metadatarelationregistry rows
        TableRowIterator tri = DatabaseManager.queryTable(context,"Scheme2Concept",
                "SELECT * FROM Scheme2Concept WHERE role_id= ? " +
                        " ORDER BY scheme_id, concept_id", role_id);

        try
        {
            // Make into DC Type objects
            while (tri.hasNext())
            {
                relations.add(new Scheme2Concept(tri.next()));
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
        Scheme2Concept[] typeArray = new Scheme2Concept[relations.size()];
        return (Scheme2Concept[]) relations.toArray(typeArray);
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

        row.setColumn("scheme_id", scheme_id);
        row.setColumn("concept_id", concept_id);
        DatabaseManager.update(context, row);
        decache();

        log.info(LogManager.getHeader(context, "update_Scheme2Concept",
                "Scheme2Concept_id=" + getRelationID() + "scheme_id=" + getSchemeId()
                        + "concept_id=" + getConceptId()));
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
                                      int scheme_id, int concept_id) throws SQLException,
            AuthorizeException
    {
        return Scheme2Concept.findByElement(context, role_id, scheme_id,
                concept_id) != null;
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
                    "Only administrators may modify the scheme registry");
        }

        log.info(LogManager.getHeader(context, "delete_Scheme2Concept",
                "metadata_scheme_relation_id=" + getRelationID()));

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
    private boolean unique(Context context, int role_id, int scheme_id,
                           int concept_id) throws IOException, SQLException,
            AuthorizeException
    {
        int count = 0;
        Connection con = null;
        PreparedStatement statement = null;
        ResultSet rs = null;

        try
        {
            con = context.getDBConnection();
            TableRow reg = DatabaseManager.row("Scheme2Concept");

            String query = "SELECT COUNT(*) FROM " + reg.getTable()
                    + " WHERE role_id= ? "
                    + " and scheme_id= ? " + "and concept_id = ?";

            statement = con.prepareStatement(query);
            statement.setInt(1,role_id);
            statement.setInt(2,scheme_id);
            statement.setInt(3,concept_id);


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
    public static String formKey(int role_id, int scheme_id, int concept_id)
    {
        return role_id+"_"+scheme_id+"_"+concept_id;
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
    public static Scheme2Concept find(Context context, int id)
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
            Map<Integer, Scheme2Concept> new_id2relation = new HashMap<Integer, Scheme2Concept>();
            log.info("Loading Scheme2Concept elements into cache.");

            // Grab rows from DB
            TableRowIterator tri = DatabaseManager.queryTable(context,"Scheme2Concept",
                    "SELECT * from Scheme2Concept");

            try
            {
                while (tri.hasNext())
                {
                    TableRow row = tri.next();
                    int relationID = row.getIntColumn("metadata_scheme_relation_id");
                    new_id2relation.put(Integer.valueOf(relationID), new Scheme2Concept(row));
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
        final Scheme2Concept other = (Scheme2Concept) obj;
        if (this.relationID != other.relationID)
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
        return hash;
    }
    public static Scheme2Concept[] search(Context context, String query)
            throws SQLException
    {
        return search(context, query, -1, -1);
    }


    public static Scheme2Concept[] search(Context context, String query, int offset, int limit)
            throws SQLException
    {
        String params = "%"+query.toLowerCase()+"%";
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append("SELECT count(*) as conceptcount FROM scheme2concept,scheme2conceptrole WHERE  scheme2concept.id = scheme2conceptrole.id AND (scheme2concept.id = ? OR ");
        queryBuf.append("scheme2concept.role_id = ? OR scheme2concept.scheme_id = ? OR scheme2concept.concept_id = ? OR scheme2concept.role like LOWER(?) ORDER BY  scheme2concept.id desc");

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
            List<TableRow> scheme2ConceptsRows = rows.toList();
            Scheme2Concept[] scheme2Concepts = new Scheme2Concept[scheme2ConceptsRows.size()];

            for (int i = 0; i < scheme2ConceptsRows.size(); i++)
            {
                TableRow row = (TableRow) scheme2ConceptsRows.get(i);

                // First check the cache
                Scheme2Concept fromCache = (Scheme2Concept) context.fromCache(Scheme2Concept.class, row
                        .getIntColumn("id"));

                if (fromCache != null)
                {
                    scheme2Concepts[i] = fromCache;
                }
                else
                {
                    scheme2Concepts[i] = new Scheme2Concept(row);
                }
            }

            return scheme2Concepts;
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
                "SELECT count(*) as conceptcount FROM scheme2concept,scheme2conceptrole WHERE  scheme2concept.role_id = scheme2conceptrole.id AND (scheme2concept.id = ? OR " +
                        "scheme2concept.role_id = ? OR scheme2concept.scheme_id = ? OR scheme2concept.concept_id = ? OR scheme2conceptrole.role like LOWER(?) ) ",
                new Object[] {int_param,int_param,int_param,int_param,dbquery});

        // use getIntColumn for Oracle count data
        if ("oracle".equals(ConfigurationManager.getProperty("db.name")))
        {
            count = Long.valueOf(row.getIntColumn("conceptcount"));
        }
        else  //getLongColumn works for postgres
        {
            count = Long.valueOf(row.getLongColumn("conceptcount"));
        }

        return count.intValue();
    }


    public static Scheme2Concept findBySchemeAndConcept(Context context,Integer scheme_id,Integer concept_id)
            throws SQLException, AuthorizeException
    {
        if (scheme_id == null||concept_id == null||scheme_id<0||concept_id<0)
        {
            return null;
        }

        Object[] paramArr = new Object[] {scheme_id,concept_id};
        // All email addresses are stored as lowercase, so ensure that the email address is lowercased for the lookup
        TableRowIterator rows = DatabaseManager.query(context,
                "SELECT * FROM scheme2concept WHERE scheme_id = ? AND concept_id = ? ", paramArr);

        try
        {
            List<TableRow> scheme2ConceptsRows = rows.toList();
            Scheme2Concept[] scheme2Concepts = new Scheme2Concept[scheme2ConceptsRows.size()];

            for (int i = 0; i < scheme2ConceptsRows.size(); i++)
            {
                TableRow row = (TableRow) scheme2ConceptsRows.get(i);

                // First check the cache
                Scheme2Concept fromCache = (Scheme2Concept) context.fromCache(Scheme2Concept.class, row
                        .getIntColumn("id"));

                if (fromCache != null)
                {
                    scheme2Concepts[i] = fromCache;
                }
                else
                {
                    scheme2Concepts[i] = new Scheme2Concept(row);
                }
            }

            return scheme2Concepts[0];
        }
        finally
        {
            if (rows != null)
            {
                rows.close();
            }
        }
    }


    public Scheme2Concept[] findBySchemeAndRole(Context context,Integer scheme_id,Integer role_id)
            throws SQLException, AuthorizeException
    {
        if (scheme_id == null||role_id == null||scheme_id<0||role_id<0)
        {
            return null;
        }

        Object[] paramArr = new Object[] {scheme_id,role_id};
        // All email addresses are stored as lowercase, so ensure that the email address is lowercased for the lookup
        TableRowIterator rows = DatabaseManager.query(context,
                "SELECT * FROM scheme2concept WHERE concpet_id = ? AND role_id = ? ", paramArr);

        try
        {
            List<TableRow> scheme2ConceptsRows = rows.toList();
            Scheme2Concept[] scheme2Concepts = new Scheme2Concept[scheme2ConceptsRows.size()];

            for (int i = 0; i < scheme2ConceptsRows.size(); i++)
            {
                TableRow row = (TableRow) scheme2ConceptsRows.get(i);

                // First check the cache
                Scheme2Concept fromCache = (Scheme2Concept) context.fromCache(Scheme2Concept.class, row
                        .getIntColumn("id"));

                if (fromCache != null)
                {
                    scheme2Concepts[i] = fromCache;
                }
                else
                {
                    scheme2Concepts[i] = new Scheme2Concept(row);
                }
            }

            return scheme2Concepts;
        }
        finally
        {
            if (rows != null)
            {
                rows.close();
            }
        }

    }
    public Scheme2Concept[] findByConceptAndRole(Context context,Integer concept_id,Integer role_id)
            throws SQLException, AuthorizeException
    {
        if (concept_id == null||role_id == null||concept_id<0||role_id<0)
        {
            return null;
        }

        Object[] paramArr = new Object[] {scheme_id,concept_id};
        // All email addresses are stored as lowercase, so ensure that the email address is lowercased for the lookup
        TableRowIterator rows = DatabaseManager.query(context,
                "SELECT * FROM scheme2concept WHERE concept_id = ? AND role_id = ? ", paramArr);

        try
        {
            List<TableRow> scheme2ConceptsRows = rows.toList();
            Scheme2Concept[] scheme2Concepts = new Scheme2Concept[scheme2ConceptsRows.size()];

            for (int i = 0; i < scheme2ConceptsRows.size(); i++)
            {
                TableRow row = (TableRow) scheme2ConceptsRows.get(i);

                // First check the cache
                Scheme2Concept fromCache = (Scheme2Concept) context.fromCache(Scheme2Concept.class, row
                        .getIntColumn("id"));

                if (fromCache != null)
                {
                    scheme2Concepts[i] = fromCache;
                }
                else
                {
                    scheme2Concepts[i] = new Scheme2Concept(row);
                }
            }

            return scheme2Concepts;
        }
        finally
        {
            if (rows != null)
            {
                rows.close();
            }
        }
    }

    public Scheme2Concept[] findByConcept(Context context,Integer concept_id)
            throws SQLException, AuthorizeException
    {
        if (concept_id == null||concept_id<0)
        {
            return null;
        }

        Object[] paramArr = new Object[] {concept_id};
        // All email addresses are stored as lowercase, so ensure that the email address is lowercased for the lookup
        TableRowIterator rows = DatabaseManager.query(context,
                "SELECT * FROM scheme2concept WHERE concept_id = ?", paramArr);

        try
        {
            List<TableRow> scheme2ConceptsRows = rows.toList();
            Scheme2Concept[] scheme2Concepts = new Scheme2Concept[scheme2ConceptsRows.size()];

            for (int i = 0; i < scheme2ConceptsRows.size(); i++)
            {
                TableRow row = (TableRow) scheme2ConceptsRows.get(i);

                // First check the cache
                Scheme2Concept fromCache = (Scheme2Concept) context.fromCache(Scheme2Concept.class, row
                        .getIntColumn("id"));

                if (fromCache != null)
                {
                    scheme2Concepts[i] = fromCache;
                }
                else
                {
                    scheme2Concepts[i] = new Scheme2Concept(row);
                }
            }

            return scheme2Concepts;
        }
        finally
        {
            if (rows != null)
            {
                rows.close();
            }
        }
    }

    public Scheme2Concept[] findByScheme(Context context,Integer concept_id)
            throws SQLException, AuthorizeException
    {
        if (concept_id == null||concept_id<0)
        {
            return null;
        }

        Object[] paramArr = new Object[] {scheme_id};
        // All email addresses are stored as lowercase, so ensure that the email address is lowercased for the lookup
        TableRowIterator rows = DatabaseManager.query(context,
                "SELECT * FROM scheme2concept WHERE concept_id = ? ", paramArr);

        try
        {
            List<TableRow> scheme2ConceptsRows = rows.toList();
            Scheme2Concept[] scheme2Concepts = new Scheme2Concept[scheme2ConceptsRows.size()];

            for (int i = 0; i < scheme2ConceptsRows.size(); i++)
            {
                TableRow row = (TableRow) scheme2ConceptsRows.get(i);

                // First check the cache
                Scheme2Concept fromCache = (Scheme2Concept) context.fromCache(Scheme2Concept.class, row
                        .getIntColumn("id"));

                if (fromCache != null)
                {
                    scheme2Concepts[i] = fromCache;
                }
                else
                {
                    scheme2Concepts[i] = new Scheme2Concept(row);
                }
            }

            return scheme2Concepts;
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
