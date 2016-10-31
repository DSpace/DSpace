/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
/**
 * Copyright (c) 2008, Aberystwyth University
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions 
 * are met:
 * 
 *  - Redistributions of source code must retain the above 
 *    copyright notice, this list of conditions and the 
 *    following disclaimer.
 *  
 *  - Redistributions in binary form must reproduce the above copyright 
 *    notice, this list of conditions and the following disclaimer in 
 *    the documentation and/or other materials provided with the 
 *    distribution.
 *    
 *  - Neither the name of the Centre for Advanced Software and 
 *    Intelligent Systems (CASIS) nor the names of its 
 *    contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR 
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF 
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF 
 * SUCH DAMAGE.
 */

package org.purl.sword.client;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.purl.sword.base.DepositResponse;
import org.purl.sword.base.SWORDEntry;
import org.purl.sword.base.ServiceDocument;

/**
 * Example client that runs as a Servlet.
 * 
 * @author Stuart Lewis
 */
public class ServletClient extends HttpServlet {

    /**
     * The user agent name of this library
     */
    public static final String userAgent = "SWORDAPP Java Client: SWORD version 1.3 compatible (http://sourceforge.net/projects/sword-app/)";
    
    /** 
     * Temporary directory.
     */
    private String tempDirectory;

    /**
     * List of urls for the destination services to access.
     */
    private String[] urls;

    /**
     * Used to determine if a proxy value should be set.
     */
    private boolean useProxy = false;

    /**
     * The proxy host name.
     */
    private String pHost;

    /**
     * The proxy port name.
     */
    private int pPort;

    /** Counter used during Deposit information. */
    private static int counter = 0;

    /**
     * Initialise the servlet.
     */
    public void init() {
        tempDirectory = getServletContext().getInitParameter(
                "upload-temp-directory");
        if ((tempDirectory == null) || (tempDirectory.equals(""))) {
            tempDirectory = System.getProperty("java.io.tmpdir");
        }
        String lots = getServletContext().getInitParameter("client-urls");
        urls = lots.split(",");
        
        pHost = getServletContext().getInitParameter("proxy-host");
        String pPortstr = getServletContext().getInitParameter("proxy-port");
        if (((pHost != null) && (!pHost.equals("")))
                && ((pPortstr != null) && (!pPortstr.equals("")))) {
            try {
                pPort = Integer.parseInt(pPortstr);
                useProxy = true;
            } catch (Exception e) {
                // Port number not numeric
            }
        }
    }

