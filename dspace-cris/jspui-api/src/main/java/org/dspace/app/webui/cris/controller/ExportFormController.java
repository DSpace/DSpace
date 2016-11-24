/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.cris.discovery.CrisSearchService;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.OrganizationUnit;
import org.dspace.app.cris.model.Project;
import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.jdyna.DynamicNestedPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.DynamicObjectType;
import org.dspace.app.cris.model.jdyna.DynamicPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.DynamicTypeNestedObject;
import org.dspace.app.cris.model.jdyna.TabResearcherPage;
import org.dspace.app.cris.util.ImportExportUtils;
import org.dspace.app.webui.cris.dto.ExportParametersDTO;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import it.cilea.osd.jdyna.model.ATypeNestedObject;
import it.cilea.osd.jdyna.model.IContainable;

/**
 * This SpringMVC controller is responsible to handle request of export
 * 
 * @author cilea
 * 
 */
public class ExportFormController extends BaseFormController
{

    private static final DateFormat dateFormat = new SimpleDateFormat(
            "dd-MM-yyyy HH:mm");

    private CrisSearchService searchService;

    @Override
    protected Map referenceData(HttpServletRequest request) throws Exception
    {

        Map<String, Object> map = new HashMap<String, Object>();

        map.put("tabs", applicationService.getList(TabResearcherPage.class));
        map.put("dynamicobjects",
                applicationService.getList(DynamicObjectType.class));
        return map;
    }

    @Override
    protected Object formBackingObject(HttpServletRequest request)
            throws Exception
    {
        Context context = UIUtil.obtainContext(request);
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "Only system administrator can access to the export functionality");
        }
        return super.formBackingObject(request);
    }

    @Override
    protected ModelAndView onSubmit(HttpServletRequest request,
            HttpServletResponse response, Object command, BindException errors)
                    throws Exception
    {
        ExportParametersDTO exportParameters = (ExportParametersDTO) command;
        ACrisObject object = null;
        List<ACrisObject> list = new ArrayList<ACrisObject>();
        try
        {
            try
            {
                SolrQuery query = new SolrQuery(exportParameters.getQuery());
                if (exportParameters.getFilter() != null)
                {
                    int parseInt = Integer
                            .parseInt(exportParameters.getFilter());
                    if (parseInt > CrisConstants.CRIS_DYNAMIC_TYPE_ID_START)
                    {
                        object = new ResearchObject();                        
                    }
                    else
                    {
                        if (parseInt == CrisConstants.RP_TYPE_ID)
                        {
                            object = new ResearcherPage();
                        }
                        else
                        {
                            if (parseInt == CrisConstants.PROJECT_TYPE_ID)
                            {
                                object = new Project();
                            }
                            else
                            {
                                object = new OrganizationUnit();
                            }
                        }
                    }
                    query.addFilterQuery("{!field f=search.resourcetype}"
                            + exportParameters.getFilter());
                }
                query.setFields("search.resourceid", "search.resourcetype",
                        "cris-uuid");
                query.setRows(Integer.MAX_VALUE);
                searchService.commit();
                QueryResponse qresponse = searchService.search(query);
                SolrDocumentList docList = qresponse.getResults();
                Iterator<SolrDocument> solrDoc = docList.iterator();
                while (solrDoc.hasNext())
                {
                    SolrDocument doc = solrDoc.next();
                    String uuid = (String) doc.getFirstValue("cris-uuid");
                    list.add(applicationService.getEntityByUUID(uuid));
                }
            }
            catch (SearchServiceException e)
            {
                log.error("Error retrieving documents", e);
            }
        }
        catch (Exception e)
        {
            errors.reject("jsp.layout.hku.export.validation.notvalid.query");
            return showForm(request, errors, getFormView());

        }

        List<IContainable> metadataFirstLevel = new ArrayList<IContainable>();
        List<IContainable> metadataNestedLevel = new LinkedList<IContainable>();
        
        int parseInt = Integer
                .parseInt(exportParameters.getFilter());
        if (parseInt > CrisConstants.CRIS_DYNAMIC_TYPE_ID_START)
        {
            DynamicObjectType type = applicationService.get(DynamicObjectType.class, (parseInt-CrisConstants.CRIS_DYNAMIC_TYPE_ID_START));
            List<DynamicPropertiesDefinition> tps = type.getMask();            
            for (DynamicPropertiesDefinition tp : tps)
            {
                IContainable ic = applicationService.findContainableByDecorable(
                        tp.getDecoratorClass(), tp.getId());
                if (ic != null)
                {
                    metadataFirstLevel.add(ic);
                }
            }
            List<DynamicTypeNestedObject> ttps = type.getTypeNestedDefinitionMask();
            for (DynamicTypeNestedObject ttp : ttps)
            {
                IContainable ic = applicationService.findContainableByDecorable(
                        ttp.getDecoratorClass(), ttp.getId());
                if (ic != null)
                {
                    metadataNestedLevel.add(ic);
                }
            }
        }
        else
        {
            metadataFirstLevel = applicationService
                    .findAllContainables(object.getClassPropertiesDefinition());
            List<ATypeNestedObject> ttps = applicationService
                    .getList(object.getClassTypeNested());
            
            for (ATypeNestedObject ttp : ttps)
            {
                IContainable ic = applicationService.findContainableByDecorable(
                        ttp.getDecoratorClass(), ttp.getId());
                if (ic != null)
                {
                    metadataNestedLevel.add(ic);
                }
            }
        }



        // if (exportParameters.getMainMode() == null) {
        response.setContentType("application/excel");
        response.addHeader("Content-Disposition",
                "attachment; filename=dspace-cris-exportdata.xls");
        ImportExportUtils.exportExcel(list, applicationService,
                response.getOutputStream(), metadataFirstLevel,
                metadataNestedLevel);
        response.getOutputStream().flush();
        response.getOutputStream().close();
        // } else {
        // response.setContentType("application/xml;charset=UTF-8");
        // response.addHeader("Content-Disposition",
        // "attachment; filename=dspace-cris-exportdata.xml");
        // ImportExportUtils.exportXML(response.getWriter(),
        // applicationService, metadataFirstLevel, metadataNestedLevel,
        // list);
        // response.getWriter().flush();
        // response.getWriter().close();
        // }

        return null;
    }

    private void addToTempQuery(String fieldName, String value, List<String> f,
            List<String> q, boolean escape)
    {
        if (StringUtils.isNotBlank(value))
        {
            q.add(escape ? ClientUtils.escapeQueryChars(value) : value);
            f.add(fieldName);
        }
    }

    public CrisSearchService getSearchService()
    {
        return searchService;
    }

    public void setSearchService(CrisSearchService searchService)
    {
        this.searchService = searchService;
    }
}
