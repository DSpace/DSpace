/*
 */
package org.datadryad.rest.converters;

import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import org.datadryad.rest.models.Manuscript;
import org.datadryad.rest.legacymodels.LegacyManuscript;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class ManuscriptToLegacyXMLConverter {

    public static void main(String args[]) {
        Manuscript manuscript = new Manuscript();
        manuscript.configureTestValues();
        try {
            convertToInternalXML(manuscript, System.out);
        } catch (JAXBException ex) {
            System.err.println("Error converting manuscript to internal XML:" + ex);
        }
    }

    public static void convertToInternalXML(Manuscript manuscript, OutputStream outputStream) throws JAXBException {
        String journalName = null;
        if(manuscript.organization != null) {
            journalName = manuscript.organization.organizationName;
        }
        LegacyManuscript legacyManuscript = new LegacyManuscript(manuscript, journalName);
        JAXBContext context = JAXBContext.newInstance(LegacyManuscript.class);
        Marshaller marshaller = context.createMarshaller();
        // Encoding is UTF-8 by default - http://docs.oracle.com/javaee/5/api/javax/xml/bind/Marshaller.html
        marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(legacyManuscript, outputStream);
    }
}
