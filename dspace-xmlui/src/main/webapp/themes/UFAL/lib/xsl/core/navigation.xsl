<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->
<!--
    Rendering specific to the navigation (options)

    Author: art.lowel at atmire.com
    Author: lieven.droogmans at atmire.com
    Author: ben at atmire.com
    Author: Alexey Maslov

	modified for LINDAT/CLARIN

-->

<xsl:stylesheet xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
                xmlns:dri="http://di.tamu.edu/DRI/1.0/"
                xmlns:mets="http://www.loc.gov/METS/"
                xmlns:xlink="http://www.w3.org/TR/xlink/"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
                xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
                xmlns:xhtml="http://www.w3.org/1999/xhtml"
                xmlns:mods="http://www.loc.gov/mods/v3"
                xmlns:dc="http://purl.org/dc/elements/1.1/"
                xmlns="http://www.w3.org/1999/xhtml"
                exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc">

    <xsl:output indent="yes" />
    
	<xsl:template match="/dri:document/dri:options" priority="10">	
		<div id="options-menu" class="sidebar col-sm-3 col-xs-12 hidden-xs">
			<ul class="nav nav-list">
				<xsl:if test="not(//dri:div[@n='site-home'])">
					<li class="always-open hidden-xs">
						<div style="background-color: #FFFFFF;" class="clearfix">
							<div class="col-sm-7" style="height: 120px; position: relative;">
								<a href="/lindat">
									<img alt="LINDAT/CLARIN logo" class="img-responsive" style="position: absolute; top: 0px; bottom: 0px; left: 0px; right: 0px; padding: 20px;" src="{$context-path}/themes/UFAL/images/lindat/lindat-logo.png" />
								</a>
							</div>
							<div class="col-sm-5 text-center" style="height: 120px; position: relative;">
								<a href="http://www.clarin.eu/">
									<img alt="CLARIN logo" class="img-responsive" style="position: absolute; bottom: 0px; left: 0px; right: 0px; padding: 10px;" src="{$context-path}/themes/UFAL/images/lindat/clarin-logo.png" />
								</a>
							</div>
						</div>					
					</li>
				</xsl:if>
				<li class="always-open hidden-xs">
					<xsl:call-template name="howto-panel" />
				</li>
				<xsl:apply-templates select="dri:list[count(child::*)!=0]" />
				<xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='feed']">
					<li class="always-open">
						<xsl:call-template name="addRSSLinks" />
					</li>				
				</xsl:if>
			</ul>
		</div>
		<div class="sidebar col-sm-3 placeholder col-xs-12 hidden-xs">&#160;</div>
	</xsl:template>

	<xsl:template match="/dri:document/dri:options/dri:list" priority="10">
		<li class="always-open">
			<a href="#" style="cursor: default;" onclick="return false;">
				<xsl:call-template name="menu-icon">
					<xsl:with-param name="key">
						<xsl:value-of select="dri:head" />
					</xsl:with-param>
				</xsl:call-template>
				<span class="menu-text">
						<xsl:apply-templates select="dri:head" />
				</span>
				<b class="arrow fa fa-caret-down">&#160;</b>				
			</a>
			<ul class="submenu">
				<xsl:apply-templates select="*[not(name()='head')]" />
			</ul>							
		</li>
	</xsl:template>	

	<xsl:template match="/dri:document/dri:options/dri:list//dri:list" priority="10">
		<li class="">
			<a href="#" class="dropdown-toggle">
				<i class="fa fa-caret-right">&#160;</i>
				<span class="menu-text">
					<xsl:call-template name="menu-icon">
						<xsl:with-param name="key">
							<xsl:value-of select="dri:head" />
						</xsl:with-param>
					</xsl:call-template>			
					<xsl:apply-templates select="dri:head" />
				</span>
				<b class="arrow fa fa-caret-down">&#160;</b>				
			</a>
			<ul class="submenu">
				<xsl:apply-templates select="*[not(name()='head')]" />
			</ul>							
		</li>
	</xsl:template>		
	
	<xsl:template match="/dri:document/dri:options/dri:list/dri:head" priority="10">
		<i18n:text>
			<xsl:value-of select="." />
		</i18n:text>
	</xsl:template>
	
	<xsl:template match="/dri:document/dri:options/dri:list/dri:item" priority="10">
		<xsl:if test=".!=''">
			<li>
				<a>
					<xsl:attribute name="href">
						<xsl:value-of select="dri:xref/@target" />
					</xsl:attribute>
					<xsl:attribute name="class">
						<xsl:value-of select="dri:xref/@rend" />
					</xsl:attribute>
					<i class="fa fa-caret-right">&#160;</i>
					<span class="menu-text">
						<xsl:call-template name="menu-icon">
							<xsl:with-param name="key">
								<xsl:value-of select="dri:xref" />
							</xsl:with-param>
						</xsl:call-template>															
					
						<i18n:text>
							<xsl:value-of select="dri:xref" />
						</i18n:text>						
					</span>			
				</a>
			</li>
		</xsl:if>
	</xsl:template>

	
	<xsl:template match="/dri:document/dri:options/dri:list//dri:list/dri:item" priority="10">
		<xsl:if test=".!=''">
			<li>
				<a>
					<xsl:attribute name="href">
						<xsl:value-of select="dri:xref/@target" />
					</xsl:attribute>
					<xsl:attribute name="class">
						<xsl:value-of select="dri:xref/@rend" />
					</xsl:attribute>
					<xsl:call-template name="menu-icon">
						<xsl:with-param name="key">
							<xsl:value-of select="dri:xref" />
						</xsl:with-param>
					</xsl:call-template>															
					<span class="menu-text">
						<i18n:text>
							<xsl:value-of select="dri:xref" />
						</i18n:text>						
					</span>			
				</a>
			</li>
		</xsl:if>
	</xsl:template>

    <xsl:template name="menu-icon">
    	<xsl:param name="key" />
    	<xsl:choose>
    		<xsl:when test="$key='xmlui.EPerson.Navigation.my_account'">
    			<i class="fa fa-user fa-lg">&#160;</i>
    		</xsl:when>
    		<xsl:when test="$key='xmlui.EPerson.Navigation.login'">
    			<i class="fa fa-sign-in fa-lg">&#160;</i>
    		</xsl:when>
    		<xsl:when test="$key='xmlui.EPerson.Navigation.discojuice.login'">
    			<i class="fa fa-sign-in fa-lg">&#160;</i>
    		</xsl:when>    		
    		<xsl:when test="$key='xmlui.administrative.Navigation.context_head'">
    			<i class="fa fa-list fa-lg">&#160;</i>
    		</xsl:when>
    		<xsl:when test="$key='xmlui.administrative.Navigation.administrative_head'">
    			<i class="fa fa-gears fa-lg">&#160;</i>
    		</xsl:when>
			<xsl:when test="$key='xmlui.EPerson.Navigation.logout'">
    			<i class="fa fa-sign-out fa-lg">&#160;</i>
    		</xsl:when>
			<xsl:when test="$key='xmlui.EPerson.Navigation.profile'">
    			<i class="fa fa-user fa-lg">&#160;</i>
    		</xsl:when>    		    		    		
			<xsl:when test="$key='xmlui.EPerson.Navigation.helpdesk'">
    			<i class="fa fa-envelope fa-lg">&#160;</i>
    		</xsl:when>    		    		    		    		
			<xsl:when test="$key='xmlui.Submission.Navigation.submissions'">
    			<i class="fa fa-upload fa-lg">&#160;</i>
    		</xsl:when>
			<xsl:when test="$key='xmlui.administrative.Navigation.context_create_community'">
    			<i class="fa fa-file fa-lg">&#160;</i>
    		</xsl:when>
			<xsl:when test="$key='xmlui.administrative.Navigation.administrative_access_control'">
    			<i class="fa fa-eye-slash fa-lg">&#160;</i>
    		</xsl:when>
			<xsl:when test="$key='xmlui.administrative.Navigation.administrative_registries'">
    			<i class="fa fa-th fa-lg">&#160;</i>
    		</xsl:when>    		
			<xsl:when test="$key='xmlui.administrative.Navigation.administrative_items'">
    			<i class="fa fa-bars fa-lg">&#160;</i>
    		</xsl:when>
			<xsl:when test="$key='Collections &amp; Communities'">
    			<i class="fa fa-sitemap fa-lg">&#160;</i>
    		</xsl:when>
			<xsl:when test="$key='xmlui.administrative.Navigation.administrative_withdrawn'">
    			<i class="fa fa-trash-o fa-lg">&#160;</i>
    		</xsl:when>
			<xsl:when test="$key='xmlui.administrative.Navigation.administrative_control_panel'">
    			<i class="fa fa-wrench fa-lg">&#160;</i>
    		</xsl:when>
			<xsl:when test="$key='xmlui.administrative.Navigation.statistics'">
    			<i class="fa fa-bar-chart-o">&#160;</i>
    		</xsl:when>
			<xsl:when test="$key='xmlui.statistics.Navigation.title'">
    			<i class="fa fa-bar-chart-o fa-lg">&#160;</i>
    		</xsl:when>
			<xsl:when test="$key='xmlui.administrative.Navigation.administrative_import_metadata'">
    			<i class="fa fa-download fa-lg">&#160;</i>
    		</xsl:when>
			<xsl:when test="$key='xmlui.administrative.Navigation.administrative_curation'">
    			<i class="fa fa-medkit fa-lg">&#160;</i>
    		</xsl:when>
			<xsl:when test="$key='xmlui.administrative.Navigation.administrative_licenses'">
    			<i class="fa fa-legal fa-lg">&#160;</i>
    		</xsl:when>
			<xsl:when test="$key='xmlui.administrative.Navigation.administrative_handles'">
    			<i class="fa fa-hand-o-right fa-lg">&#160;</i>
    		</xsl:when>
			<xsl:when test="$key='xmlui.statistics.Navigation.view'">
    			<i class="fa fa-eye fa-lg">&#160;</i>
    		</xsl:when>    		
			<xsl:when test="$key='xmlui.statistics.Navigation.ga.title'">
    			<i class="fa fa-google-plus-square fa-lg">&#160;</i>
    		</xsl:when>
			<xsl:when test="$key='xmlui.statistics.Navigation.piwik.title'">
    			<i class="fa fa-area-chart fa-lg">&#160;</i>
    		</xsl:when>    		
			<xsl:when test="$key='xmlui.ArtifactBrowser.Navigation.head_browse'">
    			<i class="fa fa-bullseye  fa-lg">&#160;</i>
    		</xsl:when>    		    		    		
			<xsl:when test="$key='xmlui.EPerson.Navigation.about-head'">
    			<i class="fa fa-info-circle fa-lg">&#160;</i>
    		</xsl:when>    		    		    		
			<xsl:when test="$key='xmlui.EPerson.Navigation.deposit'">
				<i class="fa fa-upload fa-lg">&#160;</i>
    		</xsl:when>
			<xsl:when test="$key='xmlui.EPerson.Navigation.cite'">
				<i class="fa fa-quote-right fa-lg">&#160;</i>
    		</xsl:when>
			<xsl:when test="$key='xmlui.EPerson.Navigation.item-lifecycle'">
				<i class="fa fa-refresh fa-lg">&#160;</i>
    		</xsl:when>
			<xsl:when test="$key='xmlui.EPerson.Navigation.faq'">
				<i class="fa fa-question fa-lg">&#160;</i>
    		</xsl:when>
			<xsl:when test="$key='xmlui.EPerson.Navigation.about'">
				<i class="fa fa-exclamation-circle fa-lg">&#160;</i>
    		</xsl:when>    		
    		<xsl:otherwise>
    			<i class="fa fa-angle-right">&#160;</i>
    		</xsl:otherwise>    		    		
    	</xsl:choose>
    </xsl:template>

    <!-- Add each RSS feed from meta to a list -->
    <xsl:template name="addRSSLinks">
        <a>
                <i class="fa fa-rss-square ">&#160;</i>
                <span class="menu-text">
                        RSS Feed
                </span>
        </a>        	
		<ul class="submenu" style="padding-bottom: 8px;">    
        	<xsl:for-each select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='feed']">
	            <li>
	                <a>
	                    <xsl:attribute name="href">
	                        <xsl:value-of select="."/>
	                    </xsl:attribute>
						<i class="fa fa-caret-right">&#160;</i>
						<span class="menu-text">
		                    <i class="fa fa-rss">&#160;</i>
		                    <xsl:choose>
		                        <xsl:when test="contains(., 'rss_1.0')">
		                            <xsl:text>RSS 1.0</xsl:text>
		                        </xsl:when>
		                        <xsl:when test="contains(., 'rss_2.0')">
		                            <xsl:text>RSS 2.0</xsl:text>
		                        </xsl:when>
		                        <xsl:when test="contains(., 'atom_1.0')">
		                            <xsl:text>Atom</xsl:text>
		                        </xsl:when>
		                        <xsl:otherwise>
		                            <xsl:value-of select="@qualifier"/>
		                        </xsl:otherwise>
		                    </xsl:choose>
						</span>
	                </a>
	            </li>
        	</xsl:for-each>
		</ul>        
    </xsl:template>


	<!-- Quick patch to remove empty lists from options -->
	<xsl:template match="dri:options//dri:list[count(child::*)=0]"
		priority="5" mode="nested">
	</xsl:template>
	<xsl:template match="dri:options//dri:list[count(child::*)=0]"
		priority="5">
	</xsl:template>

    
    <xsl:template name="howto-panel">
                <a>
                        <i class="fa fa-question-circle">&#160;</i>
                        <span class="menu-text">
							<i18n:text>xmlui.UFAL.navigation.menu_head</i18n:text>
                        </span>
                </a>
                <ul class="submenu" style="padding-bottom: 8px;">
                        <li>
                        <a href="{$context-path}/page/deposit" style="border-top: none; padding: 7px 0px 8px 18px">
                                <img src="{$context-path}/themes/UFALHome/lib/images/deposit.png" align="left" class="deposit" />
                        </a>
                        </li>
                        <li>
                                <a href="{$context-path}/page/cite" style="border-top: none; padding: 7px 0px 8px 18px;">
                                <img src="{$context-path}/themes/UFALHome/lib/images/cite.png" align="right" class="cite" />
                                </a>
                        </li>
                </ul>
    </xsl:template>

	<xsl:template name="navbar">
		<nav class="navbar-fixed-top">
			<div class="container-fluid">
				<ul class="nav navbar-nav navbar-right">
				<li>
					<xsl:call-template name="userbox"/>
				</li>
				</ul>
			<ul class="nav navbar-nav navbar-left">
			<li>
				<xsl:call-template name="languageSelection"/>
			</li>
			</ul>
			</div>
        </nav>
	</xsl:template>
	<xsl:template name="userbox">
		<xsl:choose>
			<xsl:when
				test="/dri:document/dri:meta/dri:userMeta[@authenticated = 'yes']">
				<div id="userbox" style="opacity: 1 !important;">
					<div class="label label-primary"
						style="margin: 5px 15px 5px 5px; padding: 5px 10px 5px 10px; font-size: 12px;">

						<a style="color: #FFF;">
							<xsl:attribute name="href">
                                                        <xsl:value-of
								select="/dri:document/dri:meta/dri:userMeta/dri:metadata[@element='identifier' and @qualifier='url']" />
                                                </xsl:attribute>
							<i class="fa fa-user fa-lg">&#160;</i>
							<xsl:value-of
								select="/dri:document/dri:meta/dri:userMeta/dri:metadata[@element='identifier' and @qualifier='displayName']" />
						</a>
						<span style="margin-left: 5px; margin-right: 5px;">
							<xsl:text> | </xsl:text>
						</span>
						<a style="color: #FFF;">
							<xsl:attribute name="href">
                                                        <xsl:value-of
								select="/dri:document/dri:meta/dri:userMeta/dri:metadata[@element='identifier' and @qualifier='logoutURL']" />
                                                </xsl:attribute>
							<i class="fa fa-sign-out fa-lg">&#160;</i>
							<i18n:text>xmlui.dri2xhtml.structural.logout</i18n:text>
						</a>
					</div>
				</div>

			</xsl:when>
			<xsl:otherwise>
				<div id="userbox" style="opacity: 1 !important;">
					<div class="label label-important"
						style="margin: 5px 15px 5px 5px; padding: 5px 10px 5px 10px; font-size: 16px;">

						<a style="color: #FFF;" class="signon">
							<xsl:attribute name="href">
                                                        <xsl:value-of
								select="/dri:document/dri:meta/dri:userMeta/dri:metadata[@element='identifier' and @qualifier='loginURL']" />
                                                </xsl:attribute>
							<i class="fa fa-sign-in fa-lg">&#160;</i>
							<i18n:text>xmlui.dri2xhtml.structural.login</i18n:text>
						</a>
					</div>
				</div>

			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
    <!-- Display language selection if more than 1 language is supported (overides buggy dir2xhtml-alt).
    Uses a page metadata curRequestURI which was introduced by in /xmlui/src/main/webapp/themes/Mirage/sitemap.xmap-->
    <xsl:template name="languageSelection">
        <xsl:variable name="curRequestURI">
            <xsl:value-of select="substring-after(/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='curRequestURI'],/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='URI'])"/>
        </xsl:variable>
		<xsl:variable name="currentLocale">
			<xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='page'][@qualifier='currentLocale']"/>
		</xsl:variable>
        <xsl:if test="count(/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='page'][@qualifier='supportedLocale']) &gt; 1">
            <div id="ds-language-selection">
				<xsl:attribute name="data-locale"><xsl:value-of select="$currentLocale"/></xsl:attribute>
                <xsl:for-each select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='page'][@qualifier='supportedLocale']">
                    <xsl:variable name="locale" select="."/>
                    <a>
                        <xsl:attribute name="href">
                            <xsl:value-of select="$curRequestURI"/>
                            <xsl:call-template name="getLanguageURL"/>
                            <xsl:value-of select="$locale"/>
                        </xsl:attribute>
			<img>
			    <xsl:attribute name="src">
				<xsl:value-of select="concat($context-path,'/themes/UFAL/lib/lindat/public/img/flags/',$locale,'.png')"/>
			    </xsl:attribute>
			    <xsl:attribute name="alt">
	                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='supportedLocale'][@qualifier=$locale]"/>
			    </xsl:attribute>
			    <xsl:attribute name="title">
	                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='supportedLocale'][@qualifier=$locale]"/>
			    </xsl:attribute>
			</img>
                    </a>
                </xsl:for-each>
            </div>
        </xsl:if>
    </xsl:template>
    <!-- Builds the Query String part of the language URL. If there allready is an excisting query string 
    like: ?filtertype=subject&filter_relational_operator=equals&filter=keyword1 it appends the locale parameter with the ampersand (&) symbol -->
    <xsl:template name="getLanguageURL">
        <xsl:variable name="queryString" select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='queryString']"/>
        <xsl:choose>
            <!-- There allready is a query string so append it and the language argument -->
            <xsl:when test="$queryString != ''">
                <xsl:text>?</xsl:text>
                <xsl:choose>
                    <xsl:when test="contains($queryString, '&amp;locale-attribute')">
                        <xsl:value-of select="substring-before($queryString, '&amp;locale-attribute')"/>
                        <xsl:text>&amp;locale-attribute=</xsl:text>
                    </xsl:when>
                    <!-- the query string is only the locale-attribute so remove it to append the correct one -->
                    <xsl:when test="starts-with($queryString, 'locale-attribute')">
                        <xsl:text>locale-attribute=</xsl:text>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="$queryString"/>
                        <xsl:text>&amp;locale-attribute=</xsl:text>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
                <xsl:text>?locale-attribute=</xsl:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>


</xsl:stylesheet>