    /**
     * Handle a get request. Simply show the default form (form.jsp)
     * 
     * @param request The request details
     * @param response The response to write to.
     * 
     * @throws ServletException
     *     An exception that provides information on a database access error or other errors.
     * @throws IOException
     *     A general class of exceptions produced by failed or interrupted I/O operations.
     */
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        // Get request, so show the default page
        request.setAttribute("urls", urls);
        request.getRequestDispatcher("form.jsp").forward(request, response);
    }

    /**
     * Process the post. Determine if the request is for a post or service
     * document. Then, dispatch the request to the appropriate handler.
     * 
     * @param request The request details.
     * @param response The response to write to.
     * 
     * @throws ServletException
     *     An exception that provides information on a database access error or other errors.
     * @throws IOException
     *     A general class of exceptions produced by failed or interrupted I/O operations.
     */

    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) throws ServletException, IOException {
        if (request.getParameter("servicedocument") != null) {
            this.doServiceDocument(request, response);
        } else if (request.getParameter("deposit") != null) {
            request.setAttribute("url", request.getParameter("url"));
            request.setAttribute("u", request.getParameter("u"));
            request.setAttribute("p", request.getParameter("p"));
            request.setAttribute("obo", request.getParameter("obo"));
            request.setAttribute("abstract", request.getParameter("abstract"));
            request.setAttribute("policy", request.getParameter("policy"));
            request.setAttribute("treatment", request.getParameter("treatment"));
            request.setAttribute("mediation", request.getParameter("mediation"));
            request.setAttribute("accepts", request.getParameter("accepts"));
            request.setAttribute("acceptsp", request.getParameter("acceptsp"));
            request.setAttribute("maxuploadsize", request.getParameter("maxuploadsize"));
            request.getRequestDispatcher("depositform.jsp").forward(request, response);
        } else if (ServletFileUpload.isMultipartContent(request)) {
            this.doDeposit(request, response);
        } else {
            request.setAttribute("urls", urls);
            request.getRequestDispatcher("form.jsp").forward(request, response);
        }
    }

    /** 
     * Process the request for a service document.
     * 
     * @param request The request details.
     * @param response The response to write to.
     *
     * @throws ServletException 
     *     An exception that provides information on a database access error or other errors.
     * @throws IOException
     *     A general class of exceptions produced by failed or interrupted I/O operations.
     */

    private void doServiceDocument(HttpServletRequest request,
                                   HttpServletResponse response) throws ServletException, IOException {
        // Get the service document
        Client client = new Client();
        // Which URL do we want?
        URL url = new URL(request.getParameter("url"));
        String theUrl = request.getParameter("url");
        
        if ((request.getParameter("ownurl") != null)
                && (!request.getParameter("ownurl").equals(""))) {
            url = new URL(request.getParameter("ownurl"));
            theUrl = request.getParameter("ownurl");
        }

        int port = url.getPort();
        if (port == -1) {
            port = 80;
        }

        // Set up the server
        client.setServer(url.getHost(), port);
        client.setCredentials(request.getParameter("u"), request.getParameter("p"));
        if (useProxy) {
            client.setProxy(pHost, pPort);
        }

        try {
            ServiceDocument sd = client.getServiceDocument(theUrl,
            request.getParameter("obo"));

            // Set the status 
            Status status = client.getStatus();
            request.setAttribute("status", status.toString());
            if (status.getCode() == 200) {
                // Set the debug response
                String xml = sd.marshall();

                String validateXml = xml;
                validateXml = validateXml.replaceAll("&", "&amp;");
                validateXml = validateXml.replaceAll("<", "&lt;");
                validateXml = validateXml.replaceAll(">", "&gt;");
                validateXml = validateXml.replaceAll("\"", "&quot;");
                validateXml = validateXml.replaceAll("'", "&apos;");
                request.setAttribute("xmlValidate", validateXml); // for passing to validation
                
                xml = xml.replaceAll("<", "&lt;");
                xml = xml.replaceAll(">", "&gt;");
                request.setAttribute("xml", xml);
                
                // Set the ServiceDocument and associated values
                request.setAttribute("sd", sd);
                request.setAttribute("sdURL", theUrl);
                request.setAttribute("u", request.getParameter("u"));
                request.setAttribute("p", request.getParameter("p"));
                request.setAttribute("sdOBO", request.getParameter("obo"));
                request.getRequestDispatcher("servicedocument.jsp").forward(
                        request, response);
                return;
            } else {
                request.setAttribute("error", status.getCode() + " "
                        + status.getMessage());
                request.setAttribute("urls", urls);
                request.getRequestDispatcher("form.jsp").forward(request,
                        response);
                return;
            }
        } catch (SWORDClientException e) {
            e.printStackTrace();
            request.setAttribute("error", e.toString());
            request.setAttribute("urls", urls);
            request.getRequestDispatcher("form.jsp").forward(request, response);
        }
    }

    /**
     * Process a deposit.
     * 
     * @param request The request details.
     * @param response The response to output to.
     * 
     * @throws ServletException
     *     An exception that provides information on a database access error or other errors.
     * @throws IOException
     *     A general class of exceptions produced by failed or interrupted I/O operations.
     */
    private void doDeposit(HttpServletRequest request,
                           HttpServletResponse response) throws ServletException, IOException {
        // Do the deposit
        Client client = new Client();
        try {
            PostMessage message = new PostMessage();
            message.setUserAgent(ClientConstants.SERVICE_NAME);
            
            // Get the file
            FileItemFactory factory = new DiskFileItemFactory();
            
            // Create a new file upload handler
            ServletFileUpload upload = new ServletFileUpload(factory);
            
            // Parse the request
            List<FileItem> items = upload.parseRequest(request);
            Iterator<FileItem> iter = items.iterator();
            String u = null;
            String p = null;
            String contentDisposition = null;
            String filetype = null;
            boolean useMD5 = false;
            boolean errorMD5 = false;
            boolean verbose = false;
            boolean noOp = false;
            boolean login = false;
            while (iter.hasNext()) {
                FileItem item = iter.next();
                if (item.isFormField()) {
                    String name = item.getFieldName();
                    String value = item.getString();
                    if (name.equals("url")) {
                        message.setDestination(value);
                        URL url = new URL(value);
                        int port = url.getPort();
                        if (port == -1) {
                            port = 80;
                        }
                        client.setServer(url.getHost(), port);
                    } else if (name.equals("usemd5")) {
                        useMD5 = true;
                    } else if (name.equals("errormd5")) {
                        errorMD5 = true;
                    } else if (name.equals("verbose")) {
                        verbose = true;
                    } else if (name.equals("noop")) {
                        noOp = true;
                    } else if (name.equals("obo")) {
                        message.setOnBehalfOf(value);
                    } else if (name.equals("slug")) {
                        if ((value != null) && (!value.trim().equals(""))) {
                            message.setSlug(value);
                        }
                    } else if (name.equals("cd")) {
                        contentDisposition = value;
                    } else if (name.equals("filetype")) {
                        filetype = value;
                    } else if (name.equals("formatnamespace")) {
                        if ((value != null) && (!value.trim().equals(""))) {
                            message.setFormatNamespace(value);
                        }
                    } else if (name.equals("u")) {
                        u = value;
                        login = true;
                        request.setAttribute("u", value);
                    } else if (name.equals("p")) {
                        p = value;
                        login = true;
                    }
                    request.setAttribute(name, value);
                } else {
                    String fname = tempDirectory + File.separator + 
                                   "ServletClient-" + counter++;
                    if ((contentDisposition != null) && (!contentDisposition.equals(""))) {
                        fname = tempDirectory + File.separator + contentDisposition;
                    }

                    File uploadedFile = new File(fname);
                    item.write(uploadedFile);
                    message.setFilepath(fname);

                    if ((filetype == null) || (filetype.trim().equals(""))) {
                        message.setFiletype(item.getContentType());
                    } else {
                        message.setFiletype(filetype);
                    }
                }
            }

            if (login) {
                client.setCredentials(u, p);
            }

            if (useProxy) {
                client.setProxy(pHost, pPort);
            }

            message.setUseMD5(useMD5);
            message.setChecksumError(errorMD5);
            message.setVerbose(verbose);
            message.setNoOp(noOp);

            // Post the file
            DepositResponse resp = client.postFile(message);

            // Set the status
            Status status = client.getStatus();
            request.setAttribute("status", status.toString());
            if ((status.getCode() == 201) || (status.getCode() == 202)) {
                // Set the debug response
                String xml = resp.marshall();

                String validateXml = xml;
                validateXml = validateXml.replaceAll("&", "&amp;");
                validateXml = validateXml.replaceAll("<", "&lt;");
                validateXml = validateXml.replaceAll(">", "&gt;");
                validateXml = validateXml.replaceAll("\"", "&quot;");
                validateXml = validateXml.replaceAll("'", "&apos;");
                request.setAttribute("xmlValidate", validateXml); // for passing to validation

                xml = xml.replaceAll("<", "&lt;");
                xml = xml.replaceAll(">", "&gt;");
                request.setAttribute("xml", xml);
                SWORDEntry se = resp.getEntry();
                request.setAttribute("id", se.getId());
                request.setAttribute("authors", se.getAuthors());
                request.setAttribute("contributors", se.getContributors());
                request.setAttribute("title", se.getTitle().getContent());
                request.setAttribute("updated", se.getUpdated());
                request.setAttribute("categories", se.getCategories());
                request.setAttribute("treatment", se.getTreatment());
                request.setAttribute("summary", se.getSummary().getContent());
                request.setAttribute("generator", se.getGenerator().getContent());
                request.setAttribute("userAgent", se.getUserAgent());
                request.setAttribute("packaging", se.getPackaging());
                request.setAttribute("links", se.getLinks());
                request.setAttribute("location", resp.getLocation());
                
                // Set the ServiceDocument and associated values
                request.getRequestDispatcher("deposit.jsp").forward(request, response);
                return;
            } else {
                String error = status.getCode() + " " + status.getMessage() + " - ";
                try {
                    error += resp.getEntry().getSummary().getContent();
                } catch (Exception e) { 
                    // Do nothing - we have default error message
                    e.printStackTrace();
                }
                request.setAttribute("error", error);
                
                // Try and get an error document in xml
                String xml = resp.marshall();
                xml = xml.replaceAll("<", "&lt;");
                xml = xml.replaceAll(">", "&gt;");
                request.setAttribute("xml", xml);
                
                request.getRequestDispatcher("depositform.jsp").forward(request, response);
                return;
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            request.setAttribute("error", "value: " + e.toString());
            request.setAttribute("urls", urls);
            request.getRequestDispatcher("depositform.jsp").forward(request, response);
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("error", "value: " + e.toString());
            request.setAttribute("urls", urls);
            request.getRequestDispatcher("depositform.jsp").forward(request, response);
        }
    }
}
