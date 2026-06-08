package com.coredeux.examples.nativejava.drl;

import java.util.List;
import java.util.Optional;

import com.coredeux.drl.converter.AnnotationBasedJavaToDrlConverter;
import com.coredeux.drl.converter.ConvertedDrl;
import com.coredeux.drl.source.resolver.DRLSourceResolver;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

public class NativeDrlRuleSourceService implements DRLSourceResolver {

    private final EntityManagerFactory entityManagerFactory;
    private final AnnotationBasedJavaToDrlConverter converter = new AnnotationBasedJavaToDrlConverter();

    public NativeDrlRuleSourceService(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public String resolve(String ruleId) {
        return findByCode(ruleId)
                .map(DrlRuleRecord::getDrl)
                .orElseThrow(() -> new DrlRuleNotFoundException(ruleId));
    }

    public List<DrlRuleRecord> findAll() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            return entityManager.createQuery("select r from DrlRuleRecord r order by r.code", DrlRuleRecord.class)
                    .getResultList();
        } finally {
            entityManager.close();
        }
    }

    public Optional<DrlRuleRecord> findByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            List<DrlRuleRecord> results = entityManager
                    .createQuery("select r from DrlRuleRecord r where r.code = :code", DrlRuleRecord.class)
                    .setParameter("code", code.trim())
                    .getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } finally {
            entityManager.close();
        }
    }

    public DrlRuleRecord save(String code, String description, String drl) {
        String normalizedCode = requireCode(code);
        String normalizedDrl = requireValue(drl, "DRL");
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            DrlRuleRecord record = entityManager
                    .createQuery("select r from DrlRuleRecord r where r.code = :code", DrlRuleRecord.class)
                    .setParameter("code", normalizedCode)
                    .getResultStream()
                    .findFirst()
                    .orElseGet(DrlRuleRecord::new);
            record.setCode(normalizedCode);
            record.setDescription(description);
            record.setDrl(normalizedDrl);
            if (record.getId() == null) {
                entityManager.persist(record);
            } else {
                record = entityManager.merge(record);
            }
            transaction.commit();
            return record;
        } catch (RuntimeException exception) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw exception;
        } finally {
            entityManager.close();
        }
    }

    public DrlRuleRecord convertAndSave(String source) {
        ConvertedDrl converted = converter.convert(requireValue(source, "Java DRL source"));
        return save(converted.getRuleId(), null, converted.getDrl());
    }

    public DrlRuleRecord seedSampleRule() {
        return findByCode("demoGreetingRuleSource")
                .orElseGet(() -> convertAndSave(DemoRuleSourceText.source()));
    }

    public void delete(String code) {
        DrlRuleRecord record = findByCode(code)
                .orElseThrow(() -> new DrlRuleNotFoundException(code));
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            DrlRuleRecord managed = entityManager.merge(record);
            entityManager.remove(managed);
            transaction.commit();
        } catch (RuntimeException exception) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw exception;
        } finally {
            entityManager.close();
        }
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
