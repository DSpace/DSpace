<?xml version="1.0"?>
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns="http://di.tamu.edu/DRI/1.0/"
                xmlns:dri="http://di.tamu.edu/DRI/1.0/"
                xmlns:h="http://apache.org/cocoon/request/2.0"
                xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
                xmlns:atm="http://www.atmire.com/functions"
        >

    <!--
       Using Cocoon Internal Pipeline to ease processing solr response
       parameters, cocoon://rawdiscovery is a separate internal pipeline defined in
       aspects/Discovery/sitemap.xmap
    -->
    <xsl:variable name="solr-results" select="document('cocoon://rawdiscovery')"/>

    <!--
       Using Cocoon Internal Pipeline to ease processing original request
       parameters, cocoon://request is a separate internal pipeline defined in
       aspects/Discovery/sitemap.xmap you can reuse this elsewhere...
    -->
    <xsl:variable name="request" select="document('cocoon://request')"/>

    <!-- Number of Records Per Page -->
    <xsl:variable name="rows">
        <xsl:choose>
            <xsl:when
                    test="$request/h:request/h:requestParameters/h:parameter[@name = 'rows']/h:value[1]">
                <xsl:value-of
                        select="$request/h:request/h:requestParameters/h:parameter[@name = 'rows']/h:value[1]"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:text>10</xsl:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>

    <!-- Time Solr Calculated the Query within -->
    <xsl:variable name="time"
                  select="$solr-results//lst[@name='responseHeader']/int[@name='QTime']/text()"/>

    <!-- Number of Records total found by Solr -->
    <xsl:variable name="numFound" select="$solr-results/response/result/@numFound"/>

    <!-- Starting record for this current page of results -->
    <xsl:variable name="start" select="$solr-results/response/result/@start"/>

    <!-- total number of pages for paging option -->
    <xsl:variable name="pages" select="ceiling($numFound div $rows)"/>

    <!-- current page number -->
    <xsl:variable name="currentPage" select="ceiling(($start + 1) div $rows)"/>

    <!-- original query -->
    <xsl:variable name="q" select="$request/h:request/h:requestParameters/h:parameter[@name = 'q']/h:value"/>


    <!-- XSLT 2.0 function for figuring out the grouping type -->
  <xsl:function name="atm:group-type">
    <xsl:param name="type"/>
    <xsl:choose><xsl:when test="$type = 3 or $type = 4">
         <xsl:value-of select="2"/>
    </xsl:when><xsl:otherwise>
         <xsl:value-of select="3"/>
    </xsl:otherwise></xsl:choose>
  </xsl:function>


    <xsl:template match="dri:body">
        <xsl:copy>
            <xsl:copy-of select="@*"/>

            <div rend="primary" n="search" id="aspect.artifactbrowser.SimpleSearch.div.search">
                <head><i18n:text catalogue="default">xmlui.ArtifactBrowser.SimpleSearch.head</i18n:text></head>
                <div interactive="yes" rend="secondary search" action="search" n="general-query" method="get" id="aspect.artifactbrowser.SimpleSearch.div.general-query">


                <list type="form" n="search-query" id="aspect.artifactbrowser.SimpleSearch.list.search-query">


                    <!-- Insert context of search later
                    <dri:item>
                        <dri:field type="select" n="scope" id="aspect.artifactbrowser.SimpleSearch.field.scope">
                            <dri:params/>
                            <dri:label>Search Scope</dri:label>
                            <dri:option returnValue="/">All of DSpace</dri:option>
                            <dri:option returnValue="1721.1/39118">Abdul Latif Jameel Poverty Action Lab (J-PAL)</dri:option>
                            <dri:value type="option" option="/"/>
                        </dri:field>
                    </dri:item>
                    -->

                    <item>
                        <field type="text" n="q" id="aspect.artifactbrowser.SimpleSearch.field.query">
                            <params/>
                            <label>Full Text Search</label>
                            <value type="raw">
                                <xsl:value-of select="$q"/>
                            </value>
                        </field>
                    </item>


                    <!--  Render selected Facets as form fields that can be selected/deselected -->
                    <xsl:if test="$request/h:request/h:requestParameters/h:parameter[@name = 'fq']">
                        <item>
                            <field type="checkbox" n="fq">
                                <label>Results narrowe by:</label>
                                <params/>
                                <!-- add checkboxes -->
                                <xsl:for-each
                                        select="$request/h:request/h:requestParameters/h:parameter[@name = 'fq']/h:value">
                                    <option returnValue="{.}">
                                        <xsl:value-of select="."/>
                                    </option>
                                </xsl:for-each>
                                <!-- identify selected checkboxes -->
                                <xsl:for-each
                                        select="$request/h:request/h:requestParameters/h:parameter[@name = 'fq']/h:value">
                                    <value type="option" option="{.}"/>
                                </xsl:for-each>
                            </field>
                        </item>
                    </xsl:if>
                </list>

                <!--
                <xsl:if test="lst[@name='facet_counts']/lst[@name='facet_fields']/lst">
                <h3>Narow Search</h3>
                <dl id="filterBy">
                    <xsl:apply-templates select="lst[@name='facet_counts']/lst[@name='facet_fields']/lst" />
                </dl>
                </xsl:if>
                -->


                <table cols="3" rows="1" n="search-controls"
                       id="aspect.artifactbrowser.SimpleSearch.table.search-controls">
                    <row role="data">
                        <cell>Results/page
                            <field type="select" n="rpp" id="aspect.artifactbrowser.SimpleSearch.field.rpp">
                                <params/>
                                <option returnValue="5">5</option>
                                <option returnValue="10">10</option>
                                <option returnValue="20">20</option>
                                <option returnValue="40">40</option>
                                <option returnValue="60">60</option>
                                <option returnValue="80">80</option>
                                <option returnValue="100">100</option>
                                <value type="option" option="10"/>
                            </field>
                        </cell>
                        
                        <!-- TODO: Need to implment sort in Solr and configuration -->

                        <cell>Sort items by
                            <field type="select" n="sort_by" id="aspect.artifactbrowser.SimpleSearch.field.sort_by">
                                <params/>
                                <option returnValue="0">relevance</option>
                                <option returnValue="3">submit date</option>
                                <option returnValue="2">issue date</option>
                                <option returnValue="1">title</option>
                            </field>
                        </cell>
                        <cell>in order
                            <field type="select" n="order" id="aspect.artifactbrowser.SimpleSearch.field.order">
                                <params/>
                                <option returnValue="ASC">ascending</option>
                                <option returnValue="DESC">descending</option>
                                <value type="option" option="DESC"/>
                            </field>
                        </cell>
                    </row>
                </table>
                <p rend="button-list">
                    <field type="button" n="submit" id="aspect.artifactbrowser.SimpleSearch.field.submit">
                        <params/>
                        <value type="raw">Go</value>
                    </field>
                </p>
            </div>
            <!-- print the rest of the templates -->

                <xsl:if test="$solr-results/response/*">
                    <xsl:apply-templates select="$solr-results/response" mode="solr"/>
                </xsl:if>

                <xsl:apply-templates/>

              </div>
        </xsl:copy>
    </xsl:template>


    <xsl:template match="dri:options">
        <xsl:copy>
            <xsl:copy-of select="@*"/>
            <!--  merge options (still needs deduplication) -->
            <xsl:apply-templates select="$solr-results/response/lst[@name='facet_counts']/lst[@name='facet_fields']"/>
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>


    <xsl:template match='response' mode="solr">

        <p rend="result-query" n="result-query" id="aspect.artifactbrowser.SimpleSearch.p.result-query">
            <i18n:translate>
                <i18n:text catalogue="default">xmlui.ArtifactBrowser.AbstractSearch.result_query</i18n:text>
                <i18n:param><xsl:value-of select='$q'/></i18n:param>
                <i18n:param><xsl:value-of select='$numFound'/></i18n:param>
            </i18n:translate>
        </p>

        <div
                rend="primary"
                itemsTotal="{$numFound}"
                firstItemIndex="1"
                pageURLMask="discovery?rpp={$rows}&amp;etal=0&amp;query={$q}&amp;page={$currentPage}&amp;order=DESC&amp;sort_by=0"
                pagination="masked"
                lastItemIndex="{$rows}"
                n="search-results"
                currentPage="{$currentPage}"
                pagesTotal="{$pages}"
                id="aspect.artifactbrowser.SimpleSearch.div.search-results">
                <head>
                    <i18n:text catalogue="default">xmlui.ArtifactBrowser.AbstractSearch.head1_none</i18n:text>
                </head>

                <!-- XSLT 2.0 allows us to Group by DSpaceObject type when rendering... -->
                <xsl:for-each-group select="result/doc" group-by="atm:group-type(./int[@name = 'search.resourcetype'])">


                    <referenceSet rend="repository-search-results" type="summaryList" n="search-results-repository"
                                  id="aspect.artifactbrowser.SimpleSearch.referenceSet.search-results-repository">
                    <head>
                        <i18n:text catalogue="default">xmlui.ArtifactBrowser.AbstractSearch.head<xsl:value-of select="current-grouping-key()"/></i18n:text>
                    </head>
                                                   <!-- current-group()/@size -->

                    <xsl:for-each select="current-group()">

                        <xsl:comment><xsl:copy-of select="."/></xsl:comment>

                         <xsl:apply-templates select="."/>


                    </xsl:for-each>

                    </referenceSet>

                </xsl:for-each-group>
            </div>

    </xsl:template>


    <!-- Toss in a page title etc -->
    <xsl:template match="dri:pageMeta" xmlns="http://di.tamu.edu/DRI/1.0/">
        <xsl:copy>
            <xsl:copy-of select="@*"/>
            <xsl:apply-templates/>
            <metadata element="title">Discovery<!-- TODO: needs i18n --></metadata>
            <trail target="/xmlui/">DSpace Home<!-- TODO: needs i18n --></trail>
            <trail>Discovery<!-- TODO: needs i18n --></trail>
        </xsl:copy>
    </xsl:template>

    <xsl:template name="get-type">
        <xsl:choose>
            <xsl:when test="number(int[@name='search.resourcetype']/text()) eq 2">
                <xsl:text>DSpace Item</xsl:text>
            </xsl:when>
            <xsl:when test="number(int[@name='search.resourcetype']/text()) eq 3">
                <xsl:text>DSpace Collection</xsl:text>
            </xsl:when>
            <xsl:when test="number(int[@name='search.resourcetype']/text()) eq 4">
                <xsl:text>DSpace Community</xsl:text>
            </xsl:when>
        </xsl:choose>
    </xsl:template>

    <!-- Templates below render solr results to DRI -->
    <xsl:template match="doc">
        <reference
                repositoryID="123456789"
                type="DSpace Item"
                ds-type="{*[@name = 'search.resourcetype']}"
                url="/metadata/handle/{str[@name = 'handle']}/mets.xml"
                >
            <xsl:attribute name="type">
                <xsl:call-template name="get-type"/>
            </xsl:attribute>


        </reference>
        <xsl:comment>
            <xsl:copy-of select="."/>
        </xsl:comment>
    </xsl:template>


    <!-- Construct Facet Options List -->
    <xsl:template match="response/lst[@name='facet_counts']/lst[@name='facet_fields']">
        <list n="filters" id="aspect.administrative.Navigation.list.filter">
            <head>Filters</head>
            <xsl:for-each select="lst">

                <list n="context" id="solr.Navigation.list.{@name}">
                    <head>By <xsl:value-of select="@name"/>:</head>
                    <xsl:for-each select="*">
                        <xsl:variable name="facet" select="../@name"/>
                        <xsl:variable name="facetEsc" select="utils:escapeQueryChars($facet)"
                                      xmlns:utils="java:org.apache.solr.client.solrj.util.ClientUtils"/>
                        <xsl:variable name="value" select="@name"/>
                        <xsl:variable name="valueEsc" select="utils:escapeQueryChars($value)"
                                      xmlns:utils="java:org.apache.solr.client.solrj.util.ClientUtils"/>
                        <xsl:variable name="count" select="."/>
                        <xsl:variable name="full_value" select="concat($facetEsc,':',$valueEsc)"/>

                        <!-- Show only facet values that are not selected. -->
                        <xsl:choose>
                            <xsl:when
                                    test="not($request/h:request/h:requestParameters/h:parameter[@name = 'fq'][h:value = $full_value])">
                                <item>
                                    <xref target="/community-list">
                                        <xsl:attribute name="target">
                                            <xsl:value-of select="concat('?fq=',encode-for-uri($full_value))"/>
                                            <xsl:for-each
                                                    select="$request/h:request/h:requestParameters/h:parameter[@name != 'start']/h:value">
                                                <xsl:if test="string(.) != $full_value">
                                                    <xsl:value-of select="concat('&amp;', ../@name,'=' , .)"/>
                                                </xsl:if>
                                            </xsl:for-each>
                                        </xsl:attribute>
                                        <xsl:value-of select="$value"/> (<xsl:value-of select="$count"/>)
                                    </xref>
                                </item>

                            </xsl:when>
                            <xsl:otherwise>
                                <item>
                                    <xsl:value-of select="$value"/> (<xsl:value-of select="$count"/>)
                                </item>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:for-each>

                </list>
            </xsl:for-each>
        </list>
    </xsl:template>

    <!--
    Copy all templates
     -->
    <xsl:template match="*">
        <xsl:copy>
            <xsl:copy-of select="@*"/>
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>