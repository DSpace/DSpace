package pt.uminho.sdum.dspace.requestItem.util;

import java.text.MessageFormat;
import java.util.regex.Pattern;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class KeepMessageFormat {
	private static Logger log = LogManager.getLogger(KeepMessageFormat.class);
	public static String format (String message, Object[] args) {
		for (int i=0;i<args.length;i++) {
			String replacement = MessageFormat.format("{0}", args[i]);
			message = message.replaceAll(Pattern.quote("{"+i+"}"), replacement);
		}
		return message;
	}
}