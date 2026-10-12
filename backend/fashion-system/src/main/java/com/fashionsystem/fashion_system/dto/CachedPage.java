package com.fashionsystem.fashion_system.dto;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Redis-safe representation of a Spring Data page without serializing framework internals. */
public record CachedPage<T>(
        List<T> content,
        boolean paged,
        int number,
        int size,
        long totalElements,
        List<CachedSortOrder> sort) {

    public CachedPage {
        content = content == null ? new ArrayList<>() : new ArrayList<>(content);
        sort = sort == null ? new ArrayList<>() : new ArrayList<>(sort);
        if (number < 0 || (paged && size < 1) || totalElements < 0) {
            throw new IllegalArgumentException("Invalid cached page metadata");
        }
    }

    public static <T> CachedPage<T> from(Page<T> page) {
        List<CachedSortOrder> orders = page.getSort().stream()
                .map(CachedSortOrder::from)
                .toList();
        return new CachedPage<>(page.getContent(), page.getPageable().isPaged(),
                page.getNumber(), page.getSize(),
                page.getTotalElements(), orders);
    }

    public Page<T> toPage() {
        Sort restoredSort = sort.isEmpty()
                ? Sort.unsorted()
                : Sort.by(sort.stream().map(CachedSortOrder::toOrder).toList());
        if (!paged) {
            return new PageImpl<>(new ArrayList<>(content), Pageable.unpaged(restoredSort),
                    totalElements);
        }
        return new PageImpl<>(new ArrayList<>(content), PageRequest.of(number, size, restoredSort),
                totalElements);
    }

    public record CachedSortOrder(
            String property,
            Sort.Direction direction,
            boolean ignoreCase,
            Sort.NullHandling nullHandling) {

        static CachedSortOrder from(Sort.Order order) {
            return new CachedSortOrder(order.getProperty(), order.getDirection(),
                    order.isIgnoreCase(), order.getNullHandling());
        }

        Sort.Order toOrder() {
            Sort.Order order = new Sort.Order(direction, property, nullHandling);
            return ignoreCase ? order.ignoreCase() : order;
        }
    }
}
