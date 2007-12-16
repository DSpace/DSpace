/*
 * ItemDAOPostgres.java
 *
 * Version: $Revision: 1727 $
 *
 * Date: $Date: 2007-01-19 10:52:10 +0000 (Fri, 19 Jan 2007) $
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.content.dao.postgres;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataValue;
import org.dspace.content.dao.MetadataFieldDAO;
import org.dspace.content.dao.MetadataFieldDAOFactory;
import org.dspace.content.dao.MetadataSchemaDAO;
import org.dspace.content.dao.MetadataSchemaDAOFactory;
import org.dspace.content.proxy.ItemProxy;
import org.dspace.content.dao.ItemDAO;
import org.dspace.uri.ObjectIdentifier;
import org.dspace.uri.ExternalIdentifier;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * @author James Rutherford
 * @author Richard Jones
 */
public class ItemDAOPostgres extends ItemDAO
{
    /** query to get the text value of a metadata element only (qualifier is NULL) */
    private final String getByMetadataElement =
        "SELECT text_value FROM metadatavalue " +
        "WHERE item_id = ? AND metadata_field_id = ( " +
        "    SELECT metadata_field_id FROM metadatafieldregistry " +
        "    WHERE element = ? AND qualifier IS NULL " +
        "    AND metadata_schema_id = ( " +
        "        SELECT metadata_schema_id FROM metadataschemaregistry " +
        "        WHERE short_id = ? " +
        "    )" +
        ")";

    /** query to get the text value of a metadata element and qualifier */
    private final String getByMetadata =
        "SELECT text_value FROM metadatavalue " +
        "WHERE item_id = ? AND metadata_field_id = ( " +
        "    SELECT metadata_field_id FROM metadatafieldregistry " +
        "    WHERE element = ? AND qualifier = ? " +
        "    AND metadata_schema_id = ( " +
        "        SELECT metadata_schema_id FROM metadataschemaregistry " +
        "        WHERE short_id = ? " +
        "    )" +
        ")";

    /** query to get the text value of a metadata element with the wildcard
     * qualifier (*) */
    private final String getByMetadataAnyQualifier =
        "SELECT text_value FROM metadatavalue " +
        "WHERE item_id = ? AND metadata_field_id IN ( " +
        "    SELECT metadata_field_id FROM metadatafieldregistry " +
        "    WHERE element = ? " +
        "    AND metadata_schema_id = ( " +
        "        SELECT metadata_schema_id FROM metadataschemaregistry " +
        "        WHERE short_id = ? " +
        "    )" +
        ")";

    public ItemDAOPostgres(Context context)
    {
        super(context);
    }

