package com.mintstack.finance.service;

import com.mintstack.finance.entity.NewsCategory;
import com.mintstack.finance.repository.NewsCategoryRepository;
import com.mintstack.finance.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsCategoryBootstrapService {

    private final NewsCategoryRepository newsCategoryRepository;
    private final NewsRepository newsRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void ensureDefaultCategories() {
        List<DefaultCategory> defaults = List.of(
            new DefaultCategory("Piyasa", "piyasa", "Genel piyasa gelismeleri", 1),
            new DefaultCategory("Ekonomi", "ekonomi", "Makro ekonomi ve politika haberleri", 2),
            new DefaultCategory("Sirket", "sirket", "Sirket bilanco ve operasyon haberleri", 3)
        );

        int created = 0;
        int updated = 0;
        for (DefaultCategory defaultCategory : defaults) {
            NewsCategory category = newsCategoryRepository.findBySlug(defaultCategory.slug()).orElse(null);
            if (category == null) {
                newsCategoryRepository.save(NewsCategory.builder()
                    .name(defaultCategory.name())
                    .slug(defaultCategory.slug())
                    .description(defaultCategory.description())
                    .displayOrder(defaultCategory.displayOrder())
                    .isActive(true)
                    .build());
                created++;
                continue;
            }

            boolean changed = false;
            if (category.getDisplayOrder() == null) {
                category.setDisplayOrder(defaultCategory.displayOrder());
                changed = true;
            }
            if (!Boolean.TRUE.equals(category.getIsActive())) {
                category.setIsActive(true);
                changed = true;
            }
            if (changed) {
                newsCategoryRepository.save(category);
                updated++;
            }
        }

        int normalizedSummaries = newsRepository.fillMissingSummaryWithTitle();
        int normalizedContents = newsRepository.fillMissingContentWithSummary();

        if (created > 0 || updated > 0 || normalizedSummaries > 0 || normalizedContents > 0) {
            log.info("News category bootstrap completed: created={}, updated={}", created, updated);
            log.info("News content normalization completed: summaries={}, contents={}", normalizedSummaries, normalizedContents);
        }
    }

    private record DefaultCategory(String name, String slug, String description, Integer displayOrder) {
    }
}
