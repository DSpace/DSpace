/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cocoon.components.treeprocessor.variables;

import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;

import org.apache.cocoon.sitemap.PatternException;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * @version $Id: VariableResolverFactory.java 610271 2008-01-09 03:59:08Z rgoers $
 */
public class VariableResolverFactory {

    //private static ThreadLocal disposableCollector = new ThreadLocal();
	private static Logger log = Logger.getLogger(VariableResolverFactory.class);
	
    /**
     * Set the thread-local list where all created resolvers that need to be
     * disposed will be collected.
     * <p>
     * The purpose of collecting resolvers is to avoid manual release (or lack thereof)
     * that requires most <code>ProcessingNodes</code> to implement <code>Disposable</code>.
     */
    public static void setDisposableCollector(List collector) {
    	//This variable is not used anywhere and might generate memory leaks
        //disposableCollector.set(collector);
    	log.trace("Omitiendo setear <ThreadLocal> disposableCollector con: "+collector);
    }

    /**
     * Does an expression need resolving (i.e. contain {...} patterns) ?
     */
    public static boolean needsResolve(String expression) {
        if (expression == null || expression.length() == 0) {
            return false;
        }

        // Is the first char a '{' ?
        if (expression.charAt(0) == '{') {
            return true;
        }

        if (expression.length() < 2) {
            return false;
        }

        // Is there any unescaped '{' ?
        int pos = 1;
        while ( (pos = expression.indexOf('{', pos)) != -1) {
            // Found a '{' : is it escaped ?
            if (expression.charAt(pos - 1) != '\\') {
                // No : need to resolve
                return true;
            }
            pos++;
        }
        // Nothing found...
        return false;
    }

    /**
     * Unescape an expression that doesn't need to be resolved, but may contain
     * escaped '{' characters.
     *
     * @param expression the expression to unescape.
     * @return the unescaped result, or <code>expression</code> if unescaping isn't necessary.
     */
    public static String unescape(String expression) {
        // Does it need escaping ?
        if (expression == null || expression.indexOf("\\{") == -1) {
            return expression;
        }

        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < expression.length(); i++) {
            char ch = expression.charAt(i);
            if (ch != '\\' || i >= (expression.length() - 1) || expression.charAt(i+1) != '{') {
                buf.append(ch);
            }
        }

        return buf.toString();
    }

    /**
     * Get a resolver for a given expression. Chooses the most efficient implementation
     * depending on <code>expression</code>.
     */
    public static VariableResolver getResolver(String expression, ServiceManager manager) throws PatternException {
        if (needsResolve(expression)) {
            VariableResolver resolver;
            try {
                resolver = (VariableResolver) manager.lookup(VariableResolver.ROLE);
                resolver.setExpression(expression);
            } catch (ServiceException e) {
                throw new PatternException("Couldn't obtain VariableResolver.", e);
            }

            return resolver;
        }

        return new NOPVariableResolver(expression);
    }
}
