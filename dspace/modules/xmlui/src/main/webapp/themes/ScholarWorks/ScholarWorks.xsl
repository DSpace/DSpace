<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet
	xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
	xmlns:dri="http://di.tamu.edu/DRI/1.0/"
	xmlns:mets="http://www.loc.gov/METS/"
	xmlns:dc="http://purl.org/dc/elements/1.1/"
	xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
	xmlns:mods="http://www.loc.gov/mods/v3"
	xmlns:xlink="http://www.w3.org/TR/xlink/"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

<xsl:import href="../dri2xhtml.xsl"/>
<xsl:import href="../ada-fixes.xsl"/>
<xsl:output indent="yes"/>


    <!-- The HTML head element contains references to CSS as well as embedded JavaScript code. Most of this
        information is either user-provided bits of post-processing (as in the case of the JavaScript), or
        references to stylesheets pulled directly from the pageMeta element. -->
    <xsl:template name="buildHead">
        <head>
            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
          <link rel="shortcut icon" href="/xmlui/themes/ScholarWorks/images/favicon.ico" type="image/x-icon" />
            
            
            <meta name="Generator">
              <xsl:attribute name="content">
                <xsl:text>DSpace</xsl:text>
                <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='dspace'][@qualifier='version']">
                  <xsl:text> </xsl:text>
                  <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='dspace'][@qualifier='version']"/>
                </xsl:if>
              </xsl:attribute>
            </meta>
            <!-- Add stylsheets -->
            <xsl:for-each select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='stylesheet']">
                <link rel="stylesheet" type="text/css">
                    <xsl:attribute name="media">
                        <xsl:value-of select="@qualifier"/>
                    </xsl:attribute>
                    <xsl:attribute name="href">
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                        <xsl:text>/themes/</xsl:text>
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']"/>
                        <xsl:text>/</xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:attribute>
                </link>
            </xsl:for-each>
            
            <!-- Add syndication feeds -->
            <xsl:for-each select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='feed']">
                <link rel="alternate" type="application">
                    <xsl:attribute name="type">
                        <xsl:text>application/</xsl:text>
                        <xsl:value-of select="@qualifier"/>
                    </xsl:attribute>
                    <xsl:attribute name="href">
                        <xsl:value-of select="."/>
                    </xsl:attribute>
                </link>
            </xsl:for-each>
            
            <!--  Add OpenSearch auto-discovery link -->
            <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='opensearch'][@qualifier='shortName']">
                <link rel="search" type="application/opensearchdescription+xml">
                    <xsl:attribute name="href">
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='scheme']"/>
                        <xsl:text>://</xsl:text>
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='serverName']"/>
                        <xsl:text>:</xsl:text>
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='serverPort']"/>
                        <xsl:value-of select="$context-path"/>
                        <xsl:text>/</xsl:text>
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='opensearch'][@qualifier='context']"/>
                        <xsl:text>description.xml</xsl:text>
                    </xsl:attribute>
                    <xsl:attribute name="title" >
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='opensearch'][@qualifier='shortName']"/>
                    </xsl:attribute>
                </link>
            </xsl:if>
            
            <!-- The following javascript removes the default text of empty text areas when they are focused on or submitted -->
            <!-- There is also javascript to disable submitting a form when the 'enter' key is pressed. -->
                        <script type="text/javascript">
                                //Clear default text of emty text areas on focus
                                function tFocus(element)
                                {
                                        if (element.value == '<i18n:text>xmlui.dri2xhtml.default.textarea.value</i18n:text>'){element.value='';}
                                }
                                //Clear default text of emty text areas on submit
                                function tSubmit(form)
                                {
                                        var defaultedElements = document.getElementsByTagName("textarea");
                                        for (var i=0; i != defaultedElements.length; i++){
                                                if (defaultedElements[i].value == '<i18n:text>xmlui.dri2xhtml.default.textarea.value</i18n:text>'){
                                                        defaultedElements[i].value='';}}
                                }
                                //Disable pressing 'enter' key to submit a form (otherwise pressing 'enter' causes a submission to start over)
                                function disableEnterKey(e)
                                {
                                     var key;
                                
                                     if(window.event)
                                          key = window.event.keyCode;     //Internet Explorer
                                     else
                                          key = e.which;     //Firefox and Netscape
                                
                                     if(key == 13)  //if "Enter" pressed, then disable!
                                          return false;
                                     else
                                          return true;
                                }
            </script>
            
            <!-- Add theme javascipt  -->
            <xsl:for-each select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='javascript'][not(@qualifier)]">
                <script type="text/javascript">
                    <xsl:attribute name="src">
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                        <xsl:text>/themes/</xsl:text>
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']"/>
                        <xsl:text>/</xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:attribute>&#160;</script>
            </xsl:for-each>
            
            <!-- add "shared" javascript from static, path is relative to webapp root-->
            <xsl:for-each select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='javascript'][@qualifier='static']">
                <script type="text/javascript">
                    <xsl:attribute name="src">
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                        <xsl:text>/</xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:attribute>&#160;</script>
            </xsl:for-each>
            
            
            <!-- Add a google analytics script if the key is present -->
            <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='google'][@qualifier='analytics']">
                                <script type="text/javascript">
                                        <xsl:text>var gaJsHost = (("https:" == document.location.protocol) ? "https://ssl." : "http://www.");</xsl:text>
                                        <xsl:text>document.write(unescape("%3Cscript src='" + gaJsHost + "google-analytics.com/ga.js' type='text/javascript'%3E%3C/script%3E"));</xsl:text>
                                </script>

                                <script type="text/javascript">
                                        <xsl:text>try {</xsl:text>
                                                <xsl:text>var pageTracker = _gat._getTracker("</xsl:text><xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='google'][@qualifier='analytics']"/><xsl:text>");</xsl:text>
                                                <xsl:text>pageTracker._trackPageview();</xsl:text>
                                        <xsl:text>} catch(err) {}</xsl:text>
                                </script>
            </xsl:if>
            
            
            <!-- Add the title in -->
            <xsl:variable name="page_title" select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='title']" />
            <title>
                <xsl:choose>
                        <xsl:when test="not($page_title)">
                                <xsl:text>  </xsl:text>
                        </xsl:when>
                        <xsl:otherwise>
                                <xsl:copy-of select="$page_title/node()" />
                        </xsl:otherwise>
                </xsl:choose>
            </title>

            <!-- Head metadata in item pages -->
            <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='xhtml_head_item']">
                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='xhtml_head_item']"
                              disable-output-escaping="yes"/>
            </xsl:if>
            
        </head>
    </xsl:template>
    


