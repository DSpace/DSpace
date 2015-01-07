<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xhtml="http://www.w3.org/1999/xhtml"
    xmlns:ddw="http://datadryad.org/api/v1/widgets/display"
    xmlns:csv="http://apache.org/cocoon/csv/1.0"
    xmlns:math="http://exslt.org/math"
    exclude-result-prefixes="xhtml ddw math csv"
    version="1.0">
    
    
    <!-- NOTE: experimental and incomplete -->
    <xsl:template name="application-pdf">
        <html>
            <head>
                <script src="http://mozilla.github.io/pdf.js/build/pdf.js"></script>
                <script><![CDATA[
'use strict';]]>
var url = '<xsl:value-of select="$bitstream"/>';<![CDATA[
PDFJS.getDocument(url).then(function(pdf) {
  pdf.getPage(1).then(function(page) {
    var scale = 1.5;
    var viewport = page.getViewport(scale);
    var canvas = document.getElementById('PDFcanvas');
    var context = canvas.getContext('2d');
    canvas.height = viewport.height;
    canvas.width = viewport.width;
    var renderContext = {
      canvasContext: context,
      viewport: viewport
    };
    page.render(renderContext);
  });
});
]]>
                </script>
            </head>
            <body>
                <canvas id="PDFcanvas" style="border:0; width: 100%"></canvas>
            </body>
        </html>        
    </xsl:template>
    
</xsl:stylesheet>