/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.batch;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.OrganizationUnit;
import org.dspace.app.cris.model.Project;
import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.VisibilityConstants;
import org.dspace.app.cris.model.jdyna.ACrisNestedObject;
import org.dspace.app.cris.model.jdyna.DynamicNestedObject;
import org.dspace.app.cris.model.jdyna.OUNestedObject;
import org.dspace.app.cris.model.jdyna.ProjectNestedObject;
import org.dspace.app.cris.model.jdyna.RPNestedObject;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.event.Event;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.utils.DSpace;

import it.cilea.osd.jdyna.model.ATypeNestedObject;
import it.cilea.osd.jdyna.model.Property;

public class ScriptMergeCrisObjects {
	/** log4j logger */
	private static Logger log = Logger.getLogger(ScriptMergeCrisObjects.class);

	public static final String MERGED_PROPERTY_SHORTNAME = "system-mergedinto";

	/**
	 * Batch script to delete a RP. See the technical documentation for further
	 * details.
	 */
	public static void main(String[] args) {
		log.info("#### START MERGE: -----" + new Date() + " ----- ####");
		Context dspaceContext = null;
		try {
			dspaceContext = new Context();
			dspaceContext.turnOffAuthorisationSystem();

			DSpace dspace = new DSpace();
			ApplicationService applicationService = dspace.getServiceManager().getServiceByName("applicationService",
					ApplicationService.class);

			CommandLineParser parser = new PosixParser();

			Options options = new Options();
			options.addOption("h", "help", false, "help");

			options.addOption("t", "target", true, "CRIS ID to retain (merge target)");

			options.addOption("m", "merge", true,
					"CRIS ID(s) to merge into the target (use multuple m if needed - merge occurs respecting the order from left to right)");
			
			options.addOption("d", "delete", false, "delete merged objects, the default (without the -d option) is to disable them");
			
			options.addOption("x", "exclude", false,
					"Don't merge properties, only move link from the merged object to the target");

			options.addOption("s", "skip", false, "properties to ignore during the merge");

			options.addOption("r", "replace", true,
					"properties to override in the target with the values from the merged objects");

			options.addOption("p", "replace_notempty", true,
					"properties to override in the target with the values from the merged objects IF NOT EMPTY");

			CommandLine line = parser.parse(options, args);

			if (line.hasOption('h')) {
				HelpFormatter myhelp = new HelpFormatter();
				myhelp.printHelp("ScriptMergeCrisObject \n", options);
				System.out.println(
						"\n\nUSAGE:\n ScriptMergeCrisObjects -t <crisID> -m <toMergeCRIS-ID1> m <toMergeCRIS-ID2> .. m <toMergeCRIS-IDn>  [-r propR1 -r propR2... -r propRN] [-p prop1 -p prop2... -p propN] [-s] \n");
				System.exit(0);
			}

			String crisID = line.getOptionValue('t');
			String[] mergeIDs = line.getOptionValues('m');
			String[] replace = line.getOptionValues('r');
			String[] replace_notempty = line.getOptionValues('p');
			boolean skipMergeProps = line.hasOption('x');
			boolean delete = line.hasOption('d');
			String[] skip = line.getOptionValues('s');

			Set<String> toMergeIDs = new LinkedHashSet<String>();
			for (String m : mergeIDs) {
				if (StringUtils.isNotBlank(m) && !m.contentEquals(crisID)) {
					toMergeIDs.add(m);
				} else {
					// it is not required to put the target ID in the list of merging objects but
					// someone could misunderstand, just skip
					System.out.println("Ignoring " + m + " from the list of objects to merge as it is the target");
				}
			}

			Context context = new Context();
			context.turnOffAuthorisationSystem();
			ACrisObject targetCrisObject = applicationService.getEntityByCrisId(crisID);
			// foreach toMergeIDs
			for (String m : mergeIDs) {
				merge(context, applicationService, m, targetCrisObject, delete, skipMergeProps, replace, replace_notempty,
						skip);
				context.commit();
			}
			context.complete();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new RuntimeException(e.getMessage(), e);
		} finally {
			if (dspaceContext != null && dspaceContext.isValid()) {
				dspaceContext.abort();
			}
		}
		log.info("#### END ScriptMergeCrisObject: -----" + new Date() + " ----- ####");
		System.exit(0);
	}

