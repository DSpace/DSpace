/*
 * BasicFormTest.java
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
import org.dspace.app.xmlui.wing.element.Hidden;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Password;
import org.dspace.app.xmlui.wing.element.Radio;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.app.xmlui.wing.element.TextArea;
import org.dspace.authorize.AuthorizeException;
import org.xml.sax.SAXException;

/**
 * This is a class to test the basic form capabilities of DRI. All
 * the fields used here will be simple and only used inside the
 * context of a form.
 * 
 * This class is not internationalized because it is never intended
 * to be used in production. It is merely a tool to aid developers of
 * aspects and themes.
 * 
 * @author Scott Phillips
 */
public class BasicFormTest extends AbstractDSpaceTransformer
{
    
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
        pageMeta.addMetadata("title").addContent("Basic Form Test");
       
        pageMeta.addTrailLink(contextPath + "/","DSpace Home");
        pageMeta.addTrail().addContent("Basic form test");
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
        div.setHead("Basic form test");
        div.addPara("There are two options you can use to control how this page is generated. First is the help parameter, if this is present then help text will be provided for all fields. Next is the error parameter, if it is provided then all fields will be generated in error conditions.");
		
		if (help)
			div.addPara().addXref(makeURL(false,error),"Turn help OFF");
		else 
			div.addPara().addXref(makeURL(true,error),"Turn help ON");
			
		if (error)
			div.addPara().addXref(makeURL(help,false),"Turn errors OFF");
		else 
			div.addPara().addXref(makeURL(help,true),"Turn errors ON");
		

        List list = div.addList("fieldTest",List.TYPE_FORM);
        list.setHead("Fields");
        
        // Text field
        Text text = list.addItem().addText("text");
        text.setLabel("Text");
        if (help)
        	text.setHelp("This is helpfull text.");
        if (error)
        	text.addError("This field is in error.");
        text.setValue("Current raw value");
        
        // Long help
        Text longHelp = list.addItem().addText("longHelp");
        longHelp.setLabel("Long Help");
        if (help)
        	longHelp.setHelp("This is a really long help message. It could potentially be a paragraph of material, really really long. Actually we don't know how long it can be because there is no upper limit on it! Although if you do find your self adding a long help message consider whether your user will actually read any of this, my bet is that they won't. However we still need to support these really, really, really, long messages that may break across multiple lines!");
        if (error)
        	longHelp.addError("This field is in error.");
        longHelp.setValue("Current raw value");
        
        // Long error
        Text longError = list.addItem().addText("longError");
        longError.setLabel("Long Error");
        if (help)
        	longError.setHelp("TThis is helpfull text.");
        if (error)
        	longError.addError("This field is very much is serious trouble, it's so horrible wrong that i now have to give you a very long stern message that may break across multiple lines! To fix this problem you should examine what you are attempting to do and consider other factors like what might have lead you to this path vs another path. Are you sure you even want this field or might another one work just as well?");
        longError.setValue("Current raw value");
        
        // Text Area Field
        TextArea textArea = list.addItem().addTextArea("textarea");
        textArea.setLabel("Text Area");
        if (help)
        	textArea.setHelp("This is helpfull text.");
        if (error)
        	textArea.addError("This field is in error.");
        textArea.setValue("This is the raw value");
        
        // Blank Text Area Field
        TextArea emptyTextArea = list.addItem().addTextArea("emptyTextarea");
        emptyTextArea.setLabel("Empty Text Area");
        if (help)
        	emptyTextArea.setHelp("This is helpfull text.");
        if (error)
        	emptyTextArea.addError("This field is in error.");
        
        
        // Password field
        Password password = list.addItem().addPassword("password");
        password.setLabel("password");
        if (help)
        	password.setHelp("This is helpfull text.");
        if (error)
        	password.addError("This field is in error.");
        
        // Hidden field
        Hidden hidden = list.addItem().addHidden("hidden");
        hidden.setLabel("Hidden");
        hidden.setValue("You can not see this.");
        if (help)
        	hidden.setHelp("This is hidden help?");
        if (error)
        	hidden.addError("This a hidden error - I have no idea what this means?");
        
        // Checkbox field
        CheckBox checkBox = list.addItem().addCheckBox("fruit");
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
        
        // Radio buttons
        
        Radio radio = list.addItem().addRadio("sex");
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
        
        // File
        File file = list.addItem().addFile("file");
        file.setLabel("File");
        if (help)
        	file.setHelp("Upload a file.");
        if (error)
        	file.addError("This field is in error.");
        
        // Select (single)
        Select select = list.addItem().addSelect("select");
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

        select = list.addItem().addSelect("multi-select");
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
        
        // Non-Field-item
        list.addLabel("Non-Field");
        list.addItem().addContent("This is just text, not a field, but it has a list label.");
        
        // Button
        Button button = list.addItem().addButton("button");
        button.setLabel("Button");
        button.setValue("When you touch me I do things, lots of things");
        if (help)
        	button.setHelp("Submit buttons allow the user to submit the form.");
        if (error)
        	button.addError("This button is in error.");
        
        // Non-field-unlabeled-item
        list.addItem().addContent("The following fields are all various use cases of composites. Also note that this item is an item inside a list of type form that 1) does not contain a field and 2) does not have a label.");
        
