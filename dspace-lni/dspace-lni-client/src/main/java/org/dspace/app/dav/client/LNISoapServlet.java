/**
 * LNISoapServlet.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.3 Oct 05, 2005 (05:23:37 EDT) WSDL2Java emitter.
 */

package org.dspace.app.dav.client;

public interface LNISoapServlet extends java.rmi.Remote {
    public int copy(java.lang.String source, java.lang.String destination, int depth, boolean overwrite, boolean keepProperties) throws java.rmi.RemoteException, org.dspace.app.dav.client.LNIRemoteException;
    public java.lang.String lookup(java.lang.String handle, java.lang.String bitstreamPid) throws java.rmi.RemoteException, org.dspace.app.dav.client.LNIRemoteException;
    public java.lang.String propfind(java.lang.String uri, java.lang.String doc, int depth, java.lang.String types) throws java.rmi.RemoteException, org.dspace.app.dav.client.LNIRemoteException;
    public java.lang.String proppatch(java.lang.String uri, java.lang.String doc) throws java.rmi.RemoteException, org.dspace.app.dav.client.LNIRemoteException;
}
