package com.isxcode.spark.common.jpa;

import static com.isxcode.spark.common.jpa.JpaTenantContext.TENANT_FILTER;
import static com.isxcode.spark.common.jpa.JpaTenantContext.TENANT_IDS_PARAM;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class JpaTenantFilterAspect {

    @PersistenceContext
    private EntityManager entityManager;

    @Around("execution(* com.isxcode.spark..repository..*(..)) || this(org.springframework.data.repository.Repository)")
    public Object enableTenantFilter(ProceedingJoinPoint joinPoint) throws Throwable {

        Session session = entityManager.unwrap(Session.class);

        // Default queries use ContextHolder tenantId. noTenant() adds shared data. allData() disables the
        // filter.
        List<String> tenantIds = JpaTenantContext.getVisibleTenantIds();

        if (tenantIds.isEmpty()) {
            session.disableFilter(TENANT_FILTER);
        } else {
            session.enableFilter(TENANT_FILTER).setParameterList(TENANT_IDS_PARAM, tenantIds);
        }
        applyDataScopeFilter(session);

        return joinPoint.proceed();
    }

    private void applyDataScopeFilter(Session session) {

        session.disableFilter(DataScopeContext.CLUSTER_FILTER);
        session.disableFilter(DataScopeContext.DATASOURCE_FILTER);
        session.disableFilter(DataScopeContext.FILE_FILTER);

        DataScopeContext.DataScope dataScope = DataScopeContext.getDataScope();
        if (dataScope == null) {
            return;
        }
        if (dataScope.clusterScope() != null && dataScope.clusterScope().restricted()) {
            session.enableFilter(DataScopeContext.CLUSTER_FILTER).setParameterList(DataScopeContext.CLUSTER_IDS_PARAM,
                dataScope.clusterScope().filterIds());
        }
        if (dataScope.datasourceScope() != null && dataScope.datasourceScope().restricted()) {
            session.enableFilter(DataScopeContext.DATASOURCE_FILTER)
                .setParameterList(DataScopeContext.DATASOURCE_IDS_PARAM, dataScope.datasourceScope().filterIds());
        }
        if (dataScope.fileScope() != null && dataScope.fileScope().restricted()) {
            session.enableFilter(DataScopeContext.FILE_FILTER).setParameterList(DataScopeContext.FILE_IDS_PARAM,
                dataScope.fileScope().filterIds());
        }
    }
}
