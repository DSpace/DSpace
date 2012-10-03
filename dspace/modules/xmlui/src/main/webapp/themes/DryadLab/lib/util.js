

function openCenterPopup(pageURL, title, w, h) {
    var left = (screen.width/2)-(w/2);
    var top = (screen.height/2)-(h/2);
    window.open (pageURL, title, 'width='+w+', height='+h+', top='+top+', left='+left);
    return false;
}
