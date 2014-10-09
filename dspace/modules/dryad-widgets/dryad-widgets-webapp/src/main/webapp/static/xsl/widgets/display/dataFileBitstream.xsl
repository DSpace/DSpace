<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xhtml="http://www.w3.org/1999/xhtml"
    xmlns:ddw="http://datadryad.org/api/v1/widgets/display"
    xmlns:csv="http://apache.org/cocoon/csv/1.0"
    xmlns:math="http://exslt.org/math"
    exclude-result-prefixes="xhtml ddw math csv"
    version="1.0">
    
    <!-- 
        TODO: split out per-mime-type templates into xsl:include's
    -->
    
    <xsl:output method="html" indent="yes"/>
    <xsl:preserve-space elements="script"/>
    
    <xsl:variable name="mime-type" select="/xhtml:xhtml/xhtml:head/xhtml:meta[@property='dc.format']"/>
    <xsl:variable name="extent"    select="/xhtml:xhtml/xhtml:head/xhtml:meta[@property='dc.extent']"/>
    <xsl:variable name="source"    select="/xhtml:xhtml/xhtml:head/xhtml:meta[@property='dc.source']"/>
    
    <xsl:param name="doi"/>
    <xsl:param name="object-url"/> <!-- DataOne-MN url for bitstream -->
    
    <!-- 
        Lookup table for template to call for handling input based on mime-type
    -->
    <ddw:templates>
        <ddw:template mime-type="text/plain"          >text-plain</ddw:template>
        <ddw:template mime-type="text/csv"            >text-csv</ddw:template>
        <ddw:template mime-type="application/pdf"     >application-pdf</ddw:template>
        <ddw:template mime-type="application/x-python">code</ddw:template>
        <ddw:template mime-type="image/png"           >image-native</ddw:template>
        <ddw:template mime-type="image/jpeg"          >image-native</ddw:template>
    </ddw:templates>
    
    <xsl:template match="/">
        <xsl:choose>
            <xsl:when test="not(document('')/xsl:stylesheet/ddw:templates/ddw:template[@mime-type=$mime-type])">
                <xsl:call-template name="unsupported"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:variable name="template-name" select="document('')/xsl:stylesheet/ddw:templates/ddw:template[@mime-type=$mime-type]"/>
                <xsl:choose>
                    <xsl:when test="$template-name = 'text-plain'"      ><xsl:call-template name="text-plain"        /></xsl:when>
                    <xsl:when test="$template-name = 'text-csv'"        ><xsl:call-template name="text-csv"          /></xsl:when>
                    <xsl:when test="$template-name = 'application-pdf'" ><xsl:call-template name="application-pdf"   /></xsl:when>
                    <xsl:when test="$template-name = 'code'"            ><xsl:call-template name="code"              /></xsl:when>
                    <xsl:when test="$template-name = 'image-native'"    ><xsl:call-template name="image-native"      /></xsl:when>
                    <xsl:otherwise>
                        <xsl:message terminate="yes">Misconfigured lookup or missing named template.</xsl:message>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- suppress this element from result document -->
    <xsl:template match="xhtml:head"/>
    
    <!-- -->
    <xsl:template name="unsupported">
        <html>
            <head></head>
            <body>
                <div>This content type '<xsl:value-of select="$mime-type"/>' is unsupported. 
                    Please visit the <a href="http://dx.doi.org/{$doi}">Dryad site</a>
                    to view the data or download the data from this widget.
                </div>
            </body>
        </html>
    </xsl:template>
    
    <xsl:template name="text-plain">
        <html>
            <head></head>
            <body>
