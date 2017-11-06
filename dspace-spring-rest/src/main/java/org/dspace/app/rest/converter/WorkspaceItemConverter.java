/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.sql.SQLException;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.dspace.app.rest.model.SubmissionDefinitionRest;
import org.dspace.app.rest.model.SubmissionSectionRest;
import org.dspace.app.rest.model.WorkspaceItemRest;
import org.dspace.app.rest.submit.AbstractRestProcessingStep;
import org.dspace.app.util.SubmissionConfigReader;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.eperson.EPerson;
import org.dspace.submit.AbstractProcessingStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the WorkspaceItem in the DSpace API data model
 * and the REST data model
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@Component
public class WorkspaceItemConverter
		extends DSpaceConverter<org.dspace.content.WorkspaceItem, org.dspace.app.rest.model.WorkspaceItemRest> {

	private static final Logger log = Logger.getLogger(WorkspaceItemConverter.class);

	@Autowired
	private EPersonConverter epersonConverter;

	@Autowired
	private ItemConverter itemConverter;

	@Autowired
	private CollectionConverter collectionConverter;
	
	private SubmissionConfigReader submissionConfigReader;

	@Autowired
	private SubmissionDefinitionConverter submissionDefinitionConverter;
	@Autowired
	private SubmissionSectionConverter submissionSectionConverter;

	public WorkspaceItemConverter() throws ServletException {
		submissionConfigReader = new SubmissionConfigReader();
	}

	@Override
	public WorkspaceItemRest fromModel(org.dspace.content.WorkspaceItem obj) {
		WorkspaceItemRest witem = new WorkspaceItemRest();
		
		Collection collection = obj.getCollection();
		Item item = obj.getItem();
		EPerson submitter = null;
		try {
			 submitter = obj.getSubmitter();	
		} catch (SQLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		
		
		witem.setId(obj.getID());
		witem.setCollection(collectionConverter.convert(collection));
		witem.setItem(itemConverter.convert(item));
		witem.setSubmitter(epersonConverter.convert(submitter));
		

		// 1. retrieve the submission definition
		// 2. iterate over the submission section to allow to plugin additional
		// info
		
		if (collection != null) {
			SubmissionDefinitionRest def = submissionDefinitionConverter
					.convert(submissionConfigReader.getSubmissionConfigByCollection(collection.getHandle()));
			for (SubmissionSectionRest sections : def.getPanels()) {
				SubmissionStepConfig stepConfig = submissionSectionConverter.toModel(sections);

				/*
				 * First, load the step processing class (using the current
				 * class loader)
				 */
				ClassLoader loader = this.getClass().getClassLoader();
				Class stepClass;
				try {
					stepClass = loader.loadClass(stepConfig.getProcessingClassName());

					Object stepInstance = stepClass.newInstance();

					if (stepInstance instanceof AbstractProcessingStep) {
						// load the JSPStep interface for this step
						AbstractRestProcessingStep stepProcessing = (AbstractRestProcessingStep) stepClass
								.newInstance();
						witem.getSections().put(sections.getId(), stepProcessing.getData(obj, stepConfig));
					} else {
						throw new Exception("The submission step class specified by '"
								+ stepConfig.getProcessingClassName()
								+ "' does not extend the class org.dspace.submit.AbstractProcessingStep!"
								+ " Therefore it cannot be used by the Configurable Submission as the <processing-class>!");
					}

				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}

			}
		}
		return witem;
	}

	@Override
	public org.dspace.content.WorkspaceItem toModel(WorkspaceItemRest obj) {
		return null;
	}

}
