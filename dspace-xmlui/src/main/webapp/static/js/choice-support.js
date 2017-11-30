/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
// Client-side scripting to support DSpace Choice Control

// IMPORTANT NOTE:
//  This code depends on a *MODIFIED* version of the
//  script.aculo.us controls v1.8.2, fixed to avoid a bug that
//  affects autocomplete in Firefox 3.  This code is included in DSpace.

// Entry points:
//  1. DSpaceAutocomplete -- add autocomplete (suggest) to an input field
//
//  2.  DSpaceChoiceLookup -- create popup window with authority choices
//
//  @Author: Larry Stone  <lcs@hulmail.harvard.edu>
//  $Revision $

// -------------------- support for Autocomplete (Suggest)

// Autocomplete utility:
// Arguments:
//   formID -- ID attribute of form tag
//   args properties:
//     metadataField -- metadata field e.g. dc_contributor_author
//     inputName -- input field name for text input, or base of "Name" pair
//     authorityName -- input field name in which to set authority
//     containerID -- ID attribute of DIV to hold the menu objects
//     indicatorID -- ID attribute of element to use as a "loading" indicator
//     confidenceIndicatorID -- ID of element on which to set confidence
//     confidenceName - NAME of confidence input (not ID)
//     contextPath -- URL path prefix (i.e. webapp contextPath) for DSpace.
//     collection -- db ID of dspace collection to serve as context
//     isClosed -- true if authority value is required, false = non-auth allowed
// XXX Can't really enforce "isClosed=true" with autocomplete, user can type anything
//
// NOTE: Successful autocomplete always sets confidence to 'accepted' since
//  authority value (if any) *was* chosen interactively by a human.
function DSpaceSetupAutocomplete(formID, args)
{
    if (args.authorityName == null)
        args.authorityName = dspace_makeFieldInput(args.inputName,'_authority');
    var form = document.getElementById(formID);
    var inputID = form.elements[args.inputName].id;
    var authorityID = null;
    if (form.elements[args.authorityName] != null)
        authorityID = form.elements[args.authorityName].id;

    // AJAX menu source, can add &query=TEXT
    var choiceURL = args.contextPath+"/choices/"+args.metadataField;

    var collID = args.collection == null ? -1 : args.collection;

    // field in whcih to store authority value
    var options =
      {
        // class name of spans that contain value in li elements
        select: "value",
        // make up query args for AJAX callback
        parameters: 'collection='+collID+'&format=ul',
        callback:
          function(inField, querystring) {
            return querystring+"&query="+inField.value;
          },
        // called after target field is updated
        afterUpdateElement:
          function(ignoreField, li)
            {
                // NOTE: lookup element late because it might not be in DOM
                // at the time we evaluate the function..
                var authInput = document.getElementById(authorityID);
                var authValue = li == null ? "" : li.getAttribute("authority");
                if (authInput != null)
                {
                    authInput.value = authValue;
                    // update confidence input's value too if available.
                    if (args.confidenceName != null)
                    {
                        var confInput = authInput.form.elements[args.confidenceName];
                        if (confInput != null)
                            confInput.value = 'accepted';
                    }
                }
                // make indicator blank if no authority value
                DSpaceUpdateConfidence(document, args.confidenceIndicatorID,
                    authValue == null || authValue == '' ? 'blank' :'accepted');
            }
      };

      if (args.indicatorID != null)
          options.indicator = args.indicatorID;

    // be sure to turn off autocomplete, it absorbs arrow-key events!
    form.elements[args.inputName].setAttribute("autocomplete", "off");

    new Ajax.Autocompleter(inputID, args.containerID, choiceURL, options);
}

// -------------------- support for Lookup Popup

