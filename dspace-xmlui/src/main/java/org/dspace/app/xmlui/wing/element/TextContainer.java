/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.wing.element;

import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingContext;
import org.dspace.app.xmlui.wing.WingException;

/**
 * A class representing all the containers that contain unformatted text, such
 * as head, label, help, value, etc...
 * 
 * This class may not be instantiated on it's own instead you must use one of
 * the extending classes listed above. This abstract class implements the
 * methods common to each of those elements.
 * 
 * @author Scott Phillips
 */

public abstract class TextContainer extends Container
{

    /**
     * Construct a new text container.
     * 
     * This method doesn't do anything but because the inheriting abstract class
     * mandates a constructor for this class to compile it must ensure that the
     * parent constructor is called. Just as implementors of this class must
     * ensure that this constructor is called, thus is the chain of life. :)
     * 
     * @param context
     *            (Required) The context this element is contained in.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    protected TextContainer(WingContext context) throws WingException
    {
        super(context);
    }

    /**
     * Add character content to container.
     * 
     * @param characters
     *            (Required) Direct content or a dictionary tag to be inserted
     *            into the element.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public void addContent(String characters) throws WingException
    {
        Data data = new Data(context, characters);
        contents.add(data);
    }
    
    /**
     * Add integer content to container.
     * 
     * @param integer
     *            (Required) Add the integer into the element's container.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public void addContent(int integer) throws WingException
    {
        Data data = new Data(context, String.valueOf(integer));
        contents.add(data);
    }
    
    /**
     * Add translated content to container.
     * 
     * @param message
     *            (Required) A key into the i18n catalogue for translation into
     *            the user's preferred language.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public void addContent(Message message) throws WingException
    {
        Data data = new Data(context, message);
        contents.add(data);
    }
}
