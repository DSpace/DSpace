/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
/*
 * Utility Javascript methods for DSpace
 */

// Popup window - here so it can be referred to by several methods
var popupWindow;

// =========================================================
//  Methods for e-person popup window
// =========================================================

// Add to list of e-people on this page -- invoked by eperson popup window
function addEPerson(id, email, name)
{
    var newplace = window.document.epersongroup.eperson_id.options.length;

    if (newplace > 0 && window.document.epersongroup.eperson_id.options[0].value == "")
    {
        newplace = 0;
    }

    // First we check to see if e-person is already there
    for (var i = 0; i < window.document.epersongroup.eperson_id.options.length; i++)
    {
        if (window.document.epersongroup.eperson_id.options[i].value == id)
        {
            newplace = -1;
        }
    }

    if (newplace > -1)
    {
        window.document.epersongroup.eperson_id.options[newplace] = new Option(name + " (" + email + ")", id);
    }
}

// Add to list of groups on this page -- invoked by eperson popup window
function addGroup(id, name)
{
    var newplace = window.document.epersongroup.group_ids.options.length;

	if (newplace > 0 && window.document.epersongroup.group_ids.options[0].value == "")
    {
        newplace = 0;
    }

    // First we check to see if group is already there
    for (var i = 0; i < window.document.epersongroup.group_ids.options.length; i++)
    {
        // is it in the list already
        if (window.document.epersongroup.group_ids.options[i].value == id)
        {
            newplace = -1;
        }

        // are we trying to add the new group to the new group on an Edit Group page (recursive)
        if (window.document.epersongroup.group_id)
        {
            if (window.document.epersongroup.group_id.value == id)
            {
                newplace = -1;
            }
        }
    }

    if (newplace > -1)
    {
        window.document.epersongroup.group_ids.options[newplace] = new Option(name + " (" + id + ")", id);
    }
}

// This needs to be invoked in the 'onClick' javascript event for buttons
// on pages with a dspace:selecteperson element in them
function finishEPerson()
{
    selectAll(window.document.epersongroup.eperson_id);

	if (popupWindow != null)
	{
		popupWindow.close();
	}
}

// This needs to be invoked in the 'onClick' javascript event for buttons
// on pages with a dspace:selecteperson element in them
function finishGroups()
{
    selectAll(window.document.epersongroup.group_ids);

    if (popupWindow != null)
    {
		popupWindow.close();
    }
}

// =========================================================
//  Miscellaneous utility methods
// =========================================================

// Open a popup window (or bring to front if already open)
function popup_window(winURL, winName)
{
    var props = 'scrollBars=yes,resizable=yes,toolbar=no,menubar=no,location=no,directories=no,width=640,height=480';
    popupWindow = window.open(winURL, winName, props);
    popupWindow.focus();
}


// Select all options in a <SELECT> list
function selectAll(sourceList)
{
    for(var i = 0; i < sourceList.options.length; i++)
    {
        if ((sourceList.options[i] != null) && (sourceList.options[i].value != ""))
            sourceList.options[i].selected = true;
    }
    return true;
}

// Deletes the selected options from supplied <SELECT> list
function removeSelected(sourceList)
{
    var maxCnt = sourceList.options.length;
    for(var i = maxCnt - 1; i >= 0; i--)
    {
        if ((sourceList.options[i] != null) && (sourceList.options[i].selected == true))
        {
            sourceList.options[i] = null;
        }
    }
}


// Disables accidentally submitting a form when the "Enter" key is pressed.
// Just add "onkeydown='return disableEnterKey(event);'" to form.
function disableEnterKey(e)
{
     var key;

     if(window.event)
          key = window.event.keyCode;     //Internet Explorer
     else
          key = e.which;     //Firefox & Netscape

     if(key == 13)  //if "Enter" pressed, then disable!
          return false;
     else
          return true;
}


//******************************************************
// Functions used by controlled vocabulary add-on
// There might be overlaping with existing functions
//******************************************************

