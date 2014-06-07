/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller;

import it.cilea.osd.common.controller.BaseFormController;

import java.io.File;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.jdyna.RPPropertiesDefinition;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.util.ImportExportUtils;
import org.dspace.app.webui.cris.dto.ImportDTO;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.springframework.validation.BindException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

/**
 * This SpringMVC controller is responsible to handle the import by webui
 * 
 * @author cilea
 * 
 */
public class NewImportFormController extends BaseFormController {

	private ApplicationService applicationService;

	public void setApplicationService(ApplicationService applicationService) {
		this.applicationService = applicationService;
	}

	@Override
	protected ModelAndView onSubmit(HttpServletRequest request,
			HttpServletResponse response, Object command, BindException errors)
			throws Exception {
		Context dspaceContext = UIUtil.obtainContext(request);

		ImportDTO object = (ImportDTO) command;
		MultipartFile fileDTO = object.getFile();

		// read folder from configuration and make dir
		String path = ConfigurationManager
				.getProperty(CrisConstants.CFG_MODULE,"researcherpage.file.import.path");
		File dir = new File(path);
		dir.mkdir();
		try {
			if (object.getModeXSD() != null) {
				response.setContentType("application/xml;charset=UTF-8");
				response.addHeader("Content-Disposition",
						"attachment; filename=rp.xsd");
				String nameXSD = "xsd-download-webuirequest.xsd";
				File filexsd = new File(dir, nameXSD);
				filexsd.createNewFile();
				ImportExportUtils.newGenerateXSD(response.getWriter(), dir, applicationService
						.findAllContainables(RPPropertiesDefinition.class), filexsd, new String[]{"crisobjects","crisobject"}, "rp:", "http://www.cilea.it/researcherpage/schemas", "http://www.cilea.it/researcherpage/schemas", new String[]{"publicID","uuid","businessID","type"}, new boolean[]{false,false,true,true});
				response.getWriter().flush();
				response.getWriter().close();
				return null;
			} else {
				if (fileDTO != null && !fileDTO.getOriginalFilename().isEmpty()) {
					Boolean defaultStatus = ConfigurationManager
							.getBooleanProperty(CrisConstants.CFG_MODULE,"researcherpage.file.import.rpdefaultstatus");
					if (AuthorizeManager.isAdmin(dspaceContext)) {
						dspaceContext.turnOffAuthorisationSystem();
					}
					ImportExportUtils.importResearchersXML(fileDTO.getInputStream(), dir,
							applicationService, dspaceContext, defaultStatus);
					saveMessage(
							request,
							getText("action.import.with.success",
									request.getLocale()));
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			saveMessage(
					request,
					getText("action.import.with.noSuccess", e.getMessage(),
							request.getLocale()));
		}

		return new ModelAndView(getSuccessView());

	}
}
