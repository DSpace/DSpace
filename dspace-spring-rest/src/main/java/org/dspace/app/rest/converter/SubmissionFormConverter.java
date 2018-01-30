/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.dspace.app.rest.model.ScopeEnum;
import org.dspace.app.rest.model.SubmissionFormFieldRest;
import org.dspace.app.rest.model.SubmissionFormInputTypeRest;
import org.dspace.app.rest.model.SubmissionFormRest;
import org.dspace.app.rest.model.SubmissionVisibilityRest;
import org.dspace.app.rest.model.VisibilityEnum;
import org.dspace.app.rest.utils.AuthorityUtils;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputSet;
import org.dspace.submit.model.LanguageFormField;
import org.dspace.submit.model.SelectableMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the DCInputSet in the DSpace API data model and
 * the REST data model
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class SubmissionFormConverter extends DSpaceConverter<DCInputSet, SubmissionFormRest> {

	private static final String INPUT_TYPE_ONEBOX = "onebox";
	private static final String INPUT_TYPE_NAME = "name";
	private static final String INPUT_TYPE_LOOKUP = "lookup";
	private static final String INPUT_TYPE_LOOKUP_NAME = "lookup-name";

	@Autowired
	private AuthorityUtils authorityUtils;

	@Override
	public SubmissionFormRest fromModel(DCInputSet obj) {
		SubmissionFormRest sd = new SubmissionFormRest();
		sd.setName(obj.getFormName());
		DCInput[] step = obj.getFields();
		List<SubmissionFormFieldRest> fields = getPage(step);
		sd.setFields(fields);
		return sd;
	}

	private List<SubmissionFormFieldRest> getPage(DCInput[] page) {
		List<SubmissionFormFieldRest> fields = new LinkedList<SubmissionFormFieldRest>();
		for (DCInput dcinput : page) {
			fields.add(getField(dcinput));
		}
		return fields;
	}

	private SubmissionFormFieldRest getField(DCInput dcinput) {
		SubmissionFormFieldRest inputField = new SubmissionFormFieldRest();
		List<SelectableMetadata> selectableMetadata = new ArrayList<SelectableMetadata>();

		inputField.setLabel(dcinput.getLabel());
		inputField.setHints(dcinput.getHints());
		inputField.setMandatoryMessage(dcinput.getWarning());
		inputField.setMandatory(dcinput.isRequired());
		inputField.setScope(ScopeEnum.fromString(dcinput.getScope()));
		inputField.setVisibility(new SubmissionVisibilityRest(
				VisibilityEnum.fromString(dcinput.isReadOnly("submission") ? "read-only" : null),
				VisibilityEnum.fromString(dcinput.isReadOnly("workflow") ? "read-only" : null)));
		inputField.setRepeatable(dcinput.isRepeatable());
		if(dcinput.getLanguage()) {
			int idx = 1;
			//list contains: at even position the code, at odd position the label 
			for(String code : dcinput.getValueLanguageList()) {
				//check and retrieve "even/odd" couple to build the dto with "code/display" values
				if(idx%2==0) {
					String display = dcinput.getValueLanguageList().get(idx-2);
					LanguageFormField lang = new LanguageFormField(code, display);
					inputField.getLanguageCodes().add(lang);
				}
				idx++;
			}
		}
		SubmissionFormInputTypeRest inputRest = new SubmissionFormInputTypeRest();

		inputRest.setRegex(dcinput.getRegex());

		if (!StringUtils.equalsIgnoreCase(dcinput.getInputType(), "qualdrop_value")) {
			// value-pair and vocabulary are a special kind of authorities
			String inputType = dcinput.getInputType();

			SelectableMetadata selMd = new SelectableMetadata();
			if (authorityUtils.isChoice(dcinput.getSchema(), dcinput.getElement(), dcinput.getQualifier())) {
				inputRest.setType(
						getPresentation(dcinput.getSchema(), dcinput.getElement(), dcinput.getQualifier(), inputType));
				selMd.setAuthority(getAuthorityName(dcinput.getSchema(), dcinput.getElement(),
						dcinput.getQualifier(), dcinput.getPairsType(), dcinput.getVocabulary()));
				selMd.setClosed(
						authorityUtils.isClosed(dcinput.getSchema(), dcinput.getElement(), dcinput.getQualifier()));
			} else {
				inputRest.setType(inputType);
			}
			selMd.setMetadata(org.dspace.core.Utils.standardize(dcinput.getSchema(), dcinput.getElement(), dcinput.getQualifier(), "."));
			selectableMetadata.add(selMd);

		} else {
			inputRest.setType(INPUT_TYPE_ONEBOX);
			List<String> pairs = dcinput.getPairs();
			for (int idx = 0; idx < pairs.size(); idx += 2) {
				SelectableMetadata selMd = new SelectableMetadata();
				selMd.setLabel((String) pairs.get(idx));
				selMd.setMetadata(org.dspace.core.Utils.standardize(dcinput.getSchema(), dcinput.getElement(), pairs.get(idx + 1), "."));
				if (authorityUtils.isChoice(dcinput.getSchema(), dcinput.getElement(), dcinput.getQualifier())) {
					selMd.setAuthority(getAuthorityName(dcinput.getSchema(), dcinput.getElement(),
							pairs.get(idx + 1), dcinput.getPairsType(), dcinput.getVocabulary()));
					selMd.setClosed(
							authorityUtils.isClosed(dcinput.getSchema(), dcinput.getElement(), dcinput.getQualifier()));
				}
				selectableMetadata.add(selMd);
			}
		}
		inputField.setInput(inputRest);
		inputField.setSelectableMetadata(selectableMetadata);
		return inputField;
	}

	private String getPresentation(String schema, String element, String qualifier, String inputType) {
		String presentation = authorityUtils.getPresentation(schema, element, qualifier);
		if (StringUtils.isNotBlank(presentation)) {
			if (INPUT_TYPE_ONEBOX.equals(inputType)) {
				if (AuthorityUtils.PRESENTATION_TYPE_SUGGEST.equals(presentation)) {
					return INPUT_TYPE_ONEBOX;
				} else if (AuthorityUtils.PRESENTATION_TYPE_LOOKUP.equals(presentation)) {
					return INPUT_TYPE_LOOKUP;
				}
			} else if (INPUT_TYPE_NAME.equals(inputType)) {
				if (AuthorityUtils.PRESENTATION_TYPE_LOOKUP.equals(presentation)) {
					return INPUT_TYPE_LOOKUP_NAME;
				}
			}
		}
		return inputType;
	}
	
	private String getAuthorityName(String schema, String element, String qualifier, String valuePairsName, String vocabularyName) {
		if(StringUtils.isNotBlank(valuePairsName)) {
			return valuePairsName;
		}
		else if (StringUtils.isNotBlank(vocabularyName)) {
			return vocabularyName;
		}
		return authorityUtils.getAuthorityName(schema, element,
				qualifier);
	}

	@Override
	public DCInputSet toModel(SubmissionFormRest obj) {
		throw new NotImplementedException();
	}
}