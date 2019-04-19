/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.batch;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.model.jdyna.BoxDynamicObject;
import org.dspace.app.cris.model.jdyna.BoxOrganizationUnit;
import org.dspace.app.cris.model.jdyna.BoxProject;
import org.dspace.app.cris.model.jdyna.BoxResearcherPage;
import org.dspace.app.cris.model.jdyna.DecoratorDynamicNestedPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.DecoratorDynamicPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.DecoratorDynamicTypeNested;
import org.dspace.app.cris.model.jdyna.DecoratorOUNestedPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.DecoratorOUPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.DecoratorOUTypeNested;
import org.dspace.app.cris.model.jdyna.DecoratorProjectNestedPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.DecoratorProjectPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.DecoratorProjectTypeNested;
import org.dspace.app.cris.model.jdyna.DecoratorRPNestedPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.DecoratorRPPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.DecoratorRPTypeNested;
import org.dspace.app.cris.model.jdyna.DynamicNestedPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.DynamicObjectType;
import org.dspace.app.cris.model.jdyna.DynamicPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.DynamicTypeNestedObject;
import org.dspace.app.cris.model.jdyna.EditTabDynamicObject;
import org.dspace.app.cris.model.jdyna.EditTabOrganizationUnit;
import org.dspace.app.cris.model.jdyna.EditTabProject;
import org.dspace.app.cris.model.jdyna.EditTabResearcherPage;
import org.dspace.app.cris.model.jdyna.OUNestedPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.OUPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.OUTypeNestedObject;
import org.dspace.app.cris.model.jdyna.ProjectNestedPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.ProjectPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.ProjectTypeNestedObject;
import org.dspace.app.cris.model.jdyna.RPNestedPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.RPPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.RPTypeNestedObject;
import org.dspace.app.cris.model.jdyna.TabDynamicObject;
import org.dspace.app.cris.model.jdyna.TabOrganizationUnit;
import org.dspace.app.cris.model.jdyna.TabProject;
import org.dspace.app.cris.model.jdyna.TabResearcherPage;
import org.dspace.app.cris.model.jdyna.VisibilityTabConstant;
import org.dspace.app.cris.model.jdyna.widget.WidgetEPerson;
import org.dspace.app.cris.model.jdyna.widget.WidgetFileDO;
import org.dspace.app.cris.model.jdyna.widget.WidgetFileOU;
import org.dspace.app.cris.model.jdyna.widget.WidgetFileProject;
import org.dspace.app.cris.model.jdyna.widget.WidgetFileRP;
import org.dspace.app.cris.model.jdyna.widget.WidgetGroup;
import org.dspace.app.cris.model.jdyna.widget.WidgetPointerDO;
import org.dspace.app.cris.model.jdyna.widget.WidgetPointerOU;
import org.dspace.app.cris.model.jdyna.widget.WidgetPointerPJ;
import org.dspace.app.cris.model.jdyna.widget.WidgetPointerRP;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.core.Context;
import org.dspace.utils.DSpace;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.orm.hibernate4.HibernateTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;

import it.cilea.osd.jdyna.model.ADecoratorPropertiesDefinition;
import it.cilea.osd.jdyna.model.ADecoratorTypeDefinition;
import it.cilea.osd.jdyna.model.ANestedPropertiesDefinition;
import it.cilea.osd.jdyna.model.ATypeNestedObject;
import it.cilea.osd.jdyna.model.AccessLevelConstants;
import it.cilea.osd.jdyna.model.Containable;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.web.Box;
import it.cilea.osd.jdyna.web.IPropertyHolder;
import it.cilea.osd.jdyna.web.Tab;
import it.cilea.osd.jdyna.widget.Size;
import it.cilea.osd.jdyna.widget.WidgetBoolean;
import it.cilea.osd.jdyna.widget.WidgetCheckRadio;
import it.cilea.osd.jdyna.widget.WidgetDate;
import it.cilea.osd.jdyna.widget.WidgetLink;
import it.cilea.osd.jdyna.widget.WidgetTesto;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.read.biff.BiffException;

public class ImportCRISDataModelConfiguration
{
    private static Logger log = Logger
            .getLogger(ImportCRISDataModelConfiguration.class);

    private static boolean append = false;

