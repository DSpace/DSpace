<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : DIM2DataCite.xsl
    Created on : January 23, 2013, 1:26 PM
    Author     : pbecker, ffuerste
    Description: Converts metadata from DSpace Intermediat Format (DIM) into
                 metadata following the DataCite Schema for the Publication and
                 Citation of Research Data.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:dspace="http://www.dspace.org/xmlns/dspace/dim"
                xmlns="http://datacite.org/schema/kernel-2.2"
                version="1.0">
    
    <!-- CONFIGURATION
         TODO: better documentation -->
    <xsl:variable name="publisher">Technische Universität Berlin</xsl:variable>
    <!-- should we use the attribute select instead of a content of the node?
         Like this it could be better understood by normal users that do not know xsl so well. -->
    <xsl:variable name="datamanager"><xsl:value-of select="$publisher" /></xsl:variable>
    <xsl:variable name="hostinginstitution"><xsl:value-of select="$publisher" /></xsl:variable>
    
    
    <!-- DO NOT CHANGE ANYTHING BELOW THIS LINE EXCEPT YOU REALLY KNOW WHAT YOU ARE DOING! -->
    
    <xsl:output method="xml" indent="yes" encoding="utf-8" />
    
    <!-- Don't copy everything by default! -->
    <xsl:template match="@* | text()" />
    
    <xsl:template match="/dspace:dim[@dspaceType='ITEM']">
        <!--
            org.dspace.identifier.doi.DataCiteConnector uses this XSLT to
            transform metadata for the DataCite metadata store. This crosswalk
            should only be used, when it is ensured that all mandatory
            properties are in the metadata of the item to export.
            The classe named above respects this.
        -->
        <resource xmlns="http://datacite.org/schema/kernel-2.2"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="http://datacite.org/schema/kernel-2.2 http://schema.datacite.org/meta/kernel-2.2/metadata.xsd">

            <!-- MANDATORY PROPERTIES -->
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
            
            <!-- DataCite (4) :: Publisher -->
            <publisher><xsl:value-of select="$publisher" /></publisher>

            <!-- DataCite (5) :: PublicationYear -->
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

            <!-- 
                 Add alternativeIdentifiers.
                 This element is important as it is used to recognize for which
                 DSpace object a DOI is reserved for. See below for further 
                 information.
            -->
            <xsl:if test="//dspace:field[@mdschema='dc' and @element='identifier' and not(starts-with(., 'doi:'))]">
                <xsl:element name="alternateIdentifiers">
                    <xsl:apply-templates select="//dspace:field[@mdschema='dc' and @element='identifier' and not(starts-with(., 'doi:'))]" />
                </xsl:element>
            </xsl:if>

            <!-- Add subjects. -->
            <xsl:if test="//dspace:field[@mdschema='dc' and @element='subject']">
                <subjects>
                    <xsl:apply-templates select="//dspace:field[@mdschema='dc' and @element='subject']" />
                </subjects>
            </xsl:if>

            <!-- Add contributors. -->
            <contributors>
                <xsl:element name="contributor">
                    <xsl:attribute name="contributorType">DataManager</xsl:attribute>
                    <xsl:element name="contributorName">
                        <xsl:value-of select="$datamanager"/>
                    </xsl:element>    
                </xsl:element>
                <xsl:element name="contributor">
                    <xsl:attribute name="contributorType">HostingInstitution</xsl:attribute>
                    <contributorName>
                        <xsl:value-of select="$hostinginstitution" />
                    </contributorName>
                </xsl:element>
                <!-- TODO: check if this also matches //dspace:field[@mdschema='dc' and @element='contributor' and qualifier='editor'] -->
                <xsl:apply-templates select="//dspace:field[@mdschema='dc' and @element='contributor']" />
            </contributors>

            <!-- Add dates. -->
            <dates>                
                <xsl:apply-templates select="//dspace:field[@mdschema='dc' and @element='date']" />       
            </dates>

            <!-- Add language(s). -->              
            <xsl:apply-templates select="//dspace:field[@mdschema='dc' and @element='language' and @qualifier='iso']" />       

            <!-- Add resource type. -->              
            <xsl:apply-templates select="//dspace:field[@mdschema='dc' and @element='type']" />       

            <!-- Add size. -->              
            
            <!-- Add format. -->

            <!-- Add version. --> 
            <xsl:apply-templates select="//dspace:field[@mdschema='dc' and @element='description' and @qualifier='provenance']" />     

            <!-- Add rights. -->

            <!-- Add description. -->    
            <xsl:apply-templates select="//dspace:field[@mdschema='dc' and @element='description'][not(@qualifier='provenance')]" />       
        </resource>
    </xsl:template>
    
    <!-- Add doi identifier information. -->
    <xsl:template match="dspace:field[@mdschema='dc' and @element='identifier' and starts-with(., 'doi')]">
        <Identifier identifierType="DOI">
            <xsl:value-of select="substring(., 5)"/>
        </Identifier>
    </xsl:template>
    
    <!-- Add all identifiers except the doi. -->
    <!--
        This element is important as it is used to recognize for which DSpace
        objet a DOI is reserved for. The DataCiteConnector will test all
        AlternativeIdentifiers by using HandleManager.
        resolveUrlToHandle(context, altId) until one is recognized or all have
        been tested.
    -->
    <xsl:template match="//dspace:field[@mdschema='dc' and @element='identifier' and not(starts-with(., 'doi:'))]">
        <xsl:element name="alternateIdentifier">
            <xsl:if test="@qualifier">
                <xsl:attribute name="alternateIdentifierType"><xsl:value-of select="@qualifier" /></xsl:attribute>
            </xsl:if>
            <xsl:value-of select="." />
        </xsl:element>
    </xsl:template>

    <!-- DataCite (2) :: Creator -->
    <xsl:template match="//dspace:field[@mdschema='dc' and @element='contributor' and @qualifier='author']">
        <creator>
            <!-- DataCite (2.1) :: creatorName -->
            <creatorName>
                <xsl:value-of select="." />
            </creatorName>
        </creator>
    </xsl:template>

    <!-- DataCite (3) :: Title -->
    <xsl:template match="dspace:field[@mdschema='dc' and @element='title']">
        <xsl:element name="title">
            <!-- DataCite (3.1) :: titleType -->
            <xsl:if test="@qualifier='alternative'">
                <xsl:attribute name="titleType">AlternativeTitle</xsl:attribute>
            </xsl:if>
            <xsl:value-of select="." />
        </xsl:element>
    </xsl:template>

    <!-- DataCite (6) :: Subject -->
    <!--
        "This term is intended to be used with non-literal values as defined in the 
        DCMI Abstract Model (http://dublincore.org/documents/abstract-model/). 
        As of December 2007, the DCMI Usage Board is seeking a way to express 
        this intention with a formal range declaration." 
        (http://dublincore.org/documents/dcmi-terms/#terms-subject)
    -->
    <xsl:template match="//dspace:field[@mdschema='dc' and @element='subject']">
        <xsl:element name="subject">
            <xsl:if test="@qualifier">
                <xsl:attribute name="subjectScheme"><xsl:value-of select="@qualifier" /></xsl:attribute>
            </xsl:if>
            <xsl:value-of select="." />
        </xsl:element>
    </xsl:template>

    <!-- DataCite (7) :: Contributor -->
    <xsl:template match="//dspace:field[@mdschema='dc' and @element='contributor' and @qualifier='editor']">
        <xsl:element name="contributor">            
            <!-- DataCite (6.1) :: contributorType -->            
            <xsl:attribute name="contributorType">Editor</xsl:attribute>            
            <contributorName>
                <xsl:value-of select="." />
            </contributorName>
        </xsl:element>
    </xsl:template>
        

    <!-- DataCite (8) :: Date -->
    <xsl:template match="//dspace:field[@mdschema='dc' and @element='date']">
        <xsl:for-each select=".">
            <xsl:element name="date">            
                <!-- DataCite (8.1) :: dateType -->
                <xsl:if test="@qualifier='accessioned'">
                    <xsl:attribute name="dateType">Issued</xsl:attribute>            
                </xsl:if>
                <xsl:if test="@qualifier='available'">
                    <xsl:attribute name="dateType">Available</xsl:attribute>            
                </xsl:if>
                <xsl:if test="@qualifier='issued'">
                    <xsl:attribute name="dateType">Created</xsl:attribute>            
                </xsl:if>
                <xsl:value-of select="substring(., 1, 10)" />      
            </xsl:element>
        </xsl:for-each>
    </xsl:template>

    <!-- DataCite (9) :: Language -->
    <xsl:template match="//dspace:field[@mdschema='dc' and @element='language' and @qualifier='iso']">
        <xsl:for-each select=".">
            <!-- Transforming the language flags according to ISO 639-2/B & ISO 639-3 -->
            <xsl:element name="language">
                <xsl:choose>           
                    <xsl:when test="string(text())='en'">eng</xsl:when>
                    <xsl:when test="string(text())='de'">ger</xsl:when>
                    <xsl:when test="string(text())='fr'">fre</xsl:when>
                    <xsl:otherwise><xsl:value-of select="." /></xsl:otherwise>
                </xsl:choose>
            </xsl:element>
        </xsl:for-each>
    </xsl:template>

    <!-- DataCite (10) :: resourceType -->
    <!-- too inconsistent usage: <xsl:template match="//dspace:field[@mdschema='dc' and @element='description'][not(@qualifier)]">-->
        <xsl:template match="//dspace:field[@mdschema='dc' and @element='type']">
        <xsl:for-each select=".">
            <!-- Transforming the language flags according to ISO 639-2/B & ISO 639-3 -->
            <xsl:element name="ResourceType">
                <!--
                <xsl:variable name="s" select="." />
                <xsl:value-of select="substring($s,  string-length($s) - 7)" />
                -->

                <!-- TODO :: DataCite (10.1) :: resourceTypeGeneral -->
                <!-- Just a hack - could be well optimized - mappings also need a check -->
                <xsl:attribute name="resourceTypeGeneral">
                    <xsl:choose>           
                        <xsl:when test="string(text())='Animation'">Image</xsl:when>
                        <xsl:when test="string(text())='Article'">Text</xsl:when>
                        <xsl:when test="string(text())='Book'">Text</xsl:when>
                        <xsl:when test="string(text())='Book chapter'">Text</xsl:when>
                        <xsl:when test="string(text())='Dataset'">Dataset</xsl:when>
                        <xsl:when test="string(text())='Learning Object'">???</xsl:when>
                        <xsl:when test="string(text())='Image'">Image</xsl:when>
                        <xsl:when test="string(text())='Image, 3-D'">Image</xsl:when>
                        <xsl:when test="string(text())='Map'">Image (?)</xsl:when>
                        <xsl:when test="string(text())='Musical Score'">Sound</xsl:when>
                        <xsl:when test="string(text())='Plan or blueprint'">Image (?)</xsl:when>
                        <xsl:when test="string(text())='Preprint'">Text</xsl:when>
                        <xsl:when test="string(text())='Presentation'">Image (?)</xsl:when>
                        <xsl:when test="string(text())='Recording, acoustical'">Sound</xsl:when>
                        <xsl:when test="string(text())='Recording, musical'">Sound</xsl:when>
                        <xsl:when test="string(text())='Recording, oral'">Sound</xsl:when>
                        <xsl:when test="string(text())='Software'">Software</xsl:when>
                        <xsl:when test="string(text())='Technical Report'">Text</xsl:when>
                        <xsl:when test="string(text())='Thesis'">Text</xsl:when>
                        <xsl:when test="string(text())='Video'">Film</xsl:when>
                        <xsl:when test="string(text())='Working Paper'">Text</xsl:when>
                        <xsl:when test="string(text())='Other'">Defaulttyp?</xsl:when>
                        <xsl:otherwise>Defaulttyp?</xsl:otherwise>
                    </xsl:choose>
                </xsl:attribute>              
                <xsl:value-of select="." />                
            </xsl:element>
        </xsl:for-each>
    </xsl:template>

    <!-- DataCite (13) :: Size -->

    <!-- DataCite (14) :: Format -->

    <!-- DataCite (15) :: Version -->
    <xsl:template match="//dspace:field[@mdschema='dc' and @element='description' and @qualifier='provenance']">
        <xsl:for-each select=".">
            <xsl:element name="version">                
                <xsl:if test="contains(text(),'Made available')">
                    <xsl:value-of select="." />
                </xsl:if>
            </xsl:element>
        </xsl:for-each>
    </xsl:template>

    <!-- DataCite (16) :: Rights -->

    <!-- DataCite (17) :: Description -->
    <xsl:template match="//dspace:field[@mdschema='dc' and @element='description'][not(@qualifier='provenance')]">
        <xsl:for-each select=".">
            <xsl:element name="description">
                <xsl:attribute name="descriptionType">
                    <xsl:choose>           
                        <xsl:when test="@qualifier='abstract'">Abstract</xsl:when>
                        <xsl:otherwise>Other</xsl:otherwise>
                    </xsl:choose>
                </xsl:attribute>
                <xsl:value-of select="." />
            </xsl:element>
        </xsl:for-each>
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

        <!-- Test if item contains a publication year.
             We should use (in order of priority) date of embargo, dc.date.copyright or dc.date.
            <xsl:if test="//dspace:field[@mdschema='dc' and @element='date']">
        -->