/*
 * DSpaceOAICatalog.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2001, Hewlett-Packard Company and Massachusetts
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

package org.dspace.app.oai;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import org.apache.log4j.Logger;

import ORG.oclc.oai.server.catalog.AbstractCatalog;
import ORG.oclc.oai.server.catalog.RecordFactory;
import ORG.oclc.oai.server.verb.BadResumptionTokenException;
import ORG.oclc.oai.server.verb.CannotDisseminateFormatException;
import ORG.oclc.oai.server.verb.IdDoesNotExistException;
import ORG.oclc.oai.server.verb.NoMetadataFormatsException;
import ORG.oclc.oai.server.verb.NoSetHierarchyException;
import ORG.oclc.oai.server.verb.OAIInternalServerError;

import org.dspace.administer.DCType;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.handle.HandleManager;

// FIXME: "Tentacle" - shouldn't access storage layer directly here!!!
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;


/**
 * This is class extends OAICat's AbstractCatalog base class to allow
 * metadata harvesting of the metadata in DSpace via OAI-PMH 2.0.
 *
 * @author  Robert Tansley
 * @version $Revision$
 */
public class DSpaceOAICatalog extends AbstractCatalog
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(DSpaceOAICatalog.class);
    
    /** Location of OAI DC schema */
    private final String oaiDC =
        "http://www.openarchives.org/OAI/2.0/oai_dc/ " +
        "http://www.openarchives.org/OAI/2.0/oai_dc.xsd";
    
    private int dcTypeID;


    //** Location of qualified DC schema */
    //private final String simpleDC =
    //    "http://dublincore.org/schemas/xmls/simpledc20020312.xsd";
    
    public DSpaceOAICatalog(Properties properties)
        throws SQLException
    {
        // Find the Dublin Core DC type ID
        Context context = null;

        try
        {
            context = new Context();

            DCType type = DCType.findByElement(context, "date", "available");
            dcTypeID = type.getID();
        }
        catch (SQLException se)
        {
            // Log the error
            log.warn(LogManager.getHeader(context,
                "database_error",
                ""), se);
            
            throw se;
        }
        finally
        {
            if (context != null)
            {
                context.abort();
            }
        }
    }


    /**
     * Retrieve a list of schemaLocation values associated with the specified
     * identifier.
     *
     * @param identifier the OAI identifier
     * @return a Vector containing schemaLocation Strings
     * @exception OAIInternalServerError signals an http status code 500 problem
     * @exception IdDoesNotExistException the specified identifier can't be found
     * @exception NoMetadataFormatsException the specified identifier was found
     * but the item is flagged as deleted and thus no schemaLocations (i.e.
     * metadataFormats) can be produced.
     */
    public Vector getSchemaLocations(String identifier)
        throws OAIInternalServerError, IdDoesNotExistException, NoMetadataFormatsException
    {
        OAIItemInfo itemInfo = null;
        Context context = null;

        // Get the item from the DB
        try
        {
            context = new Context();

            itemInfo = getItem(context, identifier);
        }
        catch (SQLException se)
        {
            // Log the error
            log.warn(LogManager.getHeader(context,
                "database_error",
                ""), se);

            throw new OAIInternalServerError(se.toString());
        }
        finally
        {
            if (context != null)
            {
                context.abort();
            }
        }

        if (itemInfo == null)
        {
            throw new IdDoesNotExistException(identifier);
        }
        else
        {
            return getRecordFactory().getSchemaLocations(itemInfo);
        }
    }


    /**
     * Retrieve a list of identifiers that satisfy the specified criteria
     *
     * @param from beginning date using the proper granularity
     * @param until ending date using the proper granularity
     * @param set the set name or null if no such limit is requested
     * @param metadataPrefix the OAI metadataPrefix or null if no such limit is requested
     * @return a Map object containing entries for "headers" and "identifiers" Iterators
     * (both containing Strings) as well as an optional "resumptionMap" Map.
     * It may seem strange for the map to include both "headers" and "identifiers"
     * since the identifiers can be obtained from the headers. This may be true, but
     * AbstractCatalog.listRecords() can operate quicker if it doesn't
     * need to parse identifiers from the XML headers itself. Better
     * still, do like I do below and override AbstractCatalog.listRecords().
     * AbstractCatalog.listRecords() is relatively inefficient because given the list
     * of identifiers, it must call getRecord() individually for each as it constructs
     * its response. It's much more efficient to construct the entire response in one fell
     * swoop by overriding listRecords() as I've done here.
     * @exception OAIInternalServerError signals an http status code 500 problem
     * @exception NoSetHierarchyException the repository doesn't support sets.
     * @exception CannotDisseminateFormatException the metadata format specified
     * is not supported by your repository.
     */
    public Map listIdentifiers(String from, String until, String set, String metadataPrefix)
        throws OAIInternalServerError, NoSetHierarchyException, CannotDisseminateFormatException
    {
        // We can produce oai_dc and simple DC for all items, so just return IDs
        Context context = null;

        // Lists to put results in
        List headers = new LinkedList();
        List identifiers = new LinkedList();

        try
        {
            context = new Context();

            // Get the relevant OAIItemInfo objects to make headers
            DSpaceObject scope = resolveSet(context, set);
            List itemHeaderInfos = getHeaderInfo(context, scope, from, until);

            // Build up lists of headers and identifiers
            Iterator i = itemHeaderInfos.iterator();

            while (i.hasNext())
            {
                OAIItemInfo itemInfo = (OAIItemInfo) i.next();
                
                String[] header = getRecordFactory().createHeader(itemInfo);

                headers.add(header[0]);
                identifiers.add(header[1]);
            }
        }
        catch (SQLException se)
        {
            // Log the error
            log.warn(LogManager.getHeader(context,
                "database_error",
                ""), se);

            throw new OAIInternalServerError(se.toString());
        }
        finally
        {
            if (context != null)
            {
                context.abort();
            }
        }
        
        // Put results in form needed to return
        Map results = new HashMap();
        results.put("headers", headers.iterator());
        results.put("identifiers", identifiers.iterator());
        return results;
    }


    /**
     * Retrieve the next set of identifiers associated with the resumptionToken
     *
     * @param resumptionToken implementation-dependent format taken from the
     * previous listIdentifiers() Map result.
     * @return a Map object containing entries for "headers" and "identifiers" Iterators
     * (both containing Strings) as well as an optional "resumptionMap" Map.
     * @exception BadResumptionTokenException the value of the resumptionToken
     * is invalid or expired.
     * @exception OAIInternalServerError signals an http status code 500 problem
     */
    public Map listIdentifiers(String resumptionToken)
        throws BadResumptionTokenException, OAIInternalServerError
    {
        // Resumption tokens not yet supported
        throw new BadResumptionTokenException();
    }

        
    /**
     * Retrieve the specified metadata for the specified identifier
     *
     * @param identifier the OAI identifier
     * @param metadataPrefix the OAI metadataPrefix
     * @return the <record/> portion of the XML response.
     * @exception OAIInternalServerError signals an http status code 500 problem
     * @exception CannotDisseminateFormatException the metadataPrefix is not
     * supported by the item.
     * @exception IdDoesNotExistException the identifier wasn't found
     */
    public String getRecord(String identifier, String metadataPrefix)
        throws OAIInternalServerError, CannotDisseminateFormatException,
               IdDoesNotExistException
    {
        Context context = null;
        OAIItemInfo itemInfo = null;

        // First get the item from the DB
        try
        {
            context = new Context();

            itemInfo = getItem(context, identifier);
        }
        catch (SQLException se)
        {
            // Log the error
            log.warn(LogManager.getHeader(context,
                "database_error",
                ""), se);

            throw new OAIInternalServerError(se.toString());
        }
        finally
        {
            if (context != null)
            {
                context.abort();
            }
        }

        String schemaURL = null;

        if (itemInfo == null)
        {
            throw new IdDoesNotExistException(identifier);
        }
        else if ((schemaURL = getCrosswalks().getSchemaURL(metadataPrefix)) == null)
        {
            throw new CannotDisseminateFormatException(metadataPrefix);
        }

        return getRecordFactory().create(itemInfo, schemaURL, metadataPrefix);
    }


    /**
     * Retrieve a list of records that satisfy the specified criteria. Note, though,
     * that unlike the other OAI verb type methods implemented here, both of the
     * listRecords methods are already implemented in AbstractCatalog rather than
     * abstracted. This is because it is possible to implement ListRecords as a
     * combination of ListIdentifiers and GetRecord combinations. Nevertheless,
     * I suggest that you override both the AbstractCatalog.listRecords methods
     * here since it will probably improve the performance if you create the
     * response in one fell swoop rather than construct it one GetRecord at a time.
     *
     * @param from beginning date using the proper granularity
     * @param until ending date using the proper granularity
     * @param set the set name or null if no such limit is requested
     * @param metadataPrefix the OAI metadataPrefix or null if no such limit is requested
     * @return a Map object containing entries for a "records" Iterator object
     * (containing XML <record/> Strings) and an optional "resumptionMap" Map.
     * @exception OAIInternalServerError signals an http status code 500 problem
     * @exception NoSetHierarchyException The repository doesn't support sets.
     * @exception CannotDisseminateFormatException the metadataPrefix isn't
     * supported by the item.
     */
    public Map listRecords(String from, String until, String set, String metadataPrefix)
        throws OAIInternalServerError, NoSetHierarchyException, CannotDisseminateFormatException
    {
        Context context = null;
        String schemaURL = getCrosswalks().getSchemaURL(metadataPrefix);

        if (schemaURL == null)
        {
            throw new CannotDisseminateFormatException(metadataPrefix);
        }

        // List to put results in
        List records = new LinkedList();

        try
        {
            context = new Context();

            // Get the relevant OAIItemInfo objects to make headers
            DSpaceObject scope = resolveSet(context, set);
            List itemInfos = getItems(context, scope, from, until);

            // Build list of records from item info objects
            Iterator i = itemInfos.iterator();

            while (i.hasNext())
            {
                OAIItemInfo itemInfo = (OAIItemInfo) i.next();

                /*
                 * FIXME: I've a feeling a "CannotDisseminateFormatException"
                 * should be caught and discarded here - it's OK if some
                 * records in the requested date range don't have the
                 * requested metadata format available
                 */
                String recordXML = getRecordFactory().create(
                    itemInfo, schemaURL, metadataPrefix);

                records.add(recordXML);
            }
        }
        catch (SQLException se)
        {
            // Log the error
            log.warn(LogManager.getHeader(context,
                "database_error",
                ""), se);

            throw new OAIInternalServerError(se.toString());
        }
        finally
        {
            if (context != null)
            {
                context.abort();
            }
        }
        
        // Put results in form needed to return
        Map results = new HashMap();
        results.put("records", records.iterator());
        return results;
    }


    /**
     * Retrieve the next set of records associated with the resumptionToken
     *
     * @param resumptionToken implementation-dependent format taken from the
     * previous listRecords() Map result.
     * @return a Map object containing entries for "headers" and "identifiers" Iterators
     * (both containing Strings) as well as an optional "resumptionMap" Map.
     * @exception OAIInternalServerError signals an http status code 500 problem
     * @exception BadResumptionTokenException the value of the resumptionToken argument
     * is invalid or expired.
     */
    public Map listRecords(String resumptionToken)
        throws BadResumptionTokenException, OAIInternalServerError
    {
        // Resumption tokens not yet supported
        throw new BadResumptionTokenException();
    }
    

    /**
     * Retrieve a list of sets that satisfy the specified criteria
     *
     * @return a Map object containing "sets" Iterator object (contains
     * <setSpec/> XML Strings) as well as an optional resumptionMap Map.
     * @exception OAIBadRequestException signals an http status code 400 problem
     * @exception OAIInternalServerError signals an http status code 500 problem
     */
    public Map listSets()
        throws NoSetHierarchyException, OAIInternalServerError
    {
        Context context = null;

        // List to put results in
        List sets = new LinkedList();

        try
        {
            context = new Context();

            // FIXME: This may break the OAI protocol with multiple inclusion
            // FIXME: Should check perms?
            Community[] allComms = Community.findAll(context);

            for (int i = 0; i < allComms.length; i++)
            {
                // Set XML community
                String communitySpec = "<set><setSpec>" + allComms[i].getID() +
                    "</setSpec><setName>" + allComms[i].getMetadata("name") +
                    "</setName></set>";
                sets.add(communitySpec);

                // Add set XML for collections
                Collection[] colls = allComms[i].getCollections();

                for (int j = 0; j < colls.length; j++)
                {
                    StringBuffer collectionSpec =
                        new StringBuffer("<set><setSpec>");
                    
                    collectionSpec.append(allComms[i].getID())
                        .append(":")
                        .append(colls[j].getID())
                        .append("</setSpec><setName>")
                        .append(colls[j].getMetadata("name"))
                        .append("</setName></set>");
                    
                    sets.add(collectionSpec.toString());
                }
            }
        }
        catch (SQLException se)
        {
            // Log the error
            log.warn(LogManager.getHeader(context,
                "database_error",
                ""), se);

            throw new OAIInternalServerError(se.toString());
        }
        finally
        {
            if (context != null)
            {
                context.abort();
            }
        }
        
        // Put results in form needed to return
        Map results = new HashMap();
        results.put("sets", sets.iterator());
        return results;
    }


    /**
     * Retrieve the next set of sets associated with the resumptionToken
     *
     * @param resumptionToken implementation-dependent format taken from the
     * previous listSets() Map result.
     * @return a Map object containing "sets" Iterator object (contains
     * <setSpec/> XML Strings) as well as an optional resumptionMap Map.
     * @exception BadResumptionTokenException the value of the resumptionToken
     * is invalid or expired.
     * @exception OAIInternalServerError signals an http status code 500 problem
     */
    public Map listSets(String resumptionToken)
        throws BadResumptionTokenException, OAIInternalServerError
    {
        // Resumption tokens not yet supported
        throw new BadResumptionTokenException();
    }


    /**
     * close the repository
     */
    public void close() { }


    // ******************************************
    // Internal DSpace utility methods below here
    // ******************************************

    /**
     * Get the community or collection signified by a set spec
     *
     * @param context    DSpace context object
     * @param set        OAI set spec
     * @return  the corresponding community or collection, or null if
     *          no set provided
     */
    private DSpaceObject resolveSet(Context context, String set)
        throws SQLException
    {
        if (set == null)
        {
            return null;
        }
        
        // We can be fairly simple because of our two-level hierarchy.
        // Set spec is community-id[:collection-id]
        int colon = set.indexOf(':');

        if (colon >= 0)
        {
            // We have community & collection IDs
            int collID = Integer.parseInt(set.substring(colon + 1));
            
            return Collection.find(context, collID);
        }
        else
        {
            // Just a community ID
            return Community.find(context, Integer.parseInt(set));
        }
    }


    /**
     * Get information about a single item
     *
     * @param context    DSpace context
     * @param handle     the Handle, as provided by OAI
     *
     * @return  the item info, or null
     */
    private OAIItemInfo getItem(Context context, String handle)
        throws SQLException
    {
        OAIItemInfo itemInfo = null;

        if (handle.startsWith("hdl:"))
        {
            // FIXME: Assume Handle is item
            Item i = (Item) HandleManager.resolveToObject(context,
                handle.substring(4)); // strip hdl: prefix

            if (i != null)
            {
                // Fill out OAI info item object
                itemInfo = new OAIItemInfo();

                itemInfo.item = i;
                itemInfo.handle = handle;
                // FIXME: Assume data.available is there
                DCValue[] dateAvail =
                    i.getDC("date", "available", Item.ANY);
                itemInfo.datestamp = dateAvail[0].value;
                itemInfo.itemID = i.getID();

                // Get the sets
                itemInfo.setSpecs = new LinkedList();
                Collection[] colls = i.getCollections();
                
                for (int colNo = 0; colNo < colls.length; colNo++)
                {
                    Community[] comms = colls[colNo].getCommunities();
                    
                    for (int comNo = 0; comNo < comms.length; comNo++)
                    {
                        // For each container, we add a setSpec of:
                        // community-id:collection-id
                        String setSpec =
                            String.valueOf(comms[comNo].getID()) +
                            ":" +
                            String.valueOf(colls[colNo].getID());

                        itemInfo.setSpecs.add(setSpec);
                    }
                }
            }
        }
        
        return itemInfo;
    }
    

    /**
     * Obtain Handles for a given date range.
     * FIXME: Assumes all in_archive items have date.available set,
     * and have public metadata
     * FIXME: Shouldn't use storage layer directly!
     *
     * @param context    DSpace context
     * @param scope      a Community or Collection, or null
     * @param startDate  start of date range, or null
     * @param endDate    end of date range, or null
     *
     * @return  List of OAIItemInfo objects, without the item field
     */
    private List getHeaderInfo(Context context, DSpaceObject scope,
        String startDate, String endDate)
        throws SQLException
    {
        // SQL to add to the list of tables after the SELECT
        String scopeTableSQL = "";

        // SQL to add to the WHERE clause of the query
        String scopeWhereSQL = "";

        if (scope != null)
        {
            if (scope.getType() == Constants.COMMUNITY)
            {
                // Getting things within a community
                scopeTableSQL = ", community2item";
                scopeWhereSQL = " AND community2item.community_id=" +
                    scope.getID() +
                    " AND community2item.item_id=handle.resource_id";
            }
            else if (scope.getType() == Constants.COLLECTION)
            {
                scopeTableSQL = ", collection2item";
                scopeWhereSQL = " AND collection2item.collection_id=" +
                    scope.getID() +
                    " AND collection2item.item_id=handle.resource_id";
            }
            // Theoretically, no other possibilities, won't bother to check
        }

        // Put together our query
        String query = 
            "SELECT handle.handle, handle.resource_id, dcvalue.text_value FROM handle, dcvalue" +
            scopeTableSQL + " WHERE handle.resource_type_id=" + Constants.ITEM +
            " AND handle.resource_id=dcvalue.item_id AND dcvalue.dc_type_id=" +
            dcTypeID + scopeWhereSQL;
        
        if (startDate != null)
        {
            query = query + " AND dcvalue.text_value >= '" + startDate + "'";
        }
        
        if (endDate != null)
        {
            query = query + " AND dcvalue.text_value <= '" + endDate + "'";
        }

System.err.println("OAI SQL query: " + query);

        // Execute
        TableRowIterator tri = DatabaseManager.query(context, query);
        List infoObjects = new LinkedList();
        
        while (tri.hasNext())
        {
            TableRow row = tri.next();
            OAIItemInfo itemInfo = new OAIItemInfo();
            
            itemInfo.handle = "hdl:" + row.getStringColumn("handle");
            itemInfo.itemID = row.getIntColumn("resource_id");
            itemInfo.datestamp = row.getStringColumn("text_value");

            // Get the sets (communities/collections)
            TableRowIterator containers = DatabaseManager.query(context,
                "SELECT community2collection.community_id, community2collection.collection_id FROM community2collection, collection2item WHERE community2collection.collection_id=collection2item.collection_id AND collection2item.item_id=" +
                itemInfo.itemID);
            
            itemInfo.setSpecs = new LinkedList();
            while (containers.hasNext())
            {
                TableRow containerRow = containers.next();
                
                // For each container, we add a setSpec of:
                // community-id:collection-id
                String setSpec =
                    String.valueOf(containerRow.getIntColumn("community_id")) +
                    ":" +
                    String.valueOf(containerRow.getIntColumn("collection_id"));

                itemInfo.setSpecs.add(setSpec);
            }

            infoObjects.add(itemInfo);
        }
            
        return infoObjects;
    }


    /**
     * Obtain items and Handles with a given date range
     * FIXME: Same caveats as getHandles()
     *
     * @param context    DSpace context
     * @param scope      a Community or Collection, or null
     * @param startDate  start of date range, or null
     * @param endDate    end of date range, or null
     *
     * @return  List of OAIItemInfo objects, without the item field
     */
    private List getItems(Context context, DSpaceObject scope,
        String startDate, String endDate)
        throws SQLException
    {
        // Get header info objects
        List infoObjects = getHeaderInfo(context, scope, startDate, endDate);
        
        // Fill out the item fields
        Iterator i = infoObjects.iterator();
        while (i.hasNext())
        {
            OAIItemInfo itemInfo = (OAIItemInfo) i.next();
            itemInfo.item = Item.find(context, itemInfo.itemID);
        }

        return infoObjects;
    }
}
