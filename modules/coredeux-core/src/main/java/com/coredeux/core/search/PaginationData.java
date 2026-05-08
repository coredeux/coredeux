package com.coredeux.core.search;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaginationData implements Serializable {

	private static final long serialVersionUID = 1L;

    private Long currentPage;
    private Long totalResults;
    private Long pageSize;
    private Long totalPages;
    private Long resultSize;
}
