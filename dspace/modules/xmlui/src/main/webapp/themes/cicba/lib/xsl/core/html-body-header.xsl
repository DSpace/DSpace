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

					<!-- Collect the nav links, forms, and other content for toggling -->
					<div class="collapse navbar-collapse" id="bs-example-navbar-collapse-1">
						<ul class="nav navbar-nav">
							<li class="active">
								<xsl:call-template name="build-anchor">
									<xsl:with-param name="a.href">/</xsl:with-param>
									<xsl:with-param name="a.value">
										Inicio
									</xsl:with-param>
								</xsl:call-template>
							</li>
							<li>
								<xsl:call-template name="build-anchor">
									<xsl:with-param name="a.href">/submissions</xsl:with-param>
									<xsl:with-param name="a.value">
										Aportar Material
									</xsl:with-param>
								</xsl:call-template>
							</li>
							
							<li class="dropdown">
								<a href="#" class="dropdown-toggle" data-toggle="dropdown"
									role="button" aria-expanded="false">
									Mas información
									<span class="caret"></span>
								</a>
								<ul class="dropdown-menu" role="menu">
									<li class="dropdown-header">Sobre el repositorio</li>
									<li>
										<a href="#">Qué es CIC-DIGITAL?</a>
									</li>
									<li>
										<a href="#">Políticas del repositorio</a>
									</li>
									<li class="divider"></li>
									<li class="dropdown-header">Información para autores</li>
									<li>
										<a href="#">Como aportar Material</a>
									</li>
									<li>
										<a href="#">Registrarse</a>
									</li>
								</ul>
							</li>
							<li>
								<xsl:call-template name="build-anchor">
									<xsl:with-param name="a.href">/feedback</xsl:with-param>
									<xsl:with-param name="a.value">
										Contacto
									</xsl:with-param>
								</xsl:call-template>
							</li>
						</ul>

						<ul class="nav navbar-nav navbar-right">
							<li>
								<a>
									<span class="glyphicon glyphicon-user" aria-hidden="true"></span>
								</a>
							</li>
							<li>
								<xsl:choose>
									<xsl:when
										test="/dri:document/dri:meta/dri:userMeta/@authenticated = 'yes'">
										<a class="dropdown-toggle" id="dropdownMenu1" data-toggle="dropdown">
											<xsl:value-of
												select="/dri:document/dri:meta/dri:userMeta/
                                    dri:metadata[@element='identifier' and @qualifier='firstName']" />
											<xsl:text> </xsl:text>
											<xsl:value-of
												select="/dri:document/dri:meta/dri:userMeta/
                                    dri:metadata[@element='identifier' and @qualifier='lastName']" />
											<span class="caret"></span>
										</a>
										<ul class="dropdown-menu" role="menu" aria-labelledby="dropdownMenu1">
											<li role="presentation">
												<xsl:for-each
													select="//dri:options/dri:list[@n='account']/dri:item/dri:xref">
													<a role="menuitem" tabindex="-1">
														<xsl:attribute name="href"><xsl:value-of
															select="@target" /></xsl:attribute>
														<xsl:copy-of select="." />
													</a>
												</xsl:for-each>
											</li>
										</ul>
									</xsl:when>
									<xsl:otherwise>
										<a>
											<xsl:attribute name="href">
	                        	<xsl:value-of
												select="/dri:document/dri:meta/dri:userMeta/dri:metadata[@element='identifier' and @qualifier='loginURL']" />
							</xsl:attribute>
											<i18n:text>xmlui.dri2xhtml.structural.login</i18n:text>
										</a>
									</xsl:otherwise>
								</xsl:choose>
							</li>
							<li>
								<xsl:call-template name="languageSelection" />

							</li>
						</ul>
					</div><!-- /.navbar-collapse -->
				</div><!-- /.container-fluid -->
			</nav>
		</div>
	</xsl:template>

	<!-- The header (distinct from the HTML head element) contains the title, 
		subtitle, login box and various placeholders for header images -->
	<xsl:template name="buildHeader">
		<xsl:call-template name="buildTopSidebar" />
		<div id="cic-header" class="row">
			<div class="page-header">
				<h1>
					<i18n:text>xmlui.dri2xhtml.structural.head-subtitle</i18n:text>
				</h1>
			</div>
		</div>
		<!--The trail is built by applying a template over pageMeta's trail children. -->
		<xsl:call-template name="buildTrail" />
	</xsl:template>



	<!-- The header (distinct from the HTML head element) contains the title, 
		subtitle, login box and various placeholders for header images -->
	<xsl:template name="buildTrail">
		<ol class="breadcrumb row">

			<xsl:choose>
				<xsl:when test="starts-with($request-uri, 'page/about')">
					<li class="active">
						<xsl:text>About This Repository</xsl:text>
					</li>
				</xsl:when>
				<xsl:when test="count(/dri:document/dri:meta/dri:pageMeta/dri:trail) > 0">
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
	</xsl:template>
</xsl:stylesheet>