<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fn="http://www.w3.org/2005/02/xpath-function" 
	xmlns:d1="http://ns.dataone.org/service/types/v1"
	version="1.0">
	
	<xsl:output method="html" encoding="UTF-8" indent="yes" />
		
	<xsl:template name="nodeList">
		<p>Registered DataONE nodes. Includes Coordinating Nodes (CN) and Member Nodes (MN).</p>
		<hr/>
		<xsl:for-each select="*[local-name()='nodeList']/node">
			<xsl:call-template name="node" />
			<hr/>
		</xsl:for-each>
	</xsl:template>
	
	<xsl:template name="node">
		<xsl:for-each select=".">
			<table>
				<tr>
					<td>Node Id: </td>
					<td><xsl:value-of select="identifier"/></td>
				</tr>
				<tr>
					<td>Type: </td>
					<td><xsl:value-of select="@type"/></td>
				</tr>
				<tr>
					<td>Name: </td>
					<td><xsl:value-of select="name"/></td>
				</tr>
				<tr>
					<td>Description: </td>
					<td><xsl:value-of select="description"/></td>
				</tr>
				<tr>
					<td>Base URL: </td>
					<td>
						<a>
							<xsl:attribute name="href"><xsl:value-of select="baseURL"/></xsl:attribute>
							<xsl:attribute name="target">_blank</xsl:attribute>
							<xsl:value-of select="baseURL"/>
						</a>	
					</td>
				</tr>
				<tr>
					<td>Subject[s]: </td>
					<td>
						<xsl:for-each select="subject">
							<xsl:value-of select="."/>
							<br/>
						</xsl:for-each>	
					</td>
				</tr>
				<tr>
					<td>Services: </td>
					<td>
						<xsl:for-each select="services">
							<xsl:call-template name="services" />
						</xsl:for-each>	
					</td>
				</tr>
				<tr>
					<td>Status: </td>
					<td><xsl:value-of select="@state"/></td>
				</tr>
				<tr>
					<td>Replicate: </td>
					<td><xsl:value-of select="@replicate"/></td>
				</tr>
				<tr>
					<td>Synchronize: </td>
					<td><xsl:value-of select="@synchronize"/></td>
				</tr>
				<tr>
					<td>Synchronization schedule: </td>
					<td>
						<xsl:for-each select="synchronization/schedule">
							<xsl:call-template name="schedule" />
						</xsl:for-each>	
					</td>
				</tr>
				<tr>
					<td>Last Harvested: </td>
					<td>
						<xsl:value-of select="synchronization/lastHarvested"/>
					</td>
				</tr>
				<tr>
					<td>Last Complete Harvest: </td>
					<td>
						<xsl:value-of select="synchronization/lastCompleteHarvest"/>
					</td>
				</tr>
			</table>		
		</xsl:for-each>
	</xsl:template>
	
	<xsl:template name="services">
		<table>
			<tr>
				<td>Name</td>
				<td>Version</td>
				<td>Available</td>
			</tr>
			<xsl:for-each select="service">		
				<tr>
					<td><xsl:value-of select="@name"/></td>
					<td><xsl:value-of select="@version"/></td>
					<td><xsl:value-of select="@available"/></td>
				</tr>
			</xsl:for-each>	
		</table>
	</xsl:template>
	
	<!-- 
	<schedule hour="*" mday="*" min="0,5,10,15,20,25,30,35,40,45,50,55"
				mon="*" sec="0" wday="?" year="*" />
			<lastHarvested>1900-01-01T00:00:00.000+00:00
			</lastHarvested>
			<lastCompleteHarvest>1900-01-01T00:00:00.000+00:00
			</lastCompleteHarvest>
	 -->
	<xsl:template name="schedule">
		<table>
			<tr>
				<td>Year</td>
				<td>Month</td>
				<td>Day of Month</td>
				<td>Day of Week</td>
				<td>Hour</td>
				<td>Minute</td>
				<td>Second</td>
			</tr>
			<xsl:for-each select=".">		
				<tr>
					<td><xsl:value-of select="@year"/></td>
					<td><xsl:value-of select="@mon"/></td>
					<td><xsl:value-of select="@mday"/></td>
					<td><xsl:value-of select="@wday"/></td>
					<td><xsl:value-of select="@hour"/></td>
					<td><xsl:value-of select="@min"/></td>
					<td><xsl:value-of select="@sec"/></td>
				</tr>
			</xsl:for-each>	
		</table>
	</xsl:template>			
	
</xsl:stylesheet>