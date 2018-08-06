/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
(function ($) {

    $(document).ready(function() {
        initializeGear();
        initializeFilters();
    });

    function initializeGear(){
        var gearControlsDivs = $('div#aspect_discovery_SimpleSearch_div_search-controls-gear');
        jQuery.each(gearControlsDivs, function(index, value) {
            var gearControlsDiv = $(value);
            var gearControls = gearControlsDiv.find('ul#aspect_discovery_SimpleSearch_list_sort-options');
            var gearButton = $('<button class="discovery-controls-gear ds-button-field"><div class="gear-icon">&nbsp;</div></button>');

            gearButton.click(function(){
                if(gearControls.is(':visible')){
                    gearControls.hide();
                }else{
                    gearControls.show();
                }
                return false;
            });
                    //Disable the default click
            gearControls.find('li.gear-option,li.gear-option a').click(function(event){
                //Ensure that our html click isn't called !
                event.stopPropagation();
                var $link = $(this);
                var listItem;
                if($link.is('li')){
                    listItem = $link;
                }else{
                    listItem = $link.parents('li:first');
                }

                //Check if this option is currently selected, if so skip the next stuff
                if(listItem.hasClass('gear-option-selected')){
                    return false;
                }
                if(!$link.attr('href')){
                    $link = $link.find('a');
                }
                //Retrieve the params we are to fill in in our main form
                var params = $link.attr('href').split('&');

                var mainForm = $('form#aspect_discovery_SimpleSearch_div_main-form');
                //Split them & fill in in the main form, when done submit the main form !
                for(var i = 0; i < params.length; i++){
                    var param = params[i].split('=')[0];
                    var value = params[i].split('=')[1];

                    mainForm.find('input[name="' + param + '"]').val(value);
                }

                //DS-3835 ensure that the current scope is passed as form field "scope"
                mainForm.find('input[name="current-scope"]')
                    .val($('select[name="scope"]').val())
                    .attr("name","scope");

                //Clear the page param
                mainForm.find('input[name="page"]').val('1');

                mainForm.submit();
                return false;
            });

            gearButton.prependTo(gearControlsDiv);
            gearControls.hide();

            $('html').click(function() {
                //Hide the menus if visible
                if(gearControls.is(':visible')){
                    gearControls.hide();
                }
            });


        });

    }

    function initializeFilters(){
        //Initialize the show filters link
        $('a[href="display-filters"]').click(function(){
            var filtersForm = $('form#aspect_discovery_SimpleSearch_div_search-filters');
            filtersForm.show();
            filtersForm.css('visibility', 'visible');
            $(this).hide();
            return false;
        });

        $('input[name^="add-filter_"]').click(function(){
            addFilterRow();

            //Disable "normal" button actions
            return false;
        });
        $('input[name^=remove-filter_]').click(function(){
            removeFilterRow($(this));
            return false;
        });
        //Disable the enter key !
        $('input[name^=filter_][type=text]').keypress(function(event){
            if(event.which == 13){
                //Entered pressed, do NOT submit the form, add a new filter instead !
                addFilterRow();
                event.preventDefault();
            }
        });

    }

    function removeFilterRow(button){
        var parentRow = button.parents('tr:first');

        //Check if we are removing a new filter AND our last new filter at that, if so removes values instead of the row
        if(parentRow.is('[id^="aspect_discovery_SimpleSearch_row_filter-new-"]')
            && parentRow.parents('table:first').find('tr[id^="aspect_discovery_SimpleSearch_row_filter-new-"]').length == 1){
            //Hide ourselves & clear our values!
            parentRow.find('input[type=text]", select').val('');
        }else{
            if(parentRow.is('[id^="aspect_discovery_SimpleSearch_row_used-filters-"]') && parentRow.parents('table:first').find('tr[id^="aspect_discovery_SimpleSearch_row_used-filters-"]').length == 1)
            {
                parentRow.next().remove();
                parentRow.prev().remove();
            }
            parentRow.remove();
        }
    }

    function addFilterRow(){
        var previousFilterRow = $('tr#aspect_discovery_SimpleSearch_row_filter-controls').prev();
        //Duplicate our element & give it a new index !
        var newFilterRow = previousFilterRow.clone();
        //Alter the html to update the index
        var rowIdentifier = newFilterRow.attr('id');
        var oldIndex = parseInt(rowIdentifier.substr(rowIdentifier.lastIndexOf('-')+1, rowIdentifier.length));
        //Add one to the index !
        var newIndex = oldIndex + 1;
        //Update the index of all inputs & our list
        newFilterRow.attr('id', newFilterRow.attr('id').replace('-' + oldIndex, '-' + newIndex));
        newFilterRow.find('input, select').each(function(){
            var $this = $(this);
            //Update the index of the name (if present)
            $this.attr('name', $this.attr('name').replace('_' + oldIndex, '_' + newIndex));
            $this.attr('id', $this.attr('id').replace('_' + oldIndex, '_' + newIndex));
        });
        //Clear the values
        newFilterRow.find('input[type=text], select').val('');

        previousFilterRow = newFilterRow.insertAfter(previousFilterRow);
        //Initialize the add button
        previousFilterRow.find('input[name^="add-filter_"]').click(function(){
            addFilterRow();
            return false;
        });
        //Initialize the remove button
        previousFilterRow.find('input[name^=remove-filter_]').click(function(){
            removeFilterRow($(this));
            return false;
        });
    }
})(jQuery);