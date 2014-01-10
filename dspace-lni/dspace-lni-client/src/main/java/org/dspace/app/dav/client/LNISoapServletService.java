/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
/**
 * LNISoapServletService.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.3 Oct 05, 2005 (05:23:37 EDT) WSDL2Java emitter.
 */

package org.dspace.app.dav.client;

public interface LNISoapServletService extends javax.xml.rpc.Service {
    public java.lang.String getDSpaceLNIAddress();

    public org.dspace.app.dav.client.LNISoapServlet getDSpaceLNI() throws javax.xml.rpc.ServiceException;

    public org.dspace.app.dav.client.LNISoapServlet getDSpaceLNI(java.net.URL portAddress) throws javax.xml.rpc.ServiceException;
}