    public static void main(String[] args)
            throws ParseException, SQLException, BiffException, IOException,
            InstantiationException, IllegalAccessException
    {
		String fileExcel = null;
		CommandLineParser parser = new PosixParser();
		Options options = new Options();
		options.addOption("f", "excel", true,
				"Excel file that you can use to create/update DSpace-CRIS properties definitions (metadata for all CRIS software entities; NOTE the script doesn't delete anything but works only in append mode so you can use this option many times)");
        options.addOption("a", "append", false,
                "Append mode (TODO manage the clean of all tab and map, now clean only metadata in box and box in tab)");

		CommandLine line = parser.parse(options, args);
        if (line.hasOption('h'))
        {
			HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp(
                    "StartupMetadataConfiguratorTool (BETA version use with caution!!!)\n",
                    options);
			System.exit(0);
		}

        if (line.hasOption('f'))
        {
			fileExcel = line.getOptionValue('f');
		}

        if (line.hasOption('a'))
        {
            append = true;
        }

		Context dspaceContext = new Context();
		dspaceContext.setIgnoreAuthorization(true);
		DSpace dspace = new DSpace();
        ApplicationService applicationService = dspace.getServiceManager()
                .getServiceByName("applicationService",
				ApplicationService.class);

		// leggo tutte le righe e mi creo una struttura dati ad hoc per
		// accogliere definizioni dei metadati di primo livello e inizializzo la
		// struttura per i nested
		WorkbookSettings ws = new WorkbookSettings();
		ws.setEncoding("Cp1252");

		Workbook workbook = Workbook.getWorkbook(new File(fileExcel), ws);

		// target , lista altri metadati
		Map<String, List<List<String>>> widgetMap = new HashMap<String, List<List<String>>>();
		Map<String, List<List<String>>> nestedMap = new HashMap<String, List<List<String>>>();
        Map<String, List<List<String>>> tabMap = new HashMap<String, List<List<String>>>();
        Map<String, List<List<String>>> etabMap = new HashMap<String, List<List<String>>>();
        Map<String, List<List<String>>> boxMap = new HashMap<String, List<List<String>>>();
        Map<String, List<List<String>>> tab2boxMap = new HashMap<String, List<List<String>>>();
        Map<String, List<List<String>>> etab2boxMap = new HashMap<String, List<List<String>>>();
        Map<String, List<List<String>>> box2metadataMap = new HashMap<String, List<List<String>>>();
        Map<String, List<List<String>>> tab2Policies = new HashMap<String, List<List<String>>>();
        Map<String, List<List<String>>> etab2Policies = new HashMap<String, List<List<String>>>();
        Map<String, List<List<String>>> box2Policies= new HashMap<String, List<List<String>>>();
        
		Map<String, List<String>> controlledListMap = new HashMap<String, List<String>>();

        PlatformTransactionManager transactionManager = (PlatformTransactionManager) dspace
                .getServiceManager().getServiceByName("transactionManager",
                        HibernateTransactionManager.class);
		DefaultTransactionAttribute transactionAttribute = new DefaultTransactionAttribute(
				TransactionDefinition.PROPAGATION_REQUIRED);
		//
		// transactionAttribute
		// .setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
        TransactionStatus status = transactionManager
                .getTransaction(transactionAttribute);
		boolean success = false;
        try
        {
			buildMap(workbook, "propertiesdefinition", widgetMap, 0);
			buildMap(workbook, "nesteddefinition", nestedMap, 10);
            buildMap(workbook, "tab", tabMap, 0);
            buildMap(workbook, "etab", etabMap, 0);
            buildMap(workbook, "box", boxMap, 0);
            buildMap(workbook, "tab2box", tab2boxMap, 0);
            buildMap(workbook, "etab2box", etab2boxMap, 0);
            buildMap(workbook, "box2metadata", box2metadataMap, 0);
            buildMap(workbook, "tabpolicy", tab2Policies, 0);
            buildMap(workbook, "etabpolicy", etab2Policies, 0);
            buildMap(workbook, "boxpolicy", box2Policies, 0);
            
			buildControlledList(workbook, "controlledlist", controlledListMap);

			buildResearchObject(workbook, "utilsdata", applicationService, transactionManager, status);
			
            for (String key : widgetMap.keySet())
            {
				List<List<String>> meta = widgetMap.get(key);
                if (key.equals("rp"))
                {
                    build(applicationService, meta, nestedMap,
                            RPPropertiesDefinition.class,
                            DecoratorRPPropertiesDefinition.class,
                            RPNestedPropertiesDefinition.class,
                            DecoratorRPNestedPropertiesDefinition.class,
                            RPTypeNestedObject.class,
							DecoratorRPTypeNested.class, controlledListMap);
                }
                else if (key.equals("pj"))
                {
                    build(applicationService, meta, nestedMap,
                            ProjectPropertiesDefinition.class,
                            DecoratorProjectPropertiesDefinition.class,
                            ProjectNestedPropertiesDefinition.class,
                            DecoratorProjectNestedPropertiesDefinition.class,
                            ProjectTypeNestedObject.class,
                            DecoratorProjectTypeNested.class,
                            controlledListMap);
                }
                else if (key.equals("ou"))
                {
                    build(applicationService, meta, nestedMap,
                            OUPropertiesDefinition.class,
                            DecoratorOUPropertiesDefinition.class,
                            OUNestedPropertiesDefinition.class,
                            DecoratorOUNestedPropertiesDefinition.class,
                            OUTypeNestedObject.class,
							DecoratorOUTypeNested.class, controlledListMap);
                }
                else
                {
					ResultObject<DynamicPropertiesDefinition, DynamicNestedPropertiesDefinition, DynamicTypeNestedObject> result = build(
                            applicationService, meta, nestedMap,
                            DynamicPropertiesDefinition.class,
                            DecoratorDynamicPropertiesDefinition.class,
                            DynamicNestedPropertiesDefinition.class,
                            DecoratorDynamicNestedPropertiesDefinition.class,
                            DynamicTypeNestedObject.class,
                            DecoratorDynamicTypeNested.class,
                            controlledListMap);
                    DynamicObjectType dtp = applicationService
                            .findTypoByShortName(DynamicObjectType.class, key);
                    if (dtp == null)
                    {
                        throw new RuntimeException(
                                "DynamicObjectType with shortname:" + key
                                        + " not found");
					}

                    if (result.getTPtoPDEF() != null
                            && !result.getTPtoPDEF().isEmpty())
                    {
                        if (dtp.getMask() != null && !dtp.getMask().isEmpty())
                        {
							result.getTPtoPDEF().addAll(dtp.getMask());
						}
						dtp.setMask(result.getTPtoPDEF());

					}

                    if (result.getTPtoNOTP() != null
                            && !result.getTPtoNOTP().isEmpty())
                    {
                        if (dtp.getTypeNestedDefinitionMask() != null
                                && !dtp.getTypeNestedDefinitionMask().isEmpty())
                        {
                            result.getTPtoNOTP()
                                    .addAll(dtp.getTypeNestedDefinitionMask());
						}
						dtp.setTypeNestedDefinitionMask(result.getTPtoNOTP());
					}

                    applicationService.saveOrUpdate(DynamicObjectType.class,
                            dtp);
				}
			}

            for (String key : tabMap.keySet())
            {
                List<List<String>> rows = tabMap.get(key);
                List<List<String>> policies = tab2Policies.get(key);
                if (key.equals("rp"))
                {
                    buildTab(applicationService, rows, policies, TabResearcherPage.class, RPPropertiesDefinition.class);
                }
                else if (key.equals("pj"))
                {
                    buildTab(applicationService, rows, policies, TabProject.class, ProjectPropertiesDefinition.class);
                }
                else if (key.equals("ou"))
                {
                    buildTab(applicationService, rows, policies, 
                            TabOrganizationUnit.class, OUPropertiesDefinition.class);
                }
                else
                {
                    buildTab(applicationService, rows, policies, TabDynamicObject.class, DynamicPropertiesDefinition.class);
                }
            }
            for (String key : etabMap.keySet())
            {
                List<List<String>> rows = etabMap.get(key);
                List<List<String>> policies = etab2Policies.get(key);
                if (key.equals("rp"))
                { 
                    buildTab(applicationService, rows, policies,
                            EditTabResearcherPage.class, RPPropertiesDefinition.class);
                }
                else if (key.equals("pj"))
                {
                    buildTab(applicationService, rows, policies, EditTabProject.class, ProjectPropertiesDefinition.class);
                }
                else if (key.equals("ou"))
                {
                    buildTab(applicationService, rows, policies,
                            EditTabOrganizationUnit.class, OUPropertiesDefinition.class);
                }
                else
                {
                    buildTab(applicationService, rows, policies,
                            EditTabDynamicObject.class, DynamicPropertiesDefinition.class);
                }
            }

            for (String key : boxMap.keySet())
            {
                List<List<String>> rows = boxMap.get(key);
                List<List<String>> policies = box2Policies.get(key);
                if (key.equals("rp"))
                {
                    buildBox(applicationService, rows, policies, BoxResearcherPage.class, RPPropertiesDefinition.class);
                }
                else if (key.equals("pj"))
                {
                    buildBox(applicationService, rows, policies, BoxProject.class, ProjectPropertiesDefinition.class);
                }
                else if (key.equals("ou"))
                {
                    buildBox(applicationService, rows, policies,
                            BoxOrganizationUnit.class, OUPropertiesDefinition.class);
                }
                else
                {
                    buildBox(applicationService, rows, policies, BoxDynamicObject.class, DynamicPropertiesDefinition.class);
                }
            }

            for (String key : box2metadataMap.keySet())
            {
                List<List<String>> rows = box2metadataMap.get(key);
                if (key.equals("rp"))
                {
                    buildBoxToMetadata(applicationService, rows,
                            BoxResearcherPage.class,
                            DecoratorRPPropertiesDefinition.class,
                            DecoratorRPTypeNested.class,
                            RPPropertiesDefinition.class);
                }
                else if (key.equals("pj"))
                {
                    buildBoxToMetadata(applicationService, rows,
                            BoxProject.class,
                            DecoratorProjectPropertiesDefinition.class,
                            DecoratorProjectTypeNested.class,
                            ProjectPropertiesDefinition.class);
                }
                else if (key.equals("ou"))
                {
                    buildBoxToMetadata(applicationService, rows,
                            BoxOrganizationUnit.class,
                            DecoratorOUPropertiesDefinition.class,
                            DecoratorOUTypeNested.class,
                            OUPropertiesDefinition.class);
                }
                else
                {
                    buildBoxToMetadata(applicationService, rows,
                            BoxDynamicObject.class,
                            DecoratorDynamicPropertiesDefinition.class,
                            DecoratorDynamicTypeNested.class,
                            DynamicPropertiesDefinition.class);
                }
            }

            for (String key : tab2boxMap.keySet())
            {
                List<List<String>> rows = tab2boxMap.get(key);
                if (key.equals("rp"))
                {
                    buildTabToBox(applicationService, rows,
                            TabResearcherPage.class, BoxResearcherPage.class);
                }
                else if (key.equals("pj"))
                {
                    buildTabToBox(applicationService, rows, TabProject.class,
                            BoxProject.class);
                }
                else if (key.equals("ou"))
                {
                    buildTabToBox(applicationService, rows,
                            TabOrganizationUnit.class,
                            BoxOrganizationUnit.class);
                }
                else
                {
                    buildTabToBox(applicationService, rows,
                            TabDynamicObject.class, BoxDynamicObject.class);
                }
            }

            for (String key : etab2boxMap.keySet())
            {
                List<List<String>> rows = etab2boxMap.get(key);
                if (key.equals("rp"))
                {
                    buildTabToBox(applicationService, rows,
                            EditTabResearcherPage.class,
                            BoxResearcherPage.class);
                }
                else if (key.equals("pj"))
                {
                    buildTabToBox(applicationService, rows,
                            EditTabProject.class, BoxProject.class);
                }
                else if (key.equals("ou"))
                {
                    buildTabToBox(applicationService, rows,
                            EditTabOrganizationUnit.class,
                            BoxOrganizationUnit.class);
                }
                else
                {
                    buildTabToBox(applicationService, rows,
                            EditTabDynamicObject.class, BoxDynamicObject.class);
                }
            }
            success = true;
        }
        finally
        {
            if (success)
            {
                transactionManager.commit(status);
            }
            else
            {
                transactionManager.rollback(status);
            }
        }

    }

