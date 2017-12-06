/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
(function($) {
    DSpace.getTemplate = function(name) {
        if (DSpace.dev_mode || DSpace.templates === undefined || DSpace.templates[name] === undefined) {
            $.ajax({
                url : DSpace.theme_path + 'templates/' + name + '.hbs',
                success : function(data) {
                    if (DSpace.templates === undefined) {
                        DSpace.templates = {};
                    }
                    DSpace.templates[name] = Handlebars.compile(data);
                },
                async : false
            });
        }
        return DSpace.templates[name];
    };
})(jQuery);