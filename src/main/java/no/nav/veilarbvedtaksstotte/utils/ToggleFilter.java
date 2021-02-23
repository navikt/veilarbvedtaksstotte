package no.nav.veilarbvedtaksstotte.utils;

import no.nav.veilarbvedtaksstotte.service.UnleashService;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class ToggleFilter implements Filter {

    private final UnleashService unleashService;

    public ToggleFilter(UnleashService unleashService) {
        this.unleashService = unleashService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        if (httpServletRequest.getServletPath().startsWith("/internal") ||
                unleashService.isVedtaksstotteEnabled() ||
                unleashService.isVedtaksstottePilotEnabled()) {
            chain.doFilter(request, response);
        } else {
            throw new IllegalStateException("ikke tilgjengelig");
        }
    }

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void destroy() {}
}
