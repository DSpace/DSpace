<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xhtml="http://www.w3.org/1999/xhtml"
    xmlns:ddw="http://datadryad.org/api/v1/widgets/display"
    xmlns:csv="http://apache.org/cocoon/csv/1.0"
    xmlns:math="http://exslt.org/math"
    exclude-result-prefixes="xhtml ddw math csv"
    version="1.0">
        
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
                    <!-- 
                        TODO: coordinate this using properties/config with the
                              text/csv generator
                    -->
                    <xsl:if test="//csv:record[position()=last()]/@number &gt;= 100">
                        <xsl:variable name="all-cols" select="math:max(//csv:record/@number)"/>
                        <tfoot>
                            <tr><td colspan="{$all-cols}">Please download the file to see data additional rows.</td></tr>
                        </tfoot>
                    </xsl:if>
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