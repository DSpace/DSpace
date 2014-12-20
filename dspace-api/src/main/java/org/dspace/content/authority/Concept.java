/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import org.apache.log4j.Logger;
import org.dspace.authority.AuthorityValue;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.event.Event;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Class representing a DSpace Concept, that acts as
 * a container of DSpace Terms.
 *
 * @author Lantian Gai, Mark Diggory
 */
public class Concept extends AuthorityObject
{


    public enum Status {
        CANDIDATE,
        ACCEPTED,
        WITHDRAWN;
    }

    /** log4j logger */
    private static Logger log = Logger.getLogger(Concept.class);


    /**
     * Construct a Concept from a given context and tablerow
     *
     * @param context
     * @param row
     */
    Concept(Context context, TableRow row) throws SQLException
    {
        super(context,row);

    }

    public Date getCreated()
    {
        Date myDate = myRow.getDateColumn("created");

        if (myDate == null)
        {
            myDate = new Date();
        }

        return myDate;
    }
    public void setCreated(Date date)
    {
        Date myDate = myRow.getDateColumn("created");

        if (date != null)
        {
            myRow.setColumn("created", date);
            modified = true;
        }
    }

    public String getStatus()
    {
        return myRow.getStringColumn("status");

    }

    public void setStatus(String status)
    {
        try
        {
            Status c = Status.valueOf(status);
            if (!c.name().equals(getStatus()) ) {
                myRow.setColumn("status", c.name());
                modified = true;
            }
        }
        catch(Exception e)
        {
            if(status == null && getStatus() != null)
            {
                myRow.setColumnNull("status");
                modified = true;
            }
        }

    }

    public void setStatus(Status status)
    {
        if(status == null)
        {
            if(getStatus() != null)
            {
                myRow.setColumnNull("status");
                modified = true;
            }
        }
        else if (!status.name().equals(getStatus()))
        {
            myRow.setColumn("status", status.name());
            modified = true;
        }
    }

    public String getSource()
    {
        return myRow.getStringColumn("source");

    }
    public void setSource(String source)
    {
        myRow.setColumn("source", source);
        modified = true;
    }

    public String getLang()
    {
        return myRow.getStringColumn("lang");

    }
    public void setLang(String lang)
    {
        myRow.setColumn("lang", lang);
        modified = true;
    }



