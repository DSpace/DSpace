/*
 * Text.java
 *
 * Version: $Revision: 1.2 $
 *
 * Date: $Date: 2006/07/06 17:02:03 $
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package org.dspace.app.xmlui.wing.element;

/**
 * A class representing a text input control. The text input control allows the
 * user to enter one-line of text.
 * 
 * @author Scott Phillips
 */

import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingContext;
import org.dspace.app.xmlui.wing.WingException;

public class Text extends Field
{
    /**
     * Construct a new field.
     * 
     * @param context
     *            (Required) The context this element is contained in, such as
     *            where to route SAX events and what i18n catalogue to use.
     * 
     * @param name
     *            (Required) a non-unique local identifier used to differentiate
     *            the element from its siblings within an interactive division.
     *            This is the name of the field use when data is submitted back
     *            to the server.
     * @param rend
     *            (May be null) a rendering hint used to override the default
     *            display of the element.
     */
    protected Text(WingContext context, String name, String rend)
            throws WingException
    {
        super(context, name, Field.TYPE_TEXT, rend);
        this.params = new Params(context);
    }

    /**
     * Set the size of the text field.
     * 
     * @param size
     *            (May be zero for no defined value) he default size for a 
     *            field.
     */
    public void setSize(int size)
    {
        this.params.setSize(size);
    }

    /**
     * Set the size and maximum size of the text field.
     * 
     * @param size
     *            (May be zero for no defined value) he default size for a
     *            field.
     * @param maxLength
     *            (May be zero for no defined value) The maximum length that the
     *            theme should accept for form input.
     */
    public void setSize(int size, int maxLength)
    {
        this.params.setSize(size);
        this.params.setMaxLength(maxLength);
    }
    
    /**
     * Enable the add operation for this field. When this is enabled the
     * front end will add a button to add more items to the field.
     * 
     */
    public void enableAddOperation() throws WingException
    {
        this.params.enableAddOperation();
    }

    /**
     * Enable the delete operation for this field. When this is enabled then
     * the front end will provide a way for the user to select fields (probably
     * checkboxes) along with a submit button to delete the selected fields.
     * 
     */
    public void enableDeleteOperation()throws WingException
    {
        this.params.enableDeleteOperation();
    }
    
    
    /** ******************************************************************** */
    /** Raw Values * */
    /** ******************************************************************** */

    /**
     * Set the raw value of the field removing any previous raw values.
     * 
     * @param characters
     *            (May be null) Field value as a string
     */
    public Value setValue() throws WingException
    {
        removeValueOfType(Value.TYPE_RAW);
        Value value = new Value(context, Value.TYPE_RAW);
        values.add(value);
        return value;
    }

    /**
     * Set the raw value of the field removing any previous raw values.
     * 
     * @param characters
     *            (May be null) Field value as a string
     */
    public void setValue(String characters) throws WingException
    {
        Value value = this.setValue();
        value.addContent(characters);
    }

    /**
     * Set the raw value of the field removing any previous raw values.
     * 
     * @param message
     *            (Required) A key into the i18n catalogue for translation into
     *            the user's preferred language.
     */
    public void setValue(Message message) throws WingException
    {
        Value value = this.setValue();
        value.addContent(message);
    }
    
    
    /**
     * Add a field instance
     * @return instance
     */
    public Instance addInstance() throws WingException
    {
        Instance instance = new Instance(context);
        instances.add(instance);
        return instance;
    }
}
