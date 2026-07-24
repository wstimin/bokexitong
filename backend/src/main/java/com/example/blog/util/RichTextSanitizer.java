package com.example.blog.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class RichTextSanitizer {
    private static final Set<String> ALLOWED_STYLE_PROPERTIES = Set.of(
            "color", "background-color", "font-size", "text-align"
    );
    private static final Safelist SAFELIST = Safelist.relaxed()
            .removeProtocols("a", "href", "ftp", "http", "https", "mailto")
            .removeProtocols("img", "src", "http", "https")
            .addTags("video", "source")
            .addAttributes(":all", "class", "style", "title", "data-list")
            .addAttributes("a", "target", "rel", "download")
            .addAttributes("video", "src", "controls", "preload")
            .addAttributes("source", "src", "type")
            .preserveRelativeLinks(true);

    private RichTextSanitizer() {
    }

    public static String sanitize(String content, String contentType) {
        if (content == null || !isHtml(contentType)) return content;
        Document dirty = Jsoup.parseBodyFragment(content);
        Document clean = new Cleaner(SAFELIST).clean(dirty);
        clean.outputSettings().prettyPrint(false);
        clean.select("a[href]").forEach(link -> sanitizeUrl(link, "href", true));
        clean.select("img[src], video[src], source[src]").forEach(media -> sanitizeUrl(media, "src", false));
        clean.select("a[target=_blank]").forEach(link -> link.attr("rel", "noopener noreferrer"));
        clean.getAllElements().forEach(RichTextSanitizer::sanitizeElement);
        return clean.body().html();
    }

    private static boolean isHtml(String contentType) {
        String normalized = contentType == null ? "" : contentType.trim().toUpperCase(Locale.ROOT);
        return "HTML".equals(normalized) || "RICH_TEXT".equals(normalized);
    }

    private static void sanitizeElement(Element element) {
        for (Attribute attribute : element.attributes().asList()) {
            if (attribute.getKey().toLowerCase(Locale.ROOT).startsWith("on")) {
                element.removeAttr(attribute.getKey());
            }
        }
        if (element.hasAttr("style")) {
            String safeStyle = Arrays.stream(element.attr("style").split(";"))
                    .map(String::trim)
                    .filter(value -> value.contains(":"))
                    .filter(value -> ALLOWED_STYLE_PROPERTIES.contains(value.substring(0, value.indexOf(':')).trim().toLowerCase(Locale.ROOT)))
                    .filter(value -> !value.toLowerCase(Locale.ROOT).contains("url(") && !value.toLowerCase(Locale.ROOT).contains("expression"))
                    .collect(Collectors.joining("; "));
            if (safeStyle.isBlank()) element.removeAttr("style");
            else element.attr("style", safeStyle);
        }
    }

    private static void sanitizeUrl(Element element, String attribute, boolean allowMailto) {
        String value = element.attr(attribute).trim();
        String normalized = value.toLowerCase(Locale.ROOT);
        boolean safe = normalized.startsWith("https://")
                || normalized.startsWith("http://")
                || normalized.startsWith("/")
                || normalized.startsWith("./")
                || normalized.startsWith("../")
                || (allowMailto && normalized.startsWith("mailto:"))
                || (allowMailto && normalized.startsWith("#"));
        if (!safe) element.removeAttr(attribute);
    }
}
