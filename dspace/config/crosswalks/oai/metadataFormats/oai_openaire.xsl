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
        <oaire:resource
            xmlns:vc="http://www.w3.org/2007/XMLSchema-versioning" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://namespace.openaire.eu/schema/oaire/ https://www.openaire.eu/schema/repo-lit/4.0/openaire.xsd">

            <xsl:apply-templates
                select="doc:metadata/doc:element[@name='dc']/doc:element[@name='title']" mode="datacite"/>
            <xsl:apply-templates
                select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']"
                mode="datacite"/>
            <datacite:contributors>
                <xsl:apply-templates
                    select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name!='author']"
                    mode="datacite"/>
                <xsl:apply-templates select="//doc:metadata/doc:element[@name='repository']"
                    mode="contributor"/>
            </datacite:contributors>
            <xsl:apply-templates
                select="doc:metadata/doc:element[@name='dc']/doc:element[@name='relation']" mode="oaire"/>
            <!-- datacite:dates and embargo -->
            <xsl:apply-templates
                select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']" mode="datacite"/>

        <!--dcterms:audience>Researchers</dcterms:audience-->
        </oaire:resource>
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


    <!-- datacite.creators -->
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

    <!-- The repository is considered a contributor of this work
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

    <xsl:template match="doc:field[starts-with(@name,'person.givenName')]" mode="entity_author">
        <datacite:givenName>
            <xsl:value-of select="./text()"/>
        </datacite:givenName>
    </xsl:template>

    <xsl:template match="doc:field[starts-with(@name,'person.familyName')]" mode="entity_author">
        <datacite:familyName>
            <xsl:value-of select="./text()"/>
        </datacite:familyName>
    </xsl:template>

    <xsl:template match="doc:field[starts-with(@name,'person.affiliation.name')]" mode="entity_author">
        <datacite:affiliation>
            <xsl:value-of select="./text()"/>
        </datacite:affiliation>
    </xsl:template>

    <xsl:template match="doc:field[starts-with(@name,'person.identifier.orcid')]" mode="entity_author">
        <datacite:nameIdentifier nameIdentifierScheme="ORCID" schemeURI="http://orcid.org">
            <xsl:value-of select="./text()"/>
        </datacite:nameIdentifier>
    </xsl:template>

    <!-- oaire:fundingReferences -->
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

    <xsl:template match="doc:field[starts-with(@name,'dc.relation')]" mode="entity_funding">
        <oaire:awardTitle>
            <xsl:value-of select="./text()"/>
        </oaire:awardTitle>
    </xsl:template>

    <xsl:template match="doc:field[starts-with(@name,'project.funder.name')]" mode="entity_funding">
        <oaire:funderName>
            <xsl:value-of select="./text()"/>
        </oaire:funderName>
    </xsl:template>

    <xsl:template match="doc:field[starts-with(@name,'project.funder.identifier')]" mode="entity_funding">
        <!-- TODO: recognize the type ISNI / GRID / Crossref Funder -->
        <oaire:funderIdentifier funderIdentifierType="Crossref Funder ID">
            <xsl:value-of select="./text()"/>
        </oaire:funderIdentifier>
    </xsl:template>

    <xsl:template match="doc:field[starts-with(@name,'oaire.fundingStream')]" mode="entity_funding">
        <oaire:fundingStream>
            <xsl:value-of select="./text()"/>
        </oaire:fundingStream>
    </xsl:template>

    <xsl:template match="doc:field[starts-with(@name,'oaire.awardNumber')]" mode="entity_funding">
        <!-- TODO: get the awarduri from doc:field@name='oaire.awardURI -->
        <oaire:awardNumber>
            <xsl:attribute name="awardURI">
                <xsl:apply-templates select="../doc:field[starts-with(@name,'oaire.awardURI')]" mode="entity_funding_param"/>
            </xsl:attribute>        
            <xsl:value-of select="./text()"/>
        </oaire:awardNumber>
    </xsl:template>

    <xsl:template match="doc:field[starts-with(@name,'oaire.awardURI')]" mode="entity_funding_param">
        <xsl:value-of select="./text()"/>
    </xsl:template>

    <!-- datacite.dates -->
    <xsl:template match="doc:element[@name='dc']/doc:element[@name='date']" mode="datacite">
        <datacite:dates>
        <!-- datacite:date (embargo) -->
            <xsl:for-each select="./doc:element">
                <xsl:apply-templates select="." mode="datacite"/>
            </xsl:for-each>
        </datacite:dates>
    </xsl:template>

    <!-- datacite.date @name=issued or @name=accepted -->
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
                <xsl:attribute name="dateType" select="$dateType"/>
                <xsl:value-of select="./doc:element/doc:field[@name='value']/text()"/>
            </datacite:date>
        </xsl:if>
    </xsl:template>

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
        this template will verify if a field name with "authority"
        is present and if it start with "virtual::"
        if it occurs, than we are in a presence of an related entity 
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
        <xsl:value-of
            select="//doc:element[@name='date']/doc:element[@name='issued']/doc:element/doc:field[@name='value']/text()"/>
    </xsl:template>

   <!-- get the issued date -->
    <xsl:template name="getDSpaceURL">
        <xsl:value-of select="//doc:element[@name='others']/doc:field[@name='url']/text()"/>
    </xsl:template>

    <!-- ignore all non specified text values or attributes -->
    <xsl:template match="text()|@*"/>
    <xsl:template match="text()|@*" mode="entity_author"/>
    <xsl:template match="text()|@*" mode="entity_funding"/>

</xsl:stylesheet>