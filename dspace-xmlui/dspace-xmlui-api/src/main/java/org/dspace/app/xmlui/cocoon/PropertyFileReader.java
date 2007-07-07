/*
 * PropertyFileReader.java
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

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.SourceResolver;

/**
 * Class will read the the specified configuration file via the property-file parameter
 * 
 * It accepts the name of the property file to read and the properties to set to the sitemap scope
 * For example:
 * 		<map:act type="PropertyFileReader">
 *  		<map:parameter name="property-file" value="/absolute/path/to/property/file />
 *			<map:parameter name="some.property" value="some_property" />
 *			...
 *		</map:act>
 * Will place the value of the "some.property" property in the "some_property" variable to be
 * used in the sitemap using the {some_property} syntax.
 * 
 * @author Jay Paz
 * 
 */
public class PropertyFileReader {

	public Map act(Redirector redirector, SourceResolver resolver,
			Map objectModel, String source, Parameters parameters)
			throws Exception {
		Map<String, String> map = new HashMap<String, String>();

		String propertyFile = parameters.getParameter("property-file");
		
		Properties props = new Properties();
		InputStream in = new FileInputStream(propertyFile);
		props.load(in);
		in.close();
		
		final String[] parameterNames = parameters.getNames();
		
		for (int i = 0; i < parameterNames.length; i++) {
			final String paramName = parameterNames[i];
			if ("property-file".equals(paramName)) {
				continue;
			}
			map.put(parameters.getParameter(paramName), props
					.getProperty(paramName));
		}

		return map;
	}
}
