/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.dspace.app.bulkedit.DSpaceCSV;
import org.dspace.app.bulkedit.DSpaceCSVLine;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.SelfNamedPlugin;

public class ExcelDisseminationCrosswalk extends SelfNamedPlugin
        implements StreamGenericDisseminationCrosswalk, FileNameDisseminator
{
    private static Logger log = Logger
            .getLogger(ExcelDisseminationCrosswalk.class);

    public static final String CHECK_CONFIGURATION_EXPORT_EXCEL = "excel.exports.extended.flag";

    public static final String METADATAVALUES_SEPARATOR = "metadatavalues.separator";

    public static final String FILE_NAME_EXPORT_EXCEL = "excel.export.filename";

    public static final String EXCEL_TYPE_LABEL = "collection";

    public static final String EXCEL_ID_LABEL = "id";

    @Override
    public boolean canDisseminate(Context context, DSpaceObject dso)
    {
        return (dso.getType() == Constants.ITEM);
    }

    @Override
    public void disseminate(Context context, List<DSpaceObject> dso,
            OutputStream out) throws CrosswalkException, IOException,
                    SQLException, AuthorizeException
    {

        // Process each item
        DSpaceCSV csv = new DSpaceCSV(false);
        for (DSpaceObject toExport : dso)
        {
            try
            {
                csv.addItem((Item) toExport);
            }
            catch (Exception e)
            {
                log.error(e.getMessage(), e);
            }
        }

        buildExcel(out, csv);
    }

    private void buildExcel(OutputStream out, DSpaceCSV csv)
    {
        int rowNum = 0;

        HSSFWorkbook wb = new HSSFWorkbook();
        HSSFSheet sheet = wb.createSheet("-");
        HSSFRow xlsRow = sheet.createRow(rowNum);
        HSSFCellStyle headerStyle = wb.createCellStyle();

        HSSFFont bold = wb.createFont();
        bold.setBoldweight((short) 700);
        bold.setColor((short) 0);

        headerStyle.setFont(bold);

        HSSFCell cellCol, cellID, cell;
        sheet.autoSizeColumn(0);

        cellID = xlsRow.createCell(0);
        cellID.setCellStyle(headerStyle);
        cellID.setCellValue(new HSSFRichTextString(EXCEL_ID_LABEL));

        cellCol = xlsRow.createCell(1);
        cellCol.setCellStyle(headerStyle);
        cellCol.setCellValue(new HSSFRichTextString(EXCEL_TYPE_LABEL));

        int i = 2;
        for (String heading : csv.getHeadings())
        {
            cell = xlsRow.createCell(i);
            cell.setCellStyle(headerStyle);
            cell.setCellValue(new HSSFRichTextString(heading));
            i++;
        }

        int exportCounter = 0;

        for (DSpaceCSVLine line : csv.getCSVLines())
        {
            xlsRow = sheet.createRow(++rowNum);
            cell = xlsRow.createCell(0, HSSFCell.CELL_TYPE_NUMERIC);
            writeCell(line.getID(), cell);
            cell = xlsRow.createCell(1, HSSFCell.CELL_TYPE_STRING);
            writeCell(line.get("collection"), cell);

            i = 2;
            for (String heading : csv.getHeadings())
            {
                List<String> values = line.get(heading);           
                if (values != null && !"collection".equals(heading)) {
                    cell = xlsRow.createCell(i, HSSFCell.CELL_TYPE_STRING);
                    String value = valueToCell(values,
                            getValueSeparator());
                    writeCell(value, cell);                
                }
                i++;
            }
            exportCounter++;
        }

        log.info("INFO: items exported: " + exportCounter);

        try
        {
            wb.write(out);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }

    private String getValueSeparator()
    {
        // Get the value separator
        String valueSeparator = ConfigurationManager.getProperty("bulkedit", "valueseparator");
        if ((valueSeparator != null) && (!"".equals(valueSeparator.trim())))
        {
            valueSeparator = valueSeparator.trim();
        }
        else
        {
            valueSeparator = "||";
        }
        return valueSeparator;
    }

    @Override
    public String getMIMEType()
    {
        return "application/vnd.ms-excel";
    }

    protected static void writeCell(Object value, HSSFCell cell)
    {
        if (value instanceof Number)
        {
            Number num = (Number) value;
            cell.setCellValue(num.doubleValue());
        }
        else if (value instanceof Date)
            cell.setCellValue((Date) value);
        else if (value instanceof Calendar)
            cell.setCellValue((Calendar) value);
        else
        {
            if (value.toString().length() > 32750)
            {
                value = value.toString().substring(0, 32750);
            }
            cell.setCellValue(new HSSFRichTextString(escapeColumnValue(value)));
        }
    }

    protected static String escapeColumnValue(Object rawValue)
    {
        if (rawValue == null)
        {
            return null;
        }
        else
        {
            String returnString = ObjectUtils.toString(rawValue);
            returnString = StringEscapeUtils
                    .escapeJava(StringUtils.trimToEmpty(returnString));
            returnString = StringUtils.replace(StringUtils.trim(returnString),
                    "\\t", " ");
            returnString = StringUtils.replace(StringUtils.trim(returnString),
                    "\\r", " ");
            returnString = StringEscapeUtils.unescapeJava(returnString);
            return returnString;
        }
    }

    @Override
    public void disseminate(Context context, DSpaceObject dso, OutputStream out)
            throws CrosswalkException, IOException, SQLException,
            AuthorizeException
    {
        // Process each item
        DSpaceCSV csv = new DSpaceCSV(false);
        try
        {
            csv.addItem((Item) dso);
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }

        buildExcel(out, csv);
    }

    @Override
    public String getFileName()
    {
        String result = ConfigurationManager
                .getProperty(FILE_NAME_EXPORT_EXCEL);
        if (StringUtils.isNotEmpty(result))
            return result;
        return "export.xls";
    }

    protected String valueToCell(List<String> values, String valueSeparator)
    {
        // Check there is some content
        if (values == null)
        {
            return "";
        }

        // Get on with the work
        String s;
        if (values.size() == 1)
        {
            s = values.get(0);
        }
        else
        {
            // Concatenate any fields together
            StringBuilder str = new StringBuilder();

            for (String value : values)
            {
                if (str.length() > 0)
                {
                    str.append(valueSeparator);
                }

                str.append(value);
            }

            s = str.toString();
        }

        return s;
    }
}