// Create popup window with authority choices for value of an input field.
// This is intended to be called by onClick of a "Lookup" or "Add"  button.
function DSpaceChoiceLookup(url, field, formID, valueInput, authInput,
    confIndicatorID, collectionID, isName, isRepeating)
{
    // fill in URL
    url += '?field='+field+'&formID='+formID+'&valueInput='+valueInput+
             '&authorityInput='+authInput+'&collection='+collectionID+
             '&isName='+isName+'&isRepeating='+isRepeating+'&confIndicatorID='+confIndicatorID;

    // primary input field - for positioning popup.
    var inputFieldName = isName ? dspace_makeFieldInput(valueInput,'_last') : valueInput;
    var inputField = document.getElementById(formID).elements[inputFieldName];
    // scriptactulous magic to figure out true offset:
    var cOffset = 0;
    if (inputField != null)
        cOffset = $(inputField).cumulativeOffset();
    var width = 600;  // XXX guesses! these should be params, or configured..
    var height = 470;
    var left; var top;
    if (window.screenX == null) {
        left = window.screenLeft + cOffset.left - (width/2);
        top = window.screenTop + cOffset.top - (height/2);
    } else {
        left = window.screenX + cOffset.left - (width/2);
        top = window.screenY + cOffset.top - (height/2);
    }
    if (left < 0) left = 0;
    if (top < 0) top = 0;
    var pw = window.open(url, 'ignoreme',
         'width='+width+',height='+height+',left='+left+',top='+top+
         ',toolbar=no,menubar=no,location=no,status=no,resizable');
    if (window.focus) pw.focus();
    return false;
}

// Run this as the Lookup page is loaded to initialize DOM objects, load choices
function DSpaceChoicesSetup(form)
{
    // find the "LEGEND" in fieldset, which acts as page title,
    // and save it as a bogus form element, e.g. elements['statline']
    var fieldset = document.getElementById('aspect_general_ChoiceLookupTransformer_list_choicesList');
    for (i = 0; i < fieldset.childNodes.length; ++i)
    {
      if (fieldset.childNodes[i].nodeName == 'LEGEND')
      {
          form.statline = fieldset.childNodes[i];
          form.statline_template = fieldset.childNodes[i].innerHTML;
          fieldset.childNodes[i].innerHTML = "Loading...";
          break;
      }
    }
    DSpaceChoicesLoad(form);
}


