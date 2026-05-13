package com.coredeux.demo.audit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.coredeux.core.audit.CoredeuxEntityAuditHandler;
import com.coredeux.core.context.EntityLifecycleContext;
import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.demo.domain.Item;

public class DemoAuditHandler implements CoredeuxEntityAuditHandler<Item> {

    private static final Logger LOG = LoggerFactory.getLogger(DemoAuditHandler.class);

    private final List<AuditEntry> auditEntries = new ArrayList<>();

    @Override
    public synchronized void audit(Item entity, CoredeuxEntityDefinition definition, OperationContext context) {
        EntityLifecycleContext<?> lifecycleContext = context.getLifecycleContext();
        AuditEntry entry = new AuditEntry(
                definition.getName(),
                lifecycleContext != null ? lifecycleContext.getOperation() : null,
                lifecycleContext != null ? lifecycleContext.getIdentifier() : null);
        auditEntries.add(entry);
        LOG.info("Audit entity {} operation {} identifier {}", entry.entityName(), entry.operation(),
                entry.identifier());
    }

    public synchronized List<AuditEntry> getAuditEntries() {
        return Collections.unmodifiableList(new ArrayList<>(auditEntries));
    }

    public synchronized void clear() {
        auditEntries.clear();
    }

    public record AuditEntry(String entityName, String operation, Object identifier) {
    }
}
