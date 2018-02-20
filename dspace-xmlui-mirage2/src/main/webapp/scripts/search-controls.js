/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
(function($){
    var advanced_filters_template, simple_filters_template;

    Handlebars.registerHelper('set_selected', function(value, options) {
        var $el = $('<select />').html( options.fn(this) );
        $el.find('[value=' + value + ']').attr({'selected':'selected'});
        return $el.html();
    });

    if (typeof window.DSpace.discovery !== 'undefined') {
        DSpace.discovery.start_index = 1;

        $(function () {
            calculateFilterIndices();
            backupOriginalFilters();
            assignGlobalEventHandlers();
            renderSimpleFilterSection();
            renderAdvancedFilterSection();
        });
    }





    function getAdvancedFiltersTemplate() {
        if (!advanced_filters_template) {
            advanced_filters_template = DSpace.getTemplate('discovery_advanced_filters');
        }

        return advanced_filters_template;
    }

    function getSimpleFiltersTemplate() {
        if (!simple_filters_template) {
            simple_filters_template = DSpace.getTemplate('discovery_simple_filters');
        }

        return simple_filters_template;
    }

    function getNextFilterIndex() {
        return DSpace.discovery.start_index + DSpace.discovery.filters.length;
    }

    function addNewFilter(index, type, relational_operator, query) {
        if (typeof index === 'number') {
            DSpace.discovery.filters.splice(index - DSpace.discovery.start_index, 0, {
                index: index,
                type: type,
                relational_operator: relational_operator,
                query: query
            });
            calculateFilterIndices();
        }
        else {
            DSpace.discovery.filters.push({
                index: getNextFilterIndex(),
                type: type,
                relational_operator: relational_operator,
                query: query
            });
        }
    }

    function getIndexFromFilterRow(filterRow) {
        return /filter-new-(\d+)/.exec(filterRow.attr('id'))[1] * 1;
    }

    function updateFilterValues(filterRow) {
        var index, type, relational_operator, query, filter;
        index = getIndexFromFilterRow(filterRow);
        type = filterRow.find('select[name^="filtertype_"]').val();
        relational_operator = filterRow.find('select[name^="filter_relational_operator_"]').val();
        query = filterRow.find('input[name^="filter_"]').val();
        filter = {
            index: index,
            type: type,
            relational_operator: relational_operator,
            query: query
        };
        replaceFilter(filter);
    }

    function replaceFilter(filter) {
        for (var i = 0; i < DSpace.discovery.filters.length; i++) {
            if (DSpace.discovery.filters[i].index === filter.index) {
                DSpace.discovery.filters[i] = filter;
                break;
            }
        }
        calculateFilterIndices();
    }

    function calculateFilterIndices() {
        for (var i = 0; i < DSpace.discovery.filters.length; i++) {
            DSpace.discovery.filters[i].index = i + DSpace.discovery.start_index;
        }
    }

    function removeFilterAtIndex(index) {
        for (var i = 0; i < DSpace.discovery.filters.length; i++) {
            var filter = DSpace.discovery.filters[i];
            if (filter.index === index) {
                DSpace.discovery.filters.splice(i, 1);
                break;
            }
        }
        calculateFilterIndices();
    }

    function renderAdvancedFilterSection() {
        var template, html, wrapper;

        if (DSpace.discovery.filters.length === 0) {
            addNewFilter(null, null, null, '');
        }

        template = getAdvancedFiltersTemplate();
        html = template({
            filters: DSpace.discovery.filters,
            i18n: DSpace.i18n.discovery
        });

        unAssignAdvancedFilterEventHandlers(); //prevents memory leaks
        $('#new-filters-wrapper').remove();
        wrapper = $('<div id="new-filters-wrapper"/>').html(html);
        $('#aspect_discovery_SimpleSearch_row_filter-controls').before(wrapper);
        assignAdvancedFilterEventHandlers();
    }

    function renderSimpleFilterSection() {
        var template, html, wrapper;

        if (DSpace.discovery.filters.length > 0) {
            $('.active-filters-label').removeClass('hidden');
        }

        template = getSimpleFiltersTemplate();
        html = template({
            filters: DSpace.discovery.orig_filters,
            i18n: DSpace.i18n.discovery
        });

        unAssignSimpleFilterEventHandlers();
        $('#filters-overview-wrapper').remove();
        wrapper = $('<div id="filters-overview-wrapper"/>').html(html);
        $('#filters-overview-wrapper-squared').html('').append(wrapper);
        assignSimpleFilterEventHandlers();
    }

    function assignSimpleFilterEventHandlers() {
        $('#filters-overview-wrapper .label').click(function (e) {
            var index = $(this).data('index');
            removeFilterAtIndex(index);
            renderAdvancedFilterSection();
            $('#aspect_discovery_SimpleSearch_div_search-filters').submit();
            return false;
        });
    }

    function unAssignSimpleFilterEventHandlers() {
        $('#filters-overview-wrapper .label').off();
    }

    function assignAdvancedFilterEventHandlers() {
        var $filters = $('.search-filter');
        $filters.find('select, input').change(function() {
            updateFilterValues($(this).closest('.search-filter'));
            renderAdvancedFilterSection();
        });
        $filters.find('.filter-control.filter-add').click(function (e) {
            var index = getIndexFromFilterRow($(this).closest('.search-filter'));
            addNewFilter(index + 1, null, null, '');
            renderAdvancedFilterSection();
            return false;
        });
        var $removeButtons = $filters.find('.filter-control.filter-remove');
        $removeButtons.click(function (e) {
            var index = getIndexFromFilterRow($(this).closest('.search-filter'));
            removeFilterAtIndex(index);
            renderAdvancedFilterSection();
            return false;
        });
    }

    function unAssignAdvancedFilterEventHandlers() {
        var $filters = $('.search-filter');
        $filters.find('select, input').off();
        $filters.find('.filter-control.filter-add').off();
        $filters.find('.filter-control.filter-remove').off();
    }

    function assignGlobalEventHandlers() {
        $('.show-advanced-filters').click(function () {
            var wrapper = $('#aspect_discovery_SimpleSearch_div_discovery-filters-wrapper');
            wrapper.parent().find('.discovery-filters-wrapper-head').hide().removeClass('hidden').fadeIn(200);
            wrapper.hide().removeClass('hidden').slideDown(200);
            $(this).addClass('hidden');
            $('.hide-advanced-filters').removeClass('hidden');
            return false;
        });

        $('.hide-advanced-filters').click(function () {
            var wrapper = $('#aspect_discovery_SimpleSearch_div_discovery-filters-wrapper');
            wrapper.parent().find('.discovery-filters-wrapper-head').fadeOut(200, function() {
                $(this).addClass('hidden').removeAttr('style');
            });
            wrapper.slideUp(200, function() {
                $(this).addClass('hidden').removeAttr('style');
            });
            $(this).addClass('hidden');
            $('.show-advanced-filters').removeClass('hidden');
            return false;
        });

        $('#aspect_discovery_SimpleSearch_field_submit_reset_filter').click(function() {
            restoreOriginalFilters();
            calculateFilterIndices();
            renderAdvancedFilterSection();
            return false;
        });

        $('.discovery-add-filter-button').click(function() {
            addNewFilter(null, null, null, '');
            renderAdvancedFilterSection();
            return false;
        });

        $('.controls-gear-wrapper').find('li.gear-option,li.gear-option a').click(function(event){
            var value, param, mainForm, params, listItem, $this;
            event.stopPropagation();
            $this = $(this);
            if($this.is('li')){
                listItem = $this;
            }else{
                listItem = $this.parents('li:first');
            }

            //Check if this option is currently selected, if so skip the next stuff
            if(listItem.hasClass('gear-option-selected')){
                return false;
            }
            if(!$this.attr('href')){
                $this = $this.find('a');
            }
            //Retrieve the params we are to fill in in our main form
            params = $this.attr('href').split('&');

            mainForm = $('#aspect_discovery_SimpleSearch_div_main-form');
            //Split them & fill in in the main form, when done submit the main form !
            for(var i = 0; i < params.length; i++){
                param = params[i].split('=')[0];
                value = params[i].split('=')[1];

                mainForm.find('input[name="' + param + '"]').val(value);
            }

            //DS-3835 ensure that the current scope is passed as form field "scope"
            mainForm.find('input[name="current-scope"]')
                .val($('select[name="scope"]').val())
                .attr("name","scope");

            //Clear the page param
            mainForm.find('input[name="page"]').val('1');

            mainForm.submit();
            $this.closest('.open').removeClass('open');
            return false;
        });
    }

    function backupOriginalFilters() {
        DSpace.discovery.orig_filters = DSpace.discovery.filters.slice(0);
    }

    function restoreOriginalFilters() {
        DSpace.discovery.filters = DSpace.discovery.orig_filters.slice(0);
    }

})(jQuery);