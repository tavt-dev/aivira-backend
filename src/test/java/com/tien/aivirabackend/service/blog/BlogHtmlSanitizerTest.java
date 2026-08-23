package com.tien.aivirabackend.service.blog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BlogHtmlSanitizerTest {
    private final BlogHtmlSanitizer sanitizer = new BlogHtmlSanitizer();

    @Test
    void sanitize_shouldRemoveExecutableMarkupAndKeepRichText() {
        String result = sanitizer.sanitize("<h2>News</h2><script>alert(1)</script><p onclick='bad()'>Body</p>"
                + "<a href='https://aivira.com'>Read</a><img src='javascript:bad()'>");

        assertThat(result).contains("<h2>News</h2>", "<p>Body</p>", "rel=\"noopener noreferrer\"");
        assertThat(result).doesNotContain("script", "onclick", "javascript:");
    }

    @Test
    void sanitize_shouldOnlyAllowHttpsImages() {
        String result = sanitizer
                .sanitize("<img src='http://example.com/a.jpg'><img src='https://example.com/b.jpg' alt='cover'>");

        assertThat(result).doesNotContain("http://example.com/a.jpg");
        assertThat(result).contains("https://example.com/b.jpg");
    }

    @Test
    void sanitize_shouldKeepSafeInternalLinksAndRejectExecutableLinks() {
        String result = sanitizer.sanitize("<a href='/product/clean-code'>Book</a>"
                + "<a href='/category/programming?sort=newest'>Category</a>"
                + "<a href='javascript:alert(1)'>Bad</a>");

        assertThat(result).contains("href=\"/product/clean-code\"",
                "href=\"/category/programming?sort=newest\"");
        assertThat(result).doesNotContain("javascript:");
    }
}
