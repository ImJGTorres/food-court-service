package com.pragma.powerup.infrastructure.out.jpa.adapter;

import com.pragma.powerup.domain.model.PageModel;
import org.springframework.data.domain.Page;

import java.util.function.Function;

final class PageModelConverter {

    private PageModelConverter() {
    }

    static <E, M> PageModel<M> toPageModel(Page<E> page, Function<E, M> toModel) {
        return new PageModel<>(page.getContent().stream().map(toModel).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
