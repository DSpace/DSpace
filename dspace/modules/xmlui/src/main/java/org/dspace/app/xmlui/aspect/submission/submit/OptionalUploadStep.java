/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.submission.submit;

import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.File;
import org.dspace.app.xmlui.wing.element.List;

/**
 * Custom implementation for LIBDRUM-581
 * 
 * Renders the file upload as an optional field
 *
 * @author Mohamed Abdul Rasheed
 */
public class OptionalUploadStep extends UploadStep
{

    /**
     * Establish our required parameters, abstractStep will enforce these.
     */
    public OptionalUploadStep()
    {
        this.requireSubmission = true;
        this.requireStep = true;
    }

    @Override
    protected File addFileItem(List parent, String name) throws WingException {
        File file = parent.addItem().addFile(name);
        file.setLabel(T_file);
        file.setHelp(T_file_help);
        return file;
    }

}
