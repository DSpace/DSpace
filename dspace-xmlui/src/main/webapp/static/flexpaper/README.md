## Background
Institution has a need for a document viewer with page turning capabilities (Example: display of rare books and other special collections materials).

After researching non-flash based document viewer solutions (with page turning), FlexPaper Zine seemed to offer the most attractive viewer. 

http://flexpaper.devaldi.com/flexpaper_flip_zine.jsp

This product is attractive because it provides a dynamic htm5 solution that requires no pre-compilation of assets. 
Optionally, large documents can also be pre-compiled.

http://flexpaper.devaldi.com/Convert-PDF-documents-to-HTML5.jsp

This solution requires the purchase of a license from FlexPaper. 

## Pre-requisities
* Purchase a FlexPaper Zine Commercial License and download assets http://flexpaper.devaldi.com/download/
* Copy css, js, and locale data into /dspace/modules/xmlui/src/main/webapp/static/flexpaper

## Limitations
* Large PDF's will render in the dynamic viewer, but the user will need to wait for the full PDF to download before the viewer activates.
* PDF's with inconsistent page dimensions do not render properly in the dynamic view.

## Possible Solutions to these limitations
* Precompile assets on the server or using the FlexPaper Desktop client.
* This solution contains commented-out code that looks for assets in /bookview/<item handle>/<bitstream seq>/.  If found, the static resources are presented instead of the dynamic viewer.
* A generalized pre-compile solution will require further community discussion.

## Open Questions/TODO's
* Best way to configure the Book View display to be on or off (comment out by default?)
* Neet to internationalize the language around the link (and add a header row)
* Is /static/flexpaper acceptable as a path for the solution?
* Do the comments on pre-compilation make sense?
* Should template.html be generated from an XMLUI template?
* Include sample book view icon