<?xml version="1.0" encoding="UTF-8" ?>
<!--


    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/
    Developed by DSpace @ Lyncode <dspace@lyncode.com>

 -->
<xsl:stylesheet
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:doc="http://www.lyncode.com/xoai"
        version="1.0">
    <xsl:output omit-xml-declaration="yes" method="xml" indent="yes" />

    <xsl:template match="/">
        <uketd_dc:uketddc
                xmlns:uketd_dc="http://naca.central.cranfield.ac.uk/ethos-oai/2.0/"
                xmlns:dc="http://purl.org/dc/elements/1.1/"
                xmlns:dcterms="http://purl.org/dc/terms/"
                xmlns:uketdterms="http://naca.central.cranfield.ac.uk/ethos-oai/terms/"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://naca.central.cranfield.ac.uk/ethos-oai/2.0/ http://naca.central.cranfield.ac.uk/ethos-oai/2.0/uketd_dc.xsd">

            <!-- ******* Title: <dc:title> ******* -->
            <!-- dc.title -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='title']/doc:element/doc:field[@name='value']">
                <dc:title><xsl:value-of select="." /></dc:title>
            </xsl:for-each>

            <!-- ******* Alternative Title: <dcterms:alternative> ******* -->
            <!-- dc.title.alternative -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='title']/doc:element[@name='alternative']/doc:element/doc:field[@name='value']">
                <dcterms:alternative><xsl:value-of select="." /></dcterms:alternative>
            </xsl:for-each>

            <!-- ******* Author: <dc.creator> ******* -->
            <!-- dc.contributor.author -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']/doc:element/doc:field[@name='value']">
                <dc:creator><xsl:value-of select="." /></dc:creator>
            </xsl:for-each>

            <!-- ******* Supervisor(s)/Advisor(s): <uketdterms:advisor> ******* -->
            <!-- dc.contributor.advisor -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='advisor']/doc:element/doc:field[@name='value']">
                <uketdterms:advisor><xsl:value-of select="." /></uketdterms:advisor>
            </xsl:for-each>

            <!-- ******* Abstract: <dcterms:abstract> ******* -->
            <!-- dc.description.abstract -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='description']/doc:element[@name='abstract']/doc:element/doc:field[@name='value']">
                <dcterms:abstract><xsl:value-of select="." /></dcterms:abstract>
            </xsl:for-each>

            <!-- ******* Awarding Insitution: <uketdterms:institution> ******* -->
            <!-- dc.publisher -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='publisher']/doc:element/doc:field[@name='value']">
                <uketdterms:institution><xsl:value-of select="." /></uketdterms:institution>
            </xsl:for-each>
            <!-- dc.publisher.institution -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='publisher']/doc:element[@name='institution']/doc:element/doc:field[@name='value']">
                <uketdterms:institution><xsl:value-of select="." /></uketdterms:institution>
            </xsl:for-each>

            <!-- ******* Year of award: <dcterms:issued> ******* -->
            <!-- dc.date.issued -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']/doc:element/doc:field[@name='value']">
                <dcterms:issued><xsl:value-of select="." /></dcterms:issued>
            </xsl:for-each>
            <!-- dc.date.awarded -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='awarded']/doc:element/doc:field[@name='value']">
                <dcterms:issued><xsl:value-of select="." /></dcterms:issued>
            </xsl:for-each>

            <!-- ******* Type: <dc:type> ******* -->
            <!-- dc.type -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element/doc:field[@name='value']">
                <dc:type><xsl:value-of select="." /></dc:type>
            </xsl:for-each>

            <!-- ******* Qualification Level: <uketdterms:qualificationlevel> ******* -->
            <!-- dc.type.qualificationlevel -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element[@name='qualificationlevel']/doc:element/doc:field[@name='value']">
                <uketdterms:qualificationlevel><xsl:value-of select="." /></uketdterms:qualificationlevel>
            </xsl:for-each>

            <!-- ******* Qualification Name: <uketdterms:qualificationname> ******* -->
            <!-- dc.type.qualificationname -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element[@name='qualificationname']/doc:element/doc:field[@name='value']">
                <uketdterms:qualificationname><xsl:value-of select="." /></uketdterms:qualificationname>
            </xsl:for-each>

            <!-- ******* Language: <dc:language xsi:type="dcterms:ISO639-2"> ******* -->
            <!-- dc.language.iso -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='language']/doc:element[@name='iso']/doc:element/doc:field[@name='value']">
                <dc:language xsi:type="dcterms:ISO639-2"><xsl:value-of select="." /></dc:language>
            </xsl:for-each>

            <!-- ******* Sponsors/Funders: <uketdterms:sponsor> ******* -->
            <!-- dc.contributor.sponsor -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='sponsor']/doc:element/doc:field[@name='value']">
                <uketdterms:sponsor><xsl:value-of select="." /></uketdterms:sponsor>
            </xsl:for-each>
            <!-- dc.contributor.funder -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='funder']/doc:element/doc:field[@name='value']">
                <uketdterms:sponsor><xsl:value-of select="." /></uketdterms:sponsor>
            </xsl:for-each>
            <!-- dc.description.sponsorship -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='description']/doc:element[@name='sponsorship']/doc:element/doc:field[@name='value']">
                <uketdterms:sponsor><xsl:value-of select="." /></uketdterms:sponsor>
            </xsl:for-each>

            <!-- ******* Grant Number: <uketdterms:grantnumber> ******* -->
            <!-- dc.identifier.grantnumber -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='grantnumber']/doc:element/doc:field[@name='value']">
                <uketdterms:grantnumber><xsl:value-of select="." /></uketdterms:grantnumber>
            </xsl:for-each>

            <!-- ******* Institutional Repository URL: <dcterms:isReferencedBy> ******* -->
            <!-- dc.identifier.uri -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='uri']/doc:element/doc:field[@name='value']">
                <dcterms:isReferencedBy><xsl:value-of select="." /></dcterms:isReferencedBy>
                <!-- <dc:identifier xsi:type="dcterms:URI"><xsl:value-of select="." /></dc:identifier> -->
            </xsl:for-each>

            <!-- ******* URLs for digital object(s) (obtained from file 'bundles') ******* -->
            <xsl:for-each select="doc:metadata/doc:element[@name='bundles']/doc:element[@name='bundle']">

                <!-- ******* URLs for content bitstreams (from ORIGINAL bundle): <dc:identifier xsi:type="dcterms:URI"> ******* -->
                <xsl:if test="doc:field[@name='name']/text() = 'ORIGINAL'">
                    <xsl:for-each select="doc:element[@name='bitstreams']/doc:element">
                        <dc:identifier xsi:type="dcterms:URI"><xsl:value-of select="doc:field[@name='url']/text()" /></dc:identifier>
                        <uketdterms:checksum xsi:type="uketdterms:MD5"><xsl:value-of select="doc:field[@name='checksum']/text()" /></uketdterms:checksum>
                    </xsl:for-each>
                </xsl:if>

                <!-- ******* URL for License bitstream (from LICENSE bundle): <dcterms:license> ******* -->
                <xsl:if test="doc:field[@name='name']/text() = 'LICENSE'">
                    <xsl:for-each select="doc:element[@name='bitstreams']/doc:element">
                        <dcterms:license><xsl:value-of select="doc:field[@name='url']/text()" /></dcterms:license>
                        <uketdterms:checksum xsi:type="uketdterms:MD5"><xsl:value-of select="doc:field[@name='checksum']/text()" /></uketdterms:checksum>
                    </xsl:for-each>
                </xsl:if>

                <!-- ******* URL for extracted text bitstream (from TEXT bundle): <dcterms:hasFormat> ******* -->
                <xsl:if test="doc:field[@name='name']/text() = 'TEXT'">
                    <xsl:for-each select="doc:element[@name='bitstreams']/doc:element">
                        <dcterms:hasFormat><xsl:value-of select="doc:field[@name='url']/text()" /></dcterms:hasFormat>
                        <uketdterms:checksum xsi:type="uketdterms:MD5"><xsl:value-of select="doc:field[@name='checksum']/text()" /></uketdterms:checksum>
                    </xsl:for-each>
                </xsl:if>

            </xsl:for-each>

            <!-- ******* Embargo Date: <uketdterms:embargodate> ******* -->
            <!-- dc.rights.embargodate -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='rights']/doc:element[@name='embargodate']/doc:element/doc:field[@name='value']">
                <uketdterms:embargodate><xsl:value-of select="." /></uketdterms:embargodate>
            </xsl:for-each>
            <!-- dc.embargo.endate -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='embargo']/doc:element[@name='enddate']/doc:element/doc:field[@name='value']">
                <uketdterms:embargodate><xsl:value-of select="." /></uketdterms:embargodate>
            </xsl:for-each>
            <!-- dc.embargo.terms -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='embargo']/doc:element[@name='terms']/doc:element/doc:field[@name='value']">
                <uketdterms:embargodate><xsl:value-of select="." /></uketdterms:embargodate>
            </xsl:for-each>

            <!-- ******* Embargo Type: <uketdterms:embargotype> ******* -->

            <!-- ******* Rights: <dc:rights> ******* -->
            <!-- dc.rights -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='rights']/doc:element/doc:field[@name='value']">
                <dc:rights><xsl:value-of select="." /></dc:rights>
            </xsl:for-each>
            <!-- dc.rights.embargoreason -->
            <!--
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='rights']/doc:element[@name='embargoreason']/doc:element/doc:field[@name='value']">
                <dc:rights><xsl:value-of select="." /></dc:rights>
            </xsl:for-each>
            -->

            <!-- ******* Subject Keywords: <dc:subject> ******* -->
            <!-- dc.subject -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='subject']/doc:element/doc:field[@name='value']">
                <dc:subject><xsl:value-of select="." /></dc:subject>
            </xsl:for-each>
            <!-- dc.subject.other -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='subject']/doc:element[@name='other']/doc:element/doc:field[@name='value']">
                <dc:subject><xsl:value-of select="." /></dc:subject>
            </xsl:for-each>

            <!-- ******* DDC Keywords: <dc:subject xsi:type="dcterms:DDC"> ******* -->
            <!-- dc.subject.ddc -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='subject']/doc:element[@name='ddc']/doc:element/doc:field[@name='value']">
                <dc:subject xsi:type="dcterms:DDC"><xsl:value-of select="." /></dc:subject>
            </xsl:for-each>

            <!-- ******* LCC Keywords: <dc:subject xsi:type="dcterms:LCC"> ******* -->
            <!-- dc.subject.lcc -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='subject']/doc:element[@name='lcc']/doc:element/doc:field[@name='value']">
                <dc:subject xsi:type="dcterms:LCC"><xsl:value-of select="." /></dc:subject>
            </xsl:for-each>

            <!-- ******* LCSH Keywords: <dc:subject xsi:type="dcterms:LCSH"> ******* -->
            <!-- dc.subject.lcsh -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='subject']/doc:element[@name='lcsh']/doc:element/doc:field[@name='value']">
                <dc:subject xsi:type="dcterms:LCSH"><xsl:value-of select="." /></dc:subject>
            </xsl:for-each>

            <!-- ******* MESH Keywords: <dc:subject xsi:type="dcterms:MESH"> ******* -->
            <!-- dc.subject.mesh -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='subject']/doc:element[@name='mesh']/doc:element/doc:field[@name='value']">
                <dc:subject xsi:type="dcterms:MESH"><xsl:value-of select="." /></dc:subject>
            </xsl:for-each>

            <!-- ******* Author Affiliation: <uketdterms:department> ******* -->
            <!-- dc.contributor.affiliation -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='affiliation']/doc:element/doc:field[@name='value']">
                <uketdterms:department><xsl:value-of select="." /></uketdterms:department>
            </xsl:for-each>
            <!-- dc.publisher.department -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='publisher']/doc:element[@name='department']/doc:element/doc:field[@name='value']">
                <uketdterms:department><xsl:value-of select="." /></uketdterms:department>
            </xsl:for-each>

            <!-- ******* Work Identifier(s): <dc:identifier> ******* -->
            <!-- dc.identifier.doi -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='doi']/doc:element/doc:field[@name='value']">
                <dc:identifier><xsl:value-of select="." /></dc:identifier>
            </xsl:for-each>
            <!-- dc.identifier.isbn -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='isbn']/doc:element/doc:field[@name='value']">
                <dc:identifier><xsl:value-of select="." /></dc:identifier>
            </xsl:for-each>
            <!-- dc.identifier.istc -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='istc']/doc:element/doc:field[@name='value']">
                <dc:identifier><xsl:value-of select="." /></dc:identifier>
            </xsl:for-each>

            <!-- ******* Author Identifier(s): <uketdterms:authoridentifier> ******* -->


        </uketd_dc:uketddc>
    </xsl:template>
</xsl:stylesheet>