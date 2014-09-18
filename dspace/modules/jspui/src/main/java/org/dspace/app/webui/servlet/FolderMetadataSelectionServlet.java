/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.folderimport.FolderReader;
import org.dspace.folderimport.constants.FolderMetadataImportConstants;
import org.dspace.folderimport.dto.FolderAnalyseResult;



/**
 * Servlet voltada para o tratamento de requisições iniciais a funcionalidade de importação de itens via diretórios. <br>
 * Em casos onde exista somente um diretório de exportação, a servlet irá atuar como <i>bypass</i>, redirecionando para a próxima
 * servlet ({@link FolderMetadataImportServlet#doDSGet(Context, HttpServletRequest, HttpServletResponse)})
 * @author Márcio Ribeiro Gurgel do Amaral
 *
 */
public class FolderMetadataSelectionServlet extends DSpaceServlet
{

	private static final String DSPACE_ADMIN_FOLDERMETADATASELECTION_JSP = "/dspace-admin/foldermetadataselection.jsp";
	private static final int BASE_NUMBER_MORE_THAN_ONE_EXPORT = 1;
	private static final long serialVersionUID = 1L;

	/**
	 * @see DSpaceServlet#doDSGet(Context, HttpServletRequest, HttpServletResponse)
	 */
	@Override
	protected void doDSGet(Context context, HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException,
			SQLException, AuthorizeException {
		
		HttpSession session = request.getSession();
		
		clearSessionVariables(session);

		File root = new File(ConfigurationManager.getProperty("org.dspace.app.itemexport.work.dir"));
		FolderReader folderReader = new FolderReader(root);
		
		FolderAnalyseResult listAvailableExport = folderReader.listAvailableExport(UIUtil.getSessionLocale(request), ConfigurationManager.getProperty("foldermetadataimport.directory.success"));
		
		/** Certificação da existência de diretórios e importação **/
		if(listAvailableExport != null && listAvailableExport.getUserReadble() != null && !listAvailableExport.getUserReadble().isEmpty())
		{
			session.setAttribute(FolderMetadataImportConstants.USER_DATA_READBLE_KEY_ROOT, listAvailableExport.getUserReadble());
			session.setAttribute(FolderMetadataImportConstants.SERVER_DATA_READBLE_KEY_ROOT, listAvailableExport.getServerReadble());
			
			/** Existe mais de um diretório para importação, solicita ao usuário que informe qual diretório será importado **/
			if(listAvailableExport.getUserReadble().size() > BASE_NUMBER_MORE_THAN_ONE_EXPORT)
			{
		    	JSPManager.showJSP(request, response, DSPACE_ADMIN_FOLDERMETADATASELECTION_JSP);
			}
			else
			{
				/** Em caso de existência de somente um diretório de exportação, prossegue para a próxima página **/
				response.sendRedirect(response.encodeRedirectURL(request
	                    .getContextPath() + "/dspace-admin/foldermetadataimport?submit_selection&selectedFolder=" + getRootFolderId(listAvailableExport)));
			}
		}
		else
		{
			request.setAttribute("has-error", Boolean.TRUE);
			request.setAttribute("message", FolderMetadataImportConstants.KEY_MESSAGE_NO_FOLDER_EXISTS);
			JSPManager.showJSP(request, response, DSPACE_ADMIN_FOLDERMETADATASELECTION_JSP);
		}
		
	}

	/**
	 * Clear all data that can be filled.
	 * @param session Http session
	 */
	private void clearSessionVariables(HttpSession session) {
		
		String[] attributesToRemove = new String[]{FolderMetadataImportConstants.USER_DATA_READBLE_KEY, 
				FolderMetadataImportConstants.SERVER_DATA_READBLE, FolderMetadataImportConstants.PARENT_FOLDER_MAPPPING,
				FolderMetadataImportConstants.USER_DATA_READBLE_KEY_ROOT, 
				FolderMetadataImportConstants.SERVER_DATA_READBLE_KEY_ROOT,
				FolderMetadataImportConstants.ID_OF_SELECTED_EXPORT, FolderMetadataImportConstants.ITEMS_WITH_ERROR_ON_IMPORT_KEY};
		
		for(String attribute : attributesToRemove)
		{
			session.removeAttribute(attribute);
		}
		
	}

	/**
	 * Método a ser acionado em situações onde exista somente <b>uma</b> ocorrência de diretório de exportação.
	 * @param listAvailableExport Mapa de diretórios disponíveis o qual deverá possuir somente uma ocorrência de registro de diretório
	 * @return Identificador dado ao diretório de exportação
	 */
	private Long getRootFolderId(FolderAnalyseResult listAvailableExport) {
		
		for(Long rootFolderId : listAvailableExport.getServerReadble().keySet())
		{
			return rootFolderId;
		}
		
		return null;
	}

	/**
	 * @see DSpaceServlet#doDSPost(Context, HttpServletRequest, HttpServletResponse)
	 */
	@Override
	protected void doDSPost(Context context, HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException,
			SQLException, AuthorizeException {
		
		super.doDSPost(context, request, response);
	}


	
	
}

	

