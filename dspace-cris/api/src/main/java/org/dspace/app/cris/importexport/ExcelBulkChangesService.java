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
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;

import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.jdyna.ACrisNestedObject;
import org.xml.sax.SAXException;

public class ExcelBulkChangesService implements IBulkChangesService
{
    private String encoding = "Cp1252";

    public static final String SERVICE_NAME = "ExcelBulkChangesService";

    /**
     * due to the limitation of jxl the format is Excel 97-2003
     */
    public static final String FORMAT = "xls";

    private static final DateFormat dateFormat = new SimpleDateFormat(
            "dd-MM-yyyy_HH-mm-ss");
    
    public void setEncoding(String encoding)
    {
		this.encoding = encoding;
	}
    
    public String getEncoding()
    {
		return encoding;
	}
    
    public <ACO extends ACrisObject<P, TP, NP, NTP, ACNO, ATNO>, P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> IBulkChanges getBulkChanges(
            InputStream input, File dir, Class<ACO> crisObjectClazz,
            Class<TP> pDefClazz, List<IContainable> metadataALL, List<IContainable> metadataNested)
                    throws IOException, FileNotFoundException,
                    NoSuchFieldException, InstantiationException,
                    IllegalAccessException, SAXException,
                    ParserConfigurationException
    {
        File fileXls = null;

        // build filexml
        String nameXML = "excel97-" + dateFormat.format(new Date()) + ".xls";
        fileXls = new File(dir, nameXML);
        fileXls.createNewFile();
        FileOutputStream out = new FileOutputStream(fileXls);
        Utils.bufferedCopy(input, out);
        out.close();
        
        WorkbookSettings ws = new WorkbookSettings();
        ws.setEncoding(encoding);
        Workbook workbook = null;
        try
        {
			workbook = Workbook.getWorkbook(fileXls, ws);
        }
        catch (BiffException e)
        {
            throw new IOException("Invalid excel file: " + e.getMessage());
        }
        return new ExcelBulkChanges(workbook);
    }

    public File generateTemplate(Writer writer, File dir,
            List<IContainable> metadata, List<IContainable> metadataNested, File filexsd, String[] elementsRoot,
            String namespace, String namespaceValue, String namespaceTarget,
            String[] attributeMainRow, boolean[] attributeMainRowRequired)
            throws IOException, NoSuchFieldException, SecurityException,
            InstantiationException, IllegalAccessException
    {
        WritableWorkbook workbook = Workbook.createWorkbook(filexsd);

    	WritableSheet sheetEntities = workbook.createSheet("main_entities", 0);
        WritableSheet sheetNested = workbook.createSheet("main_nested", 1);
    	int xEntities = 0;
    	int xNested = 0;
        for (String headerColumn : ExcelBulkChanges.HEADER_COLUMNS)
        {
            try
            {
				sheetEntities.addCell(new Label(xEntities++, 0, headerColumn));
            }
            catch (WriteException e)
            {
                throw new IOException(
                        "Error to create template from fixed header columns: "
                                + e.getMessage());
            }

        }
        for (String headerColumn : ExcelBulkChanges.HEADER_NESTED_COLUMNS)
        {
            try
            {
				sheetNested.addCell(new Label(xNested++, 0, headerColumn));
            }
            catch (WriteException e)
            {
                throw new IOException(
                        "Error to create template from fixed nested header columns: "
                                + e.getMessage());
			}
	    	
	    }
        
        for (IContainable cont : metadata)
        {
            try
            {
                sheetEntities.addCell(
                        new Label(xEntities++, 0, cont.getShortName()));
            }
            catch (WriteException e)
            {
                throw new IOException(
                        "Error to create template from dynamic metadata: "
                                + e.getMessage());
            }
        }
        for (IContainable cont : metadataNested)
        {
            try
            {
                sheetNested
                        .addCell(new Label(xNested++, 0, cont.getShortName()));
            }
            catch (WriteException e)
            {
                throw new IOException(
                        "Error to create template from dynamic nested metadata: "
                                + e.getMessage());
			}
	    }

    	return filexsd;
    }

    @Override
    public String getFormat()
    {
        return FORMAT;
    }

}
