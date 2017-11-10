/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.rest.converter.WorkspaceItemConverter;
import org.dspace.app.rest.model.AccessConditionOptionRest;
import org.dspace.app.rest.model.AccessConditionTypeEnum;
import org.dspace.app.rest.model.SubmissionUploadRest;
import org.dspace.app.rest.model.hateoas.SubmissionUploadResource;
import org.dspace.app.rest.submit.accesscondition.AccessConditionOption;
import org.dspace.app.rest.submit.accesscondition.UploadConfiguration;
import org.dspace.app.rest.submit.accesscondition.UploadConfigurationService;
import org.dspace.app.rest.utils.DateMathParser;
import org.dspace.app.util.SubmissionConfig;
import org.dspace.app.util.SubmissionConfigReader;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage Configuration Upload section
 * during the submission
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 *
 */
@Component(SubmissionUploadRest.CATEGORY + "." + SubmissionUploadRest.NAME)
public class SubmissionUploadRestRepository extends DSpaceRestRepository<SubmissionUploadRest, String>
		implements LinkRestRepository<SubmissionUploadRest> {
	
	private static final Logger log = Logger.getLogger(SubmissionUploadRestRepository.class);

	private SubmissionConfigReader submissionConfigReader;

	@Autowired
	private UploadConfigurationService uploadConfigurationService;

	@Autowired
	GroupService groupService;

	DateMathParser dateMathParser = new DateMathParser();
	
	public SubmissionUploadRestRepository() throws ServletException {
		submissionConfigReader = new SubmissionConfigReader();
	}

	@Override
	public SubmissionUploadRest findOne(Context context, String submitName) {
		UploadConfiguration config = uploadConfigurationService.getMap().get(submitName);
		try {
			return convert(context, config);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	@Override
	public Page<SubmissionUploadRest> findAll(Context context, Pageable pageable) {
		List<SubmissionConfig> subConfs = new ArrayList<SubmissionConfig>();
		subConfs = submissionConfigReader.getAllSubmissionConfigs(pageable.getPageSize(), pageable.getOffset());
		List<SubmissionUploadRest> results = new ArrayList<>();
		for (SubmissionConfig config : subConfs) {
			for (int i = 0; i < config.getNumberOfSteps(); i++) {
				SubmissionStepConfig step = config.getStep(i);
				if (SubmissionStepConfig.UPLOAD_STEP_NAME.equals(step.getId())) {
					UploadConfiguration uploadConfig = uploadConfigurationService.getMap().get(step.getId());
					if (uploadConfig != null) {
						try {
							results.add(convert(context, uploadConfig));
						} catch (Exception e) {
							log.error(e.getMessage(), e);
						}
					}
				}
			}
		}
		return new PageImpl<SubmissionUploadRest>(results, pageable, results.size());
	}

	@Override
	public Class<SubmissionUploadRest> getDomainClass() {
		return SubmissionUploadRest.class;
	}

	@Override
	public SubmissionUploadResource wrapResource(SubmissionUploadRest sd, String... rels) {
		return new SubmissionUploadResource(sd, utils, rels);
	}

	private SubmissionUploadRest convert(Context context, UploadConfiguration config) throws Exception {
		SubmissionUploadRest result = new SubmissionUploadRest();
		for (AccessConditionOption option : config.getOptions()) {
			AccessConditionOptionRest optionRest = new AccessConditionOptionRest();
			Group group = groupService.findByName(context, option.getGroupName());
			if (group != null) {
				if ("embargo".equals(option.getType()) || "lease".equals(option.getType())) {
					optionRest.setSelectGroupUuid(group.getID());
				} else {
					optionRest.setGroupUuid(group.getID());
				}
			}
			optionRest.setHasDate(option.getHasDate());
			if(StringUtils.isNotBlank(option.getDateLimit())) {
				optionRest.setMaxEndDate(dateMathParser.parseMath(option.getDateLimit()));
			}
			optionRest.setType(AccessConditionTypeEnum.valueOf(option.getType()));			
			result.getAccessConditions().add(optionRest);
		}
		result.setMetadata(config.getMetadata());
		result.setMaxSize(config.getMaxSize());
		result.setRequired(config.isRequired());
		result.setName(config.getName());
		return result;
	}
}