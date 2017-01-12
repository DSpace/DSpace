/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
$(document).ready(function() {
    var taskPoolCheckBoxes= $('input[name="workflowandstepID"]');
    var workflowIDCheckboxes = $('input[name="workflowID"]');
    var workspaceIDCheckBoxes = $('input[name="workspaceID"]');

    enableMultiSelect(taskPoolCheckBoxes);
    enableMultiSelect(workflowIDCheckboxes);
    enableMultiSelect(workspaceIDCheckBoxes);

    function enableMultiSelect(chkboxes) {
        var lastChecked = null;
        chkboxes.click(function (e) {
            if (!lastChecked) {
                lastChecked = this;
                return;
            }
            if (e.shiftKey) {
                var start = chkboxes.index(this);
                var end = chkboxes.index(lastChecked);
                chkboxes.slice(Math.min(start, end), Math.max(start, end) + 1).prop('checked', lastChecked.checked);
            }
            lastChecked = this;
        });
    }

});
