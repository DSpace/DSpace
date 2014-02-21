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
     $('agreement').hide(); 
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
     agreeTable = "<table class='agreement miscTable' align='center'><tbody>" + 
                    "<tr><td>" + agreementText + "</td></tr>" + 
                    "<tr><td>" + button.innerHTML + "</td></tr>" + 
                    "</tbody></table>"; 
     
     // insert before bistream link table 
     $('bitstreamLinkTable').insert({ before : agreeTable }); 
     // hide bitstream links
     $('bitstreamLinkTable').hide(); 


}

function must_agree_to_view_bitstreams_modify_table_v0(cookie, timeout, agreementText)
{
     if (getCookie(cookie) === "yes" )  {
         return ;
     }
     theCookie = cookie;
     theTimeout = timeout;

     // hide bitstream links
     // insert agreeement  --> with OK button that if clicked triggers restoreBitstreamLinks_table function
     //
     var tableBody = $('bitstreamLinkTable').childElements()[0];
     // hide al existing table rows
     //tableBody.childElements().each(Element.hide);
     $$('.bitstreamLink').each(Element.hide);

     var agreement = new Element('tr').addClassName('agreement').update('<td> ' + agreementText + '</td>');
     var button = new Element('form').update('<input type="button" Value="I Agree" onClick="restoreBitstreamLinks_table()" />' );
     button = new Element('td').update(button);
     button = new Element('tr').update(button);
     button.addClassName('agreement');
     tableBody.insert( {
          top:  agreement
     }).insert(button);;
}






// old versions --> unused
function v0agree_to_view(cookie, bitstreamUrl, agreementText) {
	var cookieValue = getCookie(cookie);
	if (cookieValue === "yes") {
		window.location.assign(bitstreamUrl);
	} else {
		var r = confirm("License Agreement: \n" + agreementText);
		if (r == true) {
			agree  = "yes";
		} else {
			agree  = "no";
		}
		setCookie(cookie, agree, 1, "/");
		if (agree === "yes") {
			window.location.assign(bitstreamUrl);  // do the download
		}
	}
	return false;
}


function vxagree_to_view(cookie, bitstreamUrl, agreementText) {
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
    setCookie(cookie, agree, 1, "/");
    return  (agree === "yes");
}