// populate the "select" with options from ajax request
// stash some parameters as properties of the "select" so we can add to
// the last start index to query for next set of results.
function DSpaceChoicesLoad(form)
{
    var field = form.elements['paramField'].value;
    var value = form.elements['paramValue'].value;
    var start = form.elements['paramStart'].value;
    var limit = form.elements['paramLimit'].value;
    var formID = form.elements['paramFormID'].value;
    var collID = form.elements['paramCollection'].value;
    var isName = form.elements['paramIsName'].value == 'true';
    var isRepeating = form.elements['paramIsRepeating'].value == 'true';
    var isClosed = form.elements['paramIsClosed'].value == 'true';
    var contextPath = form.elements['contextPath'].value;
    var fail = form.elements['paramFail'].value;
    var valueInput = form.elements['paramValueInput'].value;
    var nonAuthority = "";
    if (form.elements['paramNonAuthority'] != null)
        nonAuthority = form.elements['paramNonAuthority'].value;

    // get value from form inputs in opener if not explicitly supplied
    if (value.length == 0)
    {
        var of = window.opener.document.getElementById(formID);
        if (isName)
            value = makePersonName(of.elements[dspace_makeFieldInput(valueInput,'_last')].value,
                                   of.elements[dspace_makeFieldInput(valueInput,'_first')].value);
        else
            value = of.elements[valueInput].value;

        // if this is a repeating input, clear the source value so that e.g.
        // clicking "Next" on a submit-describe page will not *add* the proposed
        // lookup text as a metadata value:
        if (isRepeating)
        {
            if (isName)
            {
                of.elements[dspace_makeFieldInput(valueInput,'_last')].value = null;
                of.elements[dspace_makeFieldInput(valueInput,'_first')].value = null;
            }
            else
                of.elements[valueInput].value = null;
        }
    }

    // start spinner
    var indicator = document.getElementById('lookup_indicator_id');
    if (indicator != null)
        indicator.style.display = "inline";

    new Ajax.Request(contextPath+"/choices/"+field,
      {
        method: "get",
        parameters: {query: value, format: 'select', collection: collID,
                     start: start, limit: limit},
        // triggered by any exception, even in success
        onException: function(req, e) {
          window.alert(fail+" Exception="+e);
          if (indicator != null) indicator.style.display = "none";
        },
        // when http load of choices fails
        onFailure: function() {
          window.alert(fail+" HTTP error resonse");
          if (indicator != null) indicator.style.display = "none";
        },
        // format is <select><option authority="key" value="val">label</option>...
        onSuccess: function(transport) {
          var ul = transport.responseXML.documentElement;
          var err = ul.getAttributeNode('error');
          if (err != null && err.value == 'true')
              window.alert(fail+" Server indicates error in response.");
          var opts = ul.getElementsByTagName('option');

          // update range message and update 'more' button
          var oldStart = 1 * ul.getAttributeNode('start').value;
          var nextStart = oldStart + opts.length;
          var lastTotal = ul.getAttributeNode('total').value;
          var resultMore = ul.getAttributeNode('more');
          form.elements['more'].disabled = !(resultMore != null && resultMore.value == 'true');
          form.elements['paramStart'].value = nextStart;

          // clear select first
          var select = form.elements['chooser'];
          for (var i = select.length-1; i >= 0; --i)
            select.remove(i);

          // load select and look for default selection
          var selectedByValue = -1; // select by value match
          var selectedByChoices = -1;  // Choice says its selected
          for (var i = 0; i < opts.length; ++i)
          {
            var opt = opts.item(i);
            var olabel = "";
            for (var j = 0; j < opt.childNodes.length; ++j)
            {
               var node = opt.childNodes[j];
               if (node.nodeName == "#text")
                 olabel += node.data;
            }
            var ovalue = opt.getAttributeNode('value').value;
            var option = new Option(olabel, ovalue);
            option.authority = opt.getAttributeNode('authority').value;
            select.add(option, null);
            if (value == ovalue)
                selectedByValue = select.options.length - 1;
            if (opt.getAttributeNode('selected') != null)
                selectedByChoices = select.options.length - 1;
          }
          // add non-authority option if needed.
          if (!isClosed)
          {
            select.add(new Option(dspace_formatMessage(nonAuthority, value), value), null);
          }
          var defaultSelected = -1;
          if (selectedByChoices >= 0)
            defaultSelected = selectedByChoices;
          else if (selectedByValue >= 0)
            defaultSelected = selectedByValue;
          else if (select.options.length == 1)
            defaultSelected = 0;

          // load default-selected value
          if (defaultSelected >= 0)
          {
            select.options[defaultSelected].defaultSelected = true;
            var so = select.options[defaultSelected];
            if (isName)
            {
                form.elements['text1'].value = lastNameOf(so.value);
                form.elements['text2'].value = firstNameOf(so.value);
            }
            else
                form.elements['text1'].value = so.value;
          }

          // turn off spinner
          if (indicator != null)
              indicator.style.display = "none";

          // "results" status line
          var statLast =  nextStart + (isClosed ? 2 : 1);

          form.statline.innerHTML =
            dspace_formatMessage(form.statline_template,
              oldStart+1, statLast, Math.max(lastTotal,statLast), value);
        }
      });
}

// handler for change event on choice selector - load new values
function DSpaceChoicesSelectOnChange ()
{
    // "this" is the window,
    var form = document.getElementById('aspect_general_ChoiceLookupTransformer_div_lookup');
    var select = form.elements['chooser'];
    var so = select.options[select.selectedIndex];
    var isName = form.elements['paramIsName'].value == 'true';

    if (isName)
    {
        form.elements['text1'].value = lastNameOf(so.value);
        form.elements['text2'].value = firstNameOf(so.value);
    }
    else
        form.elements['text1'].value = so.value;
}