    private static <H extends IPropertyHolder<Containable>, T extends Tab<H>> void buildTabToBox(
            ApplicationService applicationService, List<List<String>> rows,
            Class<T> tabClazz, Class<H> boxClazz)
    {

        List<String> tabCache = new ArrayList<String>();
        for (List<String> row : rows)
        {

            String tabShortname = row.get(1);
            String boxShortname = row.get(2);

            T tab = applicationService.getTabByShortName(tabClazz,
                    tabShortname);
            H box = applicationService.getBoxByShortName(boxClazz,
                    boxShortname);

            if (box != null && tab != null)
            {
                if (!append)
                {
                    if (!tabCache.contains(tabShortname))
                    {
                        tabCache.add(tabShortname);
                        tab.getMask().clear();
                        tab.setMask(null);
			}
                }

                tab.getMask().add(box);
                applicationService.saveOrUpdate(tabClazz, tab);
            }
        }
    }

    private static <D extends ADecoratorPropertiesDefinition<TP>, TP extends PropertiesDefinition, NPD extends ANestedPropertiesDefinition, DNPD extends ADecoratorPropertiesDefinition<NPD>, ATNO extends ATypeNestedObject<NPD>, DATNO extends ADecoratorTypeDefinition<ATNO, NPD>, H extends IPropertyHolder<Containable>> void buildBoxToMetadata(
            ApplicationService applicationService, List<List<String>> rows,
            Class<H> boxClazz, Class<D> decoratorClazz,
            Class<DATNO> ndecoratorClazz, Class<TP> pdefClazz)
    {

        List<String> boxCache = new ArrayList<String>();
        for (List<String> row : rows)
        {

            String boxShortname = row.get(1);
            String pdefShortname = row.get(2);

            H box = applicationService.getBoxByShortName(boxClazz,
                    boxShortname);

            if (box != null)
            {

                if (!append)
                {
                    if (!boxCache.contains(boxShortname))
                    {
                        boxCache.add(boxShortname);
                        box.getMask().clear();
                        box.setMask(null);
                    }
                }

                Containable containable = (Containable) applicationService
                        .findContainableByDecorable(decoratorClazz,
                                pdefShortname);
                if (containable == null)
                {
                    containable = (Containable) applicationService
                            .findContainableByDecorable(ndecoratorClazz,
                                    pdefShortname);
                }
                box.getMask().add(containable);
                applicationService.saveOrUpdate(boxClazz, box);
			}
		}

	}

