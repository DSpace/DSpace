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
 * Object Representing a "Role" or Relationship type
 * Between Concepts.
 *
 * @author Lantian Gai, Mark Diggory
 */
public class Concept2ConceptRole
{
    private int relationID = 0;
    private String role;
    private String label;
    private String scopeNote;

    /** log4j logger */
    private static Logger log = Logger.getLogger(Concept2ConceptRole.class);

    /** The row in the table representing this type */
    private TableRow row;

    // cache of relation by ID (Integer)
    private static Map<Integer, Concept2ConceptRole> id2relation = null;
    public static String relation_hier="hierarchical";
    public static String relation_asso="hierarchical";
    public static String relation_incoming="incoming";
    public static String relation_outgoing="outgoing";



    /**
     * Default constructor.
     */
    public Concept2ConceptRole()
    {
    }

    /**
     * Full constructor for new metadata relation elements.

     * @param scopeNote scope note of the relation
     */
    public Concept2ConceptRole(String role, String label, String scopeNote)
    {
        this.role = role;
        this.label = label;
        this.scopeNote = scopeNote;
    }

    /**
     * Constructor to load the object from the database.
     *
     * @param row database row from which to populate object.
     */
    public Concept2ConceptRole(TableRow row)
    {
        if (row != null)
        {
            this.relationID = row.getIntColumn("id");
            this.role = row.getStringColumn("role");
            this.label = row.getStringColumn("label");
            this.scopeNote = row.getStringColumn("scope_note");
            this.row = row;
        }
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


    /**
     * Get the scope note.
     *
     * @return scope note
     */
    public String getRole()
    {
        return role;
    }

    /**
     * Set the scope note.
     */
    public void setRole(String role)
    {
        this.role = role;
    }
    /**
     * Get the scope note.
     *
     * @return scope note
     */
    public String getLabel()
    {
        return label;
    }

    /**
     * Set the scope note.
     *
     */
    public void setLabel(String label)
    {
        this.label = label;
    }
    /**
     * Get the scope note.
     *
     * @return scope note
     */
    public String getScopeNote()
    {
        return scopeNote;
    }

    /**
     * Set the scope note.
     *
     * @param scopeNote new value for scope note
     */
    public void setScopeNote(String scopeNote)
    {
        this.scopeNote = scopeNote;
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
    public void create(Context context) throws IOException, AuthorizeException,
            SQLException, NonUniqueMetadataException
    {
        // Check authorisation: Only admins may create DC types
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "Only administrators may modify the concept registry");
        }

        // Ensure the element and qualifier are unique within a given schema.
        if (!unique(context, role, label))
        {
            throw new NonUniqueMetadataException("Please make " + role + "."
                    + label + " unique within concept2term metadata role");
        }

        // Create a table row and update it with the values
        row = DatabaseManager.row("Concept2ConceptRole");
        row.setColumn("role", role);
        row.setColumn("label", label);
        row.setColumn("scope_note", scopeNote);
        DatabaseManager.insert(context, row);
        decache();

        // Remember the new row number
        this.relationID = row.getIntColumn("id");

        log.info(LogManager.getHeader(context, "create_metadata_relation",
                "id=" + row.getIntColumn("id")));
    }

