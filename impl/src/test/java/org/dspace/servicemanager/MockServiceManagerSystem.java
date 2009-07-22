/**
 * $Id: MockServiceManagerSystem.java 3232 2008-10-24 10:41:37Z azeckoski $
 * $URL: https://scm.dspace.org/svn/repo/dspace2/core/trunk/impl/src/test/java/org/dspace/servicemanager/MockServiceManagerSystem.java $
 * MockServiceManagerSystem.java - DSpace2 - Oct 24, 2008 11:15:02 AM - azeckoski
 **************************************************************************
 * Copyright (c) 2008 Aaron Zeckoski
 * Licensed under the Apache License, Version 2.0
 * 
 * A copy of the Apache License has been included in this 
 * distribution and is available at: http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Aaron Zeckoski (azeckoski @ gmail.com) (aaronz @ vt.edu) (aaron @ caret.cam.ac.uk)
 */

package org.dspace.servicemanager;

import java.util.List;
import java.util.Map;


/**
 * This Mock allows us to pretend that a SMS is its own parent,
 * for testing use only
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class MockServiceManagerSystem implements ServiceManagerSystem {

    private final ServiceManagerSystem sms;
    public MockServiceManagerSystem(ServiceManagerSystem sms) {
        this.sms = sms;
    }

    /* (non-Javadoc)
     * @see org.dspace.servicemanager.ServiceManagerSystem#getServices()
     */
    public Map<String, Object> getServices() {
        return this.sms.getServices();
    }

    /* (non-Javadoc)
     * @see org.dspace.servicemanager.ServiceManagerSystem#shutdown()
     */
    public void shutdown() {
        this.sms.shutdown();
    }

    /* (non-Javadoc)
     * @see org.dspace.servicemanager.ServiceManagerSystem#startup()
     */
    public void startup() {
        this.sms.startup();
    }

    /* (non-Javadoc)
     * @see org.dspace.servicemanager.ServiceManagerSystem#unregisterService(java.lang.String)
     */
    public void unregisterService(String name) {
        this.sms.unregisterService(name);
    }

    /* (non-Javadoc)
     * @see org.dspace.kernel.ServiceManager#getServiceByName(java.lang.String, java.lang.Class)
     */
    public <T> T getServiceByName(String name, Class<T> type) {
        return this.sms.getServiceByName(name, type);
    }

    /* (non-Javadoc)
     * @see org.dspace.kernel.ServiceManager#getServicesByType(java.lang.Class)
     */
    public <T> List<T> getServicesByType(Class<T> type) {
        return this.sms.getServicesByType(type);
    }

    /* (non-Javadoc)
     * @see org.dspace.kernel.ServiceManager#getServicesNames()
     */
    public List<String> getServicesNames() {
        return this.sms.getServicesNames();
    }

    /* (non-Javadoc)
     * @see org.dspace.kernel.ServiceManager#isServiceExists(java.lang.String)
     */
    public boolean isServiceExists(String name) {
        return this.sms.isServiceExists(name);
    }

    /* (non-Javadoc)
     * @see org.dspace.kernel.ServiceManager#pushConfig(java.util.Map)
     */
    public void pushConfig(Map<String, String> settings) {
        this.sms.pushConfig(settings);
    }

    /* (non-Javadoc)
     * @see org.dspace.kernel.ServiceManager#registerService(java.lang.String, java.lang.Object)
     */
    public void registerService(String name, Object service) {
        this.sms.registerService(name, service);
    }

    /* (non-Javadoc)
     * @see org.dspace.kernel.ServiceManager#registerServiceClass(java.lang.String, java.lang.Class)
     */
    public <T> T registerServiceClass(String name, Class<T> type) {
        return this.sms.registerServiceClass(name, type);
    }

}