// handler for lookup popup's accept (or add) button
//  stuff values back to calling page, force an add if necessary, and close.
function DSpaceChoicesAcceptOnClick ()
{
    var select = this.form.elements['chooser'];
    var isName = this.form.elements['paramIsName'].value == 'true';
    var isRepeating = this.form.elements['paramIsRepeating'].value == 'true';
    var valueInput = this.form.elements['paramValueInput'].value;
    var authorityInput = this.form.elements['paramAuthorityInput'].value;
    var formID = this.form.elements['paramFormID'].value;
    var confIndicatorID = this.form.elements['paramConfIndicatorID'] == null?null:this.form.elements['paramConfIndicatorID'].value;

    // default the authority input if not supplied.
    if (authorityInput.length == 0)
        authorityInput = dspace_makeFieldInput(valueInput,'_authority');

    // always stuff text fields back into caller's value input(s)
    if (valueInput.length > 0)
    {
        var of = window.opener.document.getElementById(formID);
        if (isName)
        {
            of.elements[dspace_makeFieldInput(valueInput,'_last')].value = this.form.elements['text1'].value;
            of.elements[dspace_makeFieldInput(valueInput,'_first')].value = this.form.elements['text2'].value;
        }
        else
            of.elements[valueInput].value = this.form.elements['text1'].value;

        if (authorityInput.length > 0 && of.elements[authorityInput] != null)
        {
            // conf input is auth input, substitute '_confidence' for '_authority'
            // if conf fieldname is  FIELD_confidence_NUMBER, then '_authority_' => '_confidence_'
            var confInput = "";

            var ci = authorityInput.lastIndexOf("_authority_");
            if (ci < 0)
                confInput = authorityInput.substring(0, authorityInput.length-10)+'_confidence';
            else
                confInput = authorityInput.substring(0, ci)+"_confidence_"+authorityInput.substring(ci+11);
            // DEBUG:
            // window.alert('Setting fields auth="'+authorityInput+'", conf="'+confInput+'"');

            var authValue = null;
            if (select.selectedIndex >= 0 && select.options[select.selectedIndex].authority != null)
            {
                authValue = select.options[select.selectedIndex].authority;
                of.elements[authorityInput].value = authValue;
            }
            if (of.elements[confInput] != null)
                of.elements[confInput].value = 'accepted';
            // make indicator blank if no authority value
            DSpaceUpdateConfidence(window.opener.document, confIndicatorID,
                    authValue == null || authValue == '' ? 'blank' :'accepted');
        }

        // force the submit button -- if there is an "add"
        if (isRepeating)
        {
            var add = of.elements["submit_"+valueInput+"_add"];
            if (add != null)
                add.click();
            else
                alert('Sanity check: Cannot find button named "submit_'+valueInput+'_add"');
        }
    }
    window.close();
    return false;
}

// handler for lookup popup's more button
function DSpaceChoicesMoreOnClick ()
{
    DSpaceChoicesLoad(this.form);
}

// handler for lookup popup's cancel button
function DSpaceChoicesCancelOnClick ()
{
    window.close();
    return false;
}

// -------------------- Utilities

// DSpace person-name conventions, see DCPersonName
function makePersonName(lastName, firstName)
{
    return (firstName == null || firstName.length == 0) ? lastName :
        lastName+", "+firstName;
}

// DSpace person-name conventions, see DCPersonName
function firstNameOf(personName)
{
    var comma = personName.indexOf(",");
    return (comma < 0) ? "" : stringTrim(personName.substring(comma+1));
}

