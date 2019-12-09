<?xml version="1.0" encoding="UTF-8"?>
<!-- 

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

	Developed by Paulo Graça <paulo.graca@fccn.pt>
	
	> https://www.openaire.eu/schema/repo-lit/4.0/openaire.xsd

 -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:oaire="http://namespace.openaire.eu/schema/oaire/" xmlns:datacite="http://datacite.org/schema/kernel-4"
    xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:doc="http://www.lyncode.com/xoai"
    xmlns:rdf="http://www.w3.org/TR/rdf-concepts/" version="1.0">
    <xsl:output omit-xml-declaration="yes" method="xml" indent="yes"/>

    <xsl:template match="/">
        <resource xmlns="http://namespace.openaire.eu/schema/oaire/"
            xmlns:vc="http://www.w3.org/2007/XMLSchema-versioning" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://namespace.openaire.eu/schema/oaire/ https://www.openaire.eu/schema/repo-lit/4.0/openaire.xsd">

            <xsl:apply-templates select="doc:metadata/doc:element[@name='dc']/doc:element[@name='title']"
                mode="datacite"/>
            <xsl:apply-templates
                select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']"
                mode="datacite"/>

        <!--dcterms:audience>Researchers</dcterms:audience-->
        </resource>
    </xsl:template>

   <!-- datacite.titles -->
    <xsl:template match="doc:element[@name='dc']/doc:element[@name='title']" mode="datacite">
        <datacite:titles>
            <xsl:apply-templates select="." mode="title"/>
        </datacite:titles>
    </xsl:template>

   <!-- datacite.title -->
    <xsl:template match="doc:element[@name='title']" mode="title">
        <!-- datacite.title -->
        <xsl:for-each select="./doc:element/doc:field[@name='value']">
            <datacite:title>
                <xsl:call-template name="xmlLanguage">
                    <xsl:with-param name="name" select="../@name"/>
                </xsl:call-template>
                <xsl:value-of select="."/>
            </datacite:title>
        </xsl:for-each>
         <!-- datacite.title.* -->
        <xsl:for-each select="./doc:element/doc:element/doc:field[@name='value']">
            <xsl:variable name="lc_title_type">
                <xsl:call-template name="lowercase">
                    <xsl:with-param name="value" select="../../@name"/>
                </xsl:call-template>
            </xsl:variable>
            <datacite:title>
                <xsl:call-template name="xmlLanguage">
                    <xsl:with-param name="name" select="../@name"/>
                </xsl:call-template>
                <xsl:attribute name="titleType">
                    <xsl:choose>
                        <xsl:when test="$lc_title_type = 'alternativetitle' or $lc_title_type = 'alternative'">
                        <xsl:text>AlternativeTitle</xsl:text>
                        </xsl:when>
                        <xsl:when test="$lc_title_type = 'subtitle'">
                          <xsl:text>Subtitle</xsl:text>
                        </xsl:when>
                        <xsl:when test="$lc_title_type = 'translatedtitle'">
                          <xsl:text>TranslatedTitle</xsl:text>
                        </xsl:when>
                        <xsl:otherwise>
                          <xsl:text>Other</xsl:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:attribute>
                <xsl:value-of select="."/>
            </datacite:title>
        </xsl:for-each>
    </xsl:template>

    <xsl:template
        match="doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']"
        mode="datacite">
        <datacite:creators>
            <!-- datacite.creator -->
            <xsl:for-each select="./doc:element/doc:field[@name='value']">
                 <datacite:creator>
                    <!-- authority? -->
                    <xsl:variable name="sibling1" select="following-sibling::*[1]"/>                    
                    <xsl:choose>
                        <!-- if next sibling is authority and starts with virtual:: -->
                        <xsl:when test="$sibling1[@name='authority' and starts-with(text(),'virtual::')]">
                            <xsl:variable name="entity">
                                <xsl:call-template name="buildEntityNode">
                                    <xsl:with-param name="element" select="."/>
                                </xsl:call-template>
                            </xsl:variable>
                            <xsl:apply-templates select="$entity" mode="entity_author"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <datacite:creatorName><xsl:value-of select="./text()"/></datacite:creatorName>
                        </xsl:otherwise>
                    </xsl:choose>
                </datacite:creator>
            </xsl:for-each>
        </datacite:creators>
    </xsl:template>

    <xsl:template match="doc:element" mode="entity_author">
        <datacite:creatorName><xsl:value-of select="doc:field[starts-with(@name,'dc.contributor.author')]"/></datacite:creatorName>
        <xsl:apply-templates select="doc:field" mode="entity_author"/>
    </xsl:template>

    <xsl:template match="doc:field[starts-with(@name,'person.givenName')]" mode="entity_author">
        <datacite:givenName><xsl:value-of select="./text()"/></datacite:givenName>
    </xsl:template>

    <xsl:template match="doc:field[starts-with(@name,'person.familyName')]" mode="entity_author">
        <datacite:familyName><xsl:value-of select="./text()"/></datacite:familyName>
    </xsl:template>

    <xsl:template match="doc:field[starts-with(@name,'person.affiliation.name')]" mode="entity_author">
        <datacite:affiliation><xsl:value-of select="./text()"/></datacite:affiliation>
    </xsl:template>

    <xsl:template match="doc:field[starts-with(@name,'person.identifier.orcid')]" mode="entity_author">
        <datacite:nameIdentifier nameIdentifierScheme="ORCID"
                    schemeURI="http://orcid.org">
            <xsl:value-of select="./text()"/>
        </datacite:nameIdentifier>
    </xsl:template>



   <!--  -->
   <!-- Auxiliary templates -->
   <!--  -->
    <xsl:param name="smallcase" select="'abcdefghijklmnopqrstuvwxyzàèìòùáéíóúýâêîôûãñõäëïöüÿåæœçðø'"/>
    <xsl:param name="uppercase" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZÀÈÌÒÙÁÉÍÓÚÝÂÊÎÔÛÃÑÕÄËÏÖÜŸÅÆŒÇÐØ'"/>

   <!-- to retrieve a string in uppercase -->
    <xsl:template name="uppercase">
        <xsl:param name="value"/>
        <xsl:value-of select="translate($value, $smallcase, $uppercase)"/>
    </xsl:template>

   <!-- to retrieve a string in lowercase -->
    <xsl:template name="lowercase">
        <xsl:param name="value"/>
        <xsl:value-of select="translate($value, $uppercase, $smallcase)"/>
    </xsl:template>

    <!-- to retrieve a string which the first letter is in uppercase -->
    <xsl:template name="ucfirst">
        <xsl:param name="value"/>
        <xsl:call-template name="uppercase">
            <xsl:with-param name="value" select="substring($value, 1, 1)"/>
        </xsl:call-template>
        <xsl:call-template name="lowercase">
            <xsl:with-param name="value" select="substring($value, 2)"/>
        </xsl:call-template>
    </xsl:template>

    <!-- 
        this template will try to look for all "virtual::"
        and rebuild an "Entity" based on all virtual fields 
        in the source data.
        This will retrieve something like 
     -->
    <xsl:template name="buildEntityNode">
        <xsl:param name="element"/>
        <!-- authority? -->
        <xsl:variable name="sibling1" select="$element/following-sibling::*[1]"/>
        <!-- confidence? -->
        <xsl:variable name="sibling2" select="$element/following-sibling::*[2]"/>
        <!-- if next sibling is authority and starts with virtual:: -->
        <xsl:if test="$sibling1[@name='authority' and starts-with(text(),'virtual::')]">
            <xsl:variable name="relation_id" select="$sibling1[1]/text()"/>
            <xsl:element name="element" namespace="http://www.lyncode.com/xoai">
                <xsl:attribute name="name">
                    <xsl:value-of select="$relation_id"/>
                </xsl:attribute>
                <!-- search for all virtual relations elements in XML -->
                <xsl:for-each select="//doc:field[text()=$relation_id]/preceding-sibling::*[1]">
                    <xsl:element name="field" namespace="http://www.lyncode.com/xoai">
                        <xsl:attribute name="name">
                        <xsl:call-template name="buildEntityFieldName">
                        <xsl:with-param name="element" select="."/>
                          </xsl:call-template>
                        </xsl:attribute>
                        <!-- field value -->
                        <xsl:value-of select="./text()"/>
                    </xsl:element>
                </xsl:for-each>
            </xsl:element>
        </xsl:if>
    </xsl:template>
   
   <!-- 
    This template will recursively create the field name based on parent node names
    to be something like this:
    person.familyName.*
    -->
    <xsl:template name="buildEntityFieldName">
        <xsl:param name="element"/>
        <xsl:choose>
            <xsl:when test="$element/..">
                <xsl:call-template name="buildEntityFieldName">
                    <xsl:with-param name="element" select="$element/.."/>
                </xsl:call-template>
                <!-- if parent isn't an element then don't include '.' -->
                <xsl:if test="local-name($element/../..) = 'element'">
                    <xsl:text>.</xsl:text>
                </xsl:if>
                <xsl:value-of select="$element/../@name"/>
            </xsl:when>
            <xsl:otherwise/>
        </xsl:choose>
    </xsl:template>    

   <!-- xml:language -->
    <xsl:template name="xmlLanguage">
        <xsl:param name="name"/>
        <xsl:variable name="lc_name">
            <xsl:call-template name="lowercase">
                <xsl:with-param name="value" select="$name"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:if test="$lc_name!='none' and $name!=''">
            <xsl:attribute name="xml:lang">
            <xsl:value-of select="$name"/>
         </xsl:attribute>
        </xsl:if>
    </xsl:template>

   <!-- get the issued date -->
    <xsl:template name="getIssuedDate">
        <xsl:param name="dc_node"/>
        <xsl:value-of
            select="$dc_node/doc:element[@name='date']/doc:element[@name='issued']/doc:element/doc:field[@name='value']/text()"/>
    </xsl:template>

    <!-- ignore all non specified text values or attributes -->
    <xsl:template match="text()|@*" />
    <xsl:template match="text()|@*" mode="entity_author" />
</xsl:stylesheet>