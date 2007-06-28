/*
 * LNISmokeTest.java
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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.codec.binary.Base64;
import org.dspace.app.dav.client.LNIClientUtils;
import org.dspace.app.dav.client.LNISoapServlet;
import org.dspace.app.dav.client.LNISoapServletServiceLocator;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.JDOMParseException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;


/**
 * Very simple test program for DSpace Lightweight Network Interface (LNI).
 * <p>
 * This will test and demonstrate the LNI's SOAP API and some WebDAV operations.
 * It was written as a simple "smoke" test for the LNI to exercise basic
 * features in a simple way, and to serve as a coding example.
 * 
 * Example: (SOAP endpoint is http://mydspace.edu/dspace-lni/DSpaceLNI )
 * 
 * /dspace/bin/dsrun LNISmokeTest \ -e
 * http://user:passwd@mydsapce.edu/dspace-lni/DSpaceLNI \ -f 123.45/67
 * 
 * @author Larry Stone
 * @version $Revision$
 */
public class LNISmokeTest
{

    /** 
     * The Constant NS_DAV. 
     * namespace for DAV XML objects
     */
    private static final Namespace NS_DAV = Namespace.getNamespace("DAV:");

    /** 
     * The Constant NS_DSPACE. 
     * DSpace's XML namespace
     */
    private static final Namespace NS_DSPACE = Namespace.getNamespace("dspace",
            "http://www.dspace.org/xmlns/dspace");

    /** The output pretty. */
    private static XMLOutputter outputPretty = new XMLOutputter(Format
            .getPrettyFormat());

    // XML expressions for propfind calls (below)
    
    /** The Constant allProp. */
    private static final String allProp = "<propfind xmlns=\"DAV:\"><allprop /></propfind>";

    /** The Constant nameProp. */
    private static final String nameProp = "<propfind xmlns=\"DAV:\"><propname /></propfind>";

    /** The Constant someProp. */
    private static final String someProp = "<propfind xmlns=\"DAV:\"><prop>"
            + "<displayname/>"
            + "<dspace:type xmlns:dspace=\"http://www.dspace.org/xmlns/dspace\" />"
            + "</prop></propfind>";

    /** The Constant specificPropPrefix. */
    private static final String specificPropPrefix = "<propfind xmlns=\"DAV:\""
            + " xmlns:dspace=\"http://www.dspace.org/xmlns/dspace\">"
            + "  <prop><";

    /** The Constant specificPropSuffix. */
    private static final String specificPropSuffix = "/></prop></propfind>";

    /**
     * Usage. prints usage info to System.out & dies.
     * 
     * @param options the options
     * @param status the status
     * @param msg the msg
     */
    private static void Usage(Options options, int status, String msg)
    {
        HelpFormatter hf = new HelpFormatter();
        if (msg != null)
        {
            System.out.println(msg + "\n");
        }
        hf.printHelp("LNISmokeTest\n" + "       -e SOAP-endpoint-URL\n"
                + "       [ -s collection-handle -P package -i path ] |\n"
                + "       [ -d item-handle -P package -o path ] |\n"
                + "       [ -f handle [ -N propertyName ] ] |\n"
                + "       [ -r handle [ -N propertyName ] ] |\n"
                + "       [ -n handle ] |\n"
                + "       [ -p handle  -N propertyName -V newvalue ] |\n"
                + "       [ -d item-handle -C collection-handle ]\n", options,
                false);
        System.exit(status);
    }

