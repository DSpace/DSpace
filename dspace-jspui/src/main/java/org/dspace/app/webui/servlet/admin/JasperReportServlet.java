/*
 * JasperReportServlet.java
 *
 * Version: $Revision: 1.3 $
 *
 * Date: $Date: 2007-07-09 14:59:25 $
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.app.webui.servlet.admin;

import org.dspace.app.webui.jasper.ItemsDataSource;
import org.dspace.app.webui.jasper.ItemDTO;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.export.JExcelApiExporter;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.export.JRXlsExporterParameter;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

import org.apache.log4j.Logger;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * Servlet for display a report
 * 
 * @author bollini
 * @version $Revision: 1.3 $
 */
public class JasperReportServlet extends DSpaceServlet {
	/** Logger */
	private static Logger log = Logger.getLogger(JasperReportServlet.class);

	//private static String reportName = ConfigurationManager.getProperty("dspace.dir")+"/config/report.jasper";
	private static String reportName = ConfigurationManager.getProperty("dspace.dir")+"/config/report.jrxml";
	
	protected void doDSGet(Context context, HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException,
			SQLException, AuthorizeException {
		context.abort();
		JSPManager.showJSP(request, response, "/dspace-admin/report.jsp");		
	}

	protected void doDSPost(Context context, HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException,
			SQLException, AuthorizeException {

		TableRowIterator tableRowIterator = DatabaseManager
				.query(context,
						"SELECT * FROM metadatavalue WHERE resource_type_id =2 and resource_id  IN (" +
						"SELECT item_id FROM item WHERE in_archive = true)" + 
						" ORDER BY metadatavalue.resource_id, metadata_field_id, place");
		int currItem = -1;
		
		int currFieldId = -1;
		String currMetadataKey = null;
		Map<String, String> metadata = null;
		List<ItemDTO> items = new ArrayList<ItemDTO>();
		
			
		while (tableRowIterator.hasNext()) {
			TableRow row = tableRowIterator.next();
			// nuovo item, devo avviare la creazione del DTO
			if (currItem != row.getIntColumn("item_id")) {
				//System.out.println("====Nuovo ITEM: " + ++count);
				currItem = row.getIntColumn("item_id");
				currFieldId = row.getIntColumn("metadata_field_id");
				currMetadataKey = getMetadataKey(context, currFieldId);
				if (metadata != null) {
					ItemDTO itemDTO = new ItemDTO(metadata);
					items.add(itemDTO);
				}
				metadata = new HashMap<String, String>();
			}
			// l'item non � cambiato ma se � cambiato il field devo rigenerare
			// la key
			else if (currFieldId != row.getIntColumn("metadata_field_id")) {
				currItem = row.getIntColumn("item_id");
				currFieldId = row.getIntColumn("metadata_field_id");
				currMetadataKey = getMetadataKey(context, currFieldId);
			}

			metadata.put(currMetadataKey + "_" + row.getIntColumn("place"), row
					.getStringColumn("text_value"));
		}
		tableRowIterator.close();
		context.abort();
		response.setContentType("application/octect");
		response.addHeader("Content-Disposition",
				"attachment; filename=report.xls");

		JasperPrint print = null;
		try {
			//print = JasperFillManager.fillReport(new FileInputStream(reportName), new HashMap(), new ItemsDataSource(items));
			
			JasperDesign jasperDesign = JRXmlLoader.load(reportName);
			JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);
			
			print = JasperFillManager.fillReport(jasperReport, new HashMap(), new ItemsDataSource(items));

			ByteArrayOutputStream outputByteArray = new ByteArrayOutputStream();
			// coding For Excel:
			// Vecchia chiamata in conflitto con libreria poi (eliminato metodo setEnconding)
			//JRXlsExporter exporterXLS = new JRXlsExporter();
			
			// Nuova chiamata
			JRExporter exporterXLS = new JExcelApiExporter();

			
			exporterXLS
					.setParameter(JRXlsExporterParameter.JASPER_PRINT, print);
			exporterXLS.setParameter(JRXlsExporterParameter.OUTPUT_STREAM,
					outputByteArray);
			exporterXLS
					.setParameter(JRXlsExporterParameter.IS_ONE_PAGE_PER_SHEET,
							Boolean.FALSE);
			exporterXLS.setParameter(
					JRXlsExporterParameter.IS_DETECT_CELL_TYPE, Boolean.TRUE);
			exporterXLS.setParameter(
					JRXlsExporterParameter.IS_WHITE_PAGE_BACKGROUND,
					Boolean.FALSE);
			exporterXLS.setParameter(
					JRXlsExporterParameter.IS_REMOVE_EMPTY_SPACE_BETWEEN_ROWS,
					Boolean.TRUE);
			
			//response.setHeader("Content-Disposition", "attachment; filename=" + reportName + ".xls");
            //response.setContentType("application/ms-excel");
			
			exporterXLS.exportReport();
			response.getOutputStream().write(outputByteArray.toByteArray());
		} catch(JRException jex) {
			log.info(LogManager.getHeader(context, "export_excel", "error"), jex);
		}

	}

	private String getMetadataKey(Context context, Integer fieldId)
			throws SQLException {
		MetadataField currMetadataField = MetadataField.find(context, fieldId);
		MetadataSchema metadataSchema = MetadataSchema.find(context,
				currMetadataField.getSchemaID());
		return metadataSchema.getName()
				+ "_"
				+ currMetadataField.getElement()
				+ (currMetadataField.getQualifier() != null ? "_"
						+ currMetadataField.getQualifier() : "");
	}
}