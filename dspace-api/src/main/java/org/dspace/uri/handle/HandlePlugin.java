/*
 * HandlePlugin.java
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
package org.dspace.uri.handle;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.uri.ExternalIdentifier;
import org.dspace.uri.ExternalIdentifierType;
import org.dspace.uri.dao.ExternalIdentifierDAO;
import org.dspace.uri.dao.ExternalIdentifierDAOFactory;
import net.handle.hdllib.Encoder;
import net.handle.hdllib.HandleException;
import net.handle.hdllib.HandleStorage;
import net.handle.hdllib.HandleValue;
import net.handle.hdllib.ScanCallback;
import net.handle.hdllib.Util;
import net.handle.util.StreamTable;

/**
 * Extension to the CNRI Handle Server that translates requests to resolve
 * handles into DSpace API calls. The implementation simply stubs out most of
 * the methods, and delegates the rest to the
 * {@link org.dspace.uri.ExternalIdentifierDAO}. This only provides
 * some of the functionality (namely, the resolving of handles to URLs) of the
 * CNRI HandleStorage interface.
 * 
 * <p>
 * This class is intended to be embedded in the CNRI Handle Server. It conforms
 * to the HandleStorage interface that was delivered with Handle Server version
 * 5.2.0.
 * </p>
 * 
 * @author Peter Breton
 * @version $Revision$
 */
public class HandlePlugin implements HandleStorage
{
    /** log4j category */
    private static Logger log = Logger.getLogger(HandlePlugin.class);

    /**
     * Constructor
     */
    public HandlePlugin()
    {
    }

    ////////////////////////////////////////
    // Non-Resolving methods -- unimplemented
    ////////////////////////////////////////

    /**
     * HandleStorage interface method - not implemented.
     */
    public void init(StreamTable st) throws Exception
    {
        // Not implemented
        if (log.isInfoEnabled())
        {
            log.info("Called init (not implemented)");
        }
    }

    /**
     * HandleStorage interface method - not implemented.
     */
    public void setHaveNA(byte[] theHandle, boolean haveit)
            throws HandleException
    {
        // Not implemented
        if (log.isInfoEnabled())
        {
            log.info("Called setHaveNA (not implemented)");
        }
    }

    /**
     * HandleStorage interface method - not implemented.
     */
    public void createHandle(byte[] theHandle, HandleValue[] values)
            throws HandleException
    {
        // Not implemented
        if (log.isInfoEnabled())
        {
            log.info("Called createHandle (not implemented)");
        }
    }

    /**
     * HandleStorage interface method - not implemented.
     */
    public boolean deleteHandle(byte[] theHandle) throws HandleException
    {
        // Not implemented
        if (log.isInfoEnabled())
        {
            log.info("Called deleteHandle (not implemented)");
        }

        return false;
    }

    /**
     * HandleStorage interface method - not implemented.
     */
    public void updateValue(byte[] theHandle, HandleValue[] values)
            throws HandleException
    {
        // Not implemented
        if (log.isInfoEnabled())
        {
            log.info("Called updateValue (not implemented)");
        }
    }

    /**
     * HandleStorage interface method - not implemented.
     */
    public void deleteAllRecords() throws HandleException
    {
        // Not implemented
        if (log.isInfoEnabled())
        {
            log.info("Called deleteAllRecords (not implemented)");
        }
    }

    /**
     * HandleStorage interface method - not implemented.
     */
    public void checkpointDatabase() throws HandleException
    {
        // Not implemented
        if (log.isInfoEnabled())
        {
            log.info("Called checkpointDatabase (not implemented)");
        }
    }

    /**
     * HandleStorage interface method - not implemented.
     */
    public void shutdown()
    {
        // Not implemented
        if (log.isInfoEnabled())
        {
            log.info("Called shutdown (not implemented)");
        }
    }

    /**
     * HandleStorage interface method - not implemented.
     */
    public void scanHandles(ScanCallback callback) throws HandleException
    {
        // Not implemented
        if (log.isInfoEnabled())
        {
            log.info("Called scanHandles (not implemented)");
        }
    }

    /**
     * HandleStorage interface method - not implemented.
     */
    public void scanNAs(ScanCallback callback) throws HandleException
    {
        // Not implemented
        if (log.isInfoEnabled())
        {
            log.info("Called scanNAs (not implemented)");
        }
    }

    ////////////////////////////////////////
    // Resolving methods
    ////////////////////////////////////////

