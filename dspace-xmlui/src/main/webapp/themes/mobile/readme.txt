    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

Mobile theme for DSpace 1.6
Created by Elias Tzoc <tzoce@miamioh.edu> and
           James Russell <james@ohiolink.edu>
           September 14, 2012

Mobile theme 1.1
Updated by Elias Tzoc <tzoce@miamioh.edu>
           September 30, 2013
Fixes: switch to non-mobile pages; CSS and reloading problems; and mobile-optimized page for advanced search 

===================================================================================
The mobile theme file structure
 
+-- mobile
|   +-- lib
|   |   +-- cookies.js
|   |   +-- detectmobile.js
|   |   +-- images
|   |   |   +-- ajax-loader.gif
|   |   |   +-- default-thumbnail.png
|   |   |   +-- icons-18-black.png
|   |   |   +-- icons-18-white.png
|   |   |   +-- icons-36-black.png
|   |   |   +-- icons-36-white.png
|   |   +-- m-tweaks.css
|   |   +-- sc-mobile.css
|   |   +-- sc-mobile.min.css
|   |   +-- mobile.xsl
|   |   +-- sitemap.xmap
|   |   +-- themes.xmap
|   +-- readme.txt


Installation:

0.  Get a new domain name that is an alias of the existing domain name
    for your DSpace installation.
    e.g. if your current domain is yoursite.edu your new domain name
    might be mobile.yoursite.edu
    These instructions assume that the new domain name starts with 'mobile.'
    If it is something else, you will need to make a change in Step 5.

1.  Copy the mobile theme folder into your XMLUI theme folder
    e.g. ../dspace/webapps/xmlui/themes/

2.  Add a call for the detectmobile.js and cookies.js file in the header 
    of your current main theme.xsl file.
    It should look like:
    <script type="text/javascript" src="/themes/mobile/lib/cookies.js">&#160;</script>
    <script type="text/javascript" src="/themes/mobile/lib/detectmobile.js">&#160;</script>
    * In this file, we also add a "View mobile site" link in the footer
    section, which allows users to view the full site on their
    mobile devices.  The cookies.js file saves this preference 
    but it's erased when the session is closed.
    If you want such a link, the code for this should look like the following 
    (with the appropriate URL for your mobile site):
    <a href="#" onclick="eraseCookie('viewfull');window.location='http://mobile.yoursite.edu';">
    View mobile site</a>

3.  Open the detectmobile.js file and enter your new mobile domain
    at the end of the function call e.g. mobile.yoursite.edu
    * if you choose a different domain name or theme name other than
    "mobile" make sure to update the settings in the sitemap.xmap

4.  In mobile.xsl, find the link "View full website" and replace the
    references to yoursite.edu with the domain name for your main site.
    * lines 255-257

5.  Replace or edit the themes.xmap file located in your default theme
    folder e.g. ../dspace/webapps/xmlui/themes/
    * The code for setting up the properties for the domain is in
    lines 32-37.  This will need to be changed if the domain name
    for your mobile site starts with something other than 'mobile.'

6.  Restart tomcat and now you should be able to see the mobile theme
    in action; to change the look-and-feel, you can go to 
    http://jquerymobile.com/themeroller/ and either create your own
    files or import/upgrade the uncompressed sc-mobile.css file.

NOTE: Pages with more complex structure such as "Advanced Search" were
      excluded in this first mobile theme.