	private static void merge(Context context, ApplicationService applicationService, String m,
			ACrisObject targetCrisObject, boolean delete, boolean skipMergeProps, String[] propsToReplace,
			String[] propsToReplaceIfNotEmpty, String[] propsToIgnore) throws SQLException {
		if (propsToReplace == null) {
			propsToReplace = new String[0];
		}
		if (propsToReplaceIfNotEmpty == null) {
			propsToReplaceIfNotEmpty = new String[0];
		}
		if (propsToIgnore == null) {
			propsToIgnore = new String[0];
		}
		
		ACrisObject objToMerge = applicationService.getEntityByCrisId(m);
		if (objToMerge == null || !objToMerge.getClass().isInstance(targetCrisObject)) {
			throw new RuntimeException("The object [" + objToMerge + "] with crisID " + m + " is not an instance of "
					+ targetCrisObject.getClass());
		}

		// retrieve all items linked with the merging objects (update via SQL, updating
		// the last modified timestamp)
		TableRowIterator tri = null;
		int count = 0;
		try {
			tri = DatabaseManager.queryTable(context, "metadatavalue",
					"SELECT resource_type_id, resource_id FROM METADATAVALUE WHERE AUTHORITY = ?", m);
			while (tri.hasNext()) {
				TableRow row = tri.next(context);
				// force reindex of linked items
				context.addEvent(new Event(Event.MODIFY, row.getIntColumn("resource_type_id"),
						row.getIntColumn("resource_id"), null, new String[] {}));
				count++;
			}
			DatabaseManager.updateQuery(context, "UPDATE METADATAVALUE SET authority = ? WHERE AUTHORITY = ?",
					targetCrisObject.getCrisID(), m);
			log.info(LogManager.getHeader(context, "merge", "merging " + m + " in " + targetCrisObject.getCrisID()
					+ " assigning " + count + " item(s) to the target"));
		} finally {
			tri.close();
		}

		// retrieve all the rp linked with the merging objects (update via SQL, updating
		// the last modified timestamp)
		List<ResearcherPage> linkedRPs = linkedTo(applicationService, context, ResearcherPage.class, objToMerge);
		for (ResearcherPage linkedObject : linkedRPs) {
			replaceLink(ResearcherPage.class, applicationService, context, linkedObject, objToMerge, targetCrisObject);
		}
		
		List<RPNestedObject> linkedNRPs = linkedTo(applicationService, context, RPNestedObject.class, objToMerge);
		for (RPNestedObject linkedObject : linkedNRPs) {
			replaceLink(RPNestedObject.class, applicationService, context, linkedObject, objToMerge, targetCrisObject);
		}

		// retrieve all the pj linked with the merging objects
		List<Project> linkedPJs = linkedTo(applicationService, context, Project.class, objToMerge);
		for (Project linkedObject : linkedPJs) {
			replaceLink(Project.class, applicationService, context, linkedObject, objToMerge, targetCrisObject);
		}

		List<ProjectNestedObject> linkedNPJs = linkedTo(applicationService, context, ProjectNestedObject.class, objToMerge);
		for (ProjectNestedObject linkedObject : linkedNPJs) {
			replaceLink(ProjectNestedObject.class, applicationService, context, linkedObject, objToMerge, targetCrisObject);
		}

		// retrieve all the ou linked with the merging objects
		List<OrganizationUnit> linkedOUs = linkedTo(applicationService, context, OrganizationUnit.class, objToMerge);
		for (OrganizationUnit linkedObject : linkedOUs) {
			replaceLink(OrganizationUnit.class, applicationService, context, linkedObject, objToMerge,
					targetCrisObject);
		}
		
		List<OUNestedObject> linkedNOUs = linkedTo(applicationService, context, OUNestedObject.class, objToMerge);
		for (OUNestedObject linkedObject : linkedNOUs) {
			replaceLink(OUNestedObject.class, applicationService, context, linkedObject, objToMerge,
					targetCrisObject);
		}

		// retrieve all the do linked with the merging objects
		List<ResearchObject> linkedDOs = linkedTo(applicationService, context, ResearchObject.class, objToMerge);
		for (ResearchObject linkedObject : linkedDOs) {
			replaceLink(ResearchObject.class, applicationService, context, linkedObject, objToMerge, targetCrisObject);
		}
		
		List<DynamicNestedObject> linkedNDOs = linkedTo(applicationService, context, DynamicNestedObject.class, objToMerge);
		for (DynamicNestedObject linkedObject : linkedNDOs) {
			replaceLink(DynamicNestedObject.class, applicationService, context, linkedObject, objToMerge, targetCrisObject);
		}

		if (!skipMergeProps) {
			// merge the properties of the toMergeID with the target
			for (String propName : propsToReplace) {
				ResearcherPageUtils.cleanPropertyByPropertyDefinition(targetCrisObject, propName);
				ResearcherPageUtils.cleanNestedObjectByShortname(targetCrisObject, propName);
			}
	
			Map<String, List<Property>> objToMerge_a4v = (Map<String, List<Property>>) objToMerge.getAnagrafica4view();
			for (String propName : objToMerge_a4v.keySet()) {
				List<Property> propsObjToMerge = objToMerge_a4v.get(propName);
				if (propsObjToMerge == null || propsObjToMerge.size() == 0) {
					continue;
				}
				if (ArrayUtils.contains(propsToIgnore, propName)) {
					continue;
				} else if (ArrayUtils.contains(propsToReplace, propName)) {
					// we have already cleaned the target object so we need only to add the props
					// from the merged obj
					for (Property p : propsObjToMerge) {
						ResearcherPageUtils.buildGenericValue(targetCrisObject, p.getObject(), propName, p.getVisibility());
					}
				} else if (ArrayUtils.contains(propsToReplaceIfNotEmpty, propName)) {
					if (propsObjToMerge.size() > 0) {
						ResearcherPageUtils.cleanPropertyByPropertyDefinition(targetCrisObject, propName);
					}
					for (Property p : propsObjToMerge) {
						ResearcherPageUtils.buildGenericValue(targetCrisObject, p.getObject(), propName, p.getVisibility());
					}
				} else {
					// default treatment, we will add the props from the merged obj to the target if
					// the target doesn't have any values
					List<Property> props = (List<Property>) targetCrisObject.getAnagrafica4view().get(propName);
					if (props == null || props.size() == 0) {
						for (Property p : propsObjToMerge) {
							ResearcherPageUtils.buildGenericValue(targetCrisObject, p.getObject(), propName,
									p.getVisibility());
						}
					}
				}
			}
			
			// merge nested object
			List<? extends ATypeNestedObject> ntypes = applicationService.getList(targetCrisObject.getClassTypeNested());
			
			for (ATypeNestedObject ntype : ntypes) {
				String nName = ntype.getShortName();
				if (ArrayUtils.contains(propsToIgnore, nName)) {
					continue;
				}
				List<? extends ACrisNestedObject> nestedObjects = applicationService.getNestedObjectsByParentIDAndShortname(objToMerge.getId(), nName, objToMerge.getClassNested());
				if (nestedObjects == null || nestedObjects.size() == 0) {
					continue;
				}
				
				if (ArrayUtils.contains(propsToReplace, nName)) {
					// we have already cleaned the target object so we need only to add the nested object
					// from the merged obj
					for (ACrisNestedObject no : nestedObjects) {
						ResearcherPageUtils.copyNestedObject(targetCrisObject, no);
					}
				} else if (ArrayUtils.contains(propsToReplaceIfNotEmpty, nName)) {
					ResearcherPageUtils.cleanNestedObjectByShortname(targetCrisObject, nName);
					for (ACrisNestedObject no : nestedObjects) {
						ResearcherPageUtils.copyNestedObject(targetCrisObject, no);
					}
				} else {
					// default treatment, we will add the nested object from the merged obj to the target if
					// the target doesn't have any values
					List<? extends ACrisNestedObject> nestedObjectsTarget = applicationService.getNestedObjectsByParentIDAndShortname(targetCrisObject.getId(), nName, targetCrisObject.getClassNested());
					if (nestedObjectsTarget == null || nestedObjectsTarget.size() == 0) {
						ResearcherPageUtils.cleanPropertyByPropertyDefinition(targetCrisObject, nName);
						for (ACrisNestedObject no : nestedObjects) {
							ResearcherPageUtils.copyNestedObject(targetCrisObject, no);
						}
					}
				}
			}
		}
		
		if (ResearcherPage.class.equals(objToMerge.getCRISTargetClass())) {
			ResearcherPage rpToMerge = (ResearcherPage) objToMerge;
			ResearcherPage rpTarget = (ResearcherPage) targetCrisObject;
			if (rpTarget.getEpersonID() == null) {
				rpTarget.setEpersonID(rpToMerge.getEpersonID());
				rpToMerge.setEpersonID(null);
			}
		}

		if (delete) {
			applicationService.delete(objToMerge.getCRISTargetClass(), objToMerge.getId());
		} else {
			// add a system-mergedInto prop
			if (!skipMergeProps) {
				ResearcherPageUtils.buildGenericValue(objToMerge, targetCrisObject, MERGED_PROPERTY_SHORTNAME,
						VisibilityConstants.PUBLIC);
			}
			objToMerge.setStatus(false);
			applicationService.saveOrUpdate(objToMerge.getCRISTargetClass(), objToMerge);
		}

		applicationService.saveOrUpdate(targetCrisObject.getCRISTargetClass(), targetCrisObject);
	}