    public Boolean getTopConcept()
    {
        return myRow.getBooleanColumn("topConcept");

    }
    public void setTopConcept(Boolean topConcept)
    {
        myRow.setColumn("topConcept", topConcept);
        modified = true;
    }
    /**
     * Create a new concept
     *
     * @param context
     *            DSpace context object
     */
    public static Concept create(Context context) throws SQLException,
            AuthorizeException
    {
        // authorized?
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "You must be an admin to create an Concept");
        }

        return create(context, AuthorityObject.createIdentifier());
    }

    public static Concept create(Context context, String identifier) throws SQLException,
            AuthorizeException
    {
        // authorized?
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "You must be an admin to create an Concept");
        }

        // Create a table row
        TableRow row = DatabaseManager.create(context, "concept");

        Concept e = new Concept(context, row);

        e.setIdentifier(identifier);

        log.info(LogManager.getHeader(context, "create_concept", "metadata_concept_id="
                + e.getID()));

        context.addEvent(new Event(Event.CREATE, Constants.CONCEPT, e.getID(), null));

        return e;
    }
    /**
     * get the ID of the concept object
     *
     * @return id
     */
    public int getID()
    {
        return myRow.getIntColumn("id");
    }

    /**
     * add an term member
     *
     * @param t
     *            term
     */
    public void addPreferredTerm(Term t)
    {


        addTermByType(t,1);

    }
    /**
     * add an term member
     *
     * @param t
     *            term
     */
    public void addAltTerm(Term t)
    {

        addTermByType(t,2);

    }
    /**
     * remove an term from a concept
     *
     * @param t
     *            term
     */
    public void removePreferredTerm(Term t)throws SQLException,
            AuthorizeException, IOException
    {

        // Check authorisation
        AuthorizeManager.isAdmin(myContext);
        log.info(LogManager.getHeader(myContext, "remove_preferredTerm",
                "concept_id=" + getID() + ",term_id=" + t.getID()));

        TableRow trow = DatabaseManager.querySingle(myContext,
                "SELECT COUNT(DISTINCT concept_id) AS num FROM concept2term WHERE term_id= ? AND role_id=1",
                t.getID());
        DatabaseManager.setConstraintDeferred(myContext, "concept2term_term_id_fkey");

        if (trow.getLongColumn("num") == 1)
        {
            // Orphan; delete it
            t.delete();
        }

        log.info(LogManager.getHeader(myContext, "remove_term",
                "concept_id=" + getID() + ",term_id=" + t.getID()));

        // Remove any mappings
        DatabaseManager.updateQuery(myContext,
                "DELETE FROM concept2term WHERE concept_id= ? "+
                        "AND term_id= ? AND role_id=1", getID(), t.getID());

        DatabaseManager.setConstraintImmediate(myContext, "concept2term_term_id_fkey");
        myContext.addEvent(new Event(Event.REMOVE, Constants.TERM, t.getID(), null));
    }

    /**
     * remove an term from a concept
     *
     * @param t
     *            term
     */
    public void removeAltTerm(Term t)throws SQLException,
            AuthorizeException, IOException
    {
        // Check authorisation
        AuthorizeManager.isAdmin(myContext);
        log.info(LogManager.getHeader(myContext, "remove_altTerm",
                "concept_id=" + getID() + ",term_id=" + t.getID()));

        TableRow trow = DatabaseManager.querySingle(myContext,
                "SELECT COUNT(DISTINCT concept_id) AS num FROM concept2term WHERE term_id= ? AND role_id=2",
                t.getID());
        DatabaseManager.setConstraintDeferred(myContext, "concept2term_term_id_fkey");

        if (trow.getLongColumn("num") == 1)
        {
            // Orphan; delete it
            t.delete();
        }

        log.info(LogManager.getHeader(myContext, "remove_term",
                "concept_id=" + getID() + ",term_id=" + t.getID()));

        // Remove any mappings
        DatabaseManager.updateQuery(myContext,
                "DELETE FROM concept2term WHERE concept_id= ? "+
                        "AND term_id= ? AND role_id=2", getID(), t.getID());

        DatabaseManager.setConstraintImmediate(myContext, "concept2term_term_id_fkey");
        myContext.addEvent(new Event(Event.REMOVE, Constants.TERM, t.getID(), null));
    }
    /**
     * remove concept from this concept
     *
     * @param c
     */
    public void removeParentConcept(Concept c)throws SQLException,
            AuthorizeException, IOException
    {
        // Check authorisation
        AuthorizeManager.isAdmin(myContext);
        log.info(LogManager.getHeader(myContext, "remove_parentConcept",
                "concept_id=" + getID() + ",parent_concept_id=" + c.getID()));

        DatabaseManager.setConstraintDeferred(myContext, "Concept2Concept_incoming_id_fkey ");

        log.info(LogManager.getHeader(myContext, "remove_term",
                "concept_id=" + getID() + ",parent_concept_id=" + c.getID()));

        // Remove any mappings
        DatabaseManager.updateQuery(myContext,
                "DELETE FROM concept2term WHERE imcoming_id= ? "+
                        "AND outgoing_id= ? AND role_id=1", getID(), c.getID());

        DatabaseManager.setConstraintImmediate(myContext, "Concept2Concept_incoming_id_fkey ");
        myContext.addEvent(new Event(Event.REMOVE, Constants.CONCEPT, c.getID(), null));
    }

    /**
     * remove concept from this concept
     *
     * @param c
     */
    public void removeChildConcept(Concept c)throws SQLException,
            AuthorizeException, IOException
    {
        // Check authorisation
        AuthorizeManager.isAdmin(myContext);
        log.info(LogManager.getHeader(myContext, "remove_parentConcept",
                "concept_id=" + getID() + ",child_concept_id=" + c.getID()));

        DatabaseManager.setConstraintDeferred(myContext, "Concept2Concept_outgoing_id_fkey ");

        log.info(LogManager.getHeader(myContext, "remove_term",
                "concept_id=" + getID() + ",child_concept_id=" + c.getID()));

        // Remove any mappings
        DatabaseManager.updateQuery(myContext,
                "DELETE FROM concept2term WHERE outgoing_id= ? "+
                        "AND incoming_id= ? AND role_id=1", getID(), c.getID());

        DatabaseManager.setConstraintImmediate(myContext, "Concept2Concept_outgoing_id_fkey ");
        myContext.addEvent(new Event(Event.REMOVE, Constants.CONCEPT, c.getID(), null));
    }

    /**
     * fast check to see if an term is a member called with term id, does
     * database lookup without instantiating all of the term objects and is
     * thus a static method
     *
     * @param c
     *            context
     * @param conceptid
     *            concept ID to check
     */


    /**
     * find the concept by its ID
     *
     * @param context
     * @param id
     */
    public static Concept find(Context context, int id) throws SQLException
    {
        // First check the cache
        Concept fromCache = (Concept) context.fromCache(Concept.class, id);

        if (fromCache != null)
        {
            return fromCache;
        }

        TableRow row = DatabaseManager.find(context, "concept", id);

        if (row == null)
        {
            return null;
        }
        else
        {
            return new Concept(context, row);
        }
    }

    /**
     * Find the concept by its name - assumes name is unique
     *
     * @param context
     * @param identifier
     *
     * @return the named Concept, or null if not found
     */

    public static ArrayList<Concept> findByIdentifier(Context context, String identifier)
            throws SQLException
    {
        ArrayList<Concept> concepts = new ArrayList<Concept>();
        TableRowIterator row = DatabaseManager.query(context,"select * from concept where LOWER(identifier) like '"+identifier+"'");

        if (row == null)
        {
            return null;
        }
        else
        {

            while(row.hasNext())
            {
                concepts.add(new Concept(context,row.next()));

            }
        }
        return concepts;
    }
    /**
     * Finds all concepts in the site
     *
     * @param context
     *            DSpace context
     * @param sortField
     *            field to sort by -- Concept.ID or Concept.NAME
     *
     * @return array of all concepts in the site
     */
    public static Concept[] findAll(Context context, int sortField)
            throws SQLException
    {
        String s;

        switch (sortField)
        {
            case ID:
                s = "id";

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
                context, "concept",
                "SELECT * FROM concept ORDER BY "+s);

        try
        {
            List<TableRow> gRows = rows.toList();

            Concept[] concepts = new Concept[gRows.size()];

            for (int i = 0; i < gRows.size(); i++)
            {
                TableRow row = gRows.get(i);

                // First check the cache
                Concept fromCache = (Concept) context.fromCache(Concept.class, row
                        .getIntColumn("id"));

                if (fromCache != null)
                {
                    concepts[i] = fromCache;
                }
                else
                {
                    concepts[i] = new Concept(context, row);
                }
            }

            return concepts;
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
     * Find the concepts that match the search query across term_concept_id or name
     *
     * @param context
     *            DSpace context
     * @param query
     *            The search string
     *
     * @return array of Concept objects
     */
    public static Concept[] search(Context context, String query)
            throws SQLException
    {
        return search(context, query, -1, -1,null);
    }

    /**
     * Find the concepts that match the search query across term_concept_id or name
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
     * @return array of Concept objects
     */
    public static Concept[] search(Context context, String query, int offset, int limit,String schemeId)
            throws SQLException
    {
        String params = "%"+query.toLowerCase()+"%";
        StringBuffer queryBuf = new StringBuffer();

        if(schemeId==null){
            queryBuf.append("SELECT distinct(concept.*) FROM concept LEFT JOIN concept2term ON concept.id=concept2term.concept_id LEFT JOIN term on concept2term.concept_id = term.id WHERE concept.id = ? OR " +
                    "LOWER(concept.identifier) like ? OR LOWER(term.literalform) like ? ORDER BY concept.id, concept.created ASC");
        }
        else
        {
            queryBuf.append("SELECT distinct(concept.*) FROM concept LEFT JOIN scheme2concept ON concept.id=scheme2concept.concept_id LEFT JOIN concept2term ON concept.id=concept2term.concept_id LEFT JOIN term on concept2term.term_id = term.id WHERE scheme2concept.scheme_id = "+schemeId+" AND (concept.id = ? OR LOWER(concept.identifier) like ? OR LOWER(term.literalform) like ? ) ORDER BY concept.id ASC");
        }


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

        // When checking against the term-id, make sure the query can be made into a number
        Integer int_param;
        try {
            int_param = Integer.valueOf(query);
        }
        catch (NumberFormatException e) {
            int_param = Integer.valueOf(-1);
        }

        // Create the parameter array, including limit and offset if part of the query
        Object[] paramArr = new Object[] {int_param,params,params};
        if (limit > 0 && offset > 0)
        {
            paramArr = new Object[]{int_param, params,params,limit, offset};
        }
        else if (limit > 0)
        {
            paramArr = new Object[]{int_param,params,params,  limit};
        }
        else if (offset > 0)
        {
            paramArr = new Object[]{int_param,params, params, offset};
        }

        TableRowIterator rows =
                DatabaseManager.query(context, dbquery, paramArr);

        try
        {
            List<TableRow> conceptRows = rows.toList();
            Concept[] concepts = new Concept[conceptRows.size()];

            for (int i = 0; i < conceptRows.size(); i++)
            {
                TableRow row = conceptRows.get(i);

                // First check the cache
                Concept fromCache = (Concept) context.fromCache(Concept.class, row
                        .getIntColumn("id"));

                if (fromCache != null)
                {
                    concepts[i] = fromCache;
                }
                else
                {
                    concepts[i] = new Concept(context, row);
                }
            }
            return concepts;
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
     * Returns the total number of concepts returned by a specific query, without the overhead
     * of creating the Concept objects to store the results.
     *
     * @param context
     *            DSpace context
     * @param query
     *            The search string
     *
     * @return the number of concepts matching the query
     */
    public static int searchResultCount(Context context, String query,String schemeId)
            throws SQLException
    {
        String dbquery = "%"+query.toLowerCase()+"%";
        Long count;

        // When checking against the concept-id, make sure the query can be made into a number
        Integer int_param;
        try {
            int_param = Integer.valueOf(query);
        }
        catch (NumberFormatException e) {
            int_param = Integer.valueOf(-1);
        }
        TableRow row = null;
        if(schemeId==null){
            // Get all the epeople that match the query
            row = DatabaseManager.querySingle(context,
                    "SELECT count(distinct(concept.*)) as conceptcount FROM concept LEFT JOIN concept2term ON concept.id=concept2term.concept_id LEFT JOIN term on concept2term.concept_id = term.id WHERE concept.id = ? OR " +
                            "LOWER(concept.identifier) like ? OR (concept2term.concept_id = concept.id AND concept2term.term_id=term.id AND term.literalform like ?)",
                    new Object[] {int_param,dbquery,dbquery});
        }
        else
        {
            // Get all the epeople that match the query
            row = DatabaseManager.querySingle(context,
                    "SELECT count(distinct(concept.*)) as conceptcount FROM concept LEFT JOIN scheme2concept ON concept.id=scheme2concept.concept_id LEFT JOIN concept2term ON concept.id=concept2term.concept_id LEFT JOIN term on concept2term.term_id = term.id WHERE concept.id = scheme2concept.concept_id AND scheme2concept.scheme_id = "+schemeId+" AND (concept.id = ? OR LOWER(concept.identifier) like ? OR (concept2term.concept_id = concept.id AND concept2term.term_id=term.id AND term.literalform like ?) )",
                    new Object[] {int_param,dbquery,dbquery});
        }



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


    /**
     * Delete a concept
     *
     */
    public void delete() throws SQLException
    {
        // FIXME: authorizations

        // Remove from cache
        myContext.removeCached(this, getID());

        // Remove all metadata
        DatabaseManager.updateQuery(myContext,
                "DELETE FROM ConceptMetadataValue WHERE parent_id= ? ",
                getID());

        //delete term
        Term[] terms = getTerms();
        for(Term term : terms)
        {
            try{
                term.delete();
            }catch (Exception e)
            {
                log.error("can't delete term :" +term.getID()+" for concept :"+getID());
            }
        }
        //remove concept from scheme
        DatabaseManager.updateQuery(myContext,
                "DELETE FROM Scheme2Concept WHERE concept_id= ? ",
                getID());
        //remove relationships
        DatabaseManager.updateQuery(myContext,
                "DELETE FROM Concept2Concept WHERE incoming_id= ?",
                getID());
        DatabaseManager.updateQuery(myContext,
                "DELETE FROM Concept2Concept WHERE outgoing_id= ?",
                getID());
        //delete concept
        DatabaseManager.delete(myContext, myRow);


        myContext.addEvent(new Event(Event.DELETE, Constants.CONCEPT, getID(), getIdentifier()));

        log.info(LogManager.getHeader(myContext, "delete_concept", "concept_id="
                + getID()));
    }

    /**
     * Return Concept members of a Concept.
     */
    public Concept[] getParentConcepts() throws SQLException
    {
        List<Concept> concepts = new ArrayList<Concept>();

        // Get the table rows
        TableRowIterator tri = DatabaseManager.queryTable(
                myContext,"concept",
                "SELECT concept.* FROM concept, Concept2Concept WHERE " +
                        "Concept2Concept.incoming_id=concept.id " +
                        "AND Concept2Concept.outgoing_id= ? ORDER BY concept.id",
                getID());

        // Make Collection objects
        try
        {
            while (tri.hasNext())
            {
                TableRow row = tri.next();

                // First check the cache
                Concept fromCache = (Concept) myContext.fromCache(
                        Concept.class, row.getIntColumn("id"));

                if (fromCache != null)
                {
                    concepts.add(fromCache);
                }
                else
                {
                    concepts.add(new Concept(myContext, row));
                }
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

        // Put them in an array
        Concept[] conceptArray = new Concept[concepts.size()];
        conceptArray = (Concept[]) concepts.toArray(conceptArray);

        return conceptArray;
    }



    /**
     * Return Concept members of a Concept.
     */
    public Concept[] getRelatedConcepts(String relation,String direction,String scheme) throws SQLException
    {
        List<Concept> concepts = new ArrayList<Concept>();
        String query = "SELECT concept.* FROM concept, Concept2Concept, Scheme2Concept,concept2conceptrole,scheme WHERE concept.id = scheme2concept.concept_id ";
        if(direction.equals(Concept2ConceptRole.relation_incoming))
        {
            query = query + " AND Concept2Concept.incoming_id=concept.id " +
                    "AND Concept2Concept.outgoing_id= ? ";
        }
        else if(direction.equals(Concept2ConceptRole.relation_outgoing))
        {
            query = query+" AND Concept2Concept.outgoing_id=concept.id " +
                    "AND Concept2Concept.incoming_id= ? ";
        }
        else
        {
            //find all relationships
            query = query+" AND Concept2Concept.incoming_id = ? " +
                    "OR Concept2Concept.outgoing_id= ? ";
        }


        if(relation!=null&&relation.length()>0)

        {
            query = query + " AND Concept2Concept.role_id=concept2conceptrole.id AND concept2conceptrole.role='" + relation + "' ";
        }

        if(scheme!=null&&scheme.length()>0)

        {
            query = query + " AND scheme.id=scheme2concept.scheme_id AND LOWER(scheme.identifier) = '" + scheme + "' ";
        }

        // Get the table rows
        TableRowIterator tri = DatabaseManager.queryTable(
                myContext, "concept",
                query,
                getID());

        // Make Collection objects
        try
        {
            while (tri.hasNext())
            {
                TableRow row = tri.next();

                // First check the cache
                Concept fromCache = (Concept) myContext.fromCache(
                        Concept.class, row.getIntColumn("id"));

                if (fromCache != null)
                {
                    concepts.add(fromCache);
                }
                else
                {
                    concepts.add(new Concept(myContext, row));
                }
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

        // Put them in an array
        Concept[] conceptArray = new Concept[concepts.size()];
        conceptArray = (Concept[]) concepts.toArray(conceptArray);

        return conceptArray;
    }

    /**
     * Return Concept members of a Concept.
     */
    public Concept[] getChildConcepts() throws SQLException
    {

        List<Concept> concepts = new ArrayList<Concept>();

        // Get the table rows
        TableRowIterator tri = DatabaseManager.queryTable(
                myContext,"concept",
                "SELECT concept.* FROM concept, Concept2Concept WHERE " +
                        "Concept2Concept.outgoing_id=concept.id " +
                        "AND Concept2Concept.incoming_id= ? ORDER BY concept.id",
                getID());

        // Make Collection objects
        try
        {
            while (tri.hasNext())
            {
                TableRow row = tri.next();

                // First check the cache
                Concept fromCache = (Concept) myContext.fromCache(
                        Concept.class, row.getIntColumn("id"));

                if (fromCache != null)
                {
                    concepts.add(fromCache);
                }
                else
                {
                    concepts.add(new Concept(myContext, row));
                }
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

        // Put them in an array
        Concept[] conceptArray = new Concept[concepts.size()];
        conceptArray = (Concept[]) concepts.toArray(conceptArray);

        return conceptArray;
    }

    /**
     * Return <code>true</code> if <code>other</code> is the same Concept as
     * this object, <code>false</code> otherwise
     *
     * @param obj
     *            object to compare to
     *
     * @return <code>true</code> if object passed in represents the same concept
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
        final Concept other = (Concept) obj;
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
        return Constants.CONCEPT;
    }

    public String getHandle()
    {
        return null;
    }

    @Override
    public String getName() {
        try {
            return this.getPreferredLabel() + "(" + this.getIdentifier() + ")";
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return this.getIdentifier();
    }

    public Scheme getScheme() throws SQLException
    {
        // Get the bundle table rows
        TableRowIterator tri = DatabaseManager.queryTable(myContext,"scheme",
                "SELECT scheme.* FROM scheme, scheme2concept WHERE " +
                        "scheme.id=scheme2concept.scheme_id " +
                        "AND scheme2concept.concept_id= ? ",
                getID());

        // Build a list of Scheme objects
        List<Scheme> schemes = new ArrayList<Scheme>();

        try
        {
            while (tri.hasNext())
            {
                TableRow row = tri.next();

                // First check the cache
                Scheme owner = (Scheme) myContext.fromCache(Scheme.class,
                        row.getIntColumn("id"));

                if (owner == null)
                {
                    owner = new Scheme(myContext, row);
                }

                schemes.add(owner);
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

        if(schemes==null||schemes.size() ==0)
        {
            return null;
        }
        return schemes.get(0);
    }


    public Date getLastModified()
    {
        Date myDate = myRow.getDateColumn("modified");

        if (myDate == null)
        {
            myDate = new Date();
        }

        return myDate;
    }
    public void setLastModified(Date date)
    {
        Date myDate = myRow.getDateColumn("modified");

        if (date != null)
        {
            myRow.setColumn("modified", date);
            modified = true;
        }
    }
    /**
     * Method that updates the last modified date of the item
     */
    public void updateLastModified()
    {
        try {
            Date lastModified = new java.sql.Timestamp(new Date().getTime());
            myRow.setColumn("modified", lastModified);
            DatabaseManager.updateQuery(myContext, "UPDATE concept SET modified = ? WHERE id= ? ", lastModified, getID());
            //Also fire a modified event since the item HAS been modified
            //ourContext.addEvent(new Event(Event.MODIFY, Constants.ITEM, getID(), null));
        } catch (SQLException e) {
            log.error(LogManager.getHeader(myContext, "Error while updating modified timestamp", "Concept: " + getID()));
        }
    }

    @Override
    public String getMetadataTable() {
        return "ConceptMetadataValue";
    }

    public Term[] getPreferredTerms() throws SQLException
    {
        List<Term> terms = new ArrayList<Term>();

        // Get the table rows
        TableRowIterator tri = DatabaseManager.queryTable(
                myContext,"term",
                "SELECT term.* FROM term, concept2term WHERE " +
                        "concept2term.term_id=term.id " +
                        "AND concept2term.concept_id= ?  AND role_id = 1 ORDER BY term.identifier",
                getID());

        // Make Concept objects
        try
        {
            while (tri.hasNext())
            {
                TableRow row = tri.next();

                // First check the cache
                Term fromCache = (Term) myContext.fromCache(
                        Term.class, row.getIntColumn("id"));

                if (fromCache != null)
                {
                    terms.add(fromCache);
                }
                else
                {
                    terms.add(new Term(myContext, row));
                }
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

        // Put them in an array
        Term[] termArray = new Term[terms.size()];
        termArray = (Term[]) terms.toArray(termArray);

        return termArray;
    }

    public Term[] getAltTerms() throws SQLException
    {
        List<Term> terms = new ArrayList<Term>();

        // Get the table rows
        TableRowIterator tri = DatabaseManager.queryTable(
                myContext,"term",
                "SELECT term.* FROM term, concept2term WHERE " +
                        "concept2term.term_id=term.id " +
                        "AND concept2term.concept_id= ? AND role_id = 2 ORDER BY term.identifier",
                getID());

        // Make Concept objects
        try
        {
            while (tri.hasNext())
            {
                TableRow row = tri.next();

                // First check the cache
                Term fromCache = (Term) myContext.fromCache(
                        Term.class, row.getIntColumn("id"));

                if (fromCache != null)
                {
                    terms.add(fromCache);
                }
                else
                {
                    terms.add(new Term(myContext, row));
                }
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

        // Put them in an array
        Term[] conceptArray = new Term[terms.size()];
        conceptArray = (Term[]) terms.toArray(conceptArray);

        return conceptArray;
    }

    public void addParentConcept(Concept incoming,int roleId) throws SQLException,
            AuthorizeException
    {

        // Check authorisation
        AuthorizeManager.isAdmin(myContext);

        log.info(LogManager.getHeader(myContext, "add_parentConcept",
                "concept_id=" + getID() + ",parent_concept_id=" + incoming.getID()));

        // Find out if mapping exists
        TableRowIterator tri = DatabaseManager.queryTable(myContext,
                "Concept2Concept",
                "SELECT * FROM Concept2Concept WHERE " +
                        "outgoing_id= ? AND incoming_id= ? ", getID(), incoming.getID());

        try
        {
            if (!tri.hasNext())
            {
                // No existing mapping, so add one
                TableRow mappingRow = DatabaseManager.row("Concept2Concept");

                mappingRow.setColumn("outgoing_id", getID());
                mappingRow.setColumn("incoming_id", incoming.getID());
                mappingRow.setColumn("role_id", roleId);
                DatabaseManager.insert(myContext, mappingRow);
                myContext.addEvent(new Event(Event.ADD, Constants.CONCEPT, incoming.getID(), null));
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

    public void addChildConcept(Concept outgoing, int roleId) throws SQLException,
            AuthorizeException
    {
        // Check authorisation
        AuthorizeManager.isAdmin(myContext);

        log.info(LogManager.getHeader(myContext, "add_childConcept",
                "concept_id=" + getID() + ",child_concept_id=" + outgoing.getID()));

        // Find out if mapping exists
        TableRowIterator tri = DatabaseManager.queryTable(myContext,
                "Concept2Concept",
                "SELECT * FROM Concept2Concept WHERE " +
                        "incoming_id= ? AND outgoing_id= ? ", getID(), outgoing.getID());

        try
        {
            if (!tri.hasNext())
            {
                // No existing mapping, so add one
                TableRow mappingRow = DatabaseManager.row("Concept2Concept");

                mappingRow.setColumn("outgoing_id", outgoing.getID());
                mappingRow.setColumn("incoming_id", getID());
                mappingRow.setColumn("role_id", roleId);
                DatabaseManager.insert(myContext, mappingRow);
                myContext.addEvent(new Event(Event.ADD, Constants.CONCEPT, outgoing.getID(), null));
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
    public String getPreferredLabel() throws SQLException
    {

        Term[] preferTerms = getPreferredTerms();
        if(preferTerms!=null&&preferTerms.length>0)
        {
            return preferTerms[0].getLiteralForm();
        }
        else
        {
            return null;
        }
    }

    //guarentee return a label for concept
    public String getLabel() throws SQLException
    {

        String label = getPreferredLabel();
        if(label==null)
        {
            Term [] terms =getAltTerms();

            if(terms!=null&&terms.length>0)
            {
                label = terms[0].getLiteralForm();
            }

        }
        if(label==null)
        {
            label = getIdentifier();
        }
        return label;
    }

    public void addTerm(Term t,int role_id) throws SQLException,
            AuthorizeException
    {
        // Check authorisation
        AuthorizeManager.isAdmin(myContext);

        log.info(LogManager.getHeader(myContext, "add_term",
                "concept_id=" + getID() + ",term_id=" + t.getID()));

        // Find out if mapping exists
        TableRowIterator tri = DatabaseManager.queryTable(myContext,
                "concept2term",
                "SELECT * FROM concept2term WHERE " +
                        "concept_id= ? AND term_id= ? ",getID(),t.getID());

        try
        {
            if (!tri.hasNext())
            {
                // No existing mapping, so add one
                TableRow mappingRow = DatabaseManager.row("concept2term");

                mappingRow.setColumn("concept_id", getID());
                mappingRow.setColumn("term_id", t.getID());
                mappingRow.setColumn("role_id", role_id);
                DatabaseManager.insert(myContext, mappingRow);
                myContext.addEvent(new Event(Event.ADD, Constants.TERM,t.getID(), null));
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

    public static Concept[] findByPreferredLabel(Context context,String query,int schemeId){
        //make schemeId == -1 to get all match preferred label
        ArrayList<Concept> concepts = new ArrayList<Concept>();
        String dbquery = "SELECT concept.* FROM concept,concept2term,term where concept.id=concept2term.concept_id and concept2term.term_id=term.id and term.literalform = ? order by term.literalform";
        try
        {

            TableRowIterator rows = DatabaseManager.queryTable(context,"Concept", dbquery, query);

            List<TableRow> conceptRows = rows.toList();

            for (int i = 0; i < conceptRows.size(); i++)
            {
                TableRow row = conceptRows.get(i);
                Concept concept = new Concept(context,row);
                if(schemeId==-1||concept.getScheme().getID()==schemeId){
                    concepts.add(new Concept(context, row));
                }

            }
            if (rows != null)
            {
                rows.close();
            }

            // Put them in an array
            Concept[] conceptArray = new Concept[concepts.size()];
            conceptArray = (Concept[]) concepts.toArray(conceptArray);
            return conceptArray;

        }
        catch (Exception e)
        {

            return null;
        }


    }

    public Term[] getTerms() throws SQLException{
        List<Term> terms = new ArrayList<Term>();

        // Get the table rows
        TableRowIterator tri = DatabaseManager.queryTable(
                myContext,"term",
                "SELECT term.* FROM term, concept2term WHERE " +
                        "concept2term.term_id=term.id " +
                        "AND concept2term.concept_id= ?",
                getID());

        // Make Concept objects
        try
        {
            while (tri.hasNext())
            {
                TableRow row = tri.next();

                // First check the cache
                Term fromCache = (Term) myContext.fromCache(
                        Term.class, row.getIntColumn("id"));

                if (fromCache != null)
                {
                    terms.add(fromCache);
                }
                else
                {
                    terms.add(new Term(myContext, row));
                }
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

        // Put them in an array
        Term[] termArray = new Term[terms.size()];
        termArray = (Term[]) terms.toArray(termArray);

        return termArray;
    }
    public Term createTerm(String literalForm,int relationType)throws SQLException,AuthorizeException{
        Term term = Term.create(this.myContext);
        term.setLiteralForm(literalForm);
        term.setCreated(getCreated());
        term.setLang(getLang());
        term.setSource(getSource());
        term.setLastModified(getLastModified());
        addTermByType(term,relationType);
        return term;

    }

    /**
     * add an term member
     *
     * @param t
     *            term
     */
    public void addTermByType(Term t,int type)
    {

        log.info(LogManager.getHeader(myContext, "add_Term_by_type",
                "concept_id=" + getID() + ",term_id=" + t.getID())+",type="+type);
        TableRowIterator tri = null;
        try
        {
            // Find out if mapping exists
            tri = DatabaseManager.queryTable(myContext,
                    "concept2term",
                    "SELECT * FROM concept2term WHERE " +
                            "concept_id= ? AND term_id= ? AND role_id= ?",getID(),t.getID(),type);


            if (!tri.hasNext())
            {
                // No existing mapping, so add one
                TableRow mappingRow = DatabaseManager.row("concept2term");

                mappingRow.setColumn("concept_id", getID());
                mappingRow.setColumn("term_id", t.getID());
                mappingRow.setColumn("role_id", type);
                DatabaseManager.insert(myContext, mappingRow);
                myContext.addEvent(new Event(Event.ADD, Constants.TERM, t.getID(), null));
            }
        }        catch (Exception e)
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
            log.error("error when add preferred term");
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
}
