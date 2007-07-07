/*
 * InlineFormTest.java
 *
 * Version: $Revision: 1.9 $
 *
 * Date: $Date: 2006/07/24 21:18:47 $
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
package org.dspace.app.xmlui.aspect.xmltest;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Button;
import org.dspace.app.xmlui.wing.element.CheckBox;
import org.dspace.app.xmlui.wing.element.Composite;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.File;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Radio;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.app.xmlui.wing.element.TextArea;
import org.dspace.authorize.AuthorizeException;
import org.xml.sax.SAXException;

/**
 * This is a class to test the use of form fields inline with normal 
 * paragraaphs, lists, or tables. Any other location besides forms.
 * 
 * This class is not internationalized because it is never intended
 * to be used in production. It is merely a tool to aid developers of
 * aspects and themes.
 * 
 * @author Scott Phillips
 */
public class InlineFormTest extends AbstractDSpaceTransformer
{
    
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
        pageMeta.addMetadata("title").addContent("Inline Form Test");
       
        pageMeta.addTrailLink(contextPath + "/","DSpace Home");
        pageMeta.addTrail().addContent("Inline form test");
    }

  
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
    	Request request = ObjectModelHelper.getRequest(objectModel);
		boolean help = false, error = false;
		if (request.getParameter("help") != null)
			help = true;
		if (request.getParameter("error") != null)
			error = true;
		
        Division div = body.addInteractiveDivision("test", "", "post", "primary");
        div.setHead("Inline form test");
        div.addPara("There are two options you can use to control how this page is generated. First is the help parameter, if this is present then help text will be provided for all fields. Next is the error parameter, if it is provided then all fields will be generated in error conditions.");
		
		if (help)
			div.addPara().addXref(makeURL(false,error),"Turn help OFF");
		else 
			div.addPara().addXref(makeURL(true,error),"Turn help ON");
			
		if (error)
			div.addPara().addXref(makeURL(help,false),"Turn errors OFF");
		else 
			div.addPara().addXref(makeURL(help,true),"Turn errors ON");
		
		
		Division suited = body.addDivision("suited");
		suited.setHead("Fields suited towards being used inline");
		
		suited.addPara("Below are a list of embedded fields that are normally considered usefully in an inline context.");
		
		// Text field
		Para p = suited.addPara();
		p.addContent("This is a plain 'Text' field, ");
        Text text = p.addText("text");
        text.setLabel("Text");
        if (help)
        	text.setHelp("This is helpfull text.");
        if (error)
        	text.addError("This field is in error.");
        text.setValue("Current raw value");
        p.addContent(", embedded in a paragraph.");
        
        // Single Checkbox field
        p = suited.addPara();
		p.addContent("This is a singe 'CheckBox' field, ");
        CheckBox checkBox = p.addCheckBox("yes-or-no");
        if (help)
        	checkBox.setHelp("Select either yes or no.");
        if (error)
        	checkBox.addError("You are incorrect, try again.");
        checkBox.setLabel("Yes or no");
        checkBox.addOption("yes");
        p.addContent(", embedded in a paragraph.");
        
        // File
        p = suited.addPara();
		p.addContent("This is a 'File' field, ");
        File file = p.addFile("file");
        file.setLabel("File");
        if (help)
        	file.setHelp("Upload a file.");
        if (error)
        	file.addError("This field is in error.");
        p.addContent(", embedded in a paragraph.");
        
        // Select (single)
        p = suited.addPara();
		p.addContent("This is single 'Select' (aka dropdown) field, ");
        Select select = p.addSelect("select");
        select.setLabel("Select (single)");
        if (help)
        	select.setHelp("Select one of the options");
        if (error)
        	select.addError("This field is in error.");
        select.addOption("one","uno");
        select.addOption("two","dos");
        select.addOption("three","tres");
        select.addOption("four","cuatro");
        select.addOption("five","cinco");
        select.setOptionSelected("one");
        p.addContent(", embedded in a paragraph.");
        
        // Button
        p = suited.addPara();
		p.addContent("This is a 'Button' field, ");
        Button button = p.addButton("button");
        button.setLabel("Button");
        button.setValue("When you touch me I do things, lots of things");
        if (help)
        	button.setHelp("Submit buttons allow the user to submit the form.");
        if (error)
        	button.addError("This button is in error.");
        p.addContent(", embedded in a paragraph.");
        
        
        
        Division unsuited = body.addDivision("unsuited");
        unsuited.setHead("Fields typicaly unsuited towards being used inline");
		
        unsuited.addPara("Below are a list of embedded fields that are normally considered useless in an inline context. This is because there widgets normally cross multiple lines making them hard to render inline. However these are all legal, but perhaps not advisable, and in some circumstances may be needed.");
		
        
        // Text Area Field
        p = unsuited.addPara();
		p.addContent("This is a 'Text Area' field, ");
        TextArea textArea = p.addTextArea("textarea");
        textArea.setLabel("Text Area");
        if (help)
        	textArea.setHelp("This is helpfull text.");
        if (error)
        	textArea.addError("This field is in error.");
        textArea.setValue("This is the raw value");
        p.addContent(", embedded in a paragraph.");
        
        // Multi-option Checkbox field
        p = unsuited.addPara();
		p.addContent("This is a multi-option 'CheckBox' field, ");
        checkBox = p.addCheckBox("fruit");
        if (help)
        	checkBox.setHelp("Select all the fruits that you like to eat");
        if (error)
        	checkBox.addError("You are incorrect you actualy do like Tootse Rolls.");
        checkBox.setLabel("fruits");
        checkBox.addOption("apple","Apples");
        checkBox.addOption(true,"orange","Oranges");
        checkBox.addOption("pear","Pears");
        checkBox.addOption("tootsie","Tootsie Roll");
        checkBox.addOption(true,"cherry","Cherry");
        p.addContent(", embedded in a paragraph.");
        
        // multi-option Radio field
        p = unsuited.addPara();
		p.addContent("This is a multi-option 'Radio' field, ");
        Radio radio = p.addRadio("sex");
        radio.setLabel("Football colors");         
        if (help)
        	radio.setHelp("Select the colors of the best (college) football team.");
        if (error)
        	radio.addError("Error, Maroon & White is the only acceptable answer.");
        radio.addOption("ut","Burnt Orange & White");
        radio.addOption(true,"tamu","Maroon & White");
        radio.addOption("ttu","Tech Red & Black");
        radio.addOption("baylor","Green & Gold");
        radio.addOption("rice","Blue & Gray");
        radio.addOption("uh","Scarlet Red & Albino White");
        p.addContent(", embedded in a paragraph.");

        // Select (multiple)
        p = unsuited.addPara();
		p.addContent("This is multiple 'Select' field, ");
        select = p.addSelect("multi-select");
        select.setLabel("Select (multiple)");
        select.setMultiple();
        select.setSize(4);
        if (help)
        	select.setHelp("Select one or more options");
        if (error)
        	select.addError("This field is in error.");
        select.addOption("one","uno");
        select.addOption("two","dos");
        select.addOption("three","tres");
        select.addOption("four","cuatro");
        select.addOption("five","cinco");
        select.setOptionSelected("one");
        select.setOptionSelected("three"); 
        select.setOptionSelected("five");
        p.addContent(", embedded in a paragraph.");
        
        // Composite
        p = unsuited.addPara();
		p.addContent("This is a 'Composite' field of two text fields, ");
        Composite composite = p.addComposite("composite-2text");
        composite.setLabel("Composite (two text fields)");
        if (help)
        	composite.setHelp("I am the help for the entire composite");
        if (error)
        	composite.addError("Just the composite is in error");
        text = composite.addText("partA");
        text.setLabel("Part A");
        text.setValue("Value for part A");
        if (help)
        	text.setHelp("Part A");
        text = composite.addText("partB");
        text.setLabel("Part B");
        text.setValue("Value for part B");
        if (help)
        	text.setHelp("Part B");
        p.addContent(", embedded in a paragraph.");
        
        
        
    }
    
    /**
     * Helpfull method to generate the return url to this page given the 
     * error & help parameters.
     */
    private String makeURL(boolean help, boolean error)
	{
		if (help && error)
			return "?help&error";
		
		if (help)
			return "?help";
		
		if (error)
			return "?error";
		
		return "?neither";
	}
}
