/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.dspace.kernel;

/**
 * Beans that have a lifecycle and can be controlled via their lifecycle implement this interface.
 * Based on the Sakai K2 lifecycle interface -AZ
 * 
 * @param <T> the type of object managed by this lifecycle.
 */
public interface CommonLifecycle<T> {

    /**
     * Starts the bean. This initializes and causes the object to begin functioning.
     * Should not happen automatically when the object is created.
     */
    public void start();

    /**
     * Stops the bean. This turns the object off and causes related things to be shutdown.
     * Object should be able to be started again.
     */
    public void stop();

    /**
     * Gets a reference to the bean that is being managed inside this lifecycle.
     * @return the managed object
     */
    public T getManagedBean();

    /**
     * Destroy the managed bean entirely. It will be stopped first if not stopped and cannot be
     * started again afterwards.
     */
    public void destroy();

}
