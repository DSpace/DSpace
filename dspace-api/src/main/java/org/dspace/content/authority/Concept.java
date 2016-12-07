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
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;

import java.io.IOException;
import java.lang.Exception;
import java.lang.Override;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Class representing a DSpace Concept, that acts as
 * a container of DSpace Terms.
 *
 * @author Lantian Gai, Mark Diggory
 */
public class Concept extends AuthorityObject
{
    public static String TABLE = "concept";
    private static String CONCEPT_METADATA_TABLE = "conceptmetadatavalue";

    public enum Status {
        CANDIDATE,
        ACCEPTED,
        WITHDRAWN;
    }

    /** log4j logger */
    private static Logger log = Logger.getLogger(Concept.class);
    private static int journal_field_id;

    /**
     * Construct a Concept from a given context and tablerow
     *
     * @param context
     * @param row
     */
    Concept(Context context, TableRow row) throws SQLException
    {
        super(context, row);
    }
    /**
     * Create a new concept
     *
     * @param context
     *            DSpace context object
     */
    public static Concept create(Context context) throws SQLException, AuthorizeException
    {
        return create(context, AuthorityObject.createIdentifier());
    }

    public static Concept create(Context context, String identifier) throws SQLException, AuthorizeException
    {
        // authorized?
        if (!AuthorizeManager.isAdmin(context)) {
            throw new AuthorizeException("You must be an admin to create a Concept");
        }

        // Create a table row
        TableRow row = DatabaseManager.create(context, "concept");

        Concept concept = new Concept(context, row);

        concept.setIdentifier(context, identifier);
        context.commit();

        log.info(LogManager.getHeader(context, "create_concept", "metadata_concept_id="
                + concept.getID()));

        return concept;
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


        addTermByType(t, 1);

    }
    /**
     * add an term member
     *
     * @param t
     *            term
     */
    public void addAltTerm(Term t)
    {

        addTermByType(t, 2);

    }
    /**
     * remove an term from a concept
     *
     * @param t
     *            term
     */
    public void removePreferredTerm(Context context, Term t)throws SQLException, AuthorizeException, IOException
    {

        // authorized?
        if (!AuthorizeManager.isAdmin(context)) {
            throw new AuthorizeException("You must be an admin to remove a Concept's Preferred Term");
        }
        log.info(LogManager.getHeader(context, "remove_preferredTerm",
                "concept_id=" + getID() + ",term_id=" + t.getID()));

        TableRow trow = DatabaseManager.querySingleTable(context, TABLE,
                "SELECT COUNT(DISTINCT concept_id) AS num FROM concept2term WHERE term_id= ? AND role_id=1",
                t.getID());
        DatabaseManager.setConstraintDeferred(context, "concept2term_term_id_fkey");

        if (trow.getLongColumn("num") == 1)
        {
            // Orphan; delete it
            t.delete(context);
        }

        log.info(LogManager.getHeader(context, "remove_term",
                "concept_id=" + getID() + ",term_id=" + t.getID()));

        // Remove any mappings
        DatabaseManager.updateQuery(context,
                "DELETE FROM concept2term WHERE concept_id= ? " +
                        "AND term_id= ? AND role_id=1", getID(), t.getID());

        DatabaseManager.setConstraintImmediate(context, "concept2term_term_id_fkey");
    }

