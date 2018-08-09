/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
(function($) {
    $(document).ready(function(){
        //Find any controlled vocabulary urls this page might contain
        var vocabularyUrls = $('a[href^="vocabulary:"]');
        vocabularyUrls.click(function(index){
            var $link = $(this);
            var vocabularyJSONUrl = $link.attr('href').replace('vocabulary:', '');
            //Retrieve the basic url, we will need this to add images !
            var basicUrl = vocabularyJSONUrl.substr(0, vocabularyJSONUrl.indexOf('/JSON/controlled-vocabulary'));
            var parameters = vocabularyJSONUrl.slice(vocabularyJSONUrl.indexOf('?') + 1, vocabularyJSONUrl.length).split('&');

            //Read the input field name & the vocabulary identifier from the url
            var inputFieldName;
            var vocabularyIdentifier;
            for(var i = 0; i < parameters.length; i++){
                var parameter = parameters[i].split('=')[0];
                var value = parameters[i].split('=')[1];

                if(parameter == 'vocabularyIdentifier'){
                    vocabularyIdentifier = value;
                }else
                if(parameter == 'metadataFieldName'){
                    inputFieldName = value;
                }
            }

            var id = 'modal_vocabulary_dialog_' + vocabularyIdentifier;
            //Check if we already have a (hidden) modal
            var vocabularyDialog = $('div#'+id);
            if(0 < vocabularyDialog.length){
                //Open the modal
                vocabularyDialog.find('input[type="hidden"][name="metadataFieldName"]').val(inputFieldName);
                vocabularyDialog.modal('show')
            }else{

                //No dialog window found, create a new one by requesting our data by json
                $.get(basicUrl + '/controlled-vocabulary-dialog',
                    {
                        vocabularyIdentifier: vocabularyIdentifier,
                        metadataFieldName: inputFieldName
                    },
                    function(resultingHtml){
                        //retrieve the dialog box


                        vocabularyDialog =  $('<div class="modal fade" id="'+id+'">' + resultingHtml + '</div>');
                        $( "body" ).append(vocabularyDialog);
                        vocabularyDialog.modal();

                        var mainDialogDivision = vocabularyDialog.find('div[id^=aspect_submission_ControlledVocabularyTransformer_div_vocabulary_dialog_]');
//                        $('body').append($(mainDialogDivision[0]));
//                        var vocabularyDialog = $('div#aspect_submission_ControlledVocabularyTransformer_div_vocabulary_dialog_' + vocabularyIdentifier);
//                        vocabularyDialog.dialog({
//                            autoOpen: true,
//                            height: 450,
//                            width: 650,
//                            modal: true,
//                            title: $Result.find('title').html()
//                        });

                        //The success function, retrieve the JSON
                        $.ajax({
                            url: vocabularyJSONUrl,
                            dataType: 'json',
                            data: {},
                            success: function(response) {
                                if(response == null){
                                    hideLoadingMsg();
                                    showErrorMsg();
                                }

                                var mainList = document.createElement('ul');
                                mainList.setAttribute('class', 'ds-simple-list vocabulary list-unstyled col-xs-12');
                                mainList.setAttribute('id', 'vocabulary-list');
                                createVocabularyNode(mainList, response, basicUrl, true);

                                //Hide the loading message !
                                hideLoadingMsg();

                                mainDialogDivision[0].appendChild(mainList);

                                //Initialize all the vocabulary box javascript actions
                                vocabularyDialog.find('span[id^="node_"]').click(function(e){
                                    e.preventDefault();
                                    e.stopPropagation();
                                    var $this = $(this);
                                    var subNodes = $('ul#' + $this.attr('id') + '_sub');
                                    if(subNodes.is(':visible')){
                                        subNodes.hide();
                                        subNodes.find('li:first-child').hide();
                                    }else{
                                        subNodes.show();
                                        subNodes.find('li:first-child').show();
                                    }
                                    //Flip the closed/open class
                                    if($this.hasClass('glyphicon-folder-open')){
                                        $this.removeClass('glyphicon-folder-open');
                                        $this.addClass('glyphicon-folder-close');
                                    }else
                                    if($this.hasClass('glyphicon-folder-close')){
                                        $this.removeClass('glyphicon-folder-close');
                                        $this.addClass('glyphicon-folder-open');
                                    }
                                });

                                //Each time we click a url ensure that our field is added in the input box !
                                $('a.vocabulary-label',vocabularyDialog).bind('click',function(e){
                                    e.preventDefault();
                                    e.stopPropagation();
                                    var $this = $(this);
                                    var inputFieldName = vocabularyDialog.find('input[type="hidden"][name="metadataFieldName"]').val();
                                    $('input[name="' + inputFieldName + '"]').val($this.attr('href'));

                                    //Close the current dialog
                                    vocabularyDialog.modal('hide');
                                    return false;
                                });

                                $('button[name="filter_button"]',vocabularyDialog).bind('click',function(){
                                    var filterValue =  $('input[name="filter"]',vocabularyDialog).val();
                                    var displayElements;
                                    if(0 < filterValue.length){
                                        //Perform the filtering
                                        //Start by hiding all the urls in our box
                                        var vocabularyList = vocabularyDialog.find('ul#vocabulary-list');
                                        vocabularyList.hide();
                                        vocabularyList.find('li').hide();
                                        var displayUrls = $('a[filter*="' + filterValue.toLowerCase() + '"]');
                                        //Retrieve all the parents of these urls & display them
                                        displayElements = displayUrls.parents('ul,li');
                                    }else{
                                        //Display them all !
                                        displayElements = vocabularyDialog.find('ul,li');
                                    }
                                    displayElements.show();
                                    //Flip class from closed to open
                                    displayElements.find('.glyphicon-folder-close').removeClass('glyphicon-folder-close').addClass('glyphicon-folder-open');
                                    //Disable normal action
                                    return false;
                                });
                            }
                        });
                    }, 'html'
                );
            }

            return false;
        });
    });

    function createVocabularyNode(list, data, basicUrl, displayed) {
        var childNodes = data.childNodes;
        var listItem = document.createElement('li');
        var vocabularyTypeClass;
        var parent = listItem;
        if(childNodes.length == 0){
            //An actual end point use the appropriate image
            vocabularyTypeClass = 'glyphicon-file';
        }else{
            if(displayed){
                vocabularyTypeClass = 'glyphicon-folder-open';
            }else{
                vocabularyTypeClass = 'glyphicon-folder-close';
            }
            parent =$( '<a href="#"></a>');
            parent.appendTo(listItem)
        }

        var vocabularyIcon =  $( '<span class="vocabulary-node-icon btn-xs glyphicon ' + vocabularyTypeClass + '"></span>');
        vocabularyIcon.attr('id', 'node_' + data.id);
        vocabularyIcon.appendTo(parent)


        var link = document.createElement('a');
        link.setAttribute('href', data.value);
        link.setAttribute('class', 'vocabulary-label');
        //Also store a to lower cased value of our label in the link, this will be used for filtering
        link.setAttribute('filter', data.value.toLowerCase());
        link.innerHTML = data.label;
        listItem.appendChild(link);

        list.appendChild(listItem);
        if(0 < childNodes.length){
            var subNodeList = document.createElement('ul');
            subNodeList.setAttribute('id', 'node_' + data.id + '_sub');
            if(!displayed){
                subNodeList.setAttribute('style', 'display: none;');
            }
            $.each(childNodes, function(key, childNode){
                createVocabularyNode(subNodeList, childNode, basicUrl, false);
            });
            list.appendChild(subNodeList);
        }
    }

    function hideLoadingMsg() {
        $('div#aspect_submission_ControlledVocabularyTransformer_item_vocabulary-loading').hide();
    }

    function showErrorMsg(){
        $('div#aspect_submission_ControlledVocabularyTransformer_item_vocabulary-error').removeClass('hidden');
    }

})($);
