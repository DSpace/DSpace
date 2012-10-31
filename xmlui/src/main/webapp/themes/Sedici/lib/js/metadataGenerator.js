
(function( $ ){

  $.fn.metadataGenerator = function() {

    // Guardamos la referencia al textarea sobre el que se aplica este plugin
    var self = this;

    // Si no hay elementos que matcheen con el selector, se termina la ejecucion
    if(self.length == 0)
    	return;

    // Lista de metadatos para ofrecer
    // TODO Sería genial si esto pudiese levantarse de la base de datos o de la configuracion
    var metadataList = {
        comite_de_referato: "Comité de referato",
        comite_editorial: 'Comité editorial',
        comite_organizador: "Comité organizador",
        consejo_asesor: "Consejo asesor",
        descriptores: 'Descriptores',
        director: 'Director',
        dossier: "Dossier",
        editor: 'Editor',
        editorial: "Editorial",
        entidad_de_origen: 'Entidad de Origen',
        fecha_de_publicacion: 'Fecha de publicación',
        fecha: "Fecha",
        frecuencia: 'Frecuencia',
        isbnl: "ISSN-L",
        isbn: "ISBN",
        issn: 'ISSN',
        localizacion_electronica: 'Localización electrónica',
        localizacion_fisica: 'Localización física',
        lugar: "Lugar",
        materias: 'Materias',
        nombre_del_evento: "Nombre del evento",
        notas: 'Notas',
        revista_indizada_en: 'Revista indizada en',
        secretaria_de_redaccion: 'Secretaría de redacción',
        subdirector: 'Subdirector',
        subtitulo_de_la_serie: 'Subtítulo de la serie',
        titulo_de_la_serie: 'Título de la serie',
        texto_libre: 'Texto libre'
    };


    //////////////////////////////////////////////////////////////////
    // Creación de los elementos DOM necesarios
    //////////////////////////////////////////////////////////////////

    var selectContainer = $("<div class='selection'></div>");

    // Combo con los metadatos
    var combo_text = "<select name='metadata'>";
    for(m in metadataList) {
        combo_text += "<option value='"+m+"'>"+metadataList[m]+"</option>";
    }
    combo_text += "</select>";

    var combo = $(combo_text);
    selectContainer.append(combo);

    // Caja de texto para la carga de metadatos
    var textarea = $("<textarea name='metadata_"+self.attr('name')+"'></textarea>");
    selectContainer.append(textarea);

    // Boton de agregar
    var agregar = $("<input type='button' class='add_metadata' value='Agregar'></input>");
    selectContainer.append(agregar);

    // Boton de agregar como link
    var agregar_link = $("<input type='button' class='add_metadata_link' value='Agregar como Link'></input>");
    selectContainer.append(agregar_link);


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


    // Se cargan los metadatos pre-existentes
    resultsContainer.append( self.val() );

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

    // Boton para agregar metadatos como links a un sitio web
    agregar_link.click(function() {
        if(textarea.val().trim() == "")
            return false;

        // Intentamos identificar si no se trata de un tag A previamente cargado
        var anchor_regexp = /<a.*href=.*>/;

        var link_text = textarea.val();
        if(!anchor_regexp.test(link_text))
            link_text = "<a target='_blank' href='"+link_text+"'>Acceder al sitio web</a>";

        addMetadata(combo.val(), link_text);
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

    var addMetadata = function(key, value) {
        var metadataElement = resultsContainer.find("."+key);
        if(metadataElement.length == 0) {
            metadataElement = createMetadataElement(key, value);
            resultsContainer.append(metadataElement);
        }
        metadataElement.append( createValueElement(metadataElement, value) );
        updateTarget();
    };

    // Boton para editar/eliminar metadatos
    var removeEditMetadata = function(event, ui, target) {
        var key = target.parent().attr('class');

        textarea.val(target.html());
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

    var createValueElement = function(parent, value) {
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

    var createMetadataElement = function(key, value) {
        return $("<div class='"+key+"'><span class='label'>"+metadataList[key]+": </span></div>");
    }

  }
})( jQuery );