    /**
     * Execute command line. See Usage string for options and arguments.
     * 
     * @param argv the argv
     * 
     * @throws Exception the exception
     */
    public static void main(String[] argv) throws Exception
    {
        Options options = new Options();

        OptionGroup func = new OptionGroup();
        func.addOption(new Option("c", "copy", true,
                "copy <Item> to -C <Collection>"));
        func.addOption(new Option("s", "submit", true,
                "submit <collection> -P <packager> -i <file>"));
        func.addOption(new Option("d", "disseminate", true,
                "disseminate <item> -P <packager> -o <file>"));
        func.addOption(new Option("f", "propfind", true,
                "propfind of all properties or -N <propname>"));
        func.addOption(new Option("r", "rpropfind", true,
                "recursive propfind, only collections"));
        func.addOption(new Option("n", "names", true,
                "list all property names on resource"));
        func.addOption(new Option("p", "proppatch", true,
                "set property: <handle> -N <property> -V <newvalue>"));
        func.setRequired(true);
        options.addOptionGroup(func);

        options.addOption("h", "help", false, "show help message");
        options
                .addOption("e", "endpoint", true,
                        "SOAP endpoint URL (REQUIRED)");
        options.addOption("P", "packager", true,
                "Packager to use to import/export a package.");
        options.addOption("C", "collection", true,
                "Target collection of -c copy");
        options
                .addOption("o", "output", true,
                        "file to create for new package");
        options.addOption("i", "input", true,
                "file containing package to submit");
        options.addOption("N", "name", true, "name of property to query/set");
        options.addOption("V", "value", true,
                "new value for property being set");

        try
        {
            CommandLine line = (new PosixParser()).parse(options, argv);
            if (line.hasOption("h"))
            {
                Usage(options, 0, null);
            }

            // get SOAP client connection, using the endpoint URL
            String endpoint = line.getOptionValue("e");
            if (endpoint == null)
            {
                Usage(options, 2, "Missing the required -e endpoint argument");
            }
            LNISoapServletServiceLocator loc = new LNISoapServletServiceLocator();
            LNISoapServlet lni = loc.getDSpaceLNI(new URL(endpoint));

            // propfind - with optional single-property Name
            if (line.hasOption("f"))
            {
                String pfXml = (line.hasOption("N")) ? specificPropPrefix
                        + line.getOptionValue("N") + specificPropSuffix
                        : allProp;
                doPropfind(lni, line.getOptionValue("f"), pfXml, 0, null);
            }

            // recursive propfind limited to collection, community objects
            else if (line.hasOption("r"))
            {
                doPropfind(lni, line.getOptionValue("r"), someProp, -1,
                        "collection,community");
            }
            else if (line.hasOption("n"))
            {
                doPropfind(lni, line.getOptionValue("n"), nameProp, 0, null);
            }
            else if (line.hasOption("p"))
            {
                if (line.hasOption("N") && line.hasOption("V"))
                {
                    doProppatch(lni, line.getOptionValue("p"), line
                            .getOptionValue("N"), line.getOptionValue("V"));
                }
                else
                {
                    Usage(options, 13,
                            "Missing required args: -N <name> -V <value>n");
                }
            }

            // submit a package
            else if (line.hasOption("s"))
            {
                if (line.hasOption("P") && line.hasOption("i"))
                {
                    doPut(lni, line.getOptionValue("s"), line
                            .getOptionValue("P"), line.getOptionValue("i"),
                            endpoint);
                }
                else
                {
                    Usage(options, 13,
                            "Missing required args after -s: -P <packager> -i <file>");
                }
            }

            // Disseminate (GET) item as package
            else if (line.hasOption("d"))
            {
                if (line.hasOption("P") && line.hasOption("o"))
                {
                    doGet(lni, line.getOptionValue("d"), line
                            .getOptionValue("P"), line.getOptionValue("o"),
                            endpoint);
                }
                else
                {
                    Usage(options, 13,
                            "Missing required args after -d: -P <packager> -o <file>");
                }
            }

            // copy from src to dst
            else if (line.hasOption("c"))
            {
                if (line.hasOption("C"))
                {
                    doCopy(lni, line.getOptionValue("c"), line
                            .getOptionValue("C"));
                }
                else
                {
                    Usage(options, 13,
                            "Missing required args after -c: -C <collection>\n");
                }
            }
            else
            {
                Usage(options, 14, "Missing command option.\n");
            }

        }
        catch (ParseException pe)
        {
            Usage(options, 1, "Error in arguments: " + pe.toString());
        }
        catch (java.rmi.RemoteException de)
        {
            System.out.println("ERROR, got RemoteException, message="
                    + de.getMessage());
            if (de.getCause() != null)
            {
                System.out.println("  Cause=" + de.getCause().toString());
            }
            die(1, "  Exception class=" + de.getClass().getName());
        }
    }

    /**
     * Die.
     * 
     * @param exit the exit
     * @param msg the msg
     */
    private static void die(int exit, String msg)
    {
        System.err.println(msg);
        System.exit(exit);
    }

