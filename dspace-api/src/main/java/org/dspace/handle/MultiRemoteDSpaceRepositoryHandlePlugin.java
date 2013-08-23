/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handle;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.handle.hdllib.Encoder;
import net.handle.hdllib.HandleException;
import net.handle.hdllib.HandleStorage;
import net.handle.hdllib.HandleValue;
import net.handle.hdllib.ScanCallback;
import net.handle.hdllib.Util;
import net.handle.util.StreamTable;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Extension to the CNRI Handle Server that translates requests to resolve
 * handles into remote calls to the mini-DSpace Handle resolver JSON API. This
 * only provides some of the functionality (namely, the resolving of handles to
 * URLs) of the CNRI HandleStorage interface.
 * 
 * <p>
 * This class is intended to be embedded in the CNRI Handle Server. It conforms
 * to the HandleStorage interface that was delivered with Handle Server version
 * 6.2.0.
 * </p>
 * 
 * @author Andrea Bollini
 */
public class MultiRemoteDSpaceRepositoryHandlePlugin implements HandleStorage
{
    /** log4j category */
    private static Logger log = Logger
            .getLogger(MultiRemoteDSpaceRepositoryHandlePlugin.class);

    /**
     * Constructor
     */
    public MultiRemoteDSpaceRepositoryHandlePlugin()
    {
    }

    // //////////////////////////////////////
    // Non-Resolving methods -- unimplemented
    // //////////////////////////////////////

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

    // //////////////////////////////////////
    // Resolving methods
    // //////////////////////////////////////

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

        if (theHandle == null)
        {
            throw new HandleException(HandleException.INTERNAL_ERROR);
        }

        String handle = Util.decodeString(theHandle);
        String url = getRemoteDSpaceURL(handle);
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

        List<HandleValue> values = new LinkedList<HandleValue>();

        values.add(value);

        byte[][] rawValues = new byte[values.size()][];

        for (int i = 0; i < values.size(); i++)
        {
            HandleValue hvalue = values.get(i);

            rawValues[i] = new byte[Encoder.calcStorageSize(hvalue)];
            Encoder.encodeHandleValue(rawValues[i], 0, hvalue);
        }

        return rawValues;

    }

    private String getRemoteDSpaceURL(String handle) throws HandleException
    {
        InputStreamReader jsonStreamReader = null;
        String url = null;
        try
        {
            String prefix = handle.split("/")[0];
            String endpoint = ConfigurationManager
                    .getProperty("handle-resolver.prefix." + prefix);

            String jsonurl = endpoint + "/resolve/" + handle;
            jsonStreamReader = new InputStreamReader(
                    new URL(jsonurl).openStream(), "UTF-8");
            JsonParser parser = new JsonParser();
            JsonElement jsonElement = parser.parse(jsonStreamReader);

            if (jsonElement == null || jsonElement.isJsonNull()
                    || jsonElement.getAsJsonArray().size() == 0)
            {
                throw new HandleException(HandleException.HANDLE_DOES_NOT_EXIST);
            }

            url = jsonElement.getAsJsonArray().get(0).getAsString();
        }
        catch (Exception e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Exception in getRawHandleValues", e);
            }

            // Stack loss as exception does not support cause
            throw new HandleException(HandleException.INTERNAL_ERROR);
        }
        finally
        {
            if (jsonStreamReader != null)
            {
                try
                {
                    jsonStreamReader.close();
                }
                catch (IOException e)
                {
                    log.error(e.getMessage(), e);
                }
            }
        }
        return url;
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
        return true;
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

        List<String> handles = getRemoteDSpaceHandles(naHandle);
        List<byte[]> results = new LinkedList<byte[]>();

        for (String handle : handles)
        {
            // Transforms to byte array
            results.add(Util.encodeString(handle));
        }

        return Collections.enumeration(results);

    }

    private List<String> getRemoteDSpaceHandles(String naHandle)
            throws HandleException
    {
        List<String> handles = new ArrayList<String>();

        String endpoint = ConfigurationManager
                .getProperty("handle-resolver.prefix." + naHandle);

        InputStreamReader jsonStreamReader = null;
        try
        {
            String jsonurl = endpoint + "/listhandles/" + naHandle;
            jsonStreamReader = new InputStreamReader(
                    new URL(jsonurl).openStream(), "UTF-8");
            JsonParser parser = new JsonParser();
            JsonElement jsonElement = parser.parse(jsonStreamReader);

            if (jsonElement != null && jsonElement.getAsJsonArray().size() != 0)
            {
                for (int i = 0; i < jsonElement.getAsJsonArray().size(); i++)
                {
                    handles.add(jsonElement.getAsJsonArray().get(i)
                            .getAsString());
                }
            }
        }
        catch (Exception e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Exception in getHandlesForNA", e);
            }

            // Stack loss as exception does not support cause
            throw new HandleException(HandleException.INTERNAL_ERROR);
        }
        finally
        {
            if (jsonStreamReader != null)
            {
                try
                {
                    jsonStreamReader.close();
                }
                catch (IOException e)
                {
                    log.error(e.getMessage(), e);
                }
            }
        }
        return handles;
    }

    public static void main(String[] args) throws Exception
    {
        MultiRemoteDSpaceRepositoryHandlePlugin multi = new MultiRemoteDSpaceRepositoryHandlePlugin();
        try
        {
            System.out.println(StringUtils.join(
                    multi.getRemoteDSpaceHandles("123456789"), ","));
        }
        catch (HandleException e)
        {
            e.printStackTrace();
        }
        try
        {
            System.out.println(StringUtils.join(
                    multi.getRemoteDSpaceHandles("123456780"), ","));
        }
        catch (HandleException e)
        {
            e.printStackTrace();
        }
        try
        {
            System.out.println(multi.getRemoteDSpaceURL("123456789/1"));
        }
        catch (HandleException e)
        {
            e.printStackTrace();
        }
        try
        {
            System.out.println(multi.getRemoteDSpaceURL("123456789/1111111"));
        }
        catch (HandleException e)
        {
            e.printStackTrace();
        }
        try
        {
            System.out.println(multi.getRemoteDSpaceURL("123456780/1"));
        }
        catch (HandleException e)
        {
            e.printStackTrace();
        }
        try
        {
            System.out.println(multi.getRemoteDSpaceURL("123456780/1111111"));
        }
        catch (HandleException e)
        {
            e.printStackTrace();
        }
    }
}
