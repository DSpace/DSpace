<?xml version="1.0" encoding="UTF-8"?>

<!--
    Gallery.xsl
    
    Implements an image gallery view for Manakin. See the public "About this Theme"
    page for instructions on use, credits and license info.
    
    Eric Jansson
    National Institute for Technology in Liberal Education - NITLE
    http://www.nitle.org
-->
    

<xsl:stylesheet xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
	xmlns:dri="http://di.tamu.edu/DRI/1.0/" xmlns:mets="http://www.loc.gov/METS/"
	xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
	xmlns:mods="http://www.loc.gov/mods/v3" xmlns:xlink="http://www.w3.org/TR/xlink/"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

	<xsl:import href="../dri2xhtml.xsl"/>
	<xsl:import href="config.xsl"/>
	<xsl:output indent="yes"/>

	<!-- THEME CONFIGURATION OPTIONS -->

	<!-- using these 2 options, you can restrict navigation to this collection, 
    removing links to outside colelctions, communities, etc -->

	<!--  THEME VARIABLES -->

	<!-- the URL of this theme, used to make building paths to referenced files easier -->
	<xsl:variable name="themePath">
		<xsl:value-of
			select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
		<xsl:text>/themes/</xsl:text>
		<xsl:value-of
			select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']"/>
		<xsl:text>/</xsl:text>
	</xsl:variable>

	<!-- serverUrl: path to the  server, up through the port -->
	<xsl:variable name="serverUrl">
		<xsl:value-of
			select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='scheme']"/>
		<xsl:text>://</xsl:text>
		<xsl:value-of
			select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='serverName']"/>
		<xsl:text>:</xsl:text>
		<xsl:value-of
			select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='serverPort']"
		/>
	</xsl:variable>

	<!-- apgeUrl: path to the  server, up through the port -->
	<xsl:variable name="pageUrl">
		<xsl:value-of select="$serverUrl"/>
		<xsl:value-of
			select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath']"/>
		<xsl:text>/</xsl:text>
		<xsl:value-of
			select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='URI']"
		/>
	</xsl:variable>

	<xsl:variable name="counter">
		<xsl:value-of select="1"/>
	</xsl:variable>


	<!-- 
        From: structural.xsl
        Changes:  
                1. Added $themePath variable in a number of places  to reduce number of lookups ovia XPath
                2. Added JS libraries : JQuery, AnythingZoomer, FancyBox
    -->
	<xsl:template name="buildHead">
		<head>
			<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
			
			<!-- pass through some config values to Javascript -->
			<script type="text/javascript">
				var ZOOMABLE_IMG_WIDTH = <xsl:value-of select="$config-zoomPanelWidth" />;
				var MAX_SERVICE_IMG_SIZE = <xsl:value-of select="$config-maxServiceImageSize" />;
			</script>
			
			<!-- Add stylsheets -->
			<xsl:for-each
				select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='stylesheet']">
				<link rel="stylesheet" type="text/css">
					<xsl:attribute name="media">
						<xsl:value-of select="@qualifier"/>
					</xsl:attribute>
					<xsl:attribute name="href">
						<xsl:value-of select="$themePath"/>
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

			<!-- JQuery JS   -->
			<script type="text/javascript">
				<xsl:attribute name="src">
					<xsl:value-of select="$themePath"/>
					<xsl:text>lib/jquery-1.3.2.min.js</xsl:text>
				</xsl:attribute> &#160; </script>

			<!-- the following javascript removes the default text of empty text areas when they are focused on or submitted -->
			<script type="text/javascript"> function tFocus(element){if (element.value ==
				'<i18n:text>xmlui.dri2xhtml.default.textarea.value</i18n:text>'){element.value='';}}
				function tSubmit(form){var defaultedElements =
				document.getElementsByTagName("textarea"); for (var i=0; i !=
				defaultedElements.length; i++){ if (defaultedElements[i].value ==
				'<i18n:text>xmlui.dri2xhtml.default.textarea.value</i18n:text>'){
				defaultedElements[i].value='';}}} </script>

			<!-- Add javascript  -->
			<xsl:for-each
				select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='javascript']">
				<script type="text/javascript">
					<xsl:attribute name="src">
						<xsl:value-of select="$themePath"/>
						<xsl:value-of select="."/>
					</xsl:attribute>.</script>
			</xsl:for-each>
			<!-- Add the title in -->
			<xsl:variable name="page_title"
				select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='title']"/>

			<!-- Add  Gallery JS and CSS  -->
			<link rel="stylesheet" type="text/css">
				<xsl:attribute name="href">
					<xsl:value-of select="$themePath"/>
					<xsl:text>lib/gallery.css</xsl:text>
				</xsl:attribute>
			</link>
			
			<script type="text/javascript">
				<xsl:attribute name="src">
					<xsl:value-of select="$themePath"/>
					<xsl:text>lib/gallery.js</xsl:text>
				</xsl:attribute> &#160; </script>

			<!-- Add Fancy Box JS and CSS -->
			<link rel="stylesheet" type="text/css">
				<xsl:attribute name="href">
					<xsl:value-of select="$themePath"/>
					<xsl:text>lib/fancybox/jquery.fancybox-1.2.5.css</xsl:text>
				</xsl:attribute>
			</link>

			<script type="text/javascript">
				<xsl:attribute name="src">
					<xsl:value-of select="$themePath"/>
					<xsl:text>lib/fancybox/jquery.fancybox-1.2.5.js</xsl:text>
				</xsl:attribute> &#160; </script>
			
			<!-- Add jqpuzzle JS and CSS   -->
			<script type="text/javascript">
				<xsl:attribute name="src">
					<xsl:value-of select="$themePath"/>
					<xsl:text>lib/jqpuzzle/jquery.jqpuzzle.min.js</xsl:text>
				</xsl:attribute> &#160; </script>
			
			<link rel="stylesheet" type="text/css">
				<xsl:attribute name="href">
					<xsl:value-of select="$themePath"/>
					<xsl:text>lib/jqpuzzle/jquery.jqpuzzle.css</xsl:text>
				</xsl:attribute>
			</link>

			<!--Add TJPzoom library   -->
			<script type="text/javascript">
				<xsl:attribute name="src">
					<xsl:value-of select="$themePath"/>
					<xsl:text>lib/tjpzoom/tjpzoom.js</xsl:text>
				</xsl:attribute>&#160; </script>
			
			<script type="text/javascript">
				<xsl:attribute name="src">
					<xsl:value-of select="$themePath"/>
					<xsl:text>lib/tjpzoom/tjpzoom_config_default.js</xsl:text>
				</xsl:attribute>&#160; </script>
		
			<title>
				<xsl:choose>
					<xsl:when test="not($page_title)">
						<xsl:text>  </xsl:text>
					</xsl:when>
					<xsl:otherwise>
						<xsl:copy-of select="$page_title/node()"/>
					</xsl:otherwise>
				</xsl:choose>
			</title>
		</head>
	</xsl:template>
	

	<!-- 
        From: structural.xsl
        
        Changes: 
            - Override the trail to remove hte first link to the DSpace home page
        
        Original comments:
        The trail is built one link at a time. Each link is given the ds-trail-link class, with the first and
        the last links given an additional descriptor. 
    -->
	<xsl:template match="dri:trail">

		<xsl:if test=" position() &gt; $config-numBreadcrumbsToHide ">
			<li>
				<xsl:attribute name="class">
					<xsl:text>ds-trail-link </xsl:text>
					<xsl:if test="position()=1">
						<xsl:text>first-link</xsl:text>
					</xsl:if>
					<xsl:if test="position()=last()">
						<xsl:text>last-link</xsl:text>
					</xsl:if>
				</xsl:attribute>
				<!-- Determine whether we are dealing with a link or plain text trail link -->
				<xsl:choose>
					<xsl:when test="./@target">
						<a>
							<xsl:attribute name="href">
								<xsl:value-of select="./@target"/>
							</xsl:attribute>
							<xsl:apply-templates/>
						</a>
					</xsl:when>
					<xsl:otherwise>
						<xsl:apply-templates/>
					</xsl:otherwise>
				</xsl:choose>
			</li>
		</xsl:if>

	</xsl:template>


	<!-- 
        From: DIM-Handler.xsl
        
        Changes:
                1. reversed position of thumbnail and metadata
       
       Original comments:       
            An item rendered in the summaryList pattern. Commonly encountered in various browse-by pages
            and search results. 
    -->
	<xsl:template name="itemSummaryList-DIM">

		<!-- Generate the thunbnail, if present, from the file section -->
		<xsl:apply-templates select="./mets:fileSec" mode="artifact-preview"/>

		<!-- Generate the info about the item from the metadata section -->
		<xsl:apply-templates
			select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim"
			mode="itemSummaryList-DIM"/>
	</xsl:template>


	<!-- 
        From: General-Handler.xsl        
        
        Changes: 
         	1. moved thumbnail to another rule
        
        Generate the thunbnail, if present, from the file section -->
	<xsl:template match="mets:fileSec" mode="artifact-preview">
		<!--
			Thumbnail moved to another rule
		<xsl:if test="mets:fileGrp[@USE='THUMBNAIL']">
			<div class="artifact-preview">
				<xsl:variable name="thumbnailUrl"
					select="mets:fileGrp[@USE='THUMBNAIL']/mets:file/mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
				<a href="{ancestor::mets:METS/@OBJID}">
					<img alt="Thumbnail" class="thumbnail">
						<xsl:attribute name="src">
							<xsl:value-of
								select="mets:fileGrp[@USE='THUMBNAIL']/mets:file/mets:FLocat[@LOCTYPE='URL']/@xlink:href"
							/>
						</xsl:attribute>
					</img>
				</a>
			</div>
		</xsl:if>
		-->
	</xsl:template>

	<!-- 
        From DIM-Handler.xsl
        Changes:
                1. rewrote/reordered to use the Fancybox JQuery library
                
        Generate the info about the item from the metadata section 
    -->
	<xsl:template match="dim:dim" mode="itemSummaryList-DIM">
		<xsl:variable name="itemWithdrawn" select="@withdrawn"/>

		<!-- generate an id and use it for the JS popups -->
		<xsl:variable name="itemid" select="generate-id(node())"/>

		<script type="text/javascript"> itemids.push("<xsl:value-of select="$itemid"/>"); </script>
	
		<!-- FancyBox link on image: opens popup -->
		<a>
			<xsl:attribute name="id">
				<xsl:text>image</xsl:text>
				<xsl:value-of select="$itemid"/>
			</xsl:attribute>
			<xsl:attribute name="href">
				<xsl:text>#</xsl:text>
				<xsl:value-of select="$itemid"/>
			</xsl:attribute>
			<xsl:choose>
				<xsl:when test="//mets:fileGrp[@USE='THUMBNAIL']">
				<div class="artifact-preview">
					<xsl:variable name="thumbnailUrl"
						select="//mets:fileGrp[@USE='THUMBNAIL']/mets:file/mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
					
					<img alt="Thumbnail" class="thumbnail">
						<xsl:attribute name="src">
							<xsl:value-of
								select="//mets:fileGrp[@USE='THUMBNAIL']/mets:file/mets:FLocat[@LOCTYPE='URL']/@xlink:href"
							/>
						</xsl:attribute>
					</img>
				</div>
				</xsl:when>
				<xsl:otherwise>
					<div class="artifact-preview">
						<img alt="Thumbnail" class="thumbnail">
							<xsl:attribute name="src">
								<xsl:value-of select="$themePath"/>
								<xsl:text>lib/nothumbnail.png</xsl:text>
							</xsl:attribute>
						</img>
					</div>
					
				</xsl:otherwise>
			</xsl:choose></a>
		
		
		<!-- item title -->
		<p class="ds-artifact-title">
			<xsl:variable name="artifactTitle">
				<xsl:value-of select="dim:field[@element='title'][1]/node()"/>
			</xsl:variable>
			<xsl:choose>
				<xsl:when test="dim:field[@element='title']">
					<xsl:choose>
						<xsl:when test="string-length($artifactTitle) >= 30">
							<xsl:value-of select="substring($artifactTitle,1,30)"/>... </xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="$artifactTitle"/>
						</xsl:otherwise>
					</xsl:choose>
					<!--<xsl:value-of select="dim:field[@element='title'][1]/node()"/>-->
				</xsl:when>
				<xsl:otherwise>
					<i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
				</xsl:otherwise>
			</xsl:choose>
		</p>
					
		
		<!-- Fancy box link on image -->
		<a>
			<xsl:attribute name="id">
				<xsl:text>anchor</xsl:text>
				<xsl:value-of select="$itemid"/>
			</xsl:attribute>
			<xsl:attribute name="href">
				<xsl:text>#</xsl:text>
				<xsl:value-of select="$itemid"/>
			</xsl:attribute>
			Show Details</a>

		<!-- FancyBox popup content-->
		<div style="display:none">
			<xsl:attribute name="id">
				<xsl:value-of select="$itemid"/>
			</xsl:attribute>

			<!-- title -->
			<h3 class="detail-title">
				<xsl:choose>
					<xsl:when test="dim:field[@element='title']">
						<xsl:value-of select="dim:field[@element='title'][1]/node()"/>
					</xsl:when>
					<xsl:otherwise>
						<i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
					</xsl:otherwise>
				</xsl:choose>
			</h3>

			<!-- author -->
			<p class="detail-author">
				<!-- thumbnail is a link-->
				<xsl:element name="a">
					<xsl:attribute name="href">
						<xsl:choose>
							<xsl:when test="$itemWithdrawn">
								<xsl:value-of select="ancestor::mets:METS/@OBJEDIT"/>
							</xsl:when>
							<xsl:otherwise>
								<xsl:value-of select="ancestor::mets:METS/@OBJID"/>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:attribute>
							
					<xsl:choose>
						<xsl:when test="//mets:fileGrp[@USE='THUMBNAIL']">
								<xsl:variable name="thumbnailUrl"
									select="//mets:fileGrp[@USE='THUMBNAIL']/mets:file/mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>						
							<img alt="Thumbnail"  class="detail" align="right">
									<xsl:attribute name="src">
										<xsl:value-of
											select="$thumbnailUrl"
										/>
									</xsl:attribute>
								</img>
						</xsl:when>
						<xsl:otherwise>
								<img alt="Thumbnail" class="detail" align="right">
									<xsl:attribute name="src">
										<xsl:value-of select="$themePath"/>
										<xsl:text>lib/nothumbnail.png</xsl:text>
									</xsl:attribute>
								</img>						
						</xsl:otherwise>
					</xsl:choose>
					
				</xsl:element>

				<strong>Author:</strong>
				<xsl:choose>
					<xsl:when test="dim:field[@element='contributor'][@qualifier='author']">
						<xsl:for-each
							select="dim:field[@element='contributor'][@qualifier='author']">
							<xsl:copy-of select="./node()"/>
							<xsl:if
								test="count(following-sibling::dim:field[@element='contributor'][@qualifier='author']) != 0">
								<xsl:text>; </xsl:text>
							</xsl:if>
						</xsl:for-each>
					</xsl:when>
					<xsl:when test="dim:field[@element='creator']">
						<xsl:for-each select="dim:field[@element='creator']">
							<xsl:copy-of select="node()"/>
							<xsl:if
								test="count(following-sibling::dim:field[@element='creator']) != 0">
								<xsl:text>; </xsl:text>
							</xsl:if>
						</xsl:for-each>
					</xsl:when>
					<xsl:when test="dim:field[@element='contributor']">
						<xsl:for-each select="dim:field[@element='contributor']">
							<xsl:copy-of select="node()"/>
							<xsl:if
								test="count(following-sibling::dim:field[@element='contributor']) != 0">
								<xsl:text>; </xsl:text>
							</xsl:if>
						</xsl:for-each>
					</xsl:when>
					<xsl:otherwise>
						<i18n:text>xmlui.dri2xhtml.METS-1.0.no-author</i18n:text>
					</xsl:otherwise>
				</xsl:choose>
			</p>

			<p class="detail-date">
				<strong>Publication date:</strong>
				<xsl:if test="dim:field[@element='publisher']">
					<span class="publisher">
						<xsl:copy-of select="dim:field[@element='publisher']/node()"/>
					</span>
					<xsl:text>, </xsl:text>
				</xsl:if>
				<span class="date">
					<xsl:value-of
						select="substring(dim:field[@element='date' and @qualifier='issued']/node(),1,10)"
					/>
				</span>
			</p>

			<p class="detail-desc">
				<strong>Description:</strong>
				<xsl:if test="dim:field[@element='description']">
					<span class="description">
						<xsl:copy-of
							select="dim:field[@element='description'][not(@qualifier)]/node()"/>
					</span>
					<xsl:text>, </xsl:text>
				</xsl:if>
			</p>
			
			<p class="detail-link"><a  href="{ancestor::mets:METS/@OBJID}">Go To Image</a></p>

		</div>

	</xsl:template>

	<!-- 
        From structural.xsl
        
        Changes:
             1. Added a 'clearing' element 
        
        Summarylist case.  This template used to apply templates to the "pioneer" object (the first object
        in the set) and let it figure out what to do. This is no longer the case, as everything has been 
        moved to the list model. A special theme, called TableTheme, has beeen created for the purpose of 
        preserving the pioneer model. -->
	<xsl:template match="dri:referenceSet[@type = 'summaryList']" priority="2">
		<xsl:apply-templates select="dri:head"/>
		<!-- Here we decide whether we have a hierarchical list or a flat one -->
		<xsl:choose>
			<xsl:when
				test="descendant-or-self::dri:referenceSet/@rend='hierarchy' or ancestor::dri:referenceSet/@rend='hierarchy'">
				<ul>
					<xsl:apply-templates select="*[not(name()='head')]" mode="summaryList"/>
				</ul>
			</xsl:when>
			<xsl:otherwise>

				<ul class="ds-artifact-list">
					<xsl:apply-templates select="*[not(name()='head')]" mode="summaryList"/>
				</ul>

				<!-- 1. important: need to clear after floating list-->
				<div style="clear:both;">
					<p> </p>
				</div>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!-- 
        From: structural.xsl
        
        Changes:
              1. modified to not use HTML list item: <li>
        
        Then we resolve the reference tag to an external mets object -->
	<xsl:template match="dri:reference" mode="summaryList">

		<xsl:variable name="externalMetadataURL">
			<xsl:text>cocoon:/</xsl:text>
			<xsl:value-of select="@url"/>
			<!-- Since this is a summary only grab the descriptive metadata, and the thumbnails -->
			<xsl:text>?sections=dmdSec,fileSec&amp;fileGrpTypes=THUMBNAIL</xsl:text>
			<!-- An example of requesting a specific metadata standard (MODS and QDC crosswalks only work for items)->
                <xsl:if test="@type='DSpace Item'">
                <xsl:text>&amp;dmdTypes=DC</xsl:text>
                </xsl:if>-->
		</xsl:variable>
		<xsl:comment> External Metadata URL: <xsl:value-of select="$externalMetadataURL"/>
		</xsl:comment>
		<li>
			<xsl:attribute name="class">
				<xsl:text>ds-artifact-item </xsl:text>
				<xsl:choose>
					<xsl:when test="position() mod 2 = 0">even</xsl:when>
					<xsl:otherwise>odd</xsl:otherwise>
				</xsl:choose>
			</xsl:attribute>
			<xsl:apply-templates select="document($externalMetadataURL)" mode="summaryList"/>
			<xsl:apply-templates/>
		</li>
	</xsl:template>


	<!-- 
        From: DIM-Handler.xsl
        
        Changes:
            1. add in -line image viewing
            2. reordered elements
            
        An item rendered in the summaryView pattern. This is the default way to view a DSpace item in Manakin. -->

	<xsl:template name="itemSummaryView-DIM">
		
		<script type="text/javascript">
			var o;
		<xsl:for-each select="//mets:fileGrp[@USE='CONTENT']/mets:file[@MIMETYPE='image/jpeg']">
			o = new Object();
			o.url = "<xsl:value-of select="mets:FLocat/@xlink:href"/>";
			o.size = <xsl:value-of select="./@SIZE"/>;
			o.title = "<xsl:value-of select="mets:FLocat/@xlink:title"/>";
			imageJpegArray.push(o);
		</xsl:for-each>
		</script>

		<!-- TJPZoom: the zoomable image  viewer -->
		<div id="image-zoom-panel">
			<!-- Moved this into Javascript: see gallery.js
				left this here just in case issues were found and needed to revert -->
			<!--
				<img alt="zoomable image" onmouseover="TJPzoom(this);" width="500">
				<xsl:attribute name="src">
				<xsl:value-of select="$serviceImageUrl"/>
				</xsl:attribute>
				</img>
			-->
			&#160; 
		</div>

		<!-- Generate the info about the item from the metadata section -->
		<xsl:apply-templates
			select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim"
			mode="itemSummaryView-DIM"/>

		<!-- Generate the bitstream information from the file section -->
		<xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='CONTENT']">
			<xsl:with-param name="context" select="."/>
			<xsl:with-param name="primaryBitream"
				select="./mets:structMap[@TYPE='LOGICAL']/mets:div[@TYPE='DSpace Item']/mets:fptr/@FILEID"
			/>
		</xsl:apply-templates>

		<!-- Generate the license information from the file section -->
		<xsl:apply-templates
			select="./mets:fileSec/mets:fileGrp[@USE='CC-LICENSE' or @USE='LICENSE']"/>
	</xsl:template>

	<!-- 
        From: General-Handler.xsl
        
        Changes: 
            none
        
        Original notes:
            Generate the bitstream information from the file section -->
	<xsl:template match="mets:fileGrp[@USE='CONTENT']">
		<xsl:param name="context"/>
		<xsl:param name="primaryBitream" select="-1"/>

		<h2>
			<i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-head</i18n:text>
		</h2>

		<table class="ds-table file-list">
			<tr class="ds-table-header-row">
				<th>
					<i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-file</i18n:text>
				</th>
				<th>
					<i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-size</i18n:text>
				</th>
				<th>
					<i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-format</i18n:text>
				</th>
				<th>
					<i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-view</i18n:text>
				</th>
			</tr>
			<xsl:choose>
				<!-- If one exists and it's of text/html MIME type, only display the primary bitstream -->
				<xsl:when test="mets:file[@ID=$primaryBitream]/@MIMETYPE='text/html'">
					<xsl:apply-templates select="mets:file[@ID=$primaryBitream]">
						<xsl:with-param name="context" select="$context"/>
					</xsl:apply-templates>
				</xsl:when>
				<!-- Otherwise, iterate over and display all of them -->
				<xsl:otherwise>
					<xsl:apply-templates select="mets:file">
						<xsl:sort select="mets:FLocat[@LOCTYPE='URL']/@xlink:title"/>
						<xsl:with-param name="context" select="$context"/>
					</xsl:apply-templates>
				</xsl:otherwise>
			</xsl:choose>
		</table>
	</xsl:template>


	<!-- 
        From structural.xsl
        
        Changes:
              1. removed the global navigation list
              2. builds local nav list to remove double nesting of HTML lists
        
        The template that applies to lists directly under the options tag that have other lists underneath 
        them. Each list underneath the matched one becomes an option-set and is handled by the appropriate 
        list templates. -->
	<xsl:template match="dri:options/dri:list[dri:list]" priority="4">
		<!-- <xsl:apply-templates select="dri:head"/> -->
		<div>

			<xsl:call-template name="standardAttributes">
				<xsl:with-param name="class">ds-option-set</xsl:with-param>
			</xsl:call-template>

			<!-- restrict or show options based on config option -->
			<xsl:choose>
				<xsl:when test="$config-limitNavOptionsToCollection = 'true'">

					<!-- build the navigation list ourselves to make it simple -->
					<h3>Browse collection:</h3>
					<!-- TODO: should be an i18n reference and not hardcoded text -->
					<ul>
						<xsl:apply-templates select="dri:list[@n='context']/dri:item" mode="nested"
						/>
					</ul>
				</xsl:when>
				<xsl:otherwise>

					<ul class="ds-options-list">
						<xsl:apply-templates select="*[not(name()='head')]" mode="nested"/>
					</ul>

				</xsl:otherwise>
			</xsl:choose>

		</div>

	</xsl:template>


	<!-- 
        From: structural.xsl
        
        Changes:
             1. made search local to collection by changing the action
             2. Removed advanced search box
        
        Original notes:
        
        The template to handle dri:options. Since it contains only dri:list tags (which carry the actual
        information), the only things than need to be done is creating the ds-options div and applying 
        the templates inside it. 
        
        In fact, the only bit of real work this template does is add the search box, which has to be 
        handled specially in that it is not actually included in the options div, and is instead built 
        from metadata available under pageMeta.
    -->
	<!-- TODO: figure out why i18n tags break the go button -->
	<xsl:template match="dri:options">
		<div id="ds-options">
			<h3 id="ds-search-option-head" class="ds-option-set-head">
				<i18n:text>xmlui.ArtifactBrowser.Navigation.head_this_collection</i18n:text>
			</h3>
			<!-- TODO: not the best  i18n reference: misused a bit -->
			<div id="ds-search-option" class="ds-option-set">
				<!-- The form, complete with a text box and a button, all built from attributes referenced
                from under pageMeta. -->


				<!-- TODO: this is frankly a huge hack and should be rewritten.
                    Currently, it develops the seach box target/action by lookign at the 
                    pageMetadata URL, which causes problems when there are repeated
                    searches, as the path '/search' keeps getting aggregated on the end 
                    as in: '/search/serach' etc.
                    
                    the better path forward here is to include the aspect in this chain that 
                    develops the collection search box, but I'm not sure where that is or
                    how to include it.
                -->

				<!-- develop the actionPath from page metadata -->
				<xsl:variable name="actionPath">
					<xsl:value-of
						select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath']"/>
					<xsl:text>/</xsl:text>
					<xsl:value-of
						select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='URI']"/>
					<xsl:text>/search</xsl:text>
				</xsl:variable>

				<form id="ds-search-form" method="post">

					<!-- now we have to guard against repeated '/search' paths on the end of the forms action URL -->
					<xsl:choose>
						<xsl:when test="substring-after($actionPath,'/search')='/search'">
							<xsl:attribute name="action">
								<xsl:value-of
									select="substring($actionPath,0, string-length($actionPath)-6 )"
								/>
							</xsl:attribute>
						</xsl:when>
						<xsl:otherwise>
							<xsl:attribute name="action">
								<xsl:value-of select="$actionPath"/>
							</xsl:attribute>
						</xsl:otherwise>
					</xsl:choose>

					<!-- end hack -->

			<!--
                     Changed to make search local to collection
                    <xsl:attribute name="action">
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='search'][@qualifier='simpleURL']"/>
                    </xsl:attribute>
                    -->

					<fieldset>
						<input class="ds-text-field " type="text">
							<xsl:attribute name="name">
								<xsl:value-of
									select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='search'][@qualifier='queryField']"
								/>
							</xsl:attribute>
						</input>
						<input class="ds-button-field " name="submit" type="submit"
							i18n:attr="value" value="xmlui.general.go">
							<xsl:attribute name="onclick">
								<xsl:text>
                                    var radio = document.getElementById(&quot;ds-search-form-scope-container&quot;);
                                    if (radio != undefined &amp;&amp; radio.checked)
                                    {
                                    var form = document.getElementById(&quot;ds-search-form&quot;);
                                    form.action=
                                </xsl:text>
								<xsl:text>&quot;</xsl:text>
								<xsl:value-of
									select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath']"/>
								<xsl:text>/handle/&quot; + radio.value + &quot;/search&quot; ; </xsl:text>
								<xsl:text>
                                    } 
                                </xsl:text>
							</xsl:attribute>
						</input>
						<xsl:if
							test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='focus'][@qualifier='container']">
							<label>
								<input id="ds-search-form-scope-all" type="radio" name="scope"
									value="" checked="checked"/>
								<i18n:text>xmlui.dri2xhtml.structural.search</i18n:text>
							</label>
							<br/>
							<label>
								<input id="ds-search-form-scope-container" type="radio" name="scope">
									<xsl:attribute name="value">
										<xsl:value-of
											select="substring-after(/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='focus'][@qualifier='container'],':')"
										/>
									</xsl:attribute>
								</input>
								<xsl:choose>
									<xsl:when
										test="/dri:document/dri:body//dri:div/dri:referenceSet[@type='detailView' and @n='collection-view']"
										>This Collection</xsl:when>
									<xsl:when
										test="/dri:document/dri:body//dri:div/dri:referenceSet[@type='detailView' and @n='community-view']"
										>This Community</xsl:when>
								</xsl:choose>
							</label>
						</xsl:if>
					</fieldset>
				</form>
				<!-- The "Advanced search" link, to be perched underneath the search box -->
				<!--
                <a>
                    <xsl:attribute name="href">
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='search'][@qualifier='advancedURL']"/>
                    </xsl:attribute>
                    <i18n:text>xmlui.dri2xhtml.structural.search-advanced</i18n:text>
                </a>
                -->
			</div>

			<!-- Once the search box is built, the other parts of the options are added -->
			<xsl:apply-templates/>
		</div>
	</xsl:template>

	<!-- 
        From: structural.xsl
        
        Changes:
            1. Rewrote footer, incl adding a link to a page with info about this theme

      Original notes:
      Like the header, the footer contains various miscellanious text, links, and image placeholders 
    -->
	<xsl:template name="buildFooter">
		<div id="ds-footer">
			<div id="ds-footer-links">

				<!-- only show if config option is set -->
				<xsl:if test="$config-showAboutLink = 'true'">
					<a id="about">
						<xsl:attribute name="href"><xsl:value-of select="$themePath"/>about.txt</xsl:attribute>
						About this theme
					</a>
					<xsl:text> | </xsl:text>
				</xsl:if>

				<a>
					<xsl:attribute name="href">
						<xsl:value-of
							select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
						<xsl:text>/contact</xsl:text>
					</xsl:attribute>
					<i18n:text>xmlui.dri2xhtml.structural.contact-link</i18n:text>
				</a>

				<xsl:text> | </xsl:text>

				<a>
					<xsl:attribute name="href">
						<xsl:value-of
							select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
						<xsl:text>/feedback</xsl:text>
					</xsl:attribute>
					<i18n:text>xmlui.dri2xhtml.structural.feedback-link</i18n:text>
				</a>
			</div>
		</div>
	</xsl:template>

</xsl:stylesheet>
