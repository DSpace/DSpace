
(function( $ ){

  $.fn.metadataGenerator = function() {

    // Guardamos la referencia al textarea sobre el que se aplica este plugin
    var self = this;

    // Si no hay elementos que matcheen con el selector, se termina la ejecucion
    if(self.length == 0)
    	return;

    // Lista de Handlers para tipos especiales
    var typeHandlers = {
    	link: {
    		encode: function(key, value) {
    			//
    			// Creamos un tag A con el value como link y el texo definido en el metadato
    			//
    			var linkLabel = value;
    			if(metadataList[key].linkLabel)
    				linkLabel = metadataList[key].linkLabel;
    			
    			// Agregamos el "HTTP://"
    			if(!/^http:\/\//i.test(value))
    				value = "http://"+value;
    			
    			// Arma el html a retornar
	            return "<a target='_blank' href='"+value+"'>"+linkLabel+"</a>";
    		},
    		decode: function(key, value) {
    			// 
    			// Espera recibir un tag A para extraerle el link
    			//

    			// Extrae el link
    	        var result = /<a.*href=\"(.*)\".*>.*<\/a>/i.exec(value);
    	        
    			return value;
    		}
    	}
    }
    
    // Lista de metadatos para ofrecer
    // TODO Sería genial si esto pudiese levantarse de la base de datos o de la configuracion
    var metadataList = {
	    titulo_de_la_serie: {label: "Título de la serie"},
	    subtitulo_de_la_serie: {label: "Subtítulo de la serie"},
	    texto_libre: {label: "Texto libre"},
	    dossier: {label: "Dossier"},
	    decano: {label: "Decano"},
	    vicedecano: {label: "Vicedecano"},
	    direccion: {label: "Dirección"},
	    entidad_de_origen: {label: "Entidad de Origen"},
	    fecha_de_publicacion: {label: "Fecha de publicación"},
	    director: {label: "Director"},
	    subdirector: {label: "Subdirector"},
	    editor: {label: "Editor"},
	    editorial: {label: "Editorial"},
		comite_editorial: {label: "Comité editorial"},
	    comite_de_referato: {label: "Comité de referato"},
	    consejo_asesor: {label: "Consejo asesor"},
	    secretaria_de_redaccion: {label: "Secretaría de redacción"},
	    issn: {label: "ISSN"},
	    isbnl: {label: "ISSN-L"},
	    frecuencia: {label: "Frecuencia"},
        nombre_del_evento: {label: "Nombre del evento"},
        fecha: {label: "Fecha"},
        lugar: {label: "Lugar"},
        comite_organizador: {label: "Comité organizador"},
        isbn: {label: "ISBN"},
        materias: {label: "Materias"},
        descriptores: {label: "Descriptores"},
        notas: {label: "Notas"},
        revista_indizada_en: {label: "Revista indizada en"},
        localizacion_fisica: {label: "Localización física"},
        localizacion_electronica: {label: "Localización electrónica", type: "link", linkLabel: "---Acceder al sitio web"},
    };

    // Array con los codigos de los metadatos (para determinar la posicion de inserción)
    var metadataKeys = $.map(metadataList, function(value, key) { return key; });

    // Array con los codigos de los metadatos ordenados alfabeticamente (para la combo)
    var sortedMetadataKeys = $.map(metadataList, function(value, key) { return key; });
    sortedMetadataKeys.sort();
    
    
    //////////////////////////////////////////////////////////////////
    // Creación de los elementos DOM necesarios
    //////////////////////////////////////////////////////////////////

    var selectContainer = $("<div class='selection'></div>");

    // Combo con los metadatos
    var combo_text = "<select name='metadata'>";
    
    $(sortedMetadataKeys).each(function(key, value){
    	combo_text += "<option value='"+value+"'>"+metadataList[value].label+"</option>";
    });
    
    combo_text += "</select>";

    var combo = $(combo_text);
    selectContainer.append(combo);

    // Caja de texto para la carga de metadatos
    var textarea = $("<textarea name='metadata_"+self.attr('name')+"'></textarea>");
    selectContainer.append(textarea);

    // Boton de agregar
    var agregar = $("<input type='button' class='add_metadata' value='Agregar'></input>");
    selectContainer.append(agregar);


    //////////////////////////////////////////////////////////////////
    // Contenedor para los metadatos cargados
    // Crea el contenedor principal y junta todos los controles
    //////////////////////////////////////////////////////////////////
    var resultsContainer = $("<div id='loaded_"+self.attr('name')+"' class='generator_list'></div>");

    var mainContainer = $("<div class='metadataGenerator' id='generator_"+self.attr('name')+"'></div>");
    mainContainer.append(selectContainer);
    mainContainer.append(resultsContainer);

    // Oculta el target
    self.css('display', 'none');

    // Agrega el generador al DOM
    self.parent().append(mainContainer);


    // Se cargan los metadatos pre-existentes (si es un texto html valido)
    var parsedContent = $( self.val().trim() );
    if(parsedContent.find("span.label").length == 0)
    	parsedContent = "";

	resultsContainer.append( parsedContent );

    // Creacion del Sortable de jQuery
    resultsContainer.sortable({
        update: function(event, ui) {
            updateTarget();
        },
        placeholder: "placeholder",
        helper: 'original',
        handle: 'span.label',
        tolerance: 'pointer'
    });

    //////////////////////////////////////////////////////////////////
    // Gestion de eventos
    //////////////////////////////////////////////////////////////////

    // Boton para agregar metadatos
    agregar.click(function() {
        if(textarea.val().trim() == "")
            return false;

        addMetadata(combo.val(), textarea.val());
        textarea.val("");
        combo.focus();
    });

    $(".metadataGenerator .generator_list span.value").click( function(event, ui) {
        event.preventDefault();
        removeEditMetadata( event, ui, $(this) );
    });

    //////////////////////////////////////////////////////////////////
    // Funciones auxiliares
    //////////////////////////////////////////////////////////////////

    var getInsertPosition = function(key) {
    	return $.inArray(key, metadataKeys);    	
    }
    
    var addMetadata = function(key, value) {
    	
        var metadataElement = resultsContainer.find("."+key);
        
        if(metadataElement.length == 0) {
            metadataElement = createMetadataElement(key);
            
        	// Buscamos la posicion donde insertar el nuevo elemento 
            // (si el container está vacío el bucle no modifica nada)
        	var currentInsertPosition = getInsertPosition(key);
        	var inserted = false;
        	resultsContainer.find("div").each(function() {
        		var metadataInsertPosition = getInsertPosition( $(this).attr('class') );
        		if(metadataInsertPosition > currentInsertPosition) {
        			// Insertamos el elemento como predecesor
        			metadataElement.insertBefore(this);
        			inserted = true;
        			return false;
        		}
        	});

        	// Si el container estaba vacío, o no había ningún elemento con indice mayor, 
        	// insertamos como último elemento
            if(!inserted) {
	        	resultsContainer.append(metadataElement);
            }
        }
        
        // Si tiene un tipo especial definido, hacemos el procesamiento adicional
        if(metadataList[key].type && metadataList[key].type != "text") {
        	value = typeHandlers[ metadataList[key].type ].encode(key, value);
        }
        
        metadataElement.append( createValueElement(metadataElement, key, value) );
        updateTarget();
    };

    // Boton para editar/eliminar metadatos
    var removeEditMetadata = function(event, ui, target) {
        var key = target.parent().attr('class');
        var value = target.html().replace(/&amp;/g, '&');

        // Si tiene un tipo especial definido, hacemos el procesamiento de decodificacion
        if(metadataList[key].type && metadataList[key].type != "text") {
        	value = typeHandlers[ metadataList[key].type ].decode(key, value);
        }

        textarea.val(value);
        combo.val(key);

        // Elimino el elemento y el posible ";" a su lado
        if(target.parent().find(".value").length > 1) {
            if(target.get(0).previousSibling.nodeType != null && target.get(0).previousSibling.nodeType == Node.TEXT_NODE)
                $(target.get(0).previousSibling).remove();
            else if (target.get(0).nextSibling.nodeType != null && target.get(0).nextSibling.nodeType == Node.TEXT_NODE)
                $(target.get(0).nextSibling).remove();

            target.remove();
        } else {
            // Si fue el ultimo elemento, elimino el padre
            target.parent().remove();
        }

        // Actualizamos el target principal
        updateTarget();
    }

    var updateTarget = function() {
        self.val( resultsContainer.html() );
    }

    var createValueElement = function(parent, key, value) {
        if(parent.find(".value").length > 0)
            parent.append("; ");

        var values = value.split(";");
        var element_text = "";
        for(i in values) {
            var v = values[i];
            if(v.trim() != "") {
                element_text += "<span class='value'>"+v.trim()+"</span>";
                if(i < values.length-1)
                    element_text += "; ";
            }
        }

        var element = $(element_text);

        // Asigna el handle para edicion/eliminacion
        element.click( function(event, ui) {
            event.preventDefault();
            removeEditMetadata( event, ui, $(this) );
        });

        return element;
    }

    var createMetadataElement = function(key) {
        return $("<div class='"+key+"'><span class='label'>"+metadataList[key].label+": </span></div>");
    }

  }
})( jQuery );

