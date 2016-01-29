/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.ws.interceptor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.app.cris.importexport.IBulkChangesService;
import org.dspace.app.cris.importexport.XMLBulkChangesService;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.OrganizationUnit;
import org.dspace.app.cris.model.Project;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.export.ExportConstants;
import org.dspace.app.cris.model.jdyna.ACrisNestedObject;
import org.dspace.app.cris.model.jdyna.OUNestedPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.OUPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.OUTypeNestedObject;
import org.dspace.app.cris.model.jdyna.ProjectNestedPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.ProjectPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.ProjectTypeNestedObject;
import org.dspace.app.cris.model.jdyna.RPNestedPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.RPPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.RPTypeNestedObject;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.util.UtilsXSD;
import org.dspace.core.ConfigurationManager;
import org.dspace.utils.DSpace;
import org.springframework.core.io.Resource;
import org.springframework.ws.soap.server.endpoint.interceptor.PayloadValidatingInterceptor;

import it.cilea.osd.jdyna.model.ANestedPropertiesDefinition;
import it.cilea.osd.jdyna.model.ANestedProperty;
import it.cilea.osd.jdyna.model.ATypeNestedObject;
import it.cilea.osd.jdyna.model.IContainable;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.model.Property;

public class CustomResponsePayloadValidatingInterceptor extends PayloadValidatingInterceptor
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(CustomRequestPayloadValidatingInterceptor.class);
    
    private ApplicationService applicationService;
    
    /**
     * Default absolute path where find the contact data excel file to import
     */
    public static final String PATH_DIR = ConfigurationManager.getProperty(CrisConstants.CFG_MODULE, "webservices.xsd.path");

    @Override
    public void setSchema(Resource schema)
    {
        buildXML("responseresearcherpage.xsd", ResearcherPage.class, RPPropertiesDefinition.class, RPNestedPropertiesDefinition.class, RPTypeNestedObject.class, UtilsXSD.RP_DEFAULT_ELEMENT);
        buildXML("responseresearchergrants.xsd", Project.class, ProjectPropertiesDefinition.class, ProjectNestedPropertiesDefinition.class, ProjectTypeNestedObject.class, UtilsXSD.GRANT_DEFAULT_ELEMENT);
        buildXML("responseorgunits.xsd", OrganizationUnit.class, OUPropertiesDefinition.class, OUNestedPropertiesDefinition.class, OUTypeNestedObject.class, UtilsXSD.OU_DEFAULT_ELEMENT);
        super.setSchema(schema);
    }


	private <ACO extends ACrisObject<P, TP, NP, NTP, ACNO, ATNO>, P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> void buildXML(String name,
			Class<ACO> clazzACO, Class<TP> clazz, Class<NTP> classNTP, Class<ATNO> clazzTypeNested, String[] elementsRoot)
    {
		String[] namespace = UtilsXSD.getNamespace(clazz);
        File dir = new File(PATH_DIR);
        File filexsd = null;
        String nameXSD = name;
        filexsd = new File(dir, nameXSD);
        if (filexsd.exists())
        {
            filexsd.delete();
        }
        try
        {
            filexsd.createNewFile();
        }
        catch (IOException e)
        {
            log.error(e.getMessage(), e);
         
        }
        FileWriter writer = null;
        try
        {
            writer = new FileWriter(filexsd);
        }
        catch (IOException e)
        {
            log.error(e.getMessage(), e);
         
        }

        List<IContainable> metadataALL = null;
        List<IContainable> metadataNestedLevel = null;
        try
        {
            metadataALL = applicationService.newFindAllContainables(clazz);
            List<ATNO> ttps = applicationService.getList(clazzTypeNested);
            metadataNestedLevel = new LinkedList<IContainable>();
            for (ATNO ttp : ttps) {
                IContainable ic = applicationService.findContainableByDecorable(ttp.getDecoratorClass(), ttp.getId());
                if (ic != null) {
                    metadataNestedLevel.add(ic);
                }
            }

        }
        catch (InstantiationException e)
        {
            log.error(e.getMessage(), e);
         
        }
        catch (IllegalAccessException e)
        {
            log.error(e.getMessage(), e);
         
        }
        try
        {
        	DSpace dspace = new DSpace();
            IBulkChangesService importer = dspace.getServiceManager().getServiceByName(XMLBulkChangesService.SERVICE_NAME, IBulkChangesService.class);
			filexsd = importer.generateTemplate(writer, dir, metadataALL, metadataNestedLevel,
					filexsd, elementsRoot,
	        		namespace[0]+":",
	                namespace[1],
	                namespace[1],
	                new String[] {
	                        ExportConstants.NAME_PUBLICID_ATTRIBUTE,
	                        ExportConstants.NAME_BUSINESSID_ATTRIBUTE,
	                        ExportConstants.NAME_ID_ATTRIBUTE,
	                        ExportConstants.NAME_TYPE_ATTRIBUTE },
	                new boolean[] { true, false, false,
	                        true });

        }
        catch (SecurityException e)
        {
            log.error(e.getMessage(), e);
         
        }
        catch (IOException e)
        {
            log.error(e.getMessage(), e);
         
        }
        catch (NoSuchFieldException e)
        {
            log.error(e.getMessage(), e);
        }
        catch (InstantiationException e)
        {
            log.error(e.getMessage(), e);
        }
        catch (IllegalAccessException e)
        {
            log.error(e.getMessage(), e);
        }
    }
    

    public ApplicationService getApplicationService()
    {
        return applicationService;
    }

    public void setApplicationService(ApplicationService applicationService)
    {
        this.applicationService = applicationService;
    }
}
