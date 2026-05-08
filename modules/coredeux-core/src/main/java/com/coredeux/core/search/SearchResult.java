package com.coredeux.core.search;

import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResult<T> implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private List<T> results;
	private PaginationData pagination;
}
