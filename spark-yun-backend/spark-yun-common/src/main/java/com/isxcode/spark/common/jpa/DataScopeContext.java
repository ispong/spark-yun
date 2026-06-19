package com.isxcode.spark.common.jpa;

import java.util.Set;
import java.util.function.Supplier;

public final class DataScopeContext {

    public static final String CLUSTER_FILTER = "clusterDataScopeFilter";

    public static final String CLUSTER_IDS_PARAM = "clusterDataScopeIds";

    public static final String DATASOURCE_FILTER = "datasourceDataScopeFilter";

    public static final String DATASOURCE_IDS_PARAM = "datasourceDataScopeIds";

    public static final String FILE_FILTER = "fileDataScopeFilter";

    public static final String FILE_IDS_PARAM = "fileDataScopeIds";

    private static final String NO_DATA_ID = "__no_data__";

    private static final ThreadLocal<DataScope> DATA_SCOPE = new ThreadLocal<>();

    private DataScopeContext() {}

    public static DataScope getDataScope() {

        return DATA_SCOPE.get();
    }

    public static <T> T runWithDataScope(DataScope dataScope, Supplier<T> supplier) {

        DataScope oldDataScope = DATA_SCOPE.get();
        try {
            if (dataScope == null) {
                DATA_SCOPE.remove();
            } else {
                DATA_SCOPE.set(dataScope);
            }
            return supplier.get();
        } finally {
            restore(oldDataScope);
        }
    }

    public static void runWithDataScope(DataScope dataScope, Runnable runnable) {

        runWithDataScope(dataScope, () -> {
            runnable.run();
            return null;
        });
    }

    private static void restore(DataScope dataScope) {

        if (dataScope == null) {
            DATA_SCOPE.remove();
        } else {
            DATA_SCOPE.set(dataScope);
        }
    }

    public record DataScope(ResourceScope clusterScope, ResourceScope datasourceScope, ResourceScope fileScope) {}

    public record ResourceScope(boolean allEnabled, Set<String> resourceIds) {

        public boolean restricted() {

            return !allEnabled;
        }

        public Set<String> filterIds() {

            if (resourceIds == null || resourceIds.isEmpty()) {
                return Set.of(NO_DATA_ID);
            }
            return resourceIds;
        }
    }
}
