<?xml version="1.0" encoding="UTF-8" ?>
<!-- Created for LINDAT/CLARIN based on DIM2DataCite https://guidelines.openaire.eu/wiki/OpenAIRE_Guidelines:_For_Data_Archives -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:doc="http://www.lyncode.com/xoai"
                xmlns:xalan="http://xml.apache.org/xslt"
                xmlns:odc="http://schema.datacite.org/oai/oai-1.1/"
                xmlns="http://datacite.org/schema/kernel-3"
                xmlns:fn="http://custom.crosswalk.functions"
                xmlns:fnx="http://www.w3.org/2005/xpath-functions"
                exclude-result-prefixes="doc xalan fn fnx" version="1.0">
    <xsl:output omit-xml-declaration="yes" method="xml" indent="yes" />

    <!-- TODO implement Required & optional templates -->
    <xsl:template match="/">
        <odc:oai_datacite>
            <odc:schemaVersion>3.1</odc:schemaVersion>
            <odc:datacentreSymbol></odc:datacentreSymbol>
            <odc:payload>
                <resource>
                    <xsl:call-template name="Identifier_M" />
                    <xsl:call-template name="Creator_M" />
                    <xsl:call-template name="Title_M" />
                    <xsl:call-template name="Publisher_M" />
                    <xsl:call-template name="PublicationYear_M" />
                    <xsl:call-template name="Subject_R" />
                    <xsl:call-template name="Contributor_MAO" />
                    <xsl:call-template name="Date_M" />
                    <xsl:call-template name="Language_R" />
                    <xsl:call-template name="ResourceType_R" />
                    <xsl:call-template name="AlternateIdentifier_O" />
                    <xsl:call-template name="RelatedIdentifier_MA" />
                    <xsl:call-template name="Size_O" />
                    <xsl:call-template name="Format_O" />
                    <xsl:call-template name="Version_O" />
                    <xsl:call-template name="Rights_MA" />
                    <xsl:call-template name="Description_MA" />
                    <xsl:call-template name="GeoLocation_R" />
                </resource>
            </odc:payload>
        </odc:oai_datacite>
    </xsl:template>

    <xsl:template name="Identifier_M">
        <identifier identifierType="Handle">
            <xsl:value-of
                    select="substring-after(doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element/doc:element/doc:field[@name='value'], 'http://hdl.handle.net/')" />
        </identifier>
    </xsl:template>

    <xsl:template name="Creator_M">
        <creators>
            <xsl:choose>
                <xsl:when
                        test="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']/doc:element/doc:field[@name='value']">
                    <xsl:call-template name="_process_creators" />
                </xsl:when>
                <xsl:otherwise>
                    <creator>
                        <creatorName>(:unkn) unknown</creatorName>
                    </creator>
                </xsl:otherwise>
            </xsl:choose>
        </creators>
    </xsl:template>

    <xsl:template name="Title_M">
        <titles>
            <xsl:choose>
                <xsl:when
                        test="doc:metadata/doc:element[@name='dc']/doc:element[@name='title']/doc:element/doc:field[@name='value']">
                    <title>
                        <xsl:value-of
                                select="doc:metadata/doc:element[@name='dc']/doc:element[@name='title']/doc:element/doc:field[@name='value']" />
                    </title>
                </xsl:when>
                <xsl:otherwise>
                    <title>(:unas) unassigned</title>
                </xsl:otherwise>
            </xsl:choose>
        </titles>
    </xsl:template>

    <xsl:template name="Publisher_M">
        <publisher>
            <xsl:choose>
                <xsl:when
                        test="doc:metadata/doc:element[@name='dc']/doc:element[@name='publisher']/doc:element/doc:field[@name='value']">
                    <xsl:value-of
                            select="doc:metadata/doc:element[@name='dc']/doc:element[@name='publisher']/doc:element/doc:field[@name='value']" />
                </xsl:when>
                <!-- Or fixed repository name? -->
                <xsl:otherwise>
                    (:unkn) unknown
                </xsl:otherwise>
            </xsl:choose>
        </publisher>
    </xsl:template>

    <xsl:template name="PublicationYear_M">
        <!-- bit unclear what goes here if embargo, datacite suggests end of embargo year -->
        <publicationYear>
            <xsl:value-of
                    select="substring(doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']/doc:element/doc:field[@name='value'],1,4)" />
        </publicationYear>
    </xsl:template>

    <xsl:template name="Subject_R" />

    <xsl:template name="Contributor_MAO">
        <xsl:variable name="funder" select="doc:metadata/doc:element[@name='dc']/doc:element[@name='relation']/doc:element/doc:field[@name='value']"/>
        <xsl:if test="starts-with($funder,'info:')">
            <contributors>
                <contributor contributorType="Funder">
                    <xsl:variable name="cont_name" select="tokenize($funder,'/')[3]"/>
                    <xsl:choose>
                        <xsl:when test="$cont_name='EC'">
                            <contributorName>European Commission</contributorName>
                        </xsl:when>
                        <xsl:otherwise>
                            <contributorName><xsl:value-of select="$cont_name"/></contributorName>
                        </xsl:otherwise>
                    </xsl:choose>
                    <nameIdentifier nameIdentifierScheme="info"><xsl:value-of select="$funder"/></nameIdentifier>
                </contributor>
            </contributors>
        </xsl:if>
    </xsl:template>

    <xsl:template name="Date_M">
        <!-- Use “Issued” for the date the resource is published or distributed.
            To indicate the end of an embargo period, use “Available”. To indicate the
            start of an embargo period, use “Accepted”. DataCite v3.0 further recommends
            use of “Created” and “Submitted”. -->
        <!-- TODO possibly other dates -->
        <dates>
            <date dateType="Issued">
                <xsl:value-of
                        select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']/doc:element/doc:field[@name='value']" />
            </date>
            <!-- Technically start of embargo period -->
            <date dateType="Accepted">
                <xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='accessioned']/doc:element/doc:field[@name='value']"/>
            </date>
            <xsl:choose>
                <xsl:when test="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='available']/doc:element/doc:field[@name='value']">
                    <date dateType="Available">
                        <xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='available']/doc:element/doc:field[@name='value']"/>
                    </date>
                </xsl:when>
                <xsl:otherwise>
                    <!-- empty available means embargo. item will be available on the embargo date -->
                    <date dateType="Available">
                        <xsl:value-of select="doc:metadata/doc:element[@name='local']/doc:element[@name='embargo']/doc:element[@name='termslift']/doc:element/doc:field[@name='value']"/>
                    </date>
                </xsl:otherwise>
            </xsl:choose>
        </dates>
    </xsl:template>

    <xsl:template name="Language_R" />

    <xsl:template name="ResourceType_R">
        <!--
            request for this information by OpenAIRE
                Controlled List Values:
                Audiovisual
                Collection
                Dataset
                Event
                Image
                InteractiveResource
                Model
                PhysicalObject
                Service
                Software
                Sound
                Text
                Workflow
                Other
        !-->

        <!-- type -->
        <xsl:variable name="dc_type" select="fnx:distinct-values(doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element/doc:field[@name='value'])[1]"/>
        <xsl:choose>
            <xsl:when test="$dc_type='corpus'">
                <resourceType resourceTypeGeneral="Dataset">
                    <xsl:value-of select="$dc_type"/>
                </resourceType>
            </xsl:when>
            <xsl:when test="$dc_type='toolService'">
                <resourceType resourceTypeGeneral="Software">
                    <xsl:value-of select="$dc_type"/>
                </resourceType>
            </xsl:when>
            <xsl:when test="$dc_type='lexicalConceptualResource'">
                <resourceType resourceTypeGeneral="Dataset">
                    <xsl:value-of select="$dc_type"/>
                </resourceType>
            </xsl:when>
            <xsl:when test="$dc_type='languageDescription'">
                <resourceType resourceTypeGeneral="Dataset">
                    <xsl:value-of select="."/>
                </resourceType>
            </xsl:when>
        </xsl:choose>

    </xsl:template>

    <xsl:template name="AlternateIdentifier_O" />
    <!-- TODO Related identifiers in submission? -->
    <xsl:template name="RelatedIdentifier_MA" />
    <xsl:template name="Size_O" />
    <xsl:template name="Format_O" />
    <xsl:template name="Version_O" />

    <xsl:template name="Rights_MA">
        <rightsList>
            <xsl:choose>
                <xsl:when test="/doc:metadata/doc:element[@name='local']/doc:element[@name='embargo']/doc:element[@name='termslift']/doc:element/doc:field[@name='value']">
                    <rights rightsURI="info:eu-repo/semantics/embargoedAccess" />
                </xsl:when>
                <xsl:when test="/doc:metadata/doc:element[@name='others']/doc:field[@name='restrictedAccess']/text()='true'">
                    <rights rightsURI="info:eu-repo/semantics/restrictedAccess"/>
                </xsl:when>
                <xsl:otherwise>
                    <rights rightsURI="info:eu-repo/semantics/openAccess"/>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='rights']/doc:element[@name='uri']/doc:element/doc:field[@name='value']">
                <rights><xsl:attribute name="rightsURI"><xsl:value-of select="."/></xsl:attribute></rights>
            </xsl:for-each>
        </rightsList>

    </xsl:template>

    <xsl:template name="Description_MA">
        <descriptions>
            <!-- "Abstract" is to a degree what we keep in dc.description -->
            <description descriptionType="Abstract"><xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='description']/doc:element/doc:field[@name='value']"/></description>
        </descriptions>
    </xsl:template>

    <xsl:template name="GeoLocation_R" />

    <xsl:template name="_process_creators">
        <xsl:for-each
                select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']/doc:element/doc:field[@name='value']">
            <creator>
                <creatorName>
                    <xsl:value-of select="." />
                </creatorName>
            </creator>
        </xsl:for-each>
    </xsl:template>
</xsl:stylesheet>