    private static <H extends IPropertyHolder<Containable>, T extends Tab<H>, PD extends PropertiesDefinition> void buildTab(
            ApplicationService applicationService, List<List<String>> rows, List<List<String>> policies,
            Class<T> clazzTab, Class<PD> clazzPD)
                    throws InstantiationException, IllegalAccessException
    {
        for (List<String> row : rows)
        {
            String shortName = row.get(1);
            String label = row.get(2);
            boolean mandatory = row.get(3).equals("y") ? true : false;
            String priority = row.get(4);
            Integer accessLevel = VisibilityTabConstant.ADMIN;
            String tmpAccessLevel = row.get(5);
            if (tmpAccessLevel.equals("STANDARD_ACCESS"))
            {
                accessLevel = VisibilityTabConstant.STANDARD;
            }
            else if (tmpAccessLevel.equals("HIGH_ACCESS"))
            {
                accessLevel = VisibilityTabConstant.HIGH;
            }
            else if (tmpAccessLevel.equals("LOW_ACCESS"))
            {
                accessLevel = VisibilityTabConstant.LOW;
            }
            else if (tmpAccessLevel.equals("POLICY_ACCESS"))
            {
                accessLevel = VisibilityTabConstant.POLICY;
            }
            String ext = row.get(6);
            String mime = row.get(7);

            T tabRP = applicationService.getTabByShortName(clazzTab, shortName);

            if (tabRP == null)
            {
                tabRP = clazzTab.newInstance();
                tabRP.setShortName(shortName);

            }
            if (TabDynamicObject.class.isAssignableFrom(clazzTab))
            {
                TabDynamicObject tabDynamicObject = (TabDynamicObject) tabRP;
                tabDynamicObject
                        .setTypeDef(applicationService.findTypoByShortName(
                                DynamicObjectType.class, row.get(0)));
            }
            if (EditTabDynamicObject.class.isAssignableFrom(clazzTab))
            {
                EditTabDynamicObject tabDynamicObject = (EditTabDynamicObject) tabRP;
                tabDynamicObject
                        .setTypeDef(applicationService.findTypoByShortName(
                                DynamicObjectType.class, row.get(0)));
            }
            internalUpdateTab(applicationService, policies, clazzTab, row,
                    shortName, label, mandatory, priority, accessLevel, ext,
                    mime, tabRP, clazzPD);
        }
    }

    private static <H extends IPropertyHolder<Containable>, T extends Tab<H>, PD extends PropertiesDefinition> void internalUpdateTab(
            ApplicationService applicationService, List<List<String>> policies,
            Class<T> clazzTab, List<String> row, String shortName, String label,
            boolean mandatory, String priority, Integer accessLevel, String ext,
            String mime, T tabRP, Class<PD> clazzPD)
    {
        tabRP.setExt(ext);
        tabRP.setMime(mime);
        tabRP.setMandatory(mandatory);
        tabRP.setPriority(Integer.parseInt(priority));
        tabRP.setTitle(label);
        tabRP.setVisibility(accessLevel);
        if(VisibilityTabConstant.POLICY.equals(tabRP.getVisibility())) {
            tabRP.getAuthorizedGroup().clear();
            tabRP.getAuthorizedSingle().clear();
            if(policies!=null && !policies.isEmpty()) {
                for(List<String> lpolicy : policies) {                      
                    if("group".equals(lpolicy.get(3))) {  
                        if(shortName.equals(lpolicy.get(1))) {    
                            tabRP.getAuthorizedGroup().add(applicationService.findPropertiesDefinitionByShortName(clazzPD, lpolicy.get(2)));    
                        }
                    } else {
                        if("eperson".equals(lpolicy.get(3))) {  
                            if(shortName.equals(lpolicy.get(1))) {    
                                tabRP.getAuthorizedSingle().add(applicationService.findPropertiesDefinitionByShortName(clazzPD, lpolicy.get(2)));    
                            }
                        }    
                    }
                }
            }
        }
        applicationService.saveOrUpdate(clazzTab, tabRP);
    }

    private static <H extends Box<Containable>, PD extends PropertiesDefinition> void buildBox(
            ApplicationService applicationService, List<List<String>> rows, List<List<String>> policies,
            Class<H> clazzBox, Class<PD> clazzPD)
                    throws InstantiationException, IllegalAccessException
    {
        for (List<String> row : rows)
        {
            boolean collapse = row.get(1).equals("y") ? true : false;
            String externaljsp = row.get(2);
            String priority = row.get(3);

            String shortname = row.get(4);
            String label = row.get(5);

            boolean unrelevant = row.get(6).equals("y") ? true : false;
            Integer accessLevel = AccessLevelConstants.ADMIN_ACCESS;
            String tmpAccessLevel = row.get(7);
            if (tmpAccessLevel.equals("STANDARD_ACCESS"))
            {
                accessLevel = VisibilityTabConstant.STANDARD;
            }
            else if (tmpAccessLevel.equals("HIGH_ACCESS"))
            {
                accessLevel = VisibilityTabConstant.HIGH;
            }
            else if (tmpAccessLevel.equals("LOW_ACCESS"))
            {
                accessLevel = VisibilityTabConstant.LOW;
            }
            else if (tmpAccessLevel.equals("POLICY_ACCESS"))
            {
                accessLevel = VisibilityTabConstant.POLICY;
            }
            H box = applicationService.getBoxByShortName(clazzBox, shortname);

            if (box == null)
            {
                box = clazzBox.newInstance();
                box.setShortName(shortname);
            }
            
            internalUpdateBox(applicationService, policies, clazzBox, row,
                    collapse, externaljsp, priority, shortname, label,
                    unrelevant, accessLevel, box, clazzPD);
        }
    }

