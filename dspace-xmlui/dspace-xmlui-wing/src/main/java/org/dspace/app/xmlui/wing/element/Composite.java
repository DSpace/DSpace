/*
 * Composite.java
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
 * A class representing a composite input control. The composite input control
 * enables multiple input conrols to be combined together into a single control.
 * Some example uses would be names, that are broken up into both a first and
 * last name. Together they represent a single value but the user can interacts
 * two seperate text boxes for each part of the name.
 * 
 * @author Scott Phillips
 */
import org.dspace.app.xmlui.wing.WingContext;
import org.dspace.app.xmlui.wing.WingException;

public class Composite extends Field
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
    protected Composite(WingContext context, String name, String rend)
            throws WingException
    {
        super(context, name, Field.TYPE_COMPOSITE, rend);
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
     * Add a boolean input control which may be toggled by the user. A checkbox
     * may have several fields which share the same name and each of those
     * fields may be toggled independently. This is distinct from a radio button
     * where only one field may be toggled.
     * 
     * @param name
     *            (Required) a non-unique local identifier used to differentiate
     *            the element from its siblings within an interactive division.
     *            This is the name of the field use when data is submitted back
     *            to the server.
     * @param rend
     *            (May be null) a rendering hint used to override the default
     *            display of the element.
     * @return a new checkbox field
     */
    public CheckBox addCheckBox(String name, String rend) throws WingException
    {
        CheckBox checkbox = new CheckBox(context, name, rend);
        fields.add(checkbox);
        return checkbox;
    }

    /**
     * Add a boolean input control which may be toggled by the user. A checkbox
     * may have several fields which share the same name and each of those
     * fields may be toggled independently. This is distinct from a radio button
     * where only one field may be toggled.
     * 
     * @param name
     *            (Required) a non-unique local identifier used to differentiate
     *            the element from its siblings within an interactive division.
     *            This is the name of the field use when data is submitted back
     *            to the server.
     * @return A new checkbox field
     */
    public CheckBox addCheckBox(String name) throws WingException
    {
        return addCheckBox(name, null);
    }

    /**
     * Add a boolean input control which may be toggled by the user. Multiple
     * radio button fields may share the same name. When this occurs only one
     * field may be selected to be true. This is distinct from a checkbox where
     * multiple fields may be toggled.
     * 
     * @param name
     *            (Required) a non-unique local identifier used to differentiate
     *            the element from its siblings within an interactive division.
     *            This is the name of the field use when data is submitted back
     *            to the server.
     * @param rend
     *            (May be null) a rendering hint used to override the default
     *            display of the element.
     * @return a new radio field.
     */
    public Radio addRadio(String name, String rend) throws WingException
    {
        Radio radio = new Radio(context, name, rend);
        fields.add(radio);
        return radio;
    }

    /**
     * Add a boolean input control which may be toggled by the user. Multiple
     * radio button fields may share the same name. When this occurs only one
     * field may be selected to be true. This is distinct from a checkbox where
     * multiple fields may be toggled.
     * 
     * @param name
     *            (Required) a non-unique local identifier used to differentiate
     *            the element from its siblings within an interactive division.
     *            This is the name of the field use when data is submitted back
     *            to the server.
     * 
     * @return a new radio field
     */
    public Radio addRadio(String name) throws WingException
    {
        return addRadio(name, null);
    }
    
    /**
     * Add a menu input control which allows the user to select from a list of
     * available options.
     * 
     * @param name
     *            (Required) a non-unique local identifier used to differentiate
     *            the element from its siblings within an interactive division.
     *            This is the name of the field use when data is submitted back
     *            to the server.
     * @param rend
     *            (May be null) a rendering hint used to override the default
     *            display of the element.
     * @return a new select field
     */
    public Select addSelect(String name, String rend) throws WingException
    {
        Select select = new Select(context, name, rend);
        fields.add(select);
        return select;
    }

    /**
     * Add a menu input control which allows the user to select from a list of
     * available options.
     * 
     * @param name
     *            (Required) a non-unique local identifier used to differentiate
     *            the element from its siblings within an interactive division.
     *            This is the name of the field use when data is submitted back
     *            to the server.
     * @return a new select field
     */
    public Select addSelect(String name) throws WingException
    {
        return addSelect(name, null);
    }

    /**
     * Add a single-line text input control.
     * 
     * @param name
     *            (Required) a non-unique local identifier used to differentiate
     *            the element from its siblings within an interactive division.
     *            This is the name of the field use when data is submitted back
     *            to the server.
     * @param rend
     *            (May be null) a rendering hint used to override the default
     *            display of the element.
     * @return A new text field
     */
    public Text addText(String name, String rend) throws WingException
    {

        Text text = new Text(context, name, rend);
        fields.add(text);
        return text;
    }

    /**
     * Add a single-line text input control.
     * 
     * @param name
     *            (Required) a non-unique local identifier used to differentiate
     *            the element from its siblings within an interactive division.
     *            This is the name of the field use when data is submitted back
     *            to the server.
     * @return a new text field
     */
    public Text addText(String name) throws WingException
    {
        return addText(name, null);
    }

    /**
     * Add a multi-line text input control.
     * 
     * @param name
     *            (Required) a non-unique local identifier used to differentiate
     *            the element from its siblings within an interactive division.
     *            This is the name of the field use when data is submitted back
     *            to the server.
     * @param rend
     *            (May be null) a rendering hint used to override the default
     *            display of the element.
     * @return a new text area field
     */
    public TextArea addTextArea(String name, String rend) throws WingException
    {
        TextArea textarea = new TextArea(context, name, rend);
        fields.add(textarea);
        return textarea;
    }

    /**
     * Add a multi-line text input control.
     * 
     * @param name
     *            (Required) a non-unique local identifier used to differentiate
     *            the element from its siblings within an interactive division.
     *            This is the name of the field use when data is submitted back
     *            to the server.
     * @return a new text area field
     */
    public TextArea addTextArea(String name) throws WingException
    {
        return addTextArea(name, null);
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
