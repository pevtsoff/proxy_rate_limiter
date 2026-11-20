package com.example.ratelimiter.adapter.out.persistence;

import com.example.ratelimiter.domain.model.PageQuery;
import com.example.ratelimiter.domain.model.PageResult;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/** Translates between the domain paging types and Spring Data's. */
final class Pages {

    private Pages() {
    }

    static PageRequest toPageRequest(PageQuery query) {
        return PageRequest.of(query.page(), query.size(), Sort.by(Sort.Direction.ASC, "id"));
    }

    static <E, D> PageResult<D> toPageResult(Page<E> page, Function<E, D> mapper) {
        return new PageResult<>(page.getContent().stream().map(mapper).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements());
    }
}
