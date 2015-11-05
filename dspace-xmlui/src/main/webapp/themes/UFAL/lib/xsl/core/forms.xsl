<!--
	/* Created for LINDAT/CLARIN */
	Templates to cover the forms and forms fields.
	Author: Amir Kamran
-->

<xsl:stylesheet version="1.0"
	xmlns:i18n="http://apache.org/cocoon/i18n/2.1" xmlns:dri="http://di.tamu.edu/DRI/1.0/"
	xmlns:mets="http://www.loc.gov/METS/" xmlns:xlink="http://www.w3.org/TR/xlink/"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
	xmlns:xhtml="http://www.w3.org/1999/xhtml" xmlns:mods="http://www.loc.gov/mods/v3"
	xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns="http://www.w3.org/1999/xhtml"
	exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc">

	<xsl:output indent="yes" />

	<!-- Special treatment of a list type "form", which is used to encode simple 
		forms and give them structure. This is done partly to ensure that the resulting 
		HTML form follows accessibility guidelines. -->

	<xsl:template match="dri:list[@type='form']" priority="3">
		<xsl:choose>
			<xsl:when test="ancestor::dri:list[@type='form']">
				<li>
					<div style="margin-top: 20px;">
						<xsl:call-template name="standardAttributes">
							<xsl:with-param name="class">well well-white</xsl:with-param>
						</xsl:call-template>
						<xsl:apply-templates select="dri:head" />
						<ol class="unstyled">
							<xsl:apply-templates select="*[not(name()='label' or name()='head')]" />
						</ol>
					</div>
				</li>
			</xsl:when>
			<xsl:otherwise>
				<div>
					<xsl:call-template name="standardAttributes">						
							<xsl:with-param name="class">
								<xsl:if test="dri:item[dri:field[not(@type='button')]!=0]">
									well well-light
								</xsl:if>
							</xsl:with-param>						
					</xsl:call-template>
					<xsl:apply-templates select="dri:head" />
					<ol class="unstyled">
						<xsl:apply-templates select="*[not(name()='label' or name()='head')]" />
					</ol>
				</div>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!-- TODO: Account for the dri:hi/dri:field kind of nesting here and everywhere else... -->
	<xsl:template match="dri:list[@type='form']/dri:item" priority="3">
		<li>
			<xsl:call-template name="standardAttributes">
				<xsl:with-param name="class">
					<xsl:text>form-group </xsl:text>
					<xsl:if test=".//dri:error">
						has-error
					</xsl:if>
					<xsl:if test=".//dri:field[not(@type='button'  or @type='hidden')]!=0">
						well well-white
					</xsl:if>
				</xsl:with-param>
			</xsl:call-template>
			<xsl:choose>
				<xsl:when test="dri:field[@type='composite']">
					<xsl:call-template name="pick-label" />
					<xsl:apply-templates mode="formComposite" />
				</xsl:when>
				<xsl:when test="dri:list[@type='form']">
					<xsl:apply-templates />
				</xsl:when>
				<xsl:otherwise>
					<xsl:if test=".//dri:field[not(@type='button' or @type='hidden')]!=0">
						<xsl:call-template name="pick-label" />
					</xsl:if>
					<div>
						<xsl:apply-templates />
						<!-- special name used in submission UI review page -->
						<xsl:if test="@n = 'submit-review-field-with-authority'">
							<xsl:call-template name="authorityConfidenceIcon">
								<xsl:with-param name="confidence"
									select="substring-after(./@rend, 'cf-')" />
							</xsl:call-template>
						</xsl:if>
					</div>
				</xsl:otherwise>
			</xsl:choose>
			<!-- UFAL - openaire more info link -->
			<xsl:if test="./dri:field[@id='aspect.submission.StepTransformer.field.dc_relation']">
				<a href="https://www.openaire.eu/open-access-info/open-access-in-fp7-seventh-research-framework-programme">More Information</a>
			</xsl:if>
		</li>
	</xsl:template>

	<!-- An item in a nested "form" list -->
	<xsl:template match="dri:list[@type='form']//dri:list[@type='form']/dri:item" priority="3">
		<li>
			<xsl:call-template name="standardAttributes">
				<xsl:with-param name="class">
					<xsl:text>form-group </xsl:text>
					<xsl:if test=".//dri:error">
						has-error
					</xsl:if>					
				</xsl:with-param>
			</xsl:call-template>

			<xsl:call-template name="pick-label" />

			<xsl:choose>
				<xsl:when test="dri:field[@type='composite']">
					<xsl:apply-templates mode="formComposite" />
				</xsl:when>
				<xsl:otherwise>
					<div>
						<xsl:apply-templates />
						<!-- special name used in submission UI review page -->
						<xsl:if test="@n = 'submit-review-field-with-authority'">
							<xsl:call-template name="authorityConfidenceIcon">
								<xsl:with-param name="confidence"
									select="substring-after(./@rend, 'cf-')" />
							</xsl:call-template>
						</xsl:if>
					</div>
				</xsl:otherwise>
			</xsl:choose>
		</li>
	</xsl:template>

	<xsl:template name="pick-label">
		<xsl:choose>
			<xsl:when test="dri:field/dri:label">
				<label class="control-label">
					<xsl:choose>
						<xsl:when test="./dri:field/@id">
							<xsl:attribute name="for">
								<xsl:value-of select="translate(./dri:field/@id,'.','_')" />
							</xsl:attribute>
						</xsl:when>
					</xsl:choose>
					<xsl:apply-templates select="dri:field/dri:label" mode="formComposite" />
				</label>
			</xsl:when>
			<xsl:when test="string-length(string(preceding-sibling::*[1][local-name()='label'])) > 0">
				<xsl:choose>
					<xsl:when test="./dri:field/@id">
						<label class="control-label">
							<xsl:apply-templates select="preceding-sibling::*[1][local-name()='label']" />
						</label>
					</xsl:when>
					<xsl:otherwise>
						<span>
							<xsl:apply-templates select="preceding-sibling::*[1][local-name()='label']" />
						</span>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when>
			<xsl:when test="dri:field">
				<xsl:choose>
					<xsl:when test="string-length(string(preceding-sibling::*[1][local-name()='label'])) > 0">
						<label class="control-label">
							<xsl:choose>
								<xsl:when test="./dri:field/@id">
									<xsl:attribute name="for">
										<xsl:value-of select="translate(./dri:field/@id,'.','_')" />
									</xsl:attribute>
								</xsl:when>
								<xsl:otherwise></xsl:otherwise>
							</xsl:choose>
							<xsl:apply-templates select="preceding-sibling::*[1][local-name()='label']" />
						</label>
					</xsl:when>
					<xsl:otherwise>
						<xsl:apply-templates select="preceding-sibling::*[1][local-name()='label']" />
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when>
			<xsl:otherwise>
				<!-- If the label is empty and the item contains no field, omit the label. 
					This is to make the text inside the item (since what else but text can be 
					there?) stretch across both columns of the list. -->
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template match="dri:list[@type='form']/dri:label" priority="3">
		<label>
			<xsl:attribute name="class">
				   <xsl:text>control-label</xsl:text>
	               <xsl:if test="@rend">
	                     <xsl:text> </xsl:text>
	                     <xsl:value-of select="@rend" />
	                 </xsl:if>
	        </xsl:attribute>
			<xsl:choose>
				<xsl:when test="following-sibling::dri:item[1]/dri:field/@id">
					<xsl:attribute name="for">
						<xsl:value-of select="translate(following-sibling::dri:item[1]/dri:field/@id,'.','_')" />
					</xsl:attribute>
				</xsl:when>
				<xsl:otherwise>
				</xsl:otherwise>
			</xsl:choose>
		<xsl:apply-templates />
		</label>		
	</xsl:template>

	<xsl:template match="dri:field/dri:label" mode="formComposite">
		<xsl:apply-templates />
	</xsl:template>
	
    <!-- Handle hidden fields in items with compositeComponent in usual way -->
    <xsl:template match="dri:field[@type='hidden']" mode="formComposite">
        <xsl:apply-templates select="." mode="normalField" />
    </xsl:template>

    <xsl:template match="dri:list[@type='form']/dri:head" priority="5">
        <legend>
            <xsl:apply-templates />
        </legend>
    </xsl:template>	

	<xsl:template match="dri:list[@type='form']/dri:head" priority="5">
		<legend>
			<xsl:apply-templates />
		</legend>
	</xsl:template>

	<!-- NON-instance composite fields (i.e. not repeatable) -->
	<xsl:template match="dri:field[@type='composite']" mode="formComposite">
		<div class="form-group">
			<xsl:apply-templates select="dri:field/dri:error" mode="compositeComponent" />
			<xsl:apply-templates select="dri:error" mode="compositeComponent" />		
			<xsl:variable name="confidenceIndicatorID" select="concat(translate(@id,'.','_'),'_confidence_indicator')" />
			<xsl:apply-templates select="dri:field" mode="compositeComponent" />
			<xsl:choose>
				<xsl:when test="dri:params/@choicesPresentation = 'suggest'">
					<xsl:message terminate="yes">
						<xsl:text>ERROR: Input field with "suggest" (autocomplete) choice behavior is not implemented for Composite (e.g. "name") fields.</xsl:text>
					</xsl:message>
				</xsl:when>
				<!-- lookup popup includes its own Add button if necessary. -->
				<xsl:when test="dri:params/@choicesPresentation = 'lookup'">
					<xsl:call-template name="addLookupButton">
						<xsl:with-param name="isName" select="'true'" />
						<xsl:with-param name="confIndicator" select="$confidenceIndicatorID" />
					</xsl:call-template>
				</xsl:when>
			</xsl:choose>
			<xsl:if test="dri:params/@authorityControlled">
				<xsl:variable name="confValue" select="dri:field/dri:value[@type='authority'][1]/@confidence" />
				<xsl:call-template name="authorityConfidenceIcon">
					<xsl:with-param name="confidence" select="$confValue" />
					<xsl:with-param name="id" select="$confidenceIndicatorID" />
				</xsl:call-template>
				<xsl:call-template name="authorityInputFields">
					<xsl:with-param name="name" select="@n" />
					<xsl:with-param name="authValue" select="dri:field/dri:value[@type='authority'][1]/text()" />
					<xsl:with-param name="confValue" select="$confValue" />
				</xsl:call-template>
			</xsl:if>
			<xsl:apply-templates select="dri:help" mode="compositeComponent" />
		</div>
	</xsl:template>

	<!-- The handling of the special case of instanced composite fields under "form" lists -->
	<xsl:template match="dri:field[@type='composite'][dri:field/dri:instance | dri:params/@operations]" mode="formComposite" priority="2">
		<xsl:variable name="confidenceIndicatorID" select="concat(translate(@id,'.','_'),'_confidence_indicator')" />
		<div class="form-group">
			<xsl:apply-templates select="dri:field/dri:error" mode="compositeComponent" />
			<xsl:apply-templates select="dri:error" mode="compositeComponent" />		
			<xsl:apply-templates select="dri:field" mode="compositeComponent" />
			<xsl:if test="contains(dri:params/@operations,'add')">
				<!-- Add buttons should be named "submit_[field]_add" so that we can 
					ignore errors from required fields when simply adding new values -->
				<input type="submit" value="Add" name="{concat('submit_',@n,'_add')}" class="btn btn-repository">
					<!-- Make invisible if we have choice-lookup operation that provides  its own Add. -->
					<xsl:if test="dri:params/@choicesPresentation = 'lookup'">
						<xsl:attribute name="style">
                      <xsl:text>display:none;</xsl:text>
                    </xsl:attribute>
					</xsl:if>
				</input>
			</xsl:if>
			<!-- insert choice mechansim and/or Add button here -->
			<xsl:choose>
				<xsl:when test="dri:params/@choicesPresentation = 'suggest'">
					<xsl:message terminate="yes">
						<xsl:text>ERROR: Input field with "suggest" (autocomplete) choice behavior is not implemented for Composite (e.g. "name") fields.</xsl:text>
					</xsl:message>
				</xsl:when>
				<!-- lookup popup includes its own Add button if necessary. -->
				<xsl:when test="dri:params/@choicesPresentation = 'lookup'">
					<xsl:call-template name="addLookupButton">
						<xsl:with-param name="isName" select="'true'" />
						<xsl:with-param name="confIndicator" select="$confidenceIndicatorID" />
					</xsl:call-template>
				</xsl:when>
			</xsl:choose>
			<!-- place to store authority value -->
			<xsl:if test="dri:params/@authorityControlled">
				<xsl:call-template name="authorityConfidenceIcon">
					<xsl:with-param name="confidence" select="dri:value[@type='authority']/@confidence" />
					<xsl:with-param name="id" select="$confidenceIndicatorID" />
				</xsl:call-template>
				<xsl:call-template name="authorityInputFields">
					<xsl:with-param name="name" select="@n" />
					<xsl:with-param name="authValue" select="dri:value[@type='authority']/text()" />
					<xsl:with-param name="confValue" select="dri:value[@type='authority']/@confidence" />
				</xsl:call-template>
			</xsl:if>
			<div class="spacer">&#160;</div>			
			<xsl:apply-templates select="dri:help" mode="compositeComponent" />
			<xsl:if test="dri:instance or dri:field/dri:instance">
				<div class="alert" style="margin-top: 10px;">
					<xsl:call-template name="fieldIterator">
						<xsl:with-param name="position">1</xsl:with-param>
					</xsl:call-template>
					<xsl:if test="contains(dri:params/@operations,'delete') and (dri:instance or dri:field/dri:instance)">
						<!-- Delete buttons should be named "submit_[field]_delete" so that 
							we can ignore errors from required fields when simply removing values -->
						<input type="submit" value="Remove selected" name="{concat('submit_',@n,'_delete')}" class="btn btn-link btn-small" />
					</xsl:if>
					<xsl:for-each select="dri:field">
						<xsl:apply-templates select="dri:instance" mode="hiddenInterpreter" />
					</xsl:for-each>
				</div>
			</xsl:if>
		</div>
	</xsl:template>

	<!-- TODO: The field section works but needs a lot of scrubbing. I would 
		say a third of the templates involved are either bloated or superfluous. -->


	<!-- Things I know: 1. I can tell a field is multivalued if it has instances 
		in it 2. I can't really do that for composites, although I can check its 
		component fields for condition 1 above. 3. Fields can also be inside "form" 
		lists, which is its own unique condition -->

	<!-- Fieldset (instanced) field stuff, in the case of non-composites -->
	<xsl:template match="dri:field[dri:field/dri:instance | dri:params/@operations]" priority="2">
		<xsl:apply-templates select="dri:error" mode="error" />	
		<!-- Create the first field normally -->		
		<xsl:apply-templates select="." mode="normalField" />
		<!-- Follow it up with an ADD button if the add operation is specified. 
			This allows entering more than one value for this field. -->
		<xsl:if test="contains(dri:params/@operations,'add')">
			<!-- Add buttons should be named "submit_[field]_add" so that we can ignore 
				errors from required fields when simply adding new values -->
			<input type="submit" value="Add" name="{concat('submit_',@n,'_add')}"
				class="btn btn-repository">
				<!-- Make invisible if we have choice-lookup popup that provides its 
					own Add. -->
				<xsl:if test="dri:params/@choicesPresentation = 'lookup'">
					<xsl:attribute name="style">
                  <xsl:text>display:none;</xsl:text>
                </xsl:attribute>
				</xsl:if>
			</input>
		</xsl:if>
		<br />
		<xsl:apply-templates select="dri:help" mode="help" />
		<xsl:if test="dri:instance">
			<div class="alert" style="margin-top: 10px;">
				<!-- Iterate over the dri:instance elements contained in this field. 
					The instances contain stored values as either "interpreted", "raw", or "default" 
					values. -->
				<xsl:call-template name="simpleFieldIterator">
					<xsl:with-param name="position">1</xsl:with-param>
				</xsl:call-template>
				<!-- Conclude with a DELETE button if the delete operation is specified. 
					This allows removing one or more values stored for this field. -->
				<xsl:if test="contains(dri:params/@operations,'delete') and dri:instance">
					<!-- Delete buttons should be named "submit_[field]_delete" so that 
						we can ignore errors from required fields when simply removing values -->
					<input type="submit" value="Remove selected" name="{concat('submit_',@n,'_delete')}"
						class="btn btn-link btn-small" />
				</xsl:if>
				<!-- Behind the scenes, add hidden fields for every instance set. This 
					is to make sure that the form still submits the information in those instances, 
					even though they are no longer encoded as HTML fields. The DRI Reference 
					should contain the exact attributes the hidden fields should have in order 
					for this to work properly. -->
				<xsl:apply-templates select="dri:instance" mode="hiddenInterpreter" />
			</div>
		</xsl:if>
	</xsl:template>

	<!-- The iterator is a recursive function that creates a checkbox (to be 
		used in deletion) for each value instance and interprets the value inside. 
		It also creates a hidden field from the raw value contained in the instance. -->
	<xsl:template name="simpleFieldIterator">
		<xsl:param name="position" />
		<xsl:if test="dri:instance[position()=$position]">
			<input type="checkbox" value="{concat(@n,'_',$position)}" name="{concat(@n,'_selected')}" />
			<xsl:apply-templates select="dri:instance[position()=$position]"
				mode="interpreted" />

			<!-- look for authority value in instance. -->
			<xsl:if
				test="dri:instance[position()=$position]/dri:value[@type='authority']">
				<xsl:call-template name="authorityConfidenceIcon">
					<xsl:with-param name="confidence"
						select="dri:instance[position()=$position]/dri:value[@type='authority']/@confidence" />
				</xsl:call-template>
			</xsl:if>
			<br />
			<xsl:call-template name="simpleFieldIterator">
				<xsl:with-param name="position">
					<xsl:value-of select="$position + 1" />
				</xsl:with-param>
			</xsl:call-template>
		</xsl:if>
	</xsl:template>

	<!-- Authority: added fields for auth values as well. -->
	<!-- Common case: use the raw value of the instance to create the hidden 
		field -->
	<xsl:template match="dri:instance" mode="hiddenInterpreter">
		<input type="hidden">
			<xsl:attribute name="name"><xsl:value-of
				select="concat(../@n,'_',position())" /></xsl:attribute>
			<xsl:attribute name="value">
                <xsl:value-of select="dri:value[@type='raw']" />
            </xsl:attribute>
		</input>
		<!-- XXX do we want confidence icon here?? -->
		<xsl:if test="dri:value[@type='authority']">
			<xsl:call-template name="authorityInputFields">
				<xsl:with-param name="name" select="../@n" />
				<xsl:with-param name="position" select="position()" />
				<xsl:with-param name="authValue"
					select="dri:value[@type='authority']/text()" />
				<xsl:with-param name="confValue"
					select="dri:value[@type='authority']/@confidence" />
			</xsl:call-template>
		</xsl:if>
	</xsl:template>

	<!-- Select box case: use the selected options contained in the instance 
		to create the hidden fields -->
	<xsl:template match="dri:field[@type='select']/dri:instance"
		mode="hiddenInterpreter">
		<xsl:variable name="position" select="position()" />
		<xsl:for-each select="dri:value[@type='option']">
			<input type="hidden">
				<xsl:attribute name="name">
                    <xsl:value-of select="concat(../../@n,'_',$position)" />
                </xsl:attribute>
				<!-- Since the dri:option and dri:values inside a select field are related 
					by the return value, encoded in @returnValue and @option attributes respectively, 
					the option attribute can be used directly instead of being resolved to the 
					the correct option -->
				<xsl:attribute name="value">
                    <!--<xsl:value-of select="../../dri:option[@returnValue = current()/@option]"/>-->
                    <xsl:value-of select="@option" />
                </xsl:attribute>
			</input>
		</xsl:for-each>
	</xsl:template>



	<!-- Composite instanced field stuff -->
	<!-- It is also the one that receives the special error and help handling -->
	<xsl:template match="dri:field[@type='composite'][dri:field/dri:instance | dri:params/@operations]" priority="3">
		<!-- First is special, so first we grab all the values from the child fields. 
			We do this by applying normal templates to the field, which should ignore 
			instances. -->
		<xsl:apply-templates select="dri:field/dri:error" mode="compositeComponent" />
		<xsl:apply-templates select="dri:error" mode="compositeComponent" />			
		<span class="ds-composite-field">
			<xsl:apply-templates select="dri:field" mode="compositeComponent" />
		</span>
		<xsl:apply-templates select="dri:help" mode="compositeComponent" />
		<!-- Insert choice mechanism here. Follow it up with an ADD button if the 
			add operation is specified. This allows entering more than one value for 
			this field. -->

		<xsl:if test="contains(dri:params/@operations,'add')">
			<!-- Add buttons should be named "submit_[field]_add" so that we can ignore 
				errors from required fields when simply adding new values -->
			<input type="submit" value="Add" name="{concat('submit_',@n,'_add')}"
				class="btn btn-repository">
				<!-- Make invisible if we have choice-lookup popup that provides its 
					own Add. -->
				<xsl:if test="dri:params/@choicesPresentation = 'lookup'">
					<xsl:attribute name="style">
                  <xsl:text>display:none;</xsl:text>
                </xsl:attribute>
				</xsl:if>
			</input>
		</xsl:if>

		<xsl:variable name="confidenceIndicatorID"
			select="concat(translate(@id,'.','_'),'_confidence_indicator')" />
		<xsl:if test="dri:params/@authorityControlled">
			<!-- XXX note that this is wrong and won't get any authority values, but 
				- for instanced inputs the entry box starts out empty anyway. -->
			<xsl:call-template name="authorityConfidenceIcon">
				<xsl:with-param name="confidence"
					select="dri:value[@type='authority']/@confidence" />
				<xsl:with-param name="id" select="$confidenceIndicatorID" />
			</xsl:call-template>
			<xsl:call-template name="authorityInputFields">
				<xsl:with-param name="name" select="@n" />
				<xsl:with-param name="id" select="@id" />
				<xsl:with-param name="authValue"
					select="dri:value[@type='authority']/text()" />
				<xsl:with-param name="confValue"
					select="dri:value[@type='authority']/@confidence" />
			</xsl:call-template>
		</xsl:if>
		<xsl:choose>
			<xsl:when test="dri:params/@choicesPresentation = 'suggest'">
				<xsl:call-template name="addAuthorityAutocomplete">
					<xsl:with-param name="confidenceIndicatorID" select="$confidenceIndicatorID" />
				</xsl:call-template>
			</xsl:when>
			<!-- lookup popup includes its own Add button if necessary. -->
			<!-- XXX does this need a Confidence Icon? -->
			<xsl:when test="dri:params/@choicesPresentation = 'lookup'">
				<xsl:call-template name="addLookupButton">
					<xsl:with-param name="isName" select="'true'" />
					<xsl:with-param name="confIndicator" select="$confidenceIndicatorID" />
				</xsl:call-template>
			</xsl:when>
		</xsl:choose>
		<br />
		<xsl:if test="dri:instance or dri:field/dri:instance">
			<div class="alert" style="margin-top: 10px;">
				<xsl:call-template name="fieldIterator">
					<xsl:with-param name="position">1</xsl:with-param>
				</xsl:call-template>
				<!-- Conclude with a DELETE button if the delete operation is specified. 
					This allows removing one or more values stored for this field. -->
				<xsl:if
					test="contains(dri:params/@operations,'delete') and (dri:instance or dri:field/dri:instance)">
					<!-- Delete buttons should be named "submit_[field]_delete" so that 
						we can ignore errors from required fields when simply removing values -->
					<input type="submit" value="Remove selected" name="{concat('submit_',@n,'_delete')}"
						class="btn btn-link btn-small" />
				</xsl:if>
				<xsl:for-each select="dri:field">
					<xsl:apply-templates select="dri:instance"
						mode="hiddenInterpreter" />
				</xsl:for-each>
			</div>
		</xsl:if>
	</xsl:template>

	<!-- The iterator is a recursive function that creates a checkbox (to be 
		used in deletion) for each value instance and interprets the value inside. 
		It also creates a hidden field from the raw value contained in the instance. 
		What makes it different from the simpleFieldIterator is that it works with 
		a composite field's components rather than a single field, which requires 
		it to consider several sets of instances. -->
	<xsl:template name="fieldIterator">
		<xsl:param name="position" />
		<!-- add authority value for this instance -->
		<xsl:if
			test="dri:instance[position()=$position]/dri:value[@type='authority']">
			<xsl:call-template name="authorityInputFields">
				<xsl:with-param name="name" select="@n" />
				<xsl:with-param name="position" select="$position" />
				<xsl:with-param name="authValue"
					select="dri:instance[position()=$position]/dri:value[@type='authority']/text()" />
				<xsl:with-param name="confValue"
					select="dri:instance[position()=$position]/dri:value[@type='authority']/@confidence" />
			</xsl:call-template>
		</xsl:if>
		<xsl:choose>
			<!-- First check to see if the composite itself has a non-empty instance 
				value in that position. In that case there is no need to go into the individual 
				fields. -->
			<xsl:when
				test="count(dri:instance[position()=$position]/dri:value[@type != 'authority'])">
				<input type="checkbox" value="{concat(@n,'_',$position)}"
					name="{concat(@n,'_selected')}" />
				<xsl:apply-templates select="dri:instance[position()=$position]"
					mode="interpreted" />
				<xsl:call-template name="authorityConfidenceIcon">
					<xsl:with-param name="confidence"
						select="dri:instance[position()=$position]/dri:value[@type='authority']/@confidence" />
				</xsl:call-template>
				<br />
				<xsl:call-template name="fieldIterator">
					<xsl:with-param name="position">
						<xsl:value-of select="$position + 1" />
					</xsl:with-param>
				</xsl:call-template>
			</xsl:when>
			<!-- Otherwise, build the string from the component fields -->
			<xsl:when test="dri:field/dri:instance[position()=$position]">
				<input type="checkbox" value="{concat(@n,'_',$position)}"
					name="{concat(@n,'_selected')}" />
				<xsl:apply-templates select="dri:field" mode="compositeField">
					<xsl:with-param name="position" select="$position" />
				</xsl:apply-templates>
				<br />
				<xsl:call-template name="fieldIterator">
					<xsl:with-param name="position">
						<xsl:value-of select="$position + 1" />
					</xsl:with-param>
				</xsl:call-template>
			</xsl:when>
		</xsl:choose>
	</xsl:template>

	<xsl:template match="dri:field[@type='text' or @type='textarea']" mode="compositeField">
		<xsl:param name="position">1</xsl:param>
		<xsl:if test="not(position()=1)">
			<xsl:text>, </xsl:text>
		</xsl:if>
		<!--<input type="hidden" name="{concat(@n,'_',$position)}" value="{dri:instance[position()=$position]/dri:value[@type='raw']}"/> -->
		<xsl:choose>
			<xsl:when
				test="dri:instance[position()=$position]/dri:value[@type='interpreted']">
				<span class="ds-interpreted-field">
					<xsl:apply-templates
						select="dri:instance[position()=$position]/dri:value[@type='interpreted']"
						mode="interpreted" />
				</span>
			</xsl:when>
			<xsl:when test="dri:instance[position()=$position]/dri:value[@type='raw']">
				<span class="ds-interpreted-field">
					<xsl:apply-templates
						select="dri:instance[position()=$position]/dri:value[@type='raw']"
						mode="interpreted" />
				</span>
			</xsl:when>
			<xsl:when
				test="dri:instance[position()=$position]/dri:value[@type='default']">
				<span class="ds-interpreted-field">
					<xsl:apply-templates
						select="dri:instance[position()=$position]/dri:value[@type='default']"
						mode="interpreted" />
				</span>
			</xsl:when>
			<xsl:otherwise>
				<span class="ds-interpreted-field">No value submitted.</span>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>


	<xsl:template match="dri:field[@type='select']" mode="compositeField">
		<xsl:param name="position">1</xsl:param>
		<xsl:if test="not(position()=1)">
			<xsl:text>, </xsl:text>
		</xsl:if>
		<xsl:for-each
			select="dri:instance[position()=$position]/dri:value[@type='option']">
			<!--<input type="hidden" name="{concat(@n,'_',$position)}" value="{../../dri:option[@returnValue 
				= current()/@option]}"/> -->
		</xsl:for-each>
		<xsl:choose>
			<xsl:when
				test="dri:instance[position()=$position]/dri:value[@type='interpreted']">
				<span class="ds-interpreted-field">
					<xsl:apply-templates
						select="dri:instance[position()=$position]/dri:value[@type='interpreted']"
						mode="interpreted" />
				</span>
			</xsl:when>
			<xsl:when
				test="dri:instance[position()=$position]/dri:value[@type='option']">
				<span class="ds-interpreted-field">
					<xsl:for-each
						select="dri:instance[position()=$position]/dri:value[@type='option']">
						<xsl:if test="position()=1">
							<xsl:text>(</xsl:text>
						</xsl:if>
						<xsl:value-of select="../../dri:option[@returnValue = current()/@option]" />
						<xsl:if test="position()=last()">
							<xsl:text>)</xsl:text>
						</xsl:if>
						<xsl:if test="not(position()=last())">
							<xsl:text>, </xsl:text>
						</xsl:if>
					</xsl:for-each>
				</span>
			</xsl:when>
			<xsl:otherwise>
				<span class="ds-interpreted-field">No value submitted.</span>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!-- TODO: make this work? Maybe checkboxes and radio buttons should not 
		be instanced... -->
	<xsl:template match="dri:field[@type='checkbox' or @type='radio']"
		mode="compositeField">
		<xsl:param name="position">1</xsl:param>
		<xsl:if test="not(position()=1)">
			<xsl:text>, </xsl:text>
		</xsl:if>
		<span class="ds-interpreted-field">Checkbox</span>
	</xsl:template>










	<xsl:template match="dri:field[@type='select']/dri:instance"
		mode="interpreted">
		<span class="ds-interpreted-field">
			<xsl:for-each select="dri:value[@type='option']">
				<!--<input type="hidden" name="{concat(../@n,'_',position())}" value="{../../dri:option[@returnValue 
					= current()/@option]}"/> -->
				<xsl:if test="position()=1">
					<xsl:text>(</xsl:text>
				</xsl:if>
				<xsl:value-of select="../../dri:option[@returnValue = current()/@option]" />
				<xsl:if test="position()=last()">
					<xsl:text>)</xsl:text>
				</xsl:if>
				<xsl:if test="not(position()=last())">
					<xsl:text>, </xsl:text>
				</xsl:if>
			</xsl:for-each>
		</span>
	</xsl:template>


	<xsl:template match="dri:instance" mode="interpreted">
		<!--<input type="hidden" name="{concat(../@n,'_',position())}" value="dri:value[@type='raw']"/> -->
		<xsl:choose>
			<xsl:when test="dri:value[@type='interpreted']">
				<span class="ds-interpreted-field">
					<xsl:apply-templates select="dri:value[@type='interpreted']"
						mode="interpreted" />
				</span>
			</xsl:when>
			<xsl:when test="dri:value[@type='raw']">
				<span class="ds-interpreted-field">
					<xsl:apply-templates select="dri:value[@type='raw']"
						mode="interpreted" />
				</span>
			</xsl:when>
			<xsl:when test="dri:value[@type='default']">
				<span class="ds-interpreted-field">
					<xsl:apply-templates select="dri:value[@type='default']"
						mode="interpreted" />
				</span>
			</xsl:when>
			<xsl:otherwise>
				<span class="ds-interpreted-field">No value submitted.</span>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>





	<xsl:template match="dri:value" mode="interpreted">
		<xsl:apply-templates />
	</xsl:template>

	<!-- <xsl:template match="dri:field"> Possible child elements: params(one), 
		help(zero or one), error(zero or one), value(any), field(one or more – only 
		with the composite type) Possible attributes: @n, @id, @rend @disabled @required 
		@type = button: A button input control that when activated by the user will 
		submit the form, including all the fields, back to the server for processing. 
		checkbox: A boolean input control which may be toggled by the user. A checkbox 
		may have several fields which share the same name and each of those fields 
		may be toggled independently. This is distinct from a radio button where 
		only one field may be toggled. file: An input control that allows the user 
		to select files to be submitted with the form. Note that a form which uses 
		a file field must use the multipart method. hidden: An input control that 
		is not rendered on the screen and hidden from the user. password: A single-line 
		text input control where the input text is rendered in such a way as to hide 
		the characters from the user. radio: A boolean input control which may be 
		toggled by the user. Multiple radio button fields may share the same name. 
		When this occurs only one field may be selected to be true. This is distinct 
		from a checkbox where multiple fields may be toggled. select: A menu input 
		control which allows the user to select from a list of available options. 
		text: A single-line text input control. textarea: A multi-line text input 
		control. composite: A composite input control combines several input controls 
		into a single field. The only fields that may be combined together are: checkbox, 
		password, select, text, and textarea. When fields are combined together they 
		can posses multiple combined values. </xsl:template> -->



	<!-- The handling of component fields, that is fields that are part of a 
		composite field type -->
	<xsl:template match="dri:field" mode="compositeComponent">
		<xsl:choose>
			<xsl:when test="@type = 'checkbox'  or @type='radio'">
				<xsl:if test="dri:label">
					<xsl:apply-templates select="dri:label" mode="compositeComponent" />
				</xsl:if>			
				<xsl:apply-templates select="." mode="normalField" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:choose>
					<xsl:when test="dri:label">
						<div>
							<xsl:apply-templates select="dri:label" mode="compositeComponent" />						
							<xsl:apply-templates select="." mode="normalField" />
						</div>
					</xsl:when>
					<xsl:otherwise>
						<span>
							<xsl:apply-templates select="." mode="normalField" />
						</span>					
					</xsl:otherwise>
				</xsl:choose>			
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template match="dri:error" mode="compositeComponent">
		<xsl:apply-templates select="." mode="error" />
	</xsl:template>

	<xsl:template match="dri:help" mode="compositeComponent">
		<div class="text-info" style="margin-bottom: 0px; margin-top: 5px;">
			<i class="fa fa-info-circle fa-lg">&#160;</i>
			<xsl:apply-templates />
		</div>
	</xsl:template>



	<!-- The handling of the field element is more complex. At the moment, the 
		handling of input fields in the DRI schema is very similar to HTML, utilizing 
		the same controlled vocabulary in most cases. This makes converting DRI fields 
		to HTML inputs a straightforward, if a bit verbose, task. We are currently 
		looking at other ways of encoding forms, so this may change in the future. -->
	<!-- The simple field case... not part of a complex field and does not contain 
		instance values -->
	<xsl:template match="dri:field">
		<xsl:apply-templates select="dri:error" mode="error" />	
		<xsl:apply-templates select="." mode="normalField" />
		<xsl:if test="not(@type='composite') and ancestor::dri:list[@type='form']">
			<!-- <xsl:if test="not(@type='checkbox') and not(@type='radio') and not(@type='button')"> 
				<br/> </xsl:if> -->
			<xsl:apply-templates select="dri:help" mode="help" />
		</xsl:if>
	</xsl:template>

	<xsl:template match="dri:field" mode="normalField">
		<xsl:variable name="confidenceIndicatorID" select="concat(translate(@id,'.','_'),'_confidence_indicator')" />
		<xsl:choose>
			<!-- TODO: this has changed drammatically (see form3.xml) -->
			<xsl:when test="@type= 'select'">
				<select>
					<xsl:call-template name="fieldAttributes" />
					<xsl:apply-templates />
				</select>
			</xsl:when>
			<xsl:when test="@type= 'textarea'">
				<textarea>
					<xsl:call-template name="fieldAttributes" />
					<xsl:attribute name="onkeydown">event.cancelBubble=true;</xsl:attribute>
					<!-- if the cols and rows attributes are not defined we need to call 
						the tempaltes for them since they are required attributes in strict xhtml -->
					<xsl:choose>
						<xsl:when test="not(./dri:params[@cols])">
							<xsl:call-template name="textAreaCols" />
						</xsl:when>
					</xsl:choose>
					<xsl:choose>
						<xsl:when test="not(./dri:params[@rows])">
							<xsl:call-template name="textAreaRows" />
						</xsl:when>
					</xsl:choose>

					<xsl:apply-templates />
					<xsl:choose>
						<xsl:when test="./dri:value[@type='raw']">
							<xsl:copy-of select="./dri:value[@type='raw']/node()" />
						</xsl:when>
						<xsl:otherwise>
							<xsl:copy-of select="./dri:value[@type='default']/node()" />
						</xsl:otherwise>
					</xsl:choose>
					<xsl:if test="string-length(./dri:value) &lt; 1">
						<i18n:text>xmlui.dri2xhtml.default.textarea.value</i18n:text>
					</xsl:if>
				</textarea>

				<!-- add place to store authority value -->
				<xsl:if test="dri:params/@authorityControlled">
					<xsl:variable name="confidence">
						<xsl:if test="./dri:value[@type='authority']">
							<xsl:value-of select="./dri:value[@type='authority']/@confidence" />
						</xsl:if>
					</xsl:variable>
					<!-- add authority confidence widget -->
					<xsl:call-template name="authorityConfidenceIcon">
						<xsl:with-param name="confidence" select="$confidence" />
						<xsl:with-param name="id" select="$confidenceIndicatorID" />
					</xsl:call-template>
					<xsl:call-template name="authorityInputFields">
						<xsl:with-param name="name" select="@n" />
						<xsl:with-param name="id" select="@id" />
						<xsl:with-param name="authValue"
							select="dri:value[@type='authority']/text()" />
						<xsl:with-param name="confValue"
							select="dri:value[@type='authority']/@confidence" />
						<xsl:with-param name="confIndicatorID" select="$confidenceIndicatorID" />
						<xsl:with-param name="unlockButton"
							select="dri:value[@type='authority']/dri:field[@rend='ds-authority-lock']/@n" />
						<xsl:with-param name="unlockHelp"
							select="dri:value[@type='authority']/dri:field[@rend='ds-authority-lock']/dri:help" />
					</xsl:call-template>
				</xsl:if>
				<!-- add choice mechanisms -->
				<xsl:choose>
					<xsl:when test="dri:params/@choicesPresentation = 'suggest'">
						<xsl:call-template name="addAuthorityAutocomplete">
							<xsl:with-param name="confidenceIndicatorID"
								select="$confidenceIndicatorID" />
							<xsl:with-param name="confidenceName">
								<xsl:value-of select="concat(@n,'_confidence')" />
							</xsl:with-param>
						</xsl:call-template>
					</xsl:when>
					<xsl:when test="dri:params/@choicesPresentation = 'lookup'">
						<xsl:call-template name="addLookupButton">
							<xsl:with-param name="isName" select="'false'" />
							<xsl:with-param name="confIndicator" select="$confidenceIndicatorID" />
						</xsl:call-template>
					</xsl:when>
				</xsl:choose>
			</xsl:when>

			<!-- This is changing drammatically -->
			<xsl:when test="@type= 'checkbox' or @type= 'radio'">
				<div>
					<xsl:call-template name="standardAttributes">
						<xsl:with-param name="class">
							<xsl:text>form-group </xsl:text>
							<xsl:if test="dri:error">
								<xsl:text>error</xsl:text>
							</xsl:if>
						</xsl:with-param>
					</xsl:call-template>
					<!--xsl:if test="dri:label">
							<xsl:apply-templates select="dri:label" mode="compositeComponent" />
					</xsl:if-->
					<xsl:apply-templates />
				</div>
			</xsl:when>
			<!-- <input> <xsl:call-template name="fieldAttributes"/> <xsl:if test="dri:value[@checked='yes']"> 
				<xsl:attribute name="checked">checked</xsl:attribute> </xsl:if> <xsl:apply-templates/> 
				</input> -->
			<xsl:when test="@type= 'composite'">
				<!-- TODO: add error and help stuff on top of the composite -->
				<span class="ds-composite-field">
					<xsl:apply-templates select="dri:field" mode="compositeComponent" />
				</span>
				<xsl:apply-templates select="dri:field/dri:error" mode="compositeComponent" />
				<xsl:apply-templates select="dri:error" mode="compositeComponent" />
				<xsl:apply-templates select="dri:field/dri:help" mode="compositeComponent" />
				<!--<xsl:apply-templates select="dri:help" mode="compositeComponent"/> -->
			</xsl:when>
			<!-- text, password, file, and hidden types are handled the same. Buttons: 
				added the xsl:if check which will override the type attribute button with 
				the value 'submit'. No reset buttons for now... -->
			<xsl:otherwise>
				<input>
					<!-- ufal adding ISO 639-3 support -->
					<xsl:if test="@n = 'dc_language_iso'">
						<xsl:attribute name="data-provide">typeahead</xsl:attribute>
						<xsl:attribute name="data-item">5</xsl:attribute>
						<xsl:attribute name="autocomplete">off</xsl:attribute>
					</xsl:if>				
					<xsl:call-template name="fieldAttributes" />
					<xsl:choose>
					<xsl:when test="@type='button'">
						<xsl:attribute name="class">btn btn-repository</xsl:attribute>
						<xsl:attribute name="type">submit</xsl:attribute>
					</xsl:when>
					<xsl:otherwise>
						<xsl:attribute name="class">form-control <xsl:value-of select="./@rend" /></xsl:attribute>
					</xsl:otherwise>
					</xsl:choose>
					<xsl:attribute name="value">
                                <xsl:choose>
                                    <xsl:when test="./dri:value[@type='raw']">
                                        <xsl:apply-templates select="./dri:value[@type='raw']/node()" />
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:value-of select="./dri:value[@type='default']" />
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:attribute>
					<xsl:if test="dri:value/i18n:text">
						<xsl:attribute name="i18n:attr">value</xsl:attribute>
					</xsl:if>
					<xsl:apply-templates />
				</input>

				<xsl:variable name="confIndicatorID"
					select="concat(@id,'_confidence_indicator')" />
				<xsl:if test="dri:params/@authorityControlled">
					<xsl:variable name="confidence">
						<xsl:if test="./dri:value[@type='authority']">
							<xsl:value-of select="./dri:value[@type='authority']/@confidence" />
						</xsl:if>
					</xsl:variable>
					<!-- add authority confidence widget -->
					<xsl:call-template name="authorityConfidenceIcon">
						<xsl:with-param name="confidence" select="$confidence" />
						<xsl:with-param name="id" select="$confidenceIndicatorID" />
					</xsl:call-template>
					<xsl:call-template name="authorityInputFields">
						<xsl:with-param name="name" select="@n" />
						<xsl:with-param name="id" select="@id" />
						<xsl:with-param name="authValue"
							select="dri:value[@type='authority']/text()" />
						<xsl:with-param name="confValue"
							select="dri:value[@type='authority']/@confidence" />
					</xsl:call-template>
				</xsl:if>
				<xsl:choose>
					<xsl:when test="dri:params/@choicesPresentation = 'suggest'">
						<xsl:call-template name="addAuthorityAutocomplete">
							<xsl:with-param name="confidenceIndicatorID"
								select="$confidenceIndicatorID" />
							<xsl:with-param name="confidenceName">
								<xsl:value-of select="concat(@n,'_confidence')" />
							</xsl:with-param>
						</xsl:call-template>
					</xsl:when>
					<xsl:when test="dri:params/@choicesPresentation = 'lookup'">
						<xsl:call-template name="addLookupButton">
							<xsl:with-param name="isName" select="'false'" />
							<xsl:with-param name="confIndicator" select="$confidenceIndicatorID" />
						</xsl:call-template>
					</xsl:when>
				</xsl:choose>
				<!-- UFAL - dragNdrop support if specified for a file input button -->
				<xsl:call-template name="dragNdrop" />



			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!-- A set of standard attributes common to all fields -->
	<xsl:template name="fieldAttributes">
		<xsl:call-template name="standardAttributes">
			<xsl:with-param name="class">
				<xsl:if test="dri:error">
					<xsl:text>error </xsl:text>
				</xsl:if>
				<xsl:if test="@type = 'textarea' or @type='select'">
					<xsl:text>form-control </xsl:text>
				</xsl:if>				
			</xsl:with-param>
		</xsl:call-template>
		<xsl:if test="@disabled='yes'">
			<xsl:choose>
				<xsl:when test="@type = 'button'">
					<xsl:attribute name="disabled">disabled</xsl:attribute>
				</xsl:when>
				<xsl:otherwise>
					<xsl:attribute name="readonly">yes</xsl:attribute>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:if>
		<xsl:if test="@type != 'checkbox' and @type != 'radio' ">
			<xsl:attribute name="name"><xsl:value-of select="@n" /></xsl:attribute>
		</xsl:if>
		<xsl:if
			test="@type != 'select' and @type != 'textarea' and @type != 'checkbox' and @type != 'radio' ">
			<xsl:attribute name="type"><xsl:value-of select="@type" /></xsl:attribute>
		</xsl:if>
		<xsl:if test="@type= 'textarea'">
			<xsl:attribute name="onfocus">javascript:tFocus(this);</xsl:attribute>
		</xsl:if>
        <xsl:if test="dri:params/@placeholder">
            <xsl:attribute name="placeholder"><xsl:value-of select="dri:params/@placeholder"/></xsl:attribute>
        </xsl:if>
	</xsl:template>

	<!-- Since the field element contains only the type attribute, all other 
		attributes commonly associated with input fields are stored on the params 
		element. Rather than parse the attributes directly, this template generates 
		a call to attribute templates, something that is not done in XSL by default. 
		The templates for the attributes can be found further down. -->
	<xsl:template match="dri:params">
		<xsl:apply-templates select="@*" />
	</xsl:template>


	<xsl:template match="dri:field[@type='select']/dri:option">
		<option>
			<xsl:attribute name="value"><xsl:value-of select="@returnValue" /></xsl:attribute>
			<xsl:if
				test="../dri:value[@type='option'][@option = current()/@returnValue]">
				<xsl:attribute name="selected">selected</xsl:attribute>
			</xsl:if>
			<xsl:apply-templates />
		</option>
	</xsl:template>

	<xsl:template match="dri:field[@type='checkbox' or @type='radio']/dri:option">
		<div>
			<input>
				<xsl:attribute name="name"><xsl:value-of select="../@n" /></xsl:attribute>
				<xsl:attribute name="type"><xsl:value-of select="../@type" /></xsl:attribute>
				<xsl:attribute name="value"><xsl:value-of select="@returnValue" /></xsl:attribute>
				<xsl:if
					test="../dri:value[@type='option'][@option = current()/@returnValue]">
					<xsl:attribute name="checked">checked</xsl:attribute>
				</xsl:if>
				<xsl:if test="../@disabled='yes'">
					<xsl:attribute name="readonly">yes</xsl:attribute>
				</xsl:if>
			</input>
			<xsl:apply-templates />
		</div>
	</xsl:template>



	<!-- A special case for the value element under field of type 'select'. 
		Instead of being used to create the value attribute of an HTML input tag, 
		these are used to create selection options. <xsl:template match="dri:field[@type='select']/dri:value" 
		priority="2"> <option> <xsl:attribute name="value"><xsl:value-of select="@optionValue"/></xsl:attribute> 
		<xsl:if test="@optionSelected = 'yes'"> <xsl:attribute name="selected">selected</xsl:attribute> 
		</xsl:if> <xsl:apply-templates /> </option> </xsl:template> -->

	<!-- In general cases the value of this element is used directly, so the 
		template does nothing. -->
	<xsl:template match="dri:value" priority="1">
	</xsl:template>

	<!-- The field label is usually invoked directly by a higher level tag, 
		so this template does nothing. -->
	<xsl:template match="dri:field/dri:label" priority="2">
	</xsl:template>

	<xsl:template match="dri:field/dri:label" mode="compositeComponent">
		<label class="control-label">
			<xsl:attribute name="for">
				<xsl:value-of select="translate(../@id, '.' ,'_')" />
			</xsl:attribute>
			<xsl:apply-templates />
		</label>
	</xsl:template>

	<!-- The error field handling -->
	<xsl:template match="dri:error">
		<xsl:attribute name="title"><xsl:value-of select="." /></xsl:attribute>
		<xsl:if test="i18n:text">
			<xsl:attribute name="i18n:attr">title</xsl:attribute>
		</xsl:if>
	</xsl:template>

	<xsl:template match="dri:error" mode="error">
		<div class="text-error">
			*
			<xsl:apply-templates />
		</div>
	</xsl:template>



	<!-- Help elementns are turning into tooltips. There might be a better way 
		tot do this -->
	<xsl:template match="dri:help">
			<xsl:attribute name="title"><xsl:value-of select="." /></xsl:attribute>
			<xsl:if test="i18n:text">
				<xsl:attribute name="i18n:attr">title</xsl:attribute>
			</xsl:if>
	</xsl:template>

	<xsl:template match="dri:help" mode="help">
		<!--Only create the <span> if there is content in the <dri:help> node -->
		<xsl:if test="./text() or ./node()">
			<div class="text-info" style="margin-bottom: 0px; margin-top: 5px;">
			<i class="fa fa-info-circle fa-lg">&#160;</i>
				<xsl:apply-templates />
			</div>
		</xsl:if>
	</xsl:template>

	<!-- UFAL If page metadata specify that they want to use drag and drop then 
		include it as a button. This relies on js/css added in buildHeader. jmisutka 
		2011/04/07 -->
	<xsl:template name="dragNdrop">
		<xsl:if
			test="@type= 'file' and /dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='include-library'][@qualifier='dragNdrop']">
			<span id="file_upload">
				<div id="file_upload_container" style="display:none">
					<script type="text/javascript">
					<![CDATA[
						var lindat_upload_file_alert_max_file_size = ]]><xsl:value-of select="$lr.upload.file.alert.max.file.size" /><![CDATA[; 						
						var ufal_help_mail = "]]><xsl:value-of select="$lr.help.mail" /><![CDATA[";
					]]>
					</script>
				</div>
			</span>
		</xsl:if>
	</xsl:template>

