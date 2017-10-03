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
import org.dspace.app.rest.model.AuthorityRest;
import org.dspace.app.rest.model.InputFormFieldRest;
import org.dspace.app.rest.model.InputFormInputTypeRest;
import org.dspace.app.rest.model.InputFormPageRest;
import org.dspace.app.rest.model.InputFormRest;
import org.dspace.app.rest.model.MetadataFieldRest;
import org.dspace.app.rest.model.MetadataSchemaRest;
import org.dspace.app.rest.model.ScopeEnum;
import org.dspace.app.rest.model.SelectableMetadata;
import org.dspace.app.rest.model.SubmissionVisibilityRest;
import org.dspace.app.rest.model.VisibilityEnum;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputSet;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the DCInputSet in the DSpace API data model and
 * the REST data model
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class InputFormConverter extends DSpaceConverter<DCInputSet, InputFormRest> {

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
			// TODO we need a lazy loading reference here
			AuthorityRest authority = new AuthorityRest();
			inputRest.setAuthority(getAuthority(dcinput.getSchema(), dcinput.getElement(), dcinput.getQualifier()));
		} else {
			inputRest.setType("onebox");
			// TODO how to get a full schema representation here?
			// it should be enough to put the ID so that the schema can be
			// retrieved by the REST client from the cache, with another call or
			// embedded automatically by the HAL wrapper
			MetadataSchemaRest schema = new MetadataSchemaRest();
			schema.setPrefix(dcinput.getSchema());
			List<SelectableMetadata> selectableMetadata = new ArrayList<SelectableMetadata>();
			List pairs = dcinput.getPairs();
			for (int idx = 0; idx < pairs.size(); idx += 2) {
				SelectableMetadata selMd = new SelectableMetadata();
				selMd.setLabel((String) pairs.get(idx));
				// TODO again we need a lazy loading reference here
				MetadataFieldRest field = new MetadataFieldRest();
				field.setSchema(schema);
				field.setElement(dcinput.getElement());
				field.setQualifier((String) pairs.get(idx + 1));
				selMd.setField(field);
				// TODO we need a lazy loading reference here
				AuthorityRest authority = new AuthorityRest();
				selMd.setAuthority(getAuthority(dcinput.getSchema(), dcinput.getElement(), dcinput.getQualifier()));
				selectableMetadata.add(selMd);
			}
			inputRest.setSelectableMetadata(selectableMetadata);
		}

		inputField.setInput(inputRest);
		return inputField;
	}

	private AuthorityRest getAuthority(String schema, String element, String qualifier) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DCInputSet toModel(InputFormRest obj) {
		throw new NotImplementedException();
	}
}