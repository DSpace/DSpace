/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
(function ($) {
    $(document).ready(function() {
        var bitstreamTable = $("table#bitstream-edit-form-table");

        var upArrows = bitstreamTable.find('input[name^="submit_order_"][name$="_up"]');
        var downArrows = bitstreamTable.find('input[name^="submit_order_"][name$="_down"]');


        upArrows.click(function(event) {
            moveRow($(this), true);
            return false;
        });

        downArrows.click(function(event){
            moveRow($(this), false);
            return false;
        });

        //Javascript successfully started, show our submit reorder button
        $('input[name="submit_update_order"]').css('visibility', 'visible').show();
    });

    function moveRow(element, moveUp){
        var row = $(element).parents("tr:first");

        //Save the row that also moves from position
        var altRow;
        if(moveUp){
            altRow = row.prev();
            row.insertBefore(altRow);
        }else{
            altRow = row.next();
            row.insertAfter(altRow);
        }

        renderRowMovement(row, moveUp);
        //The other row makes an inverse movement
        renderRowMovement(altRow, !moveUp);
    }

    function renderRowMovement(row, moveUp) {
        //Flip the class from even to odd or the other way around
        if(row.find('td').hasClass('evenRowEvenCol')){
            var cells = row.find('td[class="evenRowEvenCol"]');
            cells. removeClass('evenRowEvenCol');
            cells.addClass('oddRowEvenCol');
            cells = row.find('td[class="evenRowOddCol"]');
            cells. removeClass('evenRowOddCol');
            cells.addClass('oddRowOddCol');
        }else{
            var cells = row.find('td[class="oddRowEvenCol"]');
            cells. removeClass('oddRowEvenCol');
            cells.addClass('evenRowEvenCol');
            cells = row.find('td[class="oddRowOddCol"]');
            cells. removeClass('oddRowOddCol');
            cells.addClass('evenRowOddCol');
        }

        var orderElement = row.find('input[name^="order_"]');
        //Retrieve  the order & calculate the new order depending on we move up or down
        var newOrder = parseInt(orderElement.val());
        if (moveUp) {
            newOrder--;
        } else {
            newOrder++;
        }
        orderElement.val(newOrder);

        var upArrow = row.find('input[name$^="submit_order_"][name$="_up"]');
        var downArrow = row.find('input[name$^="submit_order_"][name$="_down"]');

        //Check if we are the first row, if so hide the up arrow
        if(!isOrginalBundleRow(row.prev())){
            upArrow.attr('disabled', 'disabled');
        }else{
            upArrow.removeAttr('disabled');
        }

        //Check if we are the last row, if so hide the down arrow
        if(!isOrginalBundleRow(row.next())){
            downArrow.attr('disabled', 'disabled');
        }else{
            downArrow.removeAttr('disabled');
        }

        //Set the displayed new order
        var displayedOrder = row.find('span[id^="aspect.administrative.item.EditItemBitstreamsForm.field.order_"][id$="_new"]');
        displayedOrder.text(newOrder);
    }

    /**
     * Checks if the given row is a bundle header
     * @param row the row to be checked
     */
    function isOrginalBundleRow(row){
        // Checks if the identifier starts with an original bundle identifier
        return row.attr("id").indexOf("row_ORIGINAL_") == 0;
    }



})(jQuery);
