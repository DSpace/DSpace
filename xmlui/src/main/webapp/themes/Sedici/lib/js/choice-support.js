/*
 * Copyright (C) 2011 SeDiCI <info@sedici.unlp.edu.ar>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// Client-side scripting to support DSpace Choice Control

// IMPORTANT NOTE:
//  This version of choice-support.js has been rewritten to use jQuery
// instead of prototype & scriptaculous. The goal was not to change the
// way it works in any way, just to get the prototype dependency out.
// @Author Art Lowel (art.lowel at atmire.com)

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
// NOTE NICO: Successful autocomplete always sets confidence to 'accepted' since
//  authority value (if any) *was* chosen interactively by a human.
function DSpaceSetupAutocomplete(formID, args) {
    $(function() {
    if (args.authorityName == null)
        args.authorityName = dspace_makeFieldInput(args.inputName, '_authority');
        var form = $('#' + formID)[0];
    var inputID = form.elements[args.inputName].id;

    var authorityID = null;
    if (form.elements[args.authorityName] != null)
        authorityID = form.elements[args.authorityName].id;    
    
    var authorityLabelID = null;
    if (form.elements[args.authorityLabel] != null)
        authorityLabelID = form.elements[args.authorityLabel].id;

    var isClosed;

    if (args.isClosed=='yes'){
    	isClosed=true; 
    } else {
    	isClosed=false;    
    };
    
    var isAuthorityControlled;

    if (args.authorityControlled=='yes'){
    	isAuthorityControlled=true; 
    } else {
    	isAuthorityControlled=false;    
    };

    // AJAX menu source, can add &query=TEXT
    var choiceURL = args.contextPath + "/choices/" + args.metadataField;
    var collID = args.collection == null ? -1 : args.collection;
        choiceURL += '?collection=' + collID;

        var ac = $('#' + inputID);
        //esta variable identifica si la lista es cerrada por seleccion.
        var seleccion;
        
        ac.autocomplete({
        	autoFocus: false,
            source: function(request, response) {
            	//pongo la variable de seleccion en false
            	seleccion=false;
                var reqUrl = choiceURL;
                if(request && request.term) {
                    reqUrl += "&query=" + request.term;
                        }
                $.get(reqUrl, function(xmldata) {
                    var options = [];
                    var authorities = [];
                    if($(xmldata).find('Choice').length>0){
	                    $(xmldata).find('Choice').each(function() {
	                        // get value
	                        var value = $(this).attr('value') ? $(this).attr('value') : null;
	                        // get label, if empty set it to value
	                        var label = $(this).text() ? $(this).text() : value;
	                        // if value was empty but label was provided, set value to label
	                        if(!value) {
	                            value = label;
	                        }
	                        // if at this point either value or label == null, this means none of both were set and we shouldn't add it to the list of options
	                        if (label != null) {
	                            options.push({
	                                label: label,
	                                value: value
	                            });
	                            authorities['label: ' + label + ', value: ' + value] = $(this).attr('authority') ? $(this).attr('authority') : value;
	                        }
	                    });
                    } else {                    	
                    	// No hay elementos que matcheen por lo que cambia la confianza y value del authority
                    	// Debo corroborar si el campo tiene control de autoridad y si es cerrado debo modificar
                    	verificarAuthority(isClosed, isAuthorityControlled, authorityID, args.confidenceIndicatorID, args.confidenceName)
                    };
                    ac.data('authorities',authorities);
                    response(options);
                });
                },
            close: function(event, ui) {            	
            	//NOTE: en este evento verifico que si la lista se cierra sin haber seleccionado un dato modificar los input de authority y los iconos
                if (!seleccion){
                	verificarAuthority(isClosed, isAuthorityControlled, authorityID, args.confidenceIndicatorID, args.confidenceName);
                	//alert('se cerro sin seleccionar');                	
                };
                
            },
            select: function(event, ui) {
                // NOTE: lookup element late because it might not be in DOM
                // at the time we evaluate the function..
            	//var authInput = document.getElementById(authorityID);
            	//var authValue = li == null ? "" : li.getAttribute("authority");
                //modifico la variable que controla si la lista se cerró por seleccion en verdadero
            	seleccion = true;
            	var authInput = $('#' + authorityID);
                
                if(authInput.length > 0) {
                    authInput = authInput[0];
                } 
                var authorities = ac.data('authorities');
                var authValue = authorities['label: ' + ui.item.label + ', value: ' + ui.item.value];
                    if (authInput != null) {
                        authInput.value = authValue;
                        if (isClosed){
                        	//alert('es Cerrado');
                        } else {
                        	if (isAuthorityControlled){
	                            //Agrego el input del label en caso de que no sea closed el authority
	                            var authLabel = document.getElementById(authorityLabelID);
	                            authLabel.value = ui.item.label;
	                        	//alert('es Abierto');
                            }
                        }
                       
                        // update confidence input's value too if available.
                        if (args.confidenceName != null) {
                            var confInput = authInput.form.elements[args.confidenceName];
                            if (confInput != null)
                                confInput.value = 'accepted';
                        }
                    }
                    // make indicator blank if no authority value
                    DSpaceUpdateConfidence(document, args.confidenceIndicatorID,
                            authValue == null || authValue == '' ? 'blank' : 'accepted');
                }
		});
	});
}

//--------------------- Funcion que regula la autoridad y los choices cerrados
function verificarAuthority(isClosed, isAuthorityControlled, authorityID, confidenceIndicatorID, confidenceName){
	if (isClosed){
 	   //Es cerrado, por lo que debo mostrar el icono de error 
 	   if (isAuthorityControlled){                     		  
     		//Como es cerrado y tiene control de autoridad debo borrar los valores del confidence y de authority
     		var authInput = $('#' + authorityID);
     		authInput.value = '';  
     		// update confidence input's value too if available.
              if (confidenceName != null) {
                  var confInput = document.getElementById(confidenceName);
                  if (confInput != null)
                      confInput.value = 'blank';
              };
          };
          //alert('fails');
          DSpaceUpdateConfidence(document, confidenceIndicatorID,'failed');	                        	
       } else {
     	  //Si no es cerrado pero tiene autoridad, debo mostrar que el dato va a tener otro label distinto al de la autoridad
     	  //excepto que no tenga configurada la autorida, por le que debo mostrar el label de error
     	  if (isAuthorityControlled){
     		  var authInput = document.getElementById(authorityID);		                    	 
     		  if (authInput.value != ''){  
     		     DSpaceUpdateConfidence(document, confidenceIndicatorID,'ambiguous');
     		     //alert('ambiguos ..'+authInput.value);
     		  } else {
     			  DSpaceUpdateConfidence(document, confidenceIndicatorID,'failed');
     			  //alert('fails');
     		  }
     	  } else {
     		  //como no tiene autoridad y no es cerrado, esta bien que agregue uno nuevo
     		  DSpaceUpdateConfidence(document, confidenceIndicatorID,'accepted');
     		  //alert('aceptado');
     	  };
       }
}

// -------------------- support for Lookup Popup

// Create popup window with authority choices for value of an input field.
// This is intended to be called by onClick of a "Lookup" or "Add"  button.
function DSpaceChoiceLookup(url, field, formID, valueInput, authInput,
                            confIndicatorID, collectionID, isName, isRepeating) {
    // fill in URL
    url += '?field=' + field + '&formID=' + formID + '&valueInput=' + valueInput +
            '&authorityInput=' + authInput + '&collection=' + collectionID +
            '&isName=' + isName + '&isRepeating=' + isRepeating + '&confIndicatorID=' + confIndicatorID;

    // primary input field - for positioning popup.
    var inputFieldName = isName ? dspace_makeFieldInput(valueInput, '_last') : valueInput;
    var inputField = $('input[name = ' + inputFieldName + ']');
    // sometimes a textarea is used, in which case the previous jQuery search delivered no results...
    if(inputField.length == 0) {
        // so search for a textarea
        inputField = $('textarea[name = ' + inputFieldName + ']');
    }
    var cOffset = 0;
    if (inputField != null)
        cOffset = inputField.offset();
    var width = 600;  // XXX guesses! these should be params, or configured..
    var height = 470;
    var left;
    var top;
    if (window.screenX == null) {
        left = window.screenLeft + cOffset.left - (width / 2);
        top = window.screenTop + cOffset.top - (height / 2);
    } else {
        left = window.screenX + cOffset.left - (width / 2);
        top = window.screenY + cOffset.top - (height / 2);
    }
    if (left < 0) left = 0;
    if (top < 0) top = 0;
    var pw = window.open(url, 'ignoreme',
            'width=' + width + ',height=' + height + ',left=' + left + ',top=' + top +
                    ',toolbar=no,menubar=no,location=no,status=no,resizable');
    if (window.focus) pw.focus();
    return false;
}

// Run this as the Lookup page is loaded to initialize DOM objects, load choices
function DSpaceChoicesSetup(form) {
    // find the "LEGEND" in fieldset, which acts as page title,
    var legend = $('#aspect_general_ChoiceLookupTransformer_list_choicesList :header:first');
    //save the template as a jQuery data field
    legend.data('template', legend.html());
    legend.html("Loading...");
    DSpaceChoicesLoad(form);
}


// populate the "select" with options from ajax request
// stash some parameters as properties of the "select" so we can add to
// the last start index to query for next set of results.
function DSpaceChoicesLoad(form) {
    var field = $('*[name = paramField]').val();
    var value = $('*[name = paramValue]').val();
    if (!value)
        value = '';
    var start = $('*[name = paramStart]').val();
    var limit = $('*[name = paramLimit]').val();
    var formID = $('*[name = paramFormID]').val();
    var collID = $('*[name = paramCollection]').val();
    var isName = $('*[name = paramIsName]').val() == 'true';
    var isRepeating = $('*[name = paramIsRepeating]').val() == 'true';
    var isClosed = $('*[name = paramIsClosed]').val() == 'true';
    var contextPath = $('*[name = contextPath]').val();
    var fail = $('*[name = paramFail]').val();
    var valueInput = $('*[name = paramValueInput]').val();
    var nonAuthority = "";
    var pNAInput = $('*[name = paramNonAuthority]');
    if (pNAInput.length > 0)
        nonAuthority = pNAInput.val();

    // get value from form inputs in opener if not explicitly supplied
    if (value.length == 0) {
        var of = $(window.opener.document).find('#' + formID);
        if (isName)
            value = makePersonName(of.find('*[name = ' + dspace_makeFieldInput(valueInput, '_last') + ']').val(),
                    of.find('*[name = ' + dspace_makeFieldInput(valueInput, '_first') + ']').val());
        else
            value = of.find('*[name = ' + valueInput + ']').val();

        // if this is a repeating input, clear the source value so that e.g.
        // clicking "Next" on a submit-describe page will not *add* the proposed
        // lookup text as a metadata value:
        if (isRepeating) {
            if (isName) {
                of.find('*[name = ' + dspace_makeFieldInput(valueInput, '_last') + ']').val('');
                of.find('*[name = ' + dspace_makeFieldInput(valueInput, '_first') + ']').val('');
            }
            else
                of.find('*[name = ' + valueInput + ']').val(null);
        }
    }

    // start spinner
    var indicator = $('#lookup_indicator_id');
    indicator.show('fast');

    $(window).ajaxError(function(e, xhr, settings, exception) {
        window.alert(fail + " Exception=" + e);
        if (indicator != null) indicator.style.display = "none";
    });

    $.ajax({
        url: contextPath + "/choices/" + field,
        type: "GET",
        data: {query: value, collection: collID,
                     start: start, limit: limit},
        error: function() {
            window.alert(fail + " HTTP error resonse");
            if (indicator != null) indicator.style.display = "none";
        },
        success: function(data) {
            var Choices = $(data).find('Choices');
            var err = Choices.attr('error');
            if (err != null && err == 'true')
                window.alert(fail + " Server indicates error in response.");
            var opts = Choices.find('Choice');

            // update range message and update 'more' button
            var oldStart = 1 * Choices.attr('start');
            var nextStart = oldStart + opts.length;
            var lastTotal = Choices.attr('total');
            var resultMore = Choices.attr('more');
            $('*[name = more]').attr('disabled', !(resultMore != null && resultMore == 'true'));
            $('*[name = paramStart]').val(nextStart);

            // clear select first
            var select = $('select[name = chooser]:first');
            select.find('option:not(:last)').remove();
            var lastOption = select.find('option:last');

            var selectedByValue = -1; // select by value match
            var selectedByChoices = -1;  // Choice says its selected
            $.each(opts, function(index) {
//                debugger;
                var current = $(this);
                if (current.attr('value') == value)
                    selectedByValue = index;
                if(current.attr('selected') != undefined)
                    selectedByChoices = index;

                var newOption = $('<option value="' + current.attr('value') + '">' + current.text() + '</option>');
                newOption.data('authority', current.attr('authority'));

                if (lastOption.length > 0)
                    lastOption.insertBefore(newOption);
                else
                    select.append(newOption);
            });


            // add non-authority option if needed.
            if (!isClosed) {
                select.append(new Option(dspace_formatMessage(nonAuthority, value), value), null);
            }
            var defaultSelected = -1;
            if (selectedByChoices >= 0)
                defaultSelected = selectedByChoices;
            else if (selectedByValue >= 0)
                defaultSelected = selectedByValue;
            else if (select[0].options.length == 1)
                defaultSelected = 0;

            // load default-selected value
            if (defaultSelected >= 0) {
                select[0].options[defaultSelected].defaultSelected = true;
                var so = select[0].options[defaultSelected];
                if (isName) {
                    $('*[name = text1]').val(lastNameOf(so.value));
                    $('*[name = text2]').val(firstNameOf(so.value));
                }
                else
                    $('*[name = text1]').val(so.value);
            }

            // turn off spinner
            indicator.hide('fast');

            // "results" status line
            var statLast = nextStart + (isClosed ? 2 : 1);

            var legend = $('#aspect_general_ChoiceLookupTransformer_list_choicesList :header:first');
            legend.html(dspace_formatMessage(legend.data('template'),
                            oldStart + 1, statLast, Math.max(lastTotal, statLast), value));
        }
    });
}

// handler for change event on choice selector - load new values
function DSpaceChoicesSelectOnChange() {
    // "this" is the window,

    var form = $('#aspect_general_ChoiceLookupTransformer_div_lookup');
    var select = form.find('*[name = chooser]');

    var isName = form.find('*[name = paramIsName]').val() == 'true';

    var selectedValue = select.val();

    if (isName) {
        form.find('*[name = text1]').val(lastNameOf(selectedValue));
        form.find('*[name = text2]').val(firstNameOf(selectedValue));
    }
    else
        form.find('*[name = text1]').val(selectedValue);
}

// handler for lookup popup's accept (or add) button
//  stuff values back to calling page, force an add if necessary, and close.
function DSpaceChoicesAcceptOnClick() {
    var select = $('*[name = chooser]');
    var isName = $('*[name = paramIsName]').val() == 'true';
    var isRepeating = $('*[name = paramIsRepeating]').val() == 'true';
    var valueInput = $('*[name = paramValueInput]').val();
    var authorityInput = $('*[name = paramAuthorityInput]').val();
    var formID = $('*[name = paramFormID]').val();
    var confIndicatorID = $('*[name = paramConfIndicatorID]').length = 0 ? null : $('*[name = paramConfIndicatorID]').val();

    // default the authority input if not supplied.
    if (authorityInput.length == 0)
        authorityInput = dspace_makeFieldInput(valueInput, '_authority');

    // always stuff text fields back into caller's value input(s)
    if (valueInput.length > 0) {
        var of = $(window.opener.document).find('#' + formID);
        if (isName) {
            of.find('*[name = ' + dspace_makeFieldInput(valueInput, '_last') + ']').val($('*[name = text1]').val());
            of.find('*[name = ' + dspace_makeFieldInput(valueInput, '_first') + ']').val($('*[name = text2]').val());
        }
        else
            of.find('*[name = ' + valueInput + ']').val($('*[name = text1]').val());

        if (authorityInput.length > 0 && of.find('*[name = ' + authorityInput + ']').length > 0) {
            // conf input is auth input, substitute '_confidence' for '_authority'
            // if conf fieldname is  FIELD_confidence_NUMBER, then '_authority_' => '_confidence_'
            var confInput = "";

            var ci = authorityInput.lastIndexOf("_authority_");
            if (ci < 0)
                confInput = authorityInput.substring(0, authorityInput.length - 10) + '_confidence';
            else
                confInput = authorityInput.substring(0, ci) + "_confidence_" + authorityInput.substring(ci + 11);
            // DEBUG:
            // window.alert('Setting fields auth="'+authorityInput+'", conf="'+confInput+'"');

            var authValue = null;
            var selectedOption = select.find(':selected');
            if (selectedOption.length >= 0 && selectedOption.data('authority') != null) {
                of.find('*[name = ' + authorityInput + ']').val(selectedOption.data('authority'));
            }
            of.find('*[name = ' + confInput + ']').val('accepted');
            // make indicator blank if no authority value
            DSpaceUpdateConfidence(window.opener.document, confIndicatorID,
                    authValue == null || authValue == '' ? 'blank' : 'accepted');
        }

        // force the submit button -- if there is an "add"
        if (isRepeating) {
            var add = of.find('*[name = submit_' + valueInput + '_add]');
            if (add.length > 0)
                add.click();
            else
                alert('Sanity check: Cannot find button named "submit_' + valueInput + '_add"');
        }
    }
    window.close();
    return false;
}

// handler for lookup popup's more button
function DSpaceChoicesMoreOnClick() {
    DSpaceChoicesLoad(this.form);
}

// handler for lookup popup's cancel button
function DSpaceChoicesCancelOnClick() {
    window.close();
    return false;
}

// -------------------- Utilities

// DSpace person-name conventions, see DCPersonName
function makePersonName(lastName, firstName) {
    return (firstName == null || firstName.length == 0) ? lastName :
            lastName + ", " + firstName;
}

// DSpace person-name conventions, see DCPersonName
function firstNameOf(personName) {
    var comma = personName.indexOf(",");
    return (comma < 0) ? "" : stringTrim(personName.substring(comma + 1));
}

// DSpace person-name conventions, see DCPersonName
function lastNameOf(personName) {
    var comma = personName.indexOf(",");
    return stringTrim((comma < 0) ? personName : personName.substring(0, comma));
}

// replicate java String.trim()
function stringTrim(str) {
    var start = 0;
    var end = str.length;
    for (; str.charAt(start) == ' ' && start < end; ++start) ;
    for (; end > start && str.charAt(end - 1) == ' '; --end) ;
    return str.slice(start, end);
}

// format utility - replace @1@, @2@ etc with args 1, 2, 3..
// NOTE params MUST be monotonically increasing
// NOTE we can't use "{1}" like the i18n catalog becanewValueuse it elides them!!
// ...UNLESS maybe it were to be fixed not to when no params...
function dspace_formatMessage() {
    var template = dspace_formatMessage.arguments[0];
    var i;
    for (i = 1; i < arguments.length; ++i) {
        var pattern = '@' + i + '@';
        if (template.search(pattern) >= 0)
            {
                var value = dspace_formatMessage.arguments[i];
                if (value == undefined)
                    value = '';
                template = template.replace(pattern, value);
            }
    }
    return template;
}

// utility to make sub-field name of input field, e.g. _last, _first, _auth..
// if name ends with _1, _2 etc, put sub-name BEFORE the number
function dspace_makeFieldInput(name, sub) {
    var i = name.search("_[0-9]+$");
    if (i < 0)
        return name + sub;
    else
        return name.substr(0, i) + sub + name.substr(i);
}

// update the class value of confidence-indicating element
function DSpaceUpdateConfidence(doc, confIndicatorID, newValue) {
    // sanity checks - need valid ID and a real DOM object
    if (confIndicatorID == null || confIndicatorID == "")
        return;
    var confElt = doc.getElementById(confIndicatorID);
    if (confElt == null)
        return;

    // add or update CSS class with new confidence value, e.g. "cf-accepted".
    if (confElt.className == null)
        confElt.className = "cf-" + newValue;
    else {
        var classes = confElt.className.split(" ");
        var newClasses = "";
        var found = false;
        for (var i = 0; i < classes.length; ++i) {
            if (classes[i].match('^cf-[a-zA-Z0-9]+$')) {
                newClasses += "cf-" + newValue + " ";
                found = true;
            }
            else
                newClasses += classes[i] + " ";
        }
        if (!found)
            newClasses += "cf-" + newValue + " ";
        confElt.className = newClasses;
    }
}

// respond to "onchanged" event on authority input field
// set confidence to 'accepted' if authority was changed by user.
function DSpaceAuthorityOnChange(self, confValueID, confIndicatorID) {
    var confidence = 'accepted';
    if (confValueID != null && confValueID != '') {
        var confValueField = document.getElementById(confValueID);
        if (confValueField != null)
            confValueField.value = confidence;
    }
    DSpaceUpdateConfidence(document, confIndicatorID, confidence);
    return false;
}

// respond to click on the authority-value lock button in Edit Item Metadata:
// "button" is bound to the image input for the lock button, "this"
function DSpaceToggleAuthorityLock(button, authInputID) {
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
    for (var i = 0; i < classes.length; ++i) {
        if (classes[i] == 'is-locked') {
            newLocked = false;
            found = true;
        }
        else if (classes[i] == 'is-unlocked') {
            newLocked = true;
            found = true;
        }
        else
            newClass += classes[i] + ' ';
    }
    if (!found)
        return false;
    // toggle the image, and set readability
    button.className = newClass + (newLocked ? 'is-locked' : 'is-unlocked') + ' ';
    authInput.readOnly = newLocked;
    return false;
}
