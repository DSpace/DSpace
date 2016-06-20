/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.cocoon;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.cocoon.ProcessingException;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.WingTransformer;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Options;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.UserMeta;
import org.dspace.authorize.AuthorizeException;
import org.xml.sax.SAXException;

/**
 * The WingTransformer is a simple framework for dealing with DSpace based SAX
 * events. The implementing class is responsible for catching the appropriate
 * events and filtering them into these method calls. This allows implementors
 * to have easy access to the document without dealing with the messiness of the
 * sax event system.
 * 
 * @author Scott Phillips
 */
public interface DSpaceTransformer extends WingTransformer
{

    /** What to add at the end of the body
     * @throws org.xml.sax.SAXException whenever.
     * @throws org.dspace.app.xmlui.wing.WingException whenever.
     * @throws org.dspace.app.xmlui.utils.UIException whenever.
     * @throws java.sql.SQLException whenever.
     * @throws java.io.IOException whenever.
     * @throws org.dspace.authorize.AuthorizeException whenever.
     * @throws org.apache.cocoon.ProcessingException whenever.
     */
    @Override
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException, ProcessingException;

    /** What to add to the options list
     * @throws org.xml.sax.SAXException whenever.
     * @throws org.dspace.app.xmlui.wing.WingException whenever.
     * @throws org.dspace.app.xmlui.utils.UIException whenever.
     * @throws java.sql.SQLException whenever.
     * @throws java.io.IOException whenever.
     * @throws org.dspace.authorize.AuthorizeException whenever.
     */
    @Override
    public void addOptions(Options options) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException;

    /** What user metadata to add to the document
     * @throws org.xml.sax.SAXException whenever.
     * @throws org.dspace.app.xmlui.wing.WingException whenever.
     * @throws org.dspace.app.xmlui.utils.UIException whenever.
     * @throws java.sql.SQLException whenever.
     * @throws java.io.IOException whenever.
     * @throws org.dspace.authorize.AuthorizeException whenever.
     */
    @Override
    public void addUserMeta(UserMeta userMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException;

    /** What page metadata to add to the document
     * @throws org.xml.sax.SAXException whenever.
     * @throws org.dspace.app.xmlui.wing.WingException whenever.
     * @throws org.dspace.app.xmlui.utils.UIException whenever.
     * @throws java.sql.SQLException whenever.
     * @throws java.io.IOException whenever.
     * @throws org.dspace.authorize.AuthorizeException whenever.
     */
    @Override
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException;

    /** What is a unique name for this component? */
    @Override
    public String getComponentName();

}
