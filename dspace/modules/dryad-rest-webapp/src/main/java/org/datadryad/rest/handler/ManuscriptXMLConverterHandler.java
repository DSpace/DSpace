/*
 */
package org.datadryad.rest.handler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import javax.xml.bind.JAXBException;
import org.datadryad.rest.converters.ManuscriptToLegacyXMLConverter;
import org.datadryad.rest.models.Manuscript;
import org.datadryad.rest.storage.StoragePath;
import org.datadryad.rest.utils.DryadPathUtilities;

/**
 * Saves manuscripts as XML files when created or updated
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
class ManuscriptXMLConverterHandler implements HandlerInterface<Manuscript> {

    public ManuscriptXMLConverterHandler() {
    }

    @Override
    public void handleCreate(StoragePath path, Manuscript manuscript) throws HandlerException {
        writeXML(path, manuscript);
    }

    @Override
    public void handleUpdate(StoragePath path, Manuscript manuscript) throws HandlerException {
        writeXML(path, manuscript);
    }

    @Override
    public void handleDelete(StoragePath path, Manuscript manuscript) throws HandlerException {
        // TODO: delete the XML file?
    }

    private void writeXML(StoragePath path, Manuscript manuscript) throws HandlerException {
        String organizationCode = DryadPathUtilities.getOrganizationCode(path);
        String fileName = DryadPathUtilities.getTargetFilename(manuscript);
        String outputDirectory = DryadPathUtilities.getOutputDirectory(organizationCode);
        File outputFile = new File(outputDirectory, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(outputFile);
            ManuscriptToLegacyXMLConverter.convertToInternalXML(manuscript, fos);
        } catch (FileNotFoundException ex) {
            throw new HandlerException("Unable to write XML file", ex);
        } catch (JAXBException ex) {
            throw new HandlerException("Unable to serialize manuscript to XML", ex);
        }
    }

}