    private static <H extends Box<Containable>, PD extends PropertiesDefinition> void internalUpdateBox(
            ApplicationService applicationService, List<List<String>> policies,
            Class<H> clazzBox, List<String> row, boolean collapse,
            String externaljsp, String priority, String shortName, String label,
            boolean unrelevant, Integer accessLevel, H box, Class<PD> clazzPD)
    {
        box.setCollapsed(collapse);
        box.setExternalJSP(externaljsp);
        box.setPriority(Integer.parseInt(priority));
        box.setTitle(label);
        box.setUnrelevant(unrelevant);
        box.setVisibility(accessLevel);
        if(BoxDynamicObject.class.isAssignableFrom(clazzBox)) {
            BoxDynamicObject boxDynamicObject = (BoxDynamicObject)box;
            boxDynamicObject.setTypeDef(applicationService.findTypoByShortName(DynamicObjectType.class, row.get(0)));
        }
        if(VisibilityTabConstant.POLICY.equals(box.getVisibility())) {
            box.getAuthorizedGroup().clear();
            box.getAuthorizedSingle().clear();
            for(List<String> lpolicy : policies) {                      
                if("group".equals(lpolicy.get(3))) {  
                    if(shortName.equals(lpolicy.get(1))) {    
                        box.getAuthorizedGroup().add(applicationService.findPropertiesDefinitionByShortName(clazzPD, lpolicy.get(2)));    
                    }
                } else {
                    if("eperson".equals(lpolicy.get(3))) {  
                        if(shortName.equals(lpolicy.get(1))) {    
                            box.getAuthorizedSingle().add(applicationService.findPropertiesDefinitionByShortName(clazzPD, lpolicy.get(2)));    
                        }
                    }    
                }
            }
        }
        applicationService.saveOrUpdate(clazzBox, box);
    }

	private static void buildResearchObject(Workbook workbook, String sheetName,
            ApplicationService applicationService, PlatformTransactionManager transactionManager, TransactionStatus status)
    {
		Cell[] riga;
		Sheet sheet = workbook.getSheet(sheetName);
		int indexRiga = 1;
		int rows = sheet.getColumn(2).length;
		
		boolean commitTransaction = false;
        while (indexRiga < rows)
        {
			riga = sheet.getRow(indexRiga);
			String key = riga[2].getContents().trim();
            if (key.equals("rp") || key.equals("pj") || key.equals("ou")
                    || key.equals("###"))
            {
				indexRiga++;
				continue;
			}
            DynamicObjectType dtp = applicationService
                    .findTypoByShortName(DynamicObjectType.class, key);
            if (dtp == null)
            {
				dtp = new DynamicObjectType();
				dtp.setShortName(key);
				dtp.setLabel(key);
				System.out.println("Build research object - " + key);
				applicationService.saveOrUpdate(DynamicObjectType.class, dtp);
				commitTransaction = true;
            }
            else
            {
                System.out.println(
                        "Research object already founded- skip " + key);
			}

			indexRiga++;

		}
        if(commitTransaction) {
            transactionManager.commit(status);
            System.out.println("Please relaunch the script... founded a transaction commit operation to save Research Object type... now you can continue");
            System.exit(1);
        }
	}

