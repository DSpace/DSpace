$(document).ready(function(){
    var height_default  = 800
      , width_default   = 800
      , height          = height_default 
      , width           = width_default
      , height_id       = 'resize-height'
      , width_id        = 'resize-width'
      , select_id       = 'server-select'
      , target_id       = 'dryad-ddw-target'
      , script_id       = 'dryad-ddw-script'
      , path            = 'widgets/v1/display'
      , wrapper_id      = 'dryad-ddw-wrapper'
      , js_file         = 'loader.js?referrer=JDryadDev&wrapper=' + target_id
      , doi_regex       = new RegExp('^doi:10.5061');
    $('#update-preview').on('click', function(evt) {
        var base = $('#' + select_id).find('option[selected]').val()
          , doi = $('#doi').val()
          , url = [base, path, doi, js_file].join('/')
          , $script = $('<script async="true" type="text/javascript"'
                    + ' id="'  + script_id  + '"' 
                    + ' src="' + url        + '"'
                    + '></script>');
        doi.trim();
        if (doi.match(doi_regex)) {
            dimension_wrapper(height,width);
            if ($('#' + script_id).length > 0) {
                $('#' + script_id).remove();    
            }
            $(body).append($script);
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
        dimension_wrapper(height,width);
        evt.preventDefault();
   });
   function dimension_wrapper(h,w) {
        $('#' + height_id).val(h);
        $('#' + width_id).val(w);
        $('#' + wrapper_id).height(h).width(w);
   }
});