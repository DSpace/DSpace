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
import it.cilea.osd.jdyna.web.Utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.model.dto.DynamicAnagraficaObjectDTO;
import org.dspace.app.cris.model.jdyna.BoxDynamicObject;
import org.dspace.app.cris.model.jdyna.BoxOrganizationUnit;
import org.dspace.app.cris.model.jdyna.DynamicAdditionalFieldStorage;
import org.dspace.app.cris.model.jdyna.DynamicNestedObject;
import org.dspace.app.cris.model.jdyna.DynamicNestedPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.DynamicNestedProperty;
import org.dspace.app.cris.model.jdyna.DynamicObjectType;
import org.dspace.app.cris.model.jdyna.DynamicPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.DynamicProperty;
import org.dspace.app.cris.model.jdyna.EditTabDynamicObject;
import org.dspace.app.cris.model.jdyna.EditTabOrganizationUnit;
import org.dspace.app.cris.model.jdyna.TabDynamicObject;
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

public class FormDODynamicMetadataController
        extends
        AFormDynamicObjectController<DynamicProperty, DynamicPropertiesDefinition, BoxDynamicObject, EditTabDynamicObject, AnagraficaObject<DynamicProperty, DynamicPropertiesDefinition>, DynamicNestedObject, DynamicNestedProperty, DynamicNestedPropertiesDefinition>
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
        
        DynamicAnagraficaObjectDTO object = (DynamicAnagraficaObjectDTO)command;        
        // collection of edit tabs (all edit tabs created on system associate to
        // visibility)
        DynamicObjectType typo = getApplicationService().get(DynamicObjectType.class, object.getTipologiaId());
        List<EditTabDynamicObject> tabs = getApplicationService()
                .<BoxDynamicObject, DynamicObjectType, DynamicPropertiesDefinition, TabDynamicObject, EditTabDynamicObject>findEditTabByType(EditTabDynamicObject.class, typo);

        Integer entityId = Integer.parseInt(request.getParameter("id"));
                
		if (entityId == null) {
			return null;
		}
		Context context = UIUtil.obtainContext(request);
		List<EditTabDynamicObject> authorizedTabs = new LinkedList<EditTabDynamicObject>();

		for (EditTabDynamicObject tab : tabs) {
			if (CrisAuthorizeManager.authorize(context, getApplicationService(), ResearchObject.class,
					DynamicPropertiesDefinition.class, entityId, tab)) {
				authorizedTabs.add(tab);
			}
		}
        // check if request tab from view is active (check on collection before)
        EditTabDynamicObject editT = getApplicationService().get(
				EditTabDynamicObject.class, anagraficaObjectDTO.getTabId());
		if (!authorizedTabs.contains(editT)) {
			if (authorizedTabs.size() > 0) {
				editT = authorizedTabs.get(0);
				anagraficaObjectDTO.setTabId(editT.getId());
			} else {
				throw new AuthorizeException("You not have needed authorization level to display this tab");
			}
		}

        // collection of boxs
        List<BoxDynamicObject> propertyHolders = new LinkedList<BoxDynamicObject>();

        // if edit tab got a display tab (edit tab is hookup to display tab)
        // then edit box will be created from display box otherwise get all boxs
        // in edit tab
        if (editT.getDisplayTab() != null)
        {
            for (BoxDynamicObject box : editT.getDisplayTab().getMask())
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
        List<BoxDynamicObject> propertyHoldersCurrentAccessLevel = new LinkedList<BoxDynamicObject>();
        for (BoxDynamicObject propertyHolder : propertyHolders)
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
        for (BoxDynamicObject iph : propertyHoldersCurrentAccessLevel)
        {
            List<IContainable> temp = getApplicationService()
                    .<BoxDynamicObject, it.cilea.osd.jdyna.web.Tab<BoxDynamicObject>, DynamicPropertiesDefinition> findContainableInPropertyHolder(
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
        ResearchObject entity = getApplicationService().get(ResearchObject.class,
                id);
        Context context = UIUtil.obtainContext(request);
        boolean canEdit = false;
        if (CrisAuthorizeManager.canEdit(context, getApplicationService(), EditTabDynamicObject.class, entity))
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
                EditTabDynamicObject fuzzyEditTab = (EditTabDynamicObject) ((ApplicationService) getApplicationService())
                        .<BoxDynamicObject, TabDynamicObject, EditTabDynamicObject, DynamicPropertiesDefinition>getEditTabByDisplayTab(
                                Integer.parseInt(paramFuzzyTabId),
                                EditTabDynamicObject.class);
                areaId = fuzzyEditTab.getId();
            }
        }
        else
        {
            areaId = Integer.parseInt(paramTabId);
        }

        EditTabDynamicObject editT = null;
		if (areaId != null) {
			editT = getApplicationService().get(EditTabDynamicObject.class, areaId);
		}
		
        List<EditTabDynamicObject> tabs = getApplicationService().findEditTabByType(EditTabDynamicObject.class, entity.getTypo());
        List<EditTabDynamicObject> authorizedTabs = new LinkedList<EditTabDynamicObject>();
        
        for(EditTabDynamicObject tab : tabs) {
            if(CrisAuthorizeManager.authorize(context, getApplicationService(), ResearchObject.class, DynamicPropertiesDefinition.class, id, tab)) {
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

        List<BoxDynamicObject> propertyHolders = new LinkedList<BoxDynamicObject>();
        if (editT.getDisplayTab() != null)
        {
            for (BoxDynamicObject box : editT.getDisplayTab().getMask())
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

        for (BoxDynamicObject iph : propertyHolders)
        {
            if (editT.getDisplayTab() != null)
            {
                tipProprietaInArea
                        .addAll(getApplicationService()
                                .<BoxDynamicObject, it.cilea.osd.jdyna.web.Tab<BoxDynamicObject>, DynamicPropertiesDefinition> findContainableInPropertyHolder(
                                        BoxDynamicObject.class, iph.getId()));
            }
            else
            {
                tipProprietaInArea
                        .addAll(getApplicationService()
                                .<BoxDynamicObject, it.cilea.osd.jdyna.web.Tab<BoxDynamicObject>, DynamicPropertiesDefinition> findContainableInPropertyHolder(
                                        getClazzBox(), iph.getId()));
            }
        }
        DynamicAdditionalFieldStorage dynamicObject = entity.getDynamicField();
        DynamicAnagraficaObjectDTO anagraficaObjectDTO = new DynamicAnagraficaObjectDTO(
                entity);
        anagraficaObjectDTO.setTabId(areaId);
        anagraficaObjectDTO.setObjectId(entity.getId());
        anagraficaObjectDTO.setParentId(entity.getId());
        anagraficaObjectDTO.setTipologiaId(entity.getTypo().getId());
        
        List<DynamicPropertiesDefinition> realTPS = new LinkedList<DynamicPropertiesDefinition>();
        List<IContainable> structuralField = new LinkedList<IContainable>();
        for (IContainable c : tipProprietaInArea)
        {

            DynamicPropertiesDefinition rpPd = getApplicationService()
                    .findPropertiesDefinitionByShortName(
                            DynamicPropertiesDefinition.class, c.getShortName());
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
        DynamicAnagraficaObjectDTO anagraficaObjectDTO = (DynamicAnagraficaObjectDTO) object;

        String exitPage = "redirect:/cris/tools/" + getSpecificPartPath() + "/editDynamicData.htm?id="
                + anagraficaObjectDTO.getParentId();

        EditTabDynamicObject editT = getApplicationService().get(
                EditTabDynamicObject.class, anagraficaObjectDTO.getTabId());
        
        ResearchObject entity = getApplicationService().get(ResearchObject.class,
                anagraficaObjectDTO.getParentId());        
        if (anagraficaObjectDTO.getNewTabId() != null)
        {
            exitPage += "&tabId=" + anagraficaObjectDTO.getNewTabId();
        }
        else
        {
            exitPage = "redirect:/cris/"+ getSpecificPartPath() +"/"
                    + entity.getCrisID();
        }
        if (request.getParameter("cancel") != null)
        {
            return new ModelAndView(exitPage);
        }

        DynamicAdditionalFieldStorage myObject = entity.getDynamicField();

        List<BoxDynamicObject> propertyHolders = new LinkedList<BoxDynamicObject>();
        if (editT.getDisplayTab() != null)
        {
            for (BoxDynamicObject box : editT.getDisplayTab().getMask())
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

        for (BoxDynamicObject iph : propertyHolders)
        {

            tipProprietaInArea
                    .addAll(getApplicationService()
                            .<BoxDynamicObject, it.cilea.osd.jdyna.web.Tab<BoxDynamicObject>, DynamicPropertiesDefinition> findContainableInPropertyHolder(
                                    getClazzBox(), iph.getId()));

        }

        List<DynamicPropertiesDefinition> realTPS = new LinkedList<DynamicPropertiesDefinition>();
        List<IContainable> structuralField = new LinkedList<IContainable>();
        for (IContainable c : tipProprietaInArea)
        {
            DynamicPropertiesDefinition rpPd = getApplicationService()
                    .findPropertiesDefinitionByShortName(
                            DynamicPropertiesDefinition.class, c.getShortName());
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
        entity.setSourceID(anagraficaObjectDTO.getSourceID());
        String sourceref = StringUtils.isNotBlank(anagraficaObjectDTO.getSourceRef()) ? anagraficaObjectDTO.getSourceRef() : ConfigurationManager.getProperty(CrisConstants.CFG_MODULE, "sourceref.default");
        entity.setSourceRef(sourceref); 
        entity.setStatus(anagraficaObjectDTO.getStatus());
        entity.setTypo(getApplicationService().get(DynamicObjectType.class, anagraficaObjectDTO.getTipologiaId()));

        getApplicationService().saveOrUpdate(ResearchObject.class, entity);
        EditTabDynamicObject area = getApplicationService().get(getClazzTab(),
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
        ResearchObject researcher = getApplicationService().get(
                ResearchObject.class, dto.getParentId());
        DynamicAdditionalFieldStorage myObject = researcher.getDynamicField();

        EditTabDynamicObject editT = getApplicationService().get(
                EditTabDynamicObject.class, dto.getTabId());
        List<BoxDynamicObject> propertyHolders = new LinkedList<BoxDynamicObject>();
        if (editT.getDisplayTab() != null)
        {
            for (BoxDynamicObject box : editT.getDisplayTab().getMask())
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

        for (BoxDynamicObject iph : propertyHolders)
        {

            tipProprietaInArea
                    .addAll(getApplicationService()
                            .<BoxDynamicObject, it.cilea.osd.jdyna.web.Tab<BoxDynamicObject>, DynamicPropertiesDefinition> findContainableInPropertyHolder(
                                    getClazzBox(), iph.getId()));

        }

        List<DynamicPropertiesDefinition> realTPS = new LinkedList<DynamicPropertiesDefinition>();
        List<IContainable> structuralField = new LinkedList<IContainable>();
        for (IContainable c : tipProprietaInArea)
        {
            DynamicPropertiesDefinition rpPd = getApplicationService()
                    .findPropertiesDefinitionByShortName(
                            DynamicPropertiesDefinition.class, c.getShortName());
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
