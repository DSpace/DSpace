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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.ScopeEnum;
import org.dspace.app.rest.model.SubmissionFormFieldRest;
import org.dspace.app.rest.model.SubmissionFormInputTypeRest;
import org.dspace.app.rest.model.SubmissionFormRest;
import org.dspace.app.rest.model.SubmissionFormRowRest;
import org.dspace.app.rest.model.SubmissionVisibilityRest;
import org.dspace.app.rest.model.VisibilityEnum;
import org.dspace.app.rest.model.submit.SelectableMetadata;
import org.dspace.app.rest.model.submit.SelectableRelationship;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.repository.SubmissionFormRestRepository;
import org.dspace.app.rest.utils.AuthorityUtils;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputSet;
import org.dspace.submit.model.LanguageFormField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the DCInputSet in the DSpace API data model and
 * the REST data model
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class SubmissionFormConverter implements DSpaceConverter<DCInputSet, SubmissionFormRest> {

    private static final String INPUT_TYPE_ONEBOX = "onebox";
    private static final String INPUT_TYPE_NAME = "name";
    private static final String INPUT_TYPE_LOOKUP = "lookup";
    private static final String INPUT_TYPE_LOOKUP_NAME = "lookup-name";

    @Autowired
    private AuthorityUtils authorityUtils;

    @Autowired
    private SubmissionFormRestRepository submissionFormRestRepository;

    @Override
    public SubmissionFormRest convert(DCInputSet obj, Projection projection) {
        SubmissionFormRest sd = new SubmissionFormRest();
        sd.setProjection(projection);
        sd.setName(obj.getFormName());
        DCInput[][] step = obj.getFields();
        List<SubmissionFormRowRest> rows = getPage(step, obj.getFormName());
        sd.setRows(rows);
        return sd;
    }

    private List<SubmissionFormRowRest> getPage(DCInput[][] page, String formName) {
        List<SubmissionFormRowRest> rows = new LinkedList<SubmissionFormRowRest>();

        for (DCInput[] row : page) {
            List<SubmissionFormFieldRest> fields = new LinkedList<SubmissionFormFieldRest>();
            SubmissionFormRowRest rowRest = new SubmissionFormRowRest();
            rowRest.setFields(fields);
            rows.add(rowRest);
            for (DCInput dcinput : row) {
                fields.add(getField(dcinput, formName));
            }
        }
        return rows;
    }

    private SubmissionFormFieldRest getField(DCInput dcinput, String formName) {
        SubmissionFormFieldRest inputField = new SubmissionFormFieldRest();
        List<SelectableMetadata> selectableMetadata = new ArrayList<SelectableMetadata>();
        SelectableRelationship selectableRelationship;
        inputField.setLabel(dcinput.getLabel());
        inputField.setHints(dcinput.getHints());
        inputField.setStyle(dcinput.getStyle());
        inputField.setMandatoryMessage(dcinput.getWarning());
        inputField.setMandatory(dcinput.isRequired());
        inputField.setScope(ScopeEnum.fromString(dcinput.getScope()));
        inputField.setVisibility(new SubmissionVisibilityRest(
            VisibilityEnum.fromString(dcinput.isReadOnly("submission") ? "read-only" : null),
            VisibilityEnum.fromString(dcinput.isReadOnly("workflow") ? "read-only" : null)));
        inputField.setRepeatable(dcinput.isRepeatable());
        if (dcinput.getLanguage()) {
            int idx = 1;
            //list contains: at even position the code, at odd position the label
            for (String code : dcinput.getValueLanguageList()) {
                //check and retrieve "even/odd" couple to build the dto with "code/display" values
                if (idx % 2 == 0) {
                    String display = dcinput.getValueLanguageList().get(idx - 2);
                    LanguageFormField lang = new LanguageFormField(code, display);
                    inputField.getLanguageCodes().add(lang);
                }
                idx++;
            }
        }
        SubmissionFormInputTypeRest inputRest = new SubmissionFormInputTypeRest();

        inputRest.setRegex(dcinput.getRegex());

        if (dcinput.isMetadataField()) {
            // only try to process the metadata input type if there's a metadata field
            if (!StringUtils.equalsIgnoreCase(dcinput.getInputType(), "qualdrop_value")) {

                // value-pair and vocabulary are a special kind of authorities
                String inputType = dcinput.getInputType();

                SelectableMetadata selMd = new SelectableMetadata();
                if (isChoice(dcinput.getSchema(), dcinput.getElement(), dcinput.getQualifier(),
                        dcinput.getPairsType(), dcinput.getVocabulary())) {
                    inputRest.setType(getPresentation(dcinput.getSchema(), dcinput.getElement(),
                                                      dcinput.getQualifier(), inputType));
                    selMd.setControlledVocabulary(getAuthorityName(dcinput.getSchema(), dcinput.getElement(),
                                                        dcinput.getQualifier(), dcinput.getPairsType(),
                                                        dcinput.getVocabulary()));
                    selMd.setClosed(
                            isClosed(dcinput.getSchema(), dcinput.getElement(), dcinput.getQualifier(),
                                    dcinput.getPairsType(), dcinput.getVocabulary()));
                } else {
                    inputRest.setType(inputType);
                }
                selMd.setMetadata(org.dspace.core.Utils
                    .standardize(dcinput.getSchema(), dcinput.getElement(), dcinput.getQualifier(), "."));
                selectableMetadata.add(selMd);
            } else {
                // if the field is a qualdrop_value
                inputRest.setType(INPUT_TYPE_ONEBOX);
                List<String> pairs = dcinput.getPairs();
                for (int idx = 0; idx < pairs.size(); idx += 2) {
                    SelectableMetadata selMd = new SelectableMetadata();
                    selMd.setLabel((String) pairs.get(idx));
                    selMd.setMetadata(org.dspace.core.Utils
                            .standardize(dcinput.getSchema(), dcinput.getElement(), pairs.get(idx + 1), "."));
                    if (authorityUtils.isChoice(dcinput.getSchema(), dcinput.getElement(), dcinput.getQualifier())) {
                        selMd.setControlledVocabulary(getAuthorityName(dcinput.getSchema(), dcinput.getElement(),
                                pairs.get(idx + 1), dcinput.getPairsType(), dcinput.getVocabulary()));
                        selMd.setClosed(isClosed(dcinput.getSchema(), dcinput.getElement(),
                                dcinput.getQualifier(), null, dcinput.getVocabulary()));
                    }
                    selectableMetadata.add(selMd);
                }
            }
        }

        inputField.setInput(inputRest);
        if (dcinput.isMetadataField()) {
            inputField.setSelectableMetadata(selectableMetadata);
            inputField.setTypeBind(dcinput.getTypeBindList());
        }
        if (dcinput.isRelationshipField()) {
            selectableRelationship = getSelectableRelationships(dcinput);
            inputField.setSelectableRelationship(selectableRelationship);
        }
        return inputField;
    }

    /**
     * This method will create a SelectableRelationship object
     * The DCInput will be used to define all the properties of the SelectableRelationship object
     * @param dcinput                   The parsed input from submission-forms.xml
     * @return                          The SelectableRelationship object based on the dcinput
     */
    private SelectableRelationship getSelectableRelationships(DCInput dcinput) {
        SelectableRelationship selectableRelationship = new SelectableRelationship();
        selectableRelationship.setRelationshipType(dcinput.getRelationshipType());
        selectableRelationship.setFilter(dcinput.getFilter());
        selectableRelationship.setSearchConfiguration(dcinput.getSearchConfiguration());
        selectableRelationship.setNameVariants(String.valueOf(dcinput.areNameVariantsAllowed()));
        if (CollectionUtils.isNotEmpty(dcinput.getExternalSources())) {
            selectableRelationship.setExternalSources(dcinput.getExternalSources());
        }
        return selectableRelationship;
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
                if (AuthorityUtils.PRESENTATION_TYPE_LOOKUP.equals(presentation) ||
                        AuthorityUtils.PRESENTATION_TYPE_AUTHORLOOKUP.equals(presentation)) {
                    return INPUT_TYPE_LOOKUP_NAME;
                }
            }
        }
        return inputType;
    }

    private String getAuthorityName(String schema, String element, String qualifier, String valuePairsName,
                                    String vocabularyName) {
        if (StringUtils.isNotBlank(valuePairsName)) {
            return valuePairsName;
        } else if (StringUtils.isNotBlank(vocabularyName)) {
            return vocabularyName;
        }
        return authorityUtils.getAuthorityName(schema, element, qualifier);
    }

    private boolean isClosed(String schema, String element, String qualifier, String valuePairsName,
            String vocabularyName) {
        if (StringUtils.isNotBlank(valuePairsName) || StringUtils.isNotBlank(vocabularyName)) {
            return true;
        }
        return authorityUtils.isClosed(schema, element, qualifier);
    }

    private boolean isChoice(String schema, String element, String qualifier, String valuePairsName,
            String vocabularyName) {
        if (StringUtils.isNotBlank(valuePairsName) || StringUtils.isNotBlank(vocabularyName)) {
            return true;
        }
        return authorityUtils.isChoice(schema, element, qualifier);
    }

    @Override
    public Class<DCInputSet> getModelClass() {
        return DCInputSet.class;
    }
}