function expandCollapse(node, contextPath) {
	node = node.parentNode;
	var childNode  = (node.getElementsByTagName("ul"))[0];

	if(!childNode) return false;

	var image = node.getElementsByTagName("img")[0];
	
	if(childNode.style.display != "block") {
		childNode.style.display  = "block";
		image.src = contextPath + "/image/controlledvocabulary/m.gif";
		image.alt = "Collapse search term category";
	} else {
		childNode.style.display  = "none";
		image.src = contextPath + "/image/controlledvocabulary/p.gif";
		image.alt = "Expand search term category";
	}
	
	return false;
}


function getAnchorText(ahref) {
 	if(isMicrosoft()) return ahref.childNodes.item(0).nodeValue;
	else return ahref.text;
}

function getTextValue(node) {
 	if(node.nodeName == "A") {
 		return getAnchorText(node);
 	} else {
 		return "";
 	}
 	
}


function getParentTextNode(node) {
	var parentNode = node.parentNode.parentNode.parentNode;
	var children = parentNode.childNodes;
	var textNode;
	for(var i=0; i< children.length; i++) {
		var child = children.item(i);
		if(child.className == "value") {
			return child;
		}
	}
	return null;
}

function ec(node, contextPath) {
	expandCollapse(node, contextPath);
	return false;
}


function i(node) {
	return sendBackToParentWindow(node);
}


function getChildrenByTagName(rootNode, tagName) {
	var children = rootNode.childNodes;
	var result = new Array(0);
	if(children == null) return result;
	for(var i=0; i<children.length; i++) {
		if(children[i].tagName == tagName) {
			var elementArray = new Array(1);
			elementArray[0] = children[i];
			result = result.concat(elementArray);
		}
	}
	return result;
}

function popUp(URL) {
	var page;
	page = window.open(URL, 'controlledvocabulary', 'toolbar=0,scrollbars=1,location=0,statusbar=0,menubar=0,resizable=1,width=650,height=450');
}


function isNetscape(v) {
		  return isBrowser("Netscape", v);
}
	
function isMicrosoft(v) {
		  return isBrowser("Microsoft", v);
}

function isMicrosoft() {
		  return isBrowser("Microsoft", 0);
}


function isBrowser(b,v) {
		  browserOk = false;
		  versionOk = false;

		  browserOk = (navigator.appName.indexOf(b) != -1);
		  if (v == 0) versionOk = true;
		  else  versionOk = (v <= parseInt(navigator.appVersion));
		  return browserOk && versionOk;
}

//FUNCTIONS FOR THE ADVANCED SEARCH EFFECTS////////////////////////////////

function arrayContains(a, obj) {
    var i = a.length;
    while (i--) {
       if (a[i] === (obj+'')) {
           return true;
       }
    }
    return false;
}

function onFieldChange(searchLabel, dateIndices) {
	var indices = dateIndices.split('_');
	var selectedIndex = document.getElementById('tfield1').selectedIndex;
	if (arrayContains(indices, selectedIndex)){
		document.getElementById('tfield4_column').style.display = "block";
		document.getElementById('tfield4_label').style.display = "block";
		document.getElementById('tfield4').style.display = "block";	
	} else {
		document.getElementById('tfield4_column').style.display = "none";
		document.getElementById('tfield4_label').style.display = "none";
		document.getElementById('tfield4').style.display = "none";
		document.getElementById('tquery1_label').innerHTML = searchLabel;
		document.getElementById('tquery4_label').style.display = "none";
		document.getElementById('tquery4').style.display = "none";
		document.getElementById('date_row').setAttribute('height','25');
		document.getElementById('tfield4').selectedIndex = 0;
		document.getElementById('tfield4').options[0].selected = true;
		document.getElementById('tquery4').value = "";
	}	
	
	if (selectedIndex != 1) {
		var selectedIndex2 = document.getElementById('tfield2').selectedIndex;
		var selectedIndex3 = document.getElementById('tfield3').selectedIndex;
		if (selectedIndex2 != 1 && selectedIndex3 != 1){
			//document.getElementById('radio1').checked = true;
			//document.getElementById('radio2').checked = false;
			//document.getElementById('radio1').disabled = false;
			//document.getElementById('radio2').disabled = false;
		}
	}
}

