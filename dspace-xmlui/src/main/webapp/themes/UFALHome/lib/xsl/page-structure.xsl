<!--
	/* Created for LINDAT/CLARIN */
    Main structure of the UFAL-POINT home page
    header
    banner
    search-panel
    main-contents
    	recent-items & top-items
    	side menu
    footer
    
    Author: Amir Kamran
-->
<xsl:stylesheet version="1.0" 
	xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
	xmlns:dri="http://di.tamu.edu/DRI/1.0/"
	xmlns:mets="http://www.loc.gov/METS/"
	xmlns:xlink="http://www.w3.org/TR/xlink/"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
	xmlns:xhtml="http://www.w3.org/1999/xhtml"
	xmlns:mods="http://www.loc.gov/mods/v3"
	xmlns:confman="org.dspace.core.ConfigurationManager"
	xmlns:util="org.dspace.app.xmlui.utils.XSLUtils"
	xmlns:encoder="xalan://java.net.URLEncoder"
	xmlns:solrClientUtils="org.apache.solr.client.solrj.util.ClientUtils"
	xmlns="http://www.w3.org/1999/xhtml"
	exclude-result-prefixes="i18n encoder solrClientUtils dri mets xlink xsl dim xhtml mods confman util">

	<xsl:output indent="yes" />

    <xsl:variable name="aaiURL">
        <xsl:value-of select="confman:getProperty('lr', 'lr.aai.url')"/>
    </xsl:variable>


	<xsl:template match="dri:document">
		<html>
			<!-- First of all, build the HTML head element -->
			<xsl:call-template name="buildHead" />

			<!-- Then proceed to the body -->
			
			<!--paulirish.com/2008/conditional-stylesheets-vs-css-hacks-answer-neither/-->
            <xsl:text disable-output-escaping="yes">&lt;!--[if lt IE 7 ]&gt; &lt;body class="ie6"&gt; &lt;![endif]--&gt;
                &lt;!--[if IE 7 ]&gt;    &lt;body id="lindat-repository" class="ie7"&gt; &lt;![endif]--&gt;
                &lt;!--[if IE 8 ]&gt;    &lt;body id="lindat-repository" class="ie8"&gt; &lt;![endif]--&gt;
                &lt;!--[if IE 9 ]&gt;    &lt;body id="lindat-repository" class="ie9"&gt; &lt;![endif]--&gt;
                &lt;!--[if (gt IE 9)|!(IE)]&gt;&lt;!--&gt;&lt;body id="lindat-repository"&gt;&lt;!--&lt;![endif]--&gt;</xsl:text>
			
			
			<!-- Common Header -->
			<xsl:call-template name="buildHeader" />

			<xsl:apply-templates select="dri:body" />

			<!-- Common Footer -->
			<xsl:call-template name="buildFooter" />			

			<!-- Javascript at the bottom for fast page loading -->
			<xsl:call-template name="addJavascript" />

			<!--Invisible link to HTML sitemap (for search engines) -->
			<a class="hidden">
				<xsl:attribute name="href">
					<xsl:value-of
							select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
					<xsl:text>/htmlmap</xsl:text>
				</xsl:attribute>
				<xsl:text>&#160;</xsl:text>
			</a>

            <xsl:text disable-output-escaping="yes">&lt;/body&gt;</xsl:text>				
					
		</html>
	</xsl:template>

	<!-- The HTML head element contains references to CSS as well as embedded 
	JavaScript code. Most of this information is either user-provided bits of 
	post-processing (as in the case of the JavaScript), or references to stylesheets 
	pulled directly from the pageMeta element. -->
	<xsl:template name="buildHead">
		<head>
			<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />

			<!-- Always force latest IE rendering engine (even in intranet) & Chrome Frame -->
			<meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1" />

			<meta name="viewport" content="width=device-width, initial-scale=1.0" />
            
			<link rel="shortcut icon">
				<xsl:attribute name="href">
                    <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]" />
                    <xsl:text>/themes/</xsl:text>
                    <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']" />
                    <xsl:text>/images/favicon.ico</xsl:text>
                </xsl:attribute>
			</link>
			
			<!-- link rel="apple-touch-icon">
				<xsl:attribute name="href">
                    <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]" />
                    <xsl:text>/themes/</xsl:text>
                    <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']" />
                    <xsl:text>/images/apple-touch-icon.png</xsl:text>
                </xsl:attribute>
			</link-->

			<meta name="Generator">
				<xsl:attribute name="content">
                <xsl:text>DSpace</xsl:text>
                <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='dspace'][@qualifier='version']">
                  <xsl:text> </xsl:text>
                  <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='dspace'][@qualifier='version']" />
                </xsl:if>
              </xsl:attribute>
			</meta>
			
			<!-- Add stylsheets -->
						
			<xsl:for-each select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='stylesheet']">
				<link rel="stylesheet" type="text/css">
					<xsl:attribute name="media">
                        <xsl:value-of select="@qualifier" />
                    </xsl:attribute>
					<xsl:attribute name="href">
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]" />
                        <xsl:text>/themes/</xsl:text>
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']" />
                        <xsl:text>/</xsl:text>
                        <xsl:value-of select="." />
                    </xsl:attribute>
				</link>
			</xsl:for-each>

			<!-- Add Lindat stylesheet -->
			<link rel="stylesheet" href="{$theme-path}/lib/lindat/public/css/lindat.css" />
			
			<!-- Bootstrap stylesheets -->
			<link rel="stylesheet" href="{$theme-path}/lib/bootstrap/css/bootstrap.min.css" />
            <link rel="stylesheet" href="{$theme-path}/lib/bootstrap/css/font-awesome.min.css" />            
            <link rel="stylesheet" href="{$theme-path}/lib/bootstrap/css/ufal-theme.css" />
            <link rel="stylesheet" href="{$theme-path}/lib/bootstrap/css/repository.css" />
					
			<!-- Add syndication feeds -->
			<xsl:for-each select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='feed']">
				<link rel="alternate" type="application">
					<xsl:attribute name="type">
                        <xsl:text>application/</xsl:text>
                        <xsl:value-of select="@qualifier" />
                    </xsl:attribute>
					<xsl:attribute name="href">
                        <xsl:value-of select="." />
                    </xsl:attribute>
				</link>
			</xsl:for-each>

			<!-- Add OpenSearch auto-discovery link -->
			<xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='opensearch'][@qualifier='shortName']">
				<link rel="search" type="application/opensearchdescription+xml">
					<xsl:attribute name="href">
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='scheme']" />
                        <xsl:text>://</xsl:text>
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='serverName']" />
                        <xsl:text>:</xsl:text>
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='serverPort']" />
                        <xsl:value-of select="$context-path" />
                        <xsl:text>/</xsl:text>
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='opensearch'][@qualifier='autolink']" />
                    </xsl:attribute>
					<xsl:attribute name="title">
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='opensearch'][@qualifier='shortName']" />
                    </xsl:attribute>
				</link>
			</xsl:if>

			<!-- The following javascript removes the default text of empty text areas when they are focused on or submitted -->
			<!-- There is also javascript to disable submitting a form when the 'enter' key is pressed. -->
			<script type="text/javascript">
				
				// Clear default text of emty text areas on focus
				function tFocus(element) {
					if (element.value == '<i18n:text>xmlui.dri2xhtml.default.textarea.value</i18n:text>'){
						element.value='';
					}
				}
				
				// Clear default text of emty text areas on submit
				function tSubmit(form) {
					var defaultedElements = document.getElementsByTagName("textarea");
					for (var i=0; i != defaultedElements.length; i++){
						if (defaultedElements[i].value == '<i18n:text>xmlui.dri2xhtml.default.textarea.value</i18n:text>'){
							defaultedElements[i].value='';
						}
					}
				}
				
				// Disable pressing 'enter' key to submit a form (otherwise pressing 'enter' causes a submission to start over)
				function disableEnterKey(e) {
					var key;
					if(window.event)
						key = window.event.keyCode; //Internet Explorer
					else
						key = e.which; //Firefox and Netscape
		
					if(key == 13) //if "Enter" pressed, then disable!
						return false;
					else
						return true;
				}
		
				function FnArray() {
					this.funcs = new Array;
				}
		
				FnArray.prototype.add = function(f) {
					if( typeof f!= "function" ) {
						f = new Function(f);
					}
					this.funcs[this.funcs.length] = f;
				};
		
				FnArray.prototype.execute = function() {
					for( var i=0; i<xsl:text disable-output-escaping="yes">&lt;</xsl:text>this.funcs.length; i++ ){
						this.funcs[i]();
					}
				};
		
				var runAfterJSImports = new FnArray();
			</script>

            <!-- Add the title in -->
            <xsl:variable name="page_title" select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='title']" />
            <title>
                <xsl:choose>
                        <xsl:when test="normalize-space($static-page-name) != ''">
                                <xsl:value-of select="document(concat('../../html/', $static-page-name, '.xml'))/page/title"/>
                        </xsl:when>
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
				<xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='xhtml_head_item']" disable-output-escaping="yes" />
			</xsl:if>

			<!-- Add all Google Scholar Metadata values -->
			<xsl:for-each select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[substring(@element, 1, 9) = 'citation_']">
				<meta name="{@element}" content="{.}"></meta>
			</xsl:for-each>

            <link href="{concat($aaiURL, '/discojuice/discojuice.css')}" type="text/css" rel="stylesheet" />

		</head>
	</xsl:template>


	<xsl:template match="dri:body">

        <xsl:call-template name="navbar" />
        
		<div class="container-fluid">
			<div class="container">
			
				<xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='alert'][@qualifier='message']">
				<div class="clearfix">
					<div id="ds-system-wide-alert" class="alert alert-danger">
						<h4>
							<xsl:copy-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='alert'][@qualifier='message']/node()" />
						</h4>
					</div>
				</div>
				</xsl:if>
			
				
				<!-- Top banner with lindat logo -->
				<xsl:call-template name="top-banner" />
				
				<!-- A line with lindat branding colors -->
				<div class="row hidden-xs" style="background: url('{$theme-path}/images/lindat_color_line.png'); height: 3px;">&#160;</div>

		       	<div class="visible-xs text-center" style="margin-top: 5px; ">
					<button id="showhidemenu" type="button" class="btn btn-default btn-sm" style="border-radius: 30px; width: 100%;">
							<i class="fa fa-align-justify">&#160;</i> <i18n:text i18n:key="homepage.show_hide_menu">Show/Hide Menu</i18n:text>
					</button>        	        		
		       	</div>        


				<!-- Big SearchBox -->
				<xsl:call-template name="search-box" />

				<!-- Rest of the Body -->
				<div class="row contents">
				
					<div id="main-contents" class="col-sm-9">																								
						<xsl:choose>
							<xsl:when test="dri:div[@n='site-home']/dri:div[@n='site-recent-submission']/dri:referenceSet/dri:reference">
								<xsl:call-template name="recent-submission" />
							</xsl:when>
							<xsl:otherwise>
										<div class="alert alert-warning"><i18n:text i18n:key="homepage.norecent">No Recent Items !</i18n:text></div>
							</xsl:otherwise>
						</xsl:choose>

						<xsl:choose>
							<xsl:when test="count(dri:div[@n='home']/dri:div[@n='stats']//dri:table[@n='list-table']/dri:row) > 1">
								<xsl:call-template name="top-items" />
							</xsl:when>
						</xsl:choose>
					</div>
					<!-- sidebar -->
					<xsl:apply-templates select="/dri:document/dri:options" />
										
				</div>
			</div>
		</div>
	</xsl:template>

	<xsl:template name="facet-box">
		<div id="facet-box" class="row text-center">
				<xsl:for-each select="/dri:document/dri:options/dri:list[@n='discovery']/dri:list">
					<div>
						<xsl:attribute name="class">
							<xsl:choose>
								<xsl:when test="position() = 1">
									<xsl:text>col-md-offset-2 col-md-3 text-left</xsl:text>
								</xsl:when>
								<xsl:otherwise>
									<xsl:text>col-md-3 text-left</xsl:text>
								</xsl:otherwise>
							</xsl:choose>
						</xsl:attribute>
						<ul class="nav nav-list">
						<li style="margin: 10px;">
						<strong>
							<xsl:apply-templates select="dri:head" />
						</strong>
						<ul class="sublist">
							<xsl:for-each select="dri:item">
								<li>
									<a>										
										<xsl:attribute name="href">
											<xsl:value-of select="dri:xref/@target" />
										</xsl:attribute>
										<span>
											<strong class="truncate">
												<xsl:apply-templates select="dri:xref/node()"/>
											</strong>
										</span>
									</a>
								</li>
							</xsl:for-each>
						</ul>
						</li>
						</ul>
					</div>
				</xsl:for-each>
			</div>
	</xsl:template>


	<xsl:template match="dri:div[@n='home']" priority="100">
	</xsl:template>

	<xsl:template
		match="dri:div[@n='site-home']/dri:div[@n='site-recent-submission']"
		priority="100">
	</xsl:template>

	<xsl:template name="recent-submission">
		<xsl:for-each
			select="/dri:document/dri:body/dri:div[@n='site-home']/dri:div[@n='site-recent-submission']">
			<div class="well well-lg" id="recent-submissions">
				<h3 class="recent-submissions-head"><i18n:text i18n:key="homepage.whatsnew">What's New</i18n:text></h3>
				<xsl:for-each select="dri:referenceSet">
					<xsl:for-each select="dri:reference">
						<xsl:if test="position() &lt; 4">
							<xsl:variable name="externalMetadataURL">
								<xsl:text>cocoon:/</xsl:text>
								<xsl:value-of select="@url" />
								<!-- only grab the descriptive metadata, no files section -->
								<xsl:text>?sections=dmdSec,amdSec</xsl:text>
							</xsl:variable>
							<xsl:apply-templates select="document($externalMetadataURL)" mode="recentList" />
						</xsl:if>
					</xsl:for-each>
				</xsl:for-each>
			</div>
		</xsl:for-each>
	</xsl:template>

	<xsl:template match="mets:METS[mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']]" mode="recentList" priority="100">
		<div class="item-box">
			<xsl:variable name="itemWithdrawn" select="mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/@withdrawn" />

			<xsl:variable name="href">
				<xsl:value-of select="@OBJID" />
			</xsl:variable>

			<xsl:choose>
				<xsl:when test="@LABEL='DSpace Item'">
					<xsl:call-template name="recentList-DIM">
						<xsl:with-param name="href" select="$href" />
					</xsl:call-template>
				</xsl:when>
			</xsl:choose>
			<div class="label label-info" style="margin-bottom: 20px;">
                <xsl:variable name="file-size" select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='local' and @element='files' and @qualifier='size']/node()" />
                <xsl:variable name="formatted-file-size">
                    <xsl:call-template name="format-size">                   
                        <xsl:with-param name="size" select="$file-size" />
                    </xsl:call-template>
                </xsl:variable>
                <xsl:variable name="file-count" select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='local' and @element='files' and @qualifier='count']/node()" />
                <i class="fa fa-paperclip">&#160;</i>
                <i18n:translate>
                    <xsl:choose>
                        <xsl:when test="$file-count = 1">
                            <i18n:text>xmlui.UFAL.artifactbrowser.item-contains-one-file</i18n:text>
                        </xsl:when>
                        <xsl:when test="$file-count &gt; 1">
                            <i18n:text>xmlui.UFAL.artifactbrowser.item-contains-many-files</i18n:text>
                        </xsl:when>
                        <xsl:otherwise>
                            <i18n:text>xmlui.UFAL.artifactbrowser.item-contains-no-files</i18n:text>
                        </xsl:otherwise>
                     </xsl:choose>
                     <i18n:param><xsl:value-of select="$file-count"/></i18n:param>
                     <i18n:param><xsl:copy-of select="$formatted-file-size"/></i18n:param>
                </i18n:translate>
			</div>
			<xsl:if test="mets:amdSec/mets:rightsMD/mets:mdWrap/mets:xmlData/license">
                <div style="height: 20px;">&#160;</div>
				<div class="item-label {mets:amdSec/mets:rightsMD/mets:mdWrap/mets:xmlData/license/@label}" >
					<span title="{mets:amdSec/mets:rightsMD/mets:mdWrap/mets:xmlData/license/@label_title}">
						<xsl:value-of
							select="mets:amdSec/mets:rightsMD/mets:mdWrap/mets:xmlData/license/@label_title" />
					</span>
					<xsl:for-each
						select="mets:amdSec/mets:rightsMD/mets:mdWrap/mets:xmlData/license/labels/label">
						<img class="" style="width: 16px"
							src="{$theme-path}/images/licenses/{translate(@label, $uppercase, $smallcase)}.png"
              alt="{@label_title}" title="{@label_title}" />
					</xsl:for-each>
				</div>
			</xsl:if>
		</div>
	</xsl:template>

	<xsl:variable name="smallcase" select="'abcdefghijklmnopqrstuvwxyz'" />
	<xsl:variable name="uppercase" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZ'" />

	<xsl:template name="recentList-DIM">
		<xsl:param name="href" />
		<xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim" mode="recentList-DIM-metadata">
			<xsl:with-param name="href" select="$href" />
		</xsl:apply-templates>
	</xsl:template>

	<xsl:template match="dim:dim" mode="recentList-DIM-metadata">
		<xsl:param name="href" />
		<div class="item-type">
			<xsl:text>&#160;</xsl:text>
			<xsl:value-of select="dim:field[@element='type'][1]/node()" />
			<xsl:text>&#160;</xsl:text>
		</div>
		
        <xsl:if test="dim:field[@mdschema='local' and @element='branding']">
        	<div class="item-branding label">
				<a>
					<xsl:attribute name="href">
						<xsl:copy-of select="$context-path"/>
						<xsl:value-of select="concat('/discover?filtertype=branding&amp;filter_relational_operator=equals&amp;filter=',encoder:encode(dim:field[@mdschema='local' and @element='branding'][1]/node()))"/>
					</xsl:attribute>
					<xsl:value-of select="dim:field[@mdschema='local' and @element='branding'][1]/node()"/>
				</a>
        	</div>
        </xsl:if>
		
		<img class="artifact-icon pull-right" alt="{dim:field[@element='type'][1]/node()}">
			<xsl:attribute name="src">
                                <xsl:text>themes/UFALHome/lib/images/</xsl:text>
                                <xsl:value-of
				select="dim:field[@element='type'][1]/node()" />
                                <xsl:text>.png</xsl:text>
                        </xsl:attribute>
		</img>
		<div class="artifact-title">
			<xsl:element name="a">
				<xsl:attribute name="href">
                        <xsl:value-of select="$href" />
                    </xsl:attribute>
				<xsl:choose>
					<xsl:when test="dim:field[@element='title']">
						<xsl:value-of select="dim:field[@element='title'][1]/node()" />
					</xsl:when>
					<xsl:otherwise>
						<i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:element>
		</div>
		<div class="artifact-info">
			<div class="author-head">
				<i18n:text i18n:key="homepage.item.authors">Author(s):</i18n:text>
			</div>
			<div class="author">
				<xsl:choose>
					<xsl:when test="dim:field[@element='contributor'][@qualifier='author' or @qualifier='other']">
						<xsl:for-each
							select="dim:field[@element='contributor'][@qualifier='author' or @qualifier='other']">
							<span>
								<xsl:if test="@authority">
									<xsl:attribute name="class"><xsl:text>ds-dc_contributor_author-authority</xsl:text></xsl:attribute>
								</xsl:if>
                                <a>
									<xsl:attribute name="href"><xsl:copy-of select="$context-path"/>/browse?value=<xsl:copy-of select="node()" />&amp;type=author</xsl:attribute>
									<xsl:copy-of select="node()" />
								</a>                                
							</span>
							<xsl:if
								test="count(following-sibling::dim:field[@element='contributor'][@qualifier='author' or @qualifier='other']) != 0">
								<xsl:text>; </xsl:text>
							</xsl:if>
						</xsl:for-each>
					</xsl:when>
					<xsl:when test="dim:field[@element='creator']">
						<xsl:for-each select="dim:field[@element='creator']">
                            <a>
                                <xsl:attribute name="href"><xsl:copy-of select="$context-path"/>/browse?value=<xsl:copy-of select="node()" />&amp;type=author</xsl:attribute>
                                <xsl:copy-of select="node()" />
                            </a>                                
							<xsl:if
								test="count(following-sibling::dim:field[@element='creator']) != 0">
								<xsl:text>; </xsl:text>
							</xsl:if>
						</xsl:for-each>
					</xsl:when>
					<xsl:otherwise>
						<i18n:text>xmlui.dri2xhtml.METS-1.0.no-author</i18n:text>
					</xsl:otherwise>
				</xsl:choose>
			</div>
			<xsl:text> </xsl:text>
		</div>
		<xsl:choose>
			<xsl:when
				test="dim:field[@element = 'description' and @qualifier='abstract']">
				<xsl:variable name="abstract"
					select="dim:field[@element = 'description' and @qualifier='abstract']/node()" />
				<div class="artifact-abstract-head">
					<i18n:text i18n:key="homepage.item.description">Description:</i18n:text>
				</div>
				<div class="artifact-abstract">
					<xsl:value-of select="util:shortenString($abstract, 220, 10)" />
				</div>
			</xsl:when>
			<xsl:when test="dim:field[@element = 'description' and not(@qualifier)]">
				<xsl:variable name="description"
					select="dim:field[@element = 'description' and not(@qualifier)]/node()" />
				<div class="artifact-abstract-head">
					<i18n:text i18n:key="homepage.item.description">Description:</i18n:text>
				</div>
				<div class="artifact-abstract">
					<xsl:value-of select="util:shortenString($description, 220, 10)" />
				</div>
			</xsl:when>
		</xsl:choose>
	</xsl:template>

	<xsl:template name="top-items">

		<xsl:for-each
			select="/dri:document/dri:body/dri:div[@n='home']/dri:div[@n='stats']">
			<div class="container well well-lg" id="top-items">
				<h3 class="top-items-head"><xsl:copy-of select="dri:head/node()" /></h3>
				<xsl:for-each select="dri:div/dri:table">
					<div class="col-md-12 no-padding" style="padding: 2px;">
					<div class="panel panel-default">
					<div class="panel-heading bold"><xsl:copy-of select="dri:head/node()" /></div>
					<div class="panel-body">
					<xsl:for-each select="dri:row">
					<xsl:if test="position() &gt; 1 and position() &lt;= 4">
						<xsl:for-each select="dri:cell">
							<xsl:if test="dri:xref/@target">
								<xsl:variable name="externalMetadataURL">
									<xsl:text>cocoon://metadata</xsl:text>
									<xsl:value-of select="substring-after(dri:xref/@target, $context-path)" />
									<xsl:text>/mets.xml</xsl:text>
									<!-- only grab the descriptive metadata, no files section -->
									<xsl:text>?sections=dmdSec,amdSec</xsl:text>
								</xsl:variable>
								<xsl:apply-templates select="document($externalMetadataURL)"
									mode="recentList" />
							</xsl:if>
						</xsl:for-each>						
					</xsl:if>
					</xsl:for-each>&#160;
					</div>
					</div>		
					</div>			
				</xsl:for-each>
			</div>
		</xsl:for-each>

	</xsl:template>

	<xsl:template name="search-box">
		<div class="row jumbotron" style="margin-bottom: 0px;">		
			<form class="form-search" method="post">
				<xsl:attribute name="action">
                            <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath']" />
                            <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='search'][@qualifier='simpleURL']" />
                        </xsl:attribute>
                <div class="row">
					<div class="input-group input-group-lg col-md-6 col-md-offset-3">
						<span class="input-group-addon"><i class="fa fa-search fa-lg" style="color: #7479B8;">&#160;</i></span>
						<input class="form-control" type="text">
							<xsl:attribute name="name">
								<xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='search'][@qualifier='queryField']" />
							</xsl:attribute>
						</input>
						<span class="input-group-btn">
							<input class="btn btn-large btn-repository" name="submit" type="submit" i18n:attr="value" value="xmlui.general.search" />
						</span>
					</div>
				</div>
				<div class="container-fluid text-center">
					<ul class="list-inline">
						<!--Only add if the advanced search url is different from the simple search -->
						<xsl:if
							test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='search'][@qualifier='advancedURL'] != /dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='search'][@qualifier='simpleURL']">
								<li class="no-padding">
									<strong>
									<a class="btn btn-link btn-small no-padding">
									<xsl:attribute name="href"><xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='search'][@qualifier='advancedURL']" /><xsl:text>?advance</xsl:text></xsl:attribute>
									<i18n:text>xmlui.dri2xhtml.structural.search-advanced</i18n:text>
									</a>
									</strong>
								</li>
						</xsl:if>
					</ul>
				</div>
			</form>
                        <xsl:if test="/dri:document/dri:options/dri:list[@n='discovery']/dri:list">
                                <xsl:call-template name="facet-box" />
                        </xsl:if>			
		</div>
	</xsl:template>


	<!-- Currently the dri:meta element is not parsed directly. Instead, parts 
		of it are referenced from inside other elements (like reference). The blank 
		template below ends the execution of the meta branch -->
	<xsl:template match="dri:meta" />

	<xsl:template name="addJavascript">
		<xsl:variable name="jqueryVersion">
			<xsl:text>1.7</xsl:text>
		</xsl:variable>

		<xsl:variable name="protocol">
			<xsl:choose>
				<xsl:when
					test="starts-with(confman:getProperty('dspace.baseUrl'), 'https://')">
					<xsl:text>https://</xsl:text>
				</xsl:when>
				<xsl:otherwise>
					<!-- UFAL #303 - dspace thinks after redirect that it is still in http -->
					<xsl:text>https://</xsl:text>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<script type="text/javascript"
			src="{concat($protocol, 'ajax.googleapis.com/ajax/libs/jquery/', $jqueryVersion ,'/jquery.min.js')}">&#160;</script>
		<script type="text/javascript" src="{$theme-path}/lib/js/jquery-ui.js">&#160;</script>
		<script type="text/javascript" src="{$theme-path}/lib/js/jquery.i18n.js">&#160;</script>

        <script type="text/javascript" src="{concat($aaiURL, '/discojuice/discojuice-2.1.en.min.js')}">&#160;</script>
        <script type="text/javascript" src="{concat($aaiURL, '/aai.js')}">&#160;</script>

		<xsl:variable name="localJQuerySrc">
			<xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]" />
			<xsl:text>/static/js/jquery-</xsl:text>
			<xsl:value-of select="$jqueryVersion" />
			<xsl:text>.min.js</xsl:text>
		</xsl:variable>

		<script type="text/javascript">
			<xsl:text disable-output-escaping="yes">!window.jQuery &amp;&amp; document.write('&lt;script type="text/javascript" src="</xsl:text>
			<xsl:value-of select="$localJQuerySrc" />
			<xsl:text disable-output-escaping="yes">"&gt;&#160;&lt;\/script&gt;')</xsl:text>
		</script>

		<script type="text/javascript">
			<xsl:attribute name="src">
	             <xsl:value-of select="$theme-path" />
	             <xsl:text>/lib/bootstrap/js/bootstrap.min.js</xsl:text>
			</xsl:attribute>&#160;</script>
		
		<script type="text/javascript">
			<xsl:attribute name="src">
	             <xsl:value-of select="$theme-path" />
	             <xsl:text>/lib/bootstrap/js/ufal.min.js</xsl:text>
			</xsl:attribute>&#160;</script>

		<!-- Add theme javascipt -->
		<xsl:for-each
			select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='javascript'][not(@qualifier)]">
			<script type="text/javascript">
				<xsl:attribute name="src">
                    <xsl:value-of
					select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]" />
                    <xsl:text>/themes/</xsl:text>
                    <xsl:value-of
					select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']" />
                    <xsl:text>/</xsl:text>
                    <xsl:value-of select="." />
                </xsl:attribute>
				&#160;
			</script>
		</xsl:for-each>

		<!-- add "shared" javascript from static, path is relative to webapp root -->
		<xsl:for-each
			select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='javascript'][@qualifier='static']">
			<!--This is a dirty way of keeping the scriptaculous stuff from choice-support 
				out of our theme without modifying the administrative and submission sitemaps. 
				This is obviously not ideal, but adding those scripts in those sitemaps is 
				far from ideal as well -->
			<xsl:choose>
				<xsl:when test="text() = 'static/js/choice-support.js'">
					<script type="text/javascript">
						<xsl:attribute name="src">
                            <xsl:value-of
							select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]" />
                            <xsl:text>/themes/</xsl:text>
                            <xsl:value-of
							select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']" />
                            <xsl:text>/lib/js/choice-support.js</xsl:text>
                        </xsl:attribute>
						&#160;
					</script>
				</xsl:when>
				<xsl:when test="not(starts-with(text(), 'static/js/scriptaculous'))">
					<script type="text/javascript">
						<xsl:attribute name="src">
                            <xsl:value-of
							select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]" />
                            <xsl:text>/</xsl:text>
                            <xsl:value-of select="." />
                        </xsl:attribute>
						&#160;
					</script>
				</xsl:when>
			</xsl:choose>
		</xsl:for-each>

		<!--PNG Fix for IE6 -->
		<xsl:text disable-output-escaping="yes">&lt;!--[if lt IE 7 ]&gt;</xsl:text>
		<script type="text/javascript">
			<xsl:attribute name="src">
                <xsl:value-of
				select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]" />
                <xsl:text>/themes/</xsl:text>
                <xsl:value-of
				select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']" />
                <xsl:text>/lib/js/DD_belatedPNG_0.0.8a.js?v=1</xsl:text>
            </xsl:attribute>
			&#160;
		</script>
		<script type="text/javascript">
			<xsl:text>DD_belatedPNG.fix('#ds-header-logo');DD_belatedPNG.fix('#ds-footer-logo');$.each($('img[src$=png]'), function() {DD_belatedPNG.fixPng(this);});</xsl:text>
		</script>
		<xsl:text disable-output-escaping="yes">&lt;![endif]--&gt;</xsl:text>

		<script type="text/javascript">
			runAfterJSImports.execute();
		</script>

		<!-- Add a google analytics script if the key is present -->
		<xsl:if
			test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='google'][@qualifier='analytics']">
			<script type="text/javascript">
				<xsl:text>
                   var _gaq = _gaq || [];
                   _gaq.push(['_setAccount', '</xsl:text>
				<xsl:value-of
					select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='google'][@qualifier='analytics']" />
				<xsl:text>']);
                   _gaq.push(['_trackPageview']);

                   (function() {
                       var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
                       ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
                       var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
                   })();
           </xsl:text>
			</script>
		</xsl:if>
		
		<script>				
			jQuery.each(jQuery("#facet-box .truncate"), function(index, item) {
				splitIndex = item.textContent.lastIndexOf("(");
				if(splitIndex != -1) {
					text = item.textContent.substr(0, splitIndex);
					count = item.textContent.substr(splitIndex);
					if(text.length <xsl:text disable-output-escaping="yes">&gt;</xsl:text> 25)
						text = text.substr(0, 20) + " ...";
					item.textContent = text + count;
				}
			});
		</script>		
		
	</xsl:template>
	
	<xsl:template name="top-banner">
	<div class="row hidden-xs">
		<div style="height: 160px;" class="carousel col-xs-12 col-sm-12 col-md-7 col-lg-8" id="layerslider">
			<ol class="carousel-indicators">
				<li class="active" data-slide-to="0" data-target="#layerslider" />
				<li data-slide-to="1" data-target="#layerslider" />
				<li data-slide-to="2" data-target="#layerslider" />
			</ol>
			<div class="carousel-inner">
				<div class="item active">
					<div style="position: relative; height: 180px;">
						<img style="width: 100px; position: absolute; left: 22%; top: 20%" src="{$context-path}/themes/UFALHome/lib/images/glass.png" />
						<h3 style="left: 34%; position: absolute; top: 25%;"><i18n:text i18n:key="homepage.carousel.data_tools">Linguistic Data and NLP Tools</i18n:text></h3>
						<h5 style="left: 40%; position: absolute; top: 15%;"><i18n:text i18n:key="homepage.carousel.find">Find</i18n:text></h5>
						<h5 style="left: 54%; position: absolute; top: 45%;"><i18n:text i18n:key="homepage.carousel.citation_support">Citation Support (with Persistent IDs)</i18n:text></h5>
					</div>
				</div>
				<div class="item">
					<div style="position: relative; height: 180px;">
						<h3 style="left: 40%; position: absolute; top: 10%;"><i18n:text i18n:key="homepage.carousel.deposit">Deposit Free and Safe</i18n:text></h3>
						<h5 style="left: 28%; position: absolute; top: 30%;"><i18n:text i18n:key="homepage.carousel.license">License of your Choice (Open licenses encouraged)</i18n:text></h5>
						<h5 style="left: 32%; position: absolute; top: 42%;"><i18n:text i18n:key="homepage.carousel.easy_find">Easy to Find</i18n:text></h5>
						<h5 style="left: 36%; position: absolute; top: 54%;"><i18n:text i18n:key="homepage.carousel.easy_cite">Easy to Cite</i18n:text></h5>
					</div>
				</div>
				<div class="item">
					<div style="position: relative; height: 180px;">
						<div style="position: absolute; width: 65%; top: 20%; left: 20%; line-height: 20px;">
							<blockquote>
								<strong>
									<i class="fa fa-quote-left fa-2x pull-left">&#160;</i>
									<i18n:text i18n:key="homepage.carousel.quote">“There ought to be only one grand dépôt of art in the world, to
									which the artist might repair with his works, and on presenting them
									receive what he required... ”</i18n:text>
								</strong>
								<small>Ludwig van Beethoven, 1801</small>
							</blockquote>
						</div>
					</div>
				</div>
			</div>
		</div>
		<div class="col-md-5 col-lg-4 hidden-xs hidden-sm">
			<div class="row">
				<div style="height: 160px; position: relative;" class="col-md-7 col-lg-7">
				  <a href="/lindat">
			            <img src="{$context-path}/themes/UFAL/images/lindat/lindat-logo.png" style="position: absolute; height: 60%; top: 0px; bottom: 0px; margin: auto;" class="logo" alt="LINDAT/CLARIN logo" /></a>
				</div>
		                <div style="height: 160px; position: relative;" class="col-md-5 col-lg-5">
				    <a href="http://www.clarin.eu/">
		                    <img src="{$context-path}/themes/UFAL/images/lindat/clarin-logo.png" style="position: absolute; height: 70%; top: 0px; bottom: 0px; margin: auto;" class="logo" alt="LINDAT/CLARIN logo" /></a>
		                </div>
			</div>
        </div>		
	</div>	
	</xsl:template>
</xsl:stylesheet>