    /**
     * remove an term from a concept
     *
     * @param t
     *            term
     */
    public void removeAltTerm(Context context, Term t)throws SQLException, AuthorizeException, IOException
    {
        // authorized?
        if (!AuthorizeManager.isAdmin(context)) {
            throw new AuthorizeException("You must be an admin to remove a Concept's Alternate Term");
        }
        log.info(LogManager.getHeader(context, "remove_altTerm",
                "concept_id=" + getID() + ",term_id=" + t.getID()));

        TableRow trow = DatabaseManager.querySingle(context,
                "SELECT COUNT(DISTINCT concept_id) AS num FROM concept2term WHERE term_id= ? AND role_id=2",
                t.getID());
        DatabaseManager.setConstraintDeferred(context, "concept2term_term_id_fkey");

        if (trow.getLongColumn("num") == 1)
        {
            // Orphan; delete it
            t.delete(context);
        }

        log.info(LogManager.getHeader(context, "remove_term",
                "concept_id=" + getID() + ",term_id=" + t.getID()));

        // Remove any mappings
        DatabaseManager.updateQuery(context,
                "DELETE FROM concept2term WHERE concept_id= ? " +
                        "AND term_id= ? AND role_id=2", getID(), t.getID());

        DatabaseManager.setConstraintImmediate(context, "concept2term_term_id_fkey");
    }
    /**
     * remove concept from this concept
     *
     * @param c
     */
    public void removeParentConcept(Context context, Concept c)throws SQLException, AuthorizeException, IOException
    {
        // authorized?
        if (!AuthorizeManager.isAdmin(context)) {
            throw new AuthorizeException("You must be an admin to remove a Concept's Parent Concept");
        }
        log.info(LogManager.getHeader(context, "remove_parentConcept",
                "concept_id=" + getID() + ",parent_concept_id=" + c.getID()));

        DatabaseManager.setConstraintDeferred(context, "Concept2Concept_incoming_id_fkey ");

        log.info(LogManager.getHeader(context, "remove_term",
                "concept_id=" + getID() + ",parent_concept_id=" + c.getID()));

        // Remove any mappings
        DatabaseManager.updateQuery(context,
                "DELETE FROM concept2term WHERE imcoming_id= ? " +
                        "AND outgoing_id= ? AND role_id=1", getID(), c.getID());

        DatabaseManager.setConstraintImmediate(context, "Concept2Concept_incoming_id_fkey ");
    }

