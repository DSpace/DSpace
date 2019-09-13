/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.util;

import it.cilea.osd.jdyna.editor.AdvancedPropertyEditorSupport;
import it.cilea.osd.jdyna.model.ADecoratorPropertiesDefinition;
import it.cilea.osd.jdyna.model.ANestedProperty;
import it.cilea.osd.jdyna.model.AWidget;
import it.cilea.osd.jdyna.model.IContainable;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.model.Property;

import java.beans.PropertyEditor;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.dspace.app.cris.importexport.ExcelBulkChangesService;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.RestrictedField;
import org.dspace.app.cris.model.RestrictedFieldFile;
import org.dspace.app.cris.model.RestrictedFieldLocalOrRemoteFile;
import org.dspace.app.cris.model.VisibilityConstants;
import org.dspace.app.cris.model.jdyna.ACrisNestedObject;
import org.dspace.app.cris.model.jdyna.DecoratorRPNestedPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.DecoratorRPPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.DecoratorRestrictedField;
import org.dspace.app.cris.model.jdyna.RPNestedObject;
import org.dspace.app.cris.model.jdyna.RPNestedProperty;
import org.dspace.app.cris.model.jdyna.RPPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.RPProperty;
import org.dspace.app.cris.service.ApplicationService;

public class UtilsXLS {

	/**
	 * Characters sequence used to split multiple values in repeatable field
	 * escaped for use in regex expression
	 */
	public static final String ESCAPE_STOPFIELDS_EXCEL = "\\|\\|\\|";

	/**
	 * Characters sequence used to split multiple values in repeatable field
	 */
	public static final String STOPFIELDS_EXCEL = "|||";

	public static int createCell(ApplicationService applicationService, int y,
			int i, ADecoratorPropertiesDefinition decorator,
			ACrisObject researcher, HSSFSheet sheet) throws IOException {
		return createElement(applicationService, y, i, decorator.getReal(),
				decorator.getRendering(), researcher, sheet);
	}

	private static int createElement(ApplicationService applicationService,
			int y, int i, Object preal, AWidget rendering,
			ACrisObject researcher, HSSFSheet sheet) throws IOException {
		
	        PropertiesDefinition real = (PropertiesDefinition)preal;
			return createSimpleElement(applicationService, y, i,
					real.getShortName(), researcher
							.getProprietaDellaTipologia(real), sheet);
		
	}

	
	private static int createSimpleElement(
			ApplicationService applicationService, int y, int i,
			String shortName, List<Property> proprietaDellaTipologia,
			HSSFSheet sheet) {
		String field_value = "";
		boolean first = true;
		for (Property rr : proprietaDellaTipologia) {

			PropertyEditor pe = rr.getTypo().getRendering()
					.getImportPropertyEditor(applicationService, AdvancedPropertyEditorSupport.MODE_CSV);
			pe.setValue(rr.getObject());
			if (!first) {
				field_value += STOPFIELDS_EXCEL;
			}
			String tmp = pe.getAsText();			
			String field_visibility = VisibilityConstants.getDescription(rr
					.getVisibility());
			if(tmp.startsWith("[")) {
			    field_value += "[visibility=" +field_visibility+" " + tmp.substring(1, tmp.length());
			} else {
			    field_value += "[visibility=" +field_visibility+"]" + tmp;
			}
			first = false;

		}
		y = y + 1;
		addCell(sheet, y, i, field_value);
		addCell(sheet, y, 0, shortName);
		
		return y;
	}