    /**
     * Retrieves the metadata relation from the database.
     *
     * @param context dspace context


     * @return recalled metadata relation
     * @throws java.sql.SQLException
     * @throws org.dspace.authorize.AuthorizeException
     */
    public static Concept2ConceptRole findByRole(Context context,
                                                 String role, String label) throws SQLException,
            AuthorizeException
    {
        // Grab rows from DB
        TableRowIterator tri;
        if (label == null)
        {
            tri = DatabaseManager.queryTable(context,"Concept2ConceptRole",
                    "SELECT * FROM Concept2ConceptRole WHERE " +
                            "role= ?  AND label is NULL ",
                    role);
        }
        else
        {
            tri = DatabaseManager.queryTable(context,"Concept2ConceptRole",
                    "SELECT * FROM Concept2ConceptRole " +
                            "role= ?  AND label= ? ",
                    role, label);
        }

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
            return new Concept2ConceptRole(row);
        }
    }

    /**
     * Retrieve all Dublin Core types from the registry
     *
     * @param context dspace context
     * @return an array of all the Dublin Core types
     * @throws java.sql.SQLException
     */
    public static Concept2ConceptRole[] findAll(Context context) throws SQLException
    {
        List<Concept2ConceptRole> relations = new ArrayList<Concept2ConceptRole>();

        // Get all the metadatarelationregistry rows
        TableRowIterator tri = DatabaseManager.queryTable(context, "Concept2ConceptRole",
                "SELECT mfr.* FROM Concept2ConceptRole mfr ORDER BY mfr.role, mfr.label");

        try
        {
            // Make into DC Type objects
            while (tri.hasNext())
            {
                relations.add(new Concept2ConceptRole(tri.next()));
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
        Concept2ConceptRole[] typeArray = new Concept2ConceptRole[relations.size()];
        return (Concept2ConceptRole[]) relations.toArray(typeArray);
    }

    /**
     * Return all metadata relations that are found in a given schema.
     *
     * @param context dspace context
     * @param schemaID schema by db ID
     * @return array of metadata relations
     * @throws java.sql.SQLException
     */
    public static Concept2ConceptRole[] findAllInSchema(Context context, int schemaID)
            throws SQLException
    {
        List<Concept2ConceptRole> relations = new ArrayList<Concept2ConceptRole>();

        // Get all the metadatarelationregistry rows
        TableRowIterator tri = DatabaseManager.queryTable(context,"Concept2ConceptRole",
                "SELECT * FROM Concept2ConceptRole " +
                        " ORDER BY role, label", schemaID);

        try
        {
            // Make into DC Type objects
            while (tri.hasNext())
            {
                relations.add(new Concept2ConceptRole(tri.next()));
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
        Concept2ConceptRole[] typeArray = new Concept2ConceptRole[relations.size()];
        return (Concept2ConceptRole[]) relations.toArray(typeArray);
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

        if (Concept2ConceptRole.hasElement(context, role, label))
        {
            throw new NonUniqueMetadataException(
                    "Duplcate relation name found in target schema");
        }


        // Ensure the element and qualifier are unique within a given schema.
        if (!unique(context, role, label))
        {
            throw new NonUniqueMetadataException("Please make " + role + "."
                    + label);
        }

        row.setColumn("role", role);
        row.setColumn("label", label);
        row.setColumn("scope_note", scopeNote);
        DatabaseManager.update(context, row);
        decache();

        log.info(LogManager.getHeader(context, "update_Concept2ConceptRole",
                "id=" + getRelationID() + "role=" + getRole()
                        + "label=" + getLabel()));
    }

    /**
     * Return true if and only if the schema has a relation with the given element
     * and qualifier pair.
     *
     * @param context dspace context

     * @return true if the relation exists
     * @throws java.sql.SQLException
     * @throws org.dspace.authorize.AuthorizeException
     */
    private static boolean hasElement(Context context,
                                      String role, String label) throws SQLException,
            AuthorizeException
    {
        return Concept2ConceptRole.findByRole(context, role,
                label) != null;
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
                    "Only administrators may modify the Concept2ConceptRole registry");
        }

        log.info(LogManager.getHeader(context, "delete_Concept2ConceptRole",
                "id=" + getRelationID()));

        DatabaseManager.delete(context, row);
        decache();
    }

    /**
     * A sanity check that ensures a given element and qualifier are unique
     * within a given schema. The check happens in code as we cannot use a
     * database constraint.
     *
     * @param context dspace context

     * @return true if unique
     * @throws org.dspace.authorize.AuthorizeException
     * @throws java.sql.SQLException
     * @throws java.io.IOException
     */
    private boolean unique(Context context, String role,
                           String label) throws IOException, SQLException,
            AuthorizeException
    {
        int count = 0;
        Connection con = null;
        PreparedStatement statement = null;
        ResultSet rs = null;

        try
        {
            con = context.getDBConnection();
            TableRow reg = DatabaseManager.row("Concept2ConceptRole");

            String qualifierClause = "";

            if (label == null)
            {
                qualifierClause = "and label is null";
            }
            else
            {
                qualifierClause = "and label = ?";
            }

            String query = "SELECT COUNT(*) FROM " + reg.getTable()
                    + " and id != ? "
                    + " and role= ? " + qualifierClause;

            statement = con.prepareStatement(query);
            statement.setInt(1,relationID);
            statement.setString(2,role);

            if (label != null)
            {
                statement.setString(3,label);
            }

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
     * @param
     * @return HTML FORM key
     */
    public static String formKey(String role)
    {
        return role;
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
    public static Concept2ConceptRole find(Context context, int id)
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
            Map<Integer, Concept2ConceptRole> new_id2relation = new HashMap<Integer, Concept2ConceptRole>();
            log.info("Loading Concept2ConceptRole elements into cache.");

            // Grab rows from DB
            TableRowIterator tri = DatabaseManager.queryTable(context,"Concept2ConceptRole",
                    "SELECT * from Concept2ConceptRole");

            try
            {
                while (tri.hasNext())
                {
                    TableRow row = tri.next();
                    int relationID = row.getIntColumn("id");
                    new_id2relation.put(Integer.valueOf(relationID), new Concept2ConceptRole(row));
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
        if (this.relationID != other.getRelationID())
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
}
