//--------------------- Variables para el manejo de las confianzas y los iconos
var icono_new='ambiguous';
var icono_accepted='accepted';
var icono_accepted_variant='acceptedvariant';
var icono_change='failed';
var icono_rejected='notfound';
var icono_search='search';
var confianza_600='accepted';
var confianza_300='notfound';
var confianza_100='rejected';
var errorConfianzaSingular='Verifique el valor';
var errorConfianzaPlural='Hay errores en el formulario';

//-------------------Variable para el limite en el lookup
var limit_lookup=150;
//-------------------Variable para el limite en el suggest
var limit_suggest=50;
//-------------------Variable para el paginado del lookup
var previous_start=0;


//--------------------- Funcion que cambia la confianza del authority

function cambiarAuthorityConfidence(confidenceName, value){
     var confInput = $('#'+confidenceName);
     $('#'+confidenceName).val(value);
}

//--------------------- Funcion para vaciar input extras de authorities

function vaciarAuthority(authorityID, confidenceIndicatorID, confidenceName){
	var authInput = $('#' + authorityID);
	authInput.val(''); 
	cambiarAuthorityConfidence(confidenceName, 'blank');
}


//--------------------- Funcion para cambiar autorithies

function cambiarAuthority(inputID, authorityID, authorityValue, confidenceIndicatorID, confidenceIndicatorValue, confidenceNameID, confidenceNameValue, authorityLabelID, authorityLabelValue){
	var authInput = $('#' + authorityID);
	authInput.val(authorityValue); 
	cambiarAuthorityConfidence(confidenceNameID, confidenceNameValue);
	DSpaceUpdateConfidence(document, confidenceIndicatorID, confidenceIndicatorValue);
	if (authorityLabelID != null){
		if ($('#'+inputID).val()!=$('#' + authorityLabelID).val()){
			$('#' + authorityLabelID).show();
		}else{
			$('#' + authorityLabelID).hide();
		}
		var authLabel = document.getElementById(authorityLabelID);
		authLabel.value = authorityLabelValue;
    }
	

	if (confidenceNameValue=='rejected'){
		$('#' + inputID).attr('class', 'ds-text-field error submit-text');
	} else {
		$('#' + inputID).attr('class', 'ds-text-field submit-text');
	}
}



//------Funcion que valida que ningun campo controlado con autoridades este rejected

function verificarConfidence(){
	var authorities=$('input.ds-authority-confidence-input');
	var retorno=true;
	var primero=null;
	var actual;
	authorities.each(function() {
		  if (this.value == 'rejected' ){
			  if (primero==null){
				  primero=$('#'+this.id).siblings()[1];
			  };
			  retorno=false;
		  }
    });
	if (!retorno){
		alert(errorConfianzaPlural);
		primero.focus();
	}
    return retorno;
}

//------Funcion que valida que un campo en particular controlado con autoridades este rejected
/*
 * inputID es el campo donde se debera enfocar el cursor
 * confidenceName es el nombre del campo que tiene el valor de la confianza
 * confianza_valor es el valor que no deberá ocurrir
 * 
 */
function verificarConfidenceIndividual(inputID, confidenceName, confianza_valor){
	var retorno=true;
	if ($('#'+confidenceName).val()==confianza_valor){
		retorno=false;
		alert(errorConfianzaSingular);
		$('#'+inputID).focus();
	}

    return retorno;
}

$(document).ready(function (){
		
	$("#aspect_submission_StepTransformer_field_submit_next").click(function() {
			return verificarConfidence();

			});
	
	$("#aspect_submission_StepTransformer_field_submit_prev").click(function() {
			return verificarConfidence();
			});	

});

//-------------------- Funcion que elimina un previous value

function eliminarMetadato(metadatoCheckboxId){
     var checkbox = $('#'+metadatoCheckboxId);
     checkbox.attr('checked', true);
}

//-------------------- support for Lookup Popup

//Create popup window with authority choices for value of an input field.
//This is intended to be called by onClick of a "Lookup" or "Add"  button.
function DSpaceChoiceLookup(url, field, formID, valueInput, authInput,
                         confIndicatorID, collectionID, isName, isRepeating) {
 // fill in URL

	
	url += '?field=' + field + '&formID=' + formID + '&valueInput=' + valueInput +
         '&authorityInput=' + authInput + '&collection=' + collectionID +
         '&isName=' + isName + '&isRepeating=' + isRepeating + '&confIndicatorID=' + confIndicatorID + '&limit='+limit_lookup;
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
	 left = $(window).width()/2 - width/2;
	  top = $(window).height()/2 - height/2;
 }
 if (left < 0) left = 0;
 if (top < 0) top = 0;
 var pw = window.open(url, 'ignoreme',
         'width=' + width + ',height=' + height + ',left=' + left + ',top=' + top +
                 ',toolbar=no,menubar=no,location=no,status=no,resizable');
 if (window.focus) pw.focus();
 return false;
}

//Run this as the Lookup page is loaded to initialize DOM objects, load choices
function DSpaceChoicesSetup(form) {
 // find the "LEGEND" in fieldset, which acts as page title,
 var legend = $('#aspect_general_ChoiceLookupTransformer_list_choicesList :header:first');
 //save the template as a jQuery data field
 legend.data('template', legend.html());
 legend.html("Loading...");
 DSpaceChoicesLoad(form);
}


