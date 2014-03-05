function googleAnalytics(code, domain) {

  (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
  (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
  m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
  })(window,document,'script','//www.google-analytics.com/analytics.js','ga');

  ga('create', code, domain);
  ga('send', 'pageview');
}


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

function agree_to_view(cookie, bitstreamUrl, agreementText) {
	var cookieValue = getCookie(cookie);
	bitstreamUrl = this.href;
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
}

function must_agree_to_view_bitstreams(cookie, agreementText)
{
     var links = document.links;
     if (links)
     {
        for (var i = 0; i < links.length; ++i)
        {
          if (links[i].href.indexOf("/bitstream") != -1) {
            links[i].onclick = agree_to_view(cookie, agreementText);
          }
        }
      }
};
