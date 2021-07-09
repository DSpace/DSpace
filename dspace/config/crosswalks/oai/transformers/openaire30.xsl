<?xml version="1.0" encoding="UTF-8"?>
<!-- 


    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/
    
	Developed by DSpace @ Lyncode <dspace@lyncode.com> 
	Following Driver Guidelines 2.0:
		- http://www.driver-support.eu/managers.html#guidelines


	OpenAIRE 3.0 transformer.

 -->
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:doc="http://www.lyncode.com/xoai">
	<xsl:output indent="yes" method="xml" omit-xml-declaration="yes" />

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
        <xsl:call-template name="altIdentifier"/>
        <xsl:call-template name="publicationVersion"/>
        <xsl:call-template name="openAccess"/>
    </xsl:template>

    <!-- Formatting dc.date.issued -->
    <xsl:template match="/doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']/doc:element/doc:field/text()">
        <xsl:call-template name="formatdate">
            <xsl:with-param name="datestr" select="."/>
        </xsl:call-template>
    </xsl:template>

    <!-- Removing other dc.date.* -->
    <xsl:template match="/doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name!='issued']"/>

    <!-- Changing dc.type -->
    <!-- Removing unwanted Peer reviewed -->
    <xsl:template match="/doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element/doc:field[text() = 'Peer reviewed']"/>
    <xsl:template match="/doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element/doc:field/text()">
        <xsl:call-template name="publicationType">
            <xsl:with-param name="brageType" select="."/>
        </xsl:call-template>
    </xsl:template>

    <!-- Removing unwanted dc.rights -->
    <xsl:template match="/doc:metadata/doc:element[@name='dc']/doc:element[@name='rights']"/>

    <!-- dc.relation -->
    <xsl:template match="/doc:metadata/doc:element[@name='dc']/doc:element[@name='relation']/doc:element[@name='project']/doc:element/doc:field/text()">
        <xsl:choose>
            <xsl:when test="starts-with(.,'EC/FP') or starts-with(.,'EC/H2020') or starts-with(.,'EC/HEU')">
                <xsl:value-of select="concat('info:eu-repo/grantAgreement/',.)"/>
            </xsl:when>
            <xsl:otherwise><xsl:value-of select="."/></xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- Formatting dc.subject.ddc -->
    <xsl:template match="/doc:metadata/doc:element[@name='dc']/doc:element[@name='subject']/doc:element[@name='ddc']/doc:element/doc:field/text()">
        <xsl:call-template name="addPrefix">
            <xsl:with-param name="value" select="."/>
            <xsl:with-param name="prefix" select="'info:eu-repo/classification/ddc/'"/>
        </xsl:call-template>
    </xsl:template>

    <!-- Removing unwanted dc.description.version -->
    <xsl:template match="/doc:metadata/doc:element[@name='dc']/doc:element[@name='description']/doc:element[@name='version']"/>

    <!-- Only handle should be kept dc.identifier -->
    <!-- Removing unwanted dc.identifier.* -->
    <xsl:template match="/doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name!='uri']"/>
    <xsl:template match="/doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='uri']/doc:element/doc:field/text()">
        <xsl:if test="starts-with(., 'http://hdl.handle.net/') or starts-with(., 'https://hdl.handle.net/')">
            <xsl:value-of select="."/>
        </xsl:if>
    </xsl:template>


    <!-- AUXILIARY TEMPLATES -->
    <!-- dc.relation -->
    <xsl:template name="altIdentifier">
        <xsl:if test="@name='dc'">
            <xsl:if test="/doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='issn']">
                <xsl:call-template name="relationElement">
                    <xsl:with-param name="stripPrefix" select="'urn:issn:'"/>
                    <xsl:with-param name="qualifier" select="'issn'"/>
                    <xsl:with-param name="values" select="/doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='issn']/doc:element/doc:field"/>
                </xsl:call-template>
            </xsl:if>
            <xsl:if test="/doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='doi']">
                <xsl:call-template name="relationElement">
                    <xsl:with-param name="stripPrefix" select="'http://dx.doi.org/'"/>
                    <xsl:with-param name="qualifier" select="'doi'"/>
                    <xsl:with-param name="values" select="/doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='doi']/doc:element/doc:field"/>
                </xsl:call-template>
            </xsl:if>
            <xsl:if test="/doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='isbn']">
                <xsl:call-template name="relationElement">
                    <xsl:with-param name="stripPrefix" select="'urn:isbn:'"/>
                    <xsl:with-param name="qualifier" select="'isbn'"/>
                    <xsl:with-param name="values" select="/doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='isbn']/doc:element/doc:field"/>
                </xsl:call-template>
            </xsl:if>
        </xsl:if>
    </xsl:template>

    <!-- Transform dc.description.version to dc.type adding prefix info:eu-repo/semantics/ to its value -->
    <xsl:template name="publicationVersion">
        <xsl:if test="name(.) = 'element' and ../@name = 'type' and /doc:metadata/doc:element[@name='dc']/doc:element[@name='description']/doc:element[@name='version']">
            <xsl:element name="element" namespace="http://www.lyncode.com/xoai">
                <xsl:attribute name="name">none</xsl:attribute>
                <xsl:element name="field" namespace="http://www.lyncode.com/xoai">
                    <xsl:attribute name="name">value</xsl:attribute>
                    <xsl:call-template name="addPrefix">
                        <xsl:with-param name="prefix" select="'info:eu-repo/semantics/'"/>
                        <xsl:with-param name="value" select="/doc:metadata/doc:element[@name='dc']/doc:element[@name='description']/doc:element[@name='version']/doc:element/doc:field/text()"/>
                    </xsl:call-template>
                </xsl:element>
            </xsl:element>
        </xsl:if>
    </xsl:template>

    <!-- Append dc.rights open access (only open access items passes through the xoai driver filter) -->
    <xsl:template name="openAccess">
        <xsl:if test="@name='dc'">
            <xsl:element name="element" namespace="http://www.lyncode.com/xoai">
                <xsl:attribute name="name">dc</xsl:attribute>
                <xsl:element name="element" namespace="http://www.lyncode.com/xoai">
                    <xsl:attribute name="name">rights</xsl:attribute>
                    <xsl:element name="element" namespace="http://www.lyncode.com/xoai">
                        <xsl:attribute name="name">none</xsl:attribute>
                        <xsl:element name="field" namespace="http://www.lyncode.com/xoai">
                            <xsl:attribute name="name">value</xsl:attribute>
                            <xsl:text>info:eu-repo/semantics/openAccess</xsl:text>
                        </xsl:element>
                    </xsl:element>
                </xsl:element>
            </xsl:element>
        </xsl:if>
    </xsl:template>

    <!-- dc.relation -->
    <xsl:template name="relationElement">
        <xsl:param name="stripPrefix"/>
        <xsl:param name="qualifier"/>
        <xsl:param name="values"/>
        <xsl:element name="element" namespace="http://www.lyncode.com/xoai">
            <xsl:attribute name="name">dc</xsl:attribute>
            <xsl:element name="element" namespace="http://www.lyncode.com/xoai">
                <xsl:attribute name="name">relation</xsl:attribute>
                <xsl:element name="element" namespace="http://www.lyncode.com/xoai">
                    <xsl:attribute name="name">
                        <xsl:value-of select="$qualifier"/>
                    </xsl:attribute>
                    <xsl:for-each select="$values">
                        <xsl:variable name="strippedValue">
                            <xsl:call-template name="stripPrefix">
                                <xsl:with-param name="prefix" select="$stripPrefix"/>
                                <xsl:with-param name="value" select="."/>
                            </xsl:call-template>
                        </xsl:variable>
                        <xsl:element name="field" namespace="http://www.lyncode.com/xoai">
                            <xsl:attribute name="name">value</xsl:attribute>
                            <xsl:value-of select="concat(concat('info:eu-repo/semantics/altIdentifier/',$qualifier),'/',$strippedValue)"/>
                        </xsl:element>
                    </xsl:for-each>
                </xsl:element>
            </xsl:element>
        </xsl:element>
    </xsl:template>

    <xsl:template name="publicationType">
        <xsl:param name="brageType"/>
        <xsl:choose>
            <xsl:when test="$brageType = 'Book'">info:eu-repo/semantics/book</xsl:when>
            <xsl:when test="$brageType = 'Chapter'">info:eu-repo/semantics/bookPart</xsl:when>
            <xsl:when test="$brageType = 'Conference object'">info:eu-repo/semantics/conferenceObject</xsl:when>
            <xsl:when test="$brageType = 'Lecture'">info:eu-repo/semantics/lecture</xsl:when>
            <xsl:when test="$brageType = 'Journal article'">info:eu-repo/semantics/article</xsl:when>
            <xsl:when test="$brageType = 'Doctoral thesis'">info:eu-repo/semantics/doctoralThesis</xsl:when>
            <xsl:when test="$brageType = 'Master thesis'">info:eu-repo/semantics/masterThesis</xsl:when>
            <xsl:when test="$brageType = 'Bachelor thesis'">info:eu-repo/semantics/bachelorThesis</xsl:when>
            <xsl:when test="$brageType = 'Patent'">info:eu-repo/semantics/patent</xsl:when>
            <xsl:when test="$brageType = 'Preprint'">info:eu-repo/semantics/preprint</xsl:when>
            <xsl:when test="contains('|Research report|Report|', concat('|', $brageType, '|'))">info:eu-repo/semantics/report</xsl:when>
            <xsl:when test="$brageType = 'Working paper'">info:eu-repo/semantics/workingPaper</xsl:when>
            <xsl:otherwise>info:eu-repo/semantics/other</xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="stripPrefix">
        <xsl:param name="value"/>
        <xsl:param name="prefix"/>
        <xsl:choose>
            <xsl:when test="starts-with($value,$prefix)">
                <xsl:value-of select="substring-after($value, $prefix)"/>
            </xsl:when>
            <xsl:otherwise><xsl:value-of select="$value"/></xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- dc.type prefixing -->
    <xsl:template name="addPrefix">
        <xsl:param name="value"/>
        <xsl:param name="prefix"/>
        <xsl:choose>
            <xsl:when test="starts-with($value, $prefix)">
                <xsl:value-of select="$value"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="concat($prefix, $value)"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- Date format -->
    <xsl:template name="formatdate">
        <xsl:param name="datestr"/>
        <xsl:variable name="sub">
            <xsl:value-of select="substring($datestr,1,10)"/>
        </xsl:variable>
        <xsl:value-of select="$sub"/>
    </xsl:template>
</xsl:stylesheet>
