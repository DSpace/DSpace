$(document).ready(function(){
    var height_default  = 600
      , width_default   = 800
      , height          = height_default 
      , width           = width_default
      , height_id       = 'resize-height'
      , width_id        = 'resize-width'
      , select_id       = 'server-select'
      , target_id       = 'dryad-ddw-target'
      , script_id       = 'dryad-ddw-script'
      , css_id          = 'dryad-ddw-css'
      , path            = 'widgets/v1/display'
      , js_file         = 'loader.js?referrer=JDryadDev&wrapper=' + target_id
      , doi_regex       = new RegExp('^doi:10.5061');
    $('#update-preview').on('click', function(evt) {
        var base = $('#' + select_id).find('option:selected').val()
          , doi = $('#doi').val()
          , url = [base, path, doi, js_file].join('/')
          , $script = $('<script async="true" type="text/javascript"'
                    + ' id="'  + script_id  + '"' 
                    + ' src="' + url        + '"'
                    + '></script>')
          , $css = $('<link type="text/css" rel="stylesheet" id="' + css_id + '" href="' + base + '/static/css/widgets/display/dryad-ddw.min.css">');
        doi.trim();
        if (doi.match(doi_regex)) {
            dimension_target(height,width);
            $('#' + target_id).children().remove();
            $('#' + script_id).remove();
            $('#' + css_id).remove();
            try {
                $('body').append($css);
                $('body').append($script);
            } catch (e) {
                console.log("Error: " + e.toString());
            }
        } else {
            alert('The doi value "' + doi + '" does not look like a Dryad doi');
        }
        evt.preventDefault();
   });
   $('#resize-preview').on('click', function(evt) {
        height = $('#' + height_id).val()
                ? $('#' + height_id).val()
                : height !== undefined
                    ? height
                    : height_default;
        width = $('#' + width_id).val()
                ? $('#' + width_id).val()
                : width !== undefined
                    ? width
                    : width_default;
        dimension_target(height,width);
        $('#update-preview').click();
        evt.preventDefault();
   });
    function dimension_target(h,w) {
        $('#' + height_id).val(h);
        $('#' + width_id).val(w);
        $('#' + target_id).height(h).width(w);
    }
});
