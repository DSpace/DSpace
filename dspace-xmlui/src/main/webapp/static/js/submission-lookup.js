/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
(function($) {
   if (DSpace === undefined) DSpace= {};
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
                dataType :'text',
                mimeType :'text/plain',
                async : false
            });
        }
        return DSpace.templates[name];
    };
    var publication_records_template = DSpace.getTemplate('publication_records');

    Handlebars.registerHelper('ifCond', function (v1, operator, v2, options) {

        switch (operator) {
            case '==':
                return (v1 == v2) ? options.fn(this) : options.inverse(this);
            case '<':
                return (v1 < v2) ? options.fn(this) : options.inverse(this);
            default:
                return options.inverse(this);
        }
    });

    $('#aspect_submission_StepTransformer_field_lookup').click(function(event){
        event.preventDefault();

        var searchInput = $('#aspect_submission_StepTransformer_field_search').val();
        startLookup(searchInput,0);
    });

    function startLookup(searchInput,start) {
        $.ajax({url: window.publication.contextPath+"/json/submissionLookup?search=" + searchInput +"&start="+start,
            type: "POST",
            dataType: "json",
            async: true,
            contentType: "application/x-www-form-urlencoded;charset=UTF-8",
            error: function(xhr, status, error){
                var err = eval("(" + xhr.responseText + ")");
                alert(err.Message);
            },
            success: function(info) {
                info.shownStart = start + 1;
                info.shownCount = start + info.records.length;

                fillModal(info);
                setPagination(start,info,searchInput);

                $(".publication-records-import-btn").click(function(event) {
                    event.preventDefault();
                    var pmid = $(this).attr("id").substring( $(this).attr("id").lastIndexOf("-") + 1);

                    $("#aspect_submission_StepTransformer_field_publication_id").val(pmid);
                    $("#aspect_submission_StepTransformer_div_StartSubmissionLookupStep").submit();
                });
            }
        });
    }

    function fillModal(info){
        var lookupModal = $('#lookup-search-results');
        lookupModal.removeClass('hidden');
        var htmlData;
        if(info.records.length>0) {
            htmlData = html = publication_records_template(info);
        }
        else {
            htmlData = html = "<p>No records found</p>";
        }
        lookupModal.find('.modal-body').html(htmlData);
        if((typeof $().modal == 'function')){
            lookupModal.modal('show');
        }
    }

    function setPagination(start, info, searchInput){
        if(start + info.records.length<info.total){
            $("#publication-pagination-next").attr("disabled", false);
        }
        else {
            $("#publication-pagination-next").attr("disabled", true);
        }

        if(start>0){
            $("#publication-pagination-previous").attr("disabled", false);
        }
        else {
            $("#publication-pagination-previous").attr("disabled", true);
        }

        $("#publication-pagination-previous").unbind("click");
        $("#publication-pagination-previous").click(function(event) {
            $( this ).unbind( event );
            event.preventDefault();
            startLookup(searchInput, start - 20);
        });

        $("#publication-pagination-next").unbind("click");
        $("#publication-pagination-next").click(function(event) {
            event.preventDefault();
            startLookup(searchInput, start + 20);
        });
        $('.close-modal-results').click(function(){
            $('#lookup-search-results').addClass('hidden');
        });
    }

})(jQuery);