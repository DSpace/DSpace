function googleAnalytics(code, domain) {

  (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
  (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
  m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
  })(window,document,'script','//www.google-analytics.com/analytics.js','ga');

  ga('create', code, domain);
  ga('send', 'pageview');
}

/* <!--http://www.webappers.com/2009/11/23/fancy-javascript-popup-library-with-jquery-ui/ -->
*/
/*
 * Utility Javascript methods for Princeton University specific  Javascript
 */

function getCookie(cname) {
	var name = cname + "=";
	var ca = document.cookie.split(';');
	for (var i = 0; i < ca.length; i++) {
		var c = ca[i].trim();
		if (c.indexOf(name) == 0)
			return c.substring(name.length, c.length);
	}
	return "";
}

function setCookie(cname, cvalue, exmins, path) {
	var d = new Date();
	d.setTime(d.getTime() + (exmins * 60 * 1000));
	var expires = "expires=" + d.toGMTString();
	document.cookie = cname + "=" + cvalue + "; " + expires + "; path=" + path;
}

function getParam(name) {
   var params = {};
   var parts = (window.location.search || '').split(/[&?]/);
   for (var i = 0; i < parts.length; ++i) {
      var eq = parts[i].indexOf('=');
      if (eq < 0) continue;
      params[parts[i].substring(0, eq)]   = parts[i].substring(eq+1);
   }
   return params[name];
}

// ----------------------------------------------------------------------------
// build custom page 'in front' of bitstream
// ----------------------------------------------------------------------------
function gotoUrl(cookie, timeout, url) {
    setCookie(cookie, "yes", timeout, "/");
    window.location = url;
}

function agree_to_view_in_bitstream_page(url, cookie, timeout, agreementText)
{
    var cookieValue = getCookie(cookie);
    if (cookieValue === "yes") {
        return true;
    }

    var win=window.open(location.href + "?bitstream=" + url, '_blank');
    win.focus();
}

function must_agree_to_view_in_bitstream_pages(cookie, timeout, agreementText)
{
     var hdlcookie =  getParam('bitstream');
     var url = getCookie(hdlcookie); 
     if (url == undefined || url === "") {
        var links = document.links;
        if (links) 
        {
           for (var i = 0; i < links.length; ++i)
           {
             if (links[i].href.indexOf("/bitstream") != -1) {
               var parts = links[i].href.split('/') 
               var hdl  = parts[parts.length -3]; 
               var bitid = parts[parts.length -2]; 
               hdlcookie  =  hdl + "/" + bitid; 
               setCookie(hdlcookie, links[i].href, timeout, "/"); 
               links[i].href =  window.location + "?bitstream=" +  hdlcookie; 
             }
           }
         }
     } else {
       var cookieValue = getCookie(cookie);
       if (cookieValue === "yes") {
           window.location = url; 
           return;
       }

       // build agreemnt table
       // with OK button that if clicked triggers restoreBitstreamLinks_table function
       var agreement = new Element('tr').update('<td> ' + agreementText + '</td>');

       var fctCall = 'gotoUrl("' + cookie + '",' + timeout + ',"' + url + '")'; 
       var button = new Element('form').update('<input type="button" Value="I Agree" onClick=' + fctCall + " />"); 

       agreeTable = "<table width='80%' class='pageContent' align='center'><tbody>" +
                    "<tr><td>" + agreementText + "</td></tr>" +
                    "<tr><td>" + button.innerHTML + "</td></tr>" +
                    "</tbody></table>";

        $$('.centralPane')[0].innerHTML = agreeTable;
     }
};

// ----------------------------------------------------------------------------
// if (cookie is not set or is not yes)
//      hide bitstream links
//      show agreement instead
// restore when cookie has been set to yes
// ----------------------------------------------------------------------------
// ugly global for this page
var theCookie;
var theTimeout;

function restoreBitstreamLinks_table()
{
     setCookie(theCookie, "yes", theTimeout, "/");
     $('bitstreamLinkTable').show(); 
     $$('.agreementTable')[0].remove();
}


function must_agree_to_view_bitstreams_modify_table(cookie, timeout, agreementText)
{
     if (getCookie(cookie) === "yes" )  {
         return ;
     }
     theCookie = cookie;
     theTimeout = timeout;

     // build agreemnt table  
     // with OK button that if clicked triggers restoreBitstreamLinks_table function
     var agreement = new Element('tr').update('<td> ' + agreementText + '</td>');
     var button = new Element('form').update('<input type="button" Value="I Agree" onClick="restoreBitstreamLinks_table()" />' );
     agreeTable = "<table class='agreementTable miscTable' align='center'><tbody>" + 
                    "<tr><td>" + agreementText + "</td></tr>" + 
                    "<tr><td>" + button.innerHTML + "</td></tr>" + 
                    "</tbody></table>"; 
     
     // insert before bistream link table 
     $('bitstreamLinkTable').insert({ before : agreeTable }); 
     // hide bitstream links
     $('bitstreamLinkTable').hide(); 
}


// ----------------------------------------------------------------------------
// popup when clicking a bitstream link
// ----------------------------------------------------------------------------
function agree_to_view_popup(cookie, timeout, agreementText) {
    var cookieValue = getCookie(cookie);
    if (cookieValue === "yes") {
        return true;
    }

    var r = confirm("License Agreement: \n" + agreementText);
    if (r == true) {
        agree  = "yes";
    } else {
        agree  = "no";
    }
    setCookie(cookie, agree, timeout, "/");
    return  (agree === "yes");
}

function must_agree_to_view_bitstreams_popup(cookie, timeout, agreementText)
{
     var links = document.links;
     if (links)
     {
        for (var i = 0; i < links.length; ++i)
        {
          if (links[i].href.indexOf("/bitstream") != -1) {
            links[i].onclick = function () { return agree_to_view_popup(cookie,  timeout, agreementText); };
          }
        }
      }
};

