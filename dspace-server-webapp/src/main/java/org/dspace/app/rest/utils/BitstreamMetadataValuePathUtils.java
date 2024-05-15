/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.submit.model.UploadConfiguration;
import org.dspace.submit.model.UploadConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Utils class offering methods to validate patch operations for bitstream metadata in the submission
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class BitstreamMetadataValuePathUtils {

    private DCInputsReader inputReader;

    @Autowired
    UploadConfigurationService uploadConfigurationService;

    BitstreamMetadataValuePathUtils() throws DCInputsReaderException {
        inputReader = new DCInputsReader();
    }

    /**
     * Method to verify that the path included in the patch operation is supported
     * by the submission configuration of the upload section
     *
     * @param stepId the name of the upload configuration
     * @param absolutePath the path in the json patch operation
     * @throws DCInputsReaderException      if an error occurs reading the
     *                                      submission configuration
     * @throws UnprocessableEntityException if the path is invalid
     */
    public void validate(String stepId, String absolutePath) throws DCInputsReaderException {
        UploadConfiguration uploadService = uploadConfigurationService.getMap().get(stepId);
        DCInputSet inputConfig = inputReader.getInputsByFormName(uploadService.getMetadata());
        String[] split = absolutePath.split("/");
        // according to the rest contract the absolute path must be something like files/:idx/metadata/dc.title
        if (split.length >= 4) {
            if (!inputConfig.isFieldPresent(split[3])) {
                throw new UnprocessableEntityException("The field " + split[3] + " is not present in section "
                                                                    + stepId);
            }
        } else {
            throw new UnprocessableEntityException("The path " + absolutePath + " cannot be patched ");
        }
    }
}
