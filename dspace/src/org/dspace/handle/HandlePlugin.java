/*
 * HandlePlugin.java
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

package org.dspace.handle;


import java.net.URL;
import java.sql.*;
import java.util.*;

import net.handle.hdllib.*;
import net.handle.util.StringUtils;
import net.handle.util.StreamTable;

import org.apache.log4j.Logger;

import org.dspace.core.Context;
import org.dspace.handle.HandleManager;


/**
 * "Storage" class which resolves handles. The implementation
 * simply stubs out most requests, and delegates the rest
 * to the HandleManager class.
 *
 * @author  Peter Breton
 * @version $Revision$
 */
public class HandlePlugin implements HandleStorage
{
    /** log4j category */
    private static Logger log = Logger.getLogger(HandlePlugin.class);

    ////////////////////////////////////////
    // Non-Resolving methods -- unimplemented
    ////////////////////////////////////////

    public void init(StreamTable st)
        throws Exception
    {
        // Not implemented
        if (log.isInfoEnabled())
            log.info("Called init (not implemented)");
    }

    public void setHaveNA(byte[] theHandle, boolean haveit)
        throws HandleException
    {
        // Not implemented
        if (log.isInfoEnabled())
            log.info("Called setHaveNA (not implemented)");
    }

    public void createHandle(byte[] theHandle, HandleValue[] values)
        throws HandleException
    {
        // Not implemented
        if (log.isInfoEnabled())
            log.info("Called createHandle (not implemented)");
    }

    public boolean deleteHandle(byte[] theHandle)
        throws HandleException
    {
        // Not implemented
        if (log.isInfoEnabled())
            log.info("Called deleteHandle (not implemented)");
        return false;
    }

    public void updateValue(byte[] theHandle, HandleValue[] values)
        throws HandleException
    {
        // Not implemented
        if (log.isInfoEnabled())
            log.info("Called updateValue (not implemented)");
    }

    public void deleteAllRecords()
        throws HandleException
    {
        // Not implemented
        if (log.isInfoEnabled())
            log.info("Called deleteAllRecords (not implemented)");
    }

    public void checkpointDatabase()
        throws HandleException
    {
        // Not implemented
        if (log.isInfoEnabled())
            log.info("Called checkpointDatabase (not implemented)");
    }

    public void shutdown()
    {
        // Not implemented
        if (log.isInfoEnabled())
            log.info("Called shutdown (not implemented)");
    }

    public void scanHandles(ScanCallback callback)
        throws HandleException
    {
        // Not implemented
        if (log.isInfoEnabled())
            log.info("Called scanHandles (not implemented)");
    }

    public void scanNAs(ScanCallback callback)
        throws HandleException
    {
        // Not implemented
        if (log.isInfoEnabled())
            log.info("Called scanNAs (not implemented)");
    }

    ////////////////////////////////////////
    // Resolving methods
    ////////////////////////////////////////

    /**
     * Return the raw values for this handle.
     * The implementation returns a single URL value.
     */
    public byte[][] getRawHandleValues(byte[] theHandle,
        int[] indexList,
        byte[][] typeList)
        throws HandleException
    {
        if (log.isInfoEnabled())
            log.info("Called getRawHandleValues");

        Context context = null;

        try
        {
            if (theHandle == null)
                throw new HandleException(HandleException.INTERNAL_ERROR);

            String handle = Util.decodeString(theHandle);

            context = new Context();
            String url = HandleManager.resolveToURL(context, handle);
            if (url == null)
                throw new HandleException(HandleException.HANDLE_DOES_NOT_EXIST);

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

            byte rawValues[][] = new byte[values.size()][];

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
                log.debug("Exception in getRawHandleValues", e);

            throw new HandleException(HandleException.INTERNAL_ERROR);
        }
        finally
        {
            if (context != null)
                try
                {
                    context.complete();
                }
                catch (SQLException sqle) {}
        }
    }

    /**
     * True if we have this handle in storage
     */
    public boolean haveNA(byte[] theHandle)
        throws HandleException
    {
        if (log.isInfoEnabled())
            log.info("Called haveNA");

        Context context = null;

        try
        {
            context = new Context();
            return HandleManager.resolveToURL
                (context,
                 Util.decodeString(theHandle)) != null;
        }
        catch (Exception e)
        {
            if (log.isDebugEnabled())
                log.debug("Exception in haveNA", e);

            throw new HandleException(HandleException.INTERNAL_ERROR);
        }
        finally
        {
            if (context != null)
                try
                {
                    context.complete();
                }
                catch (SQLException sqle) {}
        }
    }

    /**
     * Return all handles which start with theNAHandle.
     */
    public Enumeration getHandlesForNA(byte[] theNAHandle)
        throws HandleException
    {
        String naHandle = Util.decodeString(theNAHandle);

        if (log.isInfoEnabled())
            log.info("Called getHandlesForNA for NA " + naHandle);

        Context context = null;

        try
        {
            context = new Context();
            List handles = HandleManager.getHandlesForPrefix(context, naHandle);
            List results = new LinkedList();

            for (Iterator iterator = handles.iterator();
                 iterator.hasNext(); )
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
                log.debug("Exception in getHandlesForNA", sqle);

            throw new HandleException(HandleException.INTERNAL_ERROR);
        }
        finally
        {
            if (context != null)
                try
                {
                    context.complete();
                }
                catch (SQLException sqle) {}
        }
    }

}
