package com.k0b3rit.domain.utils.log;

public class LogColoringUtil {

	public static final String ANSI_RESET = "\u001B[0m";

	public static String getTextWithColor(String text, LogColor color) {
		return color.getAnsiCode() + text + ANSI_RESET;
	}

	public static String getFormatedTextWithColor(String text, LogColor color, Object... args) {
		return color.getAnsiCode() + String.format(text, args) + ANSI_RESET;
	}

	public static String inYellow(String text, Object... args) {
		return getFormatedTextWithColor(text, LogColor.YELLOW, args);
	}

	public static String inYellow(String text) {
		return LogColor.YELLOW.getAnsiCode() + text + ANSI_RESET;
	}

	public static String inRed(String text, Object... args) {
		return getFormatedTextWithColor(text, LogColor.RED, args);
	}

	public static String inRed(String text) {
		return LogColor.RED.getAnsiCode() + text + ANSI_RESET;
	}

	public static String inBlue(String text, Object... args) {
		return getFormatedTextWithColor(text, LogColor.BLUE, args);
	}

	public static String inWhite(String text, Object... args) {
		return getFormatedTextWithColor(text, LogColor.WHITE, args);
	}

	public static String inGreen(String text, Object... args) {
		return getFormatedTextWithColor(text, LogColor.GREEN, args);
	}

	public static String inGreen(String text) {
		return LogColor.GREEN.getAnsiCode() + text + ANSI_RESET;
	}


	public static String inCyan(String text, Object... args) {
		return getFormatedTextWithColor(text, LogColor.CYAN, args);
	}

	public static String inCyan(String text) {
		return LogColor.CYAN.getAnsiCode() + text + ANSI_RESET;
	}

	public static String inPurple(String text, Object... args) {
		return getFormatedTextWithColor(text, LogColor.PURPLE, args);
	}
}
