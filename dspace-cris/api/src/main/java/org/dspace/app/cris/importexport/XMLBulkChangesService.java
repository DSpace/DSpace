/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.importexport;

import it.cilea.osd.common.util.Utils;
import it.cilea.osd.jdyna.model.ANestedPropertiesDefinition;
import it.cilea.osd.jdyna.model.ANestedProperty;
import it.cilea.osd.jdyna.model.ATypeNestedObject;
import it.cilea.osd.jdyna.model.IContainable;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.model.Property;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.export.ExportConstants;
import org.dspace.app.cris.model.jdyna.ACrisNestedObject;
import org.dspace.app.cris.util.UtilsXSD;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class XMLBulkChangesService implements IBulkChangesService {
	public static final String SERVICE_NAME = "XMLBulkChangesService";
	public static final String FORMAT = "xml";
	
    private static final DateFormat dateFormat = new SimpleDateFormat(
            "dd-MM-yyyy_HH-mm-ss");
    
    public <ACO extends ACrisObject<P, TP, NP, NTP, ACNO, ATNO>, P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, 
	ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> IBulkChanges getBulkChanges(InputStream input, File dir,
    		Class<ACO> crisObjectClazz, Class<TP> pDefClazz, List<IContainable> metadataALL, List<IContainable> metadataNested) throws IOException,
			FileNotFoundException, NoSuchFieldException,
			InstantiationException, IllegalAccessException, SAXException,
			ParserConfigurationException {
		File filexsd = null;
        File filexml = null;

        // build filexml
        String nameXML = "xml-" + dateFormat.format(new Date()) + ".xml";
        filexml = new File(dir, nameXML);
        filexml.createNewFile();
        FileOutputStream out = new FileOutputStream(filexml);
        Utils.bufferedCopy(input, out);
        out.close();

        // create xsd and write up
        String nameXSD = "xsd-" + dateFormat.format(new Date()) + ".xsd";
        filexsd = new File(dir, nameXSD);
        filexsd.createNewFile();
        FileWriter writer = new FileWriter(filexsd);
        
        String[] namespace = UtilsXSD.getNamespace(pDefClazz);
        String[] elementRoot = UtilsXSD.getElementRoot(pDefClazz);
        filexsd = generateTemplate(writer, dir, metadataALL, metadataNested, filexsd, elementRoot, 
        		namespace[0]+":",
                namespace[1],
                namespace[1],
                new String[] {
                        ExportConstants.NAME_PUBLICID_ATTRIBUTE,
                        ExportConstants.NAME_BUSINESSID_ATTRIBUTE,
                        ExportConstants.NAME_ID_ATTRIBUTE,
                        ExportConstants.NAME_TYPE_ATTRIBUTE },
                new boolean[] { true, false, false,
                        true });

        // create xsd validator
        SchemaFactory factory = SchemaFactory
                .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Source schemaSource = new StreamSource(filexsd);

        Schema schema = factory.newSchema(schemaSource);
        // validate source xml to xsd
        Validator validator = schema.newValidator();
        validator.validate(new StreamSource(filexml));

        // parse xml to dom
        DocumentBuilder parser = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder();
        Document document = parser.parse(filexml);
		return new XmlBulkChanges(document);
	}

    public File generateTemplate(Writer writer, File dir,
            List<IContainable> metadata, List<IContainable> metadataNested, File filexsd, String[] elementsRoot,
            String namespace, String namespaceValue, String namespaceTarget,
            String[] attributeMainRow, boolean[] attributeMainRowRequired)
            throws IOException, NoSuchFieldException, SecurityException,
            InstantiationException, IllegalAccessException
    {

        UtilsXSD xsd = new UtilsXSD(writer);
        xsd.createXSD(metadata, elementsRoot, namespace, namespaceValue,
                namespaceTarget, attributeMainRow, attributeMainRowRequired);
        return filexsd;
    }

    @Override
    public String getFormat()
    {
        return FORMAT;  
    }

}
