package no.nav.veilarbvedtaksstotte.config;

import no.nav.common.auth.context.UserRole;
import no.nav.common.auth.oidc.filter.OidcAuthenticationFilter;
import no.nav.common.auth.oidc.filter.OidcAuthenticatorConfig;
import no.nav.common.auth.utils.UserTokenFinder;
import no.nav.common.log.LogFilter;
import no.nav.common.rest.filter.SetStandardHttpHeadersFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static no.nav.common.auth.Constants.*;
import static no.nav.common.auth.oidc.filter.OidcAuthenticator.fromConfigs;
import static no.nav.common.utils.EnvironmentUtils.isDevelopment;
import static no.nav.common.utils.EnvironmentUtils.requireApplicationName;

@Configuration
public class FilterConfig {

    private OidcAuthenticatorConfig openAmAuthConfig(EnvironmentProperties properties) {
        return new OidcAuthenticatorConfig()
                .withDiscoveryUrl(properties.getOpenAmDiscoveryUrl())
                .withClientId(properties.getVeilarbloginOpenAmClientId())
                .withIdTokenCookieName(OPEN_AM_ID_TOKEN_COOKIE_NAME)
                .withRefreshTokenCookieName(REFRESH_TOKEN_COOKIE_NAME)
                .withIdTokenFinder(new UserTokenFinder())
                .withRefreshUrl(properties.getOpenAmRefreshUrl())
                .withUserRole(UserRole.INTERN);
    }

    private OidcAuthenticatorConfig azureAdAuthConfig(EnvironmentProperties properties) {
        return new OidcAuthenticatorConfig()
                .withDiscoveryUrl(properties.getAadDiscoveryUrl())
                .withClientId(properties.getVeilarbloginAadClientId())
                .withIdTokenCookieName(AZURE_AD_ID_TOKEN_COOKIE_NAME)
                .withUserRole(UserRole.INTERN);
    }

    @Bean
    public FilterRegistrationBean logFilterRegistrationBean() {
        FilterRegistrationBean<LogFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new LogFilter(requireApplicationName(), isDevelopment().orElse(false)));
        registration.setOrder(1);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    public FilterRegistrationBean authenticationFilterRegistrationBean(EnvironmentProperties properties) {
        FilterRegistrationBean<OidcAuthenticationFilter> registration = new FilterRegistrationBean<>();
        OidcAuthenticationFilter authenticationFilter = new OidcAuthenticationFilter(
                fromConfigs(openAmAuthConfig(properties), azureAdAuthConfig(properties))
        );

        registration.setFilter(authenticationFilter);
        registration.setOrder(2);
        registration.addUrlPatterns("/api/*");
        return registration;
    }

    @Bean
    public FilterRegistrationBean setStandardHeadersFilterRegistrationBean() {
        FilterRegistrationBean<SetStandardHttpHeadersFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new SetStandardHttpHeadersFilter());
        registration.setOrder(3);
        registration.addUrlPatterns("/*");
        return registration;
    }

}
