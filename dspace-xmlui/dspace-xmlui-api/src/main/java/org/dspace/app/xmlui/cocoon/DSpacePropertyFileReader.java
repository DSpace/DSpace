/*
 * DSpacePropertyFileReader.java
 *
 * Version: $Revision: 1.2 $
 *
 * Date: $Date: 2006/04/25 15:24:23 $
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
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
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
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
package org.dspace.app.xmlui.cocoon;

import java.util.HashMap;
import java.util.Map;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.SourceResolver;
import org.dspace.core.ConfigurationManager;

/**
 * Class will read the DSpace configuration file via the ConfigurationManager
 * 
 * It accepts the name of the property to read and the name of the variable to
 * use in the sitemap scope.  
 * For example:
 * 		<map:act type="DSpacePropertyFileReader">
 *			<map:parameter name="dspace.dir" value="dspace_dir" />
 *			<map:transform type="Include" src="{dspace_dir}/config/news.xml" /> 
 *		</map:act>
 * Will place the value of the "dspace.dir" property in the "dspace_dir" variable to be
 * used in the sitemap.
 * 
 * @author Jay Paz
 * 
 */
public class DSpacePropertyFileReader extends AbstractAction {
	public Map act(Redirector redirector, SourceResolver resolver,
			Map objectModel, String source, Parameters parameters)
			throws Exception {

		Map<String, String> map = new HashMap<String, String>();

		final String[] parameterNames = parameters.getNames();
		
		for (int i = 0; i < parameterNames.length; i++) {
			final String paramName = parameterNames[i];
			map.put(parameters.getParameter(paramName),
					ConfigurationManager.getProperty(paramName));
		}
		
		return map;
	}
}