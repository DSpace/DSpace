<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" 
	xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
	xmlns:dc="http://purl.org/dc/elements/1.1/"
	xmlns:dcterms="http://purl.org/dc/terms/" 
	xmlns="http://www.loc.gov/MARC21/slim" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	exclude-result-prefixes="dc dim">
	<xsl:output method="xml" indent="yes"/>
	
	<xsl:template match="/">
		<record xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.loc.gov/MARC21/slim http://www.loc.gov/standards/marcxml/schema/MARC21slim.xsd" >
			<xsl:element name="leader">
				<xsl:variable name="type" select="dc:type"/>
				<xsl:variable name="leader06">
					<xsl:choose>
						<xsl:when test="$type='collection'">p</xsl:when>
						<xsl:when test="$type='dataset'">m</xsl:when>
						<xsl:when test="$type='event'">r</xsl:when>
						<xsl:when test="$type='image'">k</xsl:when>
						<xsl:when test="$type='interactive resource'">m</xsl:when>
						<xsl:when test="$type='service'">m</xsl:when>
						<xsl:when test="$type='software'">m</xsl:when>
						<xsl:when test="$type='sound'">i</xsl:when>
						<xsl:when test="$type='text'">a</xsl:when>
						<xsl:otherwise>a</xsl:otherwise>
					</xsl:choose>
				</xsl:variable>
				<xsl:variable name="leader07">
					<xsl:choose>
						<xsl:when test="$type='collection'">c</xsl:when>
						<xsl:otherwise>m</xsl:otherwise>
					</xsl:choose>
				</xsl:variable>
				<xsl:value-of select="concat('      ',$leader06,$leader07,'         3u     ')"/>
			</xsl:element>

			<datafield tag="042" ind1=" " ind2=" ">
				<subfield code="a">dc</subfield>
			</datafield>

			<xsl:for-each select="//dim:field[@mdschema='dc'][@element='contributor']">
				<datafield tag="720" ind1="0" ind2="0">
					<subfield code="a">
						<xsl:value-of select="."/>
					</subfield>
					<subfield code="e">collaborator</subfield>
				</datafield>
			</xsl:for-each>

			<xsl:for-each select="//dim:field[@element ='creator']">
				<datafield tag="720" ind1=" " ind2=" ">
					<subfield code="a">
						<xsl:value-of select="."/>
					</subfield>
					<subfield code="e">author</subfield>
				</datafield>
			</xsl:for-each>

			<xsl:for-each select="//dim:field[@element='date'][@qualifier='available']">
				<datafield tag="260" ind1=" " ind2=" ">
					<subfield code="c">
						<xsl:value-of select="."/>
					</subfield>
				</datafield>
			</xsl:for-each>	

			<xsl:for-each select="//dim:field[@element='description'][@qualifier='abstract']">
				<datafield tag="520" ind1=" " ind2=" ">
					<subfield code="a">
						<xsl:value-of select="."/>
					</subfield>
				</datafield>
			</xsl:for-each>
			
			<xsl:for-each select="//dim:field[@element='format'][@qualifier='mimetype']">
				<datafield tag="856" ind1=" " ind2=" ">
					<subfield code="q">
						<xsl:value-of select="."/>
					</subfield>
				</datafield>
			</xsl:for-each>

			<xsl:for-each select="//dim:field[@element='identifier'][@qualifier='uri']">
				<datafield tag="024" ind1="8" ind2=" ">
					<subfield code="a">
						<xsl:value-of select="."/>
					</subfield>
				</datafield>
			</xsl:for-each>

			<xsl:for-each select="//dim:field[@element='language'][@qualifier='iso']">
				<datafield tag="546" ind1=" " ind2=" ">
					<subfield code="a">
						<xsl:value-of select="."/>
					</subfield>
				</datafield>
			</xsl:for-each>
			
			<xsl:for-each select="//dim:field[@mdschema='thesis'][@element='degree'][@qualifier='grantor']">
				<datafield tag="260" ind1=" " ind2=" ">
					<subfield code="b">
						<xsl:value-of select="."/>
					</subfield>
				</datafield>
			</xsl:for-each>

			<xsl:for-each select="//dc:relation">
				<datafield tag="787" ind1="0" ind2=" ">
					<subfield code="n">
						<xsl:value-of select="."/>
					</subfield>
				</datafield>
			</xsl:for-each>

			<xsl:for-each select="//dc:rights">
				<datafield tag="540" ind1=" " ind2=" ">
					<subfield code="a">
						<xsl:value-of select="."/>
					</subfield>
				</datafield>
			</xsl:for-each>

			<xsl:for-each select="//dc:source">
				<datafield tag="786" ind1="0" ind2=" ">
					<subfield code="n">
						<xsl:value-of select="."/>
					</subfield>
				</datafield>
			</xsl:for-each>

			<xsl:for-each select="//dim:field[@element='subject']">
				<datafield tag="653" ind1=" " ind2=" ">
					<subfield code="a">
						<xsl:value-of select="."/>
					</subfield>
				</datafield>
			</xsl:for-each>
																							
			<xsl:for-each select="//dim:field[@element='title']">
				<datafield tag="245" ind1="0" ind2="0">
					<subfield code="a">
						<xsl:value-of select="."/>
					</subfield>
				</datafield>
			</xsl:for-each>

			<xsl:for-each select="//dim:field[@element='title'][position()>1]">
				<datafield tag="246" ind1="3" ind2="3">
					<subfield code="a">
						<xsl:value-of select="."/>
					</subfield>
				</datafield>
			</xsl:for-each>

			<xsl:for-each select="//dc:type">
				<datafield tag="655" ind1="7" ind2=" ">
					<subfield code="a">
						<xsl:value-of select="."/>
					</subfield>
					<subfield code="2">local</subfield>
				</datafield>
			</xsl:for-each>
		</record>
	</xsl:template>
