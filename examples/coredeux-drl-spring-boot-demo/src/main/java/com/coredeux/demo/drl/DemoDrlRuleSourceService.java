package com.coredeux.demo.drl;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coredeux.core.search.SearchParams;
import com.coredeux.core.search.SearchResult;
import com.coredeux.core.service.CoredeuxService;
import com.coredeux.demo.domain.DrlRuleRecord;
import com.coredeux.drl.converter.AnnotationBasedJavaToDrlConverter;
import com.coredeux.drl.converter.ConvertedDrl;
import com.coredeux.drl.source.resolver.DRLSourceResolver;

@Service
public class DemoDrlRuleSourceService implements DRLSourceResolver {

    private final CoredeuxService coredeuxService;
    private final AnnotationBasedJavaToDrlConverter converter = new AnnotationBasedJavaToDrlConverter();

    public DemoDrlRuleSourceService(@Lazy CoredeuxService coredeuxService) {
        this.coredeuxService = coredeuxService;
    }

    @Override
    @Transactional(readOnly = true)
    public String resolve(String ruleId) {
        return findByCode(ruleId)
                .map(DrlRuleRecord::getDrl)
                .orElseThrow(() -> new DrlRuleNotFoundException(ruleId));
    }

    @Transactional(readOnly = true)
    public List<DrlRuleRecord> findAll() {
    	SearchResult<DrlRuleRecord> results = coredeuxService.loadAll(Collections.emptyList(), DrlRuleRecord.class, -1, -1);
        return results.getResults();
    }

    @Transactional(readOnly = true)
    public Optional<DrlRuleRecord> findByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(searchByCode(code.trim()));
    }

    @Transactional
    public DrlRuleRecord save(String code, String description, String drl) {
        String normalizedCode = requireCode(code);
        String normalizedDrl = requireValue(drl, "DRL");
        DrlRuleRecord record = this.findByCode(normalizedCode).orElseGet(DrlRuleRecord::new);
        record.setCode(normalizedCode);
        record.setDescription(description);
        record.setDrl(normalizedDrl);
        if(null != record.getPk()) {
			coredeuxService.update(record);
			coredeuxService.refresh(record);
		} else {
			String pk = coredeuxService.save(record);
			record.setPk(Long.valueOf(pk));
		}
        return record;
    }

    @Transactional
    public DrlRuleRecord convertAndSave(String source) {
        ConvertedDrl converted = converter.convert(requireValue(source, "Java DRL source"));
        return save(converted.getRuleId(), null, converted.getDrl());
    }

    @Transactional
    public void delete(String code) {
        DrlRuleRecord record = findByCode(code)
                .orElseThrow(() -> new DrlRuleNotFoundException(code));
       coredeuxService.remove(record);
    }

    private String requireCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("DRL rule code must not be blank");
        }
        return code.trim();
    }

    private String requireValue(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value;
    }
    
    private DrlRuleRecord searchByCode(String code) {
    	SearchResult<DrlRuleRecord> results = coredeuxService.loadAll(List.of(SearchParams.builder().field("code").comparator("EQUALS").value(code).build()), DrlRuleRecord.class, -1, -1);
		return   results.getResults().stream().findFirst().orElseThrow(() -> new DrlRuleNotFoundException(code));
	}
}
