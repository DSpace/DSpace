/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.dspace.core.ConfigurationManager;
import org.springframework.core.io.DefaultResourceLoader;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class CustomClasspathEntityResolver implements EntityResolver
{
	private Class clazz;

	public CustomClasspathEntityResolver(Class clazz) {
		this.clazz = clazz;
	}
    @Override
    public InputSource resolveEntity(String publicId, String systemId)
            throws SAXException, IOException
    {
        InputSource inputSource = null;
        InputStream inputStream = null;

        DefaultResourceLoader loader = new DefaultResourceLoader();

        try
        {
            inputStream = loader.getResource(systemId).getInputStream();
        }
        catch (Exception ex)
        {
            if (systemId.startsWith("classpath://"))
            {
                try
                {
                	inputStream = clazz.getResourceAsStream(systemId.replaceFirst("classpath://", "/"));

                    if (inputStream == null)
                    {
                        throw new FileNotFoundException();
                    }
                }
                catch (Exception ex1)
                {
                    
                    String basePath = ConfigurationManager.getProperty("dspace.dir") + "/config/";
                    try {
						inputStream = new FileInputStream(basePath + systemId.substring(systemId.lastIndexOf("/") + 1));
                    }
                    catch(Exception ex2) {
                    // No action; just let the null InputSource pass through
                    throw new SAXException("The entity " + publicId + " "
                            + systemId + " was not found in the classpath");
                }
            }
            }
        }
        inputSource = new InputSource(inputStream);
        return inputSource;
    }

}