    // Like LNI lookup(), but dies on error so we don't have to check result.
    // Also interprets the special format "handle bitstream" (with space
    // separating the words) as the ``handle'' of a bitstream -- a combination
    // of Item handle and bitstream sequence-ID.
    // On success it returns a DAV resource URI, relative to the
    // root of the DAV resource hierarchy, e.g. "/dso_12345.678$123"
    /**
     * Do lookup.
     * 
     * @param lni the lni
     * @param handle the handle
     * @param bitstream the bitstream
     * 
     * @return the string
     * 
     * @throws RemoteException the remote exception
     */
    private static String doLookup(LNISoapServlet lni, String handle,
            String bitstream) throws java.rmi.RemoteException
    {
        // hack: if "handle" starts with '/' and there is no bitstream
        // assume it is URI passed in and just return that.
        if (handle.startsWith("/") && bitstream == null)
        {
            return handle;
        }

        // hack: parse "handle bitstream" syntax of handle.
        if (handle.indexOf(" ") >= 0 && bitstream == null)
        {
            String h[] = handle.split("\\s+");
            handle = h[0];
            bitstream = h[1];
        }

        String uri = lni.lookup(handle, bitstream);
        if (uri == null)
        {
            die(2, "ERROR, got null from lookup(\"" + handle + "\")");
        }
        System.out.println("DEBUG: lookup returns: \"" + uri + "\"");
        return uri;
    }

    /**
     * Do propfind.
     * 
     * @param lni the lni
     * @param handle the handle
     * @param pf the pf
     * @param depth the depth
     * @param types the types
     * 
     * @throws RemoteException the remote exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static void doPropfind(LNISoapServlet lni, String handle,
            String pf, int depth, String types)
            throws java.rmi.RemoteException, java.io.IOException
    {
        String uri = doLookup(lni, handle, null);
        String result = lni.propfind(uri, pf, depth, types);
        try
        {
            SAXBuilder builder = new SAXBuilder();
            Document msDoc = builder.build(new java.io.StringReader(result));
            Element ms = msDoc.getRootElement();
            ListIterator ri = ms.getChildren("response", NS_DAV).listIterator();
            while (ri.hasNext())
            {
                Element resp = (Element) ri.next();
                String href = resp.getChildText("href", NS_DAV);
                System.out.println("Resource = " + href);
                ListIterator pi = resp.getChildren("propstat", NS_DAV)
                        .listIterator();
                while (pi.hasNext())
                {
                    Element ps = (Element) pi.next();
                    String status = ps.getChildText("status", NS_DAV);
                    if (status.indexOf("200") >= 0)
                    {
                        System.out
                                .println("  === PROPERTIES Successfully returned:");
                    }
                    else
                    {
                        System.out.println("  === PROPERTIES with Status="
                                + status);
                    }
                    // print properties and values
                    Element prop = ps.getChild("prop", NS_DAV);
                    ListIterator ppi = prop.getChildren().listIterator();
                    while (ppi.hasNext())
                    {
                        Element e = (Element) ppi.next();
                        String value = e.getTextTrim();
                        if (value.equals(""))
                        {
                            List kids = e.getChildren();
                            if (kids.size() > 0)
                            {
                                value = outputPretty.outputString(kids);
                            }
                            if (value.indexOf("\n") >= 0)
                            {
                                value = "\n" + value;
                            }
                        }
                        else
                        {
                            value = "\"" + value + "\"";
                        }
                        String equals = value.equals("") ? "" : " = ";
                        System.out.println("    " + e.getQualifiedName()
                                + equals + value);
                    }
                }
            }

        }
        catch (JDOMParseException je)
        {
            die(3, "ERROR: " + je.toString());
        }
        catch (JDOMException je)
        {
            die(4, "ERROR: " + je.toString());
        }

    }

    /**
     * Do proppatch.
     * 
     * @param lni the lni
     * @param handle the handle
     * @param prop the prop
     * @param val the val
     * 
     * @throws RemoteException the remote exception
     */
    private static void doProppatch(LNISoapServlet lni, String handle,
            String prop, String val) throws java.rmi.RemoteException
    {
        String uri = doLookup(lni, handle, null);
        String action = (val.length() > 0) ? "set" : "remove";
        String pupdate = "<propertyupdate xmlns=\"DAV:\" xmlns:dspace=\"http://www.dspace.org/xmlns/dspace\">"
                + "<"
                + action
                + "><prop><"
                + prop
                + ">"
                + val
                + "</"
                + prop
                + "></prop></" + action + "></propertyupdate>";
        System.err.println("DEBUG: sending: " + pupdate);
        String result = lni.proppatch(uri, pupdate);
        System.err.println("RESULT: " + result);
    }

    // "copy" src item into dst collection, both are handles.
    /**
     * Do copy.
     * 
     * @param lni the lni
     * @param src the src
     * @param dst the dst
     * 
     * @throws RemoteException the remote exception
     */
    private static void doCopy(LNISoapServlet lni, String src, String dst)
            throws java.rmi.RemoteException
    {
        String srcUri = doLookup(lni, src, null);
        String dstUri = doLookup(lni, dst, null);
        int status = lni.copy(srcUri, dstUri, -1, false, true);
        System.err.println("Copy status = " + String.valueOf(status));
    }

