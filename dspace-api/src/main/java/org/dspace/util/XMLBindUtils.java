/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

/**
 * Utility class for XML Bindings
 * 
 * @author DSpace @ Lyncode <dspace@lyncode.com>
 */
public class XMLBindUtils {
	public static Object unmarshall (InputStream in, Class<?> c) throws JAXBException  {
		JAXBContext context = JAXBContext.newInstance(c.getPackage().getName());
            Unmarshaller unmarshaller = context.createUnmarshaller();
            return unmarshaller.unmarshal(in);
    }

    public static void marshall (OutputStream out, Object obj) throws JAXBException {
    	JAXBContext context = JAXBContext.newInstance(obj.getClass().getPackage().getName());
		Marshaller marshaller = context.createMarshaller();
		marshaller.marshal(obj, out);
    }
}
