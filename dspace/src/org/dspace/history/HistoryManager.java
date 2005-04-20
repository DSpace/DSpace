/*
 * HistoryManager.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
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
package org.dspace.history;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.eperson.EPerson;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.workflow.WorkflowItem;

import com.hp.hpl.mesa.rdf.jena.mem.ModelMem;
import com.hp.hpl.mesa.rdf.jena.model.Model;
import com.hp.hpl.mesa.rdf.jena.model.Property;
import com.hp.hpl.mesa.rdf.jena.model.RDFException;
import com.hp.hpl.mesa.rdf.jena.model.Resource;

/**
 * Records information about changes in DSpace. The information about changes is
 * written out in RDF format to one or more files in the directory given by the
 * configuration property <em>history.dir</em>.
 * 
 * @author Peter Breton
 * @version $Revision$
 */
public class HistoryManager
{
    // Action constants
    public static final int NONE = 0;

    public static final int CREATE = 1;

    public static final int MODIFY = 2;

    public static final int REMOVE = 3;

    private static final String uriPrefixConfig = ConfigurationManager
            .getProperty("history.uri.prefix");

    /** URI prefix */
    private static final String uriPrefix = (uriPrefixConfig != null) ? uriPrefixConfig
            : "http://www.dspace.org";

    /** Handle prefix */
    private static String handlePrefix = ConfigurationManager
            .getProperty("handle.prefix");

    /** log4j category */
    private static Logger log = Logger.getLogger(HistoryManager.class);

    /** Directory for history serialization */
    private static String historyDirectory = ConfigurationManager
            .getProperty("history.dir");

    // These settings control the way an identifier is hashed into
    // directory and file names
    // FIXME: This is basically stolen from the bitstore code, and thus
    // could be factored out into a common location
    private static int digitsPerLevel = 2;

    private static int directoryLevels = 3;

    /** Identifier for the generator */
    private static final String ID = new StringBuffer().append(
            HistoryManager.class.getName()).append(" ").append(
            "$Revision$").toString();

    /**
     * Private Constructor
     */
    private HistoryManager()
    {
    }