function onFieldChange2(dateIndices){
	var indices = dateIndices.split('_');
	var selectedIndex = document.getElementById('tfield2').selectedIndex;
	if (arrayContains(indices, selectedIndex)){
		document.getElementById('tfield5_column').style.display = "block";
		document.getElementById('tfield5_label').style.display = "none";
		document.getElementById('tfield5').style.display = "block";
	} else {
		document.getElementById('tfield5_column').style.display = "none";
		document.getElementById('tfield5_label').style.display = "none";
		document.getElementById('tfield5').style.display = "none";
		document.getElementById('tquery2_label').style.display = "none";
		document.getElementById('tquery5_label').style.display = "none";
		document.getElementById('tquery5').style.display = "none";
		document.getElementById('date_row2').setAttribute('height','25');
		document.getElementById('tfield5').selectedIndex = 0;
		document.getElementById('tfield5').options[0].selected = true;
		document.getElementById('tquery5').value = "";
	}

	if (selectedIndex != 1) {
		var selectedIndex2 = document.getElementById('tfield1').selectedIndex;
		var selectedIndex3 = document.getElementById('tfield3').selectedIndex;
		if (selectedIndex2 != 1 && selectedIndex3 != 1){
			//document.getElementById('radio1').checked = true;
			//document.getElementById('radio2').checked = false;
			//document.getElementById('radio1').disabled = false;
			//document.getElementById('radio2').disabled = false;
		}
	}
}

function onFieldChange3(dateIndices) {
	var indices = dateIndices.split('_');
	var selectedIndex = document.getElementById('tfield3').selectedIndex;
	if (arrayContains(indices, selectedIndex)){
		document.getElementById('tfield6_column').style.display = "block";
		document.getElementById('tfield6_label').style.display = "none";
		document.getElementById('tfield6').style.display = "block";
	}
	else{
		document.getElementById('tfield6_column').style.display = "none";
		document.getElementById('tfield6_label').style.display = "none";
		document.getElementById('tfield6').style.display = "none";
		document.getElementById('tquery3_label').style.display = "none";
		document.getElementById('tquery6_label').style.display = "none";
		document.getElementById('tquery6').style.display = "none";
		document.getElementById('date_row3').setAttribute('height','25');
		document.getElementById('tfield6').selectedIndex = 0;
		document.getElementById('tfield6').options[0].selected = true;
		document.getElementById('tquery6').value = "";
	}

	if (selectedIndex != 1)
	{
		var selectedIndex2 = document.getElementById('tfield1').selectedIndex;
		var selectedIndex3 = document.getElementById('tfield2').selectedIndex;
		if (selectedIndex2 != 1 && selectedIndex3 != 1){
			//document.getElementById('radio1').checked = true;
			//document.getElementById('radio2').checked = false;
			//document.getElementById('radio1').disabled = false;
			//document.getElementById('radio2').disabled = false;
		}
	}
}

function onOptionChange(fromLabel, searchLabel){
	var selectedIndex = document.getElementById('tfield4').selectedIndex;
	if (selectedIndex == 3){
		document.getElementById('tquery1_label').innerHTML = fromLabel;
		document.getElementById('tquery4_label').style.display = "block";
		document.getElementById('tquery4').style.display = "block";
		document.getElementById('date_row').setAttribute('height','50');
	} else {
		document.getElementById('tquery1_label').innerHTML = searchLabel;
		document.getElementById('tquery4_label').style.display = "none";
		document.getElementById('tquery4').style.display = "none";
		document.getElementById('date_row').setAttribute('height','25');
		document.getElementById('tquery4').value = "";
	}	
}

