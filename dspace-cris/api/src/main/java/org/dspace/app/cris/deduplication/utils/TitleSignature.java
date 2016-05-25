package org.dspace.app.cris.deduplication.utils;

import java.util.Locale;

import org.dspace.content.DSpaceObject;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import com.ibm.icu.text.Normalizer;

public class TitleSignature extends MD5ValueSignature {

	@Override
	protected String normalize(DSpaceObject item, String value) {
		if (value != null) {

			String norm = Normalizer.normalize(value, Normalizer.NFD);
			CharsetDetector cd = new CharsetDetector();
			cd.setText(value.getBytes());			
			CharsetMatch detect = cd.detect();
			if (detect != null && detect.getLanguage() != null) {
				norm = norm.replaceAll("[^\\p{L}]", "").toLowerCase(
						new Locale(detect.getLanguage()));
			} else {
				norm = norm.replaceAll("[^\\p{L}]", "").toLowerCase();
			}
			return norm;
		} else {
			return "item:" + item.getID();
		}

	}

}