    /**
     * Return the raw values for this handle. This implementation returns a
     * single URL value.
     * 
     * @param theHandle
     *            byte array representation of handle
     * @param indexList
     *            ignored
     * @param typeList
     *            ignored
     * @return A byte array with the raw data for this handle. Currently, this
     *         consists of a single URL value.
     * @exception HandleException
     *                If an error occurs while calling the Handle API.
     */
    public byte[][] getRawHandleValues(byte[] theHandle, int[] indexList,
            byte[][] typeList) throws HandleException
    {
        if (log.isInfoEnabled())
        {
            log.info("Called getRawHandleValues");
        }

        Context context = null;

        try
        {
            if (theHandle == null)
            {
                throw new HandleException(HandleException.INTERNAL_ERROR);
            }

            String handle = Util.decodeString(theHandle);

            context = new Context();

            ExternalIdentifierDAO identifierDAO = ExternalIdentifierDAOFactory.getInstance(context);
            ExternalIdentifier identifier = identifierDAO.retrieve(new HandleType(), handle);

            String url = identifier.getURI().toString();

            if (url == null)
            {
                throw new HandleException(HandleException.HANDLE_DOES_NOT_EXIST);
            }

            HandleValue value = new HandleValue();

            value.setIndex(100);
            value.setType(Util.encodeString("URL"));
            value.setData(Util.encodeString(url));
            value.setTTLType((byte) 0);
            value.setTTL(100);
            value.setTimestamp(100);
            value.setReferences(null);
            value.setAdminCanRead(true);
            value.setAdminCanWrite(false);
            value.setAnyoneCanRead(true);
            value.setAnyoneCanWrite(false);

            List values = new LinkedList();

            values.add(value);

            byte[][] rawValues = new byte[values.size()][];

            for (int i = 0; i < values.size(); i++)
            {
                HandleValue hvalue = (HandleValue) values.get(i);

                rawValues[i] = new byte[Encoder.calcStorageSize(hvalue)];
                Encoder.encodeHandleValue(rawValues[i], 0, hvalue);
            }

            return rawValues;
        }
        catch (HandleException he)
        {
            throw he;
        }
        catch (Exception e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Exception in getRawHandleValues", e);
            }

            throw new HandleException(HandleException.INTERNAL_ERROR);
        }
        finally
        {
            if (context != null)
            {
                try
                {
                    context.complete();
                }
                catch (SQLException sqle)
                {
                }
            }
        }
    }

    /**
     * Return true if we have this handle in storage.
     * 
     * @param theHandle
     *            byte array representation of handle
     * @return True if we have this handle in storage
     * @exception HandleException
     *                If an error occurs while calling the Handle API.
     */
    public boolean haveNA(byte[] theHandle) throws HandleException
    {
        if (log.isInfoEnabled())
        {
            log.info("Called haveNA");
        }

        /*
         * Naming authority Handles are in the form: 0.NA/1721.1234
         * 
         * 0.NA is basically the naming authority for naming authorities. For
         * this simple implementation, we will just check that the prefix
         * configured in dspace.cfg is the one in the request, returning true if
         * this is the case, false otherwise.
         * 
         * FIXME: For more complex Handle situations, this will need enhancing.
         */

        // First, construct a string representating the naming authority Handle
        // we'd expect.
        String expected = "0.NA/"
                + ConfigurationManager.getProperty("handle.prefix");

        // Which authority does the request pertain to?
        String received = Util.decodeString(theHandle);

        // Return true if they match
        return expected.equals(received);
    }

    /**
     * Return all handles in local storage which start with the naming authority
     * handle.
     * 
     * @param theNAHandle
     *            byte array representation of naming authority handle
     * @return All handles in local storage which start with the naming
     *         authority handle.
     * @exception HandleException
     *                If an error occurs while calling the Handle API.
     */
    public Enumeration getHandlesForNA(byte[] theNAHandle)
            throws HandleException
    {
        String naHandle = Util.decodeString(theNAHandle);

        if (log.isInfoEnabled())
        {
            log.info("Called getHandlesForNA for NA " + naHandle);
        }

        Context context = null;

        try
        {
            context = new Context();

            ExternalIdentifierDAO identifierDAO = ExternalIdentifierDAOFactory.getInstance(context);
            List<ExternalIdentifier> eids = identifierDAO.startsWith(new HandleType(), naHandle);
            List<String> handles = new ArrayList<String>();

            for(ExternalIdentifier identifier : eids)
            {
                handles.add(identifier.getValue());
            }

            List results = new LinkedList();

            for (Iterator iterator = handles.iterator(); iterator.hasNext();)
            {
                String handle = (String) iterator.next();

                // Transforms to byte array
                results.add(Util.encodeString(handle));
            }

            return Collections.enumeration(results);
        }
        catch (SQLException sqle)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Exception in getHandlesForNA", sqle);
            }

            throw new HandleException(HandleException.INTERNAL_ERROR);
        }
        finally
        {
            if (context != null)
            {
                try
                {
                    context.complete();
                }
                catch (SQLException sqle)
                {
                }
            }
        }
    }
}
