package no.nav.fo.veilarbvedtaksstotte.config;

import no.nav.sbl.featuretoggle.unleash.UnleashService;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class ToggleFilter implements Filter {

    private static final String VEILARBVEDTAKSSTOTTE_ENABLED_TOGGLE = "veilarbvedtaksstotte.enabled";

    private UnleashService unleashService;

    public ToggleFilter(UnleashService unleashService) {
        this.unleashService = unleashService;
    }

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        if (httpServletRequest.getServletPath().startsWith("/internal") ||
                unleashService.isEnabled(VEILARBVEDTAKSSTOTTE_ENABLED_TOGGLE)) {
            chain.doFilter(request, response);
        } else {
            throw new IllegalStateException("ikke tilgjengelig");
        }
    }

    @Override
    public void destroy() {

    }
}
