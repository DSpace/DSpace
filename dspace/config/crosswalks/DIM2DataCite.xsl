<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : DIM2DataCite.xsl
    Created on : January 23, 2013, 1:26 PM
    Author     : pbecker
    Description: Converts metadata from DSpace Intermediat Format (DIM) into
                 metadata following the DataCite Schema for the Publication and
                 Citation of Research Data.
-->
<!-- 
     TODO: Check the used metadata and adds properties that are defined optional
           by DataCite. The current version of this XSL is just to test
           registration of DOIs.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:dspace="http://www.dspace.org/xmlns/dspace/dim"
                xmlns="http://datacite.org/schema/kernel-2.2"
                version="1.0">
    <xsl:output method="xml" indent="yes" encoding="utf-8" />
    
    <!-- Don't copy everything by default! -->
    <xsl:template match="@* | text()" />
    
    <xsl:template match="/dspace:dim[@dspaceType='ITEM']">
        <!--
            org.dspace.identifier.doi.DataCiteRegistryAgency uses this XSLT to
            transform metadata for the DataCite metadata store. This crosswalk
            should only be used, when it is ensured that all mandatory
            properties are in the metadata of the item to export.
            The classe named above respects this.
        -->
        <resource xmlns="http://datacite.org/schema/kernel-2.2"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://datacite.org/schema/kernel-2.2 http://schema.datacite.org/meta/kernel-2.2/metadata.xsd">

            <!-- calls the template for the doi identifier. -->            
            <xsl:apply-templates select="//dspace:field[@mdschema='dc' and @element='identifier' and starts-with(., 'doi')]" />

            <!-- Add Title information. -->
            <xsl:if test="//dspace:field[@mdschema='dc' and @element='title']">
                <titles>
                    <xsl:apply-templates select="//dspace:field[@mdschema='dc' and @element='title']" />
                </titles>
            </xsl:if>

            <!-- Add creator information. -->            
            <xsl:if test="//dspace:field[@mdschema='dc' and @element='contributor' and @qualifier='author']">
                <creators>
                    <xsl:apply-templates select="//dspace:field[@mdschema='dc' and @element='contributor' and @qualifier='author']" />
                </creators>
            </xsl:if>
            
            <!-- FIXME -->
            <publisher>Technische Universität Berlin</publisher>
            
            
            <!-- FIXME -->
            <publicationYear>
                <xsl:choose>
                    <xsl:when test="//dspace:field[@mdschema='dc' and @element='date' and @qualifier='issued']">
                        <xsl:value-of select="substring(//dspace:field[@mdschema='dc' and @element='date' and @qualifier='issued'], 1, 4)" />
                    </xsl:when>
                    <xsl:when test="//dspace:field[@mdschema='dc' and @element='date' and @qualifier='available']">
                        <xsl:value-of select="substring(//dspace:field[@mdschema='dc' and @element='date' and @qualifier='issued'], 1, 4)" />
                    </xsl:when>
                    <xsl:when test="//dspace:field[@mdschema='dc' and @element='date']">
                        <xsl:value-of select="substring(//dspace:field[@mdschema='dc' and @element='date'], 1, 4)" />
                    </xsl:when>
                    <xsl:otherwise>0000</xsl:otherwise>
                </xsl:choose>
            </publicationYear>
            
            <!-- OPTIONAL PROPERTIES -->
            <!-- Add alternativeIdentifiers. -->
            <xsl:if test="//dspace:field[@mdschema='dc' and @element='identifier' and not(starts-with(., 'doi:'))]">
                <xsl:element name="alternateIdentifiers">
                    <xsl:apply-templates select="//dspace:field[@mdschema='dc' and @element='identifier' and not(starts-with(., 'doi:'))]" />
                </xsl:element>
            </xsl:if>

        </resource>
    </xsl:template>
    
    <!-- Add doi identifier information. -->
    <xsl:template match="dspace:field[@mdschema='dc' and @element='identifier' and starts-with(., 'doi')]">
        <Identifier identifierType="DOI">
            <xsl:value-of select="substring(., 5)"/>
        </Identifier>
    </xsl:template>
    
    <xsl:template match="//dspace:field[@mdschema='dc' and @element='identifier' and not(starts-with(., 'doi:'))]">
        <xsl:element name="alternateIdentifier">
            <xsl:if test="@qualifier">
                <xsl:attribute name="alternateIdentifierType"><xsl:value-of select="@qualifier" /></xsl:attribute>
            </xsl:if>
            <xsl:value-of select="." />
        </xsl:element>
    </xsl:template>
    
    <xsl:template match="dspace:field[@mdschema='dc' and @element='title']">
        <xsl:element name="title">
            <xsl:if test="@qualifier='alternative'">
                <xsl:attribute name="titleTye">AlternativeTitle</xsl:attribute>
            </xsl:if>
            <xsl:value-of select="." />
        </xsl:element>
    </xsl:template>

    <xsl:template match="//dspace:field[@mdschema='dc' and @element='contributor' and @qualifier='author']">
        <creator>
            <creatorName>
                <xsl:value-of select="." />
            </creatorName>
        </creator>
    </xsl:template>
    
</xsl:stylesheet>


        <!-- Test if item contains a DOI.
            <xsl:if test="starts-with(//dspace:field[@mdschema='dc' and @element='identifier'], 'doi:')">
        -->
        <!-- Test if item contains a creator.
            In der DSpace-Tabelle metadatafieldregistry steht als commentar "Do not use; only for harvested metadata. Was hat es damit auf sich?
            <xsl:if test="//dspace:field[@mdschema='dc' and @element='creator']">
        -->
        <!-- Test if item contains a title. 
            <xsl:if test="//dspace:field[@mdschema='dc' and @element='title']">
        -->
        <!-- Test if item contains a publisher. 
            <xsl:if test="//dspace:field[@mdschema='dc' and @element='publisher']">
        -->
        <!-- Test if item contains a publication year.
             We should use (in order of priority) date of embargo, dc.date.copyright or dc.date.
            <xsl:if test="//dspace:field[@mdschema='dc' and @element='date']">
        -->
