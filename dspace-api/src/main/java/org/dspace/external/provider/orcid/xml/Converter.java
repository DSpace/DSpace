/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.provider.orcid.xml;

import java.io.InputStream;
import java.net.URISyntaxException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.xml.sax.SAXException;

/**
 * @param <T> type
 * @author Antoine Snyers (antoine at atmire.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public abstract class Converter<T> {

    public abstract T convert(InputStream document);

    protected Object unmarshall(InputStream input, Class<?> type) throws SAXException, URISyntaxException {
        try {
            XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
            // disallow DTD parsing to ensure no XXE attacks can occur
            xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(input);

            JAXBContext context = JAXBContext.newInstance(type);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            return unmarshaller.unmarshal(xmlStreamReader);
        } catch (JAXBException | XMLStreamException e) {
            throw new RuntimeException("Unable to unmarshall orcid message: " + e);
        }
    }
}
