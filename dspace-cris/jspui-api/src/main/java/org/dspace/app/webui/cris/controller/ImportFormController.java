/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.cris.importexport.IBulkChangesService;
import org.dspace.app.cris.importexport.XMLBulkChangesService;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.export.ExportConstants;
import org.dspace.app.cris.model.jdyna.ACrisNestedObject;
import org.dspace.app.cris.model.jdyna.DynamicObjectType;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.util.ImportExportUtils;
import org.dspace.app.cris.util.UtilsXSD;
import org.dspace.app.webui.cris.dto.ImportDTO;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.utils.DSpace;
import org.springframework.validation.BindException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import it.cilea.osd.common.controller.BaseFormController;
import it.cilea.osd.jdyna.model.ANestedPropertiesDefinition;
import it.cilea.osd.jdyna.model.ANestedProperty;
import it.cilea.osd.jdyna.model.ATypeNestedObject;
import it.cilea.osd.jdyna.model.IContainable;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.model.Property;

/**
 * This SpringMVC controller is responsible to handle the import by webui
 * 
 * @author cilea
 * 
 */
public class ImportFormController  <ACO extends ACrisObject<P, TP, NP, NTP, ACNO, ATNO>, P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> extends BaseFormController {

	private ApplicationService applicationService;

	public void setApplicationService(ApplicationService applicationService) {
		this.applicationService = applicationService;
	}

	@Override
	protected Map referenceData(HttpServletRequest request) throws Exception
	{
        Map<String, Object> map = new HashMap<String, Object>(); 
	    map.put("dynamicobjects", applicationService.getList(DynamicObjectType.class));
	    return map;
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
				.getProperty(CrisConstants.CFG_MODULE,"file.import.path");
		File dir = new File(path);
		dir.mkdir();
		try {
		    ACrisObject<P, TP, NP, NTP, ACNO, ATNO> obj = null; 
		    if(object.getTargetEntity()!=null) {
		        obj = (ACrisObject<P, TP, NP, NTP, ACNO, ATNO>)(Class.forName(object.getTargetEntity()).newInstance());
		    } else {
		        obj = (ACrisObject<P, TP, NP, NTP, ACNO, ATNO>)(new ResearcherPage());
		    }		    
		    
			Class<TP> clazz = obj.getClassPropertiesDefinition();
			Class<ACO> objectClass = (Class<ACO>)obj.getClass();
			if (object.isTemplate()) {
				response.setContentType("application/xml;charset=UTF-8");
				response.addHeader("Content-Disposition",
						"attachment; filename="+object.getTargetEntity()+".xsd");
				String nameXSD = "xsd-download-webuirequest.xsd";
				File filexsd = new File(dir, nameXSD);
				filexsd.createNewFile();
				
				String[] namespace = UtilsXSD.getNamespace(clazz);
				
		        List<ATNO> ttps = applicationService.getList(obj.getClassTypeNested());
		        List<IContainable> metadataNestedLevel = new LinkedList<IContainable>();
		        for (ATNO ttp : ttps) {
		            IContainable ic = applicationService.findContainableByDecorable(ttp.getDecoratorClass(), ttp.getId());
		            if (ic != null) {
		                metadataNestedLevel.add(ic);
		            }
		        }

	        	DSpace dspace = new DSpace();
	            IBulkChangesService importer = dspace.getServiceManager().getServiceByName(XMLBulkChangesService.SERVICE_NAME, IBulkChangesService.class);
				importer.generateTemplate(response.getWriter(), dir, applicationService
						.findAllContainables(clazz), metadataNestedLevel, filexsd, 
						UtilsXSD.getElementRoot(clazz),
						namespace[0]+":",
                        namespace[1],
                        namespace[1], 
						new String[] {
		                        ExportConstants.NAME_PUBLICID_ATTRIBUTE,
		                        ExportConstants.NAME_BUSINESSID_ATTRIBUTE,
		                        ExportConstants.NAME_ID_ATTRIBUTE,
		                        ExportConstants.NAME_TYPE_ATTRIBUTE },
						new boolean[]{false,false,true,true});
		                        
				response.getWriter().flush();
				response.getWriter().close();
				return null;
			} else {
				if (fileDTO != null && !fileDTO.getOriginalFilename().isEmpty()) {
					Boolean defaultStatus = ConfigurationManager
							.getBooleanProperty(CrisConstants.CFG_MODULE,"file.import.defaultstatus."+object.getTargetEntity());
					if (AuthorizeManager.isAdmin(dspaceContext)) {
						dspaceContext.turnOffAuthorisationSystem();
					}
					ACO cris = (ACO)obj.getClass().newInstance();
					if (cris instanceof ResearchObject) {
						((ResearchObject) cris).setTypo(applicationService.findTypoByShortName(DynamicObjectType.class, object.getType()));						
					}
					ImportExportUtils.process(object.getFormat(), fileDTO.getInputStream(), dir,
							applicationService, dspaceContext, defaultStatus, clazz, objectClass, cris, cris.getClassNested(), cris.getClassTypeNested(), cris.getClassNested().newInstance().getClassPropertiesDefinition());
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
