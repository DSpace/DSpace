<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<!-- THEME CONFIGURATION OPTIONS -->
	
	<!-- using these 2 options, you can restrict navigation to this collection, 
		removing links to outside colelctions, communities, etc -->
	
	<!-- number of breadcrumbs to hide -->
	<xsl:variable name="config-numBreadcrumbsToHide">
		<xsl:text>1</xsl:text>
	</xsl:variable>
	
	<!-- removes the naviagtion panel that offers repository/global navigation -->
	<xsl:variable name="config-limitNavOptionsToCollection">
		<xsl:text>true</xsl:text>
	</xsl:variable>
	
	<!-- shows for hides the "About this theme" link -->
	<xsl:variable name="config-showAboutLink">
		<xsl:text>true</xsl:text>
	</xsl:variable>
	
	<!-- the zoom factor: how much do we want to be able to zoom in -->
	<xsl:variable name="config-zoomFactor">
		<xsl:text>5</xsl:text>
	</xsl:variable>
	
	<!-- the width of the zoom panel -->
	<xsl:variable name="config-zoomPanelWidth">
		<xsl:text>600</xsl:text>
	</xsl:variable>
	
	<!-- max size in bytes of the service/web-display image: the JPG image
	CLOSEST in size but LESS THAN this value will be displayed as a zoomable image-->
	<xsl:variable name="config-maxServiceImageSize">
		<xsl:text>1048576</xsl:text>
	</xsl:variable>


</xsl:stylesheet>