    /**
     * Save history information about this object. Errors are simply logged.
     * 
     * @param context
     *            The current DSpace context
     * @param obj
     *            The object to record History information about
     * @param flag
     *            One of CREATE, MODIFY or REMOVE.
     * @param user
     *            The user who performed the action that is being recorded
     * @param tool
     *            A description of the tool that was used to effect the action.
     */
    public static void saveHistory(Context context, Object obj, int flag,
            EPerson user, String tool)
    {
        try
        {
            createHarmonyData(context, obj, flag, user, tool);
        }
        catch (Exception e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Exception while saving history", e);
            }
        }
    }

    /**
     * Create Harmony data which describes the event.
     * 
     * @param context
     *            The current DSpace context
     * @param obj
     *            The object to record History information about
     * @param flag
     *            One of CREATE, MODIFY or REMOVE.
     * @param user
     *            The user who performed the action that is being recorded
     * @param tool
     *            A description of the tool that was used to effect the action.
     */
    private static void createHarmonyData(Context context, Object historyObj,
            int flag, EPerson theUser, String theTool) throws SQLException,
            RDFException, IOException
    {
        String id = (flag == REMOVE) ? getUniqueId(historyObj) : doSerialize(
                context, historyObj);

        // Figure out the tool used
        if (theTool == null)
        {
            theTool = getTool();
        }

        String eventId = getUniqueId(getShortName(historyObj), flag);

        // Previous state (as far as History is concerned). It's possible,
        // of course, that changes were made without telling History!
        String inputStateId = (flag == CREATE) ? null : findPreviousState(id);

        // Create a model
        Model model = new ModelMem();

        // A table row and id for the new state
        Integer stateId = null;
        TableRow row = null;

        if (flag != REMOVE)
        {
            row = DatabaseManager.create(context, "HistoryState");
            stateId = new Integer(row.getIntColumn("history_state_id"));
        }

        // This is the object that we're making statements about....
        Resource obj = model.createResource(id);

        // This is the event
        Resource event = model.createResource(eventId);

        //  States
        Resource outputState = (flag == REMOVE) ? null : model
                .createResource(stateId.toString());
        Resource inputState = (flag == CREATE) ? null : model
                .createResource(inputStateId);

        //  FIXME The action (also typed??)
        Resource action = model.createResource(getUniqueId("action", NONE));

        //  The user
        Resource user = (theUser != null) ? model
                .createResource(getUniqueId(theUser)) : null;

        // These verbs are essentially constant
        Property atTime = model.createProperty(getHarmonyId("atTime"));
        Property hasInput = model.createProperty(getHarmonyId("hasInput"));
        Property hasOutput = model.createProperty(getHarmonyId("hasOutput"));
        Property inState = model.createProperty(getHarmonyId("inState"));

        //Property contains = model.createProperty(getHarmonyId("contains"));
        Property hasAction = model.createProperty(getHarmonyId("hasAction"));
        Property usesTool = model.createProperty(getHarmonyId("usesTool"));
        Property hasAgent = model.createProperty(getHarmonyId("hasAgent"));
        Property operation = null;

        // Choose the correct operation
        if (flag == CREATE)
        {
            operation = model.createProperty(getHarmonyId("creates"));
        }
        else if (flag == REMOVE)
        {
            operation = model.createProperty(getHarmonyId("destroys"));
        }
        else if (flag == MODIFY)
        {
            operation = model.createProperty(getHarmonyId("transforms"));
        }
        else
        {
            throw new IllegalArgumentException("Unknown value for flag: "
                    + flag);
        }

        // Creation events do not have input states, but everything
        // else does
        if (flag != CREATE)
        {
            model.add(event, hasInput, inputState);
        }

        // Removal events do not have output states, nor is the object
        // in a state upon completion
        if (flag != REMOVE)
        {
            model.add(event, hasOutput, outputState);
            model.add(obj, inState, outputState);

            //model.add(outputState, contains, obj);
        }

        // Time that this event occurred
        model.add(event, atTime, (new java.util.Date().toString()));

        model.add(action, operation, obj);
        model.add(event, hasAction, action);
        model.add(action, usesTool, theTool);

        if (theUser != null)
        {
            model.add(action, hasAgent, user);
        }
        else
        {
            model.add(action, hasAgent, model.createLiteral("Unknown User"));
        }

        //  FIXME Strictly speaking, this is NOT a property of the
        //  object itself, but of the resulting serialization!
        Property generatorId = model.createProperty(uriPrefix + "/generator");

        model.add(event, generatorId, ID);

        List dbobjs = new LinkedList();

        if (flag != REMOVE)
        {
            row.setColumn("history_state_id", stateId.intValue());
            row.setColumn("object_id", id);
            DatabaseManager.update(context, row);
        }

        StringWriter swdata = new StringWriter();

        model.write(swdata);
        swdata.close();

        String data = swdata.toString();

        TableRow h = DatabaseManager.create(context, "History");
        int hid = h.getIntColumn("history_id");

        File file = forId(hid, true);
        FileWriter fw = new FileWriter(file);

        fw.write(data);
        fw.close();

        h.setColumn("creation_date", nowAsTimeStamp());
        h.setColumn("checksum", Utils.getMD5(data));
        DatabaseManager.update(context, h);
    }

    ////////////////////////////////////////
    // Unique ids
    ////////////////////////////////////////

    /**
     * Return a unique id for an object.
     * 
     * @param obj
     *            The object to return an id for
     * @return A unique id for the object.
     */
    private static String getUniqueId(Object obj)
    {
        if (obj == null)
        {
            return null;
        }

        int id = -1;

        //  FIXME This would be easier there were a ContentObject
        //  interface/base class
        if (obj instanceof Community)
        {
            id = ((Community) obj).getID();
        }
        else if (obj instanceof Collection)
        {
            id = ((Collection) obj).getID();
        }
        else if (obj instanceof Item)
        {
            id = ((Item) obj).getID();
        }
        else if (obj instanceof EPerson)
        {
            id = ((EPerson) obj).getID();
        }
        else if (obj instanceof WorkspaceItem)
        {
            id = ((WorkspaceItem) obj).getID();
        }
        else if (obj instanceof WorkflowItem)
        {
            id = ((WorkflowItem) obj).getID();
        }

        return getUniqueIdInternal(uriPrefix, handlePrefix, getShortName(obj),
                Integer.toString(id));
    }

    /**
     * Return a unique id corresponding to a Harmony object.
     * 
     * @param name
     *            The name of a Harmony object (action, event, etc)
     * @param flag
     *            One of CREATE, MODIFY, REMOVE
     * @return A unique id
     */
    private static String getUniqueId(String name, int flag)
    {
        String objname = new StringBuffer("harmony").append("/").append(name)
                .append((flag == CREATE) ? "create" : "").append(
                        (flag == MODIFY) ? "modify" : "").append(
                        (flag == REMOVE) ? "remove" : "").toString();

        return getUniqueIdInternal(uriPrefix, null, objname, Utils
                .generateHexKey());
    }

    /**
     * Return an RDF property id.
     * 
     * @param objname
     *            The name of an object
     * @param property
     *            The name of a property of the objecty
     * @return The id for the property
     */
    private static String getPropertyId(String objname, String property)
    {
        return getUniqueIdInternal(uriPrefix, null, objname, property);
    }

    /**
     * Return an RDF property id for Harmony property.
     * 
     * @param property
     *            The name of a Harmony property
     * @return The id for the property
     */
    private static String getHarmonyId(String property)
    {
        return getUniqueIdInternal(uriPrefix, null, "harmony", property);
    }

    /**
     * Internal method for id generation.
     * 
     * @param uriPrefix
     *            A URI
     * @param handlePrefix
     * @param objname
     * @param objid
     * @return A unique id
     */
    private static String getUniqueIdInternal(String uriPrefix,
            String handlePrefix, String objname, String objid)
    {
        final String SLASH = "/";

        return new StringBuffer().append(uriPrefix).append(
                uriPrefix.endsWith(SLASH) ? "" : SLASH).append(objname).append(
                objname.endsWith(SLASH) ? "" : SLASH).append(
                (handlePrefix == null) ? "" : handlePrefix).append(
                ((handlePrefix == null) || (handlePrefix.endsWith(SLASH))) ? ""
                        : SLASH).append(objid).toString();
    }

    ////////////////////////////////////////
    // Serialize methods
    ////////////////////////////////////////

    /**
     * Return an RDF stream for this object.
     * 
     * @param context
     *            Current DSpace context
     * @param obj
     *            The object to serialize
     * @return The serialization of the object
     * @exception RDFException
     *                If an error occurs while constructing an RDF graph
     * @exception SQLException
     *                If an error occurs while accessing the database
     */
    private static String serialize(Context context, Object obj)
            throws RDFException, SQLException
    {
        if (obj == null)
        {
            return null;
        }

        Model model = new ModelMem();

        // Add statements about this object
        serializeInternal(context, obj, model);

        StringWriter data = new StringWriter();

        model.write(data);

        // Since this is all in-memory, IOExceptions should never happen
        try
        {
            data.close();
        }
        catch (IOException ioe)
        {
        }

        return data.toString();
    }

    /**
     * Add RDF statements about this object to the model.
     * 
     * @param context
     *            Current DSpace context
     * @param obj
     *            The object to make statements about
     * @param model
     *            The RDF statement graph
     * @exception RDFException
     *                If an error occurs while constructing an RDF graph
     * @exception SQLException
     *                If an error occurs while accessing the database
     */
    private static void serializeInternal(Context context, Object obj,
            Model model) throws RDFException, SQLException
    {
        if (obj == null)
        {
            return;
        }

        String id = getUniqueId(obj);
        Resource res = model.createResource(id);

        //  FIXME Strictly speaking, this is NOT a property of the
        //  object itself, but of the resulting serialization!
        Property generatorId = model.createProperty(uriPrefix + "/generator");

        model.add(res, generatorId, ID);

        if (obj instanceof Community)
        {
            addData(context, (Community) obj, res, model);
        }
        else if (obj instanceof Collection)
        {
            addData(context, (Collection) obj, res, model);
        }
        else if (obj instanceof Item)
        {
            addData(context, (Item) obj, res, model);
        }
        else if (obj instanceof WorkspaceItem)
        {
            addData(context, (WorkspaceItem) obj, res, model);
        }
        else if (obj instanceof WorkflowItem)
        {
            addData(context, (WorkflowItem) obj, res, model);
        }
        else if (obj instanceof EPerson)
        {
            addData(context, (EPerson) obj, res, model);
        }
    }

    /**
     * Serialize and store an object.
     * 
     * @param context
     *            Current DSpace context.
     * @param obj
     *            The object to serialize and store.
     * @return A unique id for the object.
     * @exception RDFException
     *                If an error occurs while constructing an RDF graph
     * @exception IOException
     *                If an error occurs while storing the serialization
     * @exception SQLException
     *                If an error occurs while accessing the database
     */
    private static String doSerialize(Context context, Object obj)
            throws SQLException, IOException, RDFException
    {
        if (obj == null)
        {
            return null;
        }

        String id = getUniqueId(obj);
        String serialization = serialize(context, obj);

        store(context, serialization);

        return id;
    }

    /**
     * Store the serialization (unless it already exists).
     * 
     * @param context
     *            Current DSpace context.
     * @param serialization
     *            The serialization to store.
     * @exception IOException
     *                If an error occurs while storing the serialization
     * @exception SQLException
     *                If an error occurs while accessing the database
     */
    private static void store(Context context, String serialization)
            throws SQLException, IOException
    {
        String checksum = Utils.getMD5(serialization);
        TableRow row = DatabaseManager.findByUnique(context, "history",
                "checksum", checksum);

        // Already stored
        if (row != null)
        {
            return;
        }

        TableRow h = DatabaseManager.create(context, "History");
        int hid = h.getIntColumn("history_id");

        File file = forId(hid, true);
        FileWriter fw = new FileWriter(file);

        fw.write(serialization);
        fw.close();

        h.setColumn("checksum", checksum);
        h.setColumn("creation_date", nowAsTimeStamp());
        DatabaseManager.update(context, h);
    }

    /**
     * Return the last state for the object with id, or null.
     * 
     * @param id
     *            The object's history id
     * @exception SQLException
     *                If an error occurs while accessing the database
     */
    private static String findPreviousState(String id) throws SQLException
    {
        Connection connection = null;
        PreparedStatement statement = null;

        try
        {
            String sql = "SELECT MAX(history_state_id) FROM HistoryState WHERE object_id = ?";

            connection = DatabaseManager.getConnection();
            statement = connection.prepareStatement(sql);
            statement.setString(1, id);

            ResultSet results = statement.executeQuery();

            return results.next() ? results.getString(1) : null;
        }
        finally
        {
            if (statement != null)
            {
                statement.close();
            }

            if (connection != null)
            {
                connection.close();
            }
        }
    }

    ////////////////////////////////////////
    // addData methods
    ////////////////////////////////////////

    /**
     * Add community-specific data to the model.
     * 
     * @param context
     *            Current DSpace context.
     * @param community
     *            The community
     * @param res
     *            RDF resource for the community
     * @param model
     *            The RDF graph
     * @exception RDFException
     *                If an error occurs while constructing an RDF graph
     * @exception SQLException
     *                If an error occurs while accessing the database
     */
    private static void addData(Context context, Community community,
            Resource res, Model model) throws RDFException, SQLException
    {
        String shortname = getShortName(community);
        model.add(res, model.createProperty(getPropertyId(shortname, "ID")),
                community.getID());

        String[] metadata = new String[] { "name", "short_description",
                "introductory_text", "copyright_text", "side_bar_text" };

        for (int i = 0; i < metadata.length; i++)
        {
            String meta = metadata[i];
            addMetadata(model, res, shortname, meta, community
                    .getMetadata(meta));
        }

        Property hasPart = model.createProperty(getHarmonyId("hasPart"));
        Collection[] collections = community.getCollections();

        for (int i = 0; i < collections.length; i++)
        {
            model.add(res, hasPart, getUniqueId(collections[i]));
        }
    }

    /**
     * Add collection-specific data to the model.
     * 
     * @param context
     *            Current DSpace context.
     * @param collection
     *            The collection
     * @param res
     *            RDF resource for the collection
     * @param model
     *            The RDF graph
     * @exception RDFException
     *                If an error occurs while constructing an RDF graph
     * @exception SQLException
     *                If an error occurs while accessing the database
     */
    private static void addData(Context context, Collection collection,
            Resource res, Model model) throws SQLException, RDFException
    {
        String shortname = getShortName(collection);

        model.add(res, model.createProperty(getPropertyId(shortname, "ID")),
                collection.getID());
        model.add(res, model
                .createProperty(getPropertyId(shortname, "license")),
                collection.getLicense());

        String[] metadata = new String[] { "name", "short_description",
                "introductory_text", "copyright_text", "side_bar_text",
                "provenance_description" };

        for (int i = 0; i < metadata.length; i++)
        {
            String meta = metadata[i];
            addMetadata(model, res, shortname, meta, collection
                    .getMetadata(meta));
        }

        Property hasPart = model.createProperty(getHarmonyId("hasPart"));
        ItemIterator items = collection.getItems();

        while (items.hasNext())
        {
            Item item = items.next();

            model.add(res, hasPart, getUniqueId(item));
        }
    }

    /**
     * Add item-specific data to the model.
     * 
     * @param context
     *            Current DSpace context.
     * @param item
     *            The item
     * @param res
     *            RDF resource for the item
     * @param model
     *            The RDF graph
     * @exception RDFException
     *                If an error occurs while constructing an RDF graph
     * @exception SQLException
     *                If an error occurs while accessing the database
     */
    private static void addData(Context context, Item item, Resource res,
            Model model) throws RDFException, SQLException
    {
        DCValue[] dcfields = item.getDC(Item.ANY, Item.ANY, Item.ANY);

        for (int i = 0; i < dcfields.length; i++)
        {
            DCValue dc = dcfields[i];
            String element = dc.element;
            String qualifier = dc.qualifier;

            String type = new StringBuffer().append(element).append(
                    (qualifier == null) ? "" : ".").append(
                    (qualifier == null) ? "" : qualifier).toString();

            Property p = model
                    .createProperty(uriPrefix + "/dublincore/" + type);
            model.add(res, p, dc.value);
        }

        //  FIXME Ignoring Bundles for now, as they simply hold
        //  bitstreams in the Early Adopters release.
        //  When Bundles have their own metadata, they should be recorded
        Property hasPart = model.createProperty(getHarmonyId("hasPart"));

        //  FIXME Not clear that we should ignore the internal bitstreams
        Bitstream[] bitstreams = item.getNonInternalBitstreams();

        for (int i = 0; i < bitstreams.length; i++)
        {
            Bitstream bitstream = bitstreams[i];

            model.add(res, hasPart, getUniqueId(bitstream));

            // Serialize the bitstream's metadata
            serializeInternal(context, bitstream, model);
        }
    }

    /**
     * Add workspace-item-specific data to the model.
     * 
     * @param context
     *            The current DSpace context
     * @param wi
     *            The WorkspaceItem
     * @param res
     *            The RDF Resource representing the WorkspaceItem
     * @param model
     *            The RDF Model
     * @exception SQLException
     *                If an error occurs reading data about the WorkspaceItem
     *                from the database
     * @exception RDFException
     *                If an error occurs adding RDF statements to the model
     */
    private static void addData(Context context, WorkspaceItem wi,
            Resource res, Model model) throws SQLException, RDFException
    {
        Item item = Item.find(context, wi.getItem().getID());

        serializeInternal(context, item, model);
    }

    /**
     * Add workflow-item-specific data to the model.
     * 
     * @param context
     *            The current DSpace context
     * @param wi
     *            The WorkflowItem
     * @param res
     *            The RDF Resource representing the WorkflowItem
     * @param model
     *            The RDF Model
     * @exception SQLException
     *                If an error occurs reading data about the WorkflowItem
     *                from the database
     * @exception RDFException
     *                If an error occurs adding RDF statements to the model
     */
    private static void addData(Context context, WorkflowItem wi, Resource res,
            Model model) throws SQLException, RDFException
    {
        Item item = Item.find(context, wi.getItem().getID());

        serializeInternal(context, item, model);
    }

    /**
     * Add eperson-specific data to the model.
     * 
     * @param context
     *            The current DSpace context
     * @param eperson
     *            The EPerson
     * @param res
     *            The RDF Resource representing the EPerson
     * @param model
     *            The RDF Model
     * @exception SQLException
     *                If an error occurs reading data about the EPerson from the
     *                database
     * @exception RDFException
     *                If an error occurs adding RDF statements to the model
     */
    private static void addData(Context context, EPerson eperson, Resource res,
            Model model) throws SQLException, RDFException
    {
        String shortname = getShortName(eperson);
        model.add(res, model.createProperty(getPropertyId(shortname, "ID")),
                eperson.getID());
        model.add(res, model.createProperty(getPropertyId(shortname, "email")),
                eperson.getEmail());
        model.add(res, model.createProperty(getPropertyId(shortname,
                "firstname")), eperson.getFirstName());
        model.add(res, model
                .createProperty(getPropertyId(shortname, "lastname")), eperson
                .getLastName());
        model.add(res,
                model.createProperty(getPropertyId(shortname, "active")),
                eperson.canLogIn());
        model.add(res, model.createProperty(getPropertyId(shortname,
                "require_certificate")), eperson.getRequireCertificate());

        String[] metadata = new String[] { "phone" };

        for (int i = 0; i < metadata.length; i++)
        {
            String meta = metadata[i];
            addMetadata(model, res, shortname, meta, eperson.getMetadata(meta));
        }
    }

    /**
     * Convenience method to add a metadata statement to a model.
     * 
     * @param model
     *            The RDF graph
     * @param res
     *            RDF resource
     * @param object
     *            The name of the object
     * @param property
     *            The name of a property of the object
     * @param value
     *            The value of the property
     * @exception RDFException
     *                If an error occurs while constructing an RDF graph
     */
    private static void addMetadata(Model model, Resource res, String object,
            String property, String value) throws RDFException
    {
        if (value == null)
        {
            return;
        }

        model.add(res, model.createProperty(getPropertyId(object, property)),
                value);
    }

    ////////////////////////////////////////
    // File storage methods (stolen from BitstreamStorageManager)
    ////////////////////////////////////////

    /**
     * Return the File corresponding to id.
     * 
     * @param id
     *            The id
     * @param create
     *            If true, the file will be created if it does not exist.
     *            Otherwise, null will be returned.
     * @return The File corresponding to id
     * @exception IOException
     *                If a filesystem error occurs while determining the
     *                corresponding file.
     */
    private static File forId(int id, boolean create) throws IOException
    {
        File file = new File(id2Filename(id));

        if (!file.exists())
        {
            if (!create)
            {
                return null;
            }

            File parent = file.getParentFile();

            if (!parent.exists())
            {
                parent.mkdirs();
            }

            file.createNewFile();
        }

        return file;
    }

    /**
     * Return the filename corresponding to id.
     * 
     * @param id
     *            The id
     * @return The filename corresponding to id
     * @exception IOException
     *                If a filesystem error occurs while determining the name.
     */
    private static String id2Filename(int id) throws IOException
    {
        NumberFormat nf = NumberFormat.getInstance();

        // Do not use commas to separate digits
        nf.setGroupingUsed(false);

        // Ensure numbers are at least 8 digits
        nf.setMinimumIntegerDigits(8);

        String ID = nf.format(new Integer(id));

        // Start with the root directory
        StringBuffer result = new StringBuffer().append(historyDirectory);

        // Split the id into groups
        for (int i = 0; i < directoryLevels; i++)
        {
            int digits = i * digitsPerLevel;

            result.append(File.separator).append(
                    ID.substring(digits, digits + digitsPerLevel));
        }

        // Lastly, add the id itself
        String theName = result.append(File.separator).append(id).toString();

        if (log.isDebugEnabled())
        {
            log.debug("Filename for " + id + " is " + theName);
        }

        return theName;
    }

    ////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////

    /**
     * Return a basic idea of which tool was used to do something.
     * 
     * @return A String indicating the tool used
     */
    private static String getTool()
    {
        String stacktrace = getStackTrace();

        // Find the highest stack frame that contains org.dspace.
        try
        {
            BufferedReader reader = new BufferedReader(new StringReader(
                    stacktrace));
            String line = null;
            String match = null;

            while ((line = reader.readLine()) != null)
            {
                if (line.indexOf("org.dspace") != -1)
                {
                    match = line.trim();
                }
            }

            // If nothing matched, maybe the stacktrace will give a clue
            return (match == null) ? stacktrace : match;
        }
        // Should never get here -- no real I/O
        catch (Exception e)
        {
            return stacktrace;
        }
    }

    /**
     * Return a String containing the stack trace of the caller.
     * 
     * @return A String containing the stack trace of the caller.
     */
    public static String getStackTrace()
    {
        StringWriter writer = new StringWriter();

        new Throwable().printStackTrace(new PrintWriter(writer));

        return writer.toString();
    }

    /**
     * Return the unqualified classname for object.
     * 
     * @param obj
     *            The object to check
     * @return The object's unqualified classname
     */
    private static String getShortName(Object obj)
    {
        if (obj == null)
        {
            return null;
        }

        String classname = obj.getClass().getName();
        int index = classname.lastIndexOf(".");

        return (index == -1) ? classname : classname.substring(index + 1);
    }

    /**
     * Return the current instant as an SQL timestamp.
     * 
     * @return The current instant as an SQL timestamp.
     */
    private static Timestamp nowAsTimeStamp()
    {
        return new java.sql.Timestamp(new java.util.Date().getTime());
    }

    ////////////////////////////////////////
    // Main
    ////////////////////////////////////////

    /**
     * Embedded test harness
     * 
     * @param argv -
     *            Command-line arguments
     */
    public static void main(String[] argv)
    {
        Context context = null;

        try
        {
            context = new Context();

            Community c = Community.find(context, 1);

            if (c != null)
            {
                System.out.println("Got unique id " + getUniqueId(c));

                saveHistory(context, c, CREATE, null, null);
            }

            Collection collection = Collection.find(context, 3);

            if (collection != null)
            {
                System.out.println("Got unique id " + getUniqueId(collection));

                saveHistory(context, collection, CREATE, null, null);
            }

            // New eperson
            String email = "historytestuser@HistoryManager";
            EPerson nep = EPerson.findByEmail(context, email);

            if (nep == null)
            {
                nep = EPerson.create(context);
            }

            nep.setFirstName("History");
            nep.setEmail(email);
            nep.update();

            saveHistory(context, nep, CREATE, null, null);

            // Change some things around
            nep.setFirstName("History Test");
            nep.setLastName("User");
            nep.update();

            saveHistory(context, nep, MODIFY, null, null);

            Item item = Item.find(context, 1);

            if (item != null)
            {
                saveHistory(context, item, CREATE, null, null);
            }

            System.exit(0);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (context != null)
            {
                context.abort();
            }
        }
    }
}