    /**
     * remove concept from this concept
     *
     * @param c
     */
    public void removeChildConcept(Context context, Concept c)throws SQLException, AuthorizeException, IOException
    {
        // authorized?
        if (!AuthorizeManager.isAdmin(context)) {
            throw new AuthorizeException("You must be an admin to remove a Concept's Child Concept");
        }
        log.info(LogManager.getHeader(context, "remove_parentConcept",
                "concept_id=" + getID() + ",child_concept_id=" + c.getID()));

        DatabaseManager.setConstraintDeferred(context, "Concept2Concept_outgoing_id_fkey ");

        log.info(LogManager.getHeader(context, "remove_term",
                "concept_id=" + getID() + ",child_concept_id=" + c.getID()));

        // Remove any mappings
        DatabaseManager.updateQuery(context,
                "DELETE FROM concept2term WHERE outgoing_id= ? " +
                        "AND incoming_id= ? AND role_id=1", getID(), c.getID());

        DatabaseManager.setConstraintImmediate(context, "Concept2Concept_outgoing_id_fkey ");
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
    public static Concept find(Context context, int id) throws SQLException {
        TableRow row = DatabaseManager.find(context, "concept", id);

        if (row == null) {
            return null;
        } else {
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
        TableRowIterator row = DatabaseManager.queryTable(context, TABLE, "select * from concept where LOWER(identifier) like ?", identifier);

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
     * Find concepts by a metadata value in the concept metadata.
     *
     * @return the matching Concepts, or null if not found
     */
    public static ArrayList<Concept> findByConceptMetadata(Context context, String searchString, String metadataSchema, String metadataElement)
            throws SQLException
    {
        ArrayList<Concept> concepts = new ArrayList<Concept>();
        try {
            MetadataSchema mds = MetadataSchema.find(context, metadataSchema);
            MetadataField mdf = MetadataField.findByElement(context, mds.getSchemaID(), metadataElement, null);
            int target_field_id = mdf.getFieldID();
            log.info ("looking up concept metadata for " + searchString + " in field number " + target_field_id);
            TableRowIterator row = DatabaseManager.queryTable(context, TABLE, "select c.* from concept as c, conceptmetadatavalue as cmv where upper(cmv.text_value) = ? and cmv.parent_id = c.id and cmv.field_id = ?;", searchString, target_field_id);


            if (row == null) {
                return null;
            } else {
                while(row.hasNext()) {
                    concepts.add(new Concept(context,row.next()));
                }
            }
        } catch (NullPointerException e) {
            log.error("Unable to find concept by metadata: search=" + searchString + ", schema=" + metadataSchema + ", element=" + metadataElement, e);
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
                context, TABLE,
                "SELECT * FROM concept ORDER BY " + s);

        try
        {
            List<TableRow> gRows = rows.toList();

            Concept[] concepts = new Concept[gRows.size()];

            for (int i = 0; i < gRows.size(); i++) {
                TableRow row = gRows.get(i);
                concepts[i] = new Concept(context, row);
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

    public static Concept[] searchByMetadata(Context context, String metadatafield, String metadataValue) {
        ArrayList<Concept> conceptArrayList = new ArrayList<Concept>();
        // find the metadata field id from the registry:
        try {
            MetadataField mdf = MetadataField.findByElement(context, metadatafield);
            TableRowIterator tri = DatabaseManager.queryTable(context, CONCEPT_METADATA_TABLE, "SELECT * FROM " + CONCEPT_METADATA_TABLE + " where field_id = ?", mdf.getFieldID());
            while (tri.hasNext()) {
                TableRow row = tri.next();
                if (metadataValue.equals(row.getStringColumn("text_value"))) {
                    int conceptID = row.getIntColumn("parent_id");
                    Concept concept = find(context, conceptID);
                    if (concept != null) {
                        conceptArrayList.add(concept);
                    }
                }
            }
            tri.close();
        } catch (SQLException e) {
            log.error("couldn't find metadata field " + metadatafield);
        }

        return conceptArrayList.toArray(new Concept[conceptArrayList.size()]);
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
                DatabaseManager.queryTable(context, TABLE, dbquery, paramArr);

        try
        {
            List<TableRow> conceptRows = rows.toList();
            Concept[] concepts = new Concept[conceptRows.size()];

            for (int i = 0; i < conceptRows.size(); i++)
            {
                TableRow row = conceptRows.get(i);

                concepts[i] = new Concept(context, row);
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

    /* Delete data specific to concepts
     */
    @Override
    protected void deleteAssociatedData(Context context) throws SQLException, AuthorizeException {
        //delete term
        Term[] terms = getTerms();
        for (Term term : terms) {
            try {
                term.delete(context);
            } catch (Exception e) {
                log.error("can't delete term: " +term.getID()+" for concept: "+getID() + ": " + e.getMessage());
            }
        }

        //remove concept from scheme
        DatabaseManager.updateQuery(context,
                "DELETE FROM Scheme2Concept WHERE concept_id= ? ",
                getID());
        //remove relationships
        DatabaseManager.updateQuery(context,
                "DELETE FROM Concept2Concept WHERE incoming_id= ?",
                getID());
        DatabaseManager.updateQuery(context,
                "DELETE FROM Concept2Concept WHERE outgoing_id= ?",
                getID());
    }

    /**
     * Return Concept members of a Concept.
     */
    public Concept[] getParentConcepts() throws SQLException
    {
        String query = "SELECT concept.* FROM concept, Concept2Concept WHERE " +
                "Concept2Concept.incoming_id=concept.id " +
                "AND Concept2Concept.outgoing_id= ? ORDER BY concept.id";
        return conceptsFromQuery(query);
    }



    /**
     * Return related members of a Concept.
     */
    public Concept[] getRelatedConcepts(String relation,String direction,String scheme) throws SQLException
    {
        String query = "SELECT concept.* FROM concept, Concept2Concept, Scheme2Concept,concept2conceptrole,scheme WHERE concept.id = scheme2concept.concept_id ";
        if (direction.equals(Concept2ConceptRole.relation_incoming)) {
            query = query + " AND Concept2Concept.incoming_id=concept.id " +
                    "AND Concept2Concept.outgoing_id= ? ";
        } else if (direction.equals(Concept2ConceptRole.relation_outgoing)) {
            query = query+" AND Concept2Concept.outgoing_id=concept.id " +
                    "AND Concept2Concept.incoming_id= ? ";
        } else {
            //find all relationships
            query = query+" AND Concept2Concept.incoming_id = ? " +
                    "OR Concept2Concept.outgoing_id= ? ";
        }

        if (relation!=null&&relation.length()>0) {
            query = query + " AND Concept2Concept.role_id=concept2conceptrole.id AND concept2conceptrole.role='" + relation + "' ";
        }

        if (scheme!=null&&scheme.length()>0) {
            query = query + " AND scheme.id=scheme2concept.scheme_id AND LOWER(scheme.identifier) = '" + scheme + "' ";
        }

        return conceptsFromQuery(query);
    }

    /**
     * Return Concept members of a Concept.
     */
    public Concept[] getChildConcepts() throws SQLException {
        String query = "SELECT concept.* FROM concept, Concept2Concept WHERE " +
                "Concept2Concept.outgoing_id=concept.id " +
                "AND Concept2Concept.incoming_id= ? ORDER BY concept.id";

        return conceptsFromQuery(query);
    }

    private Concept[] conceptsFromQuery(String query) {
        List<Concept> concepts = new ArrayList<Concept>();
        Context context = getContext();
        TableRowIterator tri = null;
        // Make Collection objects
        try {
            // Get the table rows
            tri = DatabaseManager.queryTable(context, TABLE, query, getID());
            while (tri.hasNext()) {
                TableRow row = tri.next();
                concepts.add(new Concept(context, row));
            }
            completeContext(context);
        } catch (SQLException e) {
            abortContext(context);
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
        Context context = getContext();
        TableRowIterator tri = null;
        List<Scheme> schemes = new ArrayList<Scheme>();
        try {
            tri = DatabaseManager.queryTable(context,"scheme",
                "SELECT scheme.* FROM scheme, scheme2concept WHERE " +
                        "scheme.id=scheme2concept.scheme_id " +
                        "AND scheme2concept.concept_id= ? ",
                getID());

            while (tri.hasNext()) {
                TableRow row = tri.next();
                Scheme owner = new Scheme(context, row);
                schemes.add(owner);
            }
            completeContext(context);
        } catch (SQLException e) {
            abortContext(context);
        }
        finally {
            // close the TableRowIterator to free up resources
            if (tri != null) {
                tri.close();
            }
        }

        if (schemes.size() ==0) {
            return null;
        }
        return schemes.get(0);
    }


    /**
     * Method that updates the last modified date of the item
     */
    public void updateLastModified() {
        Context context = getContext();
        try {
            Date lastModified = new java.sql.Timestamp(new Date().getTime());
            myRow.setColumn("modified", lastModified);
            DatabaseManager.updateQuery(context, "UPDATE " + TABLE + " SET modified = ? WHERE id= ? ", lastModified, getID());
            completeContext(context);
        } catch (SQLException e) {
            abortContext(context);
            log.error(LogManager.getHeader(context, "Error while updating modified timestamp", "Concept: " + getID()));
        }
    }

    @Override
    public String getMetadataTable() {
        return CONCEPT_METADATA_TABLE;
    }

    public Term[] getPreferredTerms() throws SQLException {
        List<Term> terms = new ArrayList<Term>();

        Context context = getContext();
        TableRowIterator tri = DatabaseManager.queryTable(
                context,"term",
                "SELECT term.* FROM term, concept2term WHERE " +
                        "concept2term.term_id=term.id " +
                        "AND concept2term.concept_id= ?  AND role_id = 1 ORDER BY term.identifier",
                getID());

        try {
            while (tri.hasNext()) {
                TableRow row = tri.next();
                terms.add(new Term(context, row));
            }
            completeContext(context);
        } catch (SQLException e) {
            abortContext(context);
        }
        finally {
            // close the TableRowIterator to free up resources
            if (tri != null) {
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

        Context context = getContext();
        TableRowIterator tri = DatabaseManager.queryTable(
                context,"term",
                "SELECT term.* FROM term, concept2term WHERE " +
                        "concept2term.term_id=term.id " +
                        "AND concept2term.concept_id= ? AND role_id = 2 ORDER BY term.identifier",
                getID());

        try {
            while (tri.hasNext()) {
                TableRow row = tri.next();
                terms.add(new Term(context, row));
            }
            completeContext(context);
        } catch (SQLException e) {
            abortContext(context);
        }
        finally {
            // close the TableRowIterator to free up resources
            if (tri != null) {
                tri.close();
            }
        }

        // Put them in an array
        Term[] conceptArray = new Term[terms.size()];
        conceptArray = (Term[]) terms.toArray(conceptArray);

        return conceptArray;
    }

    public void addParentConcept(Context context, Concept incoming, int roleId) throws SQLException, AuthorizeException
    {
        // authorized?
        if (!AuthorizeManager.isAdmin(context)) {
            throw new AuthorizeException("You must be an admin to modify a Concept's Parent Concept");
        }

        log.info(LogManager.getHeader(context, "add_parentConcept",
                "concept_id=" + getID() + ",parent_concept_id=" + incoming.getID()));

        // Find out if mapping exists
        TableRowIterator tri = DatabaseManager.queryTable(context,
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

    public void addChildConcept(Context context, Concept outgoing, int roleId) throws SQLException, AuthorizeException
    {
        // authorized?
        if (!AuthorizeManager.isAdmin(context)) {
            throw new AuthorizeException("You must be an admin to add a Concept's Child Concept");
        }

        log.info(LogManager.getHeader(context, "add_childConcept",
                "concept_id=" + getID() + ",child_concept_id=" + outgoing.getID()));

        // Find out if mapping exists
        TableRowIterator tri = DatabaseManager.queryTable(context,
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

    public void addTerm(Context context, Term t,int role_id) throws SQLException, AuthorizeException
    {
        // authorized?
        if (!AuthorizeManager.isAdmin(context)) {
            throw new AuthorizeException("You must be an admin to add a Term to a Concept");
        }

        log.info(LogManager.getHeader(context, "add_term",
                "concept_id=" + getID() + ",term_id=" + t.getID()));

        // Find out if mapping exists
        TableRowIterator tri = DatabaseManager.queryTable(context,
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

    public static Concept[] findByPreferredLabel(Context context,String query,int schemeId){
        //make schemeId == -1 to get all match preferred label
        ArrayList<Concept> concepts = new ArrayList<Concept>();
        String dbquery = "SELECT concept.* FROM concept,concept2term,term where concept.id=concept2term.concept_id and concept2term.term_id=term.id and LOWER(term.literalform) = LOWER(?) order by term.literalform";
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
        Context context = getContext();
        TableRowIterator tri = DatabaseManager.queryTable(
                context,"term",
                "SELECT term.* FROM term, concept2term WHERE " +
                        "concept2term.term_id=term.id " +
                        "AND concept2term.concept_id= ?",
                getID());

        try {
            if (tri != null) {
                while (tri.hasNext()) {
                    TableRow row = tri.next();
                    terms.add(new Term(context, row));
                }
            }
            completeContext(context);
        } catch (SQLException e) {
            abortContext(context);
        }
        finally {
            // close the TableRowIterator to free up resources
            if (tri != null) {
                tri.close();
            }
        }

        // Put them in an array
        Term[] termArray = new Term[terms.size()];
        termArray = (Term[]) terms.toArray(termArray);

        return termArray;
    }

    public Term createTerm(Context context, String literalForm,int relationType)throws SQLException,AuthorizeException{
        // authorized?
        if (!AuthorizeManager.isAdmin(context)) {
            throw new AuthorizeException("You must be an admin to create a Term");
        }
        Term term = null;
        try {
            term = Term.create(context);
        } catch (Exception e) {
            log.error("couldn't create Term " + e.getMessage());
        }
        context.commit();
        term.setLiteralForm(context, literalForm);
        term.setCreated(context, getCreated());
        term.setLang(context, getLang());
        term.setSource(context, getSource());
        term.setLastModified(context, getLastModified());
        context.commit();
        addTermByType(term, relationType);
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
        Context context = getContext();
        log.info(LogManager.getHeader(context, "add_Term_by_type",
                "concept_id=" + getID() + ",term_id=" + t.getID())+",type="+type);
        TableRowIterator tri = null;
        try {
            // Find out if mapping exists
            tri = DatabaseManager.queryTable(context,
                    "concept2term",
                    "SELECT * FROM concept2term WHERE " +
                            "concept_id= ? AND term_id= ? AND role_id= ?",getID(),t.getID(),type);


            if (!tri.hasNext()) {
                // No existing mapping, so add one
                TableRow mappingRow = DatabaseManager.row("concept2term");

                mappingRow.setColumn("concept_id", getID());
                mappingRow.setColumn("term_id", t.getID());
                mappingRow.setColumn("role_id", type);
                DatabaseManager.insert(context, mappingRow);
            }
            completeContext(context);
        } catch (Exception e) {
            abortContext(context);
            log.error("error when adding term by type: " + e.getMessage());
        }
        finally {
            // close the TableRowIterator to free up resources
            if (tri != null) {
                tri.close();
            }
        }

    }

    @Override
    public DSpaceObject getParentObject() throws SQLException
    {
        return this.getScheme();
    }
}
