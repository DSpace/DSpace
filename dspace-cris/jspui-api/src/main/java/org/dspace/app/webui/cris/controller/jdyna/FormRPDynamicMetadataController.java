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
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.dto.RPAnagraficaObjectDTO;
import org.dspace.app.cris.model.jdyna.BoxResearcherPage;
import org.dspace.app.cris.model.jdyna.EditTabResearcherPage;
import org.dspace.app.cris.model.jdyna.RPAdditionalFieldStorage;
import org.dspace.app.cris.model.jdyna.RPNestedObject;
import org.dspace.app.cris.model.jdyna.RPNestedPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.RPNestedProperty;
import org.dspace.app.cris.model.jdyna.RPPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.RPProperty;
import org.dspace.app.cris.model.jdyna.TabResearcherPage;
import org.dspace.app.cris.model.jdyna.VisibilityTabConstant;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

public class FormRPDynamicMetadataController
        extends
        AFormDynamicRPController<RPProperty, RPPropertiesDefinition, BoxResearcherPage, EditTabResearcherPage, AnagraficaObject<RPProperty, RPPropertiesDefinition>, RPNestedObject, RPNestedProperty, RPNestedPropertiesDefinition>
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

        RPAnagraficaObjectDTO anagraficaObjectDTO = (RPAnagraficaObjectDTO) command;

        // check admin authorization
        boolean isAdmin = false;
        Context context = UIUtil.obtainContext(request);
        if (AuthorizeManager.isAdmin(context))
        {
            isAdmin = true;
        }

        // collection of edit tabs (all edit tabs created on system associate to
        // visibility)
        List<EditTabResearcherPage> tabs = getApplicationService()
                .getTabsByVisibility(EditTabResearcherPage.class,
                        isAdmin);

        EPerson currentUser = context.getCurrentUser();
        
        // if admin but also owner of the profile add the reserved tabs as well
		if (isAdmin && anagraficaObjectDTO.getEpersonID() != null
				&& currentUser.getID() == anagraficaObjectDTO.getEpersonID()) {
			List<EditTabResearcherPage> extratabs = getApplicationService().getTabsByAccessLevel(EditTabResearcherPage.class, VisibilityTabConstant.LOW);
			if (extratabs != null) {
				tabs.addAll(extratabs);
			}
        }
        
        // check if request tab from view is active (check on collection before)
        EditTabResearcherPage editT = getApplicationService().get(
                EditTabResearcherPage.class,
                anagraficaObjectDTO.getTabId());
        if (!tabs.contains(editT))
        {
            throw new AuthorizeException(
                    "You not have needed authorization level to display this tab");
        }

        // collection of boxs
        List<BoxResearcherPage> propertyHolders = new LinkedList<BoxResearcherPage>();

        // if edit tab got a display tab (edit tab is hookup to display tab)
        // then edit box will be created from display box otherwise get all boxs
        // in edit tab
        if (editT.getDisplayTab() != null)
        {
            for (BoxResearcherPage box : editT.getDisplayTab()
                    .getMask())
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
        List<BoxResearcherPage> propertyHoldersCurrentAccessLevel = new LinkedList<BoxResearcherPage>();
        for (BoxResearcherPage propertyHolder : propertyHolders)
        {
            if (isAdmin)
            {
                if ((currentUser!=null && (anagraficaObjectDTO.getEpersonID()!=null && currentUser.getID()==anagraficaObjectDTO.getEpersonID()))
                       || !propertyHolder.getVisibility().equals(
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
        for (BoxResearcherPage iph : propertyHoldersCurrentAccessLevel)
        {
            List<IContainable> temp = getApplicationService()
                    .<BoxResearcherPage, it.cilea.osd.jdyna.web.Tab<BoxResearcherPage>, RPPropertiesDefinition> findContainableInPropertyHolder(
                            getClazzBox(), iph.getId());            
            mapBoxToContainables.put(iph.getShortName(), temp);
            pDInTab.addAll(temp);
        }

        map.put("propertiesHolders", propertyHoldersCurrentAccessLevel);
        map.put("propertiesDefinitionsInTab", pDInTab);
        map.put("propertiesDefinitionsInHolder", mapBoxToContainables);
        map.put("tabList", tabs);
        map.put("simpleNameAnagraficaObject", getClazzAnagraficaObject()
                .getSimpleName());
        map.put("addModeType", "edit");
        return map;
    }
    
    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request,
            HttpServletResponse response) throws Exception
    {
        try {
            return super.handleRequestInternal(request, response);
        }
        catch (AuthorizeException ex) {
            JSPManager
            .showAuthorizeError(
                    request,
                    response,
                    new AuthorizeException(
                            "Only system administrator can access to disabled researcher page"));
        }
        return null;
    }

    @Override
    protected Object formBackingObject(HttpServletRequest request)
            throws Exception
    {
        String paramFuzzyTabId = request.getParameter("hooktabId");
        String paramTabId = request.getParameter("tabId");
        String paramId = request.getParameter("id");

        Integer id = null;
        Boolean isAdmin = false;
        if (paramId != null)
        {
            id = Integer.parseInt(paramId);
        }
        ResearcherPage researcher = getApplicationService().get(
                ResearcherPage.class, id);
        Context context = UIUtil.obtainContext(request);
        EPerson currentUser = context.getCurrentUser();
        if ((currentUser==null || (researcher.getEpersonID()!=null && currentUser.getID()!=researcher.getEpersonID()))
               && !AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "Only system admin can edit not personal researcher page");
        }

        if (AuthorizeManager.isAdmin(context))
        {
            isAdmin = true;
        }

        Integer areaId;
        if (paramTabId == null)
        {
            if (paramFuzzyTabId == null)
            {
                List<EditTabResearcherPage> tabs = getApplicationService()
                        .getTabsByVisibility(
                                EditTabResearcherPage.class, isAdmin);
                if (tabs.isEmpty())
                {
                    throw new AuthorizeException("No tabs defined!!");
                }
                areaId = tabs.get(0).getId();
            }
            else
            {
                EditTabResearcherPage fuzzyEditTab = (EditTabResearcherPage)((ApplicationService)getApplicationService()).<BoxResearcherPage, TabResearcherPage, EditTabResearcherPage, RPPropertiesDefinition>getEditTabByDisplayTab(Integer.parseInt(paramFuzzyTabId),EditTabResearcherPage.class);
                areaId = fuzzyEditTab.getId();
            }
        }
        else
        {
            areaId = Integer.parseInt(paramTabId);
        }
        
        EditTabResearcherPage editT = getApplicationService().get(
                EditTabResearcherPage.class, areaId);
        List<BoxResearcherPage> propertyHolders = new LinkedList<BoxResearcherPage>();
        if (editT.getDisplayTab() != null)
        {
            for (BoxResearcherPage box : editT.getDisplayTab()
                    .getMask())
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

        for (BoxResearcherPage iph : propertyHolders)
        {
            if (editT.getDisplayTab() != null)
            {
                tipProprietaInArea
                        .addAll(getApplicationService()
                                .<BoxResearcherPage, it.cilea.osd.jdyna.web.Tab<BoxResearcherPage>, RPPropertiesDefinition> findContainableInPropertyHolder(
                                        BoxResearcherPage.class,
                                        iph.getId()));
            }
            else
            {
                tipProprietaInArea
                        .addAll(getApplicationService()
                                .<BoxResearcherPage, it.cilea.osd.jdyna.web.Tab<BoxResearcherPage>, RPPropertiesDefinition> findContainableInPropertyHolder(
                                        getClazzBox(), iph.getId()));
            }
        }

        RPAdditionalFieldStorage dynamicObject = researcher.getDynamicField();
        RPAnagraficaObjectDTO anagraficaObjectDTO = new RPAnagraficaObjectDTO(
                researcher);
        anagraficaObjectDTO.setTabId(areaId);
        anagraficaObjectDTO.setObjectId(dynamicObject.getId());
        anagraficaObjectDTO.setParentId(researcher.getId());

        List<RPPropertiesDefinition> realTPS = new LinkedList<RPPropertiesDefinition>();
        List<IContainable> structuralField = new LinkedList<IContainable>();
        for (IContainable c : tipProprietaInArea)
        {
            RPPropertiesDefinition rpPd = getApplicationService()
                    .findPropertiesDefinitionByShortName(
                            RPPropertiesDefinition.class, c.getShortName());
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
        RPAnagraficaObjectDTO anagraficaObjectDTO = (RPAnagraficaObjectDTO) object;

        String exitPage = "redirect:/cris/tools/rp/editDynamicData.htm?id="
                + anagraficaObjectDTO.getParentId();

        EditTabResearcherPage editT = getApplicationService().get(
                EditTabResearcherPage.class,
                anagraficaObjectDTO.getTabId());
        ResearcherPage researcher = getApplicationService().get(
                ResearcherPage.class, anagraficaObjectDTO.getParentId());
        
        if (anagraficaObjectDTO.getNewTabId() != null)
        {
            exitPage += "&tabId=" + anagraficaObjectDTO.getNewTabId();
        }
        else
        {
            exitPage = "redirect:/cris/rp/"
                    + researcher.getCrisID();
        }
        if (request.getParameter("cancel") != null)
        {
            return new ModelAndView(exitPage);
        }
        
        RPAdditionalFieldStorage myObject = researcher.getDynamicField();

        List<BoxResearcherPage> propertyHolders = new LinkedList<BoxResearcherPage>();
        if (editT.getDisplayTab() != null)
        {
            for (BoxResearcherPage box : editT.getDisplayTab()
                    .getMask())
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

        for (BoxResearcherPage iph : propertyHolders)
        {

            tipProprietaInArea
                    .addAll(getApplicationService()
                            .<BoxResearcherPage, it.cilea.osd.jdyna.web.Tab<BoxResearcherPage>, RPPropertiesDefinition> findContainableInPropertyHolder(
                                    getClazzBox(), iph.getId()));

        }

        List<RPPropertiesDefinition> realTPS = new LinkedList<RPPropertiesDefinition>();
        List<IContainable> structuralField = new LinkedList<IContainable>();
        for (IContainable c : tipProprietaInArea)
        {
            RPPropertiesDefinition rpPd = getApplicationService()
                    .findPropertiesDefinitionByShortName(
                            RPPropertiesDefinition.class, c.getShortName());
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
        researcher.setSourceID(anagraficaObjectDTO.getSourceID());
        String sourceref = StringUtils.isNotBlank(anagraficaObjectDTO.getSourceRef()) ? anagraficaObjectDTO.getSourceRef() : ConfigurationManager.getProperty(CrisConstants.CFG_MODULE, "sourceref.default");
        researcher.setSourceRef(sourceref); 
        researcher.setStatus(anagraficaObjectDTO.getStatus());
        researcher.setEpersonID(anagraficaObjectDTO.getEpersonID());
        
        getApplicationService().saveOrUpdate(ResearcherPage.class, researcher);
        EditTabResearcherPage area = getApplicationService().get(
                getClazzTab(), anagraficaObjectDTO.getTabId());
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
        ResearcherPage researcher = getApplicationService().get(
                ResearcherPage.class, dto.getParentId());
        RPAdditionalFieldStorage myObject = researcher.getDynamicField();

        EditTabResearcherPage editT = getApplicationService().get(
                EditTabResearcherPage.class, dto.getTabId());
        List<BoxResearcherPage> propertyHolders = new LinkedList<BoxResearcherPage>();
        if (editT.getDisplayTab() != null)
        {
            for (BoxResearcherPage box : editT.getDisplayTab()
                    .getMask())
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

        for (BoxResearcherPage iph : propertyHolders)
        {

            tipProprietaInArea
                    .addAll(getApplicationService()
                            .<BoxResearcherPage, it.cilea.osd.jdyna.web.Tab<BoxResearcherPage>, RPPropertiesDefinition> findContainableInPropertyHolder(
                                    getClazzBox(), iph.getId()));

        }

        List<RPPropertiesDefinition> realTPS = new LinkedList<RPPropertiesDefinition>();
        List<IContainable> structuralField = new LinkedList<IContainable>();
        for (IContainable c : tipProprietaInArea)
        {
            RPPropertiesDefinition rpPd = getApplicationService()
                    .findPropertiesDefinitionByShortName(
                            RPPropertiesDefinition.class, c.getShortName());
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