    /**
     * Implement WebDAV PUT http request.
     * 
     * This might be simpler with a real HTTP client library, but
     * java.net.HttpURLConnection is part of the standard SDK and it
     * demonstrates the concepts.
     * 
     * @param lni the lni
     * @param collHandle the coll handle
     * @param packager the packager
     * @param source the source
     * @param endpoint the endpoint
     * 
     * @throws RemoteException the remote exception
     * @throws ProtocolException the protocol exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws FileNotFoundException the file not found exception
     */
    private static void doPut(LNISoapServlet lni, String collHandle,
            String packager, String source, String endpoint)
            throws java.rmi.RemoteException, ProtocolException, IOException,
            FileNotFoundException
    {
        // assemble URL from chopped endpoint-URL and relative URI
        String collURI = doLookup(lni, collHandle, null);
        URL url = LNIClientUtils.makeDAVURL(endpoint, collURI, packager);
        System.err.println("DEBUG: PUT file=" + source + " to URL="
                + url.toString());

        // connect with PUT method, then copy file over.
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("PUT");
        conn.setDoOutput(true);
        fixBasicAuth(url, conn);
        conn.connect();

        InputStream in = new FileInputStream(source);
        OutputStream out = conn.getOutputStream();
        copyStream(in, out);
        in.close();
        out.close();

        int status = conn.getResponseCode();
        if (status < 200 || status >= 300)
        {
            die(status, "HTTP error, status=" + String.valueOf(status)
                    + ", message=" + conn.getResponseMessage());
        }

        // diagnostics, and get resulting new item's location if avail.
        System.err.println("DEBUG: sent " + source);
        System.err.println("RESULT: Status="
                + String.valueOf(conn.getResponseCode()) + " "
                + conn.getResponseMessage());
        String loc = conn.getHeaderField("Location");
        System.err.println("RESULT: Location="
                + ((loc == null) ? "NULL!" : loc));
    }

    /**
     * Get an item with WebDAV GET http request.
     * 
     * @param lni the lni
     * @param itemHandle the item handle
     * @param packager the packager
     * @param output the output
     * @param endpoint the endpoint
     * 
     * @throws RemoteException the remote exception
     * @throws ProtocolException the protocol exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws FileNotFoundException the file not found exception
     */
    private static void doGet(LNISoapServlet lni, String itemHandle,
            String packager, String output, String endpoint)
            throws java.rmi.RemoteException, ProtocolException, IOException,
            FileNotFoundException
    {
        // assemble URL from chopped endpoint-URL and relative URI
        String itemURI = doLookup(lni, itemHandle, null);
        URL url = LNIClientUtils.makeDAVURL(endpoint, itemURI, packager);
        System.err.println("DEBUG: GET from URL: " + url.toString());

        // connect with GET method, then copy file over.
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        fixBasicAuth(url, conn);
        conn.connect();
        int status = conn.getResponseCode();
        if (status < 200 || status >= 300)
        {
            die(status, "HTTP error, status=" + String.valueOf(status)
                    + ", message=" + conn.getResponseMessage());
        }

        InputStream in = conn.getInputStream();
        OutputStream out = new FileOutputStream(output);
        copyStream(in, out);
        in.close();
        out.close();

        System.err.println("DEBUG: Created local file " + output);
        System.err.println("RESULT: Status="
                + String.valueOf(conn.getResponseCode()) + " "
                + conn.getResponseMessage());
    }

    // 
    /**
     * Fix basic auth.
     * 
     * Set up HTTP basic authentication based on user/password in URL.
     * The HttpURLConnection class should do this itself!
     * 
     * @param url the url
     * @param conn the conn
     */
    private static void fixBasicAuth(URL url, HttpURLConnection conn)
    {
        String userinfo = url.getUserInfo();
        if (userinfo != null)
        {
            String cui = new String(Base64.encodeBase64(userinfo.getBytes()));
            conn.addRequestProperty("Authorization", "Basic " + cui);
            System.err.println("DEBUG: Sending Basic auth=" + cui);
        }
    }

    /**
     * Copy stream. copy from one stream to another
     * 
     * @param input the input
     * @param output the output
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static void copyStream(final InputStream input,
            final OutputStream output) throws IOException
    {
        final int BUFFER_SIZE = 1024 * 4;
        final byte[] buffer = new byte[BUFFER_SIZE];

        while (true)
        {
            final int count = input.read(buffer, 0, BUFFER_SIZE);
            if (-1 == count)
            {
                break;
            }
            output.write(buffer, 0, count);
        }
    }
}