function onOptionChange2(fromLabel, searchLabel){
	var selectedIndex = document.getElementById('tfield5').selectedIndex;
	if (selectedIndex == 3){
		document.getElementById('tquery2_label').innerHTML = fromLabel;
		document.getElementById('tquery2_label').style.display = "block";
		document.getElementById('tquery5_label').style.display = "block";
		document.getElementById('tquery5').style.display = "block";
		document.getElementById('date_row2').setAttribute('height','50');
	}
	else{
		document.getElementById('tquery2_label').style.display = "none";
		document.getElementById('tquery5_label').style.display = "none";
		document.getElementById('tquery5').style.display = "none";
		document.getElementById('date_row2').setAttribute('height','25');
		document.getElementById('tquery5').value = "";
	}	
}

function onOptionChange3(fromLabel, searchLabel){
	var selectedIndex = document.getElementById('tfield6').selectedIndex;
	if (selectedIndex == 3){
		document.getElementById('tquery3_label').innerHTML = fromLabel;
		document.getElementById('tquery3_label').style.display = "block";
		document.getElementById('tquery6_label').style.display = "block";
		document.getElementById('tquery6').style.display = "block";
		document.getElementById('date_row3').setAttribute('height','50');
	}
	else{
		document.getElementById('tquery3_label').style.display = "none";
		document.getElementById('tquery6_label').style.display = "none";
		document.getElementById('tquery6').style.display = "none";
		document.getElementById('date_row3').setAttribute('height','25');
		document.getElementById('tquery6').value = "";
	}	
}

function validateDateNumber(check, dateIndices) {
	var indices = dateIndices.split('_');
	if (arrayContains(indices, document.getElementById('tfield1').selectedIndex)){
		var date1 = document.getElementById('tquery1').value;
		var date2 = document.getElementById('tquery4').value;
		if (!isNaN(date1) && !isNaN(date2)){
			document.getElementById('error_log').style.display = "none";
			document.getElementById('adv_search_submit').disabled = false;
			if (check){
				validateDateNumber2(false);
				validateDateNumber3(false);	
			}
		}
		else{
			document.getElementById('error_log').innerHTML = "Please, insert a valid year!";
			document.getElementById('error_log').style.display = "block";
			document.getElementById('adv_search_submit').disabled = true;
		}
	}
}

function validateDateNumber2(check, dateIndices){
	var indices = dateIndices.split('_');
	if (arrayContains(indices, document.getElementById('tfield2').selectedIndex)){
		var date1 = document.getElementById('tquery2').value;
		var date2 = document.getElementById('tquery5').value;
		if (!isNaN(date1) && !isNaN(date2)){
			document.getElementById('error_log').style.display = "none";
			document.getElementById('adv_search_submit').disabled = false;
			if (check){
				validateDateNumber(false);
				validateDateNumber3(false);
			}
		}
		else{
			document.getElementById('error_log').innerHTML = "Please, insert a valid year!";
			document.getElementById('error_log').style.display = "block";
			document.getElementById('adv_search_submit').disabled = true;
		}
	}	
}

function validateDateNumber3(check, dateIndices){
	var indices = dateIndices.split('_');
	if (arrayContains(indices, document.getElementById('tfield3').selectedIndex)){
		var date1 = document.getElementById('tquery3').value;
		var date2 = document.getElementById('tquery6').value;
		if (!isNaN(date1) && !isNaN(date2)){
			document.getElementById('error_log').style.display = "none";
			document.getElementById('adv_search_submit').disabled = false;
			if (check){
				validateDateNumber(false);
				validateDateNumber2(false);
			}
		}
		else{
			document.getElementById('error_log').innerHTML = "Please, insert a valid year!";
			document.getElementById('error_log').style.display = "block";
			document.getElementById('adv_search_submit').disabled = true;
		}
	}	
}
//////////////////////////////////////////////////////////////////////////

