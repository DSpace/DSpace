<xsl:stylesheet xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
	xmlns:dri="http://di.tamu.edu/DRI/1.0/" xmlns:mets="http://www.loc.gov/METS/"
	xmlns:xlink="http://www.w3.org/TR/xlink/" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="1.0" xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
	xmlns:xhtml="http://www.w3.org/1999/xhtml" xmlns:mods="http://www.loc.gov/mods/v3"
	xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:confman="org.dspace.core.ConfigurationManager"
	xmlns="http://www.w3.org/1999/xhtml" exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc confman">

	<!-- Display language selection if more than 1 language is supported -->
	<xsl:template name="languageSelection">
		<xsl:variable name="currentLocale"
			select="//dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='page'][@qualifier='currentLocale']" />
		<a class="dropdown-toggle text-uppercase" id="dropdownMenuLocale"
			data-toggle="dropdown">
			<xsl:value-of select="$currentLocale" />
			<span class="caret"></span>
		</a>
		<ul class="dropdown-menu" role="menu" aria-labelledby="dropdownMenuLocale">
			<xsl:for-each
				select="//dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='page'][@qualifier='supportedLocale']">
				<xsl:variable name="locale" select="." />
				<li role="presentation">
					<xsl:if test="$locale = $currentLocale">
						<xsl:attribute name="class">active</xsl:attribute>
					</xsl:if>
					<xsl:call-template name="build-anchor">
						<xsl:with-param name="a.href">
							<xsl:value-of select="$current-uri" />
							<xsl:text>?locale-attribute=</xsl:text>
							<xsl:value-of select="$locale" />
						</xsl:with-param>
						<xsl:with-param name="a.value">
							<xsl:value-of
								select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='supportedLocale'][@qualifier=$locale]" />
						</xsl:with-param>
					</xsl:call-template>
				</li>
			</xsl:for-each>

		</ul>

	</xsl:template>

	<!-- The header (distinct from the HTML head element) contains the title, 
		subtitle, login box and various placeholders for header images -->
	<xsl:template name="buildTopSidebar">
		<div class="row">
			<nav class="navbar navbar-inverse" role="navigation">
				<div class="container-fluid">
					<div class="navbar-header" id="navbar-brand-dspace">
				      <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#bs-example-navbar-collapse-1">
				        <span class="sr-only">Toggle navigation</span>
				        <span class="icon-bar"></span>
				        <span class="icon-bar"></span>
				        <span class="icon-bar"></span>
				      </button>
				      <a class="navbar-brand" href="#">
					      <xsl:call-template name="build-img">
					      	<xsl:with-param name="img.src">images/dspace-logo-only.png</xsl:with-param>
					      	<xsl:with-param name="img.alt">DSpace</xsl:with-param>
					      </xsl:call-template>
					  </a>
				    </div>
					<!-- Collect the nav links, forms, and other content for toggling -->
 					<div id="bs-example-navbar-collapse-1" class="collapse navbar-collapse" >
						<ul class="nav navbar-nav">
<!-- 							<li class="link-ba"> -->
<!-- 							    <xsl:call-template name="build-anchor"> -->
<!-- 									<xsl:with-param name="a.href">http://www.gba.gob.ar</xsl:with-param> -->
<!-- 									<xsl:with-param name="img.src">images/header_ba-10.png</xsl:with-param> -->
<!-- 									<xsl:with-param name="img.alt">BA</xsl:with-param> -->
<!-- 								</xsl:call-template> -->
<!-- 							</li> -->
							<li>
								<xsl:call-template name="build-anchor">
									<xsl:with-param name="a.href">/</xsl:with-param>
									<xsl:with-param name="a.value">
										<i18n:text>xmlui.general.dspace_home</i18n:text>
									</xsl:with-param>
								</xsl:call-template>
							</li>
							<xsl:for-each select="/dri:document/dri:options/dri:list[@n='browse']">
								<xsl:call-template  name="buildMenuItemAsList"   />
							</xsl:for-each>
							
							<li>
								<xsl:call-template name="build-anchor">
									<xsl:with-param name="a.href">/submissions</xsl:with-param>
									<xsl:with-param name="a.value">
										<i18n:text>xmlui.cicdigital.home.aportar-material</i18n:text>
									</xsl:with-param>
								</xsl:call-template>
							</li>
							
							<li class="dropdown">
								<a href="#" class="dropdown-toggle" data-toggle="dropdown"
									role="button" aria-expanded="false">
									<i18n:text>xmlui.cicdigital.home.mas-informacion</i18n:text>
									<span class="caret"></span>
								</a>
								<ul class="dropdown-menu" role="menu">
									<li class="dropdown-header">
										<i18n:text>xmlui.cicdigital.home.sobre-repositorio</i18n:text>
									</li>
									<li>
										<a>
											<xsl:attribute name="href">
												<xsl:value-of select="concat($context-path,'/page/que-es-cic-digital')"></xsl:value-of>
											</xsl:attribute>
											<i18n:text>xmlui.cicdigital.title.que-es-cic-digital</i18n:text>
										</a>
									</li>
									<li>
										<a>
											<xsl:attribute name="href">
												<xsl:value-of select="concat($context-path,'/page/politicas-del-repositorio')"></xsl:value-of>
											</xsl:attribute>
											<i18n:text>xmlui.cicdigital.title.politicas-del-repositorio</i18n:text>	
										</a>
									</li>
									<li class="divider"></li>
									<li class="dropdown-header">
										<i18n:text>xmlui.cicdigital.home.informacion-autores</i18n:text>
									</li>
									<li>
										<a>
											<xsl:attribute name="href">
												<xsl:value-of select="concat($context-path,'/page/como-aportar-material')"></xsl:value-of>
											</xsl:attribute>
											<i18n:text>xmlui.cicdigital.title.como-aportar-material</i18n:text>	
										</a>
									</li>
									<li>
										<a>
											<xsl:attribute name="href">
												<xsl:value-of select="concat($context-path,'/register')"></xsl:value-of>
											</xsl:attribute>
											<i18n:text>xmlui.EPerson.Navigation.register</i18n:text>
										</a>
									</li>
								</ul>
							</li>
							<li>
								<xsl:call-template name="build-anchor">
									<xsl:with-param name="a.href">/feedback</xsl:with-param>
									<xsl:with-param name="a.value">
										<i18n:text>xmlui.dri2xhtml.structural.contact-link</i18n:text>
									</xsl:with-param>
								</xsl:call-template>
							</li>
							
						</ul>
