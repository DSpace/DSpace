/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cocoon.core.container.spring.avalon;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.log4j.Logger;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Proxy for all poolable components.
 *
 * @version $Id: PoolableProxyHandler.java 639686 2008-03-21 15:58:40Z gkossakowski $
 * @since 2.2
 */
public class PoolableProxyHandler implements InvocationHandler, Runnable {

    private final ThreadLocal componentHolder = new ThreadLocal();
    private final PoolableFactoryBean handler;
    private final String attributeName;

    private Logger log = Logger.getLogger(PoolableProxyHandler.class);
    
    public PoolableProxyHandler(PoolableFactoryBean handler) {
        this.handler = handler;
        this.attributeName = PoolableProxyHandler.class.getName() + '/' + this.handler.hashCode();
    }

    /**
     * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
     */
    public Object invoke(Object proxy, Method method, Object[] args)
    throws Throwable {
        if ( method.getName().equals("putBackIntoAvalonPool") ) {
            this.run();
            // attributes might already been destroyed, because this handler might be run
            // for a component that is released by another component insided recycle()
            // which got called by PoolableFactoryBean.enteringPool() <- putIntoPool() <- run() (below),
            // which is registered as destruction callback on the request attributes -
            // such a destruction callback is called *after* the request attributes
            // are set back to null
            final RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                // removing the attribute removes the call back handler!
                attributes.removeAttribute(this.attributeName, RequestAttributes.SCOPE_REQUEST);
            }
            return null;
        }
        if ( method.getName().equals("hashCode") && args == null ) {
            return new Integer(this.hashCode());
        }
        if ( this.componentHolder.get() == null ) {
            this.componentHolder.set(this.handler.getFromPool());
            RequestContextHolder.currentRequestAttributes().registerDestructionCallback(this.attributeName, this, RequestAttributes.SCOPE_REQUEST);
        }
        try {
            return method.invoke(this.componentHolder.get(), args);
        } catch (InvocationTargetException ite) {
            throw ite.getTargetException();
        }
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {
        final Object o = this.componentHolder.get();
        if ( o != null ) {
            this.handler.putIntoPool(o);
        }
        //this.componentHolder.set(null);
        log.trace("Eliminando <ThreadLocal> componentHolder, que contenia: "+o);
        this.componentHolder.remove();
    }
}