//populate the "select" with options from ajax request
//stash some parameters as properties of the "select" so we can add to
//the last start index to query for next set of results.
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
    /* if (isRepeating) {
         if (isName) {
             of.find('*[name = ' + dspace_makeFieldInput(valueInput, '_last') + ']').val('');
             of.find('*[name = ' + dspace_makeFieldInput(valueInput, '_first') + ']').val('');
         }
         else
             of.find('*[name = ' + valueInput + ']').val(null);
     }*/
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
         //var oldStart = 1 * Choices.attr('start');
         //actualizo el start viejo
         var oldStart = 1 * Choices.attr('start');
         
         previous_start=oldStart-(1*limit_lookup);
         if (previous_start<0) previous_start=0;
         
         var lastTotal = 1 * Choices.attr('total');
         var nextStart = oldStart + lastTotal;
         var resultMore = Choices.attr('more');
         if(!(resultMore != null && resultMore == 'true')){
        	// $('*[name = more]').attr('disabled', 'disabled');
         } else {
        	 $('*[name = more]').attr('class','ds-button-field choices-lookup');
             $('*[name = more]').attr('disabled',null); 
         };
         
         if(oldStart==previous_start){
        	 $('#aspect_general_ChoiceLookupTransformer_field_less').attr('disabled', 'disabled');
         } else {
        	 $('#aspect_general_ChoiceLookupTransformer_field_less').attr('class','ds-button-field choices-lookup');
        	 $('#aspect_general_ChoiceLookupTransformer_field_less').attr('disabled',null); 
         };
         //Activo el botón de ver mas resultados y le cambio el estilo
         /*$('*[name = more]').attr('class','ds-button-field choices-lookup');
         $('*[name = more]').attr('disabled',null);*/

         $('*[name = paramStart]').val(nextStart);

         // clear select first
         var select = $('select[name = chooser]:first');
         select.find('option').remove();
         /*select.find('option:not(:last)').remove();
         var lastOption = select.find('option:last');*/

         var selectedByValue = -1; // select by value match
         var selectedByChoices = -1;  // Choice says its selected
         $.each(opts, function(index) {
//             debugger;

             var current = $(this);
             if (current.attr('value') == value)
                 selectedByValue = index;
             if(current.attr('selected') != undefined)
                 selectedByChoices = index;

             var newOption = $('<option value="' + current.attr('value') + '">' + current.text() + '</option>');
             newOption.data('authority', current.attr('authority'));
             select.append(newOption);
             
             /*if (lastOption.length > 0){
                 //lastOption.insertBefore(newOption);
            	 select.append(newOption);
             }
             else {                 
                 select.append(newOption);
             }*/
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

//handler for change event on choice selector - load new values
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

//handler for lookup popup's accept (or add) button
//stuff values back to calling page, force an add if necessary, and close.
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

//handler for lookup popup's more button
function DSpaceChoicesMoreOnClick() {
 DSpaceChoicesLoad(this.form);
}

//handler for lookup popup's more button
function DSpaceChoicesLessOnClick() {
 $('*[name = paramStart]').val(previous_start);
 DSpaceChoicesLoad(this.form);
}

//handler for lookup popup's cancel button
function DSpaceChoicesCancelOnClick() {
 window.close();
 return false;
}

//-------------------- Utilities

//DSpace person-name conventions, see DCPersonName
function makePersonName(lastName, firstName) {
 return (firstName == null || firstName.length == 0) ? lastName :
         lastName + ", " + firstName;
}

//DSpace person-name conventions, see DCPersonName
function firstNameOf(personName) {
 var comma = personName.indexOf(",");
 return (comma < 0) ? "" : stringTrim(personName.substring(comma + 1));
}

//DSpace person-name conventions, see DCPersonName
function lastNameOf(personName) {
 var comma = personName.indexOf(",");
 return stringTrim((comma < 0) ? personName : personName.substring(0, comma));
}

//replicate java String.trim()
function stringTrim(str) {
 var start = 0;
 var end = str.length;
 for (; str.charAt(start) == ' ' && start < end; ++start) ;
 for (; end > start && str.charAt(end - 1) == ' '; --end) ;
 return str.slice(start, end);
}

//format utility - replace @1@, @2@ etc with args 1, 2, 3..
//NOTE params MUST be monotonically increasing
//NOTE we can't use "{1}" like the i18n catalog because it elides them!!
//...UNLESS maybe it were to be fixed not to when no params...
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

//utility to make sub-field name of input field, e.g. _last, _first, _auth..
//if name ends with _1, _2 etc, put sub-name BEFORE the number
function dspace_makeFieldInput(name, sub) {
 var i = name.search("_[0-9]+$");
 if (i < 0)
     return name + sub;
 else
     return name.substr(0, i) + sub + name.substr(i);
}

//update the class value of confidence-indicating element
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
     //Set the new confidence class and the related title
     confElt.className = newClasses;
     confElt.title=confidenceMessages['cf_'+newValue];
 }
}

//respond to "onchanged" event on authority input field
//set confidence to 'accepted' if authority was changed by user.
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

//respond to click on the authority-value lock button in Edit Item Metadata:
//"button" is bound to the image input for the lock button, "this"
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