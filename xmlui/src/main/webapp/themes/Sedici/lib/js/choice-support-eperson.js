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

/*
 *  Usa las siguientes variables para manejar los niveles de confianza. 
 *  Dichas variables se encuentran en el archivo choice-support.js
 * 
 *	var icono_new='ambiguous';
 *	var icono_accepted='accepted';
 *  var icono_rejected='notfound';
 *  var icono_search='search';
 *  var confianza_600='accepted';
 *  var confianza_300='notfound';
 *  var confianza_100='rejected';
 */

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
        	autoFocus: true,
        	minLength: 0,
            source: function(request, response) {
            	//pongo la variable de seleccion en false
            	seleccion=false;
                var reqUrl = choiceURL;
                if(request && request.term) {
                    reqUrl += "&query=" + request.term;
                        }
                
                //antes de empezar a buscar pongo el icono de ambiguos en el authority icon
                DSpaceUpdateConfidence(document, args.confidenceIndicatorID, icono_search);
                
                $.get(reqUrl, function(xmldata) {
                    var options = [];
                    var authorities = [];                    
                    if($(xmldata).find('Choice').length>0){
                    	//esta variable controla el caso en que la busqueda tire un solo resultado pero que no coincidan completamente el label del authority y el valor ingresado
                    	var entrada_igual=false;
                    	if ($(xmldata).find('Choice').length==1){
                    		var entrada = $('#' + inputID).val();
                    		//como el resultado es unico ese es el valor a auto-seleccionar
                    		$(xmldata).find('Choice').each(function() {
                    			if (entrada==$(this).text()){
                    				entrada_igual=true;
                    				if (isAuthorityControlled) {
			                        	cambiarAuthority(inputID, authorityID, $(this).attr('value'), args.confidenceIndicatorID, icono_accepted, args.confidenceName, confianza_600, authorityLabelID, $(this).text());
    		                        } else {
			                        	DSpaceUpdateConfidence(document, args.confidenceIndicatorID, icono_accepted);
			                        }
			                    }
                    		});  		                   
                    	};
                        if (!entrada_igual) {
                    		//como la cantidad es mayor a uno creo la lista
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
                    	};
                    } else {                    	
                		if (!isClosed){   
                			//si no es cerrado es correcto
                			//no hay concordancias en la busqueda, por lo que borro la authority key en caso de ser controlado y paso a deeterminar el icono
                        	if (isAuthorityControlled){                				
                        		//es un nuevo elemento
 	            				cambiarAuthority(inputID, authorityID, '', args.confidenceIndicatorID, icono_new, args.confidenceName, confianza_300, null, null);
 	            			} else {
                				//en este caso es un nuevo valor si authority key asociado
                				DSpaceUpdateConfidence(document, args.confidenceIndicatorID, icono_new);
                			} 
                        } else {
                     		//si es cerrado esta mal
                     		//no hay concordancias en la busqueda, por lo que borro la authority key en caso de ser controlado y paso a deeterminar el icono
                        	if (isAuthorityControlled){                				
                        		//Como es cerrado y no tengo selecionado un authority key freno las tratativas
    	                    	cambiarAuthority(inputID, authorityID, $('#' + authorityID).val(), args.confidenceIndicatorID, icono_rejected, args.confidenceName, confianza_100, null, null);
    	                    } else {
		                    	//Como es cerrado y no hay control de autoridad freno las tratativas
		                    	DSpaceUpdateConfidence(document, args.confidenceIndicatorID, icono_rejected);
                        	}
                        }    
                    };
                    ac.data('authorities',authorities);
                    response(options);
                });
                },
            search: function(event, ui) { 
                	//si la busqueda es vacia actualizo los authority keys y oculto indicador de confianza
                	if ($('#'+inputID).val().length==0){
                		if (isAuthorityControlled){
                			//si es authority controlled, debo vaciar la autoridad
                			cambiarAuthority(inputID, authorityID, '', args.confidenceIndicatorID, 'blank', args.confidenceName, 'blank', null, null);
                		} else {
    	            		 DSpaceUpdateConfidence(document, args.confidenceIndicatorID, 'blank');
    	                	 cambiarAuthorityConfidence(args.confidenceName, 'blank');    	                		
    	            	 }      	    	
                		//retorno falso para que no ejecute la busqueda ajax
    	            	return false;
                	}
                },
            close: function(event, ui) {
            	if (!seleccion){
            		//si entra por aca es porque la lista se cerro sin una seleccion
            		if (!isClosed){   
            			//si no es cerrado es correcto
            			//no hay concordancias en la busqueda, por lo que borro la authority key en caso de ser controlado y paso a deeterminar el icono
                    	if (isAuthorityControlled){                				
                    		//es un nuevo elemento
            				cambiarAuthority(inputID, authorityID, '', args.confidenceIndicatorID, icono_new, args.confidenceName, confianza_300, null, null);
            			} else {
	            			//es un nuevo elemento
	            			DSpaceUpdateConfidence(document, args.confidenceIndicatorID, icono_new);
	            		}
                    } else {
                 		//si es cerrado esta mal
                 		//no hay concordancias en la busqueda, por lo que borro la authority key en caso de ser controlado y paso a deeterminar el icono
                    	if (isAuthorityControlled){                				
                    		cambiarAuthority(inputID, authorityID, $('#' + authorityID).val(), args.confidenceIndicatorID, icono_rejected, args.confidenceName, confianza_100, null, null);
	            		} else {
	            			//no hay control de autoridad y es cerrado, por lo que no debe permitir salir del select sin seleccionar
            				DSpaceUpdateConfidence(document, args.confidenceIndicatorID, icono_rejected);
            				alert('El dato ingresado es incorrecto');
            				//$('#'+inputID).val(""); 
	            		}
                 	}              	
            	}
            },
            select: function(event, ui) {
                //modifico la variable que controla si la lista se cerró por seleccion en verdadero
            	seleccion = true;
            	var authInput = $('#' + authorityID);

                var authorities = ac.data('authorities');
                var authValue = authorities['label: ' + ui.item.label + ', value: ' + ui.item.value];

                if (isAuthorityControlled) {
                	cambiarAuthority(inputID, authorityID, authValue, args.confidenceIndicatorID, icono_accepted, args.confidenceName, confianza_600, null, null);
                }
            }
		});
	});
}