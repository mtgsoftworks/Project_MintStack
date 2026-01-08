package com.mintstack.finance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginationInfo {

    private int page;
    
    private int size;
    
    private long totalElements;
    
    private int totalPages;
    
    private boolean first;
    
    private boolean last;
    
    private boolean hasNext;
    
    private boolean hasPrevious;

    public static PaginationInfo from(Page<?> page) {
        return PaginationInfo.builder()
            .page(page.getNumber())
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .first(page.isFirst())
            .last(page.isLast())
            .hasNext(page.hasNext())
            .hasPrevious(page.hasPrevious())
            .build();
    }
}
