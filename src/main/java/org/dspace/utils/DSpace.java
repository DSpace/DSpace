/*
 * $URL: $
 * 
 * $Revision: $
 * 
 * $Date: $
 *
 * Copyright (c) 2009, The DSpace Foundation.  All rights reserved.
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
 * - Neither the name of the DSpace Foundation nor the names of its 
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
package org.dspace.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.dspace.core.PluginManager;
import org.dspace.kernel.ServiceManager;

/**
 * This is the DSpace helper services access object, it allows access to all
 * core DSpace services for those who cannot or will not use the injection
 * service to get services <br/>
 * Note that this may not include every core service but should include all the
 * services that are useful to UI developers at least <br/>
 * This should be initialized using the constructor and then can be used as long
 * as the kernel is not shutdown, making multiple copies of this is cheap and
 * can be done without worry about the cost
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 * @author Mark Diggory (mdiggory @ atmire.com)
 */
public class DSpace
{

    private static ServiceManager manager = new ServiceManager()
    {

        @SuppressWarnings("unchecked")
        public <T> T getServiceByName(String name, Class<T> type)
        {
            return (T) PluginManager.getNamedPlugin(type, name);
        }

        @SuppressWarnings("unchecked")
        public <T> List<T> getServicesByType(Class<T> type)
        {
            List<T> services = new ArrayList<T>();
            for (Object o : PluginManager.getPluginSequence(type))
            {
                services.add((T) o);
            }
            return services;
        }

        public List<String> getServicesNames()
        {
            // TODO Need an implementation in PluginManager
            throw new java.lang.RuntimeException("Method not implemented");
        }

        public boolean isServiceExists(String name)
        {
            // TODO Need an implementation in PluginManager
            throw new java.lang.RuntimeException("Method not implemented");
        }

        public void pushConfig(Map<String, String> settings)
        {
            // TODO Need an implementation in PluginManager
            throw new java.lang.RuntimeException("Method not implemented");
        }

        public void registerService(String name, Object service)
        {
            // TODO Need an implementation in PluginManager
            throw new java.lang.RuntimeException("Method not implemented");
        }

        public <T> T registerServiceClass(String name, Class<T> type)
        {
            // TODO Need an implementation in PluginManager
            throw new java.lang.RuntimeException("Method not implemented");
        }

        public void unregisterService(String name)
        {
            // TODO Need an implementation in PluginManager
            throw new java.lang.RuntimeException("Method not implemented");
        }

    };

    /**
     * Construct a DSpace helper object which uses the default kernel
     * 
     * @throws IllegalStateException
     *             if the kernel is not already running
     */
    public DSpace()
    {

    }

    /**
     * 
     * @return the Service Manager
     */
    public ServiceManager getServiceManager()
    {
        if (manager == null)
        {
            throw new IllegalStateException("DSpace kernel cannot be null");
        }
        return manager;
    }

    /**
     * @param <T>
     * @param type
     * @return a Singleton Instance of the Service
     */
    public <T> T getSingletonService(Class<T> type)
    {
        return getServiceManager().getServiceByName(type.getName(), type);
    }

}
