<!-- The contents of this file are subject to the license and copyright detailed 
	in the LICENSE and NOTICE files at the root of the source tree and available 
	online at http://www.dspace.org/license/ -->
<!-- Main structure of the page, determines where header, footer, body, navigation 
	are structurally rendered. Rendering of the header, footer, trail and alerts -->

<xsl:stylesheet xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
	xmlns:dri="http://di.tamu.edu/DRI/1.0/" xmlns:mets="http://www.loc.gov/METS/"
	xmlns:xlink="http://www.w3.org/TR/xlink/" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="1.0" xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
	xmlns:xhtml="http://www.w3.org/1999/xhtml" xmlns:mods="http://www.loc.gov/mods/v3"
	xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:confman="org.dspace.core.ConfigurationManager"
	xmlns="http://www.w3.org/1999/xhtml" exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc confman">

	<xsl:import href="html-head.xsl" />
	<xsl:import href="html-body-header.xsl" />
	<xsl:import href="html-body-content.xsl" />
	<xsl:import href="html-body-footer.xsl" />
	<xsl:output indent="yes" />

	<!-- The starting point of any XSL processing is matching the root element. 
		In DRI the root element is document, which contains a version attribute and 
		three top level elements: body, options, meta (in that order). This template 
		creates the html document, giving it a head and body. A title and the CSS 
		style reference are placed in the html head, while the body is further split 
		into several divs. The top-level div directly under html body is called "ds-main". 
		It is further subdivided into: "ds-header" - the header div containing title, 
		subtitle, trail and other front matter "ds-body" - the div containing all 
		the content of the page; built from the contents of dri:body "ds-options" 
		- the div with all the navigation and actions; built from the contents of 
		dri:options "ds-footer" - optional footer div, containing misc information 
		The order in which the top level divisions appear may have some impact on 
		the design of CSS and the final appearance of the DSpace page. While the 
		layout of the DRI schema does favor the above div arrangement, nothing is 
		preventing the designer from changing them around or adding new ones by overriding 
		the dri:document template. -->
	<xsl:template match="dri:document">
		<html class="no-js">
			<!-- First of all, build the HTML head element -->
			<head>
				<xsl:call-template name="buildHead" />
				<xsl:apply-templates select="dri:meta"/>
			</head>
			
			<!-- Then proceed to the body -->
			<body>

				<xsl:choose>
					<xsl:when
						test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='framing'][@qualifier='popup']">
								<xsl:apply-templates select="dri:body" />
					</xsl:when>
					<xsl:otherwise>
						<div class="container-fluid" style="height:100vh">
							<!--The header div, complete with title, subtitle and other junk -->
							<xsl:call-template name="buildHeader" />

							<xsl:apply-templates select="dri:body" />
							
							<xsl:call-template name="buildFooter" />
						</div>
					</xsl:otherwise>
				</xsl:choose>
				<!-- Javascript at the bottom for fast page loading -->
				<xsl:call-template name="addJavascript" />

			</body>
		</html>
	</xsl:template>




	<!-- Currently the dri:meta element is not parsed directly. Instead, parts 
		of it are referenced from inside other elements (like reference). The blank 
		template below ends the execution of the meta branch -->
	<xsl:template match="dri:meta">
	</xsl:template>


<!-- 
	################################################################################################### 
	############################## OTHER AUXILIAR TEMPLATES ###########################################
	###################################################################################################