// DSpace person-name conventions, see DCPersonName
function lastNameOf(personName)
{
    var comma = personName.indexOf(",");
    return stringTrim((comma < 0) ? personName : personName.substring(0, comma));
}

// replicate java String.trim()
function stringTrim(str)
{
    var start = 0;
    var end = str.length;
    for (; str.charAt(start) == ' '&& start < end; ++start) ;
    for (; end > start && str.charAt(end-1) == ' '; --end) ;
    return str.slice(start, end);
}

// format utility - replace @1@, @2@ etc with args 1, 2, 3..
// NOTE params MUST be monotonically increasing
// NOTE we can't use "{1}" like the i18n catalog because it elides them!!
// ...UNLESS maybe it were to be fixed not to when no params...
function dspace_formatMessage()
{
    var template = dspace_formatMessage.arguments[0];
    var i;
    for (i = 1; i < arguments.length; ++i)
    {
        var pattern = '@'+i+'@';
        if (template.search(pattern) >= 0)
            template = template.replace(pattern, dspace_formatMessage.arguments[i]);
    }
    return template;
}

// utility to make sub-field name of input field, e.g. _last, _first, _auth..
// if name ends with _1, _2 etc, put sub-name BEFORE the number
function dspace_makeFieldInput(name, sub)
{
    var i = name.search("_[0-9]+$");
    if (i < 0)
        return name+sub;
    else
        return name.substr(0, i)+sub+name.substr(i);
}

// update the class value of confidence-indicating element
function DSpaceUpdateConfidence(doc, confIndicatorID, newValue)
{
    // sanity checks - need valid ID and a real DOM object
    if (confIndicatorID == null || confIndicatorID == "")
        return;
    var confElt = doc.getElementById(confIndicatorID);
    if (confElt == null)
        return;

    // add or update CSS class with new confidence value, e.g. "cf-accepted".
    if (confElt.className == null)
        confElt.className = "cf-"+newValue;
    else
    {
        var classes = confElt.className.split(" ");
        var newClasses = "";
        var found = false;
        for (var i = 0; i < classes.length; ++i)
        {
            if (classes[i].match('^cf-[a-zA-Z0-9]+$'))
            {
                newClasses += "cf-"+newValue+" ";
                found = true;
            }
            else
                newClasses += classes[i]+" ";
        }
        if (!found)
            newClasses += "cf-"+newValue+" ";
        confElt.className = newClasses;
    }
}

// respond to "onchanged" event on authority input field
// set confidence to 'accepted' if authority was changed by user.
function DSpaceAuthorityOnChange(self, confValueID, confIndicatorID)
{
    var confidence = 'accepted';
    if (confValueID != null && confValueID != '')
    {
        var confValueField = document.getElementById(confValueID);
        if (confValueField != null)
            confValueField.value = confidence;
    }
    DSpaceUpdateConfidence(document, confIndicatorID, confidence)
    return false;
}

// respond to click on the authority-value lock button in Edit Item Metadata:
// "button" is bound to the image input for the lock button, "this"
function DSpaceToggleAuthorityLock(button, authInputID)
{
    // sanity checks - need valid ID and a real DOM object
    if (authInputID == null || authInputID == '')
        return false;
    var authInput = document.getElementById(authInputID);
    if (authInput == null)
        return false;

    // look for is-locked or is-unlocked in class list:
    var classes = button.className.split(' ');
    var newClass = '';
    var newLocked = false;
    var found = false;
    for (var i = 0; i < classes.length; ++i)
    {
        if (classes[i] == 'is-locked')
        {
            newLocked = false;
            found = true;
        }
        else if (classes[i] == 'is-unlocked')
        {
            newLocked = true;
            found = true;
        }
        else
            newClass += classes[i]+' ';
    }
    if (!found)
        return false;
    // toggle the image, and set readability
    button.className = newClass + (newLocked ? 'is-locked' : 'is-unlocked') + ' ';
    authInput.readOnly = newLocked;
    return false;
}
