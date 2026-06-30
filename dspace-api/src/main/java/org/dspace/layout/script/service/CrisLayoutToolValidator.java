/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.script.service;

import java.util.List;

import org.apache.poi.ss.usermodel.Workbook;
import org.dspace.core.Context;
import org.dspace.layout.script.CrisLayoutToolScript;

/**
 * Validator for excel used by the {@link CrisLayoutToolScript}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface CrisLayoutToolValidator {

    String TAB_SHEET = "tab";

    String TAB2BOX_SHEET = "tab2box";

    String BOX_SHEET = "box";

    String BOX2METADATA_SHEET = "box2metadata";

    String METADATAGROUPS_SHEET = "metadatagroups";

    String TAB_POLICY_SHEET = "tabpolicy";

    String BOX_POLICY_SHEET = "boxpolicy";

    String UTILSDATA_SHEET = "utilsdata";

    String TAB_i18n_SHEET = "tab_i18n";

    String BOX_i18n_SHEET = "box_i18n";

    String METADATA_i18n_SHEET = "metadata_i18n";

    String METADATAGROUP_i18n_SHEET = "metadatagroup_i18n";


    String CONTAINER_COLUMN = "CONTAINER";

    String MINOR_COLUMN = "MINOR";

    String STYLE_COLUMN = "STYLE";

    String COLLAPSED_COLUMN = "COLLAPSED";

    String ENTITY_COLUMN = "ENTITY";

    String LABEL_COLUMN = "LABEL";

    String LABEL_AS_HEADING_COLUMN = "LABEL_AS_HEADING";

    String RENDERING_COLUMN = "RENDERING";

    String VALUES_INLINE_COLUMN = "VALUES_INLINE";

    String STYLE_LABEL_COLUMN = "STYLE_LABEL";

    String STYLE_VALUE_COLUMN = "STYLE_VALUE";

    String LEADING_COLUMN = "LEADING";

    String PRIORITY_COLUMN = "PRIORITY";

    String SECURITY_COLUMN = "SECURITY";

    String TYPE_COLUMN = "TYPE";

    String TAB_COLUMN = "TAB";

    String BOX_COLUMN = "BOX";

    String BOXES_COLUMN = "BOXES";

    String CELL_STYLE_COLUMN = "CELL_STYLE";

    String ROW_STYLE_COLUMN = "ROW_STYLE";

    String ROW_COLUMN = "ROW";

    String CELL_COLUMN = "CELL";

    String SHORTNAME_COLUMN = "SHORTNAME";

    String FIELD_TYPE_COLUMN = "FIELDTYPE";

    String METADATA_COLUMN = "METADATA";

    String VALUE_COLUMN = "VALUE";

    String BUNDLE_COLUMN = "BUNDLE";

    String PARENT_COLUMN = "PARENT";

    String GROUP_COLUMN = "GROUP";

    String ALTERNATIVE_TO_COLUMN = "ALTERNATIVE_TO";

    String METADATA_TYPE = "METADATA";

    String BITSTREAM_TYPE = "BITSTREAM";

    String METADATAGROUP_TYPE = "METADATAGROUP";

    List<String> ALLOWED_FIELD_TYPES = List.of(METADATA_TYPE, BITSTREAM_TYPE, METADATAGROUP_TYPE);

    List<String> ALLOWED_SECURITY_VALUES = List.of("PUBLIC", "ADMINISTRATOR", "OWNER ONLY", "OWNER & ADMINISTRATOR",
        "CUSTOM DATA", "CUSTOM DATA & ADMINISTRATOR");

    List<String> ALLOWED_BOOLEAN_VALUES = List.of("yes", "y", "no", "n");

    /**
     * Validate the given workbook.
     *
     * @param  context  the DSpace context
     * @param  workbook the workbook to validate
     * @return          the validation result
     */
    CrisLayoutToolValidationResult validate(Context context, Workbook workbook);

}
