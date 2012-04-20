/**
 * Copyright (C) 2011 SeDiCI <info@sedici.unlp.edu.ar>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ar.edu.unlp.sedici.xmlui.actions;

import java.util.HashMap;
import java.util.Map;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;


public class DpsacePropertyAction extends AbstractAction
{

	private static final Logger log = Logger.getLogger(DpsacePropertyAction.class);
	
	public Map act(Redirector redirector, SourceResolver resolver, Map objectModel,
            String source, Parameters parameters) throws Exception
    {

		String module = parameters.getParameter("module");
    	String key = parameters.getParameter("property");
    	String alias = parameters.getParameter("alias");
    	String value;
    	try {
    		value = ConfigurationManager.getProperty(module, key);
        } catch (Exception e) {
        	this.getLogger().error("Fallo la recuperación de la property "+ key + " del modulo " + module +" en el action 'DpsacePropertyAction'", e);
        	value="";
		}
    	
    	if (alias == null || "".equals(alias))
    		alias=module+"."+key;
    	
    	Request request = ObjectModelHelper.getRequest(objectModel);
    	request.setAttribute(alias, value);
    	this.getLogger().debug("Seteo en el request la property "+ key + "del modulo " + module +" con value "+value+" , bajo el nombre de "+alias+" en el action 'DpsacePropertyAction'");
    	    
    	Map params = new HashMap();
    	params.put(alias, value);
    	
		return params;
    }
	
}