	private static <PD extends PropertiesDefinition, DPD extends ADecoratorPropertiesDefinition<PD>, NPD extends ANestedPropertiesDefinition, DNPD extends ADecoratorPropertiesDefinition<NPD>, ATNO extends ATypeNestedObject<NPD>, DATNO extends ADecoratorTypeDefinition<ATNO, NPD>> ResultObject build(
            ApplicationService applicationService, List<List<String>> meta,
            Map<String, List<List<String>>> nestedMap, Class<PD> classPD,
            Class<DPD> classDPD, Class<NPD> classNPD, Class<DNPD> classDNPD,
            Class<ATNO> classATNO, Class<DATNO> classDATNO,
            Map<String, List<String>> controlledListMap)
    {

		ResultObject<PD, NPD, ATNO> result = new ResultObject<PD, NPD, ATNO>();
        for (List<String> list : meta)
        {
			String target = list.get(0);
			String shortName = list.get(1);
			String label = list.get(2);
			boolean repeatable = list.get(3).equals("y") ? true : false;
			String priority = list.get(4);
			String help = list.get(5);
			Integer accessLevel = AccessLevelConstants.ADMIN_ACCESS;
			String tmpAccessLevel = list.get(6);
            if (tmpAccessLevel.equals("STANDARD_ACCESS"))
            {
				accessLevel = AccessLevelConstants.STANDARD_ACCESS;
            }
            else if (tmpAccessLevel.equals("HIGH_ACCESS"))
            {
				accessLevel = AccessLevelConstants.HIGH_ACCESS;
            }
            else if (tmpAccessLevel.equals("LOW_ACCESS"))
            {
				accessLevel = AccessLevelConstants.LOW_ACCESS;
			}
			boolean mandatory = list.get(7).equals("y") ? true : false;
			String widget = list.get(8);

            if (widget.equals("nested"))
            {
				String tmpSN = shortName;
                if (DynamicTypeNestedObject.class.isAssignableFrom(classATNO))
                {
                	if(!shortName.startsWith(target)) {                		
                		tmpSN = target + shortName;
                	}
				}
                ATNO atno = applicationService.findTypoByShortName(classATNO,
                        tmpSN);
                if (atno != null)
                {
                    System.out
                            .println("Nested definition already founded - skip "
                                    + target + "/" + tmpSN);
                    
                    List<List<String>> nestedPropertiesDefinition = nestedMap
                            .get(shortName);
                    if (nestedPropertiesDefinition != null)
                    {
                        GenericXmlApplicationContext subctx = new GenericXmlApplicationContext();
                        for (List<String> nestedSingleRow : nestedPropertiesDefinition)
                        {
                            String checkNestedMetadata = nestedSingleRow.get(1);
                            NPD real =  applicationService.findPropertiesDefinitionByShortName(classNPD, checkNestedMetadata);
                            if(real!=null) {
                                System.out
                                .println("Nested property definition already founded - skip "
                                        + target + "/" + tmpSN + "/" + checkNestedMetadata);
                                continue;
                            }
                            else {
                                System.out.println("New nested property definition  " + target + "/" + tmpSN + "/" + checkNestedMetadata);
                                DNPD decorator = createDecorator(applicationService,
                                        nestedSingleRow, subctx, classNPD, classDNPD,
                                        controlledListMap, true);
                                System.out.println("End write  " + target + "/" + tmpSN + "/" + decorator.getShortName());                                
                            }
                        }
                        subctx.destroy();
                    }
					continue;
				}
            }
            else
            {
				String tmpSN = shortName;
                if (DynamicPropertiesDefinition.class.isAssignableFrom(classPD))
                {
                	if(!shortName.startsWith(target)) {                		
                		tmpSN = target + shortName;
                	}
				}
                PD pdef = applicationService
                        .findPropertiesDefinitionByShortName(classPD, tmpSN);
                if (pdef != null)
                {
                    System.out.println(
                            "Metadata definition already founded - skip "
                                    + target + "/" + tmpSN);
					continue;
				}
			}

            int fieldIndex = 10;
//            if (widget.equals("nested"))  fieldIndex=11; else fieldIndex=10;
            
            
            String displayFormat = list.get(fieldIndex++);

            String labelSize = list.get(fieldIndex++);
            Integer ilabelSize = null;
            if (StringUtils.isNotBlank(labelSize))
            {
                ilabelSize = Integer.parseInt(labelSize);
            }

            String fieldWidth = list.get(fieldIndex++);
            Integer ifieldWidth = null;
            if (StringUtils.isNotBlank(fieldWidth))
            {
                ifieldWidth = Integer.parseInt(fieldWidth);
            }

            String fieldHeight = list.get(fieldIndex++);
            Integer ifieldHeight = null;
            if (StringUtils.isNotBlank(fieldHeight))
            {
                ifieldHeight = Integer.parseInt(fieldHeight);
            }

            String newLine = list.get(fieldIndex++);
            Boolean bnewLine = false;
            if (StringUtils.isNotBlank(newLine))
            {
                bnewLine = newLine.equals("y") ? true : false;
            }
            
            int rows=1;
            String rowsStr = list.get(fieldIndex++);
            if(StringUtils.isNotBlank(rowsStr)) {
            	Integer row = Integer.parseInt(rowsStr);
            	rows = row >0 ? row.intValue(): rows;
            }
            boolean multiline = rows>1 ? true: false;
            
            int cols=30;
            String colsStr = list.get(fieldIndex++);
            if(StringUtils.isNotBlank(colsStr)) {
            	Integer col = Integer.parseInt(colsStr);
            	cols = col >0 ? col.intValue(): cols;
            }
            
			System.out.println("Writing  " + target + "/" + shortName);

			GenericXmlApplicationContext ctx = new GenericXmlApplicationContext();

            if (widget.equals("nested"))
            {

                BeanDefinitionBuilder builderNTP = BeanDefinitionBuilder
                        .genericBeanDefinition(classATNO);
				builderNTP.getBeanDefinition().setAttribute("id", shortName);
                if (DynamicPropertiesDefinition.class.isAssignableFrom(classPD))
                {
                	if(!shortName.startsWith(target)) {                		
                		shortName = target + shortName;
                	}
				}
				builderNTP.addPropertyValue("shortName", shortName);
				builderNTP.addPropertyValue("label", label);
				builderNTP.addPropertyValue("repeatable", repeatable);
				builderNTP.addPropertyValue("mandatory", mandatory);
				builderNTP.addPropertyValue("priority", priority);
				builderNTP.addPropertyValue("help", help);
				builderNTP.addPropertyValue("accessLevel", accessLevel);
                if (ilabelSize != null)
                {
                    builderNTP.addPropertyValue("labelMinSize", ilabelSize);
                }
                if (ifieldWidth != null)
                {
                    builderNTP.addPropertyValue("fieldMinSize.col",
                            ifieldWidth);
                }
                if (ifieldHeight != null)
                {
                    builderNTP.addPropertyValue("fieldMinSize.row",
                            ifieldHeight);
                }
                builderNTP.addPropertyValue("newline", bnewLine);

                ctx.registerBeanDefinition(shortName,
                        builderNTP.getBeanDefinition());

                BeanDefinitionBuilder builderDNTP = BeanDefinitionBuilder
                        .genericBeanDefinition(classDATNO);
				builderDNTP.addPropertyReference("real", shortName);
				String decoratorShortName = "decorator" + shortName;
                ctx.registerBeanDefinition(decoratorShortName,
                        builderDNTP.getBeanDefinition());

				DATNO dntp = ctx.getBean(classDATNO);				
				applicationService.saveOrUpdate(classDATNO, dntp);

                List<List<String>> nestedPropertiesDefinition = nestedMap
                        .get(shortName);
                if (nestedPropertiesDefinition != null)
                {
                    for (List<String> nestedSingleRow : nestedPropertiesDefinition)
                    {
                        DNPD decorator = createDecorator(applicationService,
                                nestedSingleRow, ctx, classNPD, classDNPD,
                                controlledListMap, true);
                        dntp.getReal().getMask().add(decorator.getReal());
                    }
				
                }
                
                result.getTPtoNOTP().add(dntp.getReal());
            }
            else
            {
                DPD decorator = createDecorator(applicationService, list, ctx,
                        classPD, classDPD, controlledListMap, false);
				result.getTPtoPDEF().add(decorator.getReal());
			}
			System.out.println("End write  " + target + "/" + shortName);
			ctx.destroy();
		}
		return result;

	}

