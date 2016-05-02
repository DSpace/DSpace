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
 * A class representing a character container, such as "p", "hi", "item", or
 * "cell".
 * 
 * <p>This class may not be instantiated on its own; instead you must use one of
 * the extending classes listed above. This abstract class implements the
 * methods common to each of those elements.
 * 
 * @author Scott Phillips
 */
public abstract class RichTextContainer extends TextContainer
{
    /**
     * Construct a new rich text container.
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
    protected RichTextContainer(WingContext context) throws WingException
    {
        super(context);
    }

    /**
     * Add highlighted content to the character container.
     * 
     * @param rend
     *            (May be null) a rendering hint used to override the default
     *            display of the element.
     * @return A new Highlight
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public Highlight addHighlight(String rend) throws WingException
    {
        Highlight highlight = new Highlight(context, rend);
        contents.add(highlight);
        return highlight;
    }

    /**
     * Add a new reference to the character container. The {@code xref} element is a
     * reference to an external document. The content will be used as part of
     * the link's visual body.
     * 
     * @param target
     *            (Required) A target URL for the references a destination for
     *            the {@code xref}.
     * @return the new xref.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public Xref addXref(String target) throws WingException
    {
        Xref xref = new Xref(context, target);
        contents.add(xref);
        return xref;
    }

    /**
     * Add a new reference to the character container. The {@code xref} element is a
     * reference to an external document. The characters will be used as the
     * visual part of the link's body
     * 
     * @param target
     *            (Required) A target URL for the references a destination for
     *            the {@code xref}.
     * @param characters
     *            (May be null) The link's body
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public void addXref(String target, String characters) throws WingException
    {
        Xref xref = addXref(target);
        xref.addContent(characters);
    }
    
    /**
     * Add a new reference to the character container. The {@code xref} element is a
     * reference to an external document. The characters will be used as the
     * visual part of the link's body
     * 
     * @param target
     *            (Required) A target URL for the references a destination for
     *            the {@code xref}.
     * @param characters
     *            (May be null) The link's body
     * @param rend
     * 			  (May be null) Special rendering instructions.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public void addXref(String target, String characters, String rend) throws WingException
    {
    	Xref xref = new Xref(context, target, rend);
    	xref.addContent(characters);
    	contents.add(xref);
    }
    
    /**
     * Add a new reference to the character container. The {@code xref} element is a
     * reference to an external document. The characters will be used as the
     * visual part of the link's body
     * 
     * @param target
     *            (Required) A target URL for the references a destination for
     *            the {@code xref}.
     * @param characters
     *            (May be null) The link's body
     * @param rend
     *            (May be null) Special rendering instructions.
     * @param name
     *            (May be null) local identifier
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public void addXref(String target, String characters, String rend, String name) throws WingException
    {
    	Xref xref = new Xref(context, target, rend, name);
    	xref.addContent(characters);
    	contents.add(xref);
    }

    /**
     * Add a new reference to the character container. The {@code xref} element is a
     * reference to an external document. The translated i18n key will be used
     * as the visual part of the link's body
     * 
     * @param target
     *            (Required) A target URL for the references a destination for
     *            the {@code xref}.
     * @param key
     *            (Required) The link's body
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public void addXref(String target, Message key) throws WingException
    {
        Xref xref = addXref(target);
        xref.addContent(key);
    }
    
    /**
     * Add a new reference to the character container. The {@code xref} element is a
     * reference to an external document. The translated i18n key will be used
     * as the visual part of the link's body
     * 
     * @param target
     *            (Required) A target URL for the references a destination for
     *            the {@code xref}.
     * @param key
     *            (Required) The link's body
     * @param rend
     *  		  (May be null) Special rendering instructions
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public void addXref(String target, Message key, String rend) throws WingException
    {
        Xref xref = new Xref(context, target, rend);
        xref.addContent(key);
    	contents.add(xref);
    }

    /**
     * Add a new reference to the character container. The {@code xref} element is a
     * reference to an external document. The translated i18n key will be used
     * as the visual part of the link's body
     *
     * @param target
     *            (Required) A target URL for the references a destination for
     *            the {@code xref}.
     * @param key
     *            (Required) The link's body
     * @param rend
     *  		  (May be null) Special rendering instructions
     * @param name
     *            Name of the link.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public void addXref(String target, Message key, String rend, String name) throws WingException
    {
        Xref xref = new Xref(context, target, rend, name);
        xref.addContent(key);
    	contents.add(xref);
    }

    /**
     * Add a figure element to the character container.
     * 
     * The figure element is used to embed a reference to an image or a graphic
     * element. The content of a figure will be use as an alternative descriptor
     * or a caption.
     * 
     * @param source
     *            (Required) The source for the image, using a URL or a
     *            pre-defined XML entity.
     * @param target
     *            (May be null) The target reference for the image if the image
     *            is to operate as a link.
     * @param rend
     *            (May be null) a rendering hint used to override the default
     *            display of the element.
     * @return the new Figure.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public Figure addFigure(String source, String target, String rend)
            throws WingException
    {
        Figure figure = new Figure(context, source, target, rend);
        contents.add(figure);
        return figure;
    }
    
     /**
     * Add a figure element to the character container.
     *
     * The figure element is used to embed a reference to an image or a graphic
     * element. The content of a figure will be use as an alternative descriptor
     * or a caption.
     *
     * @param source
     *            (Required) The source for the image, using a URL or a
     *            pre-defined XML entity.
     * @param target
     *            (May be null) The target reference for the image if the image
     *            is to operate as a link.
     * @param title
     *            Title for the figure.
     * @param rend
     *            (May be null) a rendering hint used to override the default
     *            display of the element.
     * @return the new Figure.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public Figure addFigure(String source, String target, String title, String rend)
            throws WingException
    {
        Figure figure = new Figure(context, source, target, title, rend);
        contents.add(figure);
        return figure;
    }

    /**
     * Add a button input control that when activated by the user will submit
     * the form, including all the fields, back to the server for processing.
     * 
     * @param name
     *            (Required) a non-unique local identifier used to differentiate
     *            the element from its siblings within an interactive division.
     *            This is the name of the field use when data is submitted back
     *            to the server.
     * @param rend
     *            (May be null) a rendering hint used to override the default
     *            display of the element.
     * @return A new button field.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public Button addButton(String name, String rend) throws WingException
    {
        Button button = new Button(context, name, rend);
        contents.add(button);
        return button;
    }

    /**
     * Add a button input control that when activated by the user will submit
     * the form, including all the fields, back to the server for processing.
     * 
     * @param name
     *            (Required) a non-unique local identifier used to differentiate
     *            the element from its siblings within an interactive division.
     *            This is the name of the field use when data is submitted back
     *            to the server.
     * @return a new button field
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public Button addButton(String name) throws WingException
    {
        return addButton(name, null);
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
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public CheckBox addCheckBox(String name, String rend) throws WingException
    {
        CheckBox checkbox = new CheckBox(context, name, rend);
        contents.add(checkbox);
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
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public CheckBox addCheckBox(String name) throws WingException
    {
        return addCheckBox(name, null);
    }

    /**
     * Add a composite input control. Composite controls are composed of multiple
     * individual input controls that combine to form a single value. Example, a
     * composite field might be used to represent a name which is broken up into
     * first and last names. In this case there would be a composite field that
     * consists of two text fields.
     * 
     * @param name
     *            (Required) a non-unique local identifier used to differentiate
     *            the element from its siblings within an interactive division.
     * @param rend
     *            (May be null) a rendering hint used to override the default
     *            display of the element.
     * @return a new composite field.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public Composite addComposite(String name, String rend) throws WingException
    {
        Composite composite = new Composite(context, name, rend);
        contents.add(composite);
        return composite;
    }

    /**
     * Add a composite input control. Composite controls are composed of multiple
     * individual input controls that combine to form a single value. Example, a
     * composite field might be used to represent a name which is broken up into
     * first and last names. In this case there would be a composite field that
     * consists of two text fields.
     * 
     * @param name
     *            (Required) a non-unique local identifier used to differentiate
     *            the element from its siblings within an interactive division.
     * @return a new composite field.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public Composite addComposite(String name) throws WingException
    {
        return addComposite(name, null);
    }

    /**
     * Add an input control that allows the user to select files to be submitted
     * with the form. Note that a form which uses a file field must use the
     * multipart method.
     * 
     * @param name
     *            (Required) a non-unique local identifier used to differentiate
     *            the element from its siblings within an interactive division.
     *            This is the name of the field use when data is submitted back
     *            to the server.
     * @param rend
     *            (May be null) a rendering hint used to override the default
     *            display of the element.
     * @return A new file field
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public File addFile(String name, String rend) throws WingException
    {
        File file = new File(context, name, rend);
        contents.add(file);
        return file;
    }

    /**
     * Add an input control that allows the user to select files to be submitted
     * with the form. Note that a form which uses a file field must use the
     * multipart method.
     * 
     * @param name
     *            (Required) a non-unique local identifier used to differentiate
     *            the element from its siblings within an interactive division.
     *            This is the name of the field use when data is submitted back
     *            to the server.
     * @return a new file field
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public File addFile(String name) throws WingException
    {
        return addFile(name, null);
    }

    /**
     * Add an input control that is not rendered on the screen and hidden from
     * the user.
     * 
     * @param name
     *            (Required) a non-unique local identifier used to differentiate
     *            the element from its siblings within an interactive division.
     *            This is the name of the field use when data is submitted back
     *            to the server.
     * @param rend
     *            (May be null) a rendering hint used to override the default
     *            display of the element.
     * @return a new hidden field
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public Hidden addHidden(String name, String rend) throws WingException
    {

        Hidden hidden = new Hidden(context, name, rend);
        contents.add(hidden);
        return hidden;
    }

    /**
     * Add an input control that is not rendered on the screen and hidden from
     * the user.
     * 
     * @param name
     *            (Required) a non-unique local identifier used to differentiate
     *            the element from its siblings within an interactive division.
     *            This is the name of the field use when data is submitted back
     *            to the server.
     * @return a new hidden field.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public Hidden addHidden(String name) throws WingException
    {
        return addHidden(name, null);
    }

    /**
     * Add a single-line text input control where the input text is rendered in
     * such a way as to hide the characters from the user.
     * 
     * @param name
     *            (Required) a non-unique local identifier used to differentiate
     *            the element from its siblings within an interactive division.
     *            This is the name of the field use when data is submitted back
     *            to the server.
     * @param rend
     *            (May be null) a rendering hint used to override the default
     *            display of the element.
     * @return A new password field
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public Password addPassword(String name, String rend) throws WingException
    {
        Password password = new Password(context, name, rend);
        contents.add(password);
        return password;
    }

    /**
     * Add a single-line text input control where the input text is rendered in
     * such a way as to hide the characters from the user.
     * 
     * @param name
     *            (Required) a non-unique local identifier used to differentiate
     *            the element from its siblings within an interactive division.
     *            This is the name of the field use when data is submitted back
     *            to the server.
     * @return a new password field
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public Password addPassword(String name) throws WingException
    {
        return addPassword(name, null);
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
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public Radio addRadio(String name, String rend) throws WingException
    {
        Radio radio = new Radio(context, name, rend);
        contents.add(radio);
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
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
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
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public Select addSelect(String name, String rend) throws WingException
    {
        Select select = new Select(context, name, rend);
        contents.add(select);
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
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
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
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public Text addText(String name, String rend) throws WingException
    {

        Text text = new Text(context, name, rend);
        contents.add(text);
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
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
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
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public TextArea addTextArea(String name, String rend) throws WingException
    {
        TextArea textarea = new TextArea(context, name, rend);
        contents.add(textarea);
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
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public TextArea addTextArea(String name) throws WingException
    {
        return addTextArea(name, null);
    }
}