    @Override
    public Item create() throws AuthorizeException
    {
        // UUID is not longer created here
        //UUID uuid = UUID.randomUUID();

        try
        {
            TableRow row = DatabaseManager.create(context, "item");
            // row.setColumn("uuid", uuid.toString());
            DatabaseManager.update(context, row);

            int id = row.getIntColumn("item_id");
            Item item = new ItemProxy(context, id);
            // item.setIdentifier(new ObjectIdentifier(uuid));

            return item;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public Item retrieve(int id)
    {
        try
        {
            TableRow row = DatabaseManager.find(context, "item", id);

            return retrieve(row);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public Item retrieve(UUID uuid)
    {
        try
        {
            TableRow row = DatabaseManager.findByUnique(context, "item",
                    "uuid", uuid.toString());

            return retrieve(row);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public void update(Item item) throws AuthorizeException
    {
        try
        {
            TableRow row = DatabaseManager.find(context, "item", item.getID());

            if (row != null)
            {
                // Fill out the TableRow and save it
                populateTableRowFromItem(item, row);
                row.setColumn("last_modified", new Date());
                row.setColumn("uuid", item.getIdentifier().getUUID().toString());
                DatabaseManager.update(context, row);
            }
            else
            {
                throw new RuntimeException("Didn't find item " + item.getID());
            }
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public void delete(int id) throws AuthorizeException
    {
        try
        {
            removeMetadataFromDatabase(id);

            DatabaseManager.delete(context, "item", id);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public List<Item> getItems()
    {
        try
        {
            TableRowIterator tri = DatabaseManager.queryTable(context, "item",
                    "SELECT item_id FROM item " +
                    "WHERE in_archive = '1' AND withdrawn = '0'");

            return returnAsList(tri);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public List<Item> getItems(DSpaceObject scope,
            String startDate, String endDate, int offset, int limit,
            boolean items, boolean collections, boolean withdrawn)
        throws ParseException
    {
        try
        {
            // Put together our query. Note there is no need for an
            // "in_archive=true" condition, we are using the existence of a
            // persistent identifier as our 'existence criterion'.
            String query =
                "SELECT p.value, p.type_id, p.resource_id as item_id, " +
                "i.withdrawn, i.last_modified " +
                "FROM externalidentifier p, item i";

            // We are building a complex query that may contain a variable
            // about of input data points. To accomidate this while still
            // providing type safty we build a list of parameters to be
            // plugged into the query at the database level.
            List parameters = new ArrayList();

            if (scope != null)
            {
                if (scope.getType() == Constants.COLLECTION)
                {
                    query += ", collection2item cl2i";
                }
                else if (scope.getType() == Constants.COMMUNITY)
                {
                    query += ", communities2item cm2i";
                }
            }

            query += " WHERE p.resource_type_id=" + Constants.ITEM +
                " AND p.resource_id = i.item_id ";

            if (scope != null)
            {
                if (scope.getType() == Constants.COLLECTION)
                {
                    query += " AND cl2i.collection_id= ? " +
                             " AND cl2i.item_id = p.resource_id ";
                    parameters.add(scope.getID());
                }
                else if (scope.getType() == Constants.COMMUNITY)
                {
                    query += " AND cm2i.community_id= ? " +
                             " AND cm2i.item_id = p.resource_id";
                    parameters.add(scope.getID());
                }
            }

            if (startDate != null)
            {
                query = query + " AND i.last_modified >= ? ";
                parameters.add(toTimestamp(startDate, false));
            }

            if (endDate != null)
            {
                /*
                 * If the end date has seconds precision, e.g.:
                 *
                 * 2004-04-29T13:45:43Z
                 *
                 * we need to add 999 milliseconds to this. This is because SQL
                 * TIMESTAMPs have millisecond precision, and so might have a value:
                 *
                 * 2004-04-29T13:45:43.952Z
                 *
                 * and so <= '2004-04-29T13:45:43Z' would not pick this up. Reading
                 * things out of the database, TIMESTAMPs are rounded down, so the
                 * above value would be read as '2004-04-29T13:45:43Z', and
                 * therefore a caller would expect <= '2004-04-29T13:45:43Z' to
                 * include that value.
                 *
                 * Got that? ;-)
                 */
                boolean selfGenerated = false;
                if (endDate.length() == 20)
                {
                    endDate = endDate.substring(0, 19) + ".999Z";
                    selfGenerated = true;
                }

                query += " AND i.last_modified <= ? ";
                parameters.add(toTimestamp(endDate, selfGenerated));
            }

            if (!withdrawn)
            {
                // Exclude withdrawn items
                if ("oracle".equals(ConfigurationManager.getProperty("db.name")))
                {
                    query += " AND withdrawn=0 ";
                }
                else
                {
                    // postgres uses booleans
                    query += " AND withdrawn=false ";
                }
            }

            // Order by item ID, so that for a given harvest the order will be
            // consistent. This is so that big harvests can be broken up into
            // several smaller operations (e.g. for OAI resumption tokens.)
            query += " ORDER BY p.resource_id";

            // Execute
            Object[] parametersArray = parameters.toArray();
            TableRowIterator tri = DatabaseManager.query(context, query,
                    parametersArray);

            return returnAsList(tri);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public List<Item> getItems(MetadataValue value, Date startDate, Date endDate)
    {
        // FIXME: Of course, this should actually go somewhere else
        boolean oracle = false;
        if ("oracle".equals(ConfigurationManager.getProperty("db.name")))
        {
            oracle = true;
        }

        // FIXME: this method is clearly not optimised

        String valueQuery = null;

        if (value != null)
        {
            valueQuery =
                "SELECT item_id FROM metadatavalue " +
                "WHERE metadata_field_id = ? " +
                "AND text_value LIKE ?";
//                "AND metadata_field_id = (" +
//                    "SELECT metadata_field_id FROM metadatafieldregistry " +
//                    "WHERE element = ? AND qualifier = ?" +
//                    "AND metadata_schema_id = ?" +
//                ")";
        }

        // start the date constraint query buffer
        StringBuffer dateQuery = new StringBuffer();
        // FIXME: This is a little DC-specific, but I suppose that's OK.
        dateQuery.append(
                "SELECT item_id FROM metadatavalue " +
                "WHERE metadata_field_id = (" +
                    "SELECT metadata_field_id " +
                    "FROM metadatafieldregistry " +
                    "WHERE element = 'date' " +
                    "AND qualifier = 'accessioned' " +
                ")");

        if (startDate != null)
        {
            if (oracle)
            {
                dateQuery.append(" AND TO_TIMESTAMP( TO_CHAR(text_value), "+
                        "'yyyy-mm-dd\"T\"hh24:mi:ss\"Z\"' ) > TO_DATE('" +
                        unParseDate(startDate) + "', 'yyyy-MM-dd') ");
            }
            else
            {
                dateQuery.append(" AND text_value::timestamp > '" +
                        unParseDate(startDate) + "'::timestamp ");
            }
        }

        if (endDate != null)
        {
            if (oracle)
            {
                dateQuery.append(" AND TO_TIMESTAMP( TO_CHAR(text_value), "+
                        "'yyyy-mm-dd\"T\"hh24:mi:ss\"Z\"' ) < TO_DATE('" +
                        unParseDate(endDate) + "', 'yyyy-MM-dd') ");
            }
            else
            {
                dateQuery.append(" AND text_value::timestamp < '" +
                        unParseDate(endDate) + "'::timestamp ");
            }
        }

        // build the final query
        StringBuffer query = new StringBuffer();

        query.append(
                "SELECT item_id FROM item " +
                "WHERE in_archive = " + (oracle ? "1 " : "true ") +
                "AND withdrawn = " + (oracle ? "0 " : "false "));

        if (startDate != null || endDate != null)
        {
            query.append(" AND item_id IN ( " + dateQuery.toString() + ") ");
        }

        if (value != null)
        {
            query.append(" AND item_id IN ( " + valueQuery + ") ");
        }

        try
        {
            TableRowIterator tri = null;

            if (value == null)
            {
                tri = DatabaseManager.query(context, query.toString());
            }
            else
            {
                tri = DatabaseManager.query(context, query.toString(),
                        value.getFieldID(), value.getValue());
            }

            return returnAsList(tri);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public List<Item> getItemsByCollection(Collection collection)
    {
        try
        {
            TableRowIterator tri = DatabaseManager.queryTable(context, "item",
                    "SELECT i.item_id " +
                    "FROM item i, collection2item c2i " +
                    "WHERE i.item_id = c2i.item_id "+
                    "AND c2i.collection_id = ? " +
                    "AND i.in_archive = '1'",
                    collection.getID());

            return returnAsList(tri);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public List<Item> getItemsBySubmitter(EPerson eperson)
    {
        try
        {
            TableRowIterator tri = DatabaseManager.queryTable(context, "item",
                    "SELECT item_id FROM item " +
                    "WHERE in_archive = '1' " +
                    "AND submitter_id = ? ",
                    eperson.getID());

            return returnAsList(tri);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public List<Item> getParentItems(Bundle bundle)
    {
        try
        {
            // Get items
            TableRowIterator tri = DatabaseManager.queryTable(context, "item",
                    "SELECT i.item_id FROM item i, item2bundle i2b " +
                    "WHERE i2b.item_id = i.item_id " +
                    "AND i2b.bundle_id = ? ",
                    bundle.getID());

            return returnAsList(tri);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public void link(Item item, Bundle bundle) throws AuthorizeException
    {
        if (linked(item, bundle))
        {
            return;
        }

        try
        {
            TableRow row = DatabaseManager.create(context, "item2bundle");
            row.setColumn("item_id", item.getID());
            row.setColumn("bundle_id", bundle.getID());
            DatabaseManager.update(context, row);

            // If we're adding the Bundle to the Item, we bequeath our
            // policies unto it.
            AuthorizeManager.inheritPolicies(context, item, bundle);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public void unlink(Item item, Bundle bundle) throws AuthorizeException
    {
        if (!linked(item, bundle))
        {
            return;
        }

        try
        {
            // Remove bundle mappings from DB
            DatabaseManager.updateQuery(context,
                    "DELETE FROM item2bundle WHERE item_id= ? " +
                    "AND bundle_id= ? ",
                    item.getID(), bundle.getID());
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public boolean linked(Item item, Bundle bundle)
    {
        try
        {
            TableRowIterator tri = DatabaseManager.query(context,
                    "SELECT id FROM item2bundle " +
                    " WHERE item_id=" + item.getID() +
                    " AND bundle_id=" + bundle.getID());

            boolean result = tri.hasNext();
            tri.close();

            return result;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    //
    // Note: the methods below marked public are mostly to serve the
    // ItemProxy class. The situation isn't really ideal.
    ////////////////////////////////////////////////////////////////////

    private Item retrieve(TableRow row)
    {
        if (row == null)
        {
            return null;
        }

        int id = row.getIntColumn("item_id");
        Item item = new ItemProxy(context, id);
        populateItemFromTableRow(item, row);

        // FIXME: I'd like to bump the rest of this up into the superclass
        // so we don't have to do it for every implementation, but I can't
        // figure out a clean way of doing this yet.
        List<ExternalIdentifier> identifiers =
                identifierDAO.getExternalIdentifiers(item);
        item.setExternalIdentifiers(identifiers);

        return item;
    }

    private List<Item> returnAsList(TableRowIterator tri) throws SQLException
    {
        List<Item> items = new ArrayList<Item>();

        for (TableRow row : tri.toList())
        {
            int id = row.getIntColumn("item_id");
            items.add(retrieve(id));
        }

        return items;
    }

    private void populateTableRowFromItem(Item item, TableRow row)
    {
        EPerson submitter = item.getSubmitter();
        Collection owningCollection = item.getOwningCollection();

        row.setColumn("item_id", item.getID());
        row.setColumn("in_archive", item.isArchived());
        row.setColumn("withdrawn", item.isWithdrawn());
        row.setColumn("last_modified", item.getLastModified());

        if (submitter != null)
        {
            row.setColumn("submitter_id", submitter.getID());
        }

        if (owningCollection != null)
        {
            row.setColumn("owning_collection", owningCollection.getID());
        }
    }

    private void populateItemFromTableRow(Item item, TableRow row)
    {
        UUID uuid = UUID.fromString(row.getStringColumn("uuid"));
        int submitterId = row.getIntColumn("submitter_id");
        int owningCollectionId = row.getIntColumn("owning_collection");
        boolean inArchive = row.getBooleanColumn("in_archive");
        boolean withdrawn = row.getBooleanColumn("withdrawn");
        Date lastModified = row.getDateColumn("last_modified");

        item.setIdentifier(new ObjectIdentifier(uuid));
        item.setSubmitter(submitterId);
        item.setOwningCollectionId(owningCollectionId);
        item.setArchived(inArchive);
        item.setWithdrawn(withdrawn);
        item.setLastModified(lastModified);
    }

    @Override
    public void loadMetadata(Item item)
    {
        MetadataFieldDAO mfDAO = MetadataFieldDAOFactory.getInstance(context);
        MetadataSchemaDAO msDAO = MetadataSchemaDAOFactory.getInstance(context);

        try
        {
            TableRowIterator tri = DatabaseManager.queryTable(context,
                    "metadatavalue",
                    "SELECT * FROM MetadataValue " +
                    "WHERE item_id = ? " +
                    "ORDER BY metadata_field_id, place",
                    item.getID());

            List<DCValue> metadata = new ArrayList<DCValue>();

            for (TableRow row : tri.toList())
            {
                // Get the associated metadata field and schema information
                int fieldID = row.getIntColumn("metadata_field_id");
                MetadataField field = mfDAO.retrieve(fieldID);

                if (field == null)
                {
                    log.error("Loading item - cannot find metadata field "
                            + fieldID);
                }
                else
                {
                    MetadataSchema schema =
                        msDAO.retrieve(field.getSchemaID());

                    // Make a DCValue object
                    DCValue dcv = new DCValue();
                    dcv.schema = schema.getName();
                    dcv.element = field.getElement();
                    dcv.qualifier = field.getQualifier();
                    dcv.language = row.getStringColumn("text_lang");
                    dcv.value = row.getStringColumn("text_value");

                    // Add it to the item
                    metadata.add(dcv);
                }
            }

            item.setMetadata(metadata);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    /**
     * Perform a database query to obtain the string array of values
     * corresponding to the passed parameters. This is only really called from
     *
     * <code>
     * getMetadata(schema, element, qualifier, lang);
     * </code>
     *
     * which will obtain the value from cache if available first.
     *
     * @param schema
     * @param element
     * @param qualifier
     * @param lang
     */
    @Override
    public List<DCValue> getMetadata(Item item, String schema, String element,
            String qualifier, String lang)
    {
        List<DCValue> metadata = new ArrayList<DCValue>();
        try
        {
            TableRowIterator tri;

            if (qualifier == null)
            {
                Object[] params = { item.getID(), element, schema };
                tri = DatabaseManager.query(context, getByMetadataElement,
                        params);
            }
            else if (Item.ANY.equals(qualifier))
            {
                Object[] params = { item.getID(), element, schema };
                tri = DatabaseManager.query(context, getByMetadataAnyQualifier,
                        params);
            }
            else
            {
                Object[] params = { item.getID(), element, qualifier, schema };
                tri = DatabaseManager.query(context, getByMetadata, params);
            }

            while (tri.hasNext())
            {
                TableRow tr = tri.next();
                DCValue dcv = new DCValue();
                dcv.schema = schema;
                dcv.element = element;
                dcv.qualifier = qualifier;
                dcv.language = lang;
                dcv.value = tr.getStringColumn("text_value");
                metadata.add(dcv);
            }
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
        return metadata;
    }

    private void removeMetadataFromDatabase(int itemId) throws SQLException
    {
        DatabaseManager.updateQuery(context,
                "DELETE FROM MetadataValue WHERE item_id= ? ",
                itemId);
    }

    /**
     * Convert a String to a java.sql.Timestamp object
     *
     * @param t The timestamp String
     * @param selfGenerated Is this a self generated timestamp (e.g. it has
     *                      .999 on the end)
     * @return The converted Timestamp
     * @throws ParseException
     */
    private static Timestamp toTimestamp(String t, boolean selfGenerated)
        throws ParseException
    {
        SimpleDateFormat df;

        // Choose the correct date format based on string length
        if (t.length() == 10)
        {
            df = new SimpleDateFormat("yyyy-MM-dd");
        }
        else if (t.length() == 20)
        {
            df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        }
        else if (selfGenerated)
        {
            df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        }
        else {
            // Not self generated, and not in a guessable format
            throw new ParseException("", 0);
        }

        // Parse the date
        df.setCalendar(Calendar.getInstance(TimeZone.getTimeZone("UTC")));
        return new Timestamp(df.parse(t).getTime());
    }

    /**
     * Take the date object and convert it into a string of the form YYYY-MM-DD
     *
     * @param   date    the date to be converted
     *
     * @return          A string of the form YYYY-MM-DD
     */
    private static String unParseDate(Date date)
    {
        // Use SimpleDateFormat
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy'-'MM'-'dd");
        return sdf.format(date);
    }
}
