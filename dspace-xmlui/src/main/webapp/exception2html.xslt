<?xml version="1.0" encoding="UTF-8"?>
<!-- The contents of this file are subject to the license and copyright detailed 
	in the LICENSE and NOTICE files at the root of the source tree and available 
	online at http://www.dspace.org/license/ -->

<!-- This stylsheet to handle exception display is a modified version of 
	the base apache cocoon stylesheet, this is still under the Apache license. 
	The original author is unknown. Scott Phillips adapted it for Manakin's need. 
    modified for LINDAT/CLARIN
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
    xmlns:dc="http://purl.org/dc/elements/1.1/"
	xmlns:confman="org.dspace.core.ConfigurationManager"
    xmlns="http://www.w3.org/1999/xhtml"
	xmlns:ex="http://apache.org/cocoon/exception/1.0" >

	<xsl:include href="themes/UFAL/lib/xsl/lindat/header.xsl" />
	<xsl:include href="themes/UFAL/lib/xsl/lindat/footer.xsl" />

	<xsl:param name="realPath" />

	<xsl:param name="contextPath" />

    <xsl:variable name="aaiURL">
        <xsl:value-of select="confman:getProperty('lr', 'lr.aai.url')"/>
    </xsl:variable>

	<xsl:variable name="request-uri"
		select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='URI']" />

	<!-- let sitemap override default page title -->
	<xsl:param name="pageTitle">
		An error has occurred
	</xsl:param>

	<!-- let sitemap override default context path -->

	<xsl:template match="ex:exception-report">
		<html>

			<xsl:call-template name="buildHead" />

			<body id="lindat-repository">
				<xsl:call-template name="buildHeader" />
				<div class="container">
					<div class="contents">
						<div class="container-fluid contents">

							<div class="row" style="padding: 20px;">
							
								<div class="alert alert-error">
									<h3>
										<xsl:value-of select="$pageTitle" />
									</h3>
									
									<fieldset xmlns:i18n="http://apache.org/cocoon/i18n/2.1" xmlns="http://di.tamu.edu/DRI/1.0/" class="wordbreak">
										<ol class="unstyled">
											<li class="control-group">
												<div>
													<h5>
													Please contact the
													<a target="_blank">
														<xsl:attribute name="href">
                                    						<xsl:value-of select="$contextPath" />
                                    						<xsl:text>/contact</xsl:text>
                                						</xsl:attribute>
														site administrator with details below.
													</a>
													</h5>
												</div>
											</li>
											<li>
												<xsl:apply-templates select="ex:message" mode="breakLines" />
												<xsl:if test="ex:location">
													<br />
													<span style="font-weight: normal">
														<xsl:apply-templates select="ex:location" />
													</span>
												</xsl:if>
											</li>
											
											<li class="control-group fa fa-warning fa-5x hangright">
												&#160;
											</li>
										</ol>
									</fieldset>									
								</div>
								<div class="alert alert-error">

									<div class="accordion-group">
										<div class="accordion-heading">
											<a data-toggle="collapse" class="accordion-toggle" href="#cocoon-stacktrace">
												<span class="label label-important">Cocoon stacktrace</span>
											</a>
										</div>
										<div id="cocoon-stacktrace" class="accordion-body collapse">
	
											<div id="locations">
												<xsl:for-each select="ex:cocoon-stacktrace/ex:exception">
													<xsl:sort select="position()" order="descending" />
													<strong>
														<xsl:apply-templates select="ex:message"
															mode="breakLines" />
													</strong>
													<div>
														<xsl:for-each select="ex:locations/*[string(.) != '[cause location]']">
																	<xsl:call-template name="print-location" />
																	<em>
																		<xsl:value-of select="." />
																	</em>
																	<br />
														</xsl:for-each>
													</div>
												</xsl:for-each>
											</div>
											
										</div>
									</div>
								</div>
								<div class="alert alert-error">

									<div class="accordion-group">
										<div class="accordion-heading">
											<a data-toggle="collapse" class="accordion-toggle" href="#java-stacktrace">
												<span class="label label-important">JAVA stacktrace</span>
											</a>
										</div>
										<div id="java-stacktrace" class="accordion-body collapse">
											<xsl:apply-templates select="ex:stacktrace" />
										</div>
									</div>								
								</div>
							</div>
						</div>

					</div>
				</div>

				<xsl:call-template name="buildFooter" />
			</body>
		</html>
	</xsl:template>

	<xsl:template match="ex:stacktrace|ex:full-stacktrace">
		<p class="stacktrace">
			<pre class="alert alert-error">
				<xsl:value-of select="translate(.,'&#13;','')" />
			</pre>
		</p>
	</xsl:template>

	<xsl:template match="ex:location">
		<xsl:if test="string-length(.) > 0">
			<em>
				<xsl:value-of select="." />
			</em>
			<xsl:text> - </xsl:text>
		</xsl:if>
		<xsl:call-template name="print-location" />
	</xsl:template>

	<xsl:template name="print-location">
		<xsl:choose>
			<xsl:when test="contains(@uri, $realPath)">
				<xsl:text>context:/</xsl:text>
				<xsl:value-of select="substring-after(@uri, $realPath)" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="@uri" />
			</xsl:otherwise>
		</xsl:choose>
		<xsl:text> - </xsl:text>
		<xsl:value-of select="@line" />
		:
		<xsl:value-of select="@column" />
	</xsl:template>

	<!-- output a text by splitting it with <br>s on newlines can be uses either 
		by an explicit call or with <apply-templates mode="breakLines"/> -->
	<xsl:template match="node()" mode="breakLines" name="breakLines">
		<xsl:param name="text" select="string(.)" />
		<xsl:choose>
			<xsl:when test="contains($text, '&#10;')">
				<xsl:value-of select="substring-before($text, '&#10;')" />
				<br />
				<xsl:call-template name="breakLines">
					<xsl:with-param name="text"
						select="substring-after($text, '&#10;')" />
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$text" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template name="buildHead">
		<head>
			<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />

			<!-- Always force latest IE rendering engine (even in intranet) & Chrome 
				Frame -->
			<meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1" />

			<!-- Mobile Viewport Fix j.mp/mobileviewport & davidbcalhoun.com/2010/viewport-metatag 
				device-width : Occupy full width of the screen in its current orientation 
				initial-scale = 1.0 retains dimensions instead of zooming out if page height 
				> device height maximum-scale = 1.0 retains dimensions instead of zooming 
				in if page width < device width -->
			<meta name="viewport"
				content="width=device-width, initial-scale=1.0, maximum-scale=1.0" />

			<link rel="shortcut icon">
				<xsl:attribute name="href">
                        <xsl:value-of select="$contextPath" />
                    <xsl:text>/themes/UFAL/images/favicon.ico</xsl:text>
                </xsl:attribute>
			</link>
			<link rel="apple-touch-icon">
				<xsl:attribute name="href">
                        <xsl:value-of select="$contextPath" />
                    <xsl:text>/themes/UFAL/images/apple-touch-icon.png</xsl:text>
                </xsl:attribute>
			</link>

			<meta name="Generator">
				<xsl:attribute name="content">
                <xsl:text>DSpace 1.8.2</xsl:text>
              </xsl:attribute>
			</meta>
			
	        <xsl:variable name="jqueryVersion">
	            <xsl:text>1.7</xsl:text>
	        </xsl:variable>
	
	        <xsl:variable name="protocol">
	            <xsl:choose>
	                <xsl:when test="starts-with(confman:getProperty('dspace.baseUrl'), 'https://')">
	                    <xsl:text>https://</xsl:text>
	                </xsl:when>
	                <xsl:otherwise>
	                <!-- UFAL #303 - dspace thinks after redirect that it is still in http -->
	                    <xsl:text>https://</xsl:text>
	                </xsl:otherwise>
	            </xsl:choose>
	        </xsl:variable>
	        <script type="text/javascript" src="{concat($protocol, 'ajax.googleapis.com/ajax/libs/jquery/', $jqueryVersion ,'/jquery.min.js')}">&#160;</script>
	        <script type="text/javascript" src="{$contextPath}/themes/UFAL/lib/js/jquery-ui.js">&#160;</script>
			<script type="text/javascript" src="{$theme-path}/lib/js/jquery.i18n.js">&#160;</script>

            <script type="text/javascript" src="{concat($aaiURL, '/discojuice/discojuice-2.1.en.min.js')}">&#160;</script>
            <script type="text/javascript" src="{concat($aaiURL, '/aai.js')}">&#160;</script>
	
	        <xsl:variable name="localJQuerySrc">
	            <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
	            <xsl:text>/static/js/jquery-</xsl:text>
	            <xsl:value-of select="$jqueryVersion"/>
	            <xsl:text>.min.js</xsl:text>
	        </xsl:variable>
	
			<script type="text/javascript">
				<xsl:text disable-output-escaping="yes">!window.jQuery &amp;&amp; document.write('&lt;script type="text/javascript" src="</xsl:text>
				<xsl:value-of select="$localJQuerySrc" />
				<xsl:text disable-output-escaping="yes">"&gt;&#160;&lt;\/script&gt;')</xsl:text>
			</script>

			
			
			<!-- Bootstrap stylesheets -->
			<link rel="stylesheet" href="{$contextPath}/themes/UFAL/lib/bootstrap/css/bootstrap.min.css" />
            <link rel="stylesheet" href="{$contextPath}/themes/UFAL/lib/bootstrap/css/font-awesome.min.css" />
            <link rel="stylesheet" href="{$contextPath}/themes/UFAL/lib/bootstrap/css/ufal-theme.css" />
			
			<!-- Add Lindat stylesheet -->
			<link rel="stylesheet" type="text/css"
				href="{$contextPath}/themes/UFAL/lib/lindat/public/css/lindat.css" />


			<script type="text/javascript" src="{$contextPath}/themes/UFAL/lib/bootstrap/js/bootstrap.min.js">&#160;</script>
			<script type="text/javascript" src="{$contextPath}/themes/UFAL/lib/bootstrap/js/ufal.min.js">&#160;</script>

			<!-- Add the title in -->
			<title>
				<xsl:value-of select="$pageTitle" />
			</title>
		</head>
	</xsl:template>


</xsl:stylesheet>