	private static <C extends ACrisObject> void replaceLink(Class<C> clazz, ApplicationService applicationService,
			Context context, C linkedObject, ACrisObject objToMerge, ACrisObject targetCrisObject) {
		for (Property prop : (List<Property>) linkedObject.getAnagrafica()) {
			if (targetCrisObject.getCRISTargetClass().isAssignableFrom(prop.getObject().getClass())) {
				if (((ACrisObject) prop.getObject()).getID() == objToMerge.getID()) {
					prop.getValue().setOggetto(targetCrisObject);
				}
			}
		}
		applicationService.saveOrUpdate(clazz, linkedObject);
	}

	private static <C extends ACrisNestedObject> void replaceLink(Class<C> clazz, ApplicationService applicationService,
			Context context, C linkedObject, ACrisObject objToMerge, ACrisObject targetCrisObject) {
		for (Property prop : (List<Property>) linkedObject.getAnagrafica()) {
			if (targetCrisObject.getCRISTargetClass().isAssignableFrom(prop.getObject().getClass())) {
				if (((ACrisObject) prop.getObject()).getID() == objToMerge.getID()) {
					prop.getValue().setOggetto(targetCrisObject);
				}
			}
		}
		applicationService.saveOrUpdate(clazz, linkedObject);
	}
	
