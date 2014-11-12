/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
 ## Background
Institution has a need for a document viewer with page turning capabilities (Example: display of rare books and other special collections materials).

After researching non-flash based document viewer solutions (with page turning), FlexPaper Zine seemed to offer the most attractive viewer. 

http://flexpaper.devaldi.com/flexpaper_flip_zine.jsp

This product is attractive because it provides a dynamic htm5 solution that requires no pre-compilation of assets. 
Optionally, large documents can also be pre-compiled.

http://flexpaper.devaldi.com/Convert-PDF-documents-to-HTML5.jsp

This solution requires the purchase of a license from FlexPaper. 

## Sample Screen Shots 
_Replace with live viewer links once solution is live_
https://github.com/Georgetown-University-Libraries/Georgetown-University-Libraries-Code/releases/tag/v1.0.8

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

## Notes about pre-compilation (for eventual filter-media/curation task)
* Create bookview folder
* cd to bookview folder
* symlink pdf file as link.pdf

### Sample Pre-compile Script    
    echo pdftk link.pdf burst output link_%02d.pdf compress
    pdftk link.pdf burst output link_%02d.pdf compress
    if [ $? -ne 0 ];then
        echo pdftk: cannot split pdf
        exit
    fi
         
    echo pdf2json link.pdf -enc UTF-8 -compress -split 10 link.pdf_%.js
    pdf2json link.pdf -enc UTF-8 -compress -split 10 link.pdf_%.js
    if [ $? -ne 0 ];then
       echo pdf2json: cannot index pdf for searching
       exit
    fi
     
    echo convert -thumbnail 184x283 link.pdf link-%d.jpg
    convert -thumbnail 184x283 link.pdf link-%d.jpg
    if [ $? -ne 0 ];then
       echo convert: cannot convert pdf to thumbnails
       exit
    fi
    
    sed -e "s|HANDLESEQ|${HANDLESEQ}|g" $$PATH$$/precompile.template > index.html
    if [ $? -ne 0 ];then
        echo convert: cannot create index.html file
       exit
    fi
    
    unlink link.pdf
    if [ $? -ne 0 ];then
        echo convert: cannot unlink pdf file
        exit
    fi
    
## Code to invoke pre-compiled html5 viewer
    $('#documentViewer').FlexPaperViewer(
        { config : {
            JSONFile : '/bookview/HANDLESEQ/link.pdf_{page}.js',
            PDFFile : '/bookview/HANDLESEQ/link_[*,2].pdf',
            ThumbIMGFiles : '/bookview/HANDLESEQ/link-{page}.jpg',
            RenderingOrder : 'html5',
            key : '$$YOUR_KEY$$',
        }}
    );
