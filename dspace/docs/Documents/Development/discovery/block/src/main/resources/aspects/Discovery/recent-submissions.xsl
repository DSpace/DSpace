<?xml version="1.0"?>
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:dri="http://di.tamu.edu/DRI/1.0/"
                xmlns:h="http://apache.org/cocoon/request/2.0"
                xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
        >

    <xsl:param name="type" select="string('community')"/>

    <xsl:param name="viewer" select="string('CommunityViewer')"/>


    <!--
       Using Cocoon Internal Pipeline to ease processing solr response
       parameters, cocoon://rawdiscovery is a separate internal pipeline defined in
       aspects/Discovery/sitemap.xmap
    -->
    <xsl:variable name="solr-results" select="document('cocoon://rawdiscovery?rows=5&amp;q=*:* and search.resourcetype:2')"/>

    <xsl:template match="dri:body">
        <xsl:copy>
            <xsl:copy-of select="@*"/>
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="dri:div[contains(@rend,'primary')]">
        <xsl:copy>
            <xsl:copy-of select="@*"/>
            <xsl:apply-templates select="*[@id != 'aspect.artifactbrowser.CommunityViewer.div.community-recent-submission']"/>
            <!--
               render recent submissions
            -->
            <xsl:if test="$solr-results/response/*">
                <xsl:apply-templates select="$solr-results/response" mode="solr"/>
            </xsl:if>
        </xsl:copy>
    </xsl:template>
                        <!--
     n="community-home" id="solr.CommunityViewer.div.community-home">"
    <xsl:template match="dri:div[@id='aspect.artifactbrowser.CommunityViewer.div.community-recent-submission']"/>
    <xsl:template match="dri:div[@id='aspect.artifactbrowser.CollectionViewer.div.collection-recent-submission']"/>


    <xsl:template match="dri:div[@id='aspect.artifactbrowser.CommunityViewer.referenceSet.collection-last-submitted']"/>
                             -->
    <xsl:template match='response' mode="solr">

         <div xmlns="http://di.tamu.edu/DRI/1.0/"
                rend="secondary recent-submission"
                n="community-recent-submission"
                id="aspect.artifactbrowser.CommunityViewer.div.community-recent-submission">
               <referenceSet
                        rend="recent-submissions"
                        type="summaryList"
                        n="collection-last-submitted"
                        id="aspect.artifactbrowser.CommunityViewer.referenceSet.collection-last-submitted">
                   <head><i18n:text catalogue="default">xmlui.ArtifactBrowser.CommunityViewer.head_recent_submissions</i18n:text></head>
                                                               
                        <xsl:for-each select="result/doc">
                            <!-- TODO: correct repositoryID and type of item -->
                            <reference
                                    repositoryID="123456789"
                                    type="DSpace Item"
                                    url="/metadata/handle/{str[@name = 'handle']}/mets.xml"/>
                               <xsl:comment>
                                   <xsl:copy-of select="."/> <!-- TODO: for debugging -->
                               </xsl:comment>
                        </xsl:for-each>
                </referenceSet>
        </div>

    </xsl:template>

    <!--
<div rend="secondary recent-submission" n="community-recent-submission" id="aspect.artifactbrowser.CommunityViewer.div.community-recent-submission">
<head>
<i18n:text catalogue="default">xmlui.ArtifactBrowser.CommunityViewer.head_recent_submissions</i18n:text>
</head>
<referenceSet rend="recent-submissions" type="summaryList" n="collection-last-submitted" id="aspect.artifactbrowser.CommunityViewer.referenceSet.collection-last-submitted">
<reference repositoryID="1721.1" type="DSpace Item" url="/metadata/handle/1721.1/46090/mets.xml"/>
<reference repositoryID="1721.1" type="DSpace Item" url="/metadata/handle/1721.1/45949/mets.xml"/>
<reference repositoryID="1721.1" type="DSpace Item" url="/metadata/handle/1721.1/45948/mets.xml"/>
<reference repositoryID="1721.1" type="DSpace Item" url="/metadata/handle/1721.1/45382/mets.xml"/>
<reference repositoryID="1721.1" type="DSpace Item" url="/metadata/handle/1721.1/45212/mets.xml"/>
</referenceSet>
</div>


    -->

    <xsl:template match="response/lst[@name='facet_counts']/lst[@name='facet_fields']">
        <dri:list n="filters" id="aspect.administrative.Navigation.list.filter">
            <dri:head>Filters</dri:head>
            <xsl:for-each select="lst">

                <dri:list n="context" id="solr.Navigation.list.{@name}">
                    <dri:head>By <xsl:value-of select="@name"/>:
                    </dri:head>
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
                                <dri:item>
                                    <dri:xref target="/community-list">
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
                                    </dri:xref>
                                </dri:item>

                            </xsl:when>
                            <xsl:otherwise>
                                <dri:item>
                                    <xsl:value-of select="$value"/> (<xsl:value-of select="$count"/>)
                                </dri:item>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:for-each>

                </dri:list>
            </xsl:for-each>
        </dri:list>
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