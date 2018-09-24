/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller.jdyna;

import it.cilea.osd.jdyna.dto.AnagraficaObjectAreaDTO;
import it.cilea.osd.jdyna.model.AnagraficaObject;
import it.cilea.osd.jdyna.model.IContainable;
import it.cilea.osd.jdyna.util.AnagraficaUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.OrganizationUnit;
import org.dspace.app.cris.model.dto.OUAnagraficaObjectDTO;
import org.dspace.app.cris.model.jdyna.BoxOrganizationUnit;
import org.dspace.app.cris.model.jdyna.EditTabOrganizationUnit;
import org.dspace.app.cris.model.jdyna.OUAdditionalFieldStorage;
import org.dspace.app.cris.model.jdyna.OUNestedObject;
import org.dspace.app.cris.model.jdyna.OUNestedPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.OUNestedProperty;
import org.dspace.app.cris.model.jdyna.OUPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.OUProperty;
import org.dspace.app.cris.model.jdyna.TabOrganizationUnit;
import org.dspace.app.cris.model.jdyna.VisibilityTabConstant;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.app.webui.cris.util.CrisAuthorizeManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

public class FormOUDynamicMetadataController
        extends
        AFormDynamicOUController<OUProperty, OUPropertiesDefinition, BoxOrganizationUnit, EditTabOrganizationUnit, AnagraficaObject<OUProperty, OUPropertiesDefinition>, OUNestedObject, OUNestedProperty, OUNestedPropertiesDefinition>
{

    @Override
    protected Map referenceData(HttpServletRequest request, Object command,
            Errors errors) throws Exception
    {

        // call super method
        Map<String, Object> map = super.referenceData(request);

        // this map contains key-values pairs, key = box shortname and values =
        // collection of metadata
        Map<String, List<IContainable>> mapBoxToContainables = new HashMap<String, List<IContainable>>();

        AnagraficaObjectAreaDTO anagraficaObjectDTO = (AnagraficaObjectAreaDTO) command;

        // check admin authorization
        boolean isAdmin = false;
        if(map.containsKey("isAdmin")) {
            isAdmin = (Boolean)map.get("isAdmin");
        }
        // collection of edit tabs (all edit tabs created on system associate to
        // visibility)
		Integer entityId = Integer.parseInt(request.getParameter("id"));

		if (entityId == null) {
			return null;
		}
		Context context = UIUtil.obtainContext(request);

		List<EditTabOrganizationUnit> tabs = getApplicationService().getList(EditTabOrganizationUnit.class);
		List<EditTabOrganizationUnit> authorizedTabs = new LinkedList<EditTabOrganizationUnit>();

		for (EditTabOrganizationUnit tab : tabs) {
			if (CrisAuthorizeManager.authorize(context, getApplicationService(), OrganizationUnit.class,
					OUPropertiesDefinition.class, entityId, tab)) {
				authorizedTabs.add(tab);
			}
		}

        // check if request tab from view is active (check on collection before)
        EditTabOrganizationUnit editT = getApplicationService().get(
                EditTabOrganizationUnit.class, anagraficaObjectDTO.getTabId());
		if (!authorizedTabs.contains(editT)) {
			if (authorizedTabs.size() > 0) {
				editT = authorizedTabs.get(0);
				anagraficaObjectDTO.setTabId(editT.getId());
			} else {

				throw new AuthorizeException("You not have needed authorization level to display this tab");
			}
		}

        // collection of boxs
        List<BoxOrganizationUnit> propertyHolders = new LinkedList<BoxOrganizationUnit>();

        // if edit tab got a display tab (edit tab is hookup to display tab)
        // then edit box will be created from display box otherwise get all boxs
        // in edit tab
        if (editT.getDisplayTab() != null)
        {
            for (BoxOrganizationUnit box : editT.getDisplayTab().getMask())
            {
                propertyHolders.add(box);
            }
        }
        else
        {
            propertyHolders = getApplicationService().findPropertyHolderInTab(
                    getClazzTab(), anagraficaObjectDTO.getTabId());
        }

        // clean boxs list with accesslevel
        List<BoxOrganizationUnit> propertyHoldersCurrentAccessLevel = new LinkedList<BoxOrganizationUnit>();
        for (BoxOrganizationUnit propertyHolder : propertyHolders)
        {
            if (isAdmin)
            {
                if (!propertyHolder.getVisibility().equals(
                        VisibilityTabConstant.LOW))
                {
                    propertyHoldersCurrentAccessLevel.add(propertyHolder);
                }
            }
            else
            {
                if (!propertyHolder.getVisibility().equals(
                        VisibilityTabConstant.ADMIN))
                {
                    propertyHoldersCurrentAccessLevel.add(propertyHolder);
                }
            }
        }
        Collections.sort(propertyHoldersCurrentAccessLevel);
        // this piece of code get containables object from boxs and put them on
        // map
        List<IContainable> pDInTab = new LinkedList<IContainable>();
        for (BoxOrganizationUnit iph : propertyHoldersCurrentAccessLevel)
        {
            List<IContainable> temp = getApplicationService()
                    .<BoxOrganizationUnit, it.cilea.osd.jdyna.web.Tab<BoxOrganizationUnit>, OUPropertiesDefinition> findContainableInPropertyHolder(
                            getClazzBox(), iph.getId());
            mapBoxToContainables.put(iph.getShortName(), temp);
            pDInTab.addAll(temp);
        }

        map.put("propertiesHolders", propertyHoldersCurrentAccessLevel);
        map.put("propertiesDefinitionsInTab", pDInTab);
        map.put("propertiesDefinitionsInHolder", mapBoxToContainables);
        map.put("tabList", authorizedTabs);
        map.put("simpleNameAnagraficaObject", getClazzAnagraficaObject()
                .getSimpleName());
        map.put("addModeType", "edit");
        return map;
    }

    @Override
    protected Object formBackingObject(HttpServletRequest request)
            throws Exception
    {
        String paramFuzzyTabId = request.getParameter("hooktabId");
        String paramTabId = request.getParameter("tabId");
        String paramId = request.getParameter("id");

        Integer id = null;
        if (paramId != null)
        {
            id = Integer.parseInt(paramId);
        }
        OrganizationUnit grant = getApplicationService().get(OrganizationUnit.class, id);
        Context context = UIUtil.obtainContext(request);
        boolean canEdit = false;
        if (CrisAuthorizeManager.canEdit(context, getApplicationService(), EditTabOrganizationUnit.class, grant))
        {
            canEdit = true;
        }
        else {
            throw new AuthorizeException("Only system admin can edit");
        }

        Integer areaId = null;
        if (paramTabId == null)
        {
            if (paramFuzzyTabId != null)
            {
                EditTabOrganizationUnit fuzzyEditTab = (EditTabOrganizationUnit) ((ApplicationService) getApplicationService())
                        .<BoxOrganizationUnit, TabOrganizationUnit, EditTabOrganizationUnit, OUPropertiesDefinition>getEditTabByDisplayTab(
                                Integer.parseInt(paramFuzzyTabId),
                                EditTabOrganizationUnit.class);
                areaId = fuzzyEditTab.getId();
            }
        }
        else
        {
            areaId = Integer.parseInt(paramTabId);
        }

        EditTabOrganizationUnit editT = null;
        if (areaId != null) { 
        	editT = getApplicationService().get(
                EditTabOrganizationUnit.class, areaId);
        }
        
        List<EditTabOrganizationUnit> tabs = getApplicationService().getList(EditTabOrganizationUnit.class);
        List<EditTabOrganizationUnit> authorizedTabs = new LinkedList<EditTabOrganizationUnit>();
        
        for(EditTabOrganizationUnit tab : tabs) {
            if(CrisAuthorizeManager.authorize(context, getApplicationService(), OrganizationUnit.class, OUPropertiesDefinition.class, id, tab)) {
                authorizedTabs.add(tab);
            }
        }
        if (!authorizedTabs.contains(editT))
        {
               if (authorizedTabs.size() > 0) {
                       editT = authorizedTabs.get(0);
                       areaId = editT.getId();
               }
               else {
                   throw new AuthorizeException(
                           "You not have needed authorization level to display this tab");
               }
        }
        
        List<BoxOrganizationUnit> propertyHolders = new LinkedList<BoxOrganizationUnit>();
        if (editT.getDisplayTab() != null)
        {
            for (BoxOrganizationUnit box : editT.getDisplayTab().getMask())
            {
                propertyHolders.add(box);
            }
        }
        else
        {
            propertyHolders = getApplicationService().findPropertyHolderInTab(
                    getClazzTab(), areaId);
        }

        List<IContainable> tipProprietaInArea = new LinkedList<IContainable>();

        for (BoxOrganizationUnit iph : propertyHolders)
        {
            if (editT.getDisplayTab() != null)
            {
                tipProprietaInArea
                        .addAll(getApplicationService()
                                .<BoxOrganizationUnit, it.cilea.osd.jdyna.web.Tab<BoxOrganizationUnit>, OUPropertiesDefinition> findContainableInPropertyHolder(
                                        BoxOrganizationUnit.class, iph.getId()));
            }
            else
            {
                tipProprietaInArea
                        .addAll(getApplicationService()
                                .<BoxOrganizationUnit, it.cilea.osd.jdyna.web.Tab<BoxOrganizationUnit>, OUPropertiesDefinition> findContainableInPropertyHolder(
                                        getClazzBox(), iph.getId()));
            }
        }
        OUAdditionalFieldStorage dynamicObject = grant.getDynamicField();
        OUAnagraficaObjectDTO anagraficaObjectDTO = new OUAnagraficaObjectDTO(
                grant);
        anagraficaObjectDTO.setTabId(areaId);
        anagraficaObjectDTO.setObjectId(grant.getId());
        anagraficaObjectDTO.setParentId(grant.getId());
    

        List<OUPropertiesDefinition> realTPS = new LinkedList<OUPropertiesDefinition>();
        List<IContainable> structuralField = new LinkedList<IContainable>();
        for (IContainable c : tipProprietaInArea)
        {

            OUPropertiesDefinition rpPd = getApplicationService()
                    .findPropertiesDefinitionByShortName(
                            OUPropertiesDefinition.class, c.getShortName());
            if (rpPd != null)
            {
                realTPS.add(rpPd);
            }
            else
            {
                structuralField.add(c);
            }
        }
        AnagraficaUtils.fillDTO(anagraficaObjectDTO, dynamicObject, realTPS);
        return anagraficaObjectDTO;
    }

    @Override
    protected ModelAndView onSubmit(HttpServletRequest request,
            HttpServletResponse response, Object object, BindException errors)
            throws Exception
    {
        OUAnagraficaObjectDTO anagraficaObjectDTO = (OUAnagraficaObjectDTO) object;

        String exitPage = "redirect:/cris/tools/ou/editDynamicData.htm?id="
                + anagraficaObjectDTO.getParentId();

        
        EditTabOrganizationUnit editT = getApplicationService().get(
                EditTabOrganizationUnit.class, anagraficaObjectDTO.getTabId());
        OrganizationUnit grant = getApplicationService().get(OrganizationUnit.class,
                anagraficaObjectDTO.getParentId());
        if (anagraficaObjectDTO.getNewTabId() != null)
        {
            exitPage += "&tabId=" + anagraficaObjectDTO.getNewTabId();
        }
        else
        {
            exitPage = "redirect:/cris/ou/"
                    + grant.getCrisID();
        }
        if (request.getParameter("cancel") != null)
        {
            return new ModelAndView(exitPage);
        }
        
        OUAdditionalFieldStorage myObject = grant.getDynamicField();
        
        List<BoxOrganizationUnit> propertyHolders = new LinkedList<BoxOrganizationUnit>();
        if (editT.getDisplayTab() != null)
        {
            for (BoxOrganizationUnit box : editT.getDisplayTab().getMask())
            {
                propertyHolders.add(box);
            }
        }
        else
        {
            propertyHolders = getApplicationService().findPropertyHolderInTab(
                    getClazzTab(), anagraficaObjectDTO.getTabId());
        }

        List<IContainable> tipProprietaInArea = new LinkedList<IContainable>();

        for (BoxOrganizationUnit iph : propertyHolders)
        {

            tipProprietaInArea
                    .addAll(getApplicationService()
                            .<BoxOrganizationUnit, it.cilea.osd.jdyna.web.Tab<BoxOrganizationUnit>, OUPropertiesDefinition> findContainableInPropertyHolder(
                                    getClazzBox(), iph.getId()));

        }

        List<OUPropertiesDefinition> realTPS = new LinkedList<OUPropertiesDefinition>();
        List<IContainable> structuralField = new LinkedList<IContainable>();
        for (IContainable c : tipProprietaInArea)
        {
            OUPropertiesDefinition rpPd = getApplicationService()
                    .findPropertiesDefinitionByShortName(
                            OUPropertiesDefinition.class, c.getShortName());
            if (rpPd != null)
            {
                realTPS.add(rpPd);
            }
            else
            {
                structuralField.add(c);
            }
        }

        AnagraficaUtils.reverseDTO(anagraficaObjectDTO, myObject, realTPS);
        
        myObject.pulisciAnagrafica();
        grant.setSourceID(anagraficaObjectDTO.getSourceID());
        String sourceref = StringUtils.isNotBlank(anagraficaObjectDTO.getSourceRef()) ? anagraficaObjectDTO.getSourceRef() : ConfigurationManager.getProperty(CrisConstants.CFG_MODULE, "sourceref.default");
        grant.setSourceRef(sourceref); 
        
        grant.setStatus(anagraficaObjectDTO.getStatus());
        
        getApplicationService().saveOrUpdate(OrganizationUnit.class, grant);
        EditTabOrganizationUnit area = getApplicationService().get(getClazzTab(),
                anagraficaObjectDTO.getTabId());
        final String areaTitle = area.getTitle();
        saveMessage(
                request,
                getText("action.anagrafica.edited", new Object[] { areaTitle },
                        request.getLocale()));

        return new ModelAndView(exitPage);
    }

    @Override
    protected void onBindAndValidate(HttpServletRequest request,
            Object command, BindException errors) throws Exception
    {

        AnagraficaObjectAreaDTO dto = (AnagraficaObjectAreaDTO) command;
        OrganizationUnit researcher = getApplicationService().get(OrganizationUnit.class,
                dto.getParentId());
        OUAdditionalFieldStorage myObject = researcher.getDynamicField();

        EditTabOrganizationUnit editT = getApplicationService().get(
                EditTabOrganizationUnit.class, dto.getTabId());
        List<BoxOrganizationUnit> propertyHolders = new LinkedList<BoxOrganizationUnit>();
        if (editT.getDisplayTab() != null)
        {
            for (BoxOrganizationUnit box : editT.getDisplayTab().getMask())
            {
                propertyHolders.add(box);
            }
        }
        else
        {
            propertyHolders = getApplicationService().findPropertyHolderInTab(
                    getClazzTab(), dto.getTabId());
        }

        List<IContainable> tipProprietaInArea = new LinkedList<IContainable>();

        for (BoxOrganizationUnit iph : propertyHolders)
        {

            tipProprietaInArea
                    .addAll(getApplicationService()
                            .<BoxOrganizationUnit, it.cilea.osd.jdyna.web.Tab<BoxOrganizationUnit>, OUPropertiesDefinition> findContainableInPropertyHolder(
                                    getClazzBox(), iph.getId()));

        }

        List<OUPropertiesDefinition> realTPS = new LinkedList<OUPropertiesDefinition>();
        List<IContainable> structuralField = new LinkedList<IContainable>();
        for (IContainable c : tipProprietaInArea)
        {
            OUPropertiesDefinition rpPd = getApplicationService()
                    .findPropertiesDefinitionByShortName(
                            OUPropertiesDefinition.class, c.getShortName());
            if (rpPd != null)
            {
                realTPS.add(rpPd);
            }
            else
            {
                structuralField.add(c);
            }
        }
        AnagraficaUtils.reverseDTO(dto, myObject, realTPS);
        AnagraficaUtils.fillDTO(dto, myObject, realTPS);
    }

}
