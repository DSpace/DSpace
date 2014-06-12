/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package it.cineca.surplus.ir.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.DefaultResourceLoader;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class CustomClasspathEntityResolver implements EntityResolver
{

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
                    inputStream = ClassLoader
                            .getSystemResourceAsStream(systemId.replaceFirst(
                                    "classpath://", ""));
                    if (inputStream == null)
                    {
                        throw new FileNotFoundException();
                    }
                    inputSource = new InputSource(inputStream);
                    return inputSource;
                }
                catch (Exception e)
                {
                    // No action; just let the null InputSource pass through
                    throw new SAXException("The entity " + publicId + " "
                            + systemId + " was not found in the classpath");
                }
            }

        }
        return inputSource;
    }

}
