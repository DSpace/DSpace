<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : DIM2DataCite.xsl
    Created on : January 23, 2013, 1:26 PM
    Updated on : November 26, 2015, 3:00 PM
    Author     : pbecker, ffuerste
    Description: Converts metadata from DSpace Intermediat Format (DIM) into
                 metadata following the DataCite Schema for the Publication and
                 Citation of Research Data, Version 3.1, for use with EZID.
                 Copied from DIM2DataCite.xsl, with small changes.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:dspace="http://www.dspace.org/xmlns/dspace/dim"
                xmlns="http://datacite.org/schema/kernel-3"
                version="2.0">
    
    <!-- CONFIGURATION -->
    <!-- The parameters prefix, publisher, datamanager and hostinginstitution
         moved to DSpace's configuration. They will be substituted automatically.
         It is not necessary anymore to change this file.
         Please take a look into the DSpace documentation for details on how to
         change those. -->
    <!-- DO NOT CHANGE ANYTHING BELOW THIS LINE EXCEPT YOU REALLY KNOW WHAT YOU ARE DOING! -->
    
    <!-- We need the prefix to determine DOIs that were minted by ourself. -->
    <xsl:param name="prefix">10.5072/dspace-</xsl:param>
    <!-- The content of the following parameter will be used as element publisher. -->
    <xsl:param name="publisher">My University</xsl:param>
    <!-- The content of the following variable will be used as element contributor with contributorType datamanager. -->
    <xsl:param name="datamanager"><xsl:value-of select="$publisher" /></xsl:param>
    <!-- The content of the following variable will be used as element contributor with contributorType hostingInstitution. -->
    <xsl:param name="hostinginstitution"><xsl:value-of select="$publisher" /></xsl:param>
    <!-- Please take a look into the DataCite schema documentation if you want to know how to use these elements.
         http://schema.datacite.org -->

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
        <resource xmlns="http://datacite.org/schema/kernel-3"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="http://datacite.org/schema/kernel-3 http://schema.datacite.org/meta/kernel-3/metadata.xsd">

            <!--
                MANDATORY PROPERTIES
            -->

            <!--
                DataCite (1)
                Template Call for DOI identifier.
                Occ: 1
            -->
            <!--
                 For EZID we need an empty field here.
            -->
                <identifier identifierType='DOI'/>

            <!--
                DataCite (2)
                Add creator information.
                Occ: 1-n
            -->
            <creators>
                <xsl:choose>
                    <xsl:when test="//dspace:field[@mdschema='dc' and @element='contributor' and @qualifier='author']">
                        <xsl:apply-templates select="//dspace:field[@mdschema='dc' and @element='contributor' and @qualifier='author']" />
                    </xsl:when>
                    <xsl:otherwise>
                        <creator>
                            <creatorName>(:unkn) unknown</creatorName>
                        </creator>
                    </xsl:otherwise>
                </xsl:choose>
            </creators>

            <!--
                DataCite (3)
                Add Title information. 
		Occ: 1-n
            -->
            <titles>
                <xsl:choose>
                    <xsl:when test="//dspace:field[@mdschema='dc' and @element='title']">
                        <xsl:apply-templates select="//dspace:field[@mdschema='dc' and @element='title']" />
                    </xsl:when>
                    <xsl:otherwise>
                        <title>(:unas) unassigned</title>
                    </xsl:otherwise>
                </xsl:choose>
            </titles>

            <!--
                DataCite (4)
                Add Publisher information from configuration above
                Occ: 1
                Use dc.publisher if it exists, use $publisher otherwise.
            -->
            <xsl:element name="publisher">
                <xsl:choose>
                    <xsl:when test="//dspace:field[@mdschema='dc' and @element='publisher']">
                        <xsl:value-of select="//dspace:field[@mdschema='dc' and @element='publisher'][1]" />
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="$publisher" />
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:element>

            <!--
                DataCite (5)
                Add PublicationYear information
                Occ: 1
                Format: YYYY
            -->
            <publicationYear>
                <xsl:choose>
                    <xsl:when test="//dspace:field[@mdschema='dc' and @element='date' and @qualifier='issued']">
                        <xsl:value-of select="substring(//dspace:field[@mdschema='dc' and @element='date' and @qualifier='issued'], 1, 4)" />
                    </xsl:when>
                    <xsl:when test="//dspace:field[@mdschema='dc' and @element='date' and @qualifier='available']">
                        <xsl:value-of select="substring(//dspace:field[@mdschema='dc' and @element='date' and @qualifier='available'], 1, 4)" />
                    </xsl:when>
                    <xsl:when test="//dspace:field[@mdschema='dc' and @element='date']">
                        <xsl:value-of select="substring(//dspace:field[@mdschema='dc' and @element='date'], 1, 4)" />
                    </xsl:when>
                    <xsl:otherwise>0000</xsl:otherwise>
                </xsl:choose>
            </publicationYear>

            <!--
                OPTIONAL PROPERTIES
            -->

            <!--
                DataCite (6)
                Template Call for subjects.
                Occ: 0-n
                Format: open
                Attribute: subjectSchema (optional), schemeURI (optional)
            -->  
            <xsl:if test="//dspace:field[@mdschema='dc' and @element='subject']">
                <subjects>
                    <xsl:apply-templates select="//dspace:field[@mdschema='dc' and @element='subject']" />
                </subjects>
            </xsl:if>

            <!--
                DataCite (7)
                Add contributorType from configuration above.
                Template Call for Contributors
                Occ: 0-n
                Format: personal name: family, given
                Required Attribute: contributorType - controlled list
            --> 
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
                <xsl:apply-templates select="//dspace:field[@mdschema='dc' and @element='contributor'][not(@qualifier='author')]" />
            </contributors>

            <!--
                DataCite (8)
                Template Call for Dates
                Occ: 0-n
                Required Attribute: dataType - controlled list
            --> 
            <xsl:if test="//dspace:field[@mdschema='dc' and @element='date' and 
                        (@qualifier='accessioned' 
                         or @qualifier='available' 
                         or @qualifier='copyright' 
                         or @qualifier='created' 
                         or @qualifier='issued' 
                         or @qualifier='submitted'
                         or @qualifier='updated')]" >
                <xsl:element name="dates">
                    <xsl:apply-templates select="//dspace:field[@mdschema='dc' and @element='date' and 
                        (@qualifier='accessioned' 
                         or @qualifier='available' 
                         or @qualifier='copyright' 
                         or @qualifier='created' 
                         or @qualifier='issued' 
                         or @qualifier='submitted'
                         or @qualifier='updated')]" />
                </xsl:element>
            </xsl:if>

            <!-- 
                DataCite (9)
                Templacte Call for Language
                Occ: 0-1
                Format: IETF BCP 47 or ISO 639-1
            -->
            <xsl:apply-templates select="(//dspace:field[@mdschema='dc' and @element='language' and (@qualifier='iso' or @qualifier='rfc3066')])[1]" />

            <!--
                DataCite (10)
                Template call for ResourceType
                DataCite allows the ResourceType to ouccre not more than once.
            -->
            <xsl:apply-templates select="(//dspace:field[@mdschema='dc' and @element='type'])[1]" />

            <!-- 
                DataCite (11)
                Add alternativeIdentifiers.
                This element is important as it is used to recognize for which
                DSpace object a DOI is reserved for.
                See the primary identifier for which the doi is registered.
                Occ: 0-n
                Required Attribute: alternateIdentifierType (free format)
            -->
            <xsl:if test="//dspace:field[@mdschema='dc' and @element='identifier' and @qualifier and not(starts-with(., concat('http://dx.doi.org/', $prefix)))]">
                <xsl:element name="alternateIdentifiers">
                    <xsl:apply-templates select="//dspace:field[@mdschema='dc' and @element='identifier' and @qualifier and not(starts-with(., concat('http://dx.doi.org/', $prefix)))]" />
                </xsl:element>
            </xsl:if>

            <!--
                DataCite (12)
                Add sizes.
            -->
            <xsl:if test="//dspace:field[@mdschema='dc' and @element='format' and @qualifier='extent']">             
                <xsl:element name="sizes">
                    <xsl:apply-templates select="//dspace:field[@mdschema='dc' and @element='format' and @qualifier='extent']" />      
                </xsl:element>
            </xsl:if>

            <!-- DataCite (13)
                 Add formats.
            -->
            <xsl:if test="//dspace:field[@mdschema='dc' and @element='format'][not(@qualifier='extent')]">
                <xsl:element name="formats">
                    <xsl:apply-templates select="//dspace:field[@mdschema='dc' and @element='format'][not(@qualifier='extent')]" />       
                </xsl:element>
            </xsl:if>

            <!--
                DataCite (16)
                Rights.
                Occ: 0-1
            -->
            <xsl:if test="//dspace:field[@mdschema='dc' and @element='rights']">
                <xsl:element name="rightsList">
                    <xsl:apply-templates select="//dspace:field[@mdschema='dc' and @element='rights']" />
                </xsl:element>
            </xsl:if>

            <!--
                DataCite (17)
                Add descriptions.
                Occ: 0-n
                Required Attribute: descriptionType - controlled list
            -->
            <xsl:if test="//dspace:field[@mdschema='dc' and @element='description' and (@qualifier='abstract' or @qualifier='tableofcontents' or not(@qualifier))]">
                <xsl:element name="descriptions">
                    <xsl:apply-templates select="//dspace:field[@mdschema='dc' and @element='description' and (@qualifier='abstract' or @qualifier='tableofcontents' or not(@qualifier))]" />
                </xsl:element>
            </xsl:if>
            
            <!--
                DataCite (18)
                GeoLocation
                DSpace currently doesn't store geolocations.
            -->

        </resource>
    </xsl:template>


    <!-- DataCite (2) :: Creator -->
    <xsl:template match="//dspace:field[@mdschema='dc' and @element='contributor' and @qualifier='author']">
        <creator>
            <creatorName>
                <xsl:value-of select="." />
            </creatorName>
        </creator>
    </xsl:template>

    <!-- DataCite (3) :: Title -->
    <xsl:template match="dspace:field[@mdschema='dc' and @element='title']">
        <xsl:element name="title">
            <xsl:if test="@qualifier='alternative'">
                <xsl:attribute name="titleType">AlternativeTitle</xsl:attribute>
            </xsl:if>
            <!-- DSpace does include niehter a dc.title.subtitle nor a 
                 dc.title.translated. If necessary, please create those in the 
                 metadata field registry. -->
            <xsl:if test="@qualifier='subtitle'">
                <xsl:attribute name="titleType">Subtitle</xsl:attribute>
            </xsl:if>
            <xsl:if test="@qualifier='translated'">
                <xsl:attribute name="titleType">TranslatedTitle</xsl:attribute>
            </xsl:if>
            <xsl:value-of select="." />
        </xsl:element>
    </xsl:template>

    <!--
        DataCite (6), DataCite (6.1)
        Adds subject and subjectScheme information

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

    <!--
        DataCite (7), DataCite (7.1)
        Adds contributor and contributorType information
    -->
    <xsl:template match="//dspace:field[@mdschema='dc' and @element='contributor'][not(@qualifier='author')]">
        <xsl:choose>
            <xsl:when test="@qualifier='editor'"> 
                <xsl:element name="contributor">
                    <xsl:attribute name="contributorType">Editor</xsl:attribute>
                    <contributorName>
                        <xsl:value-of select="." />
                    </contributorName>
                </xsl:element>
            </xsl:when>
            <xsl:when test="@qualifier='advisor'"> 
                <xsl:element name="contributor">
                    <xsl:attribute name="contributorType">RelatedPerson</xsl:attribute>
                    <contributorName>
                        <xsl:value-of select="." />
                    </contributorName>
                </xsl:element>
            </xsl:when>
            <xsl:when test="@qualifier='illustrator'"> 
                <xsl:element name="contributor">
                    <xsl:attribute name="contributorType">Other</xsl:attribute>
                    <contributorName>
                        <xsl:value-of select="." />
                    </contributorName>
                </xsl:element>
            </xsl:when>
            <xsl:when test="@qualifier='other'"> 
                <xsl:element name="contributor">
                    <xsl:attribute name="contributorType">Other</xsl:attribute>
                    <contributorName>
                        <xsl:value-of select="." />
                    </contributorName>
                </xsl:element>
            </xsl:when>
            <xsl:when test="not(@qualifier)"> 
                <xsl:element name="contributor">
                    <xsl:attribute name="contributorType">Other</xsl:attribute>
                    <contributorName>
                        <xsl:value-of select="." />
                    </contributorName>
                </xsl:element>
            </xsl:when>
        </xsl:choose>
    </xsl:template>

    <!--
        DataCite (8), DataCite (8.1)
        Adds Date and dateType information
    -->
    <xsl:template match="//dspace:field[@mdschema='dc' and @element='date' and 
                        (@qualifier='accessioned' 
                         or @qualifier='available' 
                         or @qualifier='copyright' 
                         or @qualifier='created' 
                         or @qualifier='issued' 
                         or @qualifier='submitted'
                         or @qualifier='updated')]">
    	<xsl:if test="@qualifier='accessioned' 
                        or @qualifier='available' 
                        or @qualifier='copyright' 
                        or @qualifier='created' 
                        or @qualifier='issued' 
                        or @qualifier='submitted'
                        or @qualifier='updated'">
            <xsl:element name="date">
                <xsl:if test="@qualifier='accessioned'">
                    <xsl:attribute name="dateType">Accepted</xsl:attribute>
                </xsl:if>
                <xsl:if test="@qualifier='available'">
                    <xsl:attribute name="dateType">Available</xsl:attribute>
                </xsl:if>
                <xsl:if test="@qualifier='copyright'">
                    <xsl:attribute name="dateType">Copyrighted</xsl:attribute>
                </xsl:if>
                <xsl:if test="@qualifier='created'">
                    <xsl:attribute name="dateType">Created</xsl:attribute>
                </xsl:if>
                <xsl:if test="@qualifier='issued'">
                    <xsl:attribute name="dateType">Issued</xsl:attribute>
                </xsl:if>
                <!-- DSpace recommends to use dc.date.submitted for theses and/or
                     dissertations. DataCite uses submitted for the "date the 
                     creator submits the resource to the publisher". -->
                <xsl:if test="@qualifier='submitted'">
                    <xsl:attribute name="dateType">Issued</xsl:attribute>
                </xsl:if>
                <xsl:if test="@qualifier='updated'">
                    <xsl:attribute name="dateType">Updated</xsl:attribute>
                </xsl:if>
	    	<xsl:value-of select="substring(., 1, 10)" />
            </xsl:element>
	</xsl:if>
    </xsl:template>

    <!--
        DataCite (9)
        Adds Language information
        Transforming the language flags according to IETF BCP 47 or ISO 639-1
    -->
    <xsl:template match="//dspace:field[@mdschema='dc' and @element='language' and (@qualifier='iso' or @qualifier='rfc3066')][1]">
        <xsl:element name="language">
            <xsl:choose>
                <xsl:when test="contains(string(text()), '_')">
                    <xsl:value-of select="translate(string(text()), '_', '-')"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="string(text())"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:element>
    </xsl:template>

    <!--
        DataCite (10), DataCite (10.1)
        Adds resourceType and resourceTypeGeneral information
    -->
    <xsl:template match="//dspace:field[@mdschema='dc' and @element='type'][1]">
        <xsl:element name="resourceType">
            <xsl:attribute name="resourceTypeGeneral">
                <xsl:choose>
                    <xsl:when test="string(text())='Animation'">Audiovisual</xsl:when>
                    <xsl:when test="string(text())='Article'">Text</xsl:when>
                    <xsl:when test="string(text())='Book'">Text</xsl:when>
                    <xsl:when test="string(text())='Book chapter'">Text</xsl:when>
                    <xsl:when test="string(text())='Dataset'">Dataset</xsl:when>
                    <xsl:when test="string(text())='Learning Object'">InteractiveResource</xsl:when>
                    <xsl:when test="string(text())='Image'">Image</xsl:when>
                    <xsl:when test="string(text())='Image, 3-D'">Image</xsl:when>
                    <xsl:when test="string(text())='Map'">Model</xsl:when>
                    <xsl:when test="string(text())='Musical Score'">Other</xsl:when>
                    <xsl:when test="string(text())='Plan or blueprint'">Model</xsl:when>
                    <xsl:when test="string(text())='Preprint'">Text</xsl:when>
                    <xsl:when test="string(text())='Presentation'">Text</xsl:when>
                    <xsl:when test="string(text())='Recording, acoustical'">Sound</xsl:when>
                    <xsl:when test="string(text())='Recording, musical'">Sound</xsl:when>
                    <xsl:when test="string(text())='Recording, oral'">Sound</xsl:when>
                    <xsl:when test="string(text())='Software'">Software</xsl:when>
                    <xsl:when test="string(text())='Technical Report'">Text</xsl:when>
                    <xsl:when test="string(text())='Thesis'">Text</xsl:when>
                    <xsl:when test="string(text())='Video'">Audiovisual</xsl:when>
                    <xsl:when test="string(text())='Working Paper'">Text</xsl:when>
                    <xsl:when test="string(text())='Other'">Other</xsl:when>
                    <xsl:otherwise>Other</xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            <xsl:value-of select="." />
        </xsl:element>
    </xsl:template>

    <!--
        DataCite (11), DataCite (11.1)
        Adds AlternativeIdentifier and alternativeIdentifierType information
        Adds all identifiers except the doi.

        This element is important as it is used to recognize for which DSpace
        objet a DOI is reserved for. The DataCiteConnector will test all
        AlternativeIdentifiers by using HandleManager.
        resolveUrlToHandle(context, altId) until one is recognized or all have
        been tested.
    -->
    <xsl:template match="//dspace:field[@mdschema='dc' and @element='identifier' and @qualifier and not(starts-with(., concat('http://dx.doi.org/', $prefix)))]">
        <xsl:element name="alternateIdentifier">
            <xsl:if test="@qualifier">
                <xsl:attribute name="alternateIdentifierType"><xsl:value-of select="@qualifier" /></xsl:attribute>
            </xsl:if>
            <xsl:value-of select="." />
        </xsl:element>
    </xsl:template>

    <!--
        DataCite (12), DataCite (12.1)
        Adds RelatedIdentifier and relatedIdentifierType information
        DataCite requires a relatedIdentifierType, but we do not know which
        type of identifier is part of the dc.relation.* fields within DSpace.
        Skip the related identifier until we find a proper solution.
    -->

    <!--
        DataCite (13)
        Adds Size information
    -->
    <xsl:template match="//dspace:field[@mdschema='dc' and @element='format' and @qualifier='extent']">
        <xsl:element name="size">
            <xsl:value-of select="." />
        </xsl:element>
    </xsl:template>

    <!--
        DataCite (14)
        Adds Format information
    -->
    <xsl:template match="//dspace:field[@mdschema='dc' and @element='format'][not(@qualifier='extent')]">
        <xsl:element name="format">
            <xsl:value-of select="." />
        </xsl:element>
    </xsl:template>
    
    <!--
        DataCite (15)
        Version information.
        As we currently do not link versions as related identifier, we skip
        the version information too.
    -->

    <!--
        DataCite (16)
        Adds Rights information
    -->
    <xsl:template match="//dspace:field[@mdschema='dc' and @element='rights']">
        <xsl:choose>
            <xsl:when test="@qualifier='uri'">
                <xsl:element name="rights">
                    <xsl:attribute name="rightsURI">
                        <xsl:value-of select="." />
                    </xsl:attribute>
                </xsl:element>
            </xsl:when>
            <xsl:otherwise>
                <xsl:element name="rights">
                    <xsl:value-of select="." />
                </xsl:element>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!--
        DataCite (17)
        Description
    -->
    <xsl:template match="//dspace:field[@mdschema='dc' and @element='description' and (@qualifier='abstract' or @qualifier='tableofcontents' or not(@qualifier))]">
        <xsl:element name="description">
            <xsl:attribute name="descriptionType">
           	<xsl:choose>
                    <xsl:when test="@qualifier='abstract'">Abstract</xsl:when>
                    <xsl:when test="@qualifier='tableofcontents'">TableOfContents</xsl:when>
               	    <xsl:otherwise>Other</xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            <xsl:value-of select="." />
        </xsl:element>
    </xsl:template>

</xsl:stylesheet>
