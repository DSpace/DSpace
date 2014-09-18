package org.dspace.app.webui.servlet;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.math.NumberUtils;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.folderimport.ImportErrorsReader;
import org.dspace.folderimport.constants.FolderMetadataImportConstants;
import org.dspace.folderimport.dto.ErrorImportRegistry;

/**
 * Serlvet voltado para tratamento de requisições que visam a averiguação de itens que foram importados com erro
 * pela funcioanlidade <b>FolderMetadatImport</b>
 * @author Márcio Ribeiro Gurgel do Amaral (marcio.rga@gmail.com)
 *
 */
public class FolderMetadataErrorServlet extends DSpaceServlet {

	private static final long serialVersionUID = 4066937582345487265L;

	/**
	 * @see DSpaceServlet#doDSGet(Context, HttpServletRequest, HttpServletResponse)
	 */
	@Override
	protected void doDSGet(Context context, HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException,
			SQLException, AuthorizeException {
		
		String itemKey = request.getParameter(FolderMetadataImportConstants.ITEMS_WITH_ERROR_ON_IMPORT_ITEM_KEY);
		String fileKey = request.getParameter(FolderMetadataImportConstants.ITEMS_WITH_ERROR_ON_IMPORT_FILE_KEY);
		
		/** Foi solicitado download de arquivo **/
		if(itemKey != null && fileKey != null && NumberUtils.isNumber(itemKey) && NumberUtils.isNumber(fileKey))
		{
			@SuppressWarnings("unchecked")
			Map<Long, ErrorImportRegistry> errorFiles = (Map<Long, ErrorImportRegistry>) request.getSession().getAttribute(FolderMetadataImportConstants.ITEMS_WITH_ERROR_ON_IMPORT_KEY);
			if(errorFiles != null)
			{
				ErrorImportRegistry errorImportRegistry = errorFiles.get(Long.valueOf(itemKey));
				if(errorImportRegistry != null && errorImportRegistry.getItemFiles() != null)
				{
					doDownload(response, fileKey, errorImportRegistry);
				}
			}
		}
		else
		{
			/** Usuário requisitou abertura da página **/
			@SuppressWarnings("unchecked")
			Map<Long, File> serverMappingRoot = (Map<Long, File>) request.getSession().getAttribute(FolderMetadataImportConstants.SERVER_DATA_READBLE_KEY_ROOT);
			Long selectedRootFolder = (Long) request.getSession().getAttribute(FolderMetadataImportConstants.ID_OF_SELECTED_EXPORT);
			if(serverMappingRoot != null && serverMappingRoot.containsKey(selectedRootFolder))
			{
				/** Map must have only occurrence **/
				Map<Long, ErrorImportRegistry> listErrorsImport = ImportErrorsReader.listErrorsImport(serverMappingRoot.get(selectedRootFolder));
				
				if(listErrorsImport == null || listErrorsImport.isEmpty())
				{
					request.setAttribute("message", FolderMetadataImportConstants.NO_ITEMS_WITH_ERROR_ON_IMPORT_FOUND_KEY);
				}
				else
				{
					request.getSession().setAttribute(FolderMetadataImportConstants.ITEMS_WITH_ERROR_ON_IMPORT_KEY, listErrorsImport);
					Collection<ErrorImportRegistry> valuesCollection = listErrorsImport.values();
					List<ErrorImportRegistry> valuesList = new ArrayList<ErrorImportRegistry>(valuesCollection);
					
					Collections.sort(valuesList, new Comparator<ErrorImportRegistry>() {

						@Override
						public int compare(ErrorImportRegistry o1,
								ErrorImportRegistry o2) {
							
							if(o1.getTitle() != null && o2.getTitle() != null)
							{
								return o1.getTitle().compareTo(o2.getTitle());
							}
							return 0;
						}
					});
					
					request.setAttribute(FolderMetadataImportConstants.ITEMS_WITH_ERROR_ON_IMPORT_KEY, valuesList);
				}
				
				JSPManager.showJSP(request, response, "/dspace-admin/foldermetadataerror.jsp");
			}
			else
			{
				request.setAttribute("message", FolderMetadataImportConstants.NO_ITEMS_WITH_ERROR_ON_IMPORT_FOUND_KEY);
				JSPManager.showJSP(request, response, "/dspace-admin/foldermetadataerror.jsp");
			}
		}
				
	}

	/**
	 * Efetua rotina para permitir download do item selecionado pelo usuário
	 * @param response Resposta HTTP
	 * @param fileKey Chave do item a ser recuperado
	 * @param errorImportRegistry Registro de arquivos do item selecionado
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	private void doDownload(HttpServletResponse response, String fileKey, ErrorImportRegistry errorImportRegistry) throws IOException,
			FileNotFoundException {
		
		File requestedFile = errorImportRegistry.getItemFiles().get(Long.valueOf(fileKey));
		ServletOutputStream servletOutputStream = response.getOutputStream();
		ServletContext servletContext  = getServletConfig().getServletContext();
		String mimetype = servletContext.getMimeType(requestedFile.getCanonicalPath());
		
		if (mimetype == null) {
		    mimetype = "application/octet-stream";
		}
		response.setContentType(mimetype);
		response.setContentLength((int)requestedFile.length());
		String fileName = requestedFile.getName();
		
		/** Força download do item **/
		response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
		
		byte[] byteBuffer = new byte[4096];
		int lengthReader = 0;
		DataInputStream dataInputStream = new DataInputStream(new FileInputStream(requestedFile));
		
		while ((dataInputStream != null) && ((lengthReader = dataInputStream.read(byteBuffer)) != -1))
		{
		    servletOutputStream.write(byteBuffer,0,lengthReader);
		}
		
		dataInputStream.close();
		servletOutputStream.close();
	}
	

}
