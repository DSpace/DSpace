/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
(function($) {
    var records_template = DSpace.getTemplate('records');

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

    $('#aspect_submission_StepTransformer_field_submit_lookup').click(function(event){
        event.preventDefault();
        var searchInput = "";

        $("input[id^='aspect_submission_StepTransformer_field_'][type='text']").each(function () {
            if($(this).val()) {

                if (searchInput != "") {
                    searchInput += "&";
                }
                var n = $(this).attr('id').lastIndexOf('_');
                searchInput += $(this).attr('id').substring(n + 1) + "=" + $(this).val();
            }
        });

        startLookup(searchInput,0)
    });

    function startLookup(searchInput,start) {
        $.ajax({url: window.import.contextPath+"/json/submissionLookup?" + searchInput +"&start="+start,
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

                $(".records-import-btn").click(function(event) {
                    event.preventDefault();
                    var eid = $(this).attr("id").substring('records-import-'.length);
                    $("#aspect_submission_StepTransformer_field_import_id").val(eid);
                    $("#aspect_submission_StepTransformer_div_StartSubmissionLookupStep").submit();
                });
            }
        });
    }

    function fillModal(info){
        var lookupModal = $('#lookup-search-results');

        var htmlData;
        if(info.records.length>0) {
            htmlData = html = records_template(info);
        }
        else {
            htmlData = html = "<p>No records found</p>";
        }
        lookupModal.find('.modal-body').html(htmlData);
        lookupModal.modal('show');
    }

    function setPagination(start, info, searchInput){
        if(start + info.records.length<info.total){
            $("#import-pagination-next").attr("disabled", false);
        }
        else {
            $("#import-pagination-next").attr("disabled", true);
        }

        if(start>0){
            $("#import-pagination-previous").attr("disabled", false);
        }
        else {
            $("#import-pagination-previous").attr("disabled", true);
        }

        $("#import-pagination-previous").unbind("click");
        $("#import-pagination-previous").click(function(event) {
            $( this ).unbind( event );
            event.preventDefault();
            startLookup(searchInput, start - 20);
        });

        $("#import-pagination-next").unbind("click");
        $("#import-pagination-next").click(function(event) {
            event.preventDefault();
            startLookup(searchInput, start + 20);
        });
    }

    function centerModal() {
        var $dialog  = $(this).find(".modal-dialog");
        var offset = $(window).scrollTop() - $("header").height() - $(".trail-wrapper").height();
        $dialog.css("margin-top", offset);

        $(".modal-body").css("height", "auto");

        if($dialog.height()>window.innerHeight){
            $(".modal-body").height(window.innerHeight - ($(".modal-header").height() + $(".modal-footer").height() + 100));
        }
        else {
            $dialog.css("margin-top", offset + (window.innerHeight - $dialog.height())/2 );
        }
    }

    $(document).on('shown.bs.modal', '.modal', centerModal);
    $(window).on("resize", function () {
        $('.modal:visible').each(centerModal);
    });

})(jQuery);