        // Composite
        Composite composite = list.addItem().addComposite("composite-2text");
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
        
        // composite select & text fields  
        composite = list.addItem().addComposite("compositeB");
        composite.setLabel("Composite (select & text fields)");
        if (help)
        	composite.setHelp("This field is composed of a select and text field, select one and type the other.");
  
        select = composite.addSelect("selectB");
        select.setLabel("Numbers");
        if (help)
        	select.setHelp("Me, me, me..... select me!");
        if (error)
        	select.addError("The composite components are in error.");
        select.addOption("one","uno");
        select.addOption("two","dos");
        select.addOption("three","tres");
        select.addOption("four","cuatro");
        select.addOption("five","cinco");
        select.setOptionSelected("one");
        
        text = composite.addText("TextB");
        text.setLabel("Spanish Numbers");
        if (help)
        	text.setHelp("Yay, yet another text field");
        if (error)
        	text.addError("The composite components are in error.");
        
        
        // Composite 
        composite = list.addItem().addComposite("composite-date");
        composite.setLabel("Composite (date)");
        if (help)
        	composite.setHelp("The data the item was published.");
        if (error)
        	composite.addError("The date is in error.");
        
        text = composite.addText("year");
        text.setLabel("Year");
        text.setSize(4,4);
        if (help)
        	text.setHelp("year");
        if (error)
        	text.addError("The year is in error");
        
        
        select = composite.addSelect("month");
        select.setLabel("Month");
        if (error)
        	select.addError("The month is in error");
        if (help)
        	text.setHelp("month");
        select.addOption("","(Select Month)");
        select.addOption(1,"January");
        select.addOption(2,"Feburary");
        select.addOption(3,"March");
        select.addOption(4,"April");
        select.addOption(5,"May");
        select.addOption(6,"June");
        select.addOption(7,"July");
        select.addOption(8,"August");
        select.addOption(9,"September");
        select.addOption(10,"August");
        select.addOption(11,"October");
        select.addOption(12,"November");
        select.addOption(13,"December");
        
        text = composite.addText("day");
        text.setLabel("Day");
        if (help)
        	text.setHelp("day");
        if (error)
        	text.addError("The day is in error.");
        text.setSize(4,2);
        
        // Buttons one typical finds at the end of forums
        Item actions = list.addItem();
        actions.addButton("submit_save").setValue("Save");
        actions.addButton("submit_cancel").setValue("Cancel");
        
        
        
        
        /////////////////////////////////////////////////
        /// Multi section 
        ////////////////////////////////////////////////
        
        div.addPara("This next test will use form sections. Sections are logical groupings of related fields that together form the entire set.");
        
        list = div.addList("sectionTest",List.TYPE_FORM);
        list.setHead("Multi-Section form");
        List identity = list.addList("identity",List.TYPE_FORM);
        identity.setHead("Identity");
        
        Text name = identity.addItem().addText("name");
        name.setLabel("Username");
        if (help)
        	name.setHelp("The username you wish to chooose");
        if (error)
        	name.addError("Sorry, that username is allready used by another user.");
        
        Composite ssn = identity.addItem().addComposite("ssn");
        ssn.setLabel("SSN");
        if (help)
        	ssn.setHelp("Your Social Security Number, really we won't use it for anything bad.... you can trust me.");
        if (error)
        	ssn.addError("The SSN you entered is invalid.");
        Text ssn1 = ssn.addText("ssn1");
        ssn1.setSize(4,4);
        Text ssn2 = ssn.addText("ssn2");
        ssn2.setSize(2,2);
        Text ssn3 = ssn.addText("ssn3");
        ssn3.setSize(4,4);
        
        List intrests = list.addList("intrests",List.TYPE_FORM);
        intrests.setHead("Intrests");
        
        CheckBox intrest = intrests.addItem().addCheckBox("intrests");
        intrest.setLabel("Intrests");
        if (help)
        	intrest.setHelp("Select all topics which are of intrests to you.");
        if (error)
        	intrest.addError("You're intrests are in error?");
        intrest.addOption("DL","Digital Libraries");
        intrest.addOption("HT","Hypertexts");
        intrest.addOption("IM","Information Managment");
        intrest.addOption("ID","Information Discovery");
        intrest.addOption("SI","Social Impact");
        
        List affiliation = list.addList("affiliation",List.TYPE_FORM);
        affiliation.setHead("Affiliation");
        
        Text institution = affiliation.addItem().addText("institution");
        institution.setLabel("Institution");
        if (help)
        	name.setHelp("The institution you are affiliated with");
        if (error)
        	name.addError("That institution is an invalid option.");
        
        Radio geography = affiliation.addItem().addRadio("geography");
        geography.setLabel("Geography");
        if (help)
        	geography.setHelp("Select your institution's geographical region");
        if (error)
        	geography.addError("Your entry is invalid.");
        geography.addOption("na","North America");
        geography.addOption("sa","South America");
        geography.addOption("eu","Europe");
        geography.addOption("af","Africa");
        geography.addOption("ai","Asia");
        geography.addOption("pi","Pacific Island");  
        geography.addOption("an","Antarctica");
        
        Item buttons = list.addItem();
        buttons.addButton("submit_save2").setValue("Save");
        buttons.addButton("submit_cancel2").setValue("Cancel");
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
