<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet xmlns="http://www.w3.org/1999/xhtml"
	xmlns:i18n="http://apache.org/cocoon/i18n/2.1" xmlns:dri="http://di.tamu.edu/DRI/1.0/"
	xmlns:mets="http://www.loc.gov/METS/" xmlns:dc="http://purl.org/dc/elements/1.1/"
	xmlns:dim="http://www.dspace.org/xmlns/dspace/dim" xmlns:mods="http://www.loc.gov/mods/v3"
	xmlns:xlink="http://www.w3.org/TR/xlink/" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:xalan="http://xml.apache.org/xalan" xmlns:datetime="http://exslt.org/dates-and-times"
	xmlns:encoder="xalan://java.net.URLEncoder" xmlns:decoder="xalan://java.net.URLDecoder"
	xmlns:strings="http://exslt.org/strings" xmlns:set="http://exslt.org/sets"
	exclude-result-prefixes="xalan set strings decoder encoder datetime"
	version="1.0">

	<xsl:import href="DryadUtils.xsl" />

	<xsl:variable name="meta"
		select="/dri:document/dri:meta/dri:pageMeta/dri:metadata" />

	<!-- Parses query parameters that come out in the dspace metadata and returns 
		a simple XML structure representing the one that was requested via the template 
		param. -->
	<xsl:template name="parse-query-param">
		<xsl:param name="param-name" />
		<xsl:param name="param-prefix" />
		<xsl:variable name="query-string"
			select="decoder:decode($meta[@element='request'][@qualifier='queryString'], 'utf-8')" />
		<xsl:if test="contains($query-string, concat($param-name, '='))">
			<xsl:for-each select="xalan:tokenize($query-string, '&amp;')">
				<xsl:if test="starts-with(., concat($param-name, '='))">
					<xsl:element name="{$param-name}">
						<xsl:choose>
							<xsl:when test="$param-prefix != ''">
								<xsl:value-of select="concat($param-prefix, xalan:tokenize(., '=')[2])" />
							</xsl:when>
							<xsl:otherwise>
								<xsl:value-of select="xalan:tokenize(., '=')[2]" />
							</xsl:otherwise>
						</xsl:choose>
					</xsl:element>
				</xsl:if>
			</xsl:for-each>
		</xsl:if>
	</xsl:template>

	<!-- Returns a reconstructed query (in url form: pname=pval&pname2=pval2&pname3=pval3) 
		from the XML form DSpace returns as page metadata. It will skip params specified 
		in this template's param value; multiple params can be passed in using 
		the semicolon as a delimiter. -->
	<xsl:template name="get-query">
		<xsl:param name="without" />
		<xsl:variable name="query-string"
			select="decoder:decode($meta[@element='request'][@qualifier='queryString'])" />
		<!-- this would be much more straight-forward with XSLT 2.0 ... grumble -->
		<xsl:variable name="without-params"
			select="concat('|', translate($without, ';', '|'), '|')" />
		<xsl:for-each select="xalan:tokenize($query-string, '&amp;')">
			<xsl:variable name="param" select="xalan:tokenize(., '=')[1]" />
			<xsl:variable name="value" select="xalan:tokenize(., '=')[2]" />
			<xsl:if test="not(contains($without-params, concat('|', $param, '|')))">
				<xsl:value-of select="concat('&amp;', $param, '=', encoder:encode($value))" />
			</xsl:if>
		</xsl:for-each>
	</xsl:template>

	<!--  Contains the pagination links for Dryad, is a modified version of
	what comes with DSpace (logic is same, we just display differently) -->
	<xsl:template match="@pagination">
		<xsl:param name="position" />
		<!-- Would fit better in Java, but trying to avoid touching discovery code -->
		<xsl:variable name="location">
			<xsl:call-template name="parse-query-param">
				<xsl:with-param name="param-name">location</xsl:with-param>
			</xsl:call-template>
		</xsl:variable>
		<xsl:choose>
			<xsl:when test=". = 'simple'">
				<div class="pagination {$position}">
					<xsl:if test="parent::node()/@previousPage">
						<a class="previous-page-link">
							<xsl:attribute name="href">
								<xsl:value-of select="parent::node()/@previousPage" />
								<xsl:if test="$location != ''">
									<xsl:value-of select="concat('&amp;location=', $location)" />
								</xsl:if>
							</xsl:attribute>
							<i18n:text>xmlui.dri2xhtml.structural.pagination-previous</i18n:text>
						</a>
					</xsl:if>
					<span class="pagination-info">
						<i18n:translate>
							<i18n:text>xmlui.dri2xhtml.structural.pagination-info</i18n:text>
							<i18n:param>
								<xsl:value-of select="parent::node()/@firstItemIndex" />
							</i18n:param>
							<i18n:param>
								<xsl:value-of select="parent::node()/@lastItemIndex" />
							</i18n:param>
							<i18n:param>
								<xsl:value-of select="parent::node()/@itemsTotal" />
							</i18n:param>
						</i18n:translate>
					</span>
					<xsl:if test="parent::node()/@nextPage">
						<a class="next-page-link">
							<xsl:attribute name="href">
								<xsl:value-of select="parent::node()/@nextPage" />
								<xsl:if test="$location != ''">
									<xsl:value-of select="concat('&amp;location=', $location)" />
								</xsl:if>
							</xsl:attribute>
							<i18n:text>xmlui.dri2xhtml.structural.pagination-next</i18n:text>
						</a>
					</xsl:if>
				</div>
			</xsl:when>
			<xsl:when test=". = 'masked'">
				<div class="pagination-masked {$position}">
					<ul class="pagination-links">
						<xsl:if
							test="not(parent::node()/@firstItemIndex = 0 or parent::node()/@firstItemIndex = 1)">
							<li>
								<a class="previous-page-link">
									<xsl:attribute name="href">
										<xsl:value-of
										select="substring-before(parent::node()/@pageURLMask,'{pageNum}')" />
										<xsl:value-of select="parent::node()/@currentPage - 1" />
										<xsl:value-of
										select="substring-after(parent::node()/@pageURLMask,'{pageNum}')" />
										<xsl:if test="$location != ''">
											<xsl:value-of select="concat('&amp;location=', $location)" />
										</xsl:if>
									</xsl:attribute>
									<i18n:text>xmlui.dri2xhtml.structural.pagination-previous</i18n:text>
								</a>
							</li>
						</xsl:if>

						<xsl:if test="(parent::node()/@currentPage - 4) &gt; 0">
							<li class="first-page-link">
								<a>
									<xsl:attribute name="href">
										<xsl:value-of
										select="substring-before(parent::node()/@pageURLMask,'{pageNum}')" />
										<xsl:text>1</xsl:text>
										<xsl:value-of
										select="substring-after(parent::node()/@pageURLMask,'{pageNum}')" />
										<xsl:if test="$location != ''">
											<xsl:value-of select="concat('&amp;location=', $location)" />
										</xsl:if>
									</xsl:attribute>
									<xsl:text>1</xsl:text>
								</a>
								<xsl:text>&#160;&#160; . . . </xsl:text>
							</li>
						</xsl:if>
						<xsl:call-template name="offset-link">
							<xsl:with-param name="pageOffset">-3</xsl:with-param>
							<xsl:with-param name="location" select="$location" />
						</xsl:call-template>
						<xsl:call-template name="offset-link">
							<xsl:with-param name="pageOffset">-2</xsl:with-param>
							<xsl:with-param name="location" select="$location" />
						</xsl:call-template>
						<xsl:call-template name="offset-link">
							<xsl:with-param name="pageOffset">-1</xsl:with-param>
							<xsl:with-param name="location" select="$location" />
						</xsl:call-template>
						<xsl:call-template name="offset-link">
							<xsl:with-param name="pageOffset">0</xsl:with-param>
							<xsl:with-param name="location" select="$location" />
						</xsl:call-template>
						<xsl:call-template name="offset-link">
							<xsl:with-param name="pageOffset">1</xsl:with-param>
							<xsl:with-param name="location" select="$location" />
						</xsl:call-template>
						<xsl:call-template name="offset-link">
							<xsl:with-param name="pageOffset">2</xsl:with-param>
							<xsl:with-param name="location" select="$location" />
						</xsl:call-template>
						<xsl:call-template name="offset-link">
							<xsl:with-param name="pageOffset">3</xsl:with-param>
							<xsl:with-param name="location" select="$location" />
						</xsl:call-template>
						<xsl:if
							test="(parent::node()/@currentPage + 4) &lt;= (parent::node()/@pagesTotal)">
							<li class="last-page-link">
								<xsl:text> . . . &#160;&#160;</xsl:text>
								<a>
									<xsl:attribute name="href">
										<xsl:value-of
										select="substring-before(parent::node()/@pageURLMask,'{pageNum}')" />
										<xsl:value-of select="parent::node()/@pagesTotal" />
										<xsl:value-of
										select="substring-after(parent::node()/@pageURLMask,'{pageNum}')" />
										<xsl:if test="$location != ''">
											<xsl:value-of select="concat('&amp;location=', $location)" />
										</xsl:if>
									</xsl:attribute>
									<xsl:value-of select="parent::node()/@pagesTotal" />
								</a>
							</li>
						</xsl:if>
						<xsl:if
							test="not(parent::node()/@lastItemIndex = parent::node()/@itemsTotal)">
							<li>
								<a class="next-page-link">
									<xsl:attribute name="href">
										<xsl:value-of
										select="substring-before(parent::node()/@pageURLMask,'{pageNum}')" />
										<xsl:value-of select="parent::node()/@currentPage + 1" />
										<xsl:value-of
										select="substring-after(parent::node()/@pageURLMask,'{pageNum}')" />
										<xsl:if test="$location != ''">
											<xsl:value-of select="concat('&amp;location=', $location)" />
										</xsl:if>
									</xsl:attribute>
									<i18n:text>xmlui.dri2xhtml.structural.pagination-next</i18n:text>
								</a>
							</li>
						</xsl:if>
					</ul>
				</div>
			</xsl:when>
		</xsl:choose>
	</xsl:template>

	<!-- A quick helper function used by the @pagination template for repetitive 
		tasks -->
	<xsl:template name="offset-link">
		<xsl:param name="pageOffset" />
		<xsl:param name="location" />
		<xsl:if
			test="((parent::node()/@currentPage + $pageOffset) &gt; 0) and
			((parent::node()/@currentPage + $pageOffset) &lt;= (parent::node()/@pagesTotal))">
			<li class="page-link">
				<xsl:if test="$pageOffset = 0">
					<xsl:attribute name="class">current-page-link</xsl:attribute>
				</xsl:if>
				<a>
					<xsl:attribute name="href">
						<xsl:value-of
						select="substring-before(parent::node()/@pageURLMask,'{pageNum}')" />
						<xsl:value-of select="parent::node()/@currentPage + $pageOffset" />
						<xsl:value-of
						select="substring-after(parent::node()/@pageURLMask,'{pageNum}')" />
						<xsl:if test="$location != ''">
							<xsl:value-of select="concat('&amp;location=', $location)" />
						</xsl:if>
					</xsl:attribute>
					<xsl:value-of select="parent::node()/@currentPage + $pageOffset" />
				</a>
			</li>
		</xsl:if>
	</xsl:template>

	<!-- Change the fields shown in search results -->
	<xsl:template match="dim:dim" mode="itemSummaryList-DIM">
		<xsl:variable name="itemWithdrawn" select="@withdrawn" />
        <xsl:variable name="doiIdentifier" select=".//dim:field[@element='identifier'][@mdschema='dc'][not(@qualifier)]" />

        <xsl:call-template name="itemSummaryTemplate">
            <xsl:with-param name="itemUrl" select="false"/>
            <xsl:with-param name="itemWithdrawn" select="$itemWithdrawn"/>
            <xsl:with-param name="doiIdentifier" select="$doiIdentifier"/>
        </xsl:call-template>
	</xsl:template>

    <xsl:template match="dim:dim" mode="summaryNonArchivedList-DIM">
        <xsl:variable name="itemUrl">
            <xsl:text>internal-item?</xsl:text>
            <xsl:value-of select="substring-after(ancestor::mets:METS/@OBJEDIT, '?')"/>
        </xsl:variable>

        <xsl:call-template name="itemSummaryTemplate">
            <xsl:with-param name="itemUrl" select="$itemUrl"/>
            <xsl:with-param name="itemWithdrawn" select="false"/>
            <xsl:with-param name="doiIdentifier" select="false"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template name="itemSummaryTemplate">
        <xsl:param name="itemUrl"/>
        <xsl:param name="itemWithdrawn"/>
        <xsl:param name="doiIdentifier"/>

        <div class="artifact-description" style="padding: 6px;">
            <xsl:element name="a">
                <xsl:attribute name="href">
                    <xsl:choose>
                        <xsl:when test="$itemUrl">
                            <xsl:value-of select="$itemUrl"/>
                        </xsl:when>
                        <xsl:when test="$itemWithdrawn">
                            <xsl:value-of select="ancestor::mets:METS/@OBJEDIT"/>
                        </xsl:when>
                        <xsl:when test="$doiIdentifier">
                            <xsl:text>/resource/</xsl:text>
                            <xsl:copy-of select=".//dim:field[@element='identifier'][@mdschema='dc'][not(@qualifier)]"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="ancestor::mets:METS/@OBJID"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:attribute>
                <span class="author">
                    <xsl:choose>
                        <xsl:when
                                test=".//dim:field[@element='contributor'][@qualifier='author']">
                            <xsl:for-each
                                    select=".//dim:field[@element='contributor'][@qualifier='author']">
                                <xsl:choose>
                                    <xsl:when test="contains(., ',')">
                                        <xsl:call-template name="name-parse-reverse">
                                            <xsl:with-param name="name" select="node()"/>
                                        </xsl:call-template>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:call-template name="name-parse">
                                            <xsl:with-param name="name" select="node()"/>
                                        </xsl:call-template>
                                    </xsl:otherwise>
                                </xsl:choose>
                                <xsl:if
                                        test="count(following-sibling::dim:field[@element='contributor'][@qualifier='author']) != 0">
                                    <xsl:text>, </xsl:text>
                                </xsl:if>
                            </xsl:for-each>
                        </xsl:when>
                        <xsl:when test=".//dim:field[@element='creator']">
                            <xsl:for-each select=".//dim:field[@element='creator']">
                                <xsl:choose>
                                    <xsl:when test="contains(., ',')">
                                        <xsl:copy-of select="."/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:call-template name="name-parse">
                                            <xsl:with-param name="name" select="node()"/>
                                        </xsl:call-template>
                                    </xsl:otherwise>
                                </xsl:choose>
                                <xsl:if
                                        test="count(following-sibling::dim:field[@element='creator']) != 0">
                                    <xsl:text>, </xsl:text>
                                </xsl:if>
                            </xsl:for-each>
                        </xsl:when>
                        <xsl:when test=".//dim:field[@element='contributor']">
                            <xsl:for-each select=".//dim:field[@element='contributor']">
                                <xsl:choose>
                                    <xsl:when test="contains(., ',')">
                                        <xsl:copy-of select="."/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:call-template name="name-parse">
                                            <xsl:with-param name="name" select="node()"/>
                                        </xsl:call-template>
                                    </xsl:otherwise>
                                </xsl:choose>
                                <xsl:if
                                        test="count(following-sibling::dim:field[@element='contributor']) != 0">
                                    <xsl:text>, </xsl:text>
                                </xsl:if>
                            </xsl:for-each>
                        </xsl:when>
                    </xsl:choose>
                </span>
                <xsl:if test="dim:field[@element='date' and @qualifier='issued']">
                    <span class="pub-date">
                        <xsl:text> (</xsl:text>
                        <xsl:value-of
                                select="substring(dim:field[@element='date' and @qualifier='issued']/node(),1,4)"/>
                        <xsl:text>) </xsl:text>
                    </span>
                </xsl:if>
                <span class="artifact-title">
                    <xsl:choose>
                        <xsl:when test="dim:field[@element='title']">
                            <xsl:variable name="title" select="dim:field[@element='title'][1]"/>
                            <xsl:variable name="titleEndChar"
                                          select="substring($title, string-length($title), 1)"/>
                            <xsl:value-of select="$title"/>
                            <xsl:choose>
                                <xsl:when test="$titleEndChar != '.' and $titleEndChar != '?'">
                                    <xsl:text>. </xsl:text>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:text>&#160;</xsl:text>
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:when>
                        <xsl:otherwise>
                            <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </span>
                <xsl:if
                        test="dim:field[@element='publicationName' and @mdschema='prism']">
                    <span class="italics">
                        <xsl:value-of
                                select="dim:field[@element='publicationName' and @mdschema='prism']"/>
                    </span>
                    <xsl:text> </xsl:text>
                </xsl:if>
                <span>
                    <xsl:variable name="id"
                                  select="dim:field[@element='identifier'][not(@qualifier)][@mdschema='dc']"/>
                    <xsl:if test="$id[starts-with(., 'doi')]">
                        <xsl:value-of select="$id"/>
                    </xsl:if>
                </span>
            </xsl:element>
            <span class="Z3988">
                <xsl:attribute name="title">
                    <xsl:call-template name="renderCOinS"/>
                </xsl:attribute>
                &#160;
            </span>
        </div>
    </xsl:template>
    <!-- Here we construct Dryad's search results tabs; externally harvested
     collections are each given a tab.  Collection values of these collections
     (l3 for instance... this is just a code assigned by DSpace) are hard-coded
     so we need to make sure a collection has the same code across different
     Dryad installs (dev, demo, staging, production, etc.) -->
	<xsl:template match="dri:referenceSet[@type = 'summaryList']"
		priority="2">
		<xsl:apply-templates select="dri:head" />
		<!-- Here we decide whether we have a hierarchical list or a flat one -->
		<xsl:choose>
			<xsl:when
				test="descendant-or-self::dri:referenceSet/@rend='hierarchy' or ancestor::dri:referenceSet/@rend='hierarchy'">
				<ul>
					<xsl:apply-templates select="*[not(name()='head')]"
						mode="summaryList" />
				</ul>
			</xsl:when>
			<xsl:otherwise>
				<xsl:if test="$meta[@element='request'][@qualifier='URI'][.='discover']">
					<xsl:variable name="myTabSelected">
						<xsl:call-template name="parse-query-param">
							<xsl:with-param name="param-name">location</xsl:with-param>
						</xsl:call-template>
					</xsl:variable>
					<!--  The tabs display a selected tab based on the location
					parameter that is being used (see variable defined above) -->
					<div id="searchTabs">
						<ul>
							<xsl:element name="li">
								<xsl:if test="$myTabSelected != 'l4' and $myTabSelected != 'l5'">
									<xsl:attribute name="id">selected</xsl:attribute>
								</xsl:if>
								<xsl:element name="a">
									<xsl:attribute name="id">dryadResultsLink</xsl:attribute>
									<xsl:if test="$myTabSelected != 'l4' and $myTabSelected != 'l5'">
										<xsl:attribute name="style">font-weight: bold;</xsl:attribute>
									</xsl:if>
									<xsl:attribute name="title">Dryad Search Results</xsl:attribute>
									<xsl:call-template name="build-tab-link">
										<xsl:with-param name="identity-test"
											select="$myTabSelected != 'l4' and $myTabSelected != 'l5'" />
										<xsl:with-param name="location">l2</xsl:with-param>
									</xsl:call-template>
									<xsl:text>Dryad </xsl:text>
									<!--  We put the query string in a class attribute so our linked
									JavaScript can send that query to solr and get back a hit count
									to insert into this span -->
									<xsl:element name="span">
										<xsl:attribute name="id">dryadCount</xsl:attribute>
										<xsl:call-template name="build-query-class" />
										<script type="text/javascript" language="javascript"
											src="/themes/Dryad/lib/solr-common.js">&#160;</script>
										<script type="text/javascript" language="javascript"
											src="/themes/Dryad/lib/solr-dryad.js">&#160;</script>
									</xsl:element>
								</xsl:element>
							</xsl:element>
							<xsl:element name="li">
								<xsl:if test="$myTabSelected = 'l5'">
									<xsl:attribute name="id">selected</xsl:attribute>
								</xsl:if>
								<xsl:element name="a">
									<xsl:attribute name="id">tbResultsLink</xsl:attribute>
									<xsl:if test="$myTabSelected = 'l5'">
										<xsl:attribute name="style">font-weight: bold;</xsl:attribute>
									</xsl:if>
									<xsl:attribute name="title">TreeBASE: Phylogenetic data and trees</xsl:attribute>
									<xsl:call-template name="build-tab-link">
										<xsl:with-param name="identity-test" select="$myTabSelected = 'l5'" />
										<xsl:with-param name="location">l5</xsl:with-param>
									</xsl:call-template>
									<xsl:text>TreeBASE </xsl:text>
									<!--  We put the query string in a class attribute so our linked
									JavaScript can send that query to solr and get back a hit count
									to insert into this span -->
									<xsl:element name="span">
										<xsl:attribute name="id">tbCount</xsl:attribute>
										<xsl:call-template name="build-query-class" />
										<script type="text/javascript" language="javascript"
											src="/themes/Dryad/lib/solr-common.js">&#160;</script>
										<script type="text/javascript" language="javascript"
											src="/themes/Dryad/lib/solr-treebase.js">&#160;</script>
									</xsl:element>
								</xsl:element>
							</xsl:element>
							<xsl:element name="li">
								<xsl:if test="$myTabSelected = 'l4'">
									<xsl:attribute name="id">selected</xsl:attribute>
								</xsl:if>
								<xsl:element name="a">
									<xsl:attribute name="id">lterResultsLink</xsl:attribute>
									<xsl:if test="$myTabSelected = 'l4'">
										<xsl:attribute name="style">font-weight: bold;</xsl:attribute>
									</xsl:if>
									<xsl:attribute name="title">Knowledge Network for Biocomplexity: ecological and environmental data</xsl:attribute>
									<xsl:call-template name="build-tab-link">
										<xsl:with-param name="identity-test" select="$myTabSelected = 'l4'" />
										<xsl:with-param name="location">l4</xsl:with-param>
									</xsl:call-template>
									<xsl:text>KNB </xsl:text>
									<!--  We put the query string in a class attribute so our linked
									JavaScript can send that query to solr and get back a hit count
									to insert into this span -->
									<xsl:element name="span">
										<xsl:attribute name="id">lterCount</xsl:attribute>
										<xsl:call-template name="build-query-class" />
										<script type="text/javascript" language="javascript"
											src="/themes/Dryad/lib/solr-common.js">&#160;</script>
										<script type="text/javascript" language="javascript"
											src="/themes/Dryad/lib/solr-lter.js">&#160;</script>
									</xsl:element>
								</xsl:element>
							</xsl:element>
						</ul>
					</div>
				</xsl:if>
				<ul class="ds-artifact-list">
                    <xsl:choose>
                        <xsl:when test="$meta[@element='request'][@qualifier='URI'][.='submissions']">
                            <xsl:apply-templates select="*[not(name()='head')]"
                                mode="summaryNonArchivedList" />
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:apply-templates select="*[not(name()='head')]"
                                mode="summaryList" />
                        </xsl:otherwise>
                    </xsl:choose>
				</ul>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!--  Constructs the link in the search result tabs so a user can click
	on a tab and see the results returned from that externally harvested
	resource.  This hit count already displayed there gives them an idea
	of what to expect before they click to follow this link. -->
	<xsl:template name="build-tab-link">
		<xsl:param name="identity-test" />
		<xsl:param name="location" />
		<xsl:choose>
			<xsl:when test="$identity-test">
				<xsl:attribute name="href">
					<xsl:text>/discover?</xsl:text>
					<xsl:call-template name="get-query">
						<xsl:with-param name="without">page;location;submit</xsl:with-param>
					</xsl:call-template>
					<xsl:value-of select="concat('&amp;location=', $location)" />
				</xsl:attribute>
			</xsl:when>
			<xsl:otherwise>
				<xsl:attribute name="href">
					<xsl:text>/discover?</xsl:text>
					<xsl:call-template name="get-query">
						<xsl:with-param name="without">page;location;submit</xsl:with-param>
					</xsl:call-template>
					<xsl:value-of select="concat('&amp;location=', $location)" />
				</xsl:attribute>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!--  This template constructs the value of the class attribute that is
	used from the JavaScript to query solr for the number of hits found
	from this externally harvested resource. It's helping to convert DSpace
	query syntax to Solr query syntax (which the JavaScript also does). -->
	<xsl:template name="build-query-class">
		<xsl:variable name="query-param">
			<xsl:call-template name="parse-query-param">
				<xsl:with-param name="param-name">query</xsl:with-param>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="fq-params">
			<xsl:call-template name="parse-query-param">
				<xsl:with-param name="param-name">fq</xsl:with-param>
				<xsl:with-param name="param-prefix">&amp;fq=</xsl:with-param>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="filter-params">
			<xsl:call-template name="parse-query-param">
				<xsl:with-param name="param-name">filter</xsl:with-param>
				<xsl:with-param name="param-prefix">&amp;fq=</xsl:with-param>
			</xsl:call-template>
		</xsl:variable>
		<xsl:attribute name="class">
			<xsl:value-of
			select="concat('q=', encoder:encode($query-param), $fq-params, $filter-params)" />
		</xsl:attribute>
	</xsl:template>


    <xsl:template match="dri:reference" mode="summaryNonArchivedList">
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
        <xsl:comment> External Metadata URL: <xsl:value-of select="$externalMetadataURL"/> </xsl:comment>
        <li>
            <xsl:attribute name="class">
                <xsl:text>ds-artifact-item </xsl:text>
                <xsl:choose>
                    <xsl:when test="position() mod 2 = 0">even</xsl:when>
                    <xsl:otherwise>odd</xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            <xsl:apply-templates select="document($externalMetadataURL)" mode="summaryNonArchivedList"/>
            <xsl:apply-templates />
        </li>
    </xsl:template>

    <xsl:template match="mets:METS[mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']]" mode="summaryNonArchivedList">
        <xsl:choose>
            <xsl:when test="@LABEL='DSpace Item'">
                <xsl:call-template name="summaryNonArchivedList-DIM"/>
            </xsl:when>
            <xsl:when test="@LABEL='DSpace Collection'">
                <xsl:call-template name="collectionSummaryList-DIM"/>
            </xsl:when>
            <xsl:when test="@LABEL='DSpace Community'">
                <xsl:call-template name="communitySummaryList-DIM"/>
            </xsl:when>
            <xsl:otherwise>
                <i18n:text>xmlui.dri2xhtml.METS-1.0.non-conformant</i18n:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="summaryNonArchivedList-DIM">
        <!-- Generate the info about the item from the metadata section -->
        <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim"
            mode="summaryNonArchivedList-DIM"/>
        <!-- Generate the thunbnail, if present, from the file section -->
        <xsl:apply-templates select="./mets:fileSec" mode="artifact-preview"/>
    </xsl:template>



</xsl:stylesheet>
