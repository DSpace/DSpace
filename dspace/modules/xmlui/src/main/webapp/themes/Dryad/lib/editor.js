//Clear default text of emty text areas on focus
function tFocus(element) {
    if (element.value == ' ') {
        element.value = '';
    }
}

//Clear default text of emty text areas on submit and handle ID searches
function tSubmit(form) {
    var defaultedElements = document.getElementsByTagName("textarea");
    for (var i = 0; i != defaultedElements.length; i++) {
        if (defaultedElements[i].value == ' ') {
            defaultedElements[i].value = '';
        }
    }
    var queryTexts = document.getElementsByName("query");
    for (var i = 0; i != queryTexts.length; i++) {
        var value = queryTexts[i].value;
        if (value.indexOf(' ') == - 1) {
            if (value.indexOf("doi:") == 0 || value.indexOf("http:") == 0) {
                queryTexts[i].value = '"' + value + '"';
            }
            
            if (value.indexOf("hdl:") == 0) {
                queryTexts[i].value = value.substring(4, value.length);
            }
        }
    }
}
//Disable pressing 'enter' key to submit a form (otherwise pressing 'enter' causes a submission to start over)
function disableEnterKey(e) {
    var key;
    
    if (window.event)
    key = window.event.keyCode;
    //Internet Explorer else
    key = e.which;
    //Firefox and Netscape
    
    if (key == 13) //if "Enter" pressed, then disable!
    return false; else
    return true;
}