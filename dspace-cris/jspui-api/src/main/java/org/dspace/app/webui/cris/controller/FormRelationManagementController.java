/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.cris.configuration.RelationPreferenceConfiguration;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.RelationPreference;
import org.dspace.app.cris.service.RelationPreferenceService;
import org.dspace.app.webui.cris.dto.ManageRelationDTO;
import org.dspace.app.webui.cris.util.RelationPreferenceUtil;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SolrServiceImpl;
import org.dspace.utils.DSpace;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

/**
 * This SpringMVC controller allow owners of the CRIS Object to handles
 * relations with other object such publications, projects, etc. Relations can
 * be: - hide, unhide, - select, unselect
 * 
 * @author Andrea Bollini
 * 
 */
public class FormRelationManagementController extends BaseFormController
{
    private DSpace dspace = new DSpace();

    private RelationPreferenceService relationService = dspace
            .getServiceManager().getServiceByName(
                    RelationPreferenceService.class.getName(),
                    RelationPreferenceService.class);

    private SearchService searcher = dspace.getServiceManager()
            .getServiceByName(SearchService.class.getName(),
                    SearchService.class);
    
    private RelationPreferenceUtil relationPreferenceUtil;

    public void setRelationPreferenceUtil(
            RelationPreferenceUtil relationPreferenceUtil)
    {
        this.relationPreferenceUtil = relationPreferenceUtil;
    }
    
    private ACrisObject getCRISObject(HttpServletRequest request)
    {
        String uuid = getUUID(request);
        ACrisObject cris = applicationService.getEntityByUUID(uuid);
        return cris;
    }

    private String getUUID(HttpServletRequest request)
    {
        String pathInfo = request.getPathInfo();
        // example /uuid/XXXXXXXXX/relMgmt/publications
        String path = pathInfo.substring("/uuid/".length());
        String[] splitted = path.split("/");
        String uuid = splitted[0];
        return uuid;
    }

    private String getRelationType(HttpServletRequest request)
    {
        String pathInfo = request.getPathInfo();
        // example /uuid/XXXXXXXXX/relMgmt/publications
        String path = pathInfo.substring("/uuid/".length());
        String[] splitted = path.split("/");
        String type = splitted[2];
        return type;
    }

    private int[] getIDsFromItemList(List<Item> items)
    {
        int[] result = new int[items.size()];
        int idx = 0;
        for (Item i : items)
        {
            result[idx] = i.getID();
            idx++;
        }
        return result;
    }

    @Override
    protected Map referenceData(HttpServletRequest request, Object command,
            Errors errors) throws Exception
    {
        Context context = UIUtil.obtainContext(request);
        ACrisObject cris = getCRISObject(request);
        AuthorizeManager.authorizeAction(context, cris, Constants.ADMIN, false);
        
        String relationType = getRelationType(request);
        Map<String, Object> data = new HashMap<String, Object>();
        String confName = relationService.getConfigurationName(cris,
                relationType);
        RelationPreferenceConfiguration conf = relationService
                .getConfigurationService().getRelationPreferenceConfiguration(
                        confName);
        
        if (conf == null)
        {
            throw new IllegalArgumentException("No ReleationPreferenceConfiguration has found for "+confName);
        }
        data.put("cris", cris);
        data.put("confName", confName);
        data.put("relationType", relationType);
        boolean admin = AuthorizeManager.isAdmin(context);
        data.put("isSelectEnabled", conf.isActionEnabled(RelationPreference.SELECTED, admin));
        data.put("isHideEnabled", conf.isActionEnabled(RelationPreference.HIDED, admin));
        data.put("isUnlinkEnabled", conf.isActionEnabled(RelationPreference.UNLINKED, admin));
        data.put("selected", relationPreferenceUtil.getSelected(context, cris, relationType));
        data.put("columns", conf.getColumnsVisualizationConfiguration());
        return data;
    }

    protected ModelAndView onSubmit(HttpServletRequest request,
            HttpServletResponse response, Object command, BindException errors)
            throws Exception
    {
        ACrisObject cris = getCRISObject(request);
        if (!"submit_exit".equalsIgnoreCase(UIUtil.getSubmitButton(request, "exit")))
        {
            Context context = UIUtil.obtainContext(request);
            ManageRelationDTO dto = (ManageRelationDTO) command;
            String relationType = getRelationType(request);
            boolean doneChange = false;
    
            doneChange = relationService.unlink(context, cris, relationType,
                    dto.getToUnLink())
                    || doneChange;
            doneChange = relationService.active(context, cris, relationType,
                    dto.getToActivate())
                    || doneChange;
            doneChange = relationService.hide(context, cris, relationType,
                    dto.getToHide())
                    || doneChange;
    
            List<String> newSelectedItems = dto.getOrderedSelected();
            doneChange = relationService.select(context, cris, relationType,
                    newSelectedItems) || doneChange;
    
            if (doneChange)
            {
                // make sure to commit the SOLR change
                ((SolrServiceImpl) searcher).commit();
            }
        }
        return new ModelAndView(getSuccessView() + cris.getUuid());
    }
}
