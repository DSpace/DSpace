/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.importexport;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.dspace.app.cris.util.UtilsXLS;


public class ExcelBulkChanges implements IBulkChanges
{
	
    private static Logger log = Logger.getLogger(ExcelBulkChanges.class);
	
    public static final String HEADER_SOURCEID = "SOURCEID";

    public static final String HEADER_SOURCEREF = "SOURCEREF";

    public static final String HEADER_UUID = "UUID";

    public static final String HEADER_CRISID = "CRISID";

    public static final String HEADER_ACTION = "ACTION";

    public static final String HEADER_SOURCEID_PARENT = "SOURCEID_PARENT";

    public static final String HEADER_SOURCEREF_PARENT = "SOURCEREF_PARENT";

    public static final String HEADER_CRISID_PARENT = "CRISID_PARENT";

    public final static String[] HEADER_COLUMNS = new String[] { HEADER_ACTION,
            HEADER_CRISID, HEADER_UUID, HEADER_SOURCEREF, HEADER_SOURCEID };

    public final static String[] HEADER_NESTED_COLUMNS = new String[] {
            HEADER_CRISID_PARENT, HEADER_SOURCEREF_PARENT, HEADER_SOURCEID_PARENT, HEADER_UUID, HEADER_SOURCEREF,
            HEADER_SOURCEID };
	
	private HSSFSheet mainObjects;
	private HSSFSheet nestedObjects;
	
	private List<String> mainHeaders = new ArrayList<String>();
	private List<String> nestedHeaders = new ArrayList<String>();

    public ExcelBulkChanges(HSSFWorkbook workbook)
    {
        Row row;
        this.mainObjects = workbook.getSheet("main_entities");
        this.nestedObjects = workbook.getSheet("nested_entities");
        row = mainObjects.getRow(0);
        for (int column = 0; column<row.getLastCellNum() + 1; column++)
        {
        	if (row.getCell(column) == null)
        		continue;
        	
        	String cellContent = UtilsXLS.stringCellValue(row.getCell(column)).trim();
            if (StringUtils.isNotBlank(cellContent))
            {
				mainHeaders.add(cellContent);
                if (HEADER_COLUMNS.length > column && !StringUtils
                        .equalsIgnoreCase(cellContent, HEADER_COLUMNS[column]))
                {
                    throw new IllegalArgumentException(
                            "Invalid excel file[main_entities sheet] - unexpected header column "
                                    + column + " -> " + cellContent
                                    + " expected " + HEADER_COLUMNS[column]);
        		}        		
        	}    
        }
        
        if (mainHeaders.size() < HEADER_COLUMNS.length)
        {
            throw new IllegalArgumentException(
                    "Invalid excel file[main_entities sheet] - unexpected header row: missing the required first 5 cells (action, CRISID, UUID, SOURCEREF, SOURCEID)");
        }
        
        if (nestedObjects != null)
        {
	        row = nestedObjects.getRow(0);
	        for (int column = 0; column<row.getLastCellNum() + 1; column++)
	        {
	        	if (row.getCell(column) == null)
	        		continue;
	        	
                String cellContent = UtilsXLS.
                        stringCellValue(row.getCell(column)).trim();
                if (StringUtils.isNotBlank(cellContent))
                {
	        		nestedHeaders.add(cellContent);
                    if (HEADER_NESTED_COLUMNS.length > column
                            && !StringUtils.equalsIgnoreCase(cellContent,
                                    HEADER_NESTED_COLUMNS[column]))
                    {
                        throw new IllegalArgumentException(
                                "Invalid excel file[nested_entities sheet] - unexpected header column "
                                        + column + " -> " + cellContent
                                        + " expected "
                                        + HEADER_NESTED_COLUMNS[column]);
	        		}	        		
	        	}
	        }
	        
            if (nestedHeaders.size() < HEADER_NESTED_COLUMNS.length)
            {
                throw new IllegalArgumentException(
                        "Invalid excel file[nested_entities sheet] - unexpected header row: missing the required first 5 cells (action, CRISID, UUID, SOURCEREF, SOURCEID)");
	        }
        }
	}

	@Override
    public boolean hasPropertyDefinition(String shortName)
    {
        return mainHeaders.contains(shortName)
                || (nestedHeaders != null && nestedHeaders.contains(shortName));
	}

	@Override
    public int size()
    {
		return mainObjects.getLastRowNum() + (nestedObjects != null ? nestedObjects.getLastRowNum() : 0);
	}

	@Override
    public IBulkChange getChanges(int i)
    {
        if (i < mainObjects.getLastRowNum() + 1)
        {
            log.debug("Retrieve in entity sheet row #" + i);
            return new ExcelBulkChange(mainObjects.getRow(i), mainHeaders);
        }
        else
        {
            log.debug("Retrieve in nested sheet row #" + (i - (mainObjects.getLastRowNum())));
            return getNestedChanges((i - (mainObjects.getLastRowNum())));
        }
		}

    private IBulkChangeNested getNestedChanges(int i)
    {
        log.debug("Retrieve in nested sheet row #" + (i));
        return new ExcelBulkChangeNested(nestedObjects.getRow(i), nestedHeaders);
	}
}
