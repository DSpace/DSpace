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

import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

import org.apache.commons.lang.StringUtils;
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
			ACrisObject researcher, WritableSheet sheet) throws IOException,
			RowsExceededException, WriteException {
		return createElement(applicationService, y, i, decorator.getReal(),
				decorator.getRendering(), researcher, sheet);
	}

	private static int createElement(ApplicationService applicationService,
			int y, int i, Object preal, AWidget rendering,
			ACrisObject researcher, WritableSheet sheet) throws IOException,
			RowsExceededException, WriteException {
		
	        PropertiesDefinition real = (PropertiesDefinition)preal;
			return createSimpleElement(applicationService, y, i,
					real.getShortName(), researcher
							.getProprietaDellaTipologia(real), sheet);
		
	}

	
	private static int createSimpleElement(
			ApplicationService applicationService, int y, int i,
			String shortName, List<Property> proprietaDellaTipologia,
			WritableSheet sheet) throws RowsExceededException, WriteException {
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
		Label label_v = new Label(y, i, field_value);
		sheet.addCell(label_v);
		Label labelCaption = new Label(y, 0, shortName);
		sheet.addCell(labelCaption);
		
		return y;
	}

	public static int createCell(ApplicationService applicationService, int y,
			int i, DecoratorRestrictedField decorator,
			ACrisObject researcher, WritableSheet sheet)
			throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException, RowsExceededException, WriteException {
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
			Label label_v = new Label(y, i, field_value);
			Label labelCaption = new Label(y, 0, decorator.getShortName());
			sheet.addCell(labelCaption);
			y = y + 1;
			Label label_vv = new Label(y, i, field_visibility);
			labelCaption = new Label(y, 0, decorator.getShortName()
					+ ImportExportUtils.LABELCAPTION_VISIBILITY_SUFFIX);
			sheet.addCell(labelCaption);

			sheet.addCell(label_v);
			sheet.addCell(label_vv);

		} else if (method.getReturnType().isAssignableFrom(String.class)) {
			y = y + 1;
			sheet.addCell(new Label(y, i, (String) field));
			Label labelCaption = new Label(y, 0, decorator.getShortName());
			sheet.addCell(labelCaption);
		} else {
			if (RestrictedFieldLocalOrRemoteFile.class.isAssignableFrom(method
					.getReturnType())) {
				RestrictedFieldLocalOrRemoteFile rflor = (RestrictedFieldLocalOrRemoteFile) field;
				y = y + 1;
				if (StringUtils.isNotEmpty(rflor.getRemoteUrl())) {
					sheet.addCell(new Label(y, i, rflor.getRemoteUrl()));
				} else {
					sheet.addCell(new Label(y, i, rflor.getMimeType()
							+ STOPFIELDS_EXCEL + rflor.getValue()));
				}
				Label labelCaption = new Label(y, 0, decorator.getShortName());
				sheet.addCell(labelCaption);
				y = y + 1;
				sheet.addCell(new Label(y, i, VisibilityConstants
						.getDescription(rflor.getVisibility())));
				labelCaption = new Label(y, 0, decorator.getShortName()
						+ ImportExportUtils.LABELCAPTION_VISIBILITY_SUFFIX);
				sheet.addCell(labelCaption);
			} else if (RestrictedFieldFile.class.isAssignableFrom(method
					.getReturnType())) {
				RestrictedFieldFile rflor = (RestrictedFieldFile) field;
				y = y + 1;
				Label labelCaption = new Label(y, 0, decorator.getShortName());
				sheet.addCell(labelCaption);
				if (StringUtils.isNotEmpty(rflor.getValue())) {
					sheet.addCell(new Label(y, i, rflor.getMimeType()
							+ STOPFIELDS_EXCEL + rflor.getValue()));
				}
				y = y + 1;
				labelCaption = new Label(y, 0, decorator.getShortName()
						+ ImportExportUtils.LABELCAPTION_VISIBILITY_SUFFIX);
				sheet.addCell(labelCaption);
				if (StringUtils.isNotEmpty(rflor.getValue())) {
					sheet.addCell(new Label(y, i, VisibilityConstants
							.getDescription(rflor.getVisibility())));
				}
			} else {
				RestrictedField rr = (RestrictedField) field;
				y = y + 1;
				sheet.addCell(new Label(y, i, rr.getValue()));
				Label labelCaption = new Label(y, 0, decorator.getShortName());
				sheet.addCell(labelCaption);
				y = y + 1;
				sheet.addCell(new Label(y, i, VisibilityConstants
						.getDescription(rr.getVisibility())));
				labelCaption = new Label(y, 0, decorator.getShortName()
						+ ImportExportUtils.LABELCAPTION_VISIBILITY_SUFFIX);
				sheet.addCell(labelCaption);
			}
		}
		return y;
	}

    public static int createCell(ApplicationService applicationService, int yy,
            int ii, IContainable containable,
            ACrisNestedObject rp, WritableSheet sheetNested) throws IOException,
        RowsExceededException, WriteException
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
        Label label_v = new Label(yy, ii, field_value);
        sheetNested.addCell(label_v);
        Label labelCaption = new Label(yy, 0, containable.getShortName());
        sheetNested.addCell(labelCaption);
        
        return yy;
    }
}