	private static <C> List<C> linkedTo(ApplicationService as, Context context,
			Class<C> clazzLinked, ACrisObject linkTargetObject) throws SQLException {
		String valueColumn = null;
		String propTable = null;

		if (clazzLinked.isAssignableFrom(ResearcherPage.class)) {
			propTable = "cris_rp_prop";
		} else if (clazzLinked.isAssignableFrom(Project.class)) {
			propTable = "cris_pj_prop";
		} else if (clazzLinked.isAssignableFrom(OrganizationUnit.class)) {
			propTable = "cris_ou_prop";
		} else if (clazzLinked.isAssignableFrom(ResearchObject.class)) {
			propTable = "cris_do_prop";
		} else if (clazzLinked.isAssignableFrom(RPNestedObject.class)) {
			propTable = "cris_rp_no_prop";
		} else if (clazzLinked.isAssignableFrom(ProjectNestedObject.class)) {
			propTable = "cris_pj_no_prop";
		} else if (clazzLinked.isAssignableFrom(OUNestedObject.class)) {
			propTable = "cris_ou_no_prop";
		} else if (clazzLinked.isAssignableFrom(DynamicNestedObject.class)) {
			propTable = "cris_do_no_prop";
		}

		if (linkTargetObject instanceof ResearcherPage) {
			// see RPPointer
			valueColumn = "rpvalue";
		} else if (linkTargetObject instanceof Project) {
			// see PJPointer
			valueColumn = "projectvalue";
		} else if (linkTargetObject instanceof OrganizationUnit) {
			// see OUPointer
			valueColumn = "ouvalue";
		} else if (linkTargetObject instanceof ResearchObject) {
			// see DOPointer
			valueColumn = "dovalue";
		}

		String query = "select parent_id from " + propTable + " where value_id in (select id from jdyna_values where "
				+ valueColumn + " = ?)";
		TableRowIterator tri = null;
		List<C> linkedObjects = new ArrayList<C>();
		try {
			tri = DatabaseManager.query(context, query, linkTargetObject.getId());
			while (tri.hasNext()) {
				TableRow row = tri.next(context);
				linkedObjects.add(as.get(clazzLinked, row.getIntColumn("parent_id")));
			}
		} finally {
			tri.close();
		}

		return linkedObjects;
	}

}
