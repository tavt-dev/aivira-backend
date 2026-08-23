package com.tien.aivirabackend.service.blog;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

@Component
public class BlogHtmlSanitizer {
    private static final String INTERNAL_BASE_URI = "https://aivira.local";
    private static final Safelist SAFELIST = Safelist.none()
            .addTags("p", "br", "h1", "h2", "h3", "h4", "h5", "h6", "strong", "b", "em", "i", "u", "s", "ul", "ol",
                    "li", "blockquote", "pre", "code", "table", "thead", "tbody", "tr", "th", "td", "a", "img",
                    "figure", "figcaption", "hr")
            .addAttributes("a", "href", "title", "target")
            .addAttributes("img", "src", "alt", "title", "width", "height").addAttributes("th", "colspan", "rowspan")
            .addAttributes("td", "colspan", "rowspan").addProtocols("a", "href", "http", "https", "mailto")
            .addProtocols("img", "src", "https").preserveRelativeLinks(true);

    public String sanitize(String html) {
        String cleaned = Jsoup.clean(html == null ? "" : html, INTERNAL_BASE_URI, SAFELIST,
                new Document.OutputSettings().prettyPrint(false));
        Document document = Jsoup.parseBodyFragment(cleaned);
        for (Element link : document.select("a[href]")) {
            link.attr("rel", "noopener noreferrer");
        }
        return document.body().html();
    }
}
