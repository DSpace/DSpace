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
        <oaire:resource xmlns:vc="http://www.w3.org/2007/XMLSchema-versioning"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://namespace.openaire.eu/schema/oaire/ https://www.openaire.eu/schema/repo-lit/4.0/openaire.xsd">

            <!-- datacite:title -->
            <xsl:apply-templates
                select="doc:metadata/doc:element[@name='dc']/doc:element[@name='title']" mode="datacite"/>
            <!-- datacite:creator -->
            <xsl:apply-templates
                select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']"
                mode="datacite"/>
            <datacite:contributors>
                <!-- other types of contributors !=  -->
                <xsl:apply-templates
                    select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name!='author']"
                    mode="datacite"/>
                <xsl:apply-templates select="//doc:metadata/doc:element[@name='repository']"
                    mode="contributor"/>
            </datacite:contributors>
            <!-- oaire:fundingRefence -->
            <xsl:apply-templates
                select="doc:metadata/doc:element[@name='dc']/doc:element[@name='relation']" mode="oaire"/>
            <!-- datacite:relatedIdentifier -->
            <xsl:apply-templates
                select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']" mode="datacite"/>
            <!-- if dc.identifier.uri has more than 1 value -->
            <xsl:if
                test="count(doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='uri']/doc:element/doc:field[@name='value'])>1">
                <datacite:alternateIdentifiers>
                    <xsl:apply-templates
                        select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='uri']"
                        mode="datacite_altid"/>
                </datacite:alternateIdentifiers>
            </xsl:if>
            <!-- datacite:dates and embargo -->
            <xsl:apply-templates
                select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']" mode="datacite"/>
            <!-- dc:language -->
            <xsl:apply-templates
                select="doc:metadata/doc:element[@name='dc']/doc:element[@name='language']/doc:element[@name='iso']"
                mode="dc"/>
            <!-- dc:publisher -->
            <xsl:apply-templates
                select="doc:metadata/doc:element[@name='dc']/doc:element[@name='publisher']" mode="dc"/>
            <!-- oaire:resourceType -->
            <xsl:apply-templates
                select="doc:metadata/doc:element[@name='dc']/doc:element[@name='type']" mode="oaire"/>
            <!-- dc:description -->
            <xsl:apply-templates
                select="doc:metadata/doc:element[@name='dc']/doc:element[@name='description']/doc:element[@name='abstract']"
                mode="dc"/>
            <!-- dc:format -->
            <xsl:apply-templates
                select="doc:metadata/doc:element[@name='bundles']/doc:element[@name='bundle']" mode="dc"/>
            <!-- datacite:identifier -->
            <xsl:apply-templates
                select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='uri']"
                mode="datacite"/>
            <!-- datacite:rights -->
            <xsl:apply-templates
                select="doc:metadata/doc:element[@name='dc']/doc:element[@name='rights']" mode="datacite"/>
            <!-- datacite:subject -->
            <xsl:apply-templates
                select="doc:metadata/doc:element[@name='dc']/doc:element[@name='subject']" mode="datacite"/>
            <!-- datacite:size -->
            <xsl:apply-templates
                select="doc:metadata/doc:element[@name='bundles']/doc:element[@name='bundle']" mode="datacite"/>
            <!-- oaire:file -->
            <xsl:apply-templates
                select="doc:metadata/doc:element[@name='bundles']/doc:element[@name='bundle']" mode="oaire"/>
            <!-- oaire:citation* -->
            <xsl:apply-templates
                select="doc:metadata/doc:element[@name='oaire']/doc:element[@name='citation']" mode="oaire"/>
            <!-- CREATIVE COMMON LICENSE -->
            <xsl:apply-templates
                select="doc:metadata/doc:element[@name='others']/doc:element[@name='cc']" mode="oaire" />
        </oaire:resource>
    </xsl:template>


   <!-- datacite.titles -->
   <!-- https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/v4.0.0/field_title.html -->
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
            <datacite:title>
                <xsl:call-template name="xmlLanguage">
                    <xsl:with-param name="name" select="../@name"/>
                </xsl:call-template>
                <xsl:attribute name="titleType">
                    <xsl:call-template name="getTitleType">
                         <xsl:with-param name="elementName" select="../../@name"/>
                    </xsl:call-template>
                </xsl:attribute>
                <xsl:value-of select="."/>
            </datacite:title>
        </xsl:for-each>
    </xsl:template>


    <!-- datacite.creators -->
    <!-- https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/v4.0.0/field_creator.html -->
    <xsl:template
        match="doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']" mode="datacite">
        <datacite:creators>
            <!-- datacite.creator -->
            <xsl:for-each select="./doc:element/doc:field[@name='value']">
                <xsl:variable name="isRelatedEntity">
                    <xsl:call-template name="isRelatedEntity">
                        <xsl:with-param name="element" select="."/>
                    </xsl:call-template>
                </xsl:variable>
                <xsl:choose>
                    <!-- if next sibling is authority and starts with virtual:: -->
                    <xsl:when test="$isRelatedEntity = 'true'">
                        <xsl:variable name="entity">
                            <xsl:call-template name="buildEntityNode">
                                <xsl:with-param name="element" select="."/>
                            </xsl:call-template>
                        </xsl:variable>
                        <xsl:apply-templates select="$entity" mode="entity_creator"/>
                    </xsl:when>
                    <!-- simple text metadata -->
                    <xsl:otherwise>
                        <datacite:creator>
                            <datacite:creatorName>
                                <xsl:value-of select="./text()"/>
                            </datacite:creatorName>
                        </datacite:creator>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:for-each>
        </datacite:creators>
    </xsl:template>

    <!-- datacite:creator -->
    <xsl:template match="doc:element" mode="entity_creator">
        <xsl:variable name="isOrgUnitEntity">
            <xsl:call-template name="hasOrganizationLegalNameField">
                <xsl:with-param name="element" select="."/>
            </xsl:call-template>
        </xsl:variable>
        <datacite:creator>
            <datacite:creatorName>
                <!-- determining which name type to use Organizational/Personal -->
                <xsl:attribute name="nameType">
                    <xsl:choose>
                        <xsl:when test="$isOrgUnitEntity = 'true'">
                            <xsl:text>Organizational</xsl:text>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:text>Personal</xsl:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:attribute>
                <xsl:choose>
                    <xsl:when test="$isOrgUnitEntity = 'true'">
                        <xsl:value-of select="doc:field[starts-with(@name,'organization.legalName')]"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="doc:field[starts-with(@name,'dc.contributor.author')]"/>
                    </xsl:otherwise>
                </xsl:choose>
            </datacite:creatorName>
            <xsl:apply-templates select="doc:field" mode="entity_author"/>
        </datacite:creator>
    </xsl:template>


    <!-- datacite:contributors -->
    <!-- https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/v4.0.0/field_contributor.html -->
    <xsl:template
        match="doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name!='author']" mode="datacite">
        <xsl:for-each select="./doc:element/doc:field[@name='value']">
            <xsl:variable name="isRelatedEntity">
                <xsl:call-template name="isRelatedEntity">
                    <xsl:with-param name="element" select="."/>
                </xsl:call-template>
            </xsl:variable>
            <xsl:choose>
                <!-- if next sibling is authority and starts with virtual:: -->
                <xsl:when test="$isRelatedEntity = 'true'">
                    <xsl:variable name="entity">
                        <xsl:call-template name="buildEntityNode">
                            <xsl:with-param name="element" select="."/>
                        </xsl:call-template>
                    </xsl:variable>
                    <xsl:apply-templates select="$entity" mode="entity_contributor"/>
                </xsl:when>
                <!-- simple text metadata -->
                <xsl:otherwise>
                    <datacite:contributor>
                        <datacite:contributorName>
                            <xsl:value-of select="./text()"/>
                        </datacite:contributorName>
                    </datacite:contributor>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:for-each>
    </xsl:template>

    <!-- datacite:contributor -->
    <xsl:template match="doc:element" mode="entity_contributor">
        <xsl:variable name="isOrgUnitEntity">
            <xsl:call-template name="hasOrganizationLegalNameField">
                <xsl:with-param name="element" select="."/>
            </xsl:call-template>
        </xsl:variable>
        <datacite:contributor>
            <!-- contributorType is a mandatory attribute for each contributor -->
            <xsl:attribute name="contributorType">
                <xsl:call-template name="getContributorType">
                    <xsl:with-param name="elementName" select="./@name"/>
                </xsl:call-template>
            </xsl:attribute>
            <datacite:contributorName>
                <!-- determining which name type to use Organizational/Personal -->
                <xsl:attribute name="nameType">
                    <xsl:choose>
                        <xsl:when test="$isOrgUnitEntity = 'true'">
                            <xsl:text>Organizational</xsl:text>
                        </xsl:when>
                        <!-- simple text metadata -->
                        <xsl:otherwise>
                            <xsl:text>Personal</xsl:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:attribute>
                <xsl:choose>
                    <xsl:when test="$isOrgUnitEntity = 'true'">
                        <xsl:value-of select="doc:field[starts-with(@name,'organization.legalName')]"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="doc:field[starts-with(@name,'dc.contributor')]"/>
                    </xsl:otherwise>
                </xsl:choose>
            </datacite:contributorName>
            <xsl:apply-templates select="doc:field" mode="entity_author"/>
        </datacite:contributor>
    </xsl:template>

    <!-- This repository is considered a contributor of this work
        a contributor of type HostingInstitution
      -->
    <xsl:template match="doc:element[@name='repository']" mode="contributor">
        <datacite:contributor contributorType="HostingInstitution">
            <datacite:contributorName nameType="Organizational">
                <xsl:value-of select="./doc:field[@name='name']"/>
            </datacite:contributorName>
            <xsl:variable name="mail" select="./doc:field[@name='mail']"/>
            <xsl:if test="$mail">
                <datacite:nameIdentifier>
                    <xsl:attribute name="nameIdentifierScheme">
                    <xsl:text>e-mail</xsl:text>
                 </xsl:attribute>
                    <xsl:attribute name="schemeURI">
                    <xsl:value-of select="concat('mailto:',$mail)"/>
                 </xsl:attribute>
                    <xsl:value-of select="$mail"/>
                </datacite:nameIdentifier>
            </xsl:if>
        </datacite:contributor>
    </xsl:template>

    <!-- This template will create a type of identifier exclusive for DSpace
         which is a resolver for that record and, at same time, the REST API end point
     -->
    <xsl:template match="doc:field[starts-with(@name,'relation.isAuthorOfPublication')]"
        mode="entity_author">
        <xsl:variable name="url">
            <xsl:call-template name="getDSpaceURL"/>
        </xsl:variable>
        <datacite:nameIdentifier nameIdentifierScheme="DSpace" schemeURI="http://dspace.org">
            <xsl:value-of select="concat($url,'/items/',./text())"/>
        </datacite:nameIdentifier>
    </xsl:template>

    <!-- This template creates the sub-element <datacite:givenName> from a Person built entity -->
    <xsl:template match="doc:field[starts-with(@name,'person.givenName')]" mode="entity_author">
        <datacite:givenName>
            <xsl:value-of select="./text()"/>
        </datacite:givenName>
    </xsl:template>

    <!-- This template creates the sub-element <datacite:familyName> from a Person built entity -->
    <xsl:template match="doc:field[starts-with(@name,'person.familyName')]" mode="entity_author">
        <datacite:familyName>
            <xsl:value-of select="./text()"/>
        </datacite:familyName>
    </xsl:template>

    <!-- This template creates the sub-element <datacite:affiliation> from a Person built entity -->
    <xsl:template match="doc:field[starts-with(@name,'person.affiliation.name')]" mode="entity_author">
        <datacite:affiliation>
            <xsl:value-of select="./text()"/>
        </datacite:affiliation>
    </xsl:template>

    <!-- This template creates the sub-element <datacite:nameIdentifier> from a Person built entity -->
    <xsl:template match="doc:field[starts-with(@name,'person.identifier.*')]" mode="entity_author">
        <!-- TODO resolve the scheme based on the value -->
        <datacite:nameIdentifier nameIdentifierScheme="" schemeURI="">
            <xsl:value-of select="./text()"/>
        </datacite:nameIdentifier>
    </xsl:template>

    <!-- This template creates the sub-element <datacite:nameIdentifier> of type ORCID from a Person built entity -->
    <xsl:template match="doc:field[starts-with(@name,'person.identifier.orcid')]" mode="entity_author">
        <datacite:nameIdentifier nameIdentifierScheme="ORCID" schemeURI="http://orcid.org">
            <xsl:value-of select="./text()"/>
        </datacite:nameIdentifier>
    </xsl:template>

    <!-- This template creates the sub-element <datacite:nameIdentifier> of type Scopus Author ID from a Person built entity -->
    <xsl:template match="doc:field[starts-with(@name,'person.identifier.scopus-author-id')]"
        mode="entity_author">
        <datacite:nameIdentifier nameIdentifierScheme="Scopus Author ID"
            schemeURI="https://www.scopus.com">
            <xsl:value-of select="./text()"/>
        </datacite:nameIdentifier>
    </xsl:template>

    <!-- This template creates the sub-element <datacite:nameIdentifier> of type Ciencia ID from a Person built entity -->
    <xsl:template match="doc:field[starts-with(@name,'person.identifier.ciencia-id')]" mode="entity_author">
        <datacite:nameIdentifier nameIdentifierScheme="Ciência ID"
            schemeURI="https://www.ciencia-id.pt">
            <xsl:value-of select="./text()"/>
        </datacite:nameIdentifier>
    </xsl:template>

    <!-- This template creates the sub-element <datacite:nameIdentifier> of type Researcher ID from a Person built entity -->
    <xsl:template match="doc:field[starts-with(@name,'person.identifier.rid')]" mode="entity_author">
        <datacite:nameIdentifier nameIdentifierScheme="Researcher ID"
            schemeURI="https://www.researcherid.com">
            <xsl:value-of select="./text()"/>
        </datacite:nameIdentifier>
    </xsl:template>

    <!-- This template creates the sub-element <datacite:nameIdentifier> of type Google Scholar ID from a Person built entity -->
    <xsl:template match="doc:field[starts-with(@name,'person.identifier.gsid')]" mode="entity_author">
        <datacite:nameIdentifier nameIdentifierScheme="Google Scholar ID"
            schemeURI="https://scholar.google.com">
            <xsl:value-of select="./text()"/>
        </datacite:nameIdentifier>
    </xsl:template>

    <!-- This template creates the sub-element <datacite:nameIdentifier> of type ISNI from a Person built entity -->
    <xsl:template match="doc:field[starts-with(@name,'person.identifier.isni')]" mode="entity_author">
        <datacite:nameIdentifier nameIdentifierScheme="ISNI" schemeURI="http://www.isni.org">
            <xsl:value-of select="./text()"/>
        </datacite:nameIdentifier>
    </xsl:template>

    <!-- This template creates the sub-element <datacite:nameIdentifier> for an organization from a built entity -->
    <xsl:template match="doc:field[starts-with(@name,'organization.identifier.*')]" mode="entity_author">
        <!-- TODO resolve the scheme based on the value -->
        <datacite:nameIdentifier nameIdentifierScheme="" schemeURI="">
            <xsl:value-of select="./text()"/>
        </datacite:nameIdentifier>
    </xsl:template>

    <!-- This template creates the sub-element <datacite:nameIdentifier> of type ISNI for an organization from a built entity -->
    <xsl:template match="doc:field[starts-with(@name,'organization.identifier.isni')]" mode="entity_author">
        <datacite:nameIdentifier nameIdentifierScheme="ISNI" schemeURI="http://www.isni.org">
            <xsl:value-of select="./text()"/>
        </datacite:nameIdentifier>
    </xsl:template>


    <!-- oaire:fundingReferences -->
    <!-- https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/v4.0.0/field_projectid.html -->
    <xsl:template match="doc:element[@name='dc']/doc:element[@name='relation']" mode="oaire">
        <oaire:fundingReferences>
            <xsl:for-each select="./doc:element/doc:field[@name='value']">
                <xsl:variable name="isRelatedEntity">
                    <xsl:call-template name="isRelatedEntity">
                        <xsl:with-param name="element" select="."/>
                    </xsl:call-template>
                </xsl:variable>
                <!-- if next sibling is authority and starts with virtual:: -->
                <xsl:if test="$isRelatedEntity = 'true'">
                    <xsl:variable name="entity">
                        <xsl:call-template name="buildEntityNode">
                            <xsl:with-param name="element" select="."/>
                        </xsl:call-template>
                    </xsl:variable>
                    <xsl:apply-templates select="$entity" mode="entity_funding"/>
                </xsl:if>
            </xsl:for-each>
        </oaire:fundingReferences>
    </xsl:template>

    <!-- oaire:fundingReference -->
    <xsl:template match="doc:element" mode="entity_funding">
        <oaire:fundingReference>
            <xsl:apply-templates select="doc:field" mode="entity_funding"/>
        </oaire:fundingReference>
    </xsl:template>

    <!-- This template creates the sub-element <oaire:awardTitle> from a Funded Project built entity -->
    <xsl:template match="doc:field[starts-with(@name,'dc.relation')]" mode="entity_funding">
        <oaire:awardTitle>
            <xsl:value-of select="./text()"/>
        </oaire:awardTitle>
    </xsl:template>

    <!-- This template creates the sub-element <oaire:funderName> from a Funded Project built entity -->
    <xsl:template match="doc:field[starts-with(@name,'project.funder.name')]" mode="entity_funding">
        <oaire:funderName>
            <xsl:value-of select="./text()"/>
        </oaire:funderName>
    </xsl:template>

    <!-- This template creates the sub-element <oaire:funderIdentifier> from a Funded Project built entity -->
    <xsl:template match="doc:field[starts-with(@name,'project.funder.identifier')]" mode="entity_funding">
        <!-- TODO: recognize the type ISNI / GRID / Crossref Funder -->
        <oaire:funderIdentifier funderIdentifierType="Crossref Funder ID">
            <xsl:value-of select="./text()"/>
        </oaire:funderIdentifier>
    </xsl:template>

    <!-- This template creates the sub-element <oaire:fundingStream> from a Funded Project built entity -->
    <xsl:template match="doc:field[starts-with(@name,'oaire.fundingStream')]" mode="entity_funding">
        <oaire:fundingStream>
            <xsl:value-of select="./text()"/>
        </oaire:fundingStream>
    </xsl:template>

    <!-- This template creates the sub-element <oaire:awardNumber> from a Funded Project built entity -->
    <xsl:template match="doc:field[starts-with(@name,'oaire.awardNumber')]" mode="entity_funding">
        <!-- TODO: get the awarduri from doc:field@name='oaire.awardURI -->
        <oaire:awardNumber>
            <xsl:attribute name="awardURI">
                <xsl:apply-templates select="../doc:field[starts-with(@name,'oaire.awardURI')]"
                mode="entity_funding_param"/>
            </xsl:attribute>
            <xsl:value-of select="./text()"/>
        </oaire:awardNumber>
    </xsl:template>

    <!-- This template creates the property awardURI from a Funded Project built entity -->
    <xsl:template match="doc:field[starts-with(@name,'oaire.awardURI')]" mode="entity_funding_param">
        <xsl:value-of select="./text()"/>
    </xsl:template>

    <!-- datacite:relatedIdentifiers -->
    <!-- https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/v4.0.0/field_relatedidentifier.html -->
    <xsl:template match="doc:element[@name='dc']/doc:element[@name='identifier']" mode="datacite">
        <datacite:relatedIdentifiers>
            <xsl:apply-templates select="./doc:element" mode="datacite_ids"/>
        </datacite:relatedIdentifiers>
    </xsl:template>
        
   <!-- datacite:relatedIdentifier -->
   <!-- handle: dc.identifier.issn -->
    <xsl:template match="doc:element[@name='issn']" mode="datacite_ids">
        <xsl:for-each select=".//doc:field[@name='value']">
            <xsl:call-template name="relatedIdentifierTemplate">
                <xsl:with-param name="value" select="text()"/>
                <xsl:with-param name="relatedIdentifierType" select="'ISSN'"/>
                <xsl:with-param name="relationType" select="'IsPartOf'"/>
            </xsl:call-template>
        </xsl:for-each>
    </xsl:template>

    <!-- handle: dc.identifier.ismn -->
    <xsl:template match="doc:element[@name='ismn']" mode="datacite_ids">
        <xsl:for-each select=".//doc:field[@name='value']">
            <xsl:call-template name="relatedIdentifierTemplate">
                <xsl:with-param name="value"
                    select="concat('ISMN:',normalize-space(text()))"/>
                <xsl:with-param name="relatedIdentifierType" select="'URN'"/>
                <xsl:with-param name="relationType" select="'IsPartOf'"/>
            </xsl:call-template>
        </xsl:for-each>
    </xsl:template>

    <!-- handle: dc.identifier.govdoc -->
    <xsl:template match="doc:element[@name='govdoc']" mode="datacite_ids">
        <xsl:for-each select=".//doc:field[@name='value']">
            <xsl:call-template name="relatedIdentifierTemplate">
                <xsl:with-param name="value"
                    select="concat('govdoc:',normalize-space(text()))"/>
                <xsl:with-param name="relatedIdentifierType" select="'URN'"/>
                <xsl:with-param name="relationType" select="'IsPartOf'"/>
            </xsl:call-template>
        </xsl:for-each>
    </xsl:template>

    <!-- handle: dc.identifier.isbn -->
    <xsl:template match="doc:element[@name='isbn']" mode="datacite_ids">
        <xsl:for-each select=".//doc:field[@name='value']">
            <xsl:call-template name="relatedIdentifierTemplate">
                <xsl:with-param name="value" select="text()"/>
                <xsl:with-param name="relatedIdentifierType" select="'ISBN'"/>
                <xsl:with-param name="relationType" select="'IsPartOf'"/>
            </xsl:call-template>
        </xsl:for-each>
    </xsl:template>

    <!-- handle: dc.identifier.sici -->
    <xsl:template match="doc:element[@name='sici']" mode="datacite_ids">
        <xsl:for-each select=".//doc:field[@name='value']">
            <xsl:call-template name="relatedIdentifierTemplate">
                <xsl:with-param name="value"
                    select="concat('sici:',normalize-space(text()))"/>
                <xsl:with-param name="relatedIdentifierType" select="'URN'"/>
                <xsl:with-param name="relationType" select="'IsPartOf'"/>
            </xsl:call-template>
        </xsl:for-each>
    </xsl:template>

    <!-- handle: dc.identifier.other -->
    <xsl:template match="doc:element[@name='other']" mode="datacite_ids">
        <xsl:for-each select=".//doc:field[@name='value']">
            <xsl:call-template name="relatedIdentifierTemplate">
                <xsl:with-param name="value" select="text()"/>
                <xsl:with-param name="relatedIdentifierType" select="'URN'"/>
                <xsl:with-param name="relationType" select="'IsPartOf'"/>
            </xsl:call-template>
        </xsl:for-each>
    </xsl:template>    

    <!-- handle: dc.identifier.doi -->
    <xsl:template match="doc:element[@name='doi']" mode="datacite_ids">
        <xsl:for-each select=".//doc:field[@name='value']">
            <xsl:call-template name="relatedIdentifierTemplate">
                <xsl:with-param name="value" select="text()"/>
                <xsl:with-param name="relatedIdentifierType" select="'DOI'"/>
                <xsl:with-param name="relationType" select="'IsPartOf'"/>
            </xsl:call-template>
        </xsl:for-each>
    </xsl:template>

    <!-- handle: dc.identifier.* -->
    <xsl:template match="doc:element" mode="datacite_ids"/>
    
    <!-- template for all relatedIdentifier -->
    <xsl:template name="relatedIdentifierTemplate">
        <xsl:param name="value"/>
        <xsl:param name="relatedIdentifierType"/>
        <xsl:param name="relationType"/>
        <datacite:relatedIdentifier>
            <xsl:attribute name="relatedIdentifierType">
                    <xsl:value-of select="$relatedIdentifierType"/>
                </xsl:attribute>
            <xsl:attribute name="relationType">
                    <xsl:value-of select="$relationType"/>
                </xsl:attribute>
            <xsl:value-of select="normalize-space($value)"/>
        </datacite:relatedIdentifier>
    </xsl:template>


   <!--  datacite:identifier  -->
   <!-- In the repository context Resource Identifier will be the Handle or the generated DOI that is present in dc.identifier.uri. -->
   <!-- https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/v4.0.0/field_resourceidentifier.html -->
    <xsl:template match="doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='uri']"
        mode="datacite">
        <xsl:variable name="identifierType">
            <!--  only consider the first dc.identifier.uri -->
            <xsl:call-template name="resolveFieldType">
                <xsl:with-param name="field" select="./doc:element[1]/doc:field[@name='value'][1]"/>
            </xsl:call-template>
        </xsl:variable>
        <!-- only process the first element -->
        <datacite:identifier>
            <xsl:attribute name="identifierType">
                <xsl:value-of select="$identifierType"/>
            </xsl:attribute>
            <xsl:value-of select="./doc:element[1]/doc:field[@name='value'][1]"/>
        </datacite:identifier>
    </xsl:template>

    <!--  datacite:alternateIdentifier dc.identifier.uri  -->
    <!--  https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/v4.0.0/field_alternativeidentifier.html -->
    <xsl:template match="doc:element[@name='uri']" mode="datacite_altid">
        <xsl:for-each select="./doc:element/doc:field[@name='value']">
        <!-- don't process the first element -->
            <xsl:if test="position()>1">
                <xsl:variable name="identifierType">
                    <xsl:call-template name="resolveFieldType">
                        <xsl:with-param name="field" select="."/>
                    </xsl:call-template>
                </xsl:variable>

                <datacite:alternateIdentifier>
                    <xsl:attribute name="alternateIdentifierType">
                <xsl:value-of select="$identifierType"/>
            </xsl:attribute>
                    <xsl:value-of select="./text()"/>
                </datacite:alternateIdentifier>
            </xsl:if>
        </xsl:for-each>
    </xsl:template>

   <!-- datacite:rights -->
   <!-- https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/v4.0.0/field_accessrights.html -->
    <xsl:template match="doc:element[@name='dc']/doc:element[@name='rights']/doc:element/doc:field[@name='value']" mode="datacite">
        <xsl:variable name="rightsValue" select="text()"/>
        <xsl:variable name="rightsURI">
            <xsl:call-template name="resolveRightsURI">
                <xsl:with-param name="field" select="$rightsValue"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:variable name="lc_rightsValue">
            <xsl:call-template name="lowercase">
                <xsl:with-param name="value" select="$rightsValue"/>
            </xsl:call-template>
        </xsl:variable>
        <!-- We are checking to ensure that only values ending in "access" can be used as datacite:rights. 
        This is a valid solution as we pre-normalize dc.rights values in openaire4.xsl to end in the term 
        "access" according to COAR Controlled Vocabulary -->
        <xsl:if test="ends-with($lc_rightsValue,'access')">
            <datacite:rights>
                <xsl:if test="$rightsURI">
                    <xsl:attribute name="rightsURI">
                    <xsl:value-of select="$rightsURI"/>
                </xsl:attribute>
                </xsl:if>
                <xsl:value-of select="$rightsValue"/>
            </datacite:rights>
        </xsl:if>
    </xsl:template>


   <!-- datacite:subjects -->
   <!-- https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/v4.0.0/field_subject.html -->
    <xsl:template match="doc:element[@name='dc']/doc:element[@name='subject']" mode="datacite">
        <datacite:subjects>
            <xsl:for-each select="./doc:element">
                <xsl:apply-templates select="." mode="datacite"/>
            </xsl:for-each>
        </datacite:subjects>
    </xsl:template>

   <!-- datacite:subject -->
    <xsl:template match="doc:element[@name='dc']/doc:element[@name='subject']/doc:element" mode="datacite">
        <xsl:for-each select="./doc:field[@name='value']">
            <datacite:subject>
                <xsl:value-of select="./text()"/>
            </datacite:subject>
        </xsl:for-each>
    </xsl:template>


    <!-- datacite.dates -->
    <!-- https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/v4.0.0/field_embargoenddate.html -->
    <!-- https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/v4.0.0/field_publicationdate.html -->
    <xsl:template match="doc:element[@name='dc']/doc:element[@name='date']" mode="datacite">
        <datacite:dates>
        <!-- datacite:date (embargo) -->
            <xsl:for-each select="./doc:element">
                <xsl:apply-templates select="." mode="datacite"/>
            </xsl:for-each>
        </datacite:dates>
    </xsl:template>

    <!-- datacite.date @name=issued or @name=accepted -->
    <!-- https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/v4.0.0/field_publicationdate.html -->
    <xsl:template
        match="doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued' or @name='accepted']"
        mode="datacite">
        <xsl:variable name="dc_date_value" select="doc:element/doc:field[@name='value']/text()"/>
        <datacite:date dateType="Accepted">
            <xsl:value-of select="$dc_date_value"/>
        </datacite:date>
        <!-- 
            datacite.date issued is different from dc.date.issued
            please check - https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/v4.0.0/field_publicationdate.html
         -->
        <datacite:date dateType="Issued">
            <xsl:value-of select="$dc_date_value"/>
        </datacite:date>
    </xsl:template> 

    <!-- datacite.date @name=accessioned -->
    <xsl:template
        match="doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='accessioned']" mode="datacite"/>

    <!-- datacite.date -->
    <xsl:template
        match="doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name!='issued' and @name!='accepted' and @name!='accessioned']"
        mode="datacite">
        <xsl:variable name="dateType">
            <xsl:call-template name="getDateType">
                <xsl:with-param name="elementName" select="./@name"/>
            </xsl:call-template>
        </xsl:variable>
        <!-- only consider elements with valid date types -->
        <xsl:if test="$dateType != ''">
            <datacite:date>
                <xsl:attribute name="dateType">
                    <xsl:value-of select="$dateType"/>
                </xsl:attribute>
                <xsl:value-of select="./doc:element/doc:field[@name='value']/text()"/>
            </datacite:date>
        </xsl:if>
    </xsl:template>


    <!-- dc:language -->
    <!-- https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/v4.0.0/field_language.html -->
    <xsl:template match="doc:element[@name='dc']/doc:element[@name='language']/doc:element[@name='iso']"
        mode="dc">
        <xsl:for-each select="./doc:element/doc:field[@name='value']">
	        <dc:language>
	            <xsl:value-of select="./text()"/>
	        </dc:language>
        </xsl:for-each>
    </xsl:template>


    <!-- dc:publisher -->
    <!-- https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/v4.0.0/field_publisher.html -->
    <xsl:template match="doc:element[@name='dc']/doc:element[@name='publisher']" mode="dc">
    	<xsl:for-each select="./doc:element/doc:field[@name='value']">
	       <dc:publisher>
	           <xsl:value-of select="./text()"/>
	       </dc:publisher>
        </xsl:for-each>
    </xsl:template>


    <!-- oaire:resourceType -->
    <!-- https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/v4.0.0/field_publicationtype.html -->
    <xsl:template match="doc:element[@name='dc']/doc:element[@name='type']/doc:element" mode="oaire">
        <xsl:variable name="resourceTypeGeneral">
            <xsl:call-template name="resolveResourceTypeGeneral">
                <xsl:with-param name="field" select="./doc:field[@name='value']/text()"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:variable name="resourceTypeURI">
            <xsl:call-template name="resolveResourceTypeURI">
                <xsl:with-param name="field" select="./doc:field[@name='value']/text()"/>
            </xsl:call-template>
        </xsl:variable>
        <oaire:resourceType>
            <xsl:attribute name="resourceTypeGeneral">
                <xsl:value-of select="$resourceTypeGeneral"/>
            </xsl:attribute>
            <xsl:attribute name="uri">
                <xsl:value-of select="$resourceTypeURI"/>
            </xsl:attribute>
            <xsl:value-of select="./doc:field[@name='value']/text()"/>
        </oaire:resourceType>
    </xsl:template>


    <!-- dc:description -->
    <!-- https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/v4.0.0/field_description.html -->
    <xsl:template
        match="doc:element[@name='dc']/doc:element[@name='description']/doc:element[@name='abstract']/doc:element"
        mode="dc">
        <dc:description>
            <xsl:call-template name="xmlLanguage">
                <xsl:with-param name="name" select="@name"/>
            </xsl:call-template>
            <xsl:value-of select="./doc:field[@name='value']"/>
        </dc:description>
    </xsl:template>

    <!-- dc:format -->
    <!-- https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/v4.0.0/field_size.html -->
    <xsl:template match="doc:element[@name='bundles']/doc:element[@name='bundle']" mode="dc">
        <xsl:if test="doc:field[@name='name' and text()='ORIGINAL']">
            <xsl:for-each select="doc:element[@name='bitstreams']/doc:element[@name='bitstream']">
                <xsl:apply-templates select="." mode="dc"/>
            </xsl:for-each>
        </xsl:if>
    </xsl:template>
    <xsl:template match="doc:element[@name='bitstreams']/doc:element[@name='bitstream']" mode="dc">
        <dc:format>
            <xsl:value-of select="doc:field[@name='format']"/>
        </dc:format>
    </xsl:template>


    <!-- datacite:sizes -->
    <!-- https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/v4.0.0/field_size.html -->
    <xsl:template match="doc:element[@name='bundles']/doc:element[@name='bundle']" mode="datacite">
        <datacite:sizes>
            <xsl:if test="doc:field[@name='name' and text()='ORIGINAL']">
                <xsl:for-each select="doc:element[@name='bitstreams']/doc:element[@name='bitstream']">
                    <xsl:apply-templates select="." mode="datacite"/>
                </xsl:for-each>
            </xsl:if>
        </datacite:sizes>
    </xsl:template>
    
     <!-- datacite:size -->
    <xsl:template match="doc:element[@name='bitstreams']/doc:element[@name='bitstream']" mode="datacite">
        <datacite:size>
            <xsl:value-of select="concat(doc:field[@name='size'],' bytes')"/>
        </datacite:size>
    </xsl:template>

   <!-- oaire:file -->
   <!-- https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/v4.0.0/field_filelocation.html -->
    <xsl:template match="doc:element[@name='bundles']/doc:element[@name='bundle']" mode="oaire">
       <!-- only consider ORIGINAL bundle -->
        <xsl:if test="doc:field[@name='name' and text()='ORIGINAL']">
            <xsl:for-each select="doc:element[@name='bitstreams']/doc:element[@name='bitstream']">
                <xsl:apply-templates select="." mode="oaire"/>
            </xsl:for-each>
        </xsl:if>
    </xsl:template>

    <!-- oaire:file -->
    <!-- processing of each bitstream entry -->
    <xsl:template match="doc:element[@name='bitstreams']/doc:element[@name='bitstream']" mode="oaire">
        <oaire:file>
            <xsl:attribute name="accessRightsURI">
                <xsl:call-template name="getRightsURI"/>
         </xsl:attribute>
            <xsl:attribute name="mimeType">
            <xsl:value-of select="doc:field[@name='format']"/>
         </xsl:attribute>
            <xsl:attribute name="objectType">
            <xsl:choose>
                <!-- Currently there is no available way to identify the type of the bitstream -->
                <xsl:when test="1">
                    <xsl:text>fulltext</xsl:text>
                </xsl:when>
                <!--xsl:when test="$type='dataset'">
                    <xsl:text>dataset</xsl:text>
                </xsl:when>
                <xsl:when test="$type='software'">
                    <xsl:text>software</xsl:text>
                </xsl:when>
                <xsl:when test="$type='article'">
                    <xsl:text>fulltext</xsl:text>
                </xsl:when-->
                <xsl:otherwise>                  
                    <xsl:text>other</xsl:text>
                </xsl:otherwise>
             </xsl:choose>
           </xsl:attribute>
            <xsl:value-of select="doc:field[@name='url']"/>
        </oaire:file>
    </xsl:template>


    <!-- https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/v4.0.0/field_citationtitle.html -->
    <xsl:template
        match="doc:element[@name='oaire']/doc:element[@name='citation']/doc:element[@name='title']" mode="oaire">
      <!-- citationTitle -->
        <oaire:citationTitle>
            <xsl:value-of select="./doc:element/doc:field[@name='value']"/>
        </oaire:citationTitle>
    </xsl:template>

    <!-- https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/v4.0.0/field_citationedition.html -->
    <xsl:template
        match="doc:element[@name='oaire']/doc:element[@name='citation']/doc:element[@name='edition']" mode="oaire">
      <!-- citationEdition -->
        <oaire:citationEdition>
            <xsl:value-of select="./doc:element/doc:field[@name='value']"/>
        </oaire:citationEdition>
    </xsl:template>

    <!-- https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/v4.0.0/field_citationvolume.html -->
    <xsl:template
        match="doc:element[@name='oaire']/doc:element[@name='citation']/doc:element[@name='volume']" mode="oaire">
      <!-- citationVolume -->
        <oaire:citationVolume>
            <xsl:value-of select="./doc:element/doc:field[@name='value']"/>
        </oaire:citationVolume>
    </xsl:template>

    <!-- https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/v4.0.0/field_citationissue.html -->
    <xsl:template
        match="doc:element[@name='oaire']/doc:element[@name='citation']/doc:element[@name='issue']" mode="oaire">
      <!-- citationVolume -->
        <oaire:citationIssue>
            <xsl:value-of select="./doc:element/doc:field[@name='value']"/>
        </oaire:citationIssue>
    </xsl:template>

    <!-- https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/v4.0.0/field_citationstartpage.html -->
    <xsl:template
        match="doc:element[@name='oaire']/doc:element[@name='citation']/doc:element[@name='startPage']" mode="oaire">
      <!-- citationStartPage -->
        <oaire:citationStartPage>
            <xsl:value-of select="./doc:element/doc:field[@name='value']"/>
        </oaire:citationStartPage>
    </xsl:template>

    <!-- https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/v4.0.0/field_citationendpage.html -->
    <xsl:template
        match="doc:element[@name='oaire']/doc:element[@name='citation']/doc:element[@name='endPage']" mode="oaire">
      <!-- citationEndPage -->
        <oaire:citationEndPage>
            <xsl:value-of select="./doc:element/doc:field[@name='value']"/>
        </oaire:citationEndPage>
    </xsl:template>

    <!-- https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/v4.0.0/field_citationconferenceplace.html -->
    <xsl:template
        match="doc:element[@name='oaire']/doc:element[@name='citation']/doc:element[@name='conferencePlace']"
        mode="oaire">
      <!-- citationConferencePlace -->
        <oaire:citationConferencePlace>
            <xsl:value-of select="./doc:element/doc:field[@name='value']"/>
        </oaire:citationConferencePlace>
    </xsl:template>

    <!-- https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/v4.0.0/field_citationconferencedate.html -->
    <xsl:template
        match="doc:element[@name='oaire']/doc:element[@name='citation']/doc:element[@name='conferenceDate']"
        mode="oaire">
      <!-- citationConferenceDate -->
        <oaire:citationConferenceDate>
            <xsl:value-of select="./doc:element/doc:field[@name='value']"/>
        </oaire:citationConferenceDate>
    </xsl:template>



   <!--  -->
   <!-- Auxiliary templates - get types -->
   <!--  -->

    <!-- This template will retrieve the type of a title element (like: alternative) based on the element name -->
    <xsl:template name="getTitleType">
        <xsl:param name="elementName"/>
        <xsl:variable name="lc_title_type">
            <xsl:call-template name="lowercase">
                <xsl:with-param name="value" select="$elementName"/>
            </xsl:call-template>
        </xsl:variable>
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
    </xsl:template>

    <!-- This template will retrieve the type of a contributor based on the element name -->
    <xsl:template name="getContributorType">
        <xsl:param name="elementName"/>
        <xsl:variable name="lc_contributor_type">
            <xsl:call-template name="lowercase">
                <xsl:with-param name="value" select="$elementName"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="$lc_contributor_type = 'advisor' or $lc_contributor_type = 'supervisor'">
                <xsl:text>Supervisor</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_contributor_type = 'editor'">
                <xsl:text>Editor</xsl:text>
            </xsl:when>
            <xsl:otherwise>
                <xsl:text>Other</xsl:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- This template will retrieve the type of a date based on the element name -->
    <xsl:template name="getDateType">
        <xsl:param name="elementName"/>
        <xsl:variable name="lc_dc_date_type">
            <xsl:call-template name="lowercase">
                <xsl:with-param name="value" select="$elementName"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="$lc_dc_date_type='available' or  $lc_dc_date_type = 'embargo'">
                <xsl:text>Available</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_date_type='collected'">
                <xsl:text>Collected</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_date_type='copyrighted' or $lc_dc_date_type='copyright'">
                <xsl:text>Copyrighted</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_date_type='created'">
                <xsl:text>Created</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_date_type='submitted'">
                <xsl:text>Submitted</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_date_type='updated'">
                <xsl:text>Updated</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_date_type='valid'">
                <xsl:text>Valid</xsl:text>
            </xsl:when>
        </xsl:choose>
    </xsl:template>

    <!-- This template will retrieve the identifier type based on the element name -->
    <!-- 
        there are some special cases like DOI or HANDLE which the type is also
        inferred from the value itself
      -->
    <xsl:template name="getRelatedIdentifierType">
        <xsl:param name="element"/>
        <xsl:variable name="lc_identifier_type">
            <xsl:call-template name="lowercase">
                <xsl:with-param name="value" select="$element/@name"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:variable name="isHandle">
            <xsl:call-template name="isHandle">
                <xsl:with-param name="field" select="$element/doc:field"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:variable name="isDOI">
            <xsl:call-template name="isDOI">
                <xsl:with-param name="field" select="$element/doc:field"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:variable name="isURL">
            <xsl:call-template name="isURL">
                <xsl:with-param name="field" select="$element/doc:field"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="$lc_identifier_type = 'ark'">
                <xsl:text>ARK</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_identifier_type = 'arxiv'">
                <xsl:text>arXiv</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_identifier_type = 'bibcode'">
                <xsl:text>bibcode</xsl:text>
            </xsl:when>
            <xsl:when test="$isDOI = 'true' or $lc_identifier_type = 'doi'">
                <xsl:text>DOI</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_identifier_type = 'ean13'">
                <xsl:text>EAN13</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_identifier_type = 'eissn'">
                <xsl:text>EISSN</xsl:text>
            </xsl:when>
            <xsl:when test="$isHandle = 'true' or $lc_identifier_type = 'handle'">
                <xsl:text>Handle</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_identifier_type = 'igsn'">
                <xsl:text>IGSN</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_identifier_type = 'isbn'">
                <xsl:text>ISBN</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_identifier_type = 'issn'">
                <xsl:text>ISSN</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_identifier_type = 'istc'">
                <xsl:text>ISTC</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_identifier_type = 'lissn'">
                <xsl:text>LISSN</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_identifier_type = 'lsid'">
                <xsl:text>LSID</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_identifier_type = 'pmid'">
                <xsl:text>PMID</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_identifier_type = 'purl'">
                <xsl:text>PURL</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_identifier_type = 'upc'">
                <xsl:text>UPC</xsl:text>
            </xsl:when>
            <xsl:when test="$isURL = 'true' or $lc_identifier_type = 'url'">
                <xsl:text>URL</xsl:text>
            </xsl:when>
            <xsl:otherwise>
                <xsl:text>URN</xsl:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>



   <!--  -->
   <!-- Auxiliary templates - get global values -->
   <!--  -->
   
    <!-- get the coar access rights globally -->
    <xsl:template name="getRightsURI">
        <xsl:call-template name="resolveRightsURI">
            <xsl:with-param name="field"
                select="//doc:element[@name='dc']/doc:element[@name='rights']/doc:element/doc:field[@name='value'and ends-with(translate(text(), $uppercase, $smallcase),'access')]/text()"/>
        </xsl:call-template>
    </xsl:template>

   <!-- get the issued date globally -->
    <xsl:template name="getIssuedDate">
        <xsl:value-of
            select="//doc:element[@name='date']/doc:element[@name='issued']/doc:element/doc:field[@name='value']/text()"/>
    </xsl:template>

   <!-- get the repository baseUrl globally -->
    <xsl:template name="getDSpaceURL">
        <xsl:value-of select="//doc:element[@name='repository']/doc:field[@name='url']/text()"/>
    </xsl:template>

   <!--  -->
   <!-- Auxiliary templates - dealing with Entities -->
   <!--  -->

    <!-- 
        this template will verify if a field name with "authority"
        is present and if it start with "virtual::"
        if it occurs, than we are in a presence of a related entity 
     -->
    <xsl:template name="isRelatedEntity">
        <xsl:param name="element"/>
        <xsl:variable name="sibling1" select="$element/following-sibling::*[1]"/>
        <!-- if next sibling is authority and starts with virtual:: -->
        <xsl:choose>
            <xsl:when test="$sibling1[@name='authority' and starts-with(text(),'virtual::')]">
                <xsl:value-of select="true()"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="false()"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- 
        this template will try to look for all "virtual::"
        and rebuild an "Entity" based on all virtual fields 
        in the source data.
        This will retrieve something like:
        <element name="virtual::226">
           <field name="dc.contributor.author.none">Doe, John</field>
           <field name="person.givenName.none">John</field>
           <field name="person.familyName.none">Doe</field>
           <field name="relation.isAuthorOfPublication.none">3f685bbd-07d9-403e-9de2-b8f0fabe27a7</field>
        </element>
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

   <!--  -->
   <!-- Other Auxiliary templates -->
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
        this template will verify if an element
        has any field that has the name starting "organization.legalName"
     -->
    <xsl:template name="hasOrganizationLegalNameField">
        <xsl:param name="element"/>
        <xsl:choose>
            <xsl:when test="$element/doc:field[starts-with(@name,'organization.legalName')]">
                <xsl:value-of select="true()"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="false()"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- it will verify if a given field is an handle -->
    <xsl:template name="isHandle">
        <xsl:param name="field"/>
        <xsl:choose>
            <xsl:when test="$field[contains(text(),'hdl.handle.net')]">
                <xsl:value-of select="true()"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="false()"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- it will verify if a given field is a DOI -->
    <xsl:template name="isDOI">
        <xsl:param name="field"/>
        <xsl:choose>
            <xsl:when test="$field[contains(text(),'doi.org') or starts-with(text(),'10.')]">
                <xsl:value-of select="true()"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="false()"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- it will verify if a given field is an ORCID -->
    <xsl:template name="isORCID">
        <xsl:param name="field"/>
        <xsl:choose>
            <xsl:when test="$field[contains(text(),'orcid.org')]">
                <xsl:value-of select="true()"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="false()"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- it will verify if a given field is an URL -->
    <xsl:template name="isURL">
        <xsl:param name="field"/>
        <xsl:variable name="lc_field">
            <xsl:call-template name="lowercase">
                <xsl:with-param name="value" select="$field"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="$lc_field[starts-with(text(),'http://') or starts-with(text(),'https://')]">
                <xsl:value-of select="true()"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="false()"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- will try to resolve the field type based on the value -->
    <xsl:template name="resolveFieldType">
        <xsl:param name="field"/>
        <!-- regexp not supported on XSLTv1 -->
        <xsl:variable name="isHandle">
            <xsl:call-template name="isHandle">
                <xsl:with-param name="field" select="$field"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:variable name="isDOI">
            <xsl:call-template name="isDOI">
                <xsl:with-param name="field" select="$field"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:variable name="isURL">
            <xsl:call-template name="isURL">
                <xsl:with-param name="field" select="$field"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="$isHandle = 'true'">
                <xsl:text>Handle</xsl:text>
            </xsl:when>
            <xsl:when test="$isDOI = 'true'">
                <xsl:text>DOI</xsl:text>
            </xsl:when>
            <xsl:when test="$isURL = 'true' and $isHandle = 'false' and $isDOI = 'false'">
                <xsl:text>URL</xsl:text>
            </xsl:when>
            <xsl:otherwise>
                <xsl:text>N/A</xsl:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!--
        This template will return the general type of the resource
        based on a valued text like 'article'
        https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/v4.0.0/field_publicationtype.html#attribute-resourcetypegeneral-m 
     -->
    <xsl:template name="resolveResourceTypeGeneral">
        <xsl:param name="field"/>
        <xsl:variable name="lc_dc_type">
            <xsl:call-template name="lowercase">
                <xsl:with-param name="value" select="$field"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="$lc_dc_type = 'article'">
                <xsl:text>literature</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'journal article'">
                <xsl:text>literature</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'book'">
                <xsl:text>literature</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'book part'">
                <xsl:text>literature</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'book review'">
                <xsl:text>literature</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'dataset'">
                <xsl:text>dataset</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'software'">
                <xsl:text>software</xsl:text>
            </xsl:when>
            <xsl:otherwise>
                <xsl:text>other research product</xsl:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!--
        This template will return the COAR Resource Type Vocabulary URI
        like http://purl.org/coar/resource_type/c_6501
        based on a valued text like 'article'
        https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/v4.0.0/field_publicationtype.html#attribute-uri-m
     -->
    <xsl:template name="resolveResourceTypeURI">
        <xsl:param name="field"/>
        <xsl:variable name="lc_dc_type">
            <xsl:call-template name="lowercase">
                <xsl:with-param name="value" select="$field"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="$lc_dc_type = 'annotation'">
                <xsl:text>http://purl.org/coar/resource_type/c_1162</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'journal'">
                <xsl:text>http://purl.org/coar/resource_type/c_0640</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'article'">
                <xsl:text>http://purl.org/coar/resource_type/c_6501</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'journal article'">
                <xsl:text>http://purl.org/coar/resource_type/c_6501</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'editorial'">
                <xsl:text>http://purl.org/coar/resource_type/c_b239</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'bachelor thesis'">
                <xsl:text>http://purl.org/coar/resource_type/c_7a1f</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'bibliography'">
                <xsl:text>http://purl.org/coar/resource_type/c_86bc</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'book'">
                <xsl:text>http://purl.org/coar/resource_type/c_2f33</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'book part'">
                <xsl:text>http://purl.org/coar/resource_type/c_3248</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'book review'">
                <xsl:text>http://purl.org/coar/resource_type/c_ba08</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'website'">
                <xsl:text>http://purl.org/coar/resource_type/c_7ad9</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'interactive resource'">
                <xsl:text>http://purl.org/coar/resource_type/c_e9a0</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'conference proceedings'">
                <xsl:text>http://purl.org/coar/resource_type/c_f744</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'conference object'">
                <xsl:text>http://purl.org/coar/resource_type/c_c94f</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'conference paper'">
                <xsl:text>http://purl.org/coar/resource_type/c_5794</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'conference poster'">
                <xsl:text>http://purl.org/coar/resource_type/c_6670</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'contribution to journal'">
                <xsl:text>http://purl.org/coar/resource_type/c_3e5a</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'data paper'">
                <xsl:text>http://purl.org/coar/resource_type/c_beb9</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'dataset'">
                <xsl:text>http://purl.org/coar/resource_type/c_ddb1</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'doctoral thesis'">
                <xsl:text>http://purl.org/coar/resource_type/c_db06</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'image'">
                <xsl:text>http://purl.org/coar/resource_type/c_c513</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'lecture'">
                <xsl:text>http://purl.org/coar/resource_type/c_8544</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'letter'">
                <xsl:text>http://purl.org/coar/resource_type/c_0857</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'master thesis'">
                <xsl:text>http://purl.org/coar/resource_type/c_bdcc</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'moving image'">
                <xsl:text>http://purl.org/coar/resource_type/c_8a7e</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'periodical'">
                <xsl:text>http://purl.org/coar/resource_type/c_2659</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'letter to the editor'">
                <xsl:text>http://purl.org/coar/resource_type/c_545b</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'patent'">
                <xsl:text>http://purl.org/coar/resource_type/c_15cd</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'preprint'">
                <xsl:text>http://purl.org/coar/resource_type/c_816b</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'report'">
                <xsl:text>http://purl.org/coar/resource_type/c_93fc</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'report part'">
                <xsl:text>http://purl.org/coar/resource_type/c_ba1f</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'research proposal'">
                <xsl:text>http://purl.org/coar/resource_type/c_baaf</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'review'">
                <xsl:text>http://purl.org/coar/resource_type/c_efa0</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'software'">
                <xsl:text>http://purl.org/coar/resource_type/c_5ce6</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'still image'">
                <xsl:text>http://purl.org/coar/resource_type/c_ecc8</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'technical documentation'">
                <xsl:text>http://purl.org/coar/resource_type/c_71bd</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'workflow'">
                <xsl:text>http://purl.org/coar/resource_type/c_393c</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'working paper'">
                <xsl:text>http://purl.org/coar/resource_type/c_8042</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'thesis'">
                <xsl:text>http://purl.org/coar/resource_type/c_46ec</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'cartographic material'">
                <xsl:text>http://purl.org/coar/resource_type/c_12cc</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'map'">
                <xsl:text>http://purl.org/coar/resource_type/c_12cd</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'video'">
                <xsl:text>http://purl.org/coar/resource_type/c_12ce</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'sound'">
                <xsl:text>http://purl.org/coar/resource_type/c_18cc</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'musical composition'">
                <xsl:text>http://purl.org/coar/resource_type/c_18cd</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'text'">
                <xsl:text>http://purl.org/coar/resource_type/c_18cf</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'conference paper not in proceedings'">
                <xsl:text>http://purl.org/coar/resource_type/c_18cp</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'conference poster not in proceedings'">
                <xsl:text>http://purl.org/coar/resource_type/c_18co</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'musical notation'">
                <xsl:text>http://purl.org/coar/resource_type/c_18cw</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'internal report'">
                <xsl:text>http://purl.org/coar/resource_type/c_18ww</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'memorandum'">
                <xsl:text>http://purl.org/coar/resource_type/c_18wz</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'other type of report'">
                <xsl:text>http://purl.org/coar/resource_type/c_18wq</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'policy report'">
                <xsl:text>http://purl.org/coar/resource_type/c_186u</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'project deliverable'">
                <xsl:text>http://purl.org/coar/resource_type/c_18op</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'report to funding agency'">
                <xsl:text>http://purl.org/coar/resource_type/c_18hj</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'research report'">
                <xsl:text>http://purl.org/coar/resource_type/c_18ws</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'technical report'">
                <xsl:text>http://purl.org/coar/resource_type/c_18gh</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'review article'">
                <xsl:text>http://purl.org/coar/resource_type/c_dcae04bc</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'research article'">
                <xsl:text>http://purl.org/coar/resource_type/c_2df8fbb1</xsl:text>
            </xsl:when>
            <!-- other -->
            <xsl:otherwise>
                <xsl:text>http://purl.org/coar/resource_type/c_1843</xsl:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!--
        This template will return the COAR Access Right Vocabulary URI
        like http://purl.org/coar/access_right/c_abf2
        based on a value text like 'open access'
        https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/v4.0.0/field_accessrights.html#definition-and-usage-instruction
     -->
    <xsl:template name="resolveRightsURI">
        <xsl:param name="field"/>
        <xsl:variable name="lc_value">
            <xsl:call-template name="lowercase">
                <xsl:with-param name="value" select="$field"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="$lc_value = 'open access'">
                <xsl:text>http://purl.org/coar/access_right/c_abf2</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_value = 'embargoed access'">
                <xsl:text>http://purl.org/coar/access_right/c_f1cf</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_value = 'restricted access'">
                <xsl:text>http://purl.org/coar/access_right/c_16ec</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_value = 'metadata only access'">
                <xsl:text>http://purl.org/coar/access_right/c_14cb</xsl:text>
            </xsl:when>
            <xsl:otherwise/>
        </xsl:choose>
    </xsl:template>

   <!-- xml:language -->
   <!-- this template will add a xml:lang parameter with the defined language of an element -->
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

    <!-- Prepare data for CC License -->
    <xsl:variable name="ccstart">
        <xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']/doc:element/doc:field[@name='value']/text()"/>
    </xsl:variable>
    
    <xsl:template
        match="doc:element[@name='others']/doc:element[@name='cc']"
        mode="oaire">
        <oaire:licenseCondition>
            <xsl:attribute name="startDate">
                <xsl:value-of
                    select="$ccstart"/>
            </xsl:attribute>
            <xsl:attribute name="uri">
                <xsl:value-of select="./doc:field[@name='uri']/text()" />
            </xsl:attribute>
            <xsl:value-of select="./doc:field[@name='name']/text()" />
        </oaire:licenseCondition>
    </xsl:template>

    <!-- ignore all non specified text values or attributes -->
    <xsl:template match="text()|@*"/>
    <xsl:template match="text()|@*" mode="oaire"/>
    <xsl:template match="text()|@*" mode="datacite"/>
    <xsl:template match="text()|@*" mode="entity_author"/>
    <xsl:template match="text()|@*" mode="entity_funding"/>

</xsl:stylesheet>
