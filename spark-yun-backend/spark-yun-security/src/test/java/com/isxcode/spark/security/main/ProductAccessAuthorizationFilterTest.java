package com.isxcode.spark.security.main;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ProductAccessAuthorizationFilterTest {

    private final TestProductAccessAuthorizationFilter filter =
        new TestProductAccessAuthorizationFilter(List.of("/", "/assets/**", "/favicon.ico"));

    @Test
    void skipsConfiguredOpenUrls() {

        assertThat(filter.shouldSkip(request("/"))).isTrue();
        assertThat(filter.shouldSkip(request("/assets/index.js"))).isTrue();
        assertThat(filter.shouldSkip(request("/favicon.ico"))).isTrue();
    }

    @Test
    void skipsOpenApiPaths() {

        assertThat(filter.shouldSkip(request("/tools/open/file"))).isTrue();
        assertThat(filter.shouldSkip(request("/vip/form/open/share"))).isTrue();
    }

    @Test
    void keepsProtectedApiPathsFiltered() {

        assertThat(filter.shouldSkip(request("/cluster/pageCluster"))).isFalse();
    }

    @Test
    void doesNotTreatTenantInviteApplyAsAdminPath() {

        assertThat(filter.isAdminPath("/tenant-user/applyInviteCode")).isFalse();
    }

    @Test
    void keepsOtherTenantUserPathsAsAdminPaths() {

        assertThat(filter.isAdminPath("/tenant-user/pageTenantUser")).isTrue();
    }

    private MockHttpServletRequest request(String path) {

        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setServletPath(path);
        return request;
    }

    private static class TestProductAccessAuthorizationFilter extends ProductAccessAuthorizationFilter {

        TestProductAccessAuthorizationFilter(List<String> excludeUrlPaths) {

            super(null, null, excludeUrlPaths);
        }

        boolean shouldSkip(MockHttpServletRequest request) {

            return shouldNotFilter(request);
        }
    }
}
