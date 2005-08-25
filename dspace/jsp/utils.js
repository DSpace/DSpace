/*
 * utils.js
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2004, Hewlett-Packard Company and Massachusetts
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
    var newplace = window.document.forms[0].eperson_id.options.length;

    if (newplace > 0 && window.document.forms[0].eperson_id.options[0].text == "")
    {
        newplace = 0;
    }

    // First we check to see if e-person is already there
    for (var i = 0; i < window.document.forms[0].eperson_id.options.length; i++)
    {
        if (window.document.forms[0].eperson_id.options[i].value == id)
        {
            newplace = -1;
        }
    }

    if (newplace > -1)
    {
        window.document.forms[0].eperson_id.options[newplace] = new Option(name + " (" + email + ")", id);
    }
}

// This needs to be invoked in the 'onClick' javascript event for buttons
// on pages with a dspace:selecteperson element in them
function finishEPerson()
{
    selectAll(window.document.forms[0].eperson_id);

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
        if (sourceList.options[i] != null)
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
