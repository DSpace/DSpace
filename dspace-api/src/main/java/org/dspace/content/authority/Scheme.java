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
import org.dspace.core.*;
import org.dspace.event.Event;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Class representing a DSpace Scheme, that acts as
 * a container of DSpace Concepts.
 *
 * @author Lantian Gai, Mark Diggory
 */
public class Scheme extends AuthorityObject
{
    public static String TABLE = "scheme";

    /** log4j logger */
    private static Logger log = Logger.getLogger(Scheme.class);

    /**
     * Construct a Scheme from a given context and tablerow
     *
     * @param context
     * @param row
     */
    Scheme(Context context, TableRow row) throws SQLException
    {
        super(context, row);
    }

    @Override
    public String getMetadataTable() {
        return "SchemeMetadataValue";
    }

    /**
     * Create a new metadata scheme
     *
     * @param context
     *            DSpace context object
     */
    public static Scheme create(Context context) throws SQLException,
            AuthorizeException
    {
        return create(context, AuthorityObject.createIdentifier());
    }
    /**
     * Create a new metadata scheme
     *
     * @param context
     *            DSpace context object
     */
    public static Scheme create(Context context,String identifier) throws SQLException,
            AuthorizeException
    {
        // authorized?
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "You must be an admin to create an Metadata Scheme");
        }

        // Create a table row
        TableRow row = DatabaseManager.create(context, TABLE);

        Scheme e = new Scheme(context, row);

        e.setIdentifier(context, identifier);
        log.info(LogManager.getHeader(context, "create_scheme", "metadata_scheme_id="
                + e.getID()));
        return e;
    }
    /**
     * get the ID of the scheme object
     *
     * @return id
     */
    public int getID()
    {
        return myRow.getIntColumn("id");
    }


    /**
     * find the scheme by its ID
     *
     * @param context
     * @param id
     */
    public static Scheme find(Context context, int id) throws SQLException
    {
        // First check the cache
        Scheme fromCache = (Scheme) context.fromCache(Scheme.class, id);

        if (fromCache != null)
        {
            return fromCache;
        }

        TableRow row = DatabaseManager.find(context, TABLE, id);

        if (row == null)
        {
            return null;
        }
        else
        {
            return new Scheme(context, row);
        }
    }

    /**
     * Find the scheme by its name - assumes name is unique
     *
     * @param context
     * @param identifier
     *
     * @return the named Scheme, or null if not found
     */
    public static Scheme findByIdentifier(Context context, String identifier)
            throws SQLException
    {
        TableRowIterator iterator = DatabaseManager.queryTable(context, TABLE, "select * from scheme where identifier = '" + identifier + "'");

        if (!iterator.hasNext())
        {
            return null;
        }
        else
        {
            TableRow row = iterator.next();
            // First check the cache
            Scheme fromCache = (Scheme) context.fromCache(Scheme.class, row.getIntColumn("id"));

            if (fromCache != null)
            {
                return fromCache;
            }
            else
            {
                return new Scheme(context,row);
            }
        }
    }

    /**
     * Finds all schemes in the site
     *
     * @param context
     *            DSpace context
     * @param sortField
     *            field to sort by -- Scheme.ID or Scheme.NAME
     *
     * @return array of all schemes in the site
     */
    public static Scheme[] findAll(Context context, int sortField)
            throws SQLException
    {
        String s;

        switch (sortField)
        {
            case ID:
                s = "concept_scheme_id";

                break;

            case NAME:
                s = "name";

                break;

            default:
                s = "name";
        }

        // NOTE: The use of 's' in the order by clause can not cause an SQL
        // injection because the string is derived from constant values above.
        TableRowIterator rows = DatabaseManager.queryTable(
                context, TABLE,
                "SELECT * FROM scheme ORDER BY " + s);

        try
        {
            List<TableRow> gRows = rows.toList();

            Scheme[] schemes = new Scheme[gRows.size()];

            for (int i = 0; i < gRows.size(); i++)
            {
                TableRow row = gRows.get(i);

                // First check the cache
                Scheme fromCache = (Scheme) context.fromCache(Scheme.class, row
                        .getIntColumn("id"));

                if (fromCache != null)
                {
                    schemes[i] = fromCache;
                }
                else
                {
                    schemes[i] = new Scheme(context, row);
                }
            }

            return schemes;
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
     * Find the schemes that match the search query across concept_scheme_id or name
     *
     * @param context
     *            DSpace context
     * @param query
     *            The search string
     *
     * @return array of Scheme objects
     */
    public static Scheme[] search(Context context, String query)
            throws SQLException
    {
        return search(context, query, -1, -1);
    }

    /**
     * Find the schemes that match the search query across concept_scheme_id or name
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
     * @return array of Scheme objects
     */
    public static Scheme[] search(Context context, String query, int offset, int limit)
            throws SQLException
    {
        String params = "%"+query.toLowerCase()+"%";
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append("SELECT * FROM scheme WHERE id = ? OR ");
        queryBuf.append("LOWER(identifier) like ? ORDER BY id, created ASC ");

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

        // When checking against the concept-id, make sure the query can be made into a number
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
            paramArr = new Object[]{int_param, params,limit, offset};
        }
        else if (limit > 0)
        {
            paramArr = new Object[]{int_param,params,  limit};
        }
        else if (offset > 0)
        {
            paramArr = new Object[]{int_param,params,  offset};
        }

        TableRowIterator rows = DatabaseManager.queryTable(context, TABLE, dbquery, paramArr);

        try
        {
            List<TableRow> schemeRows = rows.toList();
            Scheme[] schemes = new Scheme[schemeRows.size()];

            for (int i = 0; i < schemeRows.size(); i++)
            {
                TableRow row = schemeRows.get(i);
                schemes[i] = new Scheme(context, row);
            }
            return schemes;
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
     * Returns the total number of schemes returned by a specific query, without the overhead
     * of creating the Scheme objects to store the results.
     *
     * @param context
     *            DSpace context
     * @param query
     *            The search string
     *
     * @return the number of schemes matching the query
     */
    public static int searchResultCount(Context context, String query)
            throws SQLException
    {
        String dbquery = "%"+query.toLowerCase()+"%";
        Long count;

        // When checking against the scheme-id, make sure the query can be made into a number
        Integer int_param;
        try {
            int_param = Integer.valueOf(query);
        }
        catch (NumberFormatException e) {
            int_param = Integer.valueOf(-1);
        }

        // Get all the schemes that match the query
        TableRow row = DatabaseManager.querySingle(context,
                "SELECT count(*) as schemecount FROM scheme WHERE id = ? OR " +
                        "LOWER(identifier) like ?",
                new Object[] {int_param,dbquery});

        // use getIntColumn for Oracle count data
        if ("oracle".equals(ConfigurationManager.getProperty("db.name")))
        {
            count = Long.valueOf(row.getIntColumn("schemecount"));
        }
        else  //getLongColumn works for postgres
        {
            count = Long.valueOf(row.getLongColumn("schemecount"));
        }

        return count.intValue();
    }

    /* Delete data specific to schemes
     */
    @Override
    protected void deleteAssociatedData(Context context) throws SQLException, AuthorizeException {
        Concept[] concepts = getConcepts();
        for(Concept concept : concepts) {
            concept.delete(context);
        }
    }



    /**
     * Return <code>true</code> if <code>other</code> is the same Scheme as
     * this object, <code>false</code> otherwise
     *
     * @param obj
     *            object to compare to
     *
     * @return <code>true</code> if object passed in represents the same scheme
     *         as this object
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
        final Scheme other = (Scheme) obj;
        if(this.getID() != other.getID())
        {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 59 * hash + (this.myRow != null ? this.myRow.hashCode() : 0);
        return hash;
    }



    public int getType()
    {
        return Constants.SCHEME;
    }

    public String getHandle()
    {
        return null;
    }

    @Override
    public String getName() {
        return this.getMetadata("dc","title",null,"*") + "(" + this.getIdentifier() + ")";
    }

    public Concept[] getConcepts() throws SQLException {
        Context context = getContext();
        Concept[] concepts = getConcepts(context);
        try {
            completeContext(context);
        } catch (SQLException e) {
            abortContext(context);
        }
        return concepts;
    }

    public Concept[] getConcepts(Context context) throws SQLException
    {
        List<Concept> concepts = new ArrayList<Concept>();

        // Get the table rows
        TableRowIterator tri = DatabaseManager.queryTable(
                context, "concept",
                "SELECT concept.* FROM concept, scheme2concept WHERE " +
                        "scheme2concept.concept_id=concept.id " +
                        "AND scheme2concept.scheme_id= ? ORDER BY concept.id",
                getID());

        // Make Concept objects
        try {
            while (tri.hasNext()) {
                TableRow row = tri.next();
                concepts.add(new Concept(context, row));
            }
        }
        finally {
            // close the TableRowIterator to free up resources
            if (tri != null) {
                tri.close();
            }
        }

        // Put them in an array
        Concept[] conceptArray = new Concept[concepts.size()];
        conceptArray = (Concept[]) concepts.toArray(conceptArray);

        return conceptArray;
    }

    public String getAttribute(String field)
    {
        String metadata = myRow.getStringColumn(field);
        return (metadata == null) ? "" : metadata;
    }



    /**
     * Add an exisiting collection to the community
     *
     * @param c
     *            collection to add
     */
    public void addConcept(Context context, Concept c) throws SQLException, AuthorizeException {
        // authorized?
        if (!AuthorizeManager.isAdmin(context)) {
            throw new AuthorizeException("You must be an admin to add a Concept");
        }
        // Find out if mapping exists
        TableRowIterator tri = DatabaseManager.queryTable(context,
                "scheme2concept",
                "SELECT * FROM scheme2concept WHERE " +
                        "scheme_id= ? AND concept_id= ? ",getID(),c.getID());

        try
        {
            if (!tri.hasNext())
            {
                // No existing mapping, so add one
                TableRow mappingRow = DatabaseManager.row("scheme2concept");

                mappingRow.setColumn("scheme_id", getID());
                mappingRow.setColumn("concept_id", c.getID());

                DatabaseManager.insert(context, mappingRow);
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
    public void removeConcept(Context context, Concept c) throws SQLException, AuthorizeException {
        // authorized?
        if (!AuthorizeManager.isAdmin(context)) {
            throw new AuthorizeException("You must be an admin to modify a Concept's Parent Concept");
        }

        log.info(LogManager.getHeader(context, "add_concept",
                "scheme_id=" + getID() + ",concept_id=" + c.getID()));

        // Find out if mapping exists
        TableRowIterator tri = DatabaseManager.queryTable(context,
                "scheme2concept",
                "SELECT * FROM scheme2concept WHERE " +
                        "scheme_id= ? AND concept_id= ? ",getID(),c.getID());

        try
        {
            if (!tri.hasNext())
            {
                // No existing mapping, so add one
                TableRow mappingRow = DatabaseManager.row("scheme2concept");

                mappingRow.setColumn("scheme_id", getID());
                mappingRow.setColumn("concept_id", c.getID());

                DatabaseManager.insert(context, mappingRow);

                c.delete(context);
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
    public void removeConcepts(Context context) throws SQLException, AuthorizeException {
        // authorized?
        if (!AuthorizeManager.isAdmin(context)) {
            throw new AuthorizeException("You must be an admin to remove concepts");
        }

        log.info(LogManager.getHeader(context, "remove_concept",
                "scheme_id=" + getID()));

        Concept[] concepts = getConcepts();
        for(Concept concept : concepts) {
            removeConcept(context, concept);
        }
    }


    public Concept createConcept(Context context) throws SQLException, AuthorizeException {
        return createConcept(context, null);
    }

    public Concept createConcept(Context context, String identifier) throws SQLException, AuthorizeException {
        Concept concept = null;

        if (identifier == null) {
            concept = Concept.create(context);
        } else {
            concept = Concept.create(context, identifier);
        }
        Date date = new Date();
        concept.setLastModified(context, date);
        concept.setCreated(context, date);
        concept.setLang(context, I18nUtil.getDefaultLocale().getLanguage());
        concept.setTopConcept(context, true);
        concept.setStatus(context, Concept.Status.CANDIDATE.name());
        this.addConcept(context, concept);
        context.commit();

        return concept;
    }

}
