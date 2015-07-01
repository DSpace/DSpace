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
import org.apache.log4j.Logger;
import org.dspace.app.cris.model.CrisConstants;
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

import it.cilea.osd.jdyna.model.ADecoratorPropertiesDefinition;
import it.cilea.osd.jdyna.model.ADecoratorTypeDefinition;
import it.cilea.osd.jdyna.model.ANestedPropertiesDefinition;
import it.cilea.osd.jdyna.model.ATypeNestedObject;
import it.cilea.osd.jdyna.model.AccessLevelConstants;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
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

public class StartupMetadataConfiguratorTool {
	private static Logger log = Logger.getLogger(StartupMetadataConfiguratorTool.class);

	public static void main(String[] args) throws ParseException, SQLException, BiffException, IOException {
		String fileExcel = null;
		CommandLineParser parser = new PosixParser();
		Options options = new Options();
		options.addOption("f", "excel", true,
				"Excel file that you can use to create/update DSpace-CRIS properties definitions (metadata for all CRIS software entities; NOTE the script doesn't delete anything but works only in append mode so you can use this option many times)");
		options.addOption("t", "tabs", true,
				"Build the base configuration for CRIS entities; Run this only one time to create a Tab that contain one Box with title/name metadata; If runs this script with \"-t advanced\" only for Researcher profile and Journals entity will create the box with the related publications and the \"Details\" box show other metadata e.g. fullName, preferredName, variants, academicName for RP and journalname, journalissn, journaleissn, journalpublisher for Journal entity");
		options.addOption("o", "orcid", false,
				"Build tabs to show the ORCID information on token authorization and settings preference, also build the edit tab to fill the settings preferences");

		CommandLine line = parser.parse(options, args);
		if (line.hasOption('h')) {
			HelpFormatter myhelp = new HelpFormatter();
			myhelp.printHelp("StartupMetadataConfiguratorTool (BETA version use with caution!!!)\n", options);
			System.exit(0);
		}

		if (line.hasOption('f')) {
			fileExcel = line.getOptionValue('f');
		}

		Context dspaceContext = new Context();
		dspaceContext.setIgnoreAuthorization(true);
		DSpace dspace = new DSpace();
		ApplicationService applicationService = dspace.getServiceManager().getServiceByName("applicationService",
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
		Map<String, List<String>> controlledListMap = new HashMap<String, List<String>>();

		PlatformTransactionManager transactionManager = (PlatformTransactionManager) dspace.getServiceManager()
				.getServiceByName("transactionManager", HibernateTransactionManager.class);
		DefaultTransactionAttribute transactionAttribute = new DefaultTransactionAttribute(
				TransactionDefinition.PROPAGATION_REQUIRED);
		//
		// transactionAttribute
		// .setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
		TransactionStatus status = transactionManager.getTransaction(transactionAttribute);
		boolean success = false;
		try {
			buildMap(workbook, "propertiesdefinition", widgetMap, 0);
			buildMap(workbook, "nesteddefinition", nestedMap, 10);
			buildControlledList(workbook, "controlledlist", controlledListMap);

			buildResearchObject(workbook, "utilsdata", applicationService);

			// per ogni target chiama il metodo con i corretti parametri
			for (String key : widgetMap.keySet()) {
				List<List<String>> meta = widgetMap.get(key);
				if (key.equals("rp")) {
					build(applicationService, meta, nestedMap, RPPropertiesDefinition.class,
							DecoratorRPPropertiesDefinition.class, RPNestedPropertiesDefinition.class,
							DecoratorRPNestedPropertiesDefinition.class, RPTypeNestedObject.class,
							DecoratorRPTypeNested.class, controlledListMap);
				} else if (key.equals("pj")) {
					build(applicationService, meta, nestedMap, ProjectPropertiesDefinition.class,
							DecoratorProjectPropertiesDefinition.class, ProjectNestedPropertiesDefinition.class,
							DecoratorProjectNestedPropertiesDefinition.class, ProjectTypeNestedObject.class,
							DecoratorProjectTypeNested.class, controlledListMap);
				} else if (key.equals("ou")) {
					build(applicationService, meta, nestedMap, OUPropertiesDefinition.class,
							DecoratorOUPropertiesDefinition.class, OUNestedPropertiesDefinition.class,
							DecoratorOUNestedPropertiesDefinition.class, OUTypeNestedObject.class,
							DecoratorOUTypeNested.class, controlledListMap);
				} else {
					ResultObject<DynamicPropertiesDefinition, DynamicNestedPropertiesDefinition, DynamicTypeNestedObject> result = build(
							applicationService, meta, nestedMap, DynamicPropertiesDefinition.class,
							DecoratorDynamicPropertiesDefinition.class, DynamicNestedPropertiesDefinition.class,
							DecoratorDynamicNestedPropertiesDefinition.class, DynamicTypeNestedObject.class,
							DecoratorDynamicTypeNested.class, controlledListMap);
					DynamicObjectType dtp = applicationService.findTypoByShortName(DynamicObjectType.class, key);
					if (dtp == null) {
						throw new RuntimeException("DynamicObjectType with shortname:" + key + " not found");
					}

					if (result.getTPtoPDEF() != null && !result.getTPtoPDEF().isEmpty()) {
						if (dtp.getMask() != null && !dtp.getMask().isEmpty()) {
							result.getTPtoPDEF().addAll(dtp.getMask());
						}
						dtp.setMask(result.getTPtoPDEF());

					}

					if (result.getTPtoNOTP() != null && !result.getTPtoNOTP().isEmpty()) {
						if (dtp.getTypeNestedDefinitionMask() != null && !dtp.getTypeNestedDefinitionMask().isEmpty()) {
							result.getTPtoNOTP().addAll(dtp.getTypeNestedDefinitionMask());
						}
						dtp.setTypeNestedDefinitionMask(result.getTPtoNOTP());
					}

					applicationService.saveOrUpdate(DynamicObjectType.class, dtp);
				}
			}

			if (line.hasOption('t')) {
				boolean advanced = line.getOptionValue("t").equals("advanced");

				boolean alreadyConfiguredRP = false;
				TabResearcherPage tabRP = applicationService.getTabByShortName(TabResearcherPage.class, "info");

				BoxResearcherPage boxRP = applicationService.getBoxByShortName(BoxResearcherPage.class, "record");
				BoxResearcherPage boxRPPublications = applicationService.getBoxByShortName(BoxResearcherPage.class,
						"publications");
				if (tabRP != null) {
					alreadyConfiguredRP = true;
				}

				// TODO this configuration could be read from excel
				if (!alreadyConfiguredRP) {
					RPPropertiesDefinition pdef = applicationService
							.findPropertiesDefinitionByShortName(RPPropertiesDefinition.class, "fullName");

					if (pdef != null) {
						DecoratorRPPropertiesDefinition containable = (DecoratorRPPropertiesDefinition) applicationService
								.findContainableByDecorable(DecoratorRPPropertiesDefinition.class, pdef.getId());

						tabRP = new TabResearcherPage();
						tabRP.setVisibility(VisibilityTabConstant.HIGH);
						tabRP.setTitle("Info");
						tabRP.setShortName("info");
						EditTabResearcherPage etab = new EditTabResearcherPage();
						etab.setVisibility(VisibilityTabConstant.HIGH);
						etab.setTitle("EInfo");
						etab.setShortName("einfo");

						boxRP = new BoxResearcherPage();
						boxRP.setVisibility(VisibilityTabConstant.HIGH);
						boxRP.setTitle("Details");
						boxRP.setShortName("record");
						boxRP.getMask().add(containable);

						boxRPPublications = new BoxResearcherPage();
						boxRPPublications.setVisibility(VisibilityTabConstant.HIGH);
						boxRPPublications.setTitle("Publications");
						boxRPPublications.setShortName("publications");
						boxRPPublications.setExternalJSP("dspaceitems");
						applicationService.saveOrUpdate(BoxResearcherPage.class, boxRP);
						applicationService.saveOrUpdate(BoxResearcherPage.class, boxRPPublications);

						tabRP.getMask().add(boxRP);
						applicationService.saveOrUpdate(TabResearcherPage.class, tabRP);

						etab.setDisplayTab(tabRP);
						applicationService.saveOrUpdate(EditTabResearcherPage.class, etab);
					}
				}

				boolean alreadyConfiguredOU = false;
				TabOrganizationUnit tabOrg = applicationService.getTabByShortName(TabOrganizationUnit.class, "info");
				if (tabOrg != null) {
					alreadyConfiguredOU = true;
				}

				if (!alreadyConfiguredOU) {

					OUPropertiesDefinition pdef = applicationService
							.findPropertiesDefinitionByShortName(OUPropertiesDefinition.class, "name");
					if (pdef != null) {
						DecoratorOUPropertiesDefinition containable = (DecoratorOUPropertiesDefinition) applicationService
								.findContainableByDecorable(DecoratorOUPropertiesDefinition.class, pdef.getId());

						tabOrg = new TabOrganizationUnit();
						tabOrg.setVisibility(VisibilityTabConstant.HIGH);
						tabOrg.setTitle("Info");
						tabOrg.setShortName("info");

						EditTabOrganizationUnit etab = new EditTabOrganizationUnit();
						etab.setVisibility(VisibilityTabConstant.HIGH);
						etab.setTitle("EInfo");
						etab.setShortName("einfo");

						BoxOrganizationUnit box = new BoxOrganizationUnit();
						box.setVisibility(VisibilityTabConstant.HIGH);
						box.setTitle("Details");
						box.setShortName("record");
						box.getMask().add(containable);

						applicationService.saveOrUpdate(BoxOrganizationUnit.class, box);

						tabOrg.getMask().add(box);
						applicationService.saveOrUpdate(TabOrganizationUnit.class, tabOrg);

						etab.setDisplayTab(tabOrg);
						applicationService.saveOrUpdate(EditTabOrganizationUnit.class, etab);
					}
				}

				boolean alreadyConfiguredPJ = false;
				TabProject tabPJ = applicationService.getTabByShortName(TabProject.class, "info");
				if (tabPJ != null) {
					alreadyConfiguredPJ = true;
				}

				if (!alreadyConfiguredPJ) {

					ProjectPropertiesDefinition pdef = applicationService
							.findPropertiesDefinitionByShortName(ProjectPropertiesDefinition.class, "title");
					if (pdef != null) {
						DecoratorProjectPropertiesDefinition containable = (DecoratorProjectPropertiesDefinition) applicationService
								.findContainableByDecorable(DecoratorProjectPropertiesDefinition.class, pdef.getId());

						tabPJ = new TabProject();
						tabPJ.setVisibility(VisibilityTabConstant.HIGH);
						tabPJ.setTitle("Info");
						tabPJ.setShortName("info");

						EditTabProject etab = new EditTabProject();
						etab.setVisibility(VisibilityTabConstant.HIGH);
						etab.setTitle("EInfo");
						etab.setShortName("einfo");

						BoxProject box = new BoxProject();
						box.setVisibility(VisibilityTabConstant.HIGH);
						box.setTitle("Details");
						box.setShortName("record");
						box.getMask().add(containable);

						applicationService.saveOrUpdate(BoxProject.class, box);

						tabPJ.getMask().add(box);
						applicationService.saveOrUpdate(TabProject.class, tabPJ);

						etab.setDisplayTab(tabPJ);
						applicationService.saveOrUpdate(EditTabProject.class, etab);
					}
				}

				List<DynamicObjectType> listTP = applicationService.getList(DynamicObjectType.class);
				TabDynamicObject tabDO = null;
				BoxDynamicObject boxDO = null;

				TabDynamicObject tabJournal = null;
				BoxDynamicObject boxJournal = null;
				BoxDynamicObject boxJournalPublications = null;
				for (DynamicObjectType tp : listTP) {
					boolean alreadyConfiguredDO = false;
					String shortName = tp.getShortName();
					tabDO = applicationService.getTabByShortName(TabDynamicObject.class, shortName + "info");
					boxDO = applicationService.getBoxByShortName(BoxDynamicObject.class, shortName + "record");

					boolean isJournal = shortName.equals("journal");
					if (isJournal) {
						tabJournal = tabDO;
						boxJournal = boxDO;
						boxJournalPublications = applicationService.getBoxByShortName(BoxDynamicObject.class,
								shortName + "publications");

						if (boxJournalPublications == null) {
							boxJournalPublications = new BoxDynamicObject();
							boxJournalPublications.setVisibility(VisibilityTabConstant.HIGH);
							boxJournalPublications.setTitle("Publications");
							boxJournalPublications.setShortName(shortName + "publications");
							boxJournalPublications.setTypeDef(tp);
							applicationService.saveOrUpdate(BoxDynamicObject.class, boxJournalPublications);
						}
					}

					if (tabDO != null) {
						alreadyConfiguredDO = true;
					}
					if (!alreadyConfiguredDO) {
						DynamicPropertiesDefinition pdef = applicationService.findPropertiesDefinitionByShortName(
								DynamicPropertiesDefinition.class, shortName + "name");
						if (pdef != null) {
							DecoratorDynamicPropertiesDefinition containable = (DecoratorDynamicPropertiesDefinition) applicationService
									.findContainableByDecorable(DecoratorDynamicPropertiesDefinition.class,
											pdef.getId());

							tabDO = new TabDynamicObject();
							tabDO.setVisibility(VisibilityTabConstant.HIGH);
							tabDO.setTitle("Info");
							tabDO.setShortName(shortName + "info");
							tabDO.setTypeDef(tp);

							EditTabDynamicObject etab = new EditTabDynamicObject();
							etab.setVisibility(VisibilityTabConstant.HIGH);
							etab.setTitle("EInfo");
							etab.setShortName(shortName + "einfo");
							etab.setTypeDef(tp);

							boxDO = new BoxDynamicObject();
							boxDO.setVisibility(VisibilityTabConstant.HIGH);
							boxDO.setTitle("Details");
							boxDO.setShortName(shortName + "record");
							boxDO.setTypeDef(tp);

							boxDO.getMask().add(containable);

							applicationService.saveOrUpdate(BoxDynamicObject.class, boxDO);

							tabDO.getMask().add(boxDO);
							applicationService.saveOrUpdate(TabDynamicObject.class, tabDO);

							etab.setDisplayTab(tabDO);
							applicationService.saveOrUpdate(EditTabDynamicObject.class, etab);
						}
					}
				}

				if (advanced) {
					boolean saveTab = false;
					// basic configuration for RP box
					if (boxRP != null) {
						String[] rpProps = { "fullName", "preferredName", "translatedName", "variants" };

						boxRP.getMask().clear();
						boxRP.setMask(null);

						for (String prop : rpProps) {
							RPPropertiesDefinition pdef = applicationService
									.findPropertiesDefinitionByShortName(RPPropertiesDefinition.class, prop);
							if (pdef != null) {
								DecoratorRPPropertiesDefinition containable = (DecoratorRPPropertiesDefinition) applicationService
										.findContainableByDecorable(DecoratorRPPropertiesDefinition.class,
												pdef.getId());

								boxRP.getMask().add(containable);
							}
						}
						applicationService.saveOrUpdate(BoxResearcherPage.class, boxRP);

						if (!(tabRP.getMask().contains(boxRP))) {
							tabRP.getMask().add(boxRP);
							saveTab = true;
						}

					} else {
						log.warn(
								"No box configured for RP, run this script with -t option to create basic configuration first");
					}

					if (boxRPPublications != null) {
						if (!(tabRP.getMask().contains(boxRPPublications))) {
							tabRP.getMask().add(boxRPPublications);
							saveTab = true;
						}

					} else {
						log.warn(
								"No box configured for RP, run this script with -t option to create basic configuration first");
					}

					if (saveTab) {
						applicationService.saveOrUpdate(TabResearcherPage.class, tabRP);
					}

					// basic configuration for JOURNALS box
					saveTab = false;
					if (boxJournal != null) {

						String[] journalProps = { "journalname", "journalissn", "journaleissn", "journalpublisher" };

						boxJournal.getMask().clear();
						boxJournal.setMask(null);
						for (String prop : journalProps) {
							DynamicPropertiesDefinition pdef = applicationService
									.findPropertiesDefinitionByShortName(DynamicPropertiesDefinition.class, prop);
							if (pdef != null) {
								DecoratorDynamicPropertiesDefinition containable = (DecoratorDynamicPropertiesDefinition) applicationService
										.findContainableByDecorable(DecoratorDynamicPropertiesDefinition.class,
												pdef.getId());

								boxJournal.getMask().add(containable);
							}
						}
						applicationService.saveOrUpdate(BoxDynamicObject.class, boxJournal);
					} else {
						log.warn(
								"No box configured for JOURNALS, run this script with -t option to create basic configuration first");
					}
					if (tabJournal != null && boxJournalPublications != null) {
						if (!(tabJournal.getMask().contains(boxJournalPublications))) {
							tabJournal.getMask().add(boxJournalPublications);
							saveTab = true;
						}
					} else {
						log.warn(
								"No box configured for JOURNALS, run this script with -t option to create basic configuration first");
					}
					if (saveTab) {
						applicationService.saveOrUpdate(TabDynamicObject.class, tabJournal);
					}

				}

			}

			if (line.hasOption("o")) {
				boolean saveTab = false;

				TabResearcherPage tabORCID = applicationService.getTabByShortName(TabResearcherPage.class, "orcid");
				EditTabResearcherPage editTabORCID = applicationService.getTabByShortName(EditTabResearcherPage.class,
						"eorcid");

				BoxResearcherPage boxAuthorizationORCID = applicationService.getBoxByShortName(BoxResearcherPage.class,
						"orcidauthorizations");
				BoxResearcherPage boxSettingsORCID = applicationService.getBoxByShortName(BoxResearcherPage.class,
						"orcidsyncsettings");

				if (tabORCID == null) {
					tabORCID = new TabResearcherPage();
					tabORCID.setVisibility(VisibilityTabConstant.LOW);
					tabORCID.setTitle("ORCID");
					tabORCID.setShortName("orcid");

					editTabORCID = new EditTabResearcherPage();
					editTabORCID.setVisibility(VisibilityTabConstant.LOW);
					editTabORCID.setTitle("ORCID");
					editTabORCID.setShortName("eorcid");

					boxAuthorizationORCID = new BoxResearcherPage();
					boxAuthorizationORCID.setVisibility(VisibilityTabConstant.LOW);
					boxAuthorizationORCID.setTitle("ORCID Authorizations");
					boxAuthorizationORCID.setShortName("orcidauthorizations");
					boxAuthorizationORCID.setExternalJSP("orcidauthorizations");

					boxSettingsORCID = new BoxResearcherPage();
					boxSettingsORCID.setVisibility(VisibilityTabConstant.LOW);
					boxSettingsORCID.setTitle("ORCID Synchronization settings");
					boxSettingsORCID.setShortName("orcidsyncsettings");
					boxSettingsORCID.setExternalJSP("orcidsyncsettings");

					applicationService.saveOrUpdate(BoxResearcherPage.class, boxAuthorizationORCID);
					applicationService.saveOrUpdate(BoxResearcherPage.class, boxSettingsORCID);

					tabORCID.getMask().add(boxAuthorizationORCID);
					tabORCID.getMask().add(boxSettingsORCID);
					applicationService.saveOrUpdate(TabResearcherPage.class, tabORCID);

					editTabORCID.getMask().add(boxSettingsORCID);
					applicationService.saveOrUpdate(EditTabResearcherPage.class, editTabORCID);

				}

				// orcid configuration for RP box
				if (boxAuthorizationORCID != null) {
					String[] rpProps = { "fullName", "email", "orcid", "system-orcid-token-authenticate",
							"system-orcid-token-orcid-profile-read-limited", "system-orcid-token-orcid-bio-update",
							"system-orcid-token-orcid-works-create", "system-orcid-token-orcid-works-update",
							"system-orcid-token-funding-create", "system-orcid-token-funding-update" };

					boxAuthorizationORCID.getMask().clear();
					boxAuthorizationORCID.setMask(null);

					for (String prop : rpProps) {
						RPPropertiesDefinition pdef = applicationService
								.findPropertiesDefinitionByShortName(RPPropertiesDefinition.class, prop);
						if (pdef != null) {
							DecoratorRPPropertiesDefinition containable = (DecoratorRPPropertiesDefinition) applicationService
									.findContainableByDecorable(DecoratorRPPropertiesDefinition.class, pdef.getId());

							boxAuthorizationORCID.getMask().add(containable);
						}
					}
					applicationService.saveOrUpdate(BoxResearcherPage.class, boxAuthorizationORCID);

					if (!(tabORCID.getMask().contains(boxAuthorizationORCID))) {
						tabORCID.getMask().add(boxAuthorizationORCID);
						saveTab = true;
					}

				} else {
					log.warn("No box configured for RP - orcidauthorizations");
				}

				if (boxSettingsORCID != null) {
					String[] rpProps = { "fullName", "email", "orcid", "orcid-publications-prefs", "orcid-projects-prefs", "orcid-profile-pref-email",
							"orcid-profile-pref-fullName", "orcid-profile-pref-preferredName", "orcid-profile-pref-other-emails", "orcid-profile-pref-iso-3166-country", "orcid-profile-pref-keywords", "orcid-profile-pref-biography", "orcid-profile-pref-credit-name" };

					boxSettingsORCID.getMask().clear();
					boxSettingsORCID.setMask(null);

					for (String prop : rpProps) {
						RPPropertiesDefinition pdef = applicationService
								.findPropertiesDefinitionByShortName(RPPropertiesDefinition.class, prop);
						if (pdef != null) {
							DecoratorRPPropertiesDefinition containable = (DecoratorRPPropertiesDefinition) applicationService
									.findContainableByDecorable(DecoratorRPPropertiesDefinition.class, pdef.getId());

							boxSettingsORCID.getMask().add(containable);
						}
					}
					applicationService.saveOrUpdate(BoxResearcherPage.class, boxSettingsORCID);

					if (!(tabORCID.getMask().contains(boxSettingsORCID))) {
						tabORCID.getMask().add(boxSettingsORCID);
						editTabORCID.getMask().add(boxSettingsORCID);
						saveTab = true;
					}

				} else {
					log.warn("No box configured for RP - orcidsyncsettings");
				}

				if (saveTab) {
					applicationService.saveOrUpdate(TabResearcherPage.class, tabORCID);
					applicationService.saveOrUpdate(EditTabResearcherPage.class, editTabORCID);
				}

			}
			success = true;
		} finally {
			if (success) {
				transactionManager.commit(status);
			} else {
				transactionManager.rollback(status);
			}
		}

	}

	private static void buildResearchObject(Workbook workbook, String sheetName,
			ApplicationService applicationService) {
		Cell[] riga;
		Sheet sheet = workbook.getSheet(sheetName);
		int indexRiga = 1;
		int rows = sheet.getColumn(2).length;
		while (indexRiga < rows) {
			riga = sheet.getRow(indexRiga);
			String key = riga[2].getContents().trim();
			if (key.equals("rp") || key.equals("pj") || key.equals("ou") || key.equals("n")) {
				indexRiga++;
				continue;
			}
			DynamicObjectType dtp = applicationService.findTypoByShortName(DynamicObjectType.class, key);
			if (dtp == null) {
				dtp = new DynamicObjectType();
				dtp.setShortName(key);
				dtp.setLabel(key);
				System.out.println("Build research object - " + key);
				applicationService.saveOrUpdate(DynamicObjectType.class, dtp);
			} else {
				System.out.println("Research object already founded- skip " + key);
			}

			indexRiga++;

		}

	}

	private static <PD extends PropertiesDefinition, DPD extends ADecoratorPropertiesDefinition<PD>, NPD extends ANestedPropertiesDefinition, DNPD extends ADecoratorPropertiesDefinition<NPD>, ATNO extends ATypeNestedObject<NPD>, DATNO extends ADecoratorTypeDefinition<ATNO, NPD>> ResultObject build(
			ApplicationService applicationService, List<List<String>> meta, Map<String, List<List<String>>> nestedMap,
			Class<PD> classPD, Class<DPD> classDPD, Class<NPD> classNPD, Class<DNPD> classDNPD, Class<ATNO> classATNO,
			Class<DATNO> classDATNO, Map<String, List<String>> controlledListMap) {

		ResultObject<PD, NPD, ATNO> result = new ResultObject<PD, NPD, ATNO>();
		for (List<String> list : meta) {
			String target = list.get(0);
			String shortName = list.get(1);
			String label = list.get(2);
			boolean repeatable = list.get(3).equals("y") ? true : false;
			String priority = list.get(4);
			String help = list.get(5);
			Integer accessLevel = AccessLevelConstants.ADMIN_ACCESS;
			String tmpAccessLevel = list.get(6);
			if (tmpAccessLevel.equals("STANDARD_ACCESS")) {
				accessLevel = AccessLevelConstants.STANDARD_ACCESS;
			} else if (tmpAccessLevel.equals("HIGH_ACCESS")) {
				accessLevel = AccessLevelConstants.HIGH_ACCESS;
			} else if (tmpAccessLevel.equals("LOW_ACCESS")) {
				accessLevel = AccessLevelConstants.LOW_ACCESS;
			}
			boolean mandatory = list.get(7).equals("y") ? true : false;
			String widget = list.get(8);

			if (widget.equals("nested")) {
				String tmpSN = shortName;
				if (DynamicTypeNestedObject.class.isAssignableFrom(classATNO)) {
					tmpSN = target + shortName;
				}
				ATNO atno = applicationService.findTypoByShortName(classATNO, tmpSN);
				if (atno != null) {
					System.out.println("Nested definition already founded - skip " + target + "/" + tmpSN);
					continue;
				}
			} else {
				String tmpSN = shortName;
				if (DynamicPropertiesDefinition.class.isAssignableFrom(classPD)) {
					tmpSN = target + shortName;
				}
				PD pdef = applicationService.findPropertiesDefinitionByShortName(classPD, tmpSN);
				if (pdef != null) {
					System.out.println("Metadata definition already founded - skip " + target + "/" + tmpSN);
					continue;
				}
			}

			System.out.println("Writing  " + target + "/" + shortName);

			GenericXmlApplicationContext ctx = new GenericXmlApplicationContext();

			if (widget.equals("nested")) {

				BeanDefinitionBuilder builderNTP = BeanDefinitionBuilder.genericBeanDefinition(classATNO);
				builderNTP.getBeanDefinition().setAttribute("id", shortName);
				if (DynamicPropertiesDefinition.class.isAssignableFrom(classPD)) {
					shortName = target + shortName;
				}
				builderNTP.addPropertyValue("shortName", shortName);
				builderNTP.addPropertyValue("label", label);
				builderNTP.addPropertyValue("repeatable", repeatable);
				builderNTP.addPropertyValue("mandatory", mandatory);
				builderNTP.addPropertyValue("priority", priority);
				builderNTP.addPropertyValue("help", help);
				builderNTP.addPropertyValue("accessLevel", accessLevel);

				ctx.registerBeanDefinition(shortName, builderNTP.getBeanDefinition());

				BeanDefinitionBuilder builderDNTP = BeanDefinitionBuilder.genericBeanDefinition(classDATNO);
				builderDNTP.addPropertyReference("real", shortName);
				String decoratorShortName = "decorator" + shortName;
				ctx.registerBeanDefinition(decoratorShortName, builderDNTP.getBeanDefinition());

				DATNO dntp = ctx.getBean(classDATNO);
				applicationService.saveOrUpdate(classDATNO, dntp);

				List<List<String>> nestedPropertiesDefinition = nestedMap.get(shortName);
				for (List<String> nestedSingleRow : nestedPropertiesDefinition) {
					DNPD decorator = createDecorator(applicationService, nestedSingleRow, ctx, classNPD, classDNPD,
							controlledListMap);
					dntp.getReal().getMask().add(decorator.getReal());
					result.getTPtoNOTP().add(dntp.getReal());
				}
			} else {
				DPD decorator = createDecorator(applicationService, list, ctx, classPD, classDPD, controlledListMap);
				result.getTPtoPDEF().add(decorator.getReal());
			}
			System.out.println("End write  " + target + "/" + shortName);
			ctx.destroy();
		}
		return result;

	}

	private static <PD extends PropertiesDefinition, DPD extends ADecoratorPropertiesDefinition<PD>> DPD createDecorator(
			ApplicationService applicationService, List<String> metadata, GenericXmlApplicationContext ctx,
			Class<PD> classPD, Class<DPD> classDPD, Map<String, List<String>> controlledListMap) {
		String target = metadata.get(0);
		String shortName = metadata.get(1);
		String label = metadata.get(2);
		boolean repeatable = metadata.get(3).equals("y") ? true : false;
		String priority = metadata.get(4);
		String help = metadata.get(5);
		Integer accessLevel = AccessLevelConstants.ADMIN_ACCESS;
		String tmpAccessLevel = metadata.get(6);
		if (tmpAccessLevel.equals("STANDARD_ACCESS")) {
			accessLevel = AccessLevelConstants.STANDARD_ACCESS;
		} else if (tmpAccessLevel.equals("HIGH_ACCESS")) {
			accessLevel = AccessLevelConstants.HIGH_ACCESS;
		} else if (tmpAccessLevel.equals("LOW_ACCESS")) {
			accessLevel = AccessLevelConstants.LOW_ACCESS;
		}

		boolean mandatory = metadata.get(7).equals("y") ? true : false;
		String widget = metadata.get(8);
		String pointer = metadata.get(9);
		if (DynamicPropertiesDefinition.class.isAssignableFrom(classPD)) {
			shortName = target + shortName;
		}

		BeanDefinitionBuilder builderW = null;

		if (widget.equals("text")) {
			builderW = BeanDefinitionBuilder.genericBeanDefinition(WidgetTesto.class);
		} else if (widget.equals("boolean")) {
			builderW = BeanDefinitionBuilder.genericBeanDefinition(WidgetBoolean.class);
		} else if (widget.equals("radio")) {
			builderW = BeanDefinitionBuilder.genericBeanDefinition(WidgetCheckRadio.class);
			String staticValues = "";
			for(String ss : controlledListMap.get(shortName)) {
				staticValues += ss+"|||";
			}
			builderW.addPropertyValue("staticValues", staticValues.substring(0, staticValues.length()-3));
			builderW.addPropertyValue("option4row", 1);
		} else if (widget.equals("checkbox")) {
			builderW = BeanDefinitionBuilder.genericBeanDefinition(WidgetCheckRadio.class);
			String staticValues = "";
			for(String ss : controlledListMap.get(shortName)) {
				staticValues += ss+"|||";
			}
			builderW.addPropertyValue("staticValues", staticValues.substring(0, staticValues.length()-3));
			builderW.addPropertyValue("option4row", 1);
		} else if (widget.equals("link")) {
			builderW = BeanDefinitionBuilder.genericBeanDefinition(WidgetLink.class);
		} else if (widget.equals("date")) {
			builderW = BeanDefinitionBuilder.genericBeanDefinition(WidgetDate.class);
		} else if (widget.equals("image") || widget.equals("file")) {
			if (target.equals("rp")) {
				builderW = BeanDefinitionBuilder.genericBeanDefinition(WidgetFileRP.class);
			} else if (target.equals("ou")) {
				builderW = BeanDefinitionBuilder.genericBeanDefinition(WidgetFileOU.class);
			} else if (target.equals("pj")) {
				builderW = BeanDefinitionBuilder.genericBeanDefinition(WidgetFileProject.class);
			} else {
				builderW = BeanDefinitionBuilder.genericBeanDefinition(WidgetFileDO.class);
			}

			if (widget.equals("image")) {
				builderW.addPropertyValue("showPreview", true);
			}
		} else if (widget.equals("pointer")) {

			if (pointer.equals("rp")) {
				builderW = BeanDefinitionBuilder.genericBeanDefinition(WidgetPointerRP.class);
				builderW.addPropertyValue("target", "org.dspace.app.cris.model.jdyna.value.RPPointer");
			} else if (pointer.equals("ou")) {
				builderW = BeanDefinitionBuilder.genericBeanDefinition(WidgetPointerOU.class);
				builderW.addPropertyValue("target", "org.dspace.app.cris.model.jdyna.value.OUPointer");
			} else if (pointer.equals("pj")) {
				builderW = BeanDefinitionBuilder.genericBeanDefinition(WidgetPointerPJ.class);
				builderW.addPropertyValue("target", "org.dspace.app.cris.model.jdyna.value.ProjectPointer");
			} else {
				builderW = BeanDefinitionBuilder.genericBeanDefinition(WidgetPointerDO.class);
				builderW.addPropertyValue("target", "org.dspace.app.cris.model.jdyna.value.DOPointer");
				Integer tmpTP = CrisConstants.CRIS_DYNAMIC_TYPE_ID_START
						+ applicationService.findTypoByShortName(DynamicObjectType.class, pointer).getId();
				builderW.addPropertyValue("filterExtended", "search.resourcetype:" + tmpTP);
			}

			builderW.addPropertyValue("display", "${displayObject.name}");
			builderW.addPropertyValue("urlPath", "cris/uuid/${displayObject.uuid}");
		}

		builderW.getBeanDefinition().setAttribute("id", widget);
		String widgetName = "widget" + widget;
		ctx.registerBeanDefinition(widgetName, builderW.getBeanDefinition());

		BeanDefinitionBuilder builderPD = BeanDefinitionBuilder.genericBeanDefinition(classPD);
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

		BeanDefinitionBuilder builderDecorator = BeanDefinitionBuilder.genericBeanDefinition(classDPD);
		builderDecorator.addPropertyReference("real", shortName);
		String decoratorShortName = "decorator" + shortName;
		builderPD.getBeanDefinition().setAttribute("id", decoratorShortName);
		ctx.registerBeanDefinition(decoratorShortName, builderDecorator.getBeanDefinition());

		DPD dtp = ctx.getBean(decoratorShortName, classDPD);
		applicationService.saveOrUpdate(classDPD, dtp);
		return dtp;
	}

	private static void buildMap(Workbook workbook, String sheetName, Map<String, List<List<String>>> widgetMap,
			int indexKey) {
		Cell[] riga;
		Sheet sheet = workbook.getSheet(sheetName);
		int indexRiga = 1;
		int rows = sheet.getColumn(0).length;

		while (indexRiga < rows) {
			riga = sheet.getRow(indexRiga);
			String key = riga[indexKey].getContents().trim();

			List<String> metadata = new ArrayList<String>();

			metadata.add(riga[0].getContents().trim());
			metadata.add(riga[1].getContents().trim());
			metadata.add(riga[2].getContents().trim());
			metadata.add(riga[3].getContents().trim());
			metadata.add(riga[4].getContents().trim());
			metadata.add(riga[5].getContents().trim());
			metadata.add(riga[6].getContents().trim());
			metadata.add(riga[7].getContents().trim());
			metadata.add(riga[8].getContents().trim());
			metadata.add(riga[9].getContents().trim());
			metadata.add(riga[10].getContents().trim());

			insertInMap(widgetMap, key, metadata);

			indexRiga++;

		}
	}

	private static void buildControlledList(Workbook workbook, String sheetName,
			Map<String, List<String>> controlledListMap) {
		Cell row;
		Sheet sheet = workbook.getSheet(sheetName);		
		int indexColumn = 0;
		int rows = sheet.getColumn(0).length;
		int columns = sheet.getRow(0).length;
		while (indexColumn < columns) {
			int indexRiga = 1;
			String header = sheet.getRow(0)[indexColumn].getContents().trim();
			while (indexRiga < rows) {
				row = sheet.getRow(indexRiga)[indexColumn];				
				insertInList(controlledListMap, header, row.getContents().trim());
				indexRiga++;
			}
			indexColumn++;
		}
	}

	private static void insertInMap(Map<String, List<List<String>>> map, String target, List<String> metadata) {
		if (map.containsKey(target)) {
			map.get(target).add(metadata);
		} else {
			List<List<String>> singleRows = new ArrayList<List<String>>();
			singleRows.add(metadata);
			map.put(target, singleRows);
		}
	}

	private static void insertInList(Map<String, List<String>> map, String target, String metadata) {
		if (map.containsKey(target)) {
			map.get(target).add(metadata);
		} else {
			List<String> singleRows = new ArrayList<String>();
			singleRows.add(metadata);
			map.put(target, singleRows);
		}
	}
}

class ResultObject<PD extends PropertiesDefinition, NPD extends ANestedPropertiesDefinition, ATNO extends ATypeNestedObject<NPD>> {
	List<ATNO> TPtoNOTP = new ArrayList<ATNO>();

	List<PD> TPtoPDEF = new ArrayList<PD>();

	public List<ATNO> getTPtoNOTP() {
		return TPtoNOTP;
	}

	public void setTPtoNOTP(List<ATNO> tPtoNOTP) {
		TPtoNOTP = tPtoNOTP;
	}

	public List<PD> getTPtoPDEF() {
		return TPtoPDEF;
	}

	public void setTPtoPDEF(List<PD> tPtoPDEF) {
		TPtoPDEF = tPtoPDEF;
	}

}
