/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.batch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.cris.discovery.CrisSearchService;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.OrganizationUnit;
import org.dspace.app.cris.model.Project;
import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.jdyna.ACrisNestedObject;
import org.dspace.app.cris.model.jdyna.DynamicObjectType;
import org.dspace.app.cris.model.jdyna.DynamicPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.DynamicTypeNestedObject;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.util.ImportExportUtils;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.utils.DSpace;

import it.cilea.osd.jdyna.model.ANestedPropertiesDefinition;
import it.cilea.osd.jdyna.model.ANestedProperty;
import it.cilea.osd.jdyna.model.ATypeNestedObject;
import it.cilea.osd.jdyna.model.IContainable;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.model.Property;

public class ScriptCrisBulkExport {
	/** log4j logger */
	private static Logger log = Logger.getLogger(ScriptCrisBulkExport.class);

    public static <ACO extends ACrisObject<P, TP, NP, NTP, ACNO, ATNO>, P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> void main(
            String[] args) throws ParseException, SQLException {

        log.info("#### START export: -----" + new Date() + " ----- ####");
        Context dspaceContext = new Context();
        dspaceContext.setIgnoreAuthorization(true);
        
        String filePath = null;
        CommandLineParser parser = new PosixParser();

        Options options = new Options();
        options.addOption("h", "help", false, "help");
        options.addOption("f", "file", true, "File to export*");
        options.addOption("e", "entity", true, "The entity type to import (rp, ou, pj, others**)");
        options.addOption("q", "query", true, "Query filter");
        CommandLine line = parser.parse(options, args);

        String entityType = "rp";
        if (line.hasOption("e")) {
            entityType = line.getOptionValue("e");
        }

        String query = "*:*";
        if (line.hasOption("q")) {
            query = line.getOptionValue("q");
        }
        
        if (line.hasOption('h') || StringUtils.isEmpty(entityType)) {
            HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp("ScriptCrisBulkExport \n", options);
            System.out
                    .println("\n\nUSAGE:\n ScriptCrisBulkExport -e (rp|ou|) [-f path_file] [-q query]-\n"); 
            System.exit(0);
        }

        if (!line.hasOption("f")) {
            filePath = ImportExportUtils.PATH_EXPORT_EXCEL_DEFAULT;
        } else {
            filePath = line.getOptionValue("f");
        }
        String path = ConfigurationManager.getProperty(CrisConstants.CFG_MODULE, "file.export.path");       
        File dir = new File(path);
        if(!dir.exists()) {
            dir.mkdir();
        }
        File exportFile = new File(filePath);
        OutputStream out = null;
        try {
            if(!exportFile.exists()){
                exportFile.createNewFile();
            }
            out = new FileOutputStream(exportFile);
            ScriptCrisBulkExport.export(query, entityType, out);
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            try{
                if(out!=null){
                    out.close();
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
        
        log.info("#### END EXPORT: -----" + new Date() + " ----- ####");
        dspaceContext.complete();
    }
    
    public static <ACO extends ACrisObject<P, TP, NP, NTP, ACNO, ATNO>, P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> void export(String queryString, String entityType, OutputStream out) throws Exception {

        DSpace dspace = new DSpace();
        ApplicationService applicationService = dspace.getServiceManager().getServiceByName("applicationService",
                ApplicationService.class);
        CrisSearchService searchService = new DSpace().getServiceManager()
                .getServiceByName("org.dspace.discovery.SearchService",
                        CrisSearchService.class);
        
        int entity = -1;
        ACO tmpCrisObject = null;
        
        if (StringUtils.equalsIgnoreCase("rp", entityType))
        {
            tmpCrisObject = (ACO) new ResearcherPage();
        }
        else if (StringUtils.equalsIgnoreCase("ou", entityType))
        {
            tmpCrisObject = (ACO) new OrganizationUnit();
        }
        else if (StringUtils.equalsIgnoreCase("pj", entityType))
        {
            tmpCrisObject = (ACO) new Project();
        }
        else
        {
            ResearchObject tmp = new ResearchObject();
            tmp.setTypo(applicationService
                    .findTypoByShortName(DynamicObjectType.class, entityType));
            tmpCrisObject = (ACO) tmp;
        }
        
        List<ACO> list = new ArrayList<ACO>();

        SolrQuery query = new SolrQuery(queryString);
        query.addFilterQuery(
                "{!field f=search.resourcetype}" + tmpCrisObject.getType());
        query.setFields("search.resourceid", "search.resourcetype",
                "cris-uuid");
        query.setRows(Integer.MAX_VALUE);
        QueryResponse qresponse = searchService.search(query);
        SolrDocumentList docList = qresponse.getResults();
        Iterator<SolrDocument> solrDoc = docList.iterator();
        while (solrDoc.hasNext())
        {
            try
            {
                SolrDocument doc = solrDoc.next();
                String uuid = (String) doc.getFirstValue("cris-uuid");
                list.add((ACO) applicationService.getEntityByUUID(uuid));
            }
            catch (Exception e)
            {
                log.error(e.getMessage(), e);
            }
        }

        List<IContainable> metadataFirstLevel = new ArrayList<IContainable>();
        List<IContainable> metadataNestedLevel = new LinkedList<IContainable>();
        
        if (entity > CrisConstants.CRIS_DYNAMIC_TYPE_ID_START) {
            DynamicObjectType type = applicationService.get(DynamicObjectType.class, CrisConstants.CRIS_DYNAMIC_TYPE_ID_START);
            List<DynamicPropertiesDefinition> tps = type.getMask();            
            for (DynamicPropertiesDefinition tp : tps) {
                IContainable ic = applicationService.findContainableByDecorable(tp.getDecoratorClass(), tp.getId());
                if (ic != null) {
                    metadataFirstLevel.add(ic);
                }
            }
            List<DynamicTypeNestedObject> ttps = type.getTypeNestedDefinitionMask();
            for (DynamicTypeNestedObject ttp : ttps) {
                IContainable ic = applicationService.findContainableByDecorable(
                        ttp.getDecoratorClass(), ttp.getId());
                if (ic != null){
                    metadataNestedLevel.add(ic);
                }
            }
        } else {
            metadataFirstLevel = applicationService.findAllContainables(tmpCrisObject.getClassPropertiesDefinition());
            List<ATNO> ttps = applicationService.getList(tmpCrisObject.getClassTypeNested());
            
            for (ATNO ttp : ttps){
                IContainable ic = applicationService.findContainableByDecorable(ttp.getDecoratorClass(), ttp.getId());
                if (ic != null){
                    metadataNestedLevel.add(ic);
                }
            }
        }
        ImportExportUtils.exportExcel(list, applicationService,
                out, metadataFirstLevel,
                metadataNestedLevel);
        return;
    }
}
