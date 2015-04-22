/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.batch;

import it.cilea.osd.jdyna.model.ADecoratorPropertiesDefinition;
import it.cilea.osd.jdyna.model.ADecoratorTypeDefinition;
import it.cilea.osd.jdyna.model.ANestedPropertiesDefinition;
import it.cilea.osd.jdyna.model.ANestedProperty;
import it.cilea.osd.jdyna.model.ATypeNestedObject;
import it.cilea.osd.jdyna.model.AccessLevelConstants;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.model.Property;
import it.cilea.osd.jdyna.widget.WidgetDate;
import it.cilea.osd.jdyna.widget.WidgetLink;
import it.cilea.osd.jdyna.widget.WidgetTesto;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.read.biff.BiffException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.OrganizationUnit;
import org.dspace.app.cris.model.Project;
import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.jdyna.ACrisNestedObject;
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
import org.dspace.app.cris.model.jdyna.widget.WidgetFileDO;
import org.dspace.app.cris.model.jdyna.widget.WidgetFileOU;
import org.dspace.app.cris.model.jdyna.widget.WidgetFileProject;
import org.dspace.app.cris.model.jdyna.widget.WidgetFileRP;
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

public class CRISBatchCSV
{
	
	private final static String[] fixHeaderTopObject = new String[]{"action","ObjectType", "CRISID","UUID","SOURCEREF","SOURCEID"};
	
    private static Logger log = Logger
            .getLogger(CRISBatchCSV.class);

    private static ApplicationService applicationService;
    
