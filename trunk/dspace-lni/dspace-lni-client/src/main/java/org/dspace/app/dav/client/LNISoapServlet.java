/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
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
