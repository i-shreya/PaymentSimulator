package com.example.payment.audit;

import com.example.payment.entity.AuditLog;
import com.example.payment.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogRepository audits;

    public void record(String entityType, String entityId, String action, String oldStatus, String newStatus, String performedBy, String metadata) {
        var audit = new AuditLog();
        audit.setEntityType(entityType);
        audit.setEntityId(entityId);
        audit.setAction(action);
        audit.setOldStatus(oldStatus);
        audit.setNewStatus(newStatus);
        audit.setPerformedBy(performedBy);
        audit.setMetadata(metadata);
        audits.save(audit);
    }
}
