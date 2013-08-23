/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handle;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
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

import org.apache.log4j.Logger;

/**
 * Extension to the CNRI Handle Server that translates requests to resolve
 * handles using DSpaces JSON API for handle resolving. The implementation
 * simply stubs out most of the methods, and delegates the rest to the
 * {@link org.dspace.handle.HandleManager}. This only provides some of the
 * functionality (namely, the resolving of handles to URLs) of the CNRI
 * HandleStorage interface.
 * 
 * <p>
 * This class is intended to be embedded in the CNRI Handle Server. It conforms
 * to the HandleStorage interface that was delivered with Handle Server version
 * 5.2.0.
 * </p>
 * 
 * @author Peter Breton
 * @author Pascal-Nicolas Becker
 * @version $Revision$
 */
public class ExternalHandlePlugin implements HandleStorage
{
    /** log4j category */
    private static Logger log = Logger.getLogger(ExternalHandlePlugin.class);

    List<String> urllist;
    
    /**
     * Constructor
     */
    public ExternalHandlePlugin()
    {
        String urls = System.getProperty("dspace.handle.urls");
        if (urls == null || urls.isEmpty())
        {
            throw new RuntimeException("Can't load the URLs needed to connect to your DSpace isntances. "
                    + "Please run again and set them using java ... -Ddspace.handle.urls='url1, url2, ...'.");
        }
        
        this.urllist = new LinkedList<String>();
        
        for (String url : urls.split(","))
        {
            url = url.trim();
            if (!url.isEmpty())
            {
                try {
                    // Check for valid URLs on startup.
                    new URL(url);
                    urllist.add(url);
                    log.debug("Found url: " + url + ".");
                } catch (MalformedURLException ex) {
                    log.warn("Ingnogring URL '" + url + "' as it triggers a MalformedURLException:", ex);
                }
            }
        }
        
        if (this.urllist.isEmpty())
        {
            throw new RuntimeException("Cannot parse system property DSPACE_HANDLE_URLS.");
        }
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

        if (theHandle == null)
        {
            throw new HandleException(HandleException.INTERNAL_ERROR);
        }

        String handle = Util.decodeString(theHandle);

        Gson gson = new Gson();
        String handleurl[] = null;
        for (Iterator<String> it = this.urllist.iterator() ; it.hasNext(); )
        {
            URL url;
            try {
                url = new URL(it.next() + "/handleresolver/resolve/" + handle);
            } catch (MalformedURLException ex) {
                log.error("Unexpected MalformedURLException: ", ex);
                throw new HandleException(HandleException.INTERNAL_ERROR);
            }

            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
                handleurl = gson.fromJson(in, String[].class);
            } catch (IOException ex) {
                log.error("Can't read prefixes from DSpace instance running at " + url + ":", ex);
                continue;
            }
            
            // we can get null, an empty array, an array containing null, an 
            // array containing an empty string or the handle url. Let's check it:
            if (null != handleurl && handleurl.length >= 1
                    && null != handleurl[0] && !handleurl[0].isEmpty())
            {
                break;
            }
        }

        // check if all DSpace instances were unable to resolve the handle
        if (handleurl == null || handleurl.length == 0
                || handleurl[0] == null || handleurl[0].isEmpty())
        {
            throw new HandleException(HandleException.HANDLE_DOES_NOT_EXIST);
        }

        HandleValue value = new HandleValue();

        value.setIndex(100);
        value.setType(Util.encodeString("URL"));
        value.setData(Util.encodeString(handleurl[0]));
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
         * this simple implementation, we will just check if the requestes
         * prefix is one that DSpace returns when we call
         * handleresolver/listprefixes.
         */
        // Which authority does the request pertain to? Remove the heading "0.NA/".
        String received = Util.decodeString(theHandle).substring("0.NA/".length());
        
        Gson gson = new Gson();
        for (Iterator<String> it = this.urllist.iterator() ; it.hasNext(); )
        {
            URL url;
            try {
                url = new URL(it.next() + "/handleresolver/listprefixes");
            } catch (MalformedURLException ex) {
                log.error("Unexpected MalformedURLException: ", ex);
                throw new HandleException(HandleException.INTERNAL_ERROR);
            }
            
            String[] prefixes;
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
                prefixes = gson.fromJson(in, String[].class);
            } catch (IOException ex) {
                log.error("Can't read prefixes from DSpace instance running at " + url + ":", ex);
                throw new HandleException(HandleException.INTERNAL_ERROR);
            }
            if (null == prefixes || prefixes.length == 0)
            {
                log.warn("DSpace instance running at " + url + " returns empty prefix list.");
            } else {
                for (String prefix : prefixes) {
                    if (null != prefix && prefix.equals(received))
                    {
                        return true;
                    }
                }
            }
        }
        return false;
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

        Gson gson = new Gson();
        List<String> handles = new LinkedList<String>();
        for (Iterator<String> it = this.urllist.iterator() ; it.hasNext(); )
        {
            URL url;
            try {
                url = new URL(it.next() + "/handleresolver/listhandles/" + naHandle);
            } catch (MalformedURLException ex) {
                log.error("Unexpected MalformedURLException: ", ex);
                throw new HandleException(HandleException.INTERNAL_ERROR);
            }

            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
                String[] handles_result = gson.fromJson(in, String[].class);
                if (null != handles_result)
                {
                    handles.addAll(Arrays.asList(handles_result));
                }
            } catch (IOException ex) {
                log.error("Can't read prefixes from DSpace instance running at " + url + ":", ex);
                continue;
            }
        }

        List<byte[]> results = new LinkedList<byte[]>();

        for (Iterator<String> iterator = handles.iterator(); iterator.hasNext();)
        {
            String handle = iterator.next();
            
            // we can get an array with one element that can be null or an
            // empty string. Check if we got handles:
            if (null != handle && !handle.isEmpty())
            {
                // Transforms to byte array
                results.add(Util.encodeString(handle));
            }
        }

        return Collections.enumeration(results);
    }
}