	private static <PD extends PropertiesDefinition, DPD extends ADecoratorPropertiesDefinition<PD>> DPD createDecorator(
            ApplicationService applicationService, List<String> metadata,
            GenericXmlApplicationContext ctx, Class<PD> classPD,
            Class<DPD> classDPD, Map<String, List<String>> controlledListMap,
            boolean reserved)
    {
		String target = metadata.get(0);
		String shortName = metadata.get(1);
		String label = metadata.get(2);
		boolean repeatable = metadata.get(3).equals("y") ? true : false;
		String priority = metadata.get(4);
		String help = metadata.get(5);
		Integer accessLevel = AccessLevelConstants.ADMIN_ACCESS;
		String tmpAccessLevel = metadata.get(6);
        if (tmpAccessLevel.equals("STANDARD_ACCESS"))
        {
			accessLevel = AccessLevelConstants.STANDARD_ACCESS;
        }
        else if (tmpAccessLevel.equals("HIGH_ACCESS"))
        {
			accessLevel = AccessLevelConstants.HIGH_ACCESS;
        }
        else if (tmpAccessLevel.equals("LOW_ACCESS"))
        {
			accessLevel = AccessLevelConstants.LOW_ACCESS;
		}

		boolean mandatory = metadata.get(7).equals("y") ? true : false;
		String widget = metadata.get(8);
		String pointer = metadata.get(9);
        if (DynamicPropertiesDefinition.class.isAssignableFrom(classPD))
        {
        	if(!shortName.startsWith(target)) {
        		shortName = target + shortName;
        	}
		}

        String labelSize = "";
        Integer ilabelSize = null;
        String renderingText = "";
        Integer ifieldWidth = null;
        Integer ifieldHeight = null;
        Boolean bnewLine = false;
        Boolean multilinea=false;
        Integer rows= 1;
        Integer cols= 30;
        
        if (reserved)
        {
            String reservedCell = metadata.get(10);
            
            renderingText = metadata.get(11);
            
            labelSize = metadata.get(12);
            if (StringUtils.isNotBlank(labelSize))
            {
                ilabelSize = Integer.parseInt(labelSize);
            }

            String fieldWidth = metadata.get(13);
            if (StringUtils.isNotBlank(fieldWidth))
            {
                ifieldWidth = Integer.parseInt(fieldWidth);
            }

            String fieldHeight = metadata.get(14);

            if (StringUtils.isNotBlank(fieldHeight))
            {
                ifieldHeight = Integer.parseInt(fieldHeight);
            }

            String newLine = metadata.get(15);
            
            if (StringUtils.isNotBlank(newLine))
            {
                bnewLine = newLine.equals("y") ? true : false;
            }
            
            String rowsStr = metadata.get(16);
            if(StringUtils.isNotBlank(rowsStr)) {
            	Integer r = Integer.parseInt(rowsStr);
            	if(r>1) {
            		rows = r.intValue();
            		multilinea = true;
            	}
            }
            
            String colsStr = metadata.get(17);
            if(StringUtils.isNotBlank(colsStr)) {
            	Integer c = Integer.parseInt(colsStr);
            	if(c>1) {
            		cols = c.intValue();
            	}
            }
        }
        else {
            renderingText = metadata.get(10);
            
            labelSize = metadata.get(11);
            if (StringUtils.isNotBlank(labelSize))
            {
                ilabelSize = Integer.parseInt(labelSize);
            }

            String fieldWidth = metadata.get(12);
            if (StringUtils.isNotBlank(fieldWidth))
            {
                ifieldWidth = Integer.parseInt(fieldWidth);
            }

            String fieldHeight = metadata.get(13);
            if (StringUtils.isNotBlank(fieldHeight))
            {
                ifieldHeight = Integer.parseInt(fieldHeight);
            }

            String newLine = metadata.get(14);
            if (StringUtils.isNotBlank(newLine))
            {
                bnewLine = newLine.equals("y") ? true : false;
            }
            String rowsStr = metadata.get(15);
            if(StringUtils.isNotBlank(rowsStr)) {
            	Integer r = Integer.parseInt(rowsStr);
            	if(r>1) {
            		rows = r.intValue();
            		multilinea = true;
            	}
            }
            String colsStr = metadata.get(16);
            if(StringUtils.isNotBlank(colsStr)) {
            	Integer c = Integer.parseInt(colsStr);
            	if(c>1) {
            		cols = c.intValue();
            	}
            }
            
        }
		BeanDefinitionBuilder builderW = null;

        if (widget.equals("text"))
        {
            builderW = BeanDefinitionBuilder
                    .genericBeanDefinition(WidgetTesto.class);
            if (StringUtils.isNotBlank(renderingText)) {
                builderW.addPropertyValue("displayFormat", renderingText);
            }
            
            if(multilinea) {
            	builderW.addPropertyValue("multilinea", true);
            }
            Size s = new Size();
            s.setCol(cols);
            s.setRow(rows);
            builderW.addPropertyValue("dimensione", s);
        }
        else if (widget.equals("boolean"))
        {
            builderW = BeanDefinitionBuilder
                    .genericBeanDefinition(WidgetBoolean.class);
        }
        else if (widget.equals("radio") || widget.equals("checkbox")
                || widget.equals("dropdown"))
        {
            builderW = BeanDefinitionBuilder
                    .genericBeanDefinition(WidgetCheckRadio.class);
			String staticValues = "";
            if (!controlledListMap.containsKey(shortName))
            {
				log.error("controlledlist not defined: "+shortName);
			}
            for (String ss : controlledListMap.get(shortName))
            {
				staticValues += ss+"|||";
			}
            builderW.addPropertyValue("staticValues",
                    staticValues.substring(0, staticValues.length() - 3));
            if (widget.equals("radio") || widget.equals("checkbox"))
            {
				builderW.addPropertyValue("option4row", 1);
                if (widget.equals("checkbox"))
                {
					repeatable = true;
				}
                else
                {
					repeatable = false;
				}
			}
            if (widget.equals("dropdown"))
            {
				builderW.addPropertyValue("dropdown", true);
			}
        }
        else if (widget.equals("link"))
        {
            builderW = BeanDefinitionBuilder
                    .genericBeanDefinition(WidgetLink.class);
        }
        else if (widget.equals("date"))
        {
            builderW = BeanDefinitionBuilder
                    .genericBeanDefinition(WidgetDate.class);
        }
        else if (widget.equals("image") || widget.equals("file"))
        {
            if (target.equals("rp"))
            {
                builderW = BeanDefinitionBuilder
                        .genericBeanDefinition(WidgetFileRP.class);
            }
            else if (target.equals("ou"))
            {
                builderW = BeanDefinitionBuilder
                        .genericBeanDefinition(WidgetFileOU.class);
            }
            else if (target.equals("pj"))
            {
                builderW = BeanDefinitionBuilder
                        .genericBeanDefinition(WidgetFileProject.class);
            }
            else
            {
                builderW = BeanDefinitionBuilder
                        .genericBeanDefinition(WidgetFileDO.class);
			}

            if (widget.equals("image"))
            {
				builderW.addPropertyValue("showPreview", true);
			}
        }
        else if (widget.equals("pointer"))
        {

            if (pointer.equals("rp"))
            {
                builderW = BeanDefinitionBuilder
                        .genericBeanDefinition(WidgetPointerRP.class);
                builderW.addPropertyValue("target",
                        "org.dspace.app.cris.model.jdyna.value.RPPointer");
            }
            else if (pointer.equals("ou"))
            {
                builderW = BeanDefinitionBuilder
                        .genericBeanDefinition(WidgetPointerOU.class);
                builderW.addPropertyValue("target",
                        "org.dspace.app.cris.model.jdyna.value.OUPointer");
            }
            else if (pointer.equals("pj"))
            {
                builderW = BeanDefinitionBuilder
                        .genericBeanDefinition(WidgetPointerPJ.class);
                builderW.addPropertyValue("target",
                        "org.dspace.app.cris.model.jdyna.value.ProjectPointer");
            }
            else
            {
                builderW = BeanDefinitionBuilder
                        .genericBeanDefinition(WidgetPointerDO.class);
                builderW.addPropertyValue("target",
                        "org.dspace.app.cris.model.jdyna.value.DOPointer");
				Integer tmpTP = CrisConstants.CRIS_DYNAMIC_TYPE_ID_START
                        + applicationService.findTypoByShortName(
                                DynamicObjectType.class, pointer).getId();
                builderW.addPropertyValue("filterExtended",
                        "search.resourcetype:" + tmpTP);
			}

			builderW.addPropertyValue("display", "${displayObject.name}");
            builderW.addPropertyValue("urlPath",
                    "cris/uuid/${displayObject.uuid}");
		}
        else if (widget.equals("eperson"))
        {
            builderW = BeanDefinitionBuilder
                    .genericBeanDefinition(WidgetEPerson.class);
        }
        else if (widget.equals("group"))
        {
            builderW = BeanDefinitionBuilder
                    .genericBeanDefinition(WidgetGroup.class);
        }
            
		builderW.getBeanDefinition().setAttribute("id", widget);
		String widgetName = "widget" + widget;
		ctx.registerBeanDefinition(widgetName, builderW.getBeanDefinition());

        BeanDefinitionBuilder builderPD = BeanDefinitionBuilder
                .genericBeanDefinition(classPD);
		builderPD.addPropertyReference("rendering", widgetName);
		builderPD.addPropertyValue("shortName", shortName);
		builderPD.addPropertyValue("label", label);
		builderPD.addPropertyValue("repeatable", repeatable);
		builderPD.addPropertyValue("mandatory", mandatory);
		builderPD.addPropertyValue("priority", priority);
		builderPD.addPropertyValue("help", help);
		builderPD.addPropertyValue("accessLevel", accessLevel);
        if (ilabelSize != null)
        {
            builderPD.addPropertyValue("labelMinSize", ilabelSize);
        }
        if (ifieldWidth != null)
        {
            builderPD.addPropertyValue("fieldMinSize.col", ifieldWidth);
        }
        if (ifieldHeight != null)
        {
            builderPD.addPropertyValue("fieldMinSize.row", ifieldHeight);
        }
        builderPD.addPropertyValue("newline", bnewLine);
		builderPD.getBeanDefinition().setAttribute("id", shortName);
		ctx.registerBeanDefinition(shortName, builderPD.getBeanDefinition());

        BeanDefinitionBuilder builderDecorator = BeanDefinitionBuilder
                .genericBeanDefinition(classDPD);
		builderDecorator.addPropertyReference("real", shortName);
		String decoratorShortName = "decorator" + shortName;
		builderPD.getBeanDefinition().setAttribute("id", decoratorShortName);
        ctx.registerBeanDefinition(decoratorShortName,
                builderDecorator.getBeanDefinition());

		DPD dtp = ctx.getBean(decoratorShortName, classDPD);
		applicationService.saveOrUpdate(classDPD, dtp);
		return dtp;
	}

