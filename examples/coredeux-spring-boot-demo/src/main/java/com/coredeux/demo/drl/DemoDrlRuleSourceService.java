package com.coredeux.demo.drl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coredeux.drl.converter.AnnotationBasedJavaToDrlConverter;
import com.coredeux.drl.converter.ConvertedDrl;
import com.coredeux.drl.source.resolver.DRLSourceResolver;

@Service
public class DemoDrlRuleSourceService implements DRLSourceResolver {

    private final DrlRuleRecordRepository repository;
    private final AnnotationBasedJavaToDrlConverter converter = new AnnotationBasedJavaToDrlConverter();

    public DemoDrlRuleSourceService(DrlRuleRecordRepository repository) {
        this.repository = repository;
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
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<DrlRuleRecord> findByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return repository.findByCode(code.trim());
    }

    @Transactional
    public DrlRuleRecord save(String code, String description, String drl) {
        String normalizedCode = requireCode(code);
        String normalizedDrl = requireValue(drl, "DRL");
        DrlRuleRecord record = repository.findByCode(normalizedCode).orElseGet(DrlRuleRecord::new);
        record.setCode(normalizedCode);
        record.setDescription(description);
        record.setDrl(normalizedDrl);
        return repository.save(record);
    }

    @Transactional
    public DrlRuleRecord convertAndSave(String source) {
        ConvertedDrl converted = converter.convert(requireValue(source, "Java DRL source"));
        return save(converted.getRuleId(), null, converted.getDrl());
    }

    @Transactional
    public DrlRuleRecord seedSampleRule() {
        return findByCode("demoGreetingRuleSource")
                .orElseGet(() -> convertAndSave(DemoRuleSourceText.source()));
    }

    @Transactional
    public void delete(String code) {
        DrlRuleRecord record = findByCode(code)
                .orElseThrow(() -> new DrlRuleNotFoundException(code));
        repository.delete(record);
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
}
