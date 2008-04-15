/*
 * ExternalIdentifierService.java
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
package org.dspace.uri;

import org.dspace.content.DCValue;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.PluginManager;
import org.dspace.uri.dao.ExternalIdentifierDAO;
import org.dspace.uri.dao.ExternalIdentifierDAOFactory;
import org.dspace.uri.dao.ExternalIdentifierStorageException;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * General static library of methods which offer services to external identification mechanisms.  It
 * encapsulates access to configuration and to the data access layer, and therefore insulates
 * all calling code dealing with identifiers from having to deal with either of these issues
 *
 * @author Richard Jones
 */
public class ExternalIdentifierService
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(ExternalIdentifierService.class);

    /**
     * Mint all of the necessary external identifiers for the given DSpaceObject.  This will use the
     * PluginManager to load all the IdentifierAssigner implementations specified in dspace.cfg
     * and request them to assign identifiers.  These identifiers will then be returned.
     *
     * Generation of an identifier by the underlying mechanism does not guarantee that the identifier
     * will be persisted.  That is up to the caller to insert the identifiers into the DSpaceObject
     * and request its storage.
     *
     * @param context
     * @param dso
     * @return
     */
    public static List<ExternalIdentifier> mintAll(Context context, Identifiable dso)
            throws IdentifierException
    {
        List<ExternalIdentifier> eids = new ArrayList<ExternalIdentifier>();
        IdentifierAssigner[] assigners = (IdentifierAssigner[]) PluginManager.getPluginSequence(IdentifierAssigner.class);
        for (int i = 0; i < assigners.length; i++)
        {
            ExternalIdentifier eid = assigners[i].mint(context, dso);
            eids.add(eid);
        }
        return eids;
    }

    /**
     * Get the type object associated with the given namespace string.  This is useful for hooking into the
     * individual implementations of external identifiers from the namespace string only (i.e. without
     * an identifier value).  The PluginManager will be used to load all the ExternalIdentifierType objects
     * specified in dspace.cfg, find out which, if any, respond to the requested namespace, and return the
     * first instance which does.
     *
     * @param namespace
     * @return
     */
    public static ExternalIdentifierType getType(String namespace)
    {
        ExternalIdentifierType[] eits = (ExternalIdentifierType[]) PluginManager.getPluginSequence(ExternalIdentifierType.class);
        for (int i = 0; i < eits.length; i++)
        {
            if (namespace.equals(eits[i].getNamespace()))
            {
                return eits[i];
            }
        }
        return null;
    }

    /**
     * Get an instance of an external identifier which satisfies all of the criteria of the namespace and the value
     *
     * This will use the ExternalIdentifierService.getTYpe() method to obtain the type, and will then
     * retrieve an ExternalIdentifier object from the Data Access Object layer.  Therefore, the identifier
     * must not only be of the correct form of a registered plugin, but must also exist in the identifier
     * registry, otherwise this method will return null
     *
     * @param context
     * @param namespace
     * @param value
     * @return
     */
    public static ExternalIdentifier get(Context context, String namespace, String value)
            throws IdentifierException
    {
        try
        {
            ExternalIdentifierType eit = ExternalIdentifierService.getType(namespace);
            if (eit == null)
            {
                return null;
            }
            ExternalIdentifierDAO dao = ExternalIdentifierDAOFactory.getInstance(context);
            ExternalIdentifier eid = dao.retrieve(eit, value);
            return eid;
        }
        catch (ExternalIdentifierStorageException e)
        {
            log.error("caught exception: ", e);
            throw new IdentifierException(e);
        }
    }

    /**
     * Construct an ExternalIdentifier object using the given set of values.  This allows us to be sure
     * that our API calls are creating successful external identifiers by verifying each of the parameters
     * rather than just calling the complete constructor for the ExternalIdentifier class
     * 
     * @param context
     * @param type
     * @param value
     * @param oid
     * @return
     */
    public static ExternalIdentifier get(Context context, ExternalIdentifierType type, String value, ObjectIdentifier oid)
    {
        ExternalIdentifierType[] eits = (ExternalIdentifierType[]) PluginManager.getPluginSequence(ExternalIdentifierType.class);
        for (int i = 0; i < eits.length; i++)
        {
            if (type.equals(eits[i]))
            {
                ExternalIdentifier eid = type.getInstance(value, oid);
                return eid;
            }
        }
        return null;
    }

    /**
     * Parse the given identifier on the assumption that it is in canonical form.  This will use the PluginManager
     * to load each registered ExternalIdentifier and ask it to parse the canonical form.  When an identifier
     * implementation agrees that this is a canonical form of one of its allowed identifiers then the
     * actual instance of the identifier can be retrieved from the identifier registry through the data access
     * object layer and returned
     *
     * @param context
     * @param canonicalForm
     * @return
     */
    public static ExternalIdentifier parseCanonicalForm(Context context, String canonicalForm)
            throws IdentifierException
    {
        try
        {
            ExternalIdentifier[] eids = (ExternalIdentifier[]) PluginManager.getPluginSequence(ExternalIdentifier.class);
            for (int i = 0; i < eids.length; i++)
            {
                ExternalIdentifier eid = eids[i].parseCanonicalForm(canonicalForm);
                if (eid != null)
                {
                    ExternalIdentifierDAO dao = ExternalIdentifierDAOFactory.getInstance(context);
                    return dao.retrieve(eid.getType(), eid.getValue());
                }
            }
            return null;
        }
        catch (ExternalIdentifierStorageException e)
        {
            log.error("caught exception: ", e);
            throw new IdentifierException(e);
        }
    }

    /**
     * Parse the given string on the assumption that it is a URL containing an external identifier in URL
     * form, among other things.  That is, that it may be of the form:
     *
     * <code>http://mydspace:8080/resource/hdl/123456789/100/submit</code>
     *
     * And this mechanism will ask each IdentifierResolver implementation registered in dspace.cfg using the
     * PluginManager to check the string for url forms of allowed identifiers.  If an implementation matches
     * the information will be used to extract the actual ExternalIdentifier from the identifier registry through
     * the data access objhect layer and returned.
     *
     * @param context
     * @param path
     * @return
     */
    public static ExternalIdentifier extractURLIdentifier(Context context, String path)
            throws IdentifierException
    {
        try
        {
            IdentifierResolver[] eids = (IdentifierResolver[]) PluginManager.getPluginSequence(IdentifierResolver.class);
            for (int i = 0; i < eids.length; i++)
            {
                ExternalIdentifier eid = eids[i].extractURLIdentifier(path);
                if (eid != null)
                {
                    ExternalIdentifierDAO dao = ExternalIdentifierDAOFactory.getInstance(context);
                    return dao.retrieve(eid.getType(), eid.getValue());
                }
            }
            return null;
        }
        catch (ExternalIdentifierStorageException e)
        {
            log.error("caught exception: ", e);
            throw new IdentifierException(e);
        }
    }

    /**
     * Obtain a DCValue without a value representing the metadata field into which to store the
     * canonical form of the external identifier
     *
     * @param type
     * @return
     */
    public static DCValue getCanonicalField(ExternalIdentifierType type)
    {
        String cfg = "identifier.metadata.canonical-field." + type.getNamespace();
        DCValue dcv = ConfigurationManager.getMetadataProperty(cfg);
        return dcv;
    }

    /**
     * Obtain a DCValue without a value representing the metadata field into which to store the
     * actionable url of the external identifier
     * 
     * @param type
     * @return
     */
    public static DCValue getURLField(ExternalIdentifierType type)
    {
        String cfg = "identifier.metadata.url-field." + type.getNamespace();
        DCValue dcv = ConfigurationManager.getMetadataProperty(cfg);
        return dcv;
    }
}