	public static int createCell(ApplicationService applicationService, int y,
			int i, DecoratorRestrictedField decorator,
			ACrisObject researcher, HSSFSheet sheet)
			throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException {
		String shortName = decorator.getShortName();
		Method[] methods = researcher.getClass().getMethods();
		Object field = null;
		Method method = null;
		for (Method m : methods) {
			if (m.getName().toLowerCase()
					.equals("get" + shortName.toLowerCase())) {
				field = m.invoke(researcher, null);
				method = m;
				break;
			}
		}

		if (method.getReturnType().isAssignableFrom(List.class)) {
			String field_value = "";
			String field_visibility = "";
			boolean first = true;
			for (RestrictedField rr : (List<RestrictedField>) field) {

				if (!first) {
					field_value += STOPFIELDS_EXCEL;
				}
				field_value += rr.getValue();
				if (!first) {
					field_visibility += STOPFIELDS_EXCEL;
				}
				field_visibility += VisibilityConstants.getDescription(rr
						.getVisibility());
				first = false;

			}
			y = y + 1;
			addCell(sheet, y, 0, decorator.getShortName());
			
			y = y + 1;
			addCell(sheet, y, 0, decorator.getShortName()
					+ ImportExportUtils.LABELCAPTION_VISIBILITY_SUFFIX);

			addCell(sheet, y, i, field_value);
			addCell(sheet, y, i, field_visibility);

		} else if (method.getReturnType().isAssignableFrom(String.class)) {
			y = y + 1;
			addCell(sheet, y, i, (String) field);
			addCell(sheet, y, 0, decorator.getShortName());
		} else {
			if (RestrictedFieldLocalOrRemoteFile.class.isAssignableFrom(method
					.getReturnType())) {
				RestrictedFieldLocalOrRemoteFile rflor = (RestrictedFieldLocalOrRemoteFile) field;
				y = y + 1;
				if (StringUtils.isNotEmpty(rflor.getRemoteUrl())) {
					addCell(sheet, y, i, rflor.getRemoteUrl());
				} else {
					addCell(sheet, y, i, rflor.getMimeType()
							+ STOPFIELDS_EXCEL + rflor.getValue());
				}
				addCell(sheet, y, 0, decorator.getShortName());
				y = y + 1;
				addCell(sheet, y, i, VisibilityConstants
						.getDescription(rflor.getVisibility()));
				addCell(sheet, y, 0, decorator.getShortName()
						+ ImportExportUtils.LABELCAPTION_VISIBILITY_SUFFIX);
			} else if (RestrictedFieldFile.class.isAssignableFrom(method
					.getReturnType())) {
				RestrictedFieldFile rflor = (RestrictedFieldFile) field;
				y = y + 1;
				addCell(sheet, y, 0, decorator.getShortName());
				if (StringUtils.isNotEmpty(rflor.getValue())) {
					addCell(sheet, y, i, rflor.getMimeType()
							+ STOPFIELDS_EXCEL + rflor.getValue());
				}
				y = y + 1;
				addCell(sheet, y, 0, decorator.getShortName()
						+ ImportExportUtils.LABELCAPTION_VISIBILITY_SUFFIX);
				if (StringUtils.isNotEmpty(rflor.getValue())) {
					addCell(sheet, y, i, VisibilityConstants
							.getDescription(rflor.getVisibility()));
				}
			} else {
				RestrictedField rr = (RestrictedField) field;
				y = y + 1;
				addCell(sheet, y, i, rr.getValue());
				addCell(sheet, y, 0, decorator.getShortName());
				y = y + 1;
				addCell(sheet, y, i, VisibilityConstants
						.getDescription(rr.getVisibility()));
				addCell(sheet, y, 0, decorator.getShortName()
						+ ImportExportUtils.LABELCAPTION_VISIBILITY_SUFFIX);
			}
		}
		return y;
	}

    public static int createCell(ApplicationService applicationService, int yy,
            int ii, IContainable containable,
            ACrisNestedObject rp, HSSFSheet sheetNested) throws IOException
    {
        String field_value = "";
        boolean first = true;
        
        
        for (Object rrr : rp.getAnagrafica4view().get(containable.getShortName())) {

            ANestedProperty rr = (ANestedProperty)rrr;
            PropertyEditor pe = rr.getTypo().getRendering()
                    .getImportPropertyEditor(applicationService, AdvancedPropertyEditorSupport.MODE_CSV);
            pe.setValue(rr.getObject());
            if (!first) {
                field_value += STOPFIELDS_EXCEL;
            }
            String tmp = pe.getAsText();
            String field_visibility = VisibilityConstants.getDescription(rr
                    .getVisibility());
            if(tmp.startsWith("[")) {
                field_value += "[visibility=" +field_visibility+" " + tmp.substring(1, tmp.length());
            } else {
                field_value += "[visibility=" +field_visibility+"]" + tmp;
            }
            first = false;

        }
        yy = yy + 1;
        addCell(sheetNested, yy, ii, field_value);
        addCell(sheetNested, yy, 0, containable.getShortName());
        
        return yy;
    }
    
    /***
     * Add a cell to the worksheet. A row is created if necessary.
     * 
     * @param sheet The worksheet
     * @param colIndex The column of the new cell
     * @param rowIndex The row of the new cell
     * @param value The string value assigned to the cell
     * @return The cell
     */
    public static Cell addCell(HSSFSheet sheet, int colIndex, int rowIndex, String value) {
    	Row row = sheet.getRow(rowIndex);
    	if (row == null)
    		row = sheet.createRow(rowIndex);
    	
    	Cell cell = row.createCell(colIndex);
        cell.setCellValue(value);
        
        return cell;
    }
    
    /***
     * Add a cell to the worksheet. A row is created if necessary.
     * 
     * @param sheet The worksheet
     * @param colIndex The column of the new cell
     * @param rowIndex The row of the new cell
     * @param value The string value assigned to the cell
     * @param style The cell style
     * @return The cell
     */
    public static Cell addCell(HSSFSheet sheet, int colIndex, int rowIndex, String value, CellStyle style) {
    	Cell cell = addCell(sheet, colIndex, rowIndex, value);
        cell.setCellStyle(style);
        
        return cell;
    }
    
    /***
     * Convert cell value to String.
     * 
     * It is used for instance, to avoid error when calling getStringCellValue and cell hold numeric value.
     * Exception rised:
     * 	 java.lang.IllegalStateException: Cannot get a text value from a numeric cell poi
     * 
     * @param cell
     * @return
     */
    public static String stringCellValue(Cell cell) {
    	DataFormatter f = new DataFormatter();
    	
    	return f.formatCellValue(cell);
    }
}