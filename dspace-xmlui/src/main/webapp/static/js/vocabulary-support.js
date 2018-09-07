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

            //Check if we already have a (hidden) dialog
            var vocabularyDialog = $('div#aspect_submission_ControlledVocabularyTransformer_div_vocabulary_dialog_' + vocabularyIdentifier);
            if(0 < vocabularyDialog.length){
                //Open the dialog
                vocabularyDialog.find('input[type="hidden"][name="metadataFieldName"]').val(inputFieldName);
                vocabularyDialog.dialog( 'open' );
            }else{
                //No dialog window found, create a new one by requesting our data by json
                $.get(basicUrl + '/controlled-vocabulary-dialog',
                    {
                        vocabularyIdentifier: vocabularyIdentifier,
                        metadataFieldName: inputFieldName
                    },
                    function(resultingHtml){
                        //retrieve the dialog box
                        var $Result = $('<div></div>').html(resultingHtml);
                        var mainDialogDivision = $Result.find('div[id^=aspect_submission_ControlledVocabularyTransformer_div_vocabulary_dialog_]');
                        $('body').append($(mainDialogDivision[0]));
                        var vocabularyDialog = $('div#aspect_submission_ControlledVocabularyTransformer_div_vocabulary_dialog_' + vocabularyIdentifier);
                        vocabularyDialog.dialog({
                            autoOpen: true,
                            height: 450,
                            width: 650,
                            modal: true,
                            title: $Result.find('title').html()
                        });

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
                                mainList.setAttribute('class', 'ds-simple-list vocabulary');
                                mainList.setAttribute('id', 'vocabulary-list');
                                createVocabularyNode(mainList, response, basicUrl, true);

                                //Hide the loading message !
                                hideLoadingMsg();

                                mainDialogDivision[0].appendChild(mainList);

                                //Initialize all the vocabulary box javascript actions
                                vocabularyDialog.find('div[id^="node_"]').click(function(){
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
                                    if($this.hasClass('vocabulary-open')){
                                        $this.removeClass('vocabulary-open');
                                        $this.addClass('vocabulary-closed');
                                    }else
                                    if($this.hasClass('vocabulary-closed')){
                                        $this.removeClass('vocabulary-closed');
                                        $this.addClass('vocabulary-open');
                                    }
                                });

                                //Each time we click a url ensure that our field is added in the input box !
                                vocabularyDialog.find('a').click(function(){
                                    var $this = $(this);
                                    var inputFieldName = vocabularyDialog.find('input[type="hidden"][name="metadataFieldName"]').val();
                                    $('input[name="' + inputFieldName + '"]').val($this.attr('href'));

                                    //Close the current dialog
                                    vocabularyDialog.dialog("close");
                                    return false;
                                });

                                vocabularyDialog.find('input[name="filter_button"]').click(function(){
                                    var $this = $(this);
                                    var filterValue = $this.parent().find('input[name="filter"]').val();
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
                                    displayElements.find('div.vocabulary-closed').removeClass('vocabulary-closed').addClass('vocabulary-open');
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
        if(childNodes.length == 0){
            //An actual end point use the appropriate image
            vocabularyTypeClass = 'vocabulary-doc';
        }else{
            if(displayed){
                vocabularyTypeClass = 'vocabulary-open';
            }else{
                vocabularyTypeClass = 'vocabulary-closed';
            }
        }

        var vocabularyDivision = document.createElement('div');
        vocabularyDivision.setAttribute('id', 'node_' + data.id);
        vocabularyDivision.setAttribute('class', 'vocabulary-node-icon '  + vocabularyTypeClass);
        listItem.appendChild(vocabularyDivision);
        var link = document.createElement('a');
        link.setAttribute('href', data.value);
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
        $('li#aspect_submission_ControlledVocabularyTransformer_item_vocabulary-loading').hide();
    }

    function showErrorMsg(){
        $('li#aspect_submission_ControlledVocabularyTransformer_item_vocabulary-error').removeClass('hidden');
    }

})($);
