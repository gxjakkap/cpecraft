package com.cpesu.cpecraft.motd;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * Minimal markdown-ish formatting for the MOTD: **bold**, *italic*,
 * __underline__, ~~strikethrough~~. Not a full CommonMark parser - no
 * nesting, no escaping, no links; just enough for an admin to emphasize
 * part of a welcome message.
 */
public final class Markdown {
	public enum Emphasis {
		NONE, BOLD, ITALIC, UNDERLINE, STRIKETHROUGH
	}

	public record Segment(String text, Emphasis emphasis) {
	}

	// Bold checked before italic so "**x**" isn't parsed as italic-of-empty then literal asterisks.
	private static final Pattern TOKEN = Pattern.compile(
			"\\*\\*(.+?)\\*\\*|__(.+?)__|~~(.+?)~~|\\*(.+?)\\*");

	private Markdown() {
	}

	public static List<Segment> tokenize(String text) {
		List<Segment> segments = new ArrayList<>();
		Matcher matcher = TOKEN.matcher(text);
		int lastEnd = 0;
		while (matcher.find()) {
			if (matcher.start() > lastEnd) {
				segments.add(new Segment(text.substring(lastEnd, matcher.start()), Emphasis.NONE));
			}
			if (matcher.group(1) != null) {
				segments.add(new Segment(matcher.group(1), Emphasis.BOLD));
			} else if (matcher.group(2) != null) {
				segments.add(new Segment(matcher.group(2), Emphasis.UNDERLINE));
			} else if (matcher.group(3) != null) {
				segments.add(new Segment(matcher.group(3), Emphasis.STRIKETHROUGH));
			} else {
				segments.add(new Segment(matcher.group(4), Emphasis.ITALIC));
			}
			lastEnd = matcher.end();
		}
		if (lastEnd < text.length()) {
			segments.add(new Segment(text.substring(lastEnd), Emphasis.NONE));
		}
		return segments;
	}

	public static MutableComponent toComponent(String text) {
		MutableComponent result = Component.empty();
		for (Segment segment : tokenize(text.replace("\\n", "\n"))) {
			MutableComponent part = Component.literal(segment.text());
			switch (segment.emphasis()) {
				case BOLD -> part = part.withStyle(Style.EMPTY.withBold(true));
				case ITALIC -> part = part.withStyle(Style.EMPTY.withItalic(true));
				case UNDERLINE -> part = part.withStyle(Style.EMPTY.withUnderlined(true));
				case STRIKETHROUGH -> part = part.withStyle(Style.EMPTY.withStrikethrough(true));
				case NONE -> {
				}
			}
			result.append(part);
		}
		return result;
	}

	/** ponytail: no test framework wired up yet; this is the parser's one runnable check. */
	public static void main(String[] args) {
		check(tokenize("plain text").equals(List.of(new Segment("plain text", Emphasis.NONE))), "plain text");
		check(tokenize("**bold**").equals(List.of(new Segment("bold", Emphasis.BOLD))), "bold");
		check(tokenize("*italic* and **bold**").equals(List.of(
				new Segment("italic", Emphasis.ITALIC),
				new Segment(" and ", Emphasis.NONE),
				new Segment("bold", Emphasis.BOLD))), "italic+bold");
		check(tokenize("__u__~~s~~").equals(List.of(
				new Segment("u", Emphasis.UNDERLINE),
				new Segment("s", Emphasis.STRIKETHROUGH))), "underline+strikethrough");
		System.out.println("Markdown self-check passed");
	}

	private static void check(boolean condition, String label) {
		if (!condition) {
			throw new AssertionError("Markdown self-check failed: " + label);
		}
	}
}
