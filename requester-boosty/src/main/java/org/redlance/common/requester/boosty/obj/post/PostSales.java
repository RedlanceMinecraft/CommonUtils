package org.redlance.common.requester.boosty.obj.post;

import java.util.Collections;
import java.util.List;

/**
 * Page of post sales.
 *
 * @param postsSales Sales on the page.
 */
public record PostSales(List<PostSale> postsSales) {
    public PostSales {
        postsSales = postsSales == null ? List.of() : Collections.unmodifiableList(postsSales);
    }
}
