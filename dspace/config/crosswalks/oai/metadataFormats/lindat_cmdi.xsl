<?xml version="1.0" encoding="UTF-8" ?>
<!-- 
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:doc="http://www.lyncode.com/xoai"
    xmlns:fn="http://custom.crosswalk.functions"
	xmlns:fnx="http://www.w3.org/2005/xpath-functions"
    xmlns:xalan="http://xml.apache.org/xslt"
    xmlns:ms="http://www.ilsp.gr/META-XMLSchema"
    xmlns:olac="http://experimental.loc/olac"
    xmlns:cmd="http://www.clarin.eu/cmd/"
    xmlns:lindat="http://lindat.mff.cuni.cz/ns/experimental/cmdi"
    exclude-result-prefixes="doc xalan fn fnx ms" version="1.0">
    <xsl:import href="metasharev2.xsl"/>
    <xsl:import href="olac-dcmiterms.xsl"/>
    
    <xsl:output omit-xml-declaration="yes" method="xml" indent="yes" xalan:indent-amount="4"/>
    <xsl:namespace-alias stylesheet-prefix="ms" result-prefix="cmd"/>
    <!-- #default probably not working -->
    <xsl:namespace-alias stylesheet-prefix="olac" result-prefix="cmd"/>


    <xsl:variable name="handle" select="/doc:metadata/doc:element[@name='others']/doc:field[@name='handle']/text()"/>
    <xsl:variable name="dc_identifier_uri"
    select="fn:stringReplace(/doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='uri']/doc:element/doc:field[@name='value'])"/>
    <xsl:variable name="modifyDate" select="/doc:metadata/doc:element[@name='others']/doc:field[@name='lastModifyDate']/text()"/>
    <xsl:variable name="dc_rights_uri" select="/doc:metadata/doc:element[@name='dc']/doc:element[@name='rights']/doc:element[@name='uri']/doc:element/doc:field[@name='value']" />
    <xsl:variable name="dsURL" select="fn:getProperty('dspace.ui.url')"/>
    <xsl:variable name="newProfile" select="'clarin.eu:cr1:p_1403526079380'"/>
    <xsl:variable name="oldProfile" select="'clarin.eu:cr1:p_1349361150622'"/>
    
    <xsl:template match="/">
        <xsl:variable name="uploaded_md" select="fn:getUploadedMetadata($handle)"/>
        <xsl:choose>
            <xsl:when test="$uploaded_md != ''">
                <xsl:copy-of select="$uploaded_md"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="ConstructCMDI"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template name="ConstructCMDI">
    	<xsl:variable name="contact" select="/doc:metadata/doc:element[@name='local']/doc:element[@name='contact']/doc:element[@name='person']/doc:element/doc:field[@name='value']"/>
    	<xsl:variable name="profile">
                    <xsl:choose>
                            <xsl:when test="$contact != '' and $dc_rights_uri != ''">
                            	<xsl:value-of select="$newProfile"/>
                            </xsl:when>
                            <xsl:otherwise>
                            	<xsl:value-of select="$oldProfile"/>
                            </xsl:otherwise>
                    </xsl:choose>
                    
        </xsl:variable>
        <cmd:CMD CMDVersion="1.1" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        	<xsl:attribute name="xsi:schemaLocation">
        		<xsl:value-of select="concat('http://www.clarin.eu/cmd/ http://catalog.clarin.eu/ds/ComponentRegistry/rest/registry/profiles/',$profile,'/xsd')"/>
        	</xsl:attribute>
            <xsl:call-template name="AdministrativeMD">
            	<xsl:with-param name="profile" select="$profile"/>
            </xsl:call-template>
            <xsl:choose>
            	<xsl:when test="$profile = $newProfile">
                    <xsl:call-template name="NewComponents"/>
            	</xsl:when>
            	<xsl:otherwise>
            		<xsl:call-template name="OldComponents"/>
            	</xsl:otherwise>
            </xsl:choose>
        </cmd:CMD>
    </xsl:template>
    
    <xsl:template name="AdministrativeMD">
    	<xsl:param name="profile"/>
        <xsl:call-template name="Header">
        	<xsl:with-param name="profile" select="$profile"/>
        </xsl:call-template>
        <xsl:call-template name="Resources"/>
    </xsl:template>
    
    <xsl:template name="Header">
    	<xsl:param name="profile"/>
        <cmd:Header>
            <cmd:MdCreationDate><xsl:value-of select="$modifyDate"/></cmd:MdCreationDate>
            <cmd:MdSelfLink><xsl:value-of select="$dc_identifier_uri"/>@format=cmdi</cmd:MdSelfLink>
            <cmd:MdProfile><xsl:value-of select="$profile"/></cmd:MdProfile>
            <cmd:MdCollectionDisplayName><xsl:value-of select="/doc:metadata/doc:element[@name='others']/doc:field[@name='owningCollection']/text()"/></cmd:MdCollectionDisplayName>
        </cmd:Header>
    </xsl:template>

	<xsl:template name="Resources">
		<cmd:Resources>
			<cmd:ResourceProxyList>
				<cmd:ResourceProxy>
					<xsl:attribute name="id">lp_<xsl:value-of select="/doc:metadata/doc:element[@name='others']/doc:field[@name='itemId']/text()" /></xsl:attribute>
					<cmd:ResourceType>LandingPage</cmd:ResourceType>
					<cmd:ResourceRef>
						<xsl:value-of select="$dc_identifier_uri" />
					</cmd:ResourceRef>
				</cmd:ResourceProxy>
				<xsl:call-template name="ProcessSourceURI"/>
				<xsl:call-template name="ProcessBitstreams"/>
			</cmd:ResourceProxyList>
			<cmd:JournalFileProxyList/>
			<cmd:ResourceRelationList/>
		</cmd:Resources>
	</xsl:template>
	
	<!-- Omit the special "ORE" bitstream and also the consent to publish the data -->
	<xsl:template name="ProcessBitstreams">
	   <xsl:for-each select="/doc:metadata/doc:element[@name='bundles']/doc:element[@name='bundle']/doc:field[@name='name' and text()!='ORE' and text()!='LICENSE']/../doc:element[@name='bitstreams']/doc:element[@name='bitstream']">
	       <cmd:ResourceProxy>
	                   <xsl:attribute name="id">_<xsl:value-of select="./doc:field[@name='id']/text()"/></xsl:attribute>
                       <cmd:ResourceType><xsl:attribute name="mimetype"><xsl:value-of select="./doc:field[@name='format']/text()"/></xsl:attribute>Resource</cmd:ResourceType>
			   <cmd:ResourceRef><xsl:attribute name="lindat:md5_checksum"><xsl:value-of select="./doc:field[@name='checksum']/text()"/></xsl:attribute><xsl:value-of select="concat($dsURL,'/bitstream/handle/',$handle,'/',./doc:field[@name='name']/text(),'?sequence=',./doc:field[@name='sid']/text())"/></cmd:ResourceRef>
           </cmd:ResourceProxy>
	   </xsl:for-each>
	</xsl:template>
	
	<xsl:template name="ProcessSourceURI">
	   <xsl:for-each select="fnx:distinct-values(doc:metadata/doc:element[@name='dc']/doc:element[@name='source']/doc:element[@name='uri']/doc:element/doc:field[@name='value'])">
	       <cmd:ResourceProxy>
	           <xsl:attribute name="id">uri_<xsl:value-of select="position()"/></xsl:attribute>
	           <cmd:ResourceType><xsl:attribute name="mimetype">text/html</xsl:attribute>Resource</cmd:ResourceType>
	           <cmd:ResourceRef><xsl:value-of select="."/></cmd:ResourceRef>
	       </cmd:ResourceProxy>
	   </xsl:for-each>
	</xsl:template>
	
	<xsl:template name="OldComponents">
		<cmd:Components>
			<cmd:data>
				<xsl:call-template name="OLAC_DCMI"/>
				<xsl:if test="doc:metadata/doc:element[@name='metashare']/doc:element[@name='ResourceInfo#IdentificationInfo']/doc:element[@name='resourceName']/doc:element/doc:field[@name='value']">
                    <xsl:call-template name="ResourceInfo">
                        <xsl:with-param name="ns" select='"http://www.clarin.eu/cmd/"'/>
                    </xsl:call-template>
				</xsl:if>
			</cmd:data>
		</cmd:Components>
	</xsl:template>
	
	<xsl:template name="NewComponents">
		<cmd:Components>
			<cmd:LINDAT_CLARIN>
				<xsl:call-template name="bibliography"/>
				<xsl:call-template name="dataInfo"/>
				<xsl:call-template name="licenseInfo"/>
				<!-- relationsInfo -->
			</cmd:LINDAT_CLARIN>
		</cmd:Components>
	</xsl:template>
	
	<xsl:template name="bibliography">
		<cmd:bibliographicInfo>
			<xsl:if test="doc:metadata/doc:element[@name='dc']/doc:element[@name='source']/doc:element[@name='uri']/doc:element/doc:field[@name='value']">
				<cmd:projectUrl><xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='source']/doc:element[@name='uri']/doc:element/doc:field[@name='value']"/></cmd:projectUrl>
			</xsl:if>
			<cmd:titles>
				<cmd:title>
					<xsl:attribute name="xml:lang">en</xsl:attribute>
					<xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='title']/doc:element/doc:field[@name='value']"/>
				</cmd:title>
			</cmd:titles>
			<cmd:authors>
				<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']/doc:element/doc:field[@name='value']">
					<xsl:copy-of select="fn:getAuthor(.)"/>
				</xsl:for-each>
			</cmd:authors>
			<cmd:dates>
				<cmd:dateIssued>
					<xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']/doc:element/doc:field[@name='value']"/>
                </cmd:dateIssued>                
			</cmd:dates>
			<cmd:identifiers>
				<cmd:identifier type="Handle"><xsl:value-of select="$dc_identifier_uri"/></cmd:identifier>
			</cmd:identifiers>
			<xsl:if test="doc:metadata/doc:element[@name='local']/doc:element[@name='sponsor']/doc:element/doc:field[@name='value']">
                <cmd:funds>
                    <xsl:for-each select="doc:metadata/doc:element[@name='local']/doc:element[@name='sponsor']/doc:element/doc:field[@name='value']">
                        <xsl:copy-of select="fn:getFunding(.)"/>
                    </xsl:for-each>
                </cmd:funds>
            </xsl:if>
			<xsl:copy-of select="fn:getContact(doc:metadata/doc:element[@name='local']/doc:element[@name='contact']/doc:element[@name='person']/doc:element/doc:field[@name='value'])"/>
			<cmd:publishers>
				<cmd:publisher>
					<xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='publisher']/doc:element/doc:field[@name='value']"/>
				</cmd:publisher>
			</cmd:publishers>
			
		</cmd:bibliographicInfo>
	</xsl:template>

	<xsl:template name="dataInfo">
		<cmd:dataInfo>
			<cmd:type>
					<xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element/doc:field[@name='value']"/>
			</cmd:type>
			<xsl:if test="doc:metadata/doc:element[@name='metashare']/doc:element[@name='ResourceInfo#ContentInfo']/doc:element[@name='detailedType']/doc:element/doc:field[@name='value']">
				<cmd:detailedType>
					<xsl:value-of select="doc:metadata/doc:element[@name='metashare']/doc:element[@name='ResourceInfo#ContentInfo']/doc:element[@name='detailedType']/doc:element/doc:field[@name='value']"/>
                </cmd:detailedType>
			</xsl:if>
			<cmd:description>
					<xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='description']/doc:element/doc:field[@name='value']"/>
			</cmd:description>
			<xsl:if test="doc:metadata/doc:element[@name='dc']/doc:element[@name='language']/doc:element[@name='iso']/doc:element/doc:field[@name='value']">
				<cmd:languages>
					<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='language']/doc:element[@name='iso']/doc:element/doc:field[@name='value']">
						<cmd:language>
							<cmd:code><xsl:value-of select="."/></cmd:code>
							<cmd:name><xsl:value-of select="fn:getLangForCode(.)"/></cmd:name>
						</cmd:language>
                	</xsl:for-each>
				</cmd:languages>
			</xsl:if>
			<xsl:if test="doc:metadata/doc:element[@name='dc']/doc:element[@name='subject']/doc:element/doc:field[@name='value']">
				<cmd:keywords>
					<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='subject']/doc:element/doc:field[@name='value']">
						<cmd:keyword>
							<xsl:value-of select="."/>
						</cmd:keyword>
					</xsl:for-each>
				</cmd:keywords>
			</xsl:if>
			<xsl:if test="doc:metadata/doc:element[@name='local']/doc:element[@name='demo']/doc:element[@name='uri']/doc:element/doc:field[@name='value']">
				<cmd:links>
					<cmd:link>
						<xsl:value-of select="doc:metadata/doc:element[@name='local']/doc:element[@name='demo']/doc:element[@name='uri']/doc:element/doc:field[@name='value']"/>
					</cmd:link>
				</cmd:links>
			</xsl:if>
			<xsl:if test="doc:metadata/doc:element[@name='local']/doc:element[@name='size']/doc:element[@name='info']/doc:element/doc:field[@name='value']">
				<cmd:sizeInfo>
					<xsl:for-each select="doc:metadata/doc:element[@name='local']/doc:element[@name='size']/doc:element[@name='info']/doc:element/doc:field[@name='value']">
                          <xsl:copy-of select="fn:getSize(.)"/>
                	</xsl:for-each>
				</cmd:sizeInfo>
			</xsl:if>
		</cmd:dataInfo>
	</xsl:template>

	<xsl:template name="licenseInfo">
		<cmd:licenseInfo>
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='rights']/doc:element[@name='uri']/doc:element/doc:field[@name='value']">
				<cmd:license>
					<cmd:uri><xsl:value-of select="."/></cmd:uri>
				</cmd:license>
			</xsl:for-each>
		</cmd:licenseInfo>
	</xsl:template>
	
</xsl:stylesheet>
