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
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.read.biff.BiffException;

import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.jdyna.ACrisNestedObject;
import org.dspace.app.cris.util.CSVBulkChanges;
import org.xml.sax.SAXException;

public class CSVBulkChangesService implements IBulkChangesService {
	private String encoding = "Cp1252";
	
	public static final String SERVICE_NAME = "XMLBulkChangesService";
	
    private static final DateFormat dateFormat = new SimpleDateFormat(
            "dd-MM-yyyy_HH-mm-ss");
    
    public void setEncoding(String encoding) {
		this.encoding = encoding;
	}
    
    public String getEncoding() {
		return encoding;
	}
    
    public <ACO extends ACrisObject<P, TP, NP, NTP, ACNO, ATNO>, P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, 
	ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> IBulkChanges getBulkChanges(InputStream input, File dir,
    		Class<ACO> crisObjectClazz, Class<TP> pDefClazz, List<IContainable> metadataALL) throws IOException,
			FileNotFoundException, NoSuchFieldException,
			InstantiationException, IllegalAccessException, SAXException,
			ParserConfigurationException {
        File fileXls = null;

        // build filexml
        String nameXML = "xml-" + dateFormat.format(new Date()) + ".xml";
        fileXls = new File(dir, nameXML);
        fileXls.createNewFile();
        FileOutputStream out = new FileOutputStream(fileXls);
        Utils.bufferedCopy(input, out);
        out.close();
        
        
        WorkbookSettings ws = new WorkbookSettings();
        ws.setEncoding(encoding);
        Workbook workbook = null;
        try {
			workbook = Workbook.getWorkbook(fileXls, ws);
		} catch (BiffException e) {
			throw new IOException("Invalid excel file: "+e.getMessage());
		}
		return new CSVBulkChanges(workbook);
	}

    public File generateTemplate(Writer writer, File dir,
            List<IContainable> metadata, File filexsd, String[] elementsRoot,
            String namespace, String namespaceValue, String namespaceTarget,
            String[] attributeMainRow, boolean[] attributeMainRowRequired)
            throws IOException, NoSuchFieldException, SecurityException,
            InstantiationException, IllegalAccessException
    {
        return null;
    }

}
