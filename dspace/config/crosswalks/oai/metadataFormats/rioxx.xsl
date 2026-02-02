<?xml version="1.0" encoding="UTF-8" ?>
<!--


    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/
	Developed by DSpace @ Lyncode <dspace@lyncode.com>

 -->
<xsl:stylesheet
        xmlns:dc="http://purl.org/dc/elements/1.1/"
        xmlns:dcterms="http://purl.org/dc/terms/"
        xmlns:rioxxterms="http://docs.rioxx.net/schema/v3.0/rioxxterms/"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:doc="http://www.lyncode.com/xoai"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        version="1.0" xmlns:xls="http://www.w3.org/1999/XSL/Transform">
    <xsl:output omit-xml-declaration="yes" method="xml" indent="yes"/>
    <!-- First author global variable -->
    <xsl:variable name="firstAuthor">
        <xsl:call-template name="getFirstAuthor"/>
    </xsl:variable>
    <xsl:template match="/">
        <rioxx xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://www.rioxx.net/schema/v3.0/rioxx/ http://www.rioxx.net/schema/v3.0/rioxx/rioxx.xsd">

            <!-- RIOXX :: dc:coverage -->
            <!-- https://www.rioxx.net/profiles/v3-0-final/#dc:coverage -->
            <xsl:apply-templates
                    select="doc:metadata/doc:element[@name='dc']/doc:element[@name='coverage']" mode="dc"/>

            <!-- RIOXX :: dc:description -->
            <!-- https://www.rioxx.net/profiles/v3-0-final/#dc:description -->
            <xsl:apply-templates
                    select="doc:metadata/doc:element[@name='dc']/doc:element[@name='description']/doc:element[@name='abstract']"
                    mode="dc"/>

            <!-- RIOXX :: dc:identifier
               Persistent identifier for the resource. In repositories, this is typically a webpage which includes links to other related resources (splash page)
            -->
            <!-- https://www.rioxx.net/profiles/v3-0-final/#dc:identifier -->
            <xsl:apply-templates
                    select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='uri']"
                    mode="dc"/>

            <!-- RIOXX :: dc:language -->
            <!-- https://www.rioxx.net/profiles/v3-0-final/#dc:language -->
            <xsl:apply-templates
                    select="doc:metadata/doc:element[@name='dc']/doc:element[@name='language']/doc:element[@name='iso']"
                    mode="dc"/>

            <!-- RIOXX :: dc.relation -->
            <!-- https://www.rioxx.net/profiles/v3-0-final/#dc:relation -->
            <!-- Declare the local repository PID for the corresponding resource, either a local DOI or a handle -->
            <xsl:apply-templates
                    select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='uri']"
                    mode="local_pid"/>

            <!-- dc.relation: iterate through bundles (all files in bundle ORIGINAL) -->
            <xsl:apply-templates
                    select="doc:metadata/doc:element[@name='bundles']/doc:element[@name='bundle']" mode="dc"/>

            <!-- RIOXX :: dc:source -->
            <!-- https://www.rioxx.net/profiles/v3-0-final/#dc:source -->
            <xsl:variable name="isbn">
                <xsl:call-template name="getISBN"/>
            </xsl:variable>
            <xsl:variable name="issn">
                <xsl:call-template name="getISSN"/>
            </xsl:variable>
            <xsl:if test="not($isbn='') or not($issn='')">
                <dc:source>
                    <xsl:choose>
                        <xsl:when test="not($isbn='')">
                            <xsl:value-of select="$isbn"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="$issn"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </dc:source>
            </xsl:if>

            <!-- RIOXX :: dc:subject -->
            <!-- https://www.rioxx.net/profiles/v3-0-final/#dc:source -->
            <xsl:apply-templates
                    select="doc:metadata/doc:element[@name='dc']/doc:element[@name='subject']" mode="dc"/>

            <!-- RIOXX :: dc:title -->
            <!-- https://www.rioxx.net/profiles/v3-0-final/#dc:title -->
            <xsl:apply-templates
                    select="doc:metadata/doc:element[@name='dc']/doc:element[@name='title']" mode="dc"/>

            <!-- RIOXX :: dc:type -->
            <!-- https://www.rioxx.net/profiles/v3-0-final/#dc:type -->
            <xsl:apply-templates
                    select="doc:metadata/doc:element[@name='dc']/doc:element[@name='type']" mode="dc"/>

            <!-- RIOXX :: dcterms:dateAccepted -->
            <!-- https://www.rioxx.net/profiles/v3-0-final/#dcterms:dateAccepted -->
            <xsl:variable name="acceptedDate">
                <xsl:call-template name="getAcceptedDate"/>
            </xsl:variable>
            <xsl:if test="$acceptedDate!=''">
                <dcterms:dateAccepted>
                    <xls:value-of select="$acceptedDate"/>
                </dcterms:dateAccepted>
            </xsl:if>

            <!-- RIOXX :: rioxxterms:contributor -->
            <!-- https://www.rioxx.net/profiles/v3-0-final/#rioxxterms:contributor -->
            <xsl:apply-templates
                    select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name!='author']"
                    mode="rioxx"/>

            <!-- RIOXX :: rioxxterms:creator -->
            <!-- https://www.rioxx.net/profiles/v3-0-final/#rioxxterms:creator -->
            <xsl:apply-templates
                    select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']"
                    mode="rioxx"/>

            <!-- RIOXX :: rioxxterms:ext_relation -->
            <!-- https://www.rioxx.net/profiles/v3-0-final/#rioxxterms:ext_relation -->
            <!-- Include related published article if available -->
            <xsl:if test="doc:metadata/doc:element[@name='rioxxterms']/doc:element[@name='versionofrecord']/doc:element/doc:field[@name='value']">
                <rioxxterms:ext_relation
                        rel="cite-as"
                        coar_type="https://purl.org/coar/resource_type/c_6501"
                        coar_version="https://purl.org/coar/version/c_970fb48d4fbd8a85">
                    <xsl:value-of
                            select="doc:metadata/doc:element[@name='rioxxterms']/doc:element[@name='versionofrecord']/doc:element/doc:field[@name='value']"/>
                </rioxxterms:ext_relation>
            </xsl:if>

            <!-- RIOXX :: rioxxterms:grant -->
            <!-- https://www.rioxx.net/profiles/v3-0-final/#rioxxterms:grant -->
            <!-- AND -->
            <!-- RIOXX :: rioxxterms:project -->
            <!-- https://www.rioxx.net/profiles/v3-0-final/#rioxxterms:project -->
            <!-- Construct rioxxterms:project and rioxxterms:grant from project and funding entities -->
            <xsl:apply-templates
                    select="doc:metadata/doc:element[@name='dc']/doc:element[@name='relation']" mode="rioxx"/>

            <!-- RIOXX :: rioxxterms:publication_date -->
            <!-- https://www.rioxx.net/profiles/v3-0-final/#rioxxterms:publication_date -->
            <rioxxterms:publication_date>
                <xsl:choose>
                    <xsl:when
                            test="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']/doc:element/doc:field[@name='value']">
                        <xls:value-of
                                select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']/doc:element/doc:field[@name='value']"/>
                    </xsl:when>
                    <xsl:when
                            test="doc:metadata/doc:element[@name='dcterms']/doc:element[@name='issuedOnline']/doc:element/doc:field[@name='value']">
                        <xls:value-of
                                select="doc:metadata/doc:element[@name='dcterms']/doc:element[@name='issuedOnline']/doc:element/doc:field[@name='value']"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <!-- Accessioned date in form 2023-07-26T14:53:19Z; we need YYYY-MM-dd -->
                        <xsl:call-template name="getDateAccessioned"></xsl:call-template>
                    </xsl:otherwise>
                </xsl:choose>
            </rioxxterms:publication_date>

            <!-- RIOXX :: rioxxterms:publisher -->
            <!-- https://www.rioxx.net/profiles/v3-0-final/#rioxxterms:publisher -->
            <xsl:for-each
                    select="doc:metadata/doc:element[@name='dc']/doc:element[@name='publisher']/doc:element/doc:field[@name='value']">
                <dc:publisher>
                    <rioxxterms:name>
                        <xls:value-of select="."/>
                    </rioxxterms:name>
                </dc:publisher>
            </xsl:for-each>

            <!-- RIOXX :: rioxxterms:record_public_release_date -->
            <!-- https://www.rioxx.net/profiles/v3-0-final/#rioxxterms:record_public_release_date -->
            <rioxxterms:record_public_release_date>
                <xsl:call-template name="getDateAccessioned"></xsl:call-template>
            </rioxxterms:record_public_release_date>
        </rioxx>
    </xsl:template>

    <!-- XML Element templates -->
    <!-- dc.coverage -->
    <!--  https://www.rioxx.net/profiles/v3-0-final/#dc:coverage -->
    <xsl:template match="doc:element[@name='dc']/doc:element[@name='coverage']" mode="dc">
        <!-- dc.coverage -->
        <xsl:for-each select="./doc:element/doc:field[@name='value']">
            <dc:coverage>
                <xsl:value-of select="."/>
            </dc:coverage>
        </xsl:for-each>
    </xsl:template>

    <!-- dc.description -->
    <!--  https://www.rioxx.net/profiles/v3-0-final/#dc:coverage -->
    <xsl:template match="doc:element[@name='dc']/doc:element[@name='description']/doc:element[@name='abstract']"
                  mode="dc">
        <!-- dc.description -->
        <xsl:for-each select="./doc:element/doc:field[@name='value']">
            <dc:description>
                <xsl:value-of select="."/>
            </dc:description>
        </xsl:for-each>
    </xsl:template>

    <!-- dc.identifier -->
    <!--  https://www.rioxx.net/profiles/v3-0-final/#dc:identifier -->
    <xsl:template match="doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='uri']"
                  mode="dc">
        <xsl:variable name="uris">
            <xsl:for-each select="./doc:element/doc:field[@name='value']">
                <xsl:variable name="isDOI">
                    <xsl:call-template name="isDOI">
                        <xsl:with-param name="field">
                            <xsl:value-of select="text()"/>
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:variable>
                <xsl:variable name="isHandle">
                    <xsl:call-template name="isHandle">
                        <xsl:with-param name="field">
                            <xsl:value-of select="text()"/>
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:variable>
                <!-- URI that is not a DOI or a Handle -->
                <xsl:if test="$isDOI='false' and $isHandle='false'">
                    <dc:identifier>
                        <xsl:value-of select="."/>
                    </dc:identifier>
                </xsl:if>
            </xsl:for-each>
        </xsl:variable>
        <!-- Retrieve only first URI value that is not a DOI or a Handle -->
        <xsl:copy-of select="$uris[position()=1]"/>

    </xsl:template>
    <!-- dc.relation type cite-as for local pids -->
    <xsl:template match="doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='uri']"
                  mode="local_pid">
        <xsl:variable name="uris">
            <xsl:for-each select="./doc:element/doc:field[@name='value']">
                <xsl:variable name="isDOI">
                    <xsl:call-template name="isDOI">
                        <xsl:with-param name="field">
                            <xsl:value-of select="text()"/>
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:variable>
                <xsl:variable name="isHandle">
                    <xsl:call-template name="isHandle">
                        <xsl:with-param name="field">
                            <xsl:value-of select="text()"/>
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:variable>
                <!-- URI that is either a DOI or a Handle -->
                <xsl:if test="$isDOI='true' or $isHandle='true'">
                    <dc:relation>
                        <xsl:attribute name="rel">
                            <xsl:value-of select="'cite-as'"/>
                        </xsl:attribute>
                        <xsl:value-of select="."/>
                    </dc:relation>
                </xsl:if>
            </xsl:for-each>
        </xsl:variable>
        <!-- Retrieve only first URI value that is either a DOI or a Handle -->
        <xsl:copy-of select="$uris[position()=1]"/>
    </xsl:template>

    <!-- dc.title -->
    <!--  https://www.rioxx.net/profiles/v3-0-final/#dc:title -->
    <xsl:template match="doc:element[@name='dc']/doc:element[@name='title'][position() = 1]" mode="dc">
        <dc:title>
            <xsl:value-of select="./doc:element/doc:field[@name='value']"/>
        </dc:title>
    </xsl:template>

    <!-- dc.language -->
    <!--  https://www.rioxx.net/profiles/v3-0-final/#dc:language -->
    <xsl:template match="doc:element[@name='dc']/doc:element[@name='language']/doc:element[@name='iso']" mode="dc">
        <!-- dc.language -->
        <xsl:for-each select="./doc:element/doc:field[@name='value']">
            <dc:language>
                <xsl:value-of select="."/>
            </dc:language>
        </xsl:for-each>
    </xsl:template>

    <!-- dc.subject -->
    <!--  https://www.rioxx.net/profiles/v3-0-final/#dc:subject -->
    <xsl:template match="doc:element[@name='dc']/doc:element[@name='subject']" mode="dc">
        <!-- dc.subject -->
        <xsl:for-each select="./doc:element/doc:field[@name='value']">
            <dc:subject>
                <xsl:value-of select="."/>
            </dc:subject>
        </xsl:for-each>
    </xsl:template>

    <!-- dc.type -->
    <!--  https://www.rioxx.net/profiles/v3-0-final/#dc:type -->
    <xsl:template match="doc:element[@name='dc']/doc:element[@name='type']" mode="dc">
        <!-- dc.type -->
        <dc:type>
            <xsl:call-template name="resolveResourceTypeURI">
                <xsl:with-param name="field">
                    <xsl:value-of select="./doc:element/doc:field[@name='value']/text()"/>
                </xsl:with-param>
            </xsl:call-template>
        </dc:type>
    </xsl:template>

    <!-- dc.relation -->
    <!--  https://www.rioxx.net/profiles/v3-0-final/#dc:relation -->
    <xsl:template match="doc:element[@name='bundles']/doc:element[@name='bundle']" mode="dc">
        <xsl:if test="doc:field[@name='name' and text()='ORIGINAL']">
            <xsl:for-each select="doc:element[@name='bitstreams']/doc:element[@name='bitstream']">
                <xsl:apply-templates select="." mode="dc"/>
            </xsl:for-each>
        </xsl:if>
    </xsl:template>

    <!-- dc.relation -->
    <xsl:template match="doc:element[@name='bitstreams']/doc:element[@name='bitstream']" mode="dc">
        <dc:relation>
            <xsl:attribute name="rel">
                <xsl:value-of select="'item'"/>
            </xsl:attribute>
            <xsl:attribute name="type">
                <xsl:value-of select="doc:field[@name='format']"/>
            </xsl:attribute>
            <xsl:attribute name="coar_type">
                <xsl:variable name="dcTypeMetadata">
                    <xsl:call-template name="getTypeMetadata"/>
                </xsl:variable>
                <xsl:call-template name="resolveResourceTypeURI">
                    <xsl:with-param name="field" select="$dcTypeMetadata"/>
                </xsl:call-template>
            </xsl:attribute>
            <!-- coar_version
            When used, the coar_version attribute MUST contain a value which is an identifier
            from the COAR Version Types Vocabulary: http://purl.org/coar/version/.
            -->
            <xsl:attribute name="coar_version">
                <xsl:call-template name="resolveResourceVersionURI">
                    <xsl:with-param name="version">
                        <xsl:call-template name="getRioxxVersion"/>
                    </xsl:with-param>
                    <xsl:with-param name="description">
                        <xsl:value-of select="doc:field[@name='description']"/>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:attribute>
            <!-- access_rights -->
            <xsl:variable name="bitstreamAccessStatusText">
                <xsl:call-template name="getBitstreamAccessStatus">
                    <xsl:with-param name="bitstream" select="."/>
                </xsl:call-template>
            </xsl:variable>
            <xsl:attribute name="access_rights">
                <!-- get the coar access rights at the individual file level -->
                <!-- Look at resource policies to infer access level information, defaults to item-level status -->
                <xsl:choose>
                    <xsl:when test="$bitstreamAccessStatusText='embargo' or $bitstreamAccessStatusText='open.access' or $bitstreamAccessStatusText='restricted'">
                        <xsl:call-template name="resolveRightsURI">
                            <xsl:with-param name="field"
                                            select="$bitstreamAccessStatusText"/>
                        </xsl:call-template>
                    </xsl:when>
                    <xsl:otherwise>
                        <!-- Default to item-level access status information -->
                        <xsl:call-template name="getRightsURI"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            <!-- Add deposit date -->
            <xsl:variable name="depositDate">
                <xsl:call-template name="getDepositDate"/>
            </xsl:variable>
            <xsl:choose>
                <xsl:when test="$depositDate!=''">
                    <xsl:attribute name="deposit_date">
                        <xsl:value-of select="$depositDate"/>
                    </xsl:attribute>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:attribute name="deposit_date">
                        <xsl:call-template name="getDateAccessioned"/>
                    </xsl:attribute>
                </xsl:otherwise>
            </xsl:choose>
            <!-- Add embargo information: resource_exposed_date -->
            <!-- Only set resource_exposed_Date if access status != 'restricted' -->
            <xsl:if test="$bitstreamAccessStatusText!='restricted'">
                <!-- First policy found with action=READ; group=Anonymous and start-date attribute is not null -->
                <xsl:variable name="startDate">
                    <xsl:value-of
                            select="doc:element[@name='resourcePolicies']/doc:element[doc:field[@name='action']/text()='READ' and doc:field[@name='group']/text()='Anonymous' and doc:field[@name='start-date']/text()][position()=1]/doc:field[@name='start-date']/text()"/>
                </xsl:variable>
                <xsl:variable name="dateAvailable">
                    <xsl:call-template name="getDateAvailable"/>
                </xsl:variable>
                <xsl:choose>
                    <xsl:when test="$startDate!=''">
                        <xsl:attribute name="resource_exposed_date">
                            <xsl:value-of select="$startDate"/>
                        </xsl:attribute>
                    </xsl:when>
                    <xsl:when test="$dateAvailable!=''">
                        <xsl:attribute name="resource_exposed_date">
                            <xsl:value-of select="$dateAvailable"/>
                        </xsl:attribute>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:attribute name="resource_exposed_date">
                            <xsl:call-template name="getDateAccessioned"/>
                        </xsl:attribute>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:if>
            <xsl:value-of select="doc:field[@name='url']/text()"/>
        </dc:relation>
    </xsl:template>

    <!-- rioxxterms:ext_relation -->
    <!--  https://www.rioxx.net/profiles/v3-0-final/#rioxxterms:ext_relation -->
    <!-- Related dataset(s) -->
    <!-- Related published article(s) -->
    <!-- TODO Add crosswalk to related datasets and articles -->

    <!-- rioxxterms:creator -->
    <!-- https://www.rioxx.net/profiles/v3-0-final/#rioxxterms:creator -->
    <xsl:template
            match="doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']" mode="rioxx">
        <!-- rioxxterms:creator -->
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
                    <rioxxterms:creator>
                        <xsl:if test="$firstAuthor=.">
                            <xsl:attribute name="first-named-author">
                                <xsl:text>true</xsl:text>
                            </xsl:attribute>
                        </xsl:if>
                        <rioxxterms:name>
                            <xsl:value-of select="./text()"/>
                        </rioxxterms:name>
                    </rioxxterms:creator>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:for-each>
    </xsl:template>

    <!-- rioxxterms:creator -->
    <xsl:template match="doc:element" mode="entity_creator">
        <xsl:variable name="isOrgUnitEntity">
            <xsl:call-template name="hasOrganizationLegalNameField">
                <xsl:with-param name="element" select="."/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:variable name="author_name">
            <xsl:choose>
                <xsl:when test="$isOrgUnitEntity = 'true'">
                    <xsl:value-of select="doc:field[starts-with(@name,'organization.legalName')]"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="doc:field[starts-with(@name,'dc.contributor.author')]"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <rioxxterms:creator>
            <xsl:if test="$firstAuthor=$author_name">
                <xsl:attribute name="first-named-author">
                    <xsl:text>true</xsl:text>
                </xsl:attribute>
            </xsl:if>
            <rioxxterms:name>
                <xsl:value-of select="$author_name"/>
            </rioxxterms:name>
            <xsl:apply-templates select="doc:field" mode="entity_author"/>
        </rioxxterms:creator>
    </xsl:template>

    <!-- rioxxterms:contributor -->
    <!-- https://www.rioxx.net/profiles/v3-0-final/#rioxxterms:contributor -->
    <xsl:template
            match="doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name!='author']" mode="rioxx">
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
                    <rioxxterms:contributor>
                        <rioxxterms:name>
                            <xsl:value-of select="./text()"/>
                        </rioxxterms:name>
                    </rioxxterms:contributor>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:for-each>
    </xsl:template>

    <!-- rioxxterms:contributor -->
    <xsl:template match="doc:element" mode="entity_contributor">
        <xsl:variable name="isOrgUnitEntity">
            <xsl:call-template name="hasOrganizationLegalNameField">
                <xsl:with-param name="element" select="."/>
            </xsl:call-template>
        </xsl:variable>
        <rioxxterms:contributor>
            <rioxxterms:name>
                <xsl:choose>
                    <xsl:when test="$isOrgUnitEntity = 'true'">
                        <xsl:value-of select="doc:field[starts-with(@name,'organization.legalName')]"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="doc:field[starts-with(@name,'dc.contributor')]"/>
                    </xsl:otherwise>
                </xsl:choose>
            </rioxxterms:name>
            <xsl:apply-templates select="doc:field" mode="entity_author"/>
        </rioxxterms:contributor>
    </xsl:template>

    <!-- This template creates the sub-element <rioxxterms:id> from a Person built entity -->
    <xsl:template match="doc:field[starts-with(@name,'person.identifier.*')]" mode="entity_author">
        <!-- TODO resolve the scheme based on the value -->
        <rioxxterms:id>
            <xsl:value-of select="./text()"/>
        </rioxxterms:id>
    </xsl:template>

    <!-- This template creates the sub-element <rioxxterms:id> of type ORCID from a Person built entity -->
    <xsl:template match="doc:field[starts-with(@name,'person.identifier.orcid')]" mode="entity_author">
        <rioxxterms:id>
            <xsl:choose>
                <!-- Prepend ORCID URI if field only contains the ORCID ID number -->
                <xsl:when test="not(starts-with(./text(), 'http'))">
                    <xsl:value-of select="concat('https://orcid.org/', ./text())"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="./text()"/>
                </xsl:otherwise>
            </xsl:choose>
        </rioxxterms:id>
    </xsl:template>

    <!-- This template creates the sub-element <rioxxterms:id> of type Scopus Author ID from a Person built entity -->
    <xsl:template match="doc:field[starts-with(@name,'person.identifier.scopus-author-id')]"
                  mode="entity_author">
        <rioxxterms:id>
            <xsl:value-of select="./text()"/>
        </rioxxterms:id>
    </xsl:template>

    <!-- This template creates the sub-element <rioxxterms:id> of type Ciencia ID from a Person built entity -->
    <xsl:template match="doc:field[starts-with(@name,'person.identifier.ciencia-id')]" mode="entity_author">
        <rioxxterms:id>
            <xsl:value-of select="./text()"/>
        </rioxxterms:id>
    </xsl:template>

    <!-- This template creates the sub-element <rioxxterms:id> of type Researcher ID from a Person built entity -->
    <xsl:template match="doc:field[starts-with(@name,'person.identifier.rid')]" mode="entity_author">
        <rioxxterms:id>
            <xsl:value-of select="./text()"/>
        </rioxxterms:id>
    </xsl:template>

    <!-- This template creates the sub-element <rioxxterms:id> of type Google Scholar ID from a Person built entity -->
    <xsl:template match="doc:field[starts-with(@name,'person.identifier.gsid')]" mode="entity_author">
        <rioxxterms:id>
            <xsl:value-of select="./text()"/>
        </rioxxterms:id>
    </xsl:template>

    <!-- This template creates the sub-element <rioxxterms:id> of type ISNI from a Person built entity -->
    <xsl:template match="doc:field[starts-with(@name,'person.identifier.isni')]" mode="entity_author">
        <rioxxterms:id>
            <xsl:value-of select="./text()"/>
        </rioxxterms:id>
    </xsl:template>

    <!-- This template creates the sub-element <rioxxterms:id> for an organization from a built entity -->
    <xsl:template match="doc:field[starts-with(@name,'organization.identifier.*')]" mode="entity_author">
        <!-- TODO resolve the scheme based on the value -->
        <rioxxterms:id>
            <xsl:value-of select="./text()"/>
        </rioxxterms:id>
    </xsl:template>

    <!-- This template creates the sub-element <rioxxterms:id> of type ISNI for an organization from a built entity -->
    <xsl:template match="doc:field[starts-with(@name,'organization.identifier.isni')]" mode="entity_author">
        <rioxxterms:id>
            <xsl:value-of select="./text()"/>
        </rioxxterms:id>
    </xsl:template>

    <!-- rioxxterms:grant -->
    <!--  https://www.rioxx.net/profiles/v3-0-final/#rioxxterms:grant -->
    <xsl:template match="doc:element[@name='dc']/doc:element[@name='relation']" mode="rioxx">
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
                <xsl:apply-templates select="$entity" mode="entity_project"/>
                <xsl:apply-templates select="$entity" mode="entity_funding"/>
            </xsl:if>
        </xsl:for-each>
    </xsl:template>

    <!-- rioxxterms:project -->
    <xsl:template match="doc:element" mode="entity_project">
        <rioxxterms:project>
            <xsl:apply-templates select="doc:field" mode="entity_project"/>
        </rioxxterms:project>
    </xsl:template>

    <!-- This template creates the property awardURI from a Funded Project built entity -->
    <xsl:template match="doc:field[starts-with(@name,'oaire.awardURI')]" mode="entity_project">
        <xsl:value-of select="./text()"/>
    </xsl:template>

    <!-- rioxxterms:grant -->
    <xsl:template match="doc:element" mode="entity_funding">
        <rioxxterms:grant>
            <xsl:apply-templates select="doc:field" mode="entity_funding"/>
        </rioxxterms:grant>
    </xsl:template>

    <!-- This template creates the attribute funderName from a Funded Project built entity -->
    <xsl:template match="doc:field[starts-with(@name,'project.funder.name')]" mode="entity_funding">
        <xsl:attribute name="funderName">
            <xsl:value-of select="./text()"/>
        </xsl:attribute>
    </xsl:template>

    <!-- This template creates the sub-element funder_id from a Funded Project built entity -->
    <xsl:template match="doc:field[starts-with(@name,'project.funder.identifier')]" mode="entity_funding">
        <!-- TODO: recognize the type ISNI / GRID / Crossref Funder -->
        <xsl:attribute name="funder_id">
            <xsl:value-of select="./text()"/>
        </xsl:attribute>
    </xsl:template>

    <!-- This template creates the sub-element <oaire:awardNumber> from a Funded Project built entity -->
    <xsl:template match="doc:field[starts-with(@name,'oaire.awardNumber')]" mode="entity_funding">
        <xsl:value-of select="./text()"/>
    </xsl:template>

    <!--  -->
    <!-- Auxiliary templates - get global values -->
    <!--  -->

    <!-- get first author globally -->
    <xsl:template name="getFirstAuthor">
        <xsl:value-of
                select="//doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']/doc:element/doc:field[@name='value'][position()=1]/text()"/>
    </xsl:template>
    <!-- get first deposit date globally -->
    <xsl:template name="getDepositDate">
        <xsl:value-of
                select="//doc:element[@name='cam']/doc:element[@name='depositDate']/doc:element/doc:field[@name='value'][position()=1]/text()"/>
    </xsl:template>

    <!-- get first accessioned date globally -->
    <xsl:template name="getDateAccessioned">
        <xsl:value-of
                select="//doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='accessioned']/doc:element/doc:field[@name='value'][position()=1]/text()"/>
    </xsl:template>

    <!-- get first date available globally -->
    <xsl:template name="getDateAvailable">
        <xsl:value-of
                select="//doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='available']/doc:element/doc:field[@name='value'][position()=1]/text()"/>
    </xsl:template>

    <!-- get first accepted date globally -->
    <xsl:template name="getAcceptedDate">
        <xsl:value-of
                select="//doc:element[@name='dcterms']/doc:element[@name='dateAccepted']/doc:element/doc:field[@name='value'][position()=1]/text()"/>
    </xsl:template>

    <!-- get first dc type globally -->
    <xsl:template name="getTypeMetadata">
        <xsl:value-of
                select="//doc:element[@name='dc']/doc:element[@name='type']/doc:element/doc:field[@name='value'][position()=1]/text()"/>
    </xsl:template>

    <!-- get first ISBN globally -->
    <xsl:template name="getISBN">
        <xsl:value-of
                select="//doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='isbn']/doc:element/doc:field[@name='value'][position()=1]/text()"/>
    </xsl:template>

    <!-- get first ISSN globally -->
    <xsl:template name="getISSN">
        <xsl:value-of
                select="//doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='issn']/doc:element/doc:field[@name='value'][position()=1]/text()"/>
    </xsl:template>

    <!-- get first rioxxterms:version globally -->
    <xsl:template name="getRioxxVersion">
        <xsl:value-of
                select="//doc:element[@name='rioxxterms']/doc:element[@name='version']/doc:element/doc:field[@name='value'][position()=1]/text()"/>
    </xsl:template>

    <!-- get the coar access rights globally from access status mechanism -->
    <xsl:template name="getRightsURI">
        <xsl:call-template name="resolveRightsURI">
            <xsl:with-param name="field"
                            select="/doc:metadata/doc:element[@name='others']/doc:element[@name='access-status']/doc:field[@name='value']/text()"/>
        </xsl:call-template>
    </xsl:template>

    <!-- get the coar access rights globally from access status mechanism -->
    <xsl:template name="getRightsText">
        <xsl:value-of select="/doc:metadata/doc:element[@name='others']/doc:element[@name='access-status']/doc:field[@name='value']/text()"/>
    </xsl:template>

    <!--
       This template will return the COAR Version Type Vocabulary URI (https://vocabularies.coar-repositories.org/version_types/)
       like http://purl.org/coar/version/c_71e4c1898caa6e32
       based on a valued text like 'smur'
    -->
    <xsl:template name="resolveResourceVersionURI">
        <xsl:param name="description"/>
        <xsl:param name="version"/>
        <xsl:variable name="lc_description">
            <xsl:call-template name="lowercase">
                <xsl:with-param name="value" select="$description"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:variable name="lc_version">
            <xsl:call-template name="lowercase">
                <xsl:with-param name="value" select="$version"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="$lc_description = 'accepted version' or $lc_version='am'">
                <xsl:text>http://purl.org/coar/version/c_ab4af688f83e57aa</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_description = 'published version' or $lc_version='vor'">
                <xsl:text>http://purl.org/coar/version/c_970fb48d4fbd8a85</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_description = 'submitted version' or $lc_version='ao'">
                <xsl:text>http://purl.org/coar/version/c_b1a7d7d4d402bcce</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_version='cvor'">
                <xsl:text>http://purl.org/coar/version/c_e19f295774971610</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_version='p'">
                <xsl:text>http://purl.org/coar/version/c_fa2ee174bc00049f</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_version='evor'">
                <xsl:text>http://purl.org/coar/version/c_dc82b40f9837b551</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_version='smur'">
                <xsl:text>http://purl.org/coar/version/c_71e4c1898caa6e32</xsl:text>
            </xsl:when>
            <xsl:otherwise>
                <xsl:text>http://purl.org/coar/version/c_be7fb7dd8ff6fe43</xsl:text>
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

    <!-- This template will return the access.status at the bitstream level -->
    <xsl:template name="getBitstreamAccessStatus">
        <xsl:param name="bitstream"/>
        <!-- get the coar access rights at the individual file level -->
        <!-- Look at resource policies to infer access level information, defaults to item-level status -->
        <xsl:choose>
            <xsl:when test="$bitstream/doc:element[@name='resourcePolicies']/doc:element[doc:field[@name='action']/text()='READ' and doc:field[@name='group']/text()='Anonymous' and doc:field[@name='start-date']]">
                <xsl:value-of select="'embargo'"/>
            </xsl:when>
            <xsl:when test="doc:element[@name='resourcePolicies']/doc:element[doc:field[@name='action']/text()='READ' and doc:field[@name='group']/text()='Anonymous' and not(doc:field[@name='start-date'])]">
                <xsl:value-of select="'open.access'"/>
            </xsl:when>
            <xsl:when test="$bitstream/doc:element[@name='resourcePolicies']/doc:element[doc:field[@name='action']/text()='READ' and doc:field[@name='group']/text()='Administrator']">
                <xsl:value-of select="'restricted'"/>
            </xsl:when>
            <xsl:otherwise>
                <!-- Default to item-level access status information - in text form -->
                <xsl:call-template name="getRightsText"/>
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
            <xsl:when test="$lc_value = 'open access' or $lc_value = 'open.access'">
                <xsl:text>http://purl.org/coar/access_right/c_abf2</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_value = 'embargoed access' or $lc_value = 'embargo'">
                <xsl:text>http://purl.org/coar/access_right/c_f1cf</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_value = 'restricted access' or $lc_value = 'restricted'">
                <xsl:text>http://purl.org/coar/access_right/c_16ec</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_value = 'metadata only access' or $lc_value = 'metadata.only'">
                <xsl:text>http://purl.org/coar/access_right/c_14cb</xsl:text>
            </xsl:when>
            <xsl:otherwise/>
        </xsl:choose>
    </xsl:template>

    <!-- get first issued date globally -->
    <xsl:template name="getIssuedDate">
        <xsl:value-of
                select="//doc:element[@name='date']/doc:element[@name='issued']/doc:element/doc:field[@name='value' and position()=1]/text()"/>
    </xsl:template>

    <!-- Utils and auxiliary templates -->
    <xsl:template name="substring-before-last">
        <xsl:param name="string1" select="''"/>
        <xsl:param name="string2" select="''"/>
        <xsl:if test="$string1 != '' and $string2 != ''">
            <xsl:variable name="head" select="substring-before($string1, $string2)"/>
            <xsl:variable name="tail" select="substring-after($string1, $string2)"/>
            <xsl:value-of select="$head"/>
            <xsl:if test="contains($tail, $string2)">
                <xsl:value-of select="$string2"/>
                <xsl:call-template name="substring-before-last">
                    <xsl:with-param name="string1" select="$tail"/>
                    <xsl:with-param name="string2" select="$string2"/>
                </xsl:call-template>
            </xsl:if>
        </xsl:if>
    </xsl:template>

    <!--  -->
    <!-- Auxiliary templates - dealing with Entities -->
    <!--  -->

    <!--
        this template will verify if a field name with "authority"
        is present and if it starts with "virtual::"
        if it occurs, then we are in a presence of a related entity
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

    <!-- it will verify if a given field is a handle -->
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

    <!-- ignore all non specified text values or attributes -->
    <xsl:template match="text()|@*"/>
    <xsl:template match="text()|@*" mode="rioxx"/>
    <xsl:template match="text()|@*" mode="entity_author"/>
    <xsl:template match="text()|@*" mode="entity_project"/>
    <xsl:template match="text()|@*" mode="entity_funding"/>

</xsl:stylesheet>
