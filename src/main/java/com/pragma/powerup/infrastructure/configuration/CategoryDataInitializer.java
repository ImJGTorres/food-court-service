package com.pragma.powerup.infrastructure.configuration;

import com.pragma.powerup.infrastructure.out.jpa.entity.CategoryEntity;
import com.pragma.powerup.infrastructure.out.jpa.repository.ICategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CategoryDataInitializer implements ApplicationRunner {
    private static final List<String> DEFAULT_CATEGORIES = List.of("ENTRADA", "PLATO FUERTE", "POSTRE", "BEBIDA");

    private final ICategoryRepository categoryRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (categoryRepository.count() == 0) {
            DEFAULT_CATEGORIES.forEach(name -> categoryRepository.save(new CategoryEntity(null, name)));
        }
    }
}
