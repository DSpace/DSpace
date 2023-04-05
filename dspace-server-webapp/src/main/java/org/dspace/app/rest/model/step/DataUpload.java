/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.step;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * Java Bean to expose the section upload during in progress submission.
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public class DataUpload implements SectionData {

    @JsonUnwrapped
    private List<UploadBitstreamRest> files;

    public List<UploadBitstreamRest> getFiles() {
        if (files == null) {
            files = new ArrayList<UploadBitstreamRest>();
        }
        return files;
    }

    public void setFiles(List<UploadBitstreamRest> files) {
        this.files = files;
    }
}
