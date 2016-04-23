/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
(function($) {

    $(document).ready(function() {
        initAddButtons();
        initRemoveButtons();
        initFileButtons();
        //enable the validation


        $('#aspect_authorprofile_administrative_EditAuthorProfileForm_div_author-profile-form').validate();
        $.validator.addMethod("regex",regexChck,provideMessage);

    });

    function provideMessage(e,k){
        return $(k).siblings('.invalid').text();
    }

    function regexChck(value,element){

        var exp;
        if($(element).parent().hasClass('ds-composite-component')){

            exp=$($(element).parent().parent().find('.ds-composite-component >  input[name*=regex]')[0]).attr('value');
        } else {
            exp=$($(element).siblings('.ds-hidden-field')[0]).attr('value');
        }

        var patt=new RegExp(exp);
        return patt.test(value);
    }

    function initAddButtons() {
        $('input.author-profile-add').click(function(e){
            e.preventDefault();
            var $this = $(this);
            var parentList = $this.parents('li:first');
            var lastInput = parentList.find('input.actual:last');
            var lastIdentifier = lastInput.attr('id');
            var lastDivision = lastInput.parents('div:first');

            var oldIndex = parseInt(lastIdentifier.substr(lastIdentifier.lastIndexOf('_')+1, lastIdentifier.length));
            //Add one to the index !
            var newIndex = oldIndex + 1;

            var newDivision = lastDivision.clone(true);
            newDivision.find('input').each(function()
            {
                var $this = $(this);
                //Update the index of the name (if present)
                $this.attr('name', $this.attr('name').replace('_' + oldIndex, '_' + newIndex));
                $this.attr('id', $this.attr('id').replace('_' + oldIndex, '_' + newIndex));

            });
                    //Clear the values
            newDivision.find('input[type=text]').val('');

            parentList.append(newDivision);
        });
    }

    function initRemoveButtons() {
        $('input.author-profile-remove').click(function(e){
            e.preventDefault();
            var $this = $(this);
            //Check if we are the last of the rows !
            var divToRemove = $this.parents('div:first');
            if(divToRemove.parent().find('div.ds-form-content').length == 1)
            {
                //Only one left, clear the values
                divToRemove.find('input[type="text"]').val('');
            }else{
                divToRemove.remove();
            }

        });
    }

    function initFileButtons(){
        var alterButtons = $('input[name^="submit_alter_file_"]');
        alterButtons.click(function(){
            var $this = $(this);
            showUploadFileBox($this.attr('id').substr($this.attr('id').lastIndexOf('_')+1, $this.attr('id').length));

            return false;
        });
        var removeButtons = $('input[name^="submit_remove_file_"]');
        removeButtons.click(function(){
            var $this = $(this);
            var fileId = $this.attr('id').substr($this.attr('id').lastIndexOf('_')+1, $this.attr('id').length);
            $('li#aspect_authorprofile_administrative_EditAuthorProfileForm_item_' + fileId + '-figure').find('img').hide();
            $('input[name^="submit_alter_file_' + fileId + '"]').hide();
            $this.hide();
            showUploadFileBox(fileId);
            //Set a hidden input to remove the current file
            $('input[type="hidden"][name="remove_file_' + fileId + '"]').val('true');

            return false;
        });
    }

    function showUploadFileBox(name)
    {
        $('input[name="' + name + '"][type="file"]').css('visibility', 'visible').show();
    }

})(jQuery);
