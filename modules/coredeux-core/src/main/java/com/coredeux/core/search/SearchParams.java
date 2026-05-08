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
public class SearchParams implements Serializable {

	private static final long serialVersionUID = 1L;
	private String field;
    private String comparator;
    private Object value;
}