<!-- Special Handling for submissions -->

	<xsl:template match="dri:item[dri:field[@id='aspect.submission.StepTransformer.field.dc_type']]" priority="10">
                <div>
                        <xsl:attribute name='class'>
                                <xsl:if test='dri:field/dri:error'>
                                        alert alert-error
                                </xsl:if>
                        </xsl:attribute>
                        <xsl:if test='dri:field/dri:error'>
                                <div style="margin-bottom: 20px">
                                        <xsl:apply-templates select="dri:field/dri:error/node()" />
                                </div>
                        </xsl:if>
                        <div class="thumbnails clearfix">
                        <xsl:variable name='id'>
                                <xsl:value-of select="translate(dri:field/@id, '.', '_')" />
                        </xsl:variable>
                        <xsl:for-each select="dri:field/dri:option[not(@returnValue='')]">
	                        <a href="#" class="col-sm-3 thumbnail text-center">
	                        		<xsl:attribute name="id">type_<xsl:value-of select="@returnValue" /></xsl:attribute>
	                                <xsl:attribute name="onclick">$('#<xsl:copy-of select="$id" />').val('<xsl:value-of select="@returnValue"/>');return false;</xsl:attribute>
	                        <img style="width: 64px; height: 64px">
	                                <xsl:attribute name="src"><xsl:copy-of select="$theme-path" />/../UFALHome/lib/images/<xsl:value-of select="@returnValue" />.png</xsl:attribute>
	                        </img>
	                        <p class="text-center"><xsl:apply-templates select="node()" /></p>
	                        </a>
                        </xsl:for-each>
                        </div>
                        <div class="hidden">
                                <xsl:apply-templates select="dri:field" />
                        </div>
                        <div class="alert col-xs-12">
                                <i class="fa fa-info-circle fa-4x pull-left">&#160;</i>
                                <xsl:apply-templates select="dri:field/dri:help/node()" />
                        </div>
                </div>
        </xsl:template>
	
        <xsl:template match="dri:list[starts-with(@n, 'accordion')]" priority="10">
                <div>
                        <xsl:call-template name="standardAttributes">
                                <xsl:with-param name="class">accordion </xsl:with-param>
                        </xsl:call-template>
                        <xsl:apply-templates />
                </div>
        </xsl:template>

        <xsl:template match="dri:list[starts-with(@n, 'accordion-group')]" priority="10">
                <div>
                        <xsl:call-template name="standardAttributes" >
                                <xsl:with-param name="class">accordion-group </xsl:with-param>
                        </xsl:call-template>
                        <xsl:apply-templates />
                </div>
        </xsl:template>
        
        	
        <xsl:template match="dri:item[starts-with(@n, 'accordion-heading')]" priority="10">
                <div class="accordion-heading">
                        <a>
                                <xsl:attribute name="href">#<xsl:value-of select="translate(../dri:list[starts-with(@n, 'accordion-body')]/@id, '.', '_')" /></xsl:attribute>
                                <xsl:attribute name="data-toggle">collapse</xsl:attribute>
                                <xsl:if test="../../../dri:list[starts-with(@n, 'accordion')]">
                                        <xsl:attribute name="data-parent">#<xsl:value-of select="translate(../../../dri:list[starts-with(@n, 'accordion')]/@id, '.', '_')" /></xsl:attribute>
                                </xsl:if>
                                <xsl:apply-templates select="./node()" />
                        </a>
                </div>
        </xsl:template>

        <xsl:template match="dri:list[starts-with(@n, 'accordion-body')]" priority="10">
                <div>
                        <xsl:call-template name="standardAttributes">
                                <xsl:with-param name="class">accordion-body collapse </xsl:with-param>
                        </xsl:call-template>
                        <div class="accordion-inner">
                                                <xsl:apply-templates />
                        </div>
                </div>
        </xsl:template>
        
        <xsl:template match="dri:item[@id='aspect.submission.StepTransformer.item.license-decision']" priority="10">
        	<xsl:if test=".//dri:error">
        		<div class="alert alert-danger">
        			<xsl:apply-templates select=".//dri:error" mode="error" />
        		</div>
        	</xsl:if>
        	<xsl:apply-templates select="./*" />
        </xsl:template>
        
        <xsl:template match="dri:field[@id='aspect.submission.StepTransformer.field.decision']" priority="10">        	
                <input type="checkbox" data-toggle="toggle" data-on="xmlui.UFAL.forms.licenseStep.accepted" data-off="xmlui.UFAL.forms.licenseStep.click" data-onstyle="success" data-offstyle="danger" data-width="130" data-height="30" i18n:attr="data-on data-off">
                        <xsl:call-template name="standardAttributes" />
                        <xsl:attribute name="name"><xsl:value-of select="@n"/></xsl:attribute>
                        <xsl:attribute name="value"><xsl:value-of select="dri:option/@returnValue"/></xsl:attribute>
                        <xsl:if test="dri:value[@option='accept']">
                                <xsl:attribute name="checked" />
                        </xsl:if>
                </input>
        </xsl:template>
        
        <xsl:template match="dri:list[@id='aspect.submission.StepTransformer.list.license-list']" priority="10">
        	<ul>
        		<xsl:call-template name="standardAttributes" />
        		<xsl:for-each select="dri:item">
		        	<li>
		        		<a>
		        			<xsl:attribute name="name"><xsl:value-of select="dri:xref/@n"/></xsl:attribute>
		        			<xsl:attribute name="target"><xsl:value-of select="_blank"/></xsl:attribute>
		        			<xsl:attribute name="href"><xsl:value-of select="dri:xref/@target"/></xsl:attribute>
		        			<xsl:attribute name="license-label"><xsl:value-of select="dri:hi/@rend"/></xsl:attribute>
		        			<xsl:attribute name="license-label-text"><xsl:value-of select="dri:hi/node()"/></xsl:attribute>
		        			<xsl:value-of select="dri:xref/node()" />
		        		</a>
		        	</li>
	        	</xsl:for-each>
        	</ul>
        </xsl:template>
        
</xsl:stylesheet>
