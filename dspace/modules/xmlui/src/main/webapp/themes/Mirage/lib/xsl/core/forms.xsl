<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->
<!--
    Templates to cover the forms and forms fields.

    Author: art.lowel at atmire.com
    Author: lieven.droogmans at atmire.com
    Author: ben at atmire.com
    Author: Alexey Maslov

-->

<xsl:stylesheet xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
	xmlns:dri="http://di.tamu.edu/DRI/1.0/"
	xmlns:mets="http://www.loc.gov/METS/"
	xmlns:xlink="http://www.w3.org/TR/xlink/"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
	xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
	xmlns:xhtml="http://www.w3.org/1999/xhtml"
	xmlns:mods="http://www.loc.gov/mods/v3"
	xmlns:dc="http://purl.org/dc/elements/1.1/"
	xmlns:xalan="http://xml.apache.org/xalan"
	xmlns="http://www.w3.org/1999/xhtml"
	exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc xalan">

    <xsl:output indent="yes"/>

    <!--always give the last item the class 'last'-->
    <xsl:template match="dri:list[@type='form']/dri:item" priority="3">
        <li>
                <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">
                    <xsl:text>ds-form-item </xsl:text>
                <xsl:choose>
                    <!-- Makes sure that the dark always falls on the last item -->
                    <xsl:when test="count(../dri:item) mod 2 = 0">
                        <xsl:if test="count(../dri:item) > 3">
                            <xsl:if test="(count(preceding-sibling::dri:item) mod 2 = 0)">even </xsl:if>
                            <xsl:if test="(count(preceding-sibling::dri:item) mod 2 = 1)">odd </xsl:if>
                        </xsl:if>
                    </xsl:when>
                    <xsl:when test="count(../dri:item) mod 2 = 1">
                        <xsl:if test="count(../dri:item) > 3">
                            <xsl:if test="(count(preceding-sibling::dri:item) mod 2 = 1)">even </xsl:if>
                            <xsl:if test="(count(preceding-sibling::dri:item) mod 2 = 0)">odd </xsl:if>
                        </xsl:if>
                    </xsl:when>
                </xsl:choose>
                <!-- The last row is special-->
                <xsl:if test="position()=last()">last </xsl:if>
                <!-- The row is also tagged specially if it contains another "form" list -->
                <xsl:if test="dri:list[@type='form']">sublist </xsl:if>
                </xsl:with-param>
            </xsl:call-template>

            <xsl:choose>
                <xsl:when test="dri:field/dri:params[@choicesPresentation='authorLookup']">
                    <xsl:call-template name="pick-label"/>
                    <xsl:apply-templates mode="authorFormComposite"/>
                </xsl:when>
                <xsl:when test="dri:field[@type='composite']">
                    <xsl:call-template name="pick-label"/>
                    <xsl:apply-templates mode="formComposite"/>
                </xsl:when>
                <xsl:when test="dri:list[@type='form']">
                    <xsl:apply-templates />
                </xsl:when>
                <xsl:when test="dri:field[@n='dc_contributor_author']
                             or dri:field[@n='dc_coverage_spatial']
                             or dri:field[@n='dc_coverage_temporal']
                             or dri:field[@n='dc_subject']
                             or dri:field[@n='dwc_ScientificName']
                ">
                    <xsl:call-template name="pick-label"/>
                    <xsl:apply-templates mode="formText"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:call-template name="pick-label"/>
                    <div class="ds-form-content">
                        <xsl:apply-templates />
                        <!-- special name used in submission UI review page -->
                        <xsl:if test="@n = 'submit-review-field-with-authority'">
                            <xsl:call-template name="authorityConfidenceIcon">
                                <xsl:with-param name="confidence" select="substring-after(./@rend, 'cf-')"/>
                            </xsl:call-template>
                        </xsl:if>
                    </div>
                </xsl:otherwise>
            </xsl:choose>
        </li>
    </xsl:template>

    <!-- NON-instance composite fields (i.e. not repeatable) -->
    <xsl:template match="dri:field[@type='composite']" mode="formComposite">
        <xsl:variable name="confidenceIndicatorID" select="concat(translate(@id,'.','_'),'_confidence_indicator')"/>
        <div class="ds-form-content">
            <xsl:apply-templates select="dri:help" mode="compositeComponent"/>
            <xsl:apply-templates select="dri:field" mode="compositeComponent"/>

            <xsl:call-template name="authority-display">
                <xsl:with-param name="authValue" select="dri:field/dri:value[@type='authority'][1]"/>
                <xsl:with-param name="confID" select="$confidenceIndicatorID"/>
                <xsl:with-param name="name" select="@n"/>
            </xsl:call-template>

            <xsl:if test="dri:error or dri:field/dri:error">
                <div class="spacer">&#160;</div>
                <xsl:apply-templates select="dri:field/dri:error" mode="compositeComponent"/>
                <xsl:apply-templates select="dri:error" mode="compositeComponent"/>
            </xsl:if>
        </div>
    </xsl:template>

    <!-- Author lookup form -->
    <xsl:template match="dri:field[@type='composite'][dri:field/dri:instance | dri:params/@operations]" mode="authorFormComposite" priority="2">
        <xsl:variable name="confidenceIndicatorID" select="concat(translate(@id,'.','_'),'_confidence_indicator')"/>

        <div class="spacer">&#160;</div>
        <!-- This div handles ORCID lookup-->
        <div class="ds-form-content">
            <xsl:call-template name="addLookupButtonAuthor"/><br/>
            <a class="ds-form-content" href="http://orcid.org" target="_blank">What is ORCID?</a>
        </div>

        <!-- This div handles manual author add-->
        <div class="ds-form-content">
            <!-- display the help message here.-->
            <div class="ds-form-label help">
                <xsl:variable name="help" select="dri:help"/>
                <div class="help-title">
                    <xsl:text>Manually add an author:</xsl:text>
                    <xsl:call-template name="help-hover">
                        <xsl:with-param name="hover" select="string($help)"/>
                    </xsl:call-template>
                </div>
            </div>
            <xsl:apply-templates select="dri:field" mode="compositeComponent"/>
            <xsl:if test="contains(dri:params/@operations,'add')">
                <!-- Add buttons should be named "submit_[field]_add" so that we can ignore errors from required fields when simply adding new values-->
                <input type="submit" i18n:attr="value" value="xmlui.Submission.submit.DescribeStep.add" name="{concat('submit_',@n,'_add')}" class="ds-button-field ds-add-button"/>
            </xsl:if>
            <xsl:call-template name="authority-display">
                <xsl:with-param name="authValue" select="dri:value[@type='authority']"/>
                <xsl:with-param name="confID" select="$confidenceIndicatorID"/>
                <xsl:with-param name="name" select="@n"/>
            </xsl:call-template>
            <xsl:if test="dri:error or dri:field/dri:error">
                <div class="spacer">&#160;</div>
                <xsl:apply-templates select="dri:field/dri:error" mode="compositeComponent"/>
                <xsl:apply-templates select="dri:error" mode="compositeComponent"/>
            </xsl:if>
        </div>

        <!-- This div handles previous entries. -->
        <div class="ds-form-content">
            <xsl:if test="dri:field and dri:field/dri:instance">
                <div class="ds-previous-values">
                    <xsl:call-template name="ds-previous-values-edit-reorder">
                        <xsl:with-param name="type" select="'orcid'"/>
                    </xsl:call-template>
                </div>
            </xsl:if>
        </div>
    </xsl:template>

    <!-- The handling of the special case of instanced composite fields under "form" lists (except author lookup) -->
    <xsl:template match="dri:field[@type='composite'][dri:field/dri:instance | dri:params/@operations]" mode="formComposite" priority="2">
        <xsl:variable name="confidenceIndicatorID" select="concat(translate(@id,'.','_'),'_confidence_indicator')"/>
        <div class="ds-form-content">
            <xsl:apply-templates select="dri:help" mode="compositeComponent"/>
            <xsl:apply-templates select="dri:field" mode="compositeComponent"/>
            <xsl:if test="contains(dri:params/@operations,'add')">
                <!-- Add buttons should be named "submit_[field]_add" so that we can ignore errors from required fields when simply adding new values-->
                <input type="submit" i18n:attr="value" value="xmlui.Submission.submit.DescribeStep.add" name="{concat('submit_',@n,'_add')}" class="ds-button-field ds-add-button"/>
            </xsl:if>
            <xsl:call-template name="create-choices-presentation">
                <xsl:with-param name="confidenceIndicatorID" select="$confidenceIndicatorID"/>
                <xsl:with-param name="presentationType" select="dri:params/@choicesPresentation"/>
            </xsl:call-template>
            <xsl:call-template name="authority-display">
                <xsl:with-param name="authValue" select="dri:value[@type='authority']"/>
                <xsl:with-param name="confID" select="$confidenceIndicatorID"/>
                <xsl:with-param name="name" select="@n"/>
            </xsl:call-template>
            <xsl:if test="dri:error or dri:field/dri:error">
                <div class="spacer">&#160;</div>
                <xsl:apply-templates select="dri:field/dri:error" mode="compositeComponent"/>
                <xsl:apply-templates select="dri:error" mode="compositeComponent"/>
            </xsl:if>
        </div>

        <!-- This div handles previous entries. -->
        <div class="ds-form-content">
            <xsl:if test="dri:instance or dri:field/dri:instance">
                <xsl:call-template name="ds-previous-values">
                    <xsl:with-param name="field-iterator" select="'fieldIterator'"/>
                    <xsl:with-param name="instance-data" select="xalan:nodeset(dri:field/dri:instance)"/>
                </xsl:call-template>
            </xsl:if>
        </div>
    </xsl:template>

    <!-- The handling of the special case of instanced text fields under "form" lists -->
    <xsl:template match="dri:field[@type='text'][dri:field/dri:instance | dri:params/@operations]" mode="formText" priority="2">
        <xsl:variable name="confidenceIndicatorID" select="concat(translate(@id,'.','_'),'_confidence_indicator')"/>
        <div class="ds-form-content">
            <xsl:apply-templates select="dri:help"/>
            <xsl:apply-templates select="." mode="normalField"/>
            <xsl:if test="contains(dri:params/@operations,'add')">
                <!-- Add buttons should be named "submit_[field]_add" so that we can ignore errors from required fields when simply adding new values-->
                <input type="submit" i18n:attr="value" value="xmlui.Submission.submit.DescribeStep.add" name="{concat('submit_',@n,'_add')}" class="ds-button-field ds-add-button"/>
            </xsl:if>
            <xsl:call-template name="create-choices-presentation">
                <xsl:with-param name="confidenceIndicatorID" select="$confidenceIndicatorID"/>
                <xsl:with-param name="presentationType" select="dri:params/@choicesPresentation"/>
            </xsl:call-template>
            <xsl:call-template name="authority-display">
                <xsl:with-param name="authValue" select="dri:value[@type='authority']"/>
                <xsl:with-param name="confID" select="$confidenceIndicatorID"/>
                <xsl:with-param name="name" select="@n"/>
            </xsl:call-template>
            <xsl:if test="dri:error">
                <div class="spacer">&#160;</div>
                <xsl:apply-templates select="dri:error"/>
            </xsl:if>

            <!-- handle previous entries -->
            <xsl:choose>
                <xsl:when test="@n = 'dc_coverage_spatial'
                             or @n = 'dc_coverage_temporal'
                             or @n = 'dc_subject'
                             or @n = 'dwc_ScientificName'
                ">
                    <xsl:if test="dri:instance">
                        <div class="ds-previous-values">
                            <xsl:call-template name="ds-previous-values-edit-reorder"/>
                        </div>
                    </xsl:if>
                </xsl:when>
                <!-- handle non-author-list metadata -->
                <xsl:otherwise>
                    <xsl:if test="dri:instance or dri:field/dri:instance">
                        <xsl:call-template name="ds-previous-values">
                            <xsl:with-param name="field-iterator" select="'fieldIterator'"/>
                            <xsl:with-param name="instance-data" select="xalan:nodeset(dri:field/dri:instance)"/>
                        </xsl:call-template>
                    </xsl:if>
                </xsl:otherwise>
            </xsl:choose>
        </div>
    </xsl:template>

    <xsl:template name="ds-previous-values-edit-reorder">
        <xsl:param name="type" select="''"/>
        <table>
            <!-- Iterate over the dri:instance elements contained in this field. The instances contain
                    stored values as either "interpreted", "raw", or "default" values. -->
            <xsl:call-template name="fieldIterator-edit-reorder">
                <xsl:with-param name="position" select="1"/>
                <xsl:with-param name="type" select="$type"/>
            </xsl:call-template>
        </table>
    </xsl:template>

    <!-- The iterator is a recursive function that creates a checkbox (to be used in deletion) for
        each value instance and interprets the value inside. It also creates a hidden field from the
        raw value contained in the instance.

         What makes it different from the simpleFieldIterator is that it works with a composite field's
        components rather than a single field, which requires it to consider several sets of instances. -->
    <xsl:template name="fieldIterator-edit-reorder">
        <xsl:param name="position"/>
        <xsl:param name="type" select="''"/>
        <xsl:variable name="this-instance" select="dri:instance[position()=$position]"/>
        <xsl:if test="$this-instance">
            <tr class="ds-edit-reorder-input-row">
                <!-- ORDER -->
                <td class="ds-edit-reorder-order-select">
                    <select class="ds-edit-reorder-order-select">
                        <xsl:for-each select="dri:instance">
                            <option>
                                <xsl:if test="$position = position()">
                                    <xsl:attribute name="selected">selected</xsl:attribute>
                                </xsl:if>
                                <xsl:value-of select="position()"/>
                            </option>
                        </xsl:for-each>
                    </select>
                </td>
                <!-- FIELD -->
                <td class="ds-reorder-edit-input-col">
                    <!-- First check to see if the composite itself has a non-empty instance value in that
                    position. In that case there is no need to go into the individual fields. -->
                    <xsl:apply-templates select="$this-instance" mode="interpreted"/>
                    <xsl:if test="dri:params/@authorityControlled = 'yes'">
                        <xsl:call-template name="authorityConfidenceIcon">
                            <xsl:with-param name="confidence" select="dri:instance[$position]/dri:value[@type='authority']/@confidence"/>
                            <xsl:with-param name="id" select="concat(translate(@id,'.','_'),'_confidence_indicator')"/>
                            <xsl:with-param name="authType" select="$type"/>
                        </xsl:call-template>
                    </xsl:if>
                    <xsl:if test="@type='composite'">
                        <xsl:apply-templates select="dri:field/dri:instance[$position]" mode="hiddenInterpreter"/>
                    </xsl:if>
                    <xsl:apply-templates select="dri:instance[$position]" mode="hiddenInterpreter"/>
                </td>
                <!-- EDIT -->
                <td class="ds-edit-button">
                    <xsl:if test="contains(dri:params/@operations,'add')">
                        <input type="submit" i18n:attr="value" value="xmlui.Submission.submit.DescribeStep.edit" name="{concat('submit_',@n,'_',$position,'_edit')}" class="ds-button-field ds-edit-button" >
                            <xsl:if test="dri:params/@authorityControlled = 'yes'">
                                <xsl:if test="dri:instance[$position]/dri:value[@type='authority']/@confidence='ACCEPTED'">
                                    <xsl:attribute name="disabled">disabled</xsl:attribute>
                                </xsl:if>
                            </xsl:if>
                        </input>
                    </xsl:if>
                </td>
                <!-- REMOVE -->
                <td class="ds-delete-button">
                    <xsl:if test="contains(dri:params/@operations,'delete')">
                        <!-- Delete buttons should be named "submit_[field]_delete" so that we can ignore errors from required fields when simply removing values-->
                        <input type="submit" i18n:attr="value" value="xmlui.Submission.submit.DescribeStep.remove" name="{concat('submit_',@n,'_delete')}" class="ds-button-field ds-delete-button" />
                        <input type="hidden"  value="{concat(@n,'_',$position)}" name="{concat(@n,'_selected')}" disabled="disabled"/>
                    </xsl:if>
                </td>
            </tr>
            <!-- recurse to handle subsequent authors -->
            <xsl:call-template name="fieldIterator-edit-reorder">
                <xsl:with-param name="position" select="$position + 1"/>
                <xsl:with-param name="type" select="$type"/>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>

    <!-- The handling of the field element is more complex. At the moment, the handling of input fields in the
        DRI schema is very similar to HTML, utilizing the same controlled vocabulary in most cases. This makes
        converting DRI fields to HTML inputs a straightforward, if a bit verbose, task. We are currently
        looking at other ways of encoding forms, so this may change in the future. -->
    <!-- The simple field case... not part of a complex field and does not contain instance values -->
    <xsl:template match="dri:field">
        <xsl:variable name="test" select="not(@type='composite') and ancestor::dri:list[@type='form']"/>
        <xsl:if test="$test">
            <xsl:apply-templates select="dri:help" mode="help"/>
        </xsl:if>
        <xsl:apply-templates select="." mode="normalField"/>
        <xsl:if test="contains(dri:params/@operations,'add')">
            <!-- Add buttons should be named "submit_[field]_add" so that we can ignore errors from required fields when simply adding new values-->
            <input type="submit" i18n:attr="value" value="xmlui.Submission.submit.DescribeStep.add" name="{concat('submit_',@n,'_add')}" class="ds-button-field ds-add-button">
                <!-- Make invisible if we have choice-lookup popup that provides its own Add. -->
                <xsl:if test="dri:params[@choicesPresentation = 'lookup' or @choicesPresentation = 'authorLookup']">
                    <xsl:attribute name="style">
                        <xsl:text>display:none;</xsl:text>
                    </xsl:attribute>
                </xsl:if>
            </input>
        </xsl:if>
        <xsl:if test="$test">
            <xsl:apply-templates select="dri:error" mode="error"/>
        </xsl:if>
        <xsl:if test="dri:instance">
            <xsl:call-template name="ds-previous-values">
                <xsl:with-param name="field-iterator" select="'simpleFieldIterator'"/>
                <xsl:with-param name="instance-data" select="xalan:nodeset(dri:instance)"/>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>

    <!--modified to differentiate beween selects with and without attribute 'multiple'-->
    <xsl:template name="fieldAttributes">
        <xsl:call-template name="standardAttributes">
            <xsl:with-param name="class">
                <xsl:text>ds-</xsl:text>
                <xsl:value-of select="@type"/>
                <xsl:text>-field </xsl:text>
                <xsl:if test="dri:error">
                    <xsl:text>error </xsl:text>
                </xsl:if>
                <xsl:if test="dri:params/@multiple='yes'">
                    <xsl:text>multiple </xsl:text>
                </xsl:if>
            </xsl:with-param>
        </xsl:call-template>
        <xsl:if test="@disabled='yes'">
            <xsl:attribute name="disabled">disabled</xsl:attribute>
        </xsl:if>
        <xsl:if test="@type != 'checkbox' and @type != 'radio' ">
                <xsl:attribute name="name"><xsl:value-of select="@n"/></xsl:attribute>
        </xsl:if>
        <xsl:if test="@type != 'select' and @type != 'textarea' and @type != 'checkbox' and @type != 'radio' ">
                <xsl:attribute name="type"><xsl:value-of select="@type"/></xsl:attribute>
        </xsl:if>
        <xsl:if test="@type= 'textarea'">
                <xsl:attribute name="onfocus">javascript:tFocus(this);</xsl:attribute>
        </xsl:if>
    </xsl:template>


    <!-- Fieldset (instanced) field stuff, in the case of non-composites -->
    <xsl:template match="dri:field[dri:field/dri:instance | dri:params/@operations]" priority="2">
        <xsl:apply-templates select="dri:help" mode="help"/>
        <!-- Create the first field normally -->
        <xsl:apply-templates select="." mode="normalField"/>
        <!-- Follow it up with an ADD button if the add operation is specified. This allows
            entering more than one value for this field. -->
        <xsl:if test="contains(dri:params/@operations,'add')">
            <!-- Add buttons should be named "submit_[field]_add" so that we can ignore errors from required fields when simply adding new values-->
            <input type="submit" i18n:attr="value" value="xmlui.Submission.submit.DescribeStep.add" name="{concat('submit_',@n,'_add')}" class="ds-button-field ds-add-button">
              <!-- Make invisible if we have choice-lookup popup that provides its own Add. -->
              <xsl:if test="dri:params/@choicesPresentation = 'lookup'">
                <xsl:attribute name="style">
                  <xsl:text>display:none;</xsl:text>
                </xsl:attribute>
               </xsl:if>
                <xsl:if test="dri:params/@choicesPresentation = 'authorLookup'">
                    <xsl:attribute name="style">
                        <xsl:text>display:none;</xsl:text>
                    </xsl:attribute>
                </xsl:if>
           </input>
        </xsl:if>
        <br/>
        <xsl:apply-templates select="dri:error" mode="error"/>
        <xsl:if test="dri:instance">
            <xsl:call-template name="ds-previous-values">
                <xsl:with-param name="field-iterator" select="'simpleFieldIterator'"/>
                <xsl:with-param name="instance-data" select="xalan:nodeset(dri:instance)"/>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>

    <xsl:template name="create-choices-presentation">
        <xsl:param name="confidenceIndicatorID"/>
        <xsl:param name="presentationType"/>
        <xsl:choose>
            <xsl:when test="$presentationType = 'suggest'">
                <xsl:message terminate="yes">
                    <xsl:text>ERROR: Input field with "suggest" (autocomplete) choice behavior is not implemented for Composite (e.g. "name") fields.</xsl:text>
                </xsl:message>
            </xsl:when>
            <xsl:when test="$presentationType = 'lookup'">
                <!-- lookup popup includes its own Add button if necessary. -->
                <xsl:call-template name="addLookupButton">
                    <xsl:with-param name="isName" select="'true'"/>
                    <xsl:with-param name="confIndicator" select="$confidenceIndicatorID"/>
                </xsl:call-template>
            </xsl:when>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="authority-display">
        <xsl:param name="confID"/>
        <xsl:param name="authValue"/>
        <xsl:param name="name"/>
        <xsl:if test="dri:params/@authorityControlled">
            <xsl:call-template name="authorityConfidenceIcon">
                <xsl:with-param name="confidence" select="$authValue/@confidence"/>
                <xsl:with-param name="id" select="$confID"/>
            </xsl:call-template>
            <xsl:call-template name="authorityInputFields">
                <xsl:with-param name="name" select="$name"/>
                <xsl:with-param name="authValue" select="$authValue/text()"/>
                <xsl:with-param name="confValue" select="$authValue/@confidence"/>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>

    <xsl:template name="pick-label">
        <xsl:choose>
            <xsl:when test="dri:field/dri:label">
                <xsl:variable name="help" select="./dri:field/dri:help"/>
                <label class="ds-form-label">
                    <xsl:if test="./dri:field/@id">
                        <xsl:attribute name="for">
                            <xsl:value-of select="translate(./dri:field/@id,'.','_')"/>
                        </xsl:attribute>
                    </xsl:if>
                    <xsl:apply-templates select="dri:field/dri:label" mode="formComposite"/>

                    <!-- display appropriate help text-->
                    <xsl:if test="string-length($help)>0">
                        <xsl:variable name="n" select="string(./dri:field/@n)"/>
                        <xsl:choose>
                            <!-- skip the first help on the forgot-email page -->
                            <xsl:when test="./dri:field[@id='aspect.eperson.StartForgotPassword.field.email']"/>

                            <!-- skip the first help for author lookup form-->
                            <xsl:when test="./dri:field[@id='aspect.submission.StepTransformer.field.dc_contributor_author']"/>

                            <!-- handle helptext on submission forms -->
                            <xsl:when test="ancestor::dri:div[contains(@id,'aspect.submission')]">
                                <xsl:choose>
                                    <!-- put all help text in the hover-over field for these items -->
                                    <xsl:when test="$n = 'dc_subject'
                                                 or $n = 'dwc_ScientificName'
                                                 or $n = 'dc_coverage_spatial'
                                                 or $n = 'dc_coverage_temporal'
                                    ">
                                        <xsl:call-template name="help-hover">
                                            <xsl:with-param name="hover" select="$help"/>
                                        </xsl:call-template>
                                    </xsl:when>
                                    <!-- for other fields, display subsequent sentences in hover text -->
                                    <xsl:otherwise>
                                        <div class="help-title">
                                            <xsl:value-of select="concat(substring-before($help,'.'),'.')"/>
                                        </div>
                                        <xsl:call-template name="help-hover">
                                            <xsl:with-param name="hover" select="substring-after($help,'.')"/>
                                        </xsl:call-template>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:when>

                            <!-- all other cases -->
                            <xsl:otherwise>
                                <xsl:choose>
                                    <xsl:when test="$help/i18n:text">
                                        <xsl:call-template name="help-hover">
                                            <xsl:with-param name="hover" select="$help/i18n:text"/>
                                        </xsl:call-template>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:call-template name="help-hover">
                                            <xsl:with-param name="hover" select="$help"/>
                                        </xsl:call-template>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:if>
                </label>
            </xsl:when>
            <xsl:when test="string-length(string(preceding-sibling::*[1][local-name()='label'])) > 0">
                <xsl:choose>
                    <xsl:when test="./dri:field/@id">
                        <label>
                            <xsl:apply-templates select="preceding-sibling::*[1][local-name()='label']"/>
                            <xsl:text>:</xsl:text>
                        </label>
                    </xsl:when>
                    <xsl:otherwise>
                        <span>
                            <xsl:apply-templates select="preceding-sibling::*[1][local-name()='label']"/>
                            <xsl:text>:</xsl:text>
                        </span>
                    </xsl:otherwise>
                </xsl:choose>

            </xsl:when>
            <xsl:when test="dri:field">
                <xsl:variable name="help" select="dri:help/text()"/>
                <xsl:choose>
                    <xsl:when test="preceding-sibling::*[1][local-name()='label']">
                        <label class="ds-form-label">
                            <xsl:choose>
                                <xsl:when test="./dri:field/@id">
                                    <xsl:attribute name="for">
                                        <xsl:value-of select="translate(./dri:field/@id,'.','_')"/>
                                    </xsl:attribute>
                                </xsl:when>
                            </xsl:choose>
                            <xsl:apply-templates select="preceding-sibling::*[1][local-name()='label']"/>&#160;
                            <xsl:if test="string-length($help)>0">
                                <div class="help-title">
                                    <xsl:value-of select="concat(substring-before($help,'.'),'.')"/>
                                </div>
                                <xsl:call-template name="help-hover">
                                    <xsl:with-param name="hover" select="substring-after($help,'.')"/>
                                </xsl:call-template>
                            </xsl:if>
                        </label>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:apply-templates select="preceding-sibling::*[1][local-name()='label']"/>&#160;
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
                <!-- If the label is empty and the item contains no field, omit the label. This is to
                    make the text inside the item (since what else but text can be there?) stretch across
                    both columns of the list. -->
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="help-hover">
        <xsl:param name="hover" select="''"/>
        <xsl:if test="string-length($hover) > 0">
            <img class="label-mark" src="/themes/Mirage/images/help.jpg">
                <xsl:attribute name="title">
                    <xsl:value-of select="$hover"/>
                </xsl:attribute>
            </img>
        </xsl:if>
    </xsl:template>

</xsl:stylesheet>