<xsl:variable name="server_url">
    <xsl:text>http://</xsl:text>
    <xsl:value-of select="//dri:meta/dri:pageMeta/dri:metadata[@element='request' and @qualifier='serverName']" />
    <xsl:text>:</xsl:text>
    <xsl:value-of select="//dri:meta/dri:pageMeta/dri:metadata[@element='request'and @qualifier='serverPort']" />
</xsl:variable>

<!-- Generate the bitstream information from the file section -->
<xsl:template match="mets:fileGrp[@USE='CONTENT']">
    <xsl:param name="context"/>
    <xsl:param name="primaryBitstream" select="-1"/>
    
    <h2>Preview</h2>
    
    <xsl:for-each select="mets:file">
        <xsl:if test="@MIMETYPE = 'application/pdf'">
            <xsl:variable name="url">
                <xsl:value-of select="$server_url" />
                <xsl:value-of select="mets:FLocat/@xlink:href" />
            </xsl:variable>
            
            <iframe src="http://docs.google.com/gview?url={$url}&amp;embedded=true" style="width:100%; height:600px;" 
                frameborder="0">
             <xsl:text> </xsl:text>  
             </iframe>
        </xsl:if>
    </xsl:for-each>
    
    <h2><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-head</i18n:text></h2>
    
    <table class="ds-table file-list">
        <tr class="ds-table-header-row">
            <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-file</i18n:text></th>
            <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-size</i18n:text></th>
            <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-format</i18n:text></th>
            <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-view</i18n:text></th>
        </tr>
        <xsl:choose>
            <!-- If one exists and it's of text/html MIME type, only display the primary bitstream -->
            <xsl:when test="mets:file[@ID=$primaryBitstream]/@MIMETYPE='text/html'">
                <xsl:apply-templates select="mets:file[@ID=$primaryBitstream]">
                    <xsl:with-param name="context" select="$context"/>
                </xsl:apply-templates>
            </xsl:when>
            <!-- Otherwise, iterate over and display all of them -->
            <xsl:otherwise>
                <xsl:apply-templates select="mets:file">
                    <xsl:sort data-type="number" select="boolean(./@ID=$primaryBitstream)" order="descending" />
                    <xsl:sort select="mets:FLocat[@LOCTYPE='URL']/@xlink:title"/> 
                    <xsl:with-param name="context" select="$context"/>
                </xsl:apply-templates>
            </xsl:otherwise>
        </xsl:choose>
    </table>
</xsl:template>   


<xsl:template match="dri:document">
	<html lang="eng">
	<xsl:call-template name="buildHead"/>
	<body>
	<div id="surround">
		<div id="scholar_nav">
			<div id="scholarworks">
				<a href="/"><img src="{$theme-path}/images/scholarworks.gif" alt="california state university scholar works" width="255" height="147"	border="0"/></a>
			</div>
			<div id="scholar_links">
				<xsl:apply-templates select="dri:options" />
			</div>
		</div>
		<div id="scholar_main">
			<xsl:choose>
				<xsl:when test="contains(dri:body/dri:div/@n, 'front-page-search')">
					<div class="home_top">
						<h2>Cal State Institutional Repository</h2>
					</div>
					<div class="home_communities">
						<h3><i18n:text catalogue="default">xmlui.ArtifactBrowser.CommunityBrowser.head</i18n:text></h3>
						<xsl:apply-templates select="dri:body/dri:div[@n='comunity-browser']/dri:p" />
						<xsl:apply-templates select="dri:body/dri:div[@n='comunity-browser']/dri:includeSet" />
					</div>
				</xsl:when>
				<xsl:otherwise>
					<div id="breadcrumb">
						<xsl:choose>
						<xsl:when test="/dri:document/dri:meta/dri:pageMeta/dri:trail">
							<ul id="ds-trail">
								<xsl:apply-templates select="/dri:document/dri:meta/dri:pageMeta/dri:trail"/>
							</ul>
						</xsl:when>
						<xsl:otherwise>
							<p>&#160;</p>
						</xsl:otherwise>
						</xsl:choose>
					</div>
					
					<xsl:apply-templates select="dri:body" />
					
				</xsl:otherwise>
			</xsl:choose>
		</div>
	</div>
	<div id="scholar_footer">
		<div class="footerLogo">
			<a href="http://www.calstate.edu"><img src="{$theme-path}/images/csu-mark.gif" alt="csu" width="50" height="32" border="0" /></a>
		</div>
		<div class="footerContent"> ScholarWorks is a project of the California State University Libraries. <br/>
			 Hosted by the CSU Office of the Chancellor, running DSpace and Manakin.
		</div>
	</div>
	</body>
	</html>
</xsl:template>

<xsl:template match="dri:options/dri:list[@n = 'account']" priority="5">

</xsl:template>

</xsl:stylesheet>