<pre>
<xsl:value-of select="/xhtml:xhtml/xhtml:body"/>
</pre>
            </body>
        </html>
    </xsl:template>
    
    <xsl:template name="application-pdf">
        <html>
            <head>
                <script src="http://mozilla.github.io/pdf.js/build/pdf.js"></script>
                <script><![CDATA[
'use strict';]]>
var url = '<xsl:value-of select="$object-url"/>';<![CDATA[
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
    
    <xsl:template name="code">
        <html>
            <head>
                <link rel="stylesheet" href="http://cdnjs.cloudflare.com/ajax/libs/highlight.js/8.2/styles/default.min.css"></link>
                <script src="http://cdnjs.cloudflare.com/ajax/libs/highlight.js/8.2/highlight.min.js"></script>
                <script>hljs.initHighlightingOnLoad();</script>
                <!-- https://highlightjs.org/usage/ -->
            </head>
            <body>
<pre>
<code>
<xsl:value-of select="/xhtml:xhtml/xhtml:body"/>
</code>
</pre>
            </body>
        </html>
    </xsl:template>
    
    <xsl:template name="text-csv">
        <html>
            <head>
                <title>Dryad Data</title>
                <script type="text/javascript" src="http://code.jquery.com/jquery-1.11.1.min.js"></script>
                <script type="text/javascript" src="http://cdn.datatables.net/1.10.2/js/jquery.dataTables.min.js"></script>
                <style type="text/css">
                    .sorting, .sorting_asc, .sorting_desc {
                        background : none !important;
                </style>
                <link type="text/css" rel="stylesheet" href="http://cdn.datatables.net/1.10.2/css/jquery.dataTables.css"></link>
            </head>
            <body>
                <xsl:variable name="colmax" select="math:max(//csv:record/csv:field/@number)"/>
                <!-- <table id="example" class="compact hover" cellspacing="0" width="100%"></table> -->
                <table id="table" class="display" cellspacing="0" width="100%">
                    <!-- 
                        TODO: determine whether table heads can be inferred, either
                        here or in the Text/CSV converter. 
                    -->
                    <!--
                    <thead>
                        <tr>
                            <xsl:call-template name="csv-col2table">
                                <xsl:with-param name="row" select="//csv:record[@number='1']"/>
                                <xsl:with-param name="colmax" select="$colmax"/>
                                <xsl:with-param name="colcurrent" select="1"/>
                                <xsl:with-param name="elt" select="'th'"/>
                            </xsl:call-template>
                        </tr>
                    </thead>
                    <tfoot>
                        <tr>
                            <xsl:call-template name="csv-col2table">
                                <xsl:with-param name="row" select="//csv:record[@number='1']"/>
                                <xsl:with-param name="colmax" select="$colmax"/>
                                <xsl:with-param name="colcurrent" select="1"/>
                                <xsl:with-param name="elt" select="'th'"/>
                            </xsl:call-template>
                        </tr>
                    </tfoot>
                    -->
                    <tbody>
                        <xsl:call-template name="csv-row2table">
                            <!--<xsl:with-param name="row" select="//csv:record[@number='2']"/>-->
                            <xsl:with-param name="row" select="//csv:record[@number='1']"/>
                            <xsl:with-param name="colmax" select="$colmax"/>
                        </xsl:call-template>
                    </tbody>
                </table>
                <script type="text/javascript">
<![CDATA[
$(document).ready(function() {
    $('#table').dataTable({
        "sDom"    : '<"top">t<"bottom"lp><"clear">' // orig: "sDom": '<"top"i>rt<"bottom"flp><"clear">'
    });
});
]]>
                </script>
            </body>
        </html>
    </xsl:template>
   
    <!-- Pass through DataOne-MN bitstream url for
         PNG and JPEG images.
         TODO: handle scaling, positioning better
    --> 
    <xsl:template name="image-native">
        <html>
            <head>
                <style>
                .container {
                    margin: 10px;
                    max-width: 100%;
                    max-height: 100%;
                    text-align: center;
                }
                .fit {
                     max-height:100%;
                     max-width:100%;
                     vertical-aign:middle;
                }
                </style>
            </head>
            <body>
                <div class="container">
                    <img class="fit" src="{$source}" alt="Image for {$doi}"></img>
                </div>
            </body>
        </html>
    </xsl:template>
    
    <!-- 
        Note: the CSV generator org.apache.cocoon.generation.CSVGenerator
        does not output empty fields, e.g.:
            <csv:record number="3">
                <csv:field number="4">Bombina</csv:field>
                <csv:field number="5">orientalis</csv:field>
                <csv:field number="9">gonochorous</csv:field>
            </csv:record>
        so we have to output each cell by index using the field's @number, 
        rather than with a template for csv:field. 
    -->
    <xsl:template name="csv-row2table">
        <xsl:param name="colmax"/>
        <xsl:param name="row"/>
        <tr>
            <xsl:call-template name="csv-col2table">
                <xsl:with-param name="row" select="$row"/>
                <xsl:with-param name="colcurrent" select="1"/>
                <xsl:with-param name="colmax" select="$colmax"/>
                <xsl:with-param name="elt" select="'td'"/>
            </xsl:call-template>
        </tr>
        <xsl:if test="$row/following-sibling::csv:record">
            <xsl:call-template name="csv-row2table">
                <xsl:with-param name="row" select="$row/following-sibling::csv:record"/>
                <xsl:with-param name="colmax" select="$colmax"/>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>    
    
    <xsl:template name="csv-col2table">
        <xsl:param name="row"/>
        <xsl:param name="colcurrent"/>
        <xsl:param name="colmax"/>
        <xsl:param name="elt"/>
        <xsl:element name="{$elt}">
            <xsl:value-of select="string($row/csv:field[@number=$colcurrent])"/>
        </xsl:element>
        <xsl:if test="$colcurrent &lt; $colmax">
            <xsl:call-template name="csv-col2table">
                <xsl:with-param name="row" select="$row"/>
                <xsl:with-param name="colcurrent" select="$colcurrent + 1"/>
                <xsl:with-param name="colmax" select="$colmax"/>
                <xsl:with-param name="elt" select="$elt"/>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>
        
    <!--
JS-data for table has better performance than <table>, but it is
not working at the moment for unknown reasons. 
See example at https://datatables.net/examples/data_sources/js_array.html
$('#table').dataTable({
      "data"    : data
    , "columns" : columns
    , "sDom"    : '<"top">t<"bottom"lp><"clear">' // orig: "sDom": '<"top"i>rt<"bottom"flp><"clear">'
});
var columns = [ 
[<xsl:call-template name="csv-col2json">
    <xsl:with-param name="row" select="//csv:record[@number='1']"/>
    <xsl:with-param name="colmax" select="$colmax"/>
    <xsl:with-param name="colcurrent" select="1"/>
</xsl:call-template>].map( function(str) { return { "title" : str } })
];
var data = [
<xsl:call-template name="csv-row2json">
    <xsl:with-param name="row" select="//csv:record[@number='2']"/>
    <xsl:with-param name="colmax" select="$colmax"/>
</xsl:call-template>
];
    <xsl:template name="csv-row2json">
        <xsl:param name="colmax"/>
        <xsl:param name="row"/>
        <xsl:text>[</xsl:text>
        <xsl:call-template name="csv-col2json">
            <xsl:with-param name="row" select="$row"/>
            <xsl:with-param name="colcurrent" select="1"/>
            <xsl:with-param name="colmax" select="$colmax"/>
        </xsl:call-template>
        <xsl:text>]</xsl:text>
        <xsl:if test="$row/following-sibling::csv:record">
            <xsl:text>,</xsl:text>
        </xsl:if>
        <xsl:text>&#x000A;</xsl:text>
        <xsl:if test="$row/following-sibling::csv:record">
            <xsl:call-template name="csv-row2json">
                <xsl:with-param name="row" select="$row/following-sibling::csv:record"/>
                <xsl:with-param name="colmax" select="$colmax"/>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>    

    <xsl:template name="csv-col2json">
        <xsl:param name="row"/>
        <xsl:param name="colcurrent"/>
        <xsl:param name="colmax"/>
        <xsl:if test="$colcurrent != 1">
            <xsl:text>,</xsl:text>
        </xsl:if>
        <xsl:text>'</xsl:text>
        <xsl:value-of select="string($row/csv:field[@number=$colcurrent])"/>
        <xsl:text>'</xsl:text>
        <xsl:if test="$colcurrent &lt; $colmax">
            <xsl:call-template name="csv-col2json">
                <xsl:with-param name="row" select="$row"/>
                <xsl:with-param name="colcurrent" select="$colcurrent + 1"/>
                <xsl:with-param name="colmax" select="$colmax"/>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>
-->

</xsl:stylesheet>
