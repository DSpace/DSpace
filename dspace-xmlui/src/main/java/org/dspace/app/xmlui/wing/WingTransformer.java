/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.wing;

import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Options;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.UserMeta;

/**
 * 
 * The WingTransformer is a simple framework for dealing with DSpace based SAX
 * events. The implementing class is responsible for catching the appropriate
 * events and filtering them into these method calls. This allows implementors
 * to have easy access to the document without dealing with the messiness of the
 * SAX event system.
 * 
 * If the implementing class needs to insert anything into the document they
 * these methods should be implemented such that they insert the correct data
 * into the appropriate places
 * 
 * @author Scott Phillips
 */
public interface WingTransformer
{

    /** What to add at the end of the body
     * @param body to be added.
     * @throws java.lang.Exception on error.
     */
    public void addBody(Body body) throws Exception;

    /** What to add to the options list
     * @param options to be added.
     * @throws java.lang.Exception on error.
     */
    public void addOptions(Options options) throws Exception;

    /** What user metadata to add to the document
     * @param userMeta to be added.
     * @throws java.lang.Exception on error.
     */
    public void addUserMeta(UserMeta userMeta) throws Exception;

    /** What page metadata to add to the document
     * @param pageMeta to be added.
     * @throws java.lang.Exception on error.
     */
    public void addPageMeta(PageMeta pageMeta) throws Exception;

    /** What is a unique name for this component?
     * @return the name.
     */
    public String getComponentName();
}
