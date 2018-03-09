/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.submission;

import java.io.IOException;
import java.sql.SQLException;

import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.authorize.AuthorizeException;
import org.xml.sax.SAXException;

/**
 * This abstract class represents an abstract submission page.
 * <P>
 * This class only adds the required "addReviewSection()"
 * method for a submission step.  This method is used by the
 * ReviewStep to generate a single review/verify screen for the user.
 * 
 * @author Tim Donohue
 */
public abstract class AbstractSubmissionStep extends AbstractStep
{
    /** 
     * Each submission step must define its own information to be reviewed
     * during the final Review/Verify Step in the submission process.
     * <P>
     * The information to review should be tacked onto the passed in 
     * List object.
     * <P>
     * NOTE: To remain consistent across all Steps, you should first
     * add a sub-List object (with this step's name as the heading),
     * by using a call to reviewList.addList().   This sublist is
     * the list you return from this method!
     * 
     * @param reviewList
     *      The List to which all reviewable information should be added
     * @return 
     *      The new sub-List object created by this step, which contains
     *      all the reviewable information.  If this step has nothing to
     *      review, then return null!   
     * @throws org.xml.sax.SAXException whenever.
     * @throws org.dspace.app.xmlui.wing.WingException whenever.
     * @throws org.dspace.app.xmlui.utils.UIException whenever.
     * @throws java.sql.SQLException whenever.
     * @throws java.io.IOException whenever.
     * @throws org.dspace.authorize.AuthorizeException whenever.
     */
    public abstract List addReviewSection(List reviewList)
            throws SAXException, WingException, UIException, SQLException, IOException,
        AuthorizeException;
}