    public static void main(String[] args) throws ParseException, SQLException,
            BiffException, IOException, InstantiationException, IllegalAccessException
    {
        String fileExcel = null;
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption("f", "excel", true, "File excel");
        options.addOption(
                "e",
                "encode",
                true,
                "Excel Encoding (default Cp1252)");

        CommandLine line = parser.parse(options, args);
        if (line.hasOption('h'))
        {
            HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp("StartupMetadataConfiguratorTool\n", options);
            System.exit(0);
        }

        String encoding = "Cp1252";
        if (line.hasOption('f'))
        {
            fileExcel = line.getOptionValue('f');
        }
        if (line.hasOption('e'))
        {
        	encoding = line.getOptionValue('e');
        }

        Context dspaceContext = new Context();
        dspaceContext.setIgnoreAuthorization(true);
        DSpace dspace = new DSpace();
        applicationService = dspace.getServiceManager()
                .getServiceByName("applicationService",
                        ApplicationService.class);

        // leggo tutte le righe e mi creo una struttura dati ad hoc per
        // accogliere definizioni dei metadati di primo livello e inizializzo la
        // struttura per i nested
        WorkbookSettings ws = new WorkbookSettings();
        ws.setEncoding(encoding);

        Workbook workbook = Workbook.getWorkbook(new File(fileExcel), ws);

        // target , lista altri metadati
        Map<String, List<List<String>>> widgetMap = new HashMap<String, List<List<String>>>();
        Map<String, List<List<String>>> nestedMap = new HashMap<String, List<List<String>>>();

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
            workTopObjects(workbook, "top_objects");
            workNestedObjects(workbook, "nested_objects");
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

    private static void workNestedObjects(Workbook workbook, String string) {
		// TODO Auto-generated method stub
		
	}

	private static <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> void workTopObjects(Workbook workbook, String sheetName) throws InstantiationException, IllegalAccessException {
		// TODO Auto-generated method stub
        Cell[] row;
        Sheet sheet = workbook.getSheet(sheetName);
        List<String> headers = new ArrayList<String>();
        int column = 0;
        row = sheet.getRow(0);
        while (true)
        {
        	String cellContent = StringUtils.trim(row[0].getContents());
        	if (StringUtils.isNotBlank(cellContent)) {
				headers.add(cellContent);
        		if (fixHeaderTopObject.length > column && !StringUtils.equalsIgnoreCase(cellContent, fixHeaderTopObject[column])) {
        			throw new IllegalArgumentException("Invalid excel file - unexpected header column " + column+ " -> " + cellContent + " expected "+fixHeaderTopObject[column]);		
        		}
        		column++;
        	}
        	else {
        		break;
        	}
        }
        
        if (headers.size() < 5) {
        	throw new IllegalArgumentException("Invalid excel file - unexpected header row: missing the required first 5 cells (action, CRISID, UUID, SOURCEREF, SOURCEID)");
        }
        
        int rowIndex = 1;
        int rowsNumber = sheet.getColumn(0).length;

        while (rowIndex < rowsNumber)
        {
        	
            row = sheet.getRow(rowIndex);
            ACrisObject<P, TP, NP, NTP, ACNO, ATNO> crisObject = getCrisObject(row);
            for (int columnIndex = fixHeaderTopObject.length; columnIndex < row.length; columnIndex++) {
            	String cellContent = StringUtils.trim(row[rowIndex].getContents());
            	String shortName = headers.get(columnIndex);
            	
            	
            	
            }
        }


//        build(applicationService, meta, nestedMap,
//                RPPropertiesDefinition.class,
//                DecoratorRPPropertiesDefinition.class,
//                RPNestedPropertiesDefinition.class,
//                DecoratorRPNestedPropertiesDefinition.class,
//                RPTypeNestedObject.class,
//                DecoratorRPTypeNested.class);
//        build(applicationService, meta, nestedMap,
//                ProjectPropertiesDefinition.class,
//                DecoratorProjectPropertiesDefinition.class,
//                ProjectNestedPropertiesDefinition.class,
//                DecoratorProjectNestedPropertiesDefinition.class,
//                ProjectTypeNestedObject.class,
//                DecoratorProjectTypeNested.class);
//        build(applicationService, meta, nestedMap,
//                OUPropertiesDefinition.class,
//                DecoratorOUPropertiesDefinition.class,
//                OUNestedPropertiesDefinition.class,
//                DecoratorOUNestedPropertiesDefinition.class,
//                OUTypeNestedObject.class,
//        ResultObject<DynamicPropertiesDefinition, DynamicNestedPropertiesDefinition, DynamicTypeNestedObject> result = build(
//                applicationService, meta, nestedMap,
//                DynamicPropertiesDefinition.class,
//                DecoratorDynamicPropertiesDefinition.class,
//                DynamicNestedPropertiesDefinition.class,
//                DecoratorDynamicNestedPropertiesDefinition.class,
//                DynamicTypeNestedObject.class,
//                DecoratorDynamicTypeNested.class);
//        DynamicObjectType dtp = applicationService
//                .findTypoByShortName(DynamicObjectType.class, key);
//        if (dtp == null)
//        {
//            throw new RuntimeException(
//                    "DynamicObjectType with shortname:" + key
//                            + " not found");
//        }
//
//        if (result.getTPtoPDEF() != null
//                && !result.getTPtoPDEF().isEmpty())
//        {
//            if (dtp.getMask() != null && !dtp.getMask().isEmpty())
//            {
//                result.getTPtoPDEF().addAll(dtp.getMask());
//            }
//            dtp.setMask(result.getTPtoPDEF());
//
//        }
//
//        if (result.getTPtoNOTP() != null
//                && !result.getTPtoNOTP().isEmpty())
//        {
//            if (dtp.getTypeNestedDefinitionMask() != null
//                    && !dtp.getTypeNestedDefinitionMask().isEmpty())
//            {
//                result.getTPtoNOTP().addAll(
//                        dtp.getTypeNestedDefinitionMask());
//            }
//            dtp.setTypeNestedDefinitionMask(result.getTPtoNOTP());
//        }
//
//        applicationService.saveOrUpdate(DynamicObjectType.class,
//                dtp);
//    }

		
	}

	private static <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, 
		ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> ACrisObject<P, TP, NP, NTP, ACNO, ATNO> getCrisObject(
			Cell[] row) throws InstantiationException, IllegalAccessException {
		String action = StringUtils.trim(row[0].getContents());
		String objectType = StringUtils.trim(row[1].getContents());
		String crisID = StringUtils.trim(row[2].getContents());
		String uuid = StringUtils.trim(row[3].getContents());
		String sourceRef = StringUtils.trim(row[4].getContents());
		String sourceId = StringUtils.trim(row[5].getContents());
		
		ACrisObject<P, TP, NP, NTP, ACNO, ATNO> crisObject = null;

		Class<? extends ACrisObject> objectTypeClass = ResearchObject.class;
		if (StringUtils.equalsIgnoreCase(objectType, "rp")) {
			objectTypeClass = ResearcherPage.class;
		}
		else if (StringUtils.equalsIgnoreCase(objectType, "ou")) {
			objectTypeClass = OrganizationUnit.class;
		}
		else if (StringUtils.equalsIgnoreCase(objectType, "pj")) {
			objectTypeClass = Project.class;
		}
		
		if (StringUtils.isBlank(crisID) && StringUtils.isBlank(uuid) && StringUtils.isBlank(sourceRef) && StringUtils.isBlank(sourceId)) {
			if (!StringUtils.equalsIgnoreCase("delete", action)) {
				crisObject = objectTypeClass.newInstance();
			}
		}
		else {
			if (StringUtils.isNotBlank(crisID)) {
				crisObject = applicationService.getEntityByCrisId(crisID, objectTypeClass);
			}
			
			if (crisObject == null && StringUtils.isNotBlank(uuid)) {
				crisObject = applicationService.getEntityByUUID(uuid);
			}
			
			if (crisObject == null && StringUtils.isNotBlank(sourceId)) {
				crisObject = applicationService.getEntityBySourceId(sourceRef, sourceId, objectTypeClass);
			}
		}
		
		if (StringUtils.isNotBlank(crisID)) {
			crisObject.setCrisID(crisID);
		}
		
		if (StringUtils.isNotBlank(uuid)) {
			crisObject.setUuid(uuid);
		}
		
		if (StringUtils.isNotBlank(sourceRef)) {
			crisObject.setSourceRef(sourceRef);
		}
		if (StringUtils.isNotBlank(sourceId)) {
			crisObject.setSourceID(sourceId);
		}
		return crisObject;
	}

	private static void buildResearchObject(Workbook workbook,
            String sheetName, ApplicationService applicationService)
    {
        Cell[] riga;
        Sheet sheet = workbook.getSheet(sheetName);
        int indexRiga = 1;
        int rows = sheet.getColumn(2).length;
        while (indexRiga < rows)
        {
            riga = sheet.getRow(indexRiga);
            String key = riga[2].getContents().trim();
            if (key.equals("rp") || key.equals("pj") || key.equals("ou")
                    || key.equals("n"))
            {
                indexRiga++;
                continue;
            }
            DynamicObjectType dtp = applicationService.findTypoByShortName(
                    DynamicObjectType.class, key);
            if (dtp == null)
            {
                dtp = new DynamicObjectType();
                dtp.setShortName(key);
                dtp.setLabel(key);
                System.out.println("Build research object - " + key);
                applicationService.saveOrUpdate(DynamicObjectType.class, dtp);
            }
            else
            {
                System.out.println("Research object already founded- skip "
                        + key);
            }

            indexRiga++;

        }

    }

    private static <PD extends PropertiesDefinition, DPD extends ADecoratorPropertiesDefinition<PD>, NPD extends ANestedPropertiesDefinition, DNPD extends ADecoratorPropertiesDefinition<NPD>, ATNO extends ATypeNestedObject<NPD>, DATNO extends ADecoratorTypeDefinition<ATNO, NPD>> ResultObject build(
            ApplicationService applicationService, List<List<String>> meta,
            Map<String, List<List<String>>> nestedMap, Class<PD> classPD,
            Class<DPD> classDPD, Class<NPD> classNPD, Class<DNPD> classDNPD,
            Class<ATNO> classATNO, Class<DATNO> classDATNO)
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
                    tmpSN = target + shortName;
                }
                ATNO atno = applicationService.findTypoByShortName(classATNO,
                        tmpSN);
                if (atno != null)
                {
                    System.out
                            .println("Nested definition already founded - skip "
                                    + target + "/" + tmpSN);
                    continue;
                }
            }
            else
            {
                String tmpSN = shortName;
                if (DynamicPropertiesDefinition.class.isAssignableFrom(classPD))
                {
                    tmpSN = target + shortName;
                }
                PD pdef = applicationService
                        .findPropertiesDefinitionByShortName(classPD, tmpSN);
                if (pdef != null)
                {
                    System.out
                            .println("Metadata definition already founded - skip "
                                    + target + "/" + tmpSN);
                    continue;
                }
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
                    shortName = target + shortName;
                }
                builderNTP.addPropertyValue("shortName", shortName);
                builderNTP.addPropertyValue("label", label);
                builderNTP.addPropertyValue("repeatable", repeatable);
                builderNTP.addPropertyValue("repeatable", repeatable);
                builderNTP.addPropertyValue("mandatory", mandatory);
                builderNTP.addPropertyValue("priority", priority);
                builderNTP.addPropertyValue("help", help);
                builderNTP.addPropertyValue("accessLevel", accessLevel);

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
                for (List<String> nestedSingleRow : nestedPropertiesDefinition)
                {
                    DNPD decorator = createDecorator(applicationService,
                            nestedSingleRow, ctx, classNPD, classDNPD);
                    dntp.getReal().getMask().add(decorator.getReal());
                    result.getTPtoNOTP().add(dntp.getReal());
                }
            }
            else
            {
                DPD decorator = createDecorator(applicationService, list, ctx,
                        classPD, classDPD);
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
            Class<DPD> classDPD)
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
            shortName = target + shortName;
        }

        BeanDefinitionBuilder builderW = null;

        if (widget.equals("text"))
        {
            builderW = BeanDefinitionBuilder
                    .genericBeanDefinition(WidgetTesto.class);
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

}