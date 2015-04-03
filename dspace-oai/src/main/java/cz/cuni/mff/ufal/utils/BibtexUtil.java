/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.utils;

import java.util.StringTokenizer;


/**
 * Escaping from the old oai-bibtex crosswalk
 * Meant to be called from xsl
 */
public class BibtexUtil {

	/**
	 * Make the string bibtex friendly by escaping etc. See
	 * http://www.bibtex.org/SpecialSymbols/
	 * https://java-bibtex.googlecode.com/svn
	 * /trunk/src/main/java/org/jbibtex/LaTeXPrinter.java
	 */
	public static String bibtexify(String s) {
		return new BibtexString(s).toString().replaceAll(",\\s+$", "").replaceAll("(?m)[\\r\\n]+", "");
	}
	
	public static String format(String s){
		return s.replaceAll("\\s+", " ").replaceAll(" $", "").replaceAll("[,;]\\s*}", "}") +"\n";
	}
	
	public static void main(String[] args){
		System.out.println(bibtexify("Příliš žluťoučký kůň úpěl ďábelské ódy")); 
		System.out.println(bibtexify(""));
		System.out.println(bibtexify("Add some \n\n\n\n new lines\n to the mix."));
	}
}

class BibtexString {

	private String s_;

	BibtexString(String s) {
		s_ = _escape(s);
	}

	@Override
	public String toString() {
		return s_;
	}

	private static String _escape(String s) {

		// change escape characters first (we will introduce those in the next
		// replacements)
		for (String ch : to_escape) {
			s = s.replaceAll("\\" + ch, "\\\\" + ch);
		}

		String news = "";
		StringTokenizer stwords = new StringTokenizer(s, " \t\n\r\f", true);

		// first remove urls from {}ification
		//
		while (stwords.hasMoreTokens()) {
			String word = stwords.nextToken();
			if (1 < word.length()
					&& (!word.startsWith("http") && !word.startsWith("ftp"))) {
				// then, go throught all word parts long enough
				// there could still be problems with (http://P123)
				//
				String newword = "";
				StringTokenizer st = new StringTokenizer(word,
						" \t\n\r\f().!?:;<>_\"'~=+-@#$%^*/\\|,", true);
				while (st.hasMoreTokens()) {
					String wordpart = st.nextToken();
					// if it is long
					// and not url
					// and lowercase does not match
					if (1 < word.length()) {
						String ww = wordpart.substring(1);
						if (!ww.toLowerCase().equals(ww)) {
							wordpart = "{" + wordpart + "}";
						}
					}
					newword += wordpart;
				} //
				word = newword;

			}
			news += word;
		}
		s = news;

		// change accents with uppercase too
		for (String ch : accents_todo_uppercase) {
			String to_find = ch.substring(0, 1);
			String to_change_with = ch.substring(1).replaceAll("\\\\",
					"\\\\\\\\");
			s = s.replaceAll(to_find, to_change_with);
			// uppercase only chars after {
			int lbr_idx = to_change_with.indexOf("{");
			// or only the last char
			lbr_idx = lbr_idx == -1 ? to_change_with.length() - 2 : lbr_idx;
			String to_change_with_upper = to_change_with.substring(0, lbr_idx)
					+ to_change_with.substring(lbr_idx).toUpperCase();
			s = s.replaceAll(to_find.toUpperCase(), to_change_with_upper);
		}
		// change accents with uppercase too
		for (String ch : symbols_final) {
			String to_find = ch.substring(0, 1);
			String to_change_with = ch.substring(1).replaceAll("\\\\",
					"\\\\\\\\");
			s = s.replaceAll(to_find, to_change_with);
		}

		return s;
	}

	//CZ - Příliš žluťoučký kůň úpěl ďábelské ódy 
	// lower case, will do uppercase automatically
	public static final String[] accents_todo_uppercase = new String[] {
			"à\\`{a}", "á\\'{a}", "â\\^{a}", "ã\\~{a}", "ā\\={a}", "ä\\\"{a}",

			"è\\`{e}", "é\\'{e}", "ê\\^{e}", "ẽ\\~{e}", "ē\\={e}", "ë\\\"{e}", "ě\\v{e}",

			"ì\\`{i}", "í\\'{i}", "î\\^{i}", "ĩ\\~{i}", "ī\\={i}", "ï\\\"{i}",

			"ò\\`{o}", "ó\\'{o}", "ô\\^{o}", "õ\\~{o}", "ō\\={o}", "ö\\\"{o}",

			"ĺ\\'{l}",

			"ù\\`{u}", "ú\\'{u}", "û\\^{u}", "ũ\\~{u}", "ū\\={u}", "ü\\\"{u}", "ů\\r{u}",

			"ý\\'{y}", "ÿ\\\"{y}", "ñ\\~{n}",

			"ś\\'{s}", "ń\\'{n}", "ć\\'{c}",

			"ç\\c{c}", "ȩ\\c{e}",

			"ọ\\d{o}",

			"ŏ\\v{o}", "č\\v{c}", "ž\\v{z}", "š\\v{s}", "ň\\v{n}", "ď\\v{d}",
			"ť\\v{t}", "ľ\\v{l}", "ř\\v{r}",

			"œ\\oe", "æ\\ae", "å\\aa", "ø\\o", "þ\\th", "ł\\l", };

	// without automatic uppercase
	public static final String[] symbols_final = new String[] { "ß\\ss",
			"£\\pounds", "§\\S", "©\\textcopyright", "ª\\textordfeminine",
			"®\\textregistered", "¶\\P", "·\\textperiodcentered",
			"º\\textordmasculine", "¿\\\\?? ", };

	public static final String[] to_escape = new String[] { "?", "&", "$", "{",
			"}", "%", "_", "#", };

} // class BibtexString

