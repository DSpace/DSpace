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
import org.dspace.app.rest.model.InputFormFieldRest;
import org.dspace.app.rest.model.InputFormInputTypeRest;
import org.dspace.app.rest.model.InputFormPageRest;
import org.dspace.app.rest.model.InputFormRest;
import org.dspace.app.rest.model.ScopeEnum;
import org.dspace.app.rest.model.SelectableMetadata;
import org.dspace.app.rest.model.SubmissionVisibilityRest;
import org.dspace.app.rest.model.VisibilityEnum;
import org.dspace.app.rest.utils.AuthorityUtils;
import org.dspace.app.rest.utils.Utils;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the DCInputSet in the DSpace API data model and
 * the REST data model
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class InputFormConverter extends DSpaceConverter<DCInputSet, InputFormRest> {
	@Autowired
	private Utils utils;
	
	@Autowired
	private AuthorityUtils authorityUtils;
	
	@Override
	public InputFormRest fromModel(DCInputSet obj) {
		InputFormRest sd = new InputFormRest();
		sd.setName(obj.getFormName());
		sd.setDefaultConf(obj.isDefaultConf());
		List<InputFormPageRest> pages = new LinkedList<InputFormPageRest>();
		for (int idx = 0; idx < obj.getNumberPages(); idx++) {
			DCInput[] step = obj.getPageRows(idx, true, true);
			boolean mandatory = obj.isPageMandatory(idx);
			String heading = obj.getPageHeading(idx);
			InputFormPageRest sp = getPage(step, mandatory, heading);
			pages.add(sp);
		}
		sd.setPages(pages);
		return sd;
	}

	private InputFormPageRest getPage(DCInput[] page, boolean mandatory, String heading) {
		InputFormPageRest ifPage = new InputFormPageRest();
		ifPage.setMandatory(mandatory);
		ifPage.setHeader(heading);
		List<InputFormFieldRest> fields = new LinkedList<InputFormFieldRest>();
		for (DCInput dcinput : page) {
			fields.add(getField(dcinput));
		}
		ifPage.setFields(fields);
		return ifPage;
	}

	private InputFormFieldRest getField(DCInput dcinput) {
		InputFormFieldRest inputField = new InputFormFieldRest();
		List<SelectableMetadata> selectableMetadata = new ArrayList<SelectableMetadata>();
		
		inputField.setSelectableMetadata(selectableMetadata);
		inputField.setLabel(dcinput.getLabel());
		inputField.setHints(dcinput.getHints());
		inputField.setMandatoryMessage(dcinput.getWarning());
		inputField.setMandatory(dcinput.isRequired());
		inputField.setScope(ScopeEnum.fromString(dcinput.getScope()));
		inputField
				.setVisibility(new SubmissionVisibilityRest(VisibilityEnum.fromString(dcinput.isReadOnly("submission")?"read-only":null),
						VisibilityEnum.fromString(dcinput.isReadOnly("workflow")?"read-only":null)));
		inputField.setRepeatable(dcinput.isRepeatable());

		InputFormInputTypeRest inputRest = new InputFormInputTypeRest();
		inputRest.setType(dcinput.getInputType());
		inputRest.setRegex(dcinput.getRegex());
		
		if (!StringUtils.equalsIgnoreCase(inputRest.getType(), "qualdrop_value")) {
			// value-pair and vocabulary are a special kind of authorities

			SelectableMetadata selMd = new SelectableMetadata();
			if (authorityUtils.isChoice(dcinput.getSchema(), dcinput.getElement(), dcinput.getQualifier())) {
				selMd.setAuthority(
						authorityUtils.getAuthorityName(dcinput.getSchema(), dcinput.getElement(), dcinput.getQualifier()));
				selMd.setClosed(authorityUtils.isClosed(dcinput.getSchema(), dcinput.getElement(), dcinput.getQualifier()));
			}
			selMd.setMetadata(utils.getMetadataKey(dcinput.getSchema(), dcinput.getElement(), dcinput.getQualifier()));
			selectableMetadata.add(selMd);

		} else {
			inputRest.setType("onebox");
			List<String> pairs = dcinput.getPairs();
			for (int idx = 0; idx < pairs.size(); idx += 2) {
				SelectableMetadata selMd = new SelectableMetadata();
				selMd.setLabel((String) pairs.get(idx));
				selMd.setMetadata(utils.getMetadataKey(dcinput.getSchema(), dcinput.getElement(), pairs.get(idx + 1)));
				if (authorityUtils.isChoice(dcinput.getSchema(), dcinput.getElement(), dcinput.getQualifier())) {
					selMd.setAuthority(
							authorityUtils.getAuthorityName(dcinput.getSchema(), dcinput.getElement(), pairs.get(idx + 1)));
					selMd.setClosed(authorityUtils.isClosed(dcinput.getSchema(), dcinput.getElement(), dcinput.getQualifier()));
				}
				selectableMetadata.add(selMd);
			}
		}
		inputField.setInput(inputRest);
		return inputField;
	}

	@Override
	public DCInputSet toModel(InputFormRest obj) {
		throw new NotImplementedException();
	}
}