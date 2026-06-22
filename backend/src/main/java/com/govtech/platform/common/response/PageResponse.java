package com.govtech.platform.common.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Paginated response envelope returned by list endpoints that support pagination.
 *
 * <p>Used for citizen listing (admin), service request listing, notification listing,
 * and any other collection that may grow unbounded. Wraps Spring Data's {@link Page}
 * into a stable, framework-agnostic JSON structure.</p>
 *
 * <p>Example JSON output:</p>
 * <pre>{@code
 * {
 *   "content": [ ... ],
 *   "page": 0,
 *   "size": 10,
 *   "totalElements": 42,
 *   "totalPages": 5,
 *   "last": false
 * }
 * }</pre>
 *
 * @param <T> the type of each element in the page
 */
@Schema(description = "Paginated response wrapper. Use the page/size query parameters to navigate pages.")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    @Schema(description = "The current page content.")
    private List<T> content;

    @Schema(description = "Zero-based current page number.", example = "0")
    private int page;

    @Schema(description = "Number of elements per page.", example = "10")
    private int size;

    @Schema(description = "Total number of elements across all pages.", example = "42")
    private long totalElements;

    @Schema(description = "Total number of pages.", example = "5")
    private int totalPages;

    @Schema(description = "Whether this is the last page.", example = "false")
    private boolean last;

    /**
     * Convenience factory that maps a Spring Data {@link Page} directly to this response.
     * The page content must already be mapped to the target DTO type {@code T} before calling.
     *
     * @param page a Spring Data page of already-mapped DTOs
     * @param <T>  the DTO type
     * @return a populated {@link PageResponse}
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
