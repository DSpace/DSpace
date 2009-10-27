/**
 * LNISoapServletServiceLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.3 Oct 05, 2005 (05:23:37 EDT) WSDL2Java emitter.
 */

package org.dspace.app.dav.client;

public class LNISoapServletServiceLocator extends org.apache.axis.client.Service implements org.dspace.app.dav.client.LNISoapServletService {

    public LNISoapServletServiceLocator() {
    }


    public LNISoapServletServiceLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public LNISoapServletServiceLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for DSpaceLNI
    private java.lang.String DSpaceLNI_address = "http://localhost/dspace/lni/DSpaceLNI";

    public java.lang.String getDSpaceLNIAddress() {
        return DSpaceLNI_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String DSpaceLNIWSDDServiceName = "DSpaceLNI";

    public java.lang.String getDSpaceLNIWSDDServiceName() {
        return DSpaceLNIWSDDServiceName;
    }

    public void setDSpaceLNIWSDDServiceName(java.lang.String name) {
        DSpaceLNIWSDDServiceName = name;
    }

    public org.dspace.app.dav.client.LNISoapServlet getDSpaceLNI() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(DSpaceLNI_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getDSpaceLNI(endpoint);
    }

    public org.dspace.app.dav.client.LNISoapServlet getDSpaceLNI(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            org.dspace.app.dav.client.DSpaceLNISoapBindingStub _stub = new org.dspace.app.dav.client.DSpaceLNISoapBindingStub(portAddress, this);
            _stub.setPortName(getDSpaceLNIWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setDSpaceLNIEndpointAddress(java.lang.String address) {
        DSpaceLNI_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (org.dspace.app.dav.client.LNISoapServlet.class.isAssignableFrom(serviceEndpointInterface)) {
                org.dspace.app.dav.client.DSpaceLNISoapBindingStub _stub = new org.dspace.app.dav.client.DSpaceLNISoapBindingStub(new java.net.URL(DSpaceLNI_address), this);
                _stub.setPortName(getDSpaceLNIWSDDServiceName());
                return _stub;
            }
        }
        catch (java.lang.Throwable t) {
            throw new javax.xml.rpc.ServiceException(t);
        }
        throw new javax.xml.rpc.ServiceException("There is no stub implementation for the interface:  " + (serviceEndpointInterface == null ? "null" : serviceEndpointInterface.getName()));
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(javax.xml.namespace.QName portName, Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        if (portName == null) {
            return getPort(serviceEndpointInterface);
        }
        java.lang.String inputPortName = portName.getLocalPart();
        if ("DSpaceLNI".equals(inputPortName)) {
            return getDSpaceLNI();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("http://dspace.org/xmlns/lni", "LNISoapServletService");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("http://dspace.org/xmlns/lni", "DSpaceLNI"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        
if ("DSpaceLNI".equals(portName)) {
            setDSpaceLNIEndpointAddress(address);
        }
        else 
{ // Unknown Port Name
            throw new javax.xml.rpc.ServiceException(" Cannot set Endpoint Address for Unknown Port" + portName);
        }
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(javax.xml.namespace.QName portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        setEndpointAddress(portName.getLocalPart(), address);
    }

}
