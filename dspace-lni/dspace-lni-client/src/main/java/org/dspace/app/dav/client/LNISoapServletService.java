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