    private static void buildMap(Workbook workbook, String sheetName,
            Map<String, List<List<String>>> widgetMap, int indexKey)
    {
		Cell[] riga;
		Sheet sheet = workbook.getSheet(sheetName);
		int indexRiga = 1;
		int rows = sheet.getColumn(0).length;

        while (indexRiga < rows)
        {
			riga = sheet.getRow(indexRiga);
			String key = riga[indexKey].getContents().trim();
			if("nesteddefinition".equals(sheetName)) {
				String prefix = riga[0].getContents().trim();
				if(!"rp".equals(prefix) && !"ou".equals(prefix) && !"pj".equals(prefix)) {
					if(!key.startsWith(prefix)) {
						key = prefix + key;
					}
				}
			}
			List<String> metadata = new ArrayList<String>();
			
			for(int i = 0; i<sheet.getColumns(); i++) {
                metadata.add(riga[i].getContents().trim());
			}
			insertInMap(widgetMap, key, metadata);

			indexRiga++;

		}
	}

	private static void buildControlledList(Workbook workbook, String sheetName,
            Map<String, List<String>> controlledListMap)
    {
		Cell row;
		Sheet sheet = workbook.getSheet(sheetName);		
		int indexColumn = 0;		
		int columns = sheet.getRow(0).length;
        while (indexColumn < columns)
        {
			int rows = sheet.getColumn(indexColumn).length;
			int indexRiga = 1;
			String header = sheet.getRow(0)[indexColumn].getContents().trim();
            while (indexRiga < rows)
            {
				row = sheet.getRow(indexRiga)[indexColumn];				
                insertInList(controlledListMap, header,
                        row.getContents().trim());
				indexRiga++;
			}
			indexColumn++;
		}
	}

    private static void insertInMap(Map<String, List<List<String>>> map,
            String target, List<String> metadata)
    {
        if (map.containsKey(target))
        {
			map.get(target).add(metadata);
        }
        else
        {
			List<List<String>> singleRows = new ArrayList<List<String>>();
			singleRows.add(metadata);
			map.put(target, singleRows);
		}
	}

    private static void insertInList(Map<String, List<String>> map,
            String target, String metadata)
    {
        if (map.containsKey(target))
        {
			map.get(target).add(metadata);
        }
        else
        {
			List<String> singleRows = new ArrayList<String>();
			singleRows.add(metadata);
			map.put(target, singleRows);
		}
	}
}

class ResultObject<PD extends PropertiesDefinition, NPD extends ANestedPropertiesDefinition, ATNO extends ATypeNestedObject<NPD>>
{
	List<ATNO> TPtoNOTP = new ArrayList<ATNO>();

	List<PD> TPtoPDEF = new ArrayList<PD>();

    public List<ATNO> getTPtoNOTP()
    {
		return TPtoNOTP;
	}

    public void setTPtoNOTP(List<ATNO> tPtoNOTP)
    {
		TPtoNOTP = tPtoNOTP;
	}

    public List<PD> getTPtoPDEF()
    {
		return TPtoPDEF;
	}

    public void setTPtoPDEF(List<PD> tPtoPDEF)
    {
		TPtoPDEF = tPtoPDEF;
	}

}
