/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.util;

import org.mcavallo.opencloud.Cloud.Case;

/**
 * @author kstamatis
 *
 */
public class TagCloudParameters {

	String cloudCase;
	String width;
	String colorLevel1;
	String colorLevel2;
	String colorLevel3;
	
	String weightLevel1;
	String weightLevel2;
	String weightLevel3;
	
	String fontFrom;
	String fontTo;
	
	String marginRight;
	
	String cuttingLevel;
	String totalTags;
	boolean randomColors;
	
	String ordering;
	
	boolean displayScore;
	boolean shouldCenter;
	
	String locale;
	
	/**
	 * 
	 */
	public TagCloudParameters() {
		//Default values;
		
		width = "100%";
		
		cloudCase = "Case.PRESERVE_CASE";
		
		colorLevel1 = "D96C27";
		colorLevel2 = "424242";
		colorLevel3 = "818183";
		
		weightLevel1 = "normal";
		weightLevel2 = "normal";
		weightLevel3 = "normal";
		
		fontFrom = "1.1";//"15";
		fontTo = "3.2";//"40";
		
		marginRight = "5";
		
		cuttingLevel = "5";
		totalTags = "all";
		randomColors = true;
		
		ordering = "Tag.GreekNameComparatorAsc";
		
		displayScore = false;
		shouldCenter = true;
		
		locale = "el";
	}

	/**
	 * @return the cloudCase
	 */
	public String getCloudCase() {
		return cloudCase;
	}

	/**
	 * @param cloudCase the cloudCase to set
	 */
	public void setCloudCase(String cloudCase) {
		this.cloudCase = cloudCase;
	}

	/**
	 * @return the width
	 */
	public String getWidth() {
		return width;
	}

	/**
	 * @param width the width to set
	 */
	public void setWidth(String width) {
		this.width = width;
	}

	/**
	 * @return the colorLevel1
	 */
	public String getColorLevel1() {
		return colorLevel1;
	}

	/**
	 * @param colorLevel1 the colorLevel1 to set
	 */
	public void setColorLevel1(String colorLevel1) {
		this.colorLevel1 = colorLevel1;
	}

	/**
	 * @return the colorLevel2
	 */
	public String getColorLevel2() {
		return colorLevel2;
	}

	/**
	 * @param colorLevel2 the colorLevel2 to set
	 */
	public void setColorLevel2(String colorLevel2) {
		this.colorLevel2 = colorLevel2;
	}

	/**
	 * @return the colorLevel3
	 */
	public String getColorLevel3() {
		return colorLevel3;
	}

	/**
	 * @param colorLevel3 the colorLevel3 to set
	 */
	public void setColorLevel3(String colorLevel3) {
		this.colorLevel3 = colorLevel3;
	}

	/**
	 * @return the weightLevel1
	 */
	public String getWeightLevel1() {
		return weightLevel1;
	}

	/**
	 * @param weightLevel1 the weightLevel1 to set
	 */
	public void setWeightLevel1(String weightLevel1) {
		this.weightLevel1 = weightLevel1;
	}

	/**
	 * @return the weightLevel2
	 */
	public String getWeightLevel2() {
		return weightLevel2;
	}

	/**
	 * @param weightLevel2 the weightLevel2 to set
	 */
	public void setWeightLevel2(String weightLevel2) {
		this.weightLevel2 = weightLevel2;
	}

	/**
	 * @return the weightLevel3
	 */
	public String getWeightLevel3() {
		return weightLevel3;
	}

	/**
	 * @param weightLevel3 the weightLevel3 to set
	 */
	public void setWeightLevel3(String weightLevel3) {
		this.weightLevel3 = weightLevel3;
	}

	/**
	 * @return the fontFrom
	 */
	public String getFontFrom() {
		return fontFrom;
	}

	/**
	 * @param fontFrom the fontFrom to set
	 */
	public void setFontFrom(String fontFrom) {
		this.fontFrom = fontFrom;
	}

	/**
	 * @return the fontTo
	 */
	public String getFontTo() {
		return fontTo;
	}

	/**
	 * @param fontTo the fontTo to set
	 */
	public void setFontTo(String fontTo) {
		this.fontTo = fontTo;
	}

	/**
	 * @return the marginRight
	 */
	public String getMarginRight() {
		return marginRight;
	}

	/**
	 * @param marginRight the marginRight to set
	 */
	public void setMarginRight(String marginRight) {
		this.marginRight = marginRight;
	}

	/**
	 * @return the cuttingLevel
	 */
	public String getCuttingLevel() {
		return cuttingLevel;
	}

	/**
	 * @param cuttingLevel the cuttingLevel to set
	 */
	public void setCuttingLevel(String cuttingLevel) {
		this.cuttingLevel = cuttingLevel;
	}

	/**
	 * @return the totalTags
	 */
	public String getTotalTags() {
		return totalTags;
	}

	/**
	 * @param totalTags the totalTags to set
	 */
	public void setTotalTags(String totalTags) {
		this.totalTags = totalTags;
	}

	/**
	 * @return the randomColors
	 */
	public boolean isRandomColors() {
		return randomColors;
	}

	/**
	 * @param randomColors the randomColors to set
	 */
	public void setRandomColors(boolean randomColors) {
		this.randomColors = randomColors;
	}

	/**
	 * @return the ordering
	 */
	public String getOrdering() {
		return ordering;
	}

	/**
	 * @param ordering the ordering to set
	 */
	public void setOrdering(String ordering) {
		this.ordering = ordering;
	}

	/**
	 * @return the displayScore
	 */
	public boolean isDisplayScore() {
		return displayScore;
	}

	/**
	 * @param displayScore the displayScore to set
	 */
	public void setDisplayScore(boolean displayScore) {
		this.displayScore = displayScore;
	}

	/**
	 * @return the shouldCenter
	 */
	public boolean isShouldCenter() {
		return shouldCenter;
	}

	/**
	 * @param shouldCenter the shouldCenter to set
	 */
	public void setShouldCenter(boolean shouldCenter) {
		this.shouldCenter = shouldCenter;
	}

	/**
	 * @return the locale
	 */
	public String getLocale() {
		return locale;
	}

	/**
	 * @param ordering the locale to set
	 */
	public void setLocale(String locale) {
		this.locale = locale;
	}
}
