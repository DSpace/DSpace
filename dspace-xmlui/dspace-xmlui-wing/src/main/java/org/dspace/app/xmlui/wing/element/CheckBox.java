/*
 * CheckBox.java
 *
 * Version: $Revision: 1.3 $
 *
 * Date: $Date: 2006/07/20 21:47:44 $
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
 * A class representing a CheckBox input control. The checkbox input control is
 * a boolean control which may be toggled by the user. A checkbox may have
 * several fields which share the same name and each of those fields may be
 * toggled independently. This is distinct from a radio button where only one
 * field may be toggled.
 * 
 * @author Scott Phillips
 */

import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingContext;
import org.dspace.app.xmlui.wing.WingException;

public class CheckBox extends Field
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
    protected CheckBox(WingContext context, String name, String rend)
            throws WingException
    {
        super(context, name, Field.TYPE_CHECKBOX, rend);
        this.params = new Params(context);
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
    
    
    
    
    
    /**
     * Add an option.
     * 
     * @param returnValue
     *            (Required) The value to be passed back if this option is
     *            selected.
     */
    public Option addOption(String returnValue)
            throws WingException
    {
        Option option = new Option(context, returnValue);
        options.add(option);

        return option;
    }

    /**
     * Add an option.
     * 
     * @param returnValue
     *            (Required) The value to be passed back if this option is
     *            selected.
     */
    public Option addOption(int returnValue)
            throws WingException
    {
        return addOption(String.valueOf(returnValue));
    }
    
    /**
     * Add an option.
     * 
     * @param selected
     *            (Required) Set the option is checked
     * @param returnValue
     *            (Required) The value to be passed back if this option is
     *            checked.
     */
    public Option addOption(boolean selected, String returnValue)
            throws WingException
    {
    	if (selected)
    		setOptionSelected(returnValue);
        return addOption(returnValue);
    }
    
    /**
     * Add an option.
     * 
     * @param returnValue
     *            (Required) The value to be passed back if this option is
     *            checked.
     * @param characters
     *            (Required) The text to set as the visible option.
     */
    public void addOption(String returnValue, String characters) throws WingException
    {
        Option option = this.addOption(returnValue);
        option.addContent(characters);
    }
    
    /**
     * Add an option.
     * 
     * @param selected
     *            (Required) Set the option is checked
     * @param returnValue
     *            (Required) The value to be passed back if this option is
     *            checked.
     * @param characters
     *            (Required) The text to set as the visible option.
     */
    public void addOption(boolean selected,String returnValue, String characters) throws WingException
    {
    	if (selected)
    		setOptionSelected(returnValue);
        addOption(returnValue,characters);
    }
    
    /**
     * Add an option.
     * 
     * @param returnValue
     *            (Required) The value to be passed back if this option is
     *            checked.
     * @param characters
     *            (Required) The text to set as the visible option.
     */
    public void addOption(int returnValue, String characters) throws WingException
    {
        Option option = this.addOption(String.valueOf(returnValue));
        option.addContent(characters);
    }
    
    /**
     * Add an option.
     * 
     * @param selected
     *            (Required) Set the option as selected.
     * @param returnValue
     *            (Required) The value to be passed back if this option is
     *            selected.
     * @param characters
     *            (Required) The text to set as the visible option.
     */
    public void addOption(boolean selected, int returnValue, String characters) throws WingException
    {
    	if (selected)
    		setOptionSelected(returnValue);
        addOption(returnValue,characters);
    }
    
    /**
     * Add an option.
     * 
     * @param returnValue
     *            (Required) The value to be passed back if this option is
     *            selected.
     * @param message
     *            (Required) The transalted text to set as the visible option.
     */
    public void addOption(String returnValue, Message message) throws WingException
    {
        Option option = this.addOption(returnValue);
        option.addContent(message);
    }
    
    /**
     * Add an option.
     * 
     * @param selected
     *            (Required) Set the option as selected.
     * @param returnValue
     *            (Required) The value to be passed back if this option is
     *            selected.
     * @param message
     *            (Required) The transalted text to set as the visible option.
     */
    public void addOption(boolean selected, String returnValue, Message message) throws WingException
    {
    	if (selected)
    		setOptionSelected(returnValue);
        addOption(returnValue,message);
    }
    
    /**
     * Add an option.
     * 
     * @param returnValue
     *            (Required) The value to be passed back if this option is
     *            selected.
     * @param message
     *            (Required) The transalted text to set as the visible option.
     */
    public void addOption(int returnValue, Message message) throws WingException
    {
        Option option = this.addOption(String.valueOf(returnValue));
        option.addContent(message);
    }
    
    /**
     * Add an option.
     * 
     * @param selected
     *            (Required) Set the field as selected.
     * @param returnValue
     *            (Required) The value to be passed back if this option is
     *            selected.
     * @param message
     *            (Required) The transalted text to set as the visible option.
     */
    public void addOption(boolean selected, int returnValue, Message message) throws WingException
    {
    	if (selected)
    		setOptionSelected(returnValue);
        addOption(returnValue,message);
    }


    
    
    
    
    
    /**
     * Set the given option as checked.
     * 
     * @param returnValue
     *            (Required) The return value of the option to be selected.
     */
    public void setOptionSelected(String returnValue) throws WingException
    {
        Value value = new Value(context,Value.TYPE_OPTION,returnValue);
        values.add(value);
    }
    
    /**
     * Set the given option as selected.
     * 
     * @param returnValue
     *            (Required) The return value of the option to be selected.
     */
    public void setOptionSelected(int returnValue) throws WingException
    {
        Value value = new Value(context,Value.TYPE_OPTION,String.valueOf(returnValue));
        values.add(value);
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
