package org.net.news;

import java.net.URI;
import java.time.Instant;

public record NewsArticle(
        String title,
        String summary,
        String source,
        Instant publishedAt,
        URI link
) {}
