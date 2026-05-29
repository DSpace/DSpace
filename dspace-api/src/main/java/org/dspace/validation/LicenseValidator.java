/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.validation;

import static org.dspace.core.Constants.LICENSE_BITSTREAM_NAME;
import static org.dspace.core.Constants.LICENSE_BUNDLE_NAME;
import static org.dspace.validation.service.ValidationService.OPERATION_PATH_SECTIONS;
import static org.dspace.validation.util.ValidationUtils.addError;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.Bitstream;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.validation.model.ValidationError;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This submission validation check that the license has been grant for the
 * inprogress submission looking for the presence of a license bitstream in the
 * license bundle,
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 * @author Luca Giamminonni (luca.giamminonni at 4sciente.it)
 */
public class LicenseValidator implements SubmissionStepValidator {

    public static final String ERROR_VALIDATION_LICENSEREQUIRED = "error.validation.license.required";

    private String name;

    @Autowired
    private BitstreamService bitstreamService;

    @Override
    public List<ValidationError> validate(Context context, InProgressSubmission<?> obj, SubmissionStepConfig config) {
        try {
            return performValidation(obj.getItem(), config);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    private List<ValidationError> performValidation(Item item, SubmissionStepConfig config) throws SQLException {

        List<ValidationError> errors = new ArrayList<>();

        Bitstream bitstream = bitstreamService.getBitstreamByName(item, LICENSE_BUNDLE_NAME, LICENSE_BITSTREAM_NAME);
        if (bitstream == null) {
            addError(errors, ERROR_VALIDATION_LICENSEREQUIRED, "/" + OPERATION_PATH_SECTIONS + "/" + config.getId());
        }
        return errors;
    }

    public BitstreamService getBitstreamService() {
        return bitstreamService;
    }

    public void setBitstreamService(BitstreamService bitstreamService) {
        this.bitstreamService = bitstreamService;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
