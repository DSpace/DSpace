/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
(function($) {
    $(init_community_list);

    function init_community_list() {
        $('.community-browser-row .toggler').click(function() {
            var parent_row, parent_toggler, parent_wrappers, other_wrappers, other_toggler_rows, all_rows, target, $this, target_id, open_icon, closed_icon;
            $this = $(this);

            $('.current-community-browser-row').removeClass('current-community-browser-row')
                                                .find('a strong').contents().unwrap();

            target_id = $this.data('target');

            parent_wrappers = $this.parents('.sub-tree-wrapper');
            other_wrappers = $('.sub-tree-wrapper:not(' + target_id + ')').not(parent_wrappers);
            other_wrappers.addClass('hidden');

            other_toggler_rows = $([]);
            other_wrappers.each(function() {
                other_toggler_rows = other_toggler_rows.add(get_toggler_from_wrapper($(this)).closest('.community-browser-row'));
            });
            other_toggler_rows.removeClass('open-community-browser-row').addClass('closed-community-browser-row');

            parent_row = $this.closest('.community-browser-row');

            open_icon = $this.find('.open-icon');
            closed_icon = $this.find('.closed-icon');

            all_rows = $('.community-browser-row');
            clear_relation_classes(all_rows);
            target = $(target_id);
            if (target.is(':visible')) {
                target.addClass('hidden');
                open_icon.addClass('hidden');
                closed_icon.removeClass('hidden');
                parent_row.removeClass('open-community-browser-row').addClass('closed-community-browser-row');
                target.find('.open-icon').addClass('hidden');
                target.find('.closed-icon').removeClass('hidden');
                parent_toggler = get_toggler_from_wrapper($this.closest('.sub-tree-wrapper'));
                if (parent_toggler.length > 0) {
                    parent_toggler.closest('.community-browser-row').addClass('current-community-browser-row').find('a').wrapInner( "<strong></strong>");
                    set_relation_classes(all_rows, parent_toggler, $(parent_toggler.data('target')), parent_toggler.parents('.sub-tree-wrapper'));
                }
            }
            else {
                target.removeClass('hidden');
                open_icon.removeClass('hidden');
                closed_icon.addClass('hidden');
                parent_row.removeClass('closed-community-browser-row')
                        .addClass('open-community-browser-row')
                        .addClass('current-community-browser-row')
                        .find('a').wrapInner( "<strong></strong>");
                set_relation_classes(all_rows, $this, target, parent_wrappers);
            }
            set_odd_even_rows();
        }).bind('touchend', function () {
            $(this).mouseout();
        });
        set_odd_even_rows();
    }

    function set_relation_classes(all_rows, $this, target, parent_wrappers) {
        var related_rows, unrelated_rows, parent_rows;
        unrelated_rows = all_rows.not($this.parents('.community-browser-row')).not(target.find('.community-browser-row'));
        related_rows = parent_wrappers.find('.community-browser-row');
        parent_rows = $([]);
        parent_wrappers.each(function () {
            var toggler;
            toggler = get_toggler_from_wrapper($(this));
            parent_rows = parent_rows.add(toggler.parents('.community-browser-row'));
        });
        related_rows = unrelated_rows.filter(related_rows).not(parent_rows);
        unrelated_rows = unrelated_rows.not(related_rows).not(parent_rows);
        if (related_rows.length === 0 && unrelated_rows.length > 0) {
            related_rows = unrelated_rows;
            unrelated_rows = $([]);
        }
        related_rows.addClass('related-community-browser-row hidden-xs');
        related_rows.find('.open-icon').addClass('hidden');
        related_rows.find('.closed-icon').removeClass('hidden');
        unrelated_rows.addClass('unrelated-community-browser-row hidden-xs');
    }

    function clear_relation_classes(all_rows) {
        all_rows.removeClass('related-community-browser-row hidden-xs').removeClass('unrelated-community-browser-row hidden-xs');
    }

    function get_toggler_from_wrapper(wrapper) {
        return $('a[data-target="#' + wrapper.attr('id') + '"]');
    }

    function set_odd_even_rows() {
        var visible_rows = $('.community-browser-row:visible');
        visible_rows.removeClass('odd-community-browser-row');
        visible_rows = visible_rows.not('.open-community-browser-row');
        visible_rows.filter(':odd').addClass('odd-community-browser-row');
    }


})(jQuery);