<!-- 					</div> -->
						
<!-- 					<div id="bs-example-navbar-collapse-2">class="collapse navbar-collapse"  -->

						<ul class="nav navbar-nav navbar-right">
							<li>
								<a>
									<span class="glyphicon glyphicon-user" aria-hidden="true"></span>
								</a>
							</li>
							<xsl:for-each select="/dri:document/dri:options/dri:list[@n!='browse' and @n!='discovery']">
								<xsl:if test="count(child::*) &gt; 0">
									<xsl:call-template  name="buildMenuItemAsTree"   />
								</xsl:if>
							</xsl:for-each>
<!-- 							<li> -->
<!-- 								<xsl:choose> -->
<!-- 									<xsl:when -->
<!-- 										test="/dri:document/dri:meta/dri:userMeta/@authenticated = 'yes'"> -->
<!-- 										<a class="dropdown-toggle" id="dropdownMenu1" data-toggle="dropdown"> -->
<!-- 											<xsl:value-of -->
<!-- 												select="/dri:document/dri:meta/dri:userMeta/ -->
<!--                                     dri:metadata[@element='identifier' and @qualifier='firstName']" /> -->
<!-- 											<xsl:text> </xsl:text> -->
<!-- 											<xsl:value-of -->
<!-- 												select="/dri:document/dri:meta/dri:userMeta/ -->
<!--                                     dri:metadata[@element='identifier' and @qualifier='lastName']" /> -->
<!-- 											<span class="caret"></span> -->
<!-- 										</a> -->
<!-- 										<ul class="dropdown-menu" role="menu" aria-labelledby="dropdownMenu1"> -->
<!-- 											<li role="presentation"> -->
<!-- 												<xsl:for-each -->
<!-- 													select="//dri:options/dri:list[@n='account']/dri:item/dri:xref"> -->
<!-- 													<a role="menuitem" tabindex="-1"> -->
<!-- 														<xsl:attribute name="href"><xsl:value-of -->
<!-- 															select="@target" /></xsl:attribute> -->
<!-- 														<xsl:copy-of select="." /> -->
<!-- 													</a> -->
<!-- 												</xsl:for-each> -->
<!-- 											</li> -->
<!-- 										</ul> -->
<!-- 									</xsl:when> -->
<!-- 									<xsl:otherwise> -->
<!-- 										<a> -->
<!-- 											<xsl:attribute name="href"> -->
<!-- 	                        	<xsl:value-of -->
<!-- 												select="/dri:document/dri:meta/dri:userMeta/dri:metadata[@element='identifier' and @qualifier='loginURL']" /> -->
<!-- 							</xsl:attribute> -->
<!-- 											<i18n:text>xmlui.dri2xhtml.structural.login</i18n:text> -->
<!-- 										</a> -->
<!-- 									</xsl:otherwise> -->
<!-- 								</xsl:choose> -->
<!-- 							</li> -->
							<li>
								<xsl:call-template name="languageSelection" />

							</li>
						</ul>
						
					</div><!-- /.navbar-collapse -->
				</div><!-- /.container-fluid -->
			</nav>
		</div>
	</xsl:template>

	<xsl:template name="buildMenuItemAsList">
								
		<li class="dropdown">
			<a href="#" class="dropdown-toggle" data-toggle="dropdown" role="button"
				aria-expanded="false">
				<xsl:copy-of select="dri:head/*" />
				<span class="caret"></span>
			</a>

			<ul class="dropdown-menu" role="menu">
				<xsl:for-each select="dri:item/dri:xref">
					<li>
						<xsl:call-template name="build-anchor">
							<xsl:with-param name="a.href" select="@target" />
							<xsl:with-param name="a.value" select="*" />
						</xsl:call-template>
					</li>
				</xsl:for-each>
				<xsl:for-each select="dri:list">
					<xsl:if test="count(dri:item) &gt; 0">
						<li class="dropdown-header"><xsl:copy-of select="dri:head/*" /></li>
					</xsl:if>
					<xsl:for-each select="dri:item/dri:xref">
						<li>
							<xsl:call-template name="build-anchor">
								<xsl:with-param name="a.href" select="@target" />
								<xsl:with-param name="a.value" select="node()" />
							</xsl:call-template>
						</li>
					</xsl:for-each>
				</xsl:for-each>
			</ul>
		</li>
	</xsl:template>

	<xsl:template name="buildMenuItemAsTree">
								
		<li class="dropdown">
			<a href="#" class="dropdown-toggle" data-toggle="dropdown" role="button"
				aria-expanded="false">
				<xsl:copy-of select="dri:head/*" />
				<span class="caret"></span>
			</a>

			<ul class="dropdown-menu" role="menu">
				<xsl:for-each select="dri:item/dri:xref">
					<li>
						<xsl:call-template name="build-anchor">
							<xsl:with-param name="a.href" select="@target" />
							<xsl:with-param name="a.value" select="*" />
						</xsl:call-template>
					</li>
				</xsl:for-each>
				<xsl:for-each select="dri:list">
					<xsl:if test="count(dri:item) &gt; 0">
						<li class="dropdown-submenu">
							<a tabindex="-1" href="#"><xsl:copy-of select="dri:head/*" /></a>
							<ul class="dropdown-menu" role="menu">
								<xsl:for-each select="dri:item/dri:xref">
									<li>
										<xsl:call-template name="build-anchor">
											<xsl:with-param name="a.href" select="@target" />
											<xsl:with-param name="a.value" select="node()" />
										</xsl:call-template>
									</li>
								</xsl:for-each>
							</ul>
						</li>
					</xsl:if>
				</xsl:for-each>
			</ul>
		</li>
	</xsl:template>

	<!-- The header (distinct from the HTML head element) contains the title, 
		subtitle, login box and various placeholders for header images -->
	<xsl:template name="buildHeader">
		<xsl:call-template name="buildTopSidebar" />
		<div id="cic-header" class="row">
			<div class="col-md-offset-1 col-md-6">
				<xsl:call-template name="build-anchor">
					<xsl:with-param name="img.src">images/logo_72.png</xsl:with-param>
					<xsl:with-param name="img.alt">CIC-DIGITAL</xsl:with-param>
				</xsl:call-template>
			</div>
		</div>
		<!--The trail is built by applying a template over pageMeta's trail children. -->
		<xsl:call-template name="buildTrail" />
	</xsl:template>



	<!-- The header (distinct from the HTML head element) contains the title, 
		subtitle, login box and various placeholders for header images -->
	<xsl:template name="buildTrail">
		<div class="row" id="cic-trail">
		<ol class="breadcrumb">
			<xsl:choose>
				<!-- Static pages trail -->
				<xsl:when test="starts-with($request-uri, 'page/')">
					<li>
						<xsl:call-template name="build-anchor">
							<xsl:with-param name="a.href">/</xsl:with-param>
							<xsl:with-param name="a.value">
								<i18n:text>xmlui.general.dspace_home</i18n:text>
							</xsl:with-param>
						</xsl:call-template>
					</li>
					<li class="active">
						<i18n:text>
                        	<xsl:value-of select="concat('xmlui.cicdigital.trail.',substring-after($request-uri,'/'))"/>
                         </i18n:text>
					</li>
				</xsl:when>
				<xsl:when test="count(/dri:document/dri:meta/dri:pageMeta/dri:trail/@target) > 0">
					<xsl:for-each select="/dri:document/dri:meta/dri:pageMeta/dri:trail">
						<li>
							<a>
								<xsl:choose>
									<xsl:when test="@target">
										<xsl:attribute name="href"><xsl:value-of
											select="@target" /></xsl:attribute>
									</xsl:when>
									<xsl:otherwise>
										<xsl:attribute name="class">active</xsl:attribute>
									</xsl:otherwise>
								</xsl:choose>
								<xsl:copy-of select="." />
							</a>
						</li>
					</xsl:for-each>

				</xsl:when>
				<xsl:otherwise>
					<!-- No se muestra nada porque estamos en el home -->
				</xsl:otherwise>
			</xsl:choose>
		</ol>
		</div>
	</xsl:template>
</xsl:stylesheet>