</xsl:stylesheet><!-- Stylus Studio meta-information - (c)1998-2002 eXcelon Corp.
<metaInformation>
<scenarios ><scenario default="no" name="UnQual" userelativepaths="yes" externalpreview="no" url="..\xml\dc\unqualified.xml" htmlbaseurl="" outputurl="" processortype="internal" commandline="" additionalpath="" additionalclasspath="" postprocessortype="none" postprocesscommandline="" postprocessadditionalpath="" postprocessgeneratedext=""/><scenario default="no" name="Qual" userelativepaths="yes" externalpreview="no" url="..\xml\dc\qualified.xml" htmlbaseurl="" outputurl="" processortype="internal" commandline="" additionalpath="" additionalclasspath="" postprocessortype="none" postprocesscommandline="" postprocessadditionalpath="" postprocessgeneratedext=""/><scenario default="no" name="RDF" userelativepaths="yes" externalpreview="no" url="..\..\..\printmaking.shtml.rdf" htmlbaseurl="" outputurl="" processortype="internal" commandline="" additionalpath="" additionalclasspath="" postprocessortype="none" postprocesscommandline="" postprocessadditionalpath="" postprocessgeneratedext=""/><scenario default="no" name="s7" userelativepaths="yes" externalpreview="no" url="..\ifla\s7dc.xml" htmlbaseurl="" outputurl="" processortype="internal" commandline="" additionalpath="" additionalclasspath="" postprocessortype="none" postprocesscommandline="" postprocessadditionalpath="" postprocessgeneratedext=""/><scenario default="yes" name="Scenario1" userelativepaths="yes" externalpreview="no" url="..\..\..\t.xml" htmlbaseurl="" outputurl="" processortype="internal" commandline="" additionalpath="" additionalclasspath="" postprocessortype="none" postprocesscommandline="" postprocessadditionalpath="" postprocessgeneratedext=""/></scenarios><MapperInfo srcSchemaPath="" srcSchemaRoot="" srcSchemaPathIsRelative="yes" srcSchemaInterpretAsXML="no" destSchemaPath="" destSchemaRoot="" destSchemaPathIsRelative="yes" destSchemaInterpretAsXML="no"/>
</metaInformation>
-->
