/*
 * AdvancedFormTest.java
 *
 * Version: $Revision: 1.3 $
 *
 * Date: $Date: 2006/07/13 15:18:14 $
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
import org.dspace.app.xmlui.wing.element.Composite;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.app.xmlui.wing.element.Instance;
import org.dspace.authorize.AuthorizeException;
import org.xml.sax.SAXException;

/**
 * This is a class to test the advanced form capabilities of DRI. 
 * All the fields on this page will either be composite or have 
 * multiple instances.
 * 
 * This class is not internationalized because it is never intended
 * to be used in production. It is merely a tool to aid developers of
 * aspects and themes.
 * 
 * @author Scott Phillips
 */
public class AdvancedFormTest extends AbstractDSpaceTransformer {

	public void addPageMeta(PageMeta pageMeta) throws SAXException,
			WingException, UIException, SQLException, IOException,
			AuthorizeException {
		pageMeta.addMetadata("title").addContent("Advanced Form Test");

		pageMeta.addTrailLink(contextPath + "/", "DSpace Home");
		pageMeta.addTrail().addContent("Advanced Form Test");
	}

	public void addBody(Body body) throws SAXException, WingException,
			UIException, SQLException, IOException, AuthorizeException {
		
		Request request = ObjectModelHelper.getRequest(objectModel);
		boolean help = false, error = false;
		if (request.getParameter("help") != null)
			help = true;
		if (request.getParameter("error") != null)
			error = true;
		
		Division div = body.addInteractiveDivision("test", "", "post", "primary");
		div.setHead("Advanced form test");
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
		list.setHead("Tests");
		
		// text
		Text text = list.addItem().addText("text");
		text.setLabel("Text");
		text.enableAddOperation();
		text.enableDeleteOperation();
		if (help)
			text.setHelp("This is helpfull text.");
		if (error)
			text.addError("This field is in error.");
		text.setValue("First is special");
		Instance instance = text.addInstance();
		instance.setValue("Second raw");
		instance.setInterpretedValue("Second interpreted");
		instance = text.addInstance();
		instance.setValue("Third raw");
		instance.setInterpretedValue("Third interpreted");

		// Select
		Select select = list.addItem().addSelect("select");
		select.setLabel("Text");
		select.enableAddOperation();
		select.enableDeleteOperation();
		select.setMultiple();
		select.setSize(4);
		if (help)
			select.setHelp("This is helpfull text.");
		if (error)
			select.addError("This field is in error.");
		select.addOption("one", "uno");
		select.addOption("two", "dos");
		select.addOption("three", "tres");
		select.addOption("four", "cuatro");
		select.addOption("five", "cinco");
		
		instance = select.addInstance();
		instance.setOptionSelected("one");
		
		instance = select.addInstance();
		instance.setOptionSelected("one");
		instance.setOptionSelected("two");
		
		instance = select.addInstance();
		instance.setOptionSelected("one");
		instance.setOptionSelected("two");
		instance.setOptionSelected("three");
		
		instance = select.addInstance();
		instance.setOptionSelected("one");
		instance.setOptionSelected("two");
		instance.setOptionSelected("three");
		instance.setOptionSelected("four");
		
		instance = select.addInstance();
		instance.setOptionSelected("one");
		instance.setOptionSelected("two");
		instance.setOptionSelected("three");
		instance.setOptionSelected("four");
		instance.setOptionSelected("five");
		

        // composite two text fields   
        Composite composite = list.addItem().addComposite("compositeA");
        composite.setLabel("Composite (two text fields)");
        composite.enableAddOperation();
        composite.enableDeleteOperation();
        if (help)
        	composite.setHelp("This field is composed of two text fields, fill them both in.");
        if (error)
        	composite.addError("Just the composite is in error.");
        text = composite.addText("firstA");
        if (help)
        	text.setHelp("This is helpfull text.");
        text.addInstance().setValue("1, Raw A");
        text.addInstance().setValue("2, Raw A");
        text.addInstance().setValue("3, Raw A");
        
        text = composite.addText("secondA");
        if (help)
        	text.setHelp("This is helpfull text.");
        text.addInstance().setValue("1, Raw B");
        text.addInstance().setValue("2, Raw B");
        text.addInstance().setValue("3, Raw B");
        
        // composite select & text fields
        composite = list.addItem().addComposite("compositeB");
        composite.setLabel("Composite (select & text fields)");
        composite.enableAddOperation();
        composite.enableDeleteOperation();
        if (help)
        	composite.setHelp("This field is composed of a select and text field, select one and type the other.");
  
        select = composite.addSelect("selectB");
        if (help)
        	select.setHelp("Me, me, me..... select me!");
        if (error)
        	select.addError("The composite elements are in error.");
        select.addOption("one","uno");
        select.addOption("two","dos");
        select.addOption("three","tres");
        select.addOption("four","cuatro");
        select.addOption("five","cinco");
        select.setOptionSelected("one");
        
        select.addInstance().addOptionValue("one");
        select.addInstance().addOptionValue("two");
        select.addInstance().addOptionValue("three");
        
        text = composite.addText("TextB");
        if (help)
        	text.setHelp("Yay, yet another text field");
        if (error)
        	text.addError("The composite elements are in error.");
        text.addInstance().setValue("1, Raw B");
        text.addInstance().setValue("2, Raw B");
        text.addInstance().setValue("3, Raw B");
        
        composite.addInstance().setInterpretedValue("One interpreted.");
        composite.addInstance().setInterpretedValue("Two interpreted.");
        composite.addInstance().setInterpretedValue("Three interpreted.");
        
        // Composite (date)
        composite = list.addItem().addComposite("composite-date");
        composite.setLabel("Composite (date)");
        composite.enableAddOperation();
        composite.enableDeleteOperation();
        if (help)
        	composite.setHelp("The date when something happened.");
        if (error)
        	composite.setHelp("The composite is in error.");
        
        text = composite.addText("day");
        if (help)
        	text.setHelp("day");
        if (error)
        	text.setHelp("The first text field is in error.");
        text.setSize(4,2);
        
        text.addInstance().setValue("1");
        text.addInstance().setValue("2");
        text.addInstance().setValue("3");
        text.addInstance().setValue("4");
        text.addInstance().setValue("5");
        
        select = composite.addSelect("month");
        if (error)
        	select.setHelp("The select box is in error.");
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
        
        select.addInstance().setOptionSelected(1);
        select.addInstance().setOptionSelected(2);
        select.addInstance().setOptionSelected(3);
        select.addInstance().setOptionSelected(4);
        select.addInstance().setOptionSelected(5);
        
        text = composite.addText("year");
        text.setSize(4,4);
        if (help)
        	text.setHelp("year");
        if (error)
        	text.setHelp("The second text field is in error.");
        text.addInstance().setValue("2001");
        text.addInstance().setValue("2002");
        text.addInstance().setValue("2003");
        text.addInstance().setValue("2004");
        text.addInstance().setValue("2005");
        
        // Buttons one typical finds at the end of forums
        Item actions = list.addItem();
        actions.addButton("submit_save").setValue("Save");
        actions.addButton("submit_cancel").setValue("Cancel");
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