-->


	<!-- Meta's children: userMeta, pageMeta, objectMeta and repositoryMeta 
		may or may not have templates of their own. This depends on the meta template 
		implementation, which currently does not go this deep. <xsl:template match="dri:userMeta" 
		/> <xsl:template match="dri:pageMeta" /> <xsl:template match="dri:objectMeta" 
		/> <xsl:template match="dri:repositoryMeta" /> -->

	<xsl:template name="addJavascript">

		<!-- Add theme javascipt -->
		<xsl:for-each
			select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='javascript'][@qualifier='url']">
			<script type="text/javascript">
				<xsl:attribute name="src">
                    <xsl:value-of select="." />
                </xsl:attribute>
				&#160;
			</script>
		</xsl:for-each>

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
				
			</xsl:choose>
		</xsl:for-each>

		<!-- add setup JS code if this is a choices lookup page -->
		<xsl:if test="dri:body/dri:div[@n='lookup']">
			<xsl:call-template name="choiceLookupPopUpSetup" />
		</xsl:if>



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

		<!-- Add a contextpath to a JS variable -->
		<script type="text/javascript">
			<xsl:text>
                         if(typeof window.orcid === 'undefined'){
                            window.orcid={};
                          };
                        window.orcid.contextPath= '</xsl:text>
			<xsl:value-of
				select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]" />
			<xsl:text>';</xsl:text>
			<xsl:text>window.orcid.themePath= '</xsl:text>
			<xsl:value-of select="$theme-path" />
			<xsl:text>';</xsl:text>
		</script>

		<!-- When on submission process, disable buttons on submission -->
		<!-- Update an input-forms, if exists a dc.type metadata, every time this 
			element change its value. -->
		<xsl:if test="dri:body/dri:div[contains(@rend,'primary submission')]">
			<script type="text/javascript">
				$(function() {

				$('form.submission').submit(function() {
				if($(this).data("submitted") === true)
				return false;
				else
				$(this).data("submitted", true);
				});

				/* Variable que guarda el valor del select para chequear si hay que
				disparar el alert() o no */
				var dcTypeElement = $('form.submission select[name="dc_type"]');
				var oldTypeValue = dcTypeElement.val();
				$('form.submission select[name="dc_type"]').change(function() {
				var permitirSubmit = false;
				if(oldTypeValue == "")
				permitirSubmit = true;
				else
				permitirSubmit = confirm("¿Está seguro que desea cambiar el tipo de documento?");

				if(permitirSubmit) {
				/* Limpiamos el subtype para evitar que quede inconsistente */
				$('form.submission select[name="sedici_subtype"]').val("");
				$('form.submission').submit();
				} else {
				dcTypeElement.val(oldTypeValue);
				}
				});

				});
			</script>
		</xsl:if>
			
	        <script type="text/javascript">
	        <xsl:text disable-output-escaping="yes">
	        (function ($) {
			    /**
			     * When clicking an item li in a discovery context, openit
			     */
			    $(document).ready(function() {
					$('.discovery-list-results > li').click(function(){
						$(this).find('.artifact-title a')[0].click();
					})
			    });
			})(jQuery);
			
			(function ($) {
			    /**
			     * Collapse Discovery filters
			     */
			    $(document).ready(function() {
					
					$('#aspect_discovery_SimpleSearch_div_discovery-filters-wrapper').hide();
					$('#aspect_discovery_SimpleSearch_div_search-filters > .ds-div-head').click(function(){
						$('#aspect_discovery_SimpleSearch_div_discovery-filters-wrapper').toggle();
					});
					
			    });
			})(jQuery);
			
			
			</xsl:text>
	        </script>
	        
	        <script type="text/javascript">
			runAfterJSImports.execute();
			</script>
	</xsl:template>



	<!-- Otros templates -->


	<xsl:template name="cc-license">
		<xsl:param name="metadataURL" />
		<xsl:variable name="externalMetadataURL">
			<xsl:text>cocoon:/</xsl:text>
			<xsl:value-of select="$metadataURL" />
			<xsl:text>?sections=dmdSec,fileSec&amp;fileGrpTypes=THUMBNAIL</xsl:text>
		</xsl:variable>

		<xsl:variable name="ccLicenseName"
			select="document($externalMetadataURL)//dim:field[@element='rights']" />
		<xsl:variable name="ccLicenseUri"
			select="document($externalMetadataURL)//dim:field[@element='rights'][@qualifier='uri']" />
		<xsl:variable name="handleUri">
			<xsl:for-each
				select="document($externalMetadataURL)//dim:field[@element='identifier' and @qualifier='uri']">
				<a>
					<xsl:attribute name="href">
                                <xsl:copy-of select="./node()" />
                            </xsl:attribute>
					<xsl:copy-of select="./node()" />
				</a>
				<xsl:if
					test="count(following-sibling::dim:field[@element='identifier' and @qualifier='uri']) != 0">
					<xsl:text>, </xsl:text>
				</xsl:if>
			</xsl:for-each>
		</xsl:variable>

		<xsl:if
			test="$ccLicenseName and $ccLicenseUri and contains($ccLicenseUri, 'creativecommons')">
			<div about="{$handleUri}" class="clearfix">
				<xsl:attribute name="style">
                <xsl:text>margin:0em 2em 0em 2em; padding-bottom:0em;</xsl:text>
            </xsl:attribute>
				<a rel="license" href="{$ccLicenseUri}" alt="{$ccLicenseName}"
					title="{$ccLicenseName}">
					<xsl:call-template name="cc-logo">
						<xsl:with-param name="ccLicenseName" select="$ccLicenseName" />
						<xsl:with-param name="ccLicenseUri" select="$ccLicenseUri" />
					</xsl:call-template>
				</a>
				<span>
					<xsl:attribute name="style">
                    <xsl:text>vertical-align:middle; text-indent:0 !important;</xsl:text>
                </xsl:attribute>
					<i18n:text>xmlui.dri2xhtml.METS-1.0.cc-license-text</i18n:text>
					<xsl:value-of select="$ccLicenseName" />
				</span>
			</div>
		</xsl:if>
	</xsl:template>

	<xsl:template name="cc-logo">
		<xsl:param name="ccLicenseName" />
		<xsl:param name="ccLicenseUri" />
		<xsl:variable name="ccLogo">
			<xsl:choose>
				<xsl:when
					test="starts-with($ccLicenseUri,
                                           'http://creativecommons.org/licenses/by/')">
					<xsl:value-of select="'cc-by.png'" />
				</xsl:when>
				<xsl:when
					test="starts-with($ccLicenseUri,
                                           'http://creativecommons.org/licenses/by-sa/')">
					<xsl:value-of select="'cc-by-sa.png'" />
				</xsl:when>
				<xsl:when
					test="starts-with($ccLicenseUri,
                                           'http://creativecommons.org/licenses/by-nd/')">
					<xsl:value-of select="'cc-by-nd.png'" />
				</xsl:when>
				<xsl:when
					test="starts-with($ccLicenseUri,
                                           'http://creativecommons.org/licenses/by-nc/')">
					<xsl:value-of select="'cc-by-nc.png'" />
				</xsl:when>
				<xsl:when
					test="starts-with($ccLicenseUri,
                                           'http://creativecommons.org/licenses/by-nc-sa/')">
					<xsl:value-of select="'cc-by-nc-sa.png'" />
				</xsl:when>
				<xsl:when
					test="starts-with($ccLicenseUri,
                                           'http://creativecommons.org/licenses/by-nc-nd/')">
					<xsl:value-of select="'cc-by-nc-nd.png'" />
				</xsl:when>
				<xsl:when
					test="starts-with($ccLicenseUri,
                                           'http://creativecommons.org/publicdomain/zero/')">
					<xsl:value-of select="'cc-zero.png'" />
				</xsl:when>
				<xsl:when
					test="starts-with($ccLicenseUri,
                                           'http://creativecommons.org/publicdomain/mark/')">
					<xsl:value-of select="'cc-mark.png'" />
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="'cc-generic.png'" />
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<xsl:variable name="ccLogoImgSrc">
			<xsl:value-of select="$theme-path" />
			<xsl:text>/images/creativecommons/</xsl:text>
			<xsl:value-of select="$ccLogo" />
		</xsl:variable>
		<img>
			<xsl:attribute name="src">
                <xsl:value-of select="$ccLogoImgSrc" />
             </xsl:attribute>
			<xsl:attribute name="alt">
                 <xsl:value-of select="$ccLicenseName" />
             </xsl:attribute>
			<xsl:attribute name="style">
                 <xsl:text>float:left; margin:0em 1em 0em 0em; border:none;</xsl:text>
             </xsl:attribute>
		</img>
	</xsl:template>


</xsl:stylesheet>
