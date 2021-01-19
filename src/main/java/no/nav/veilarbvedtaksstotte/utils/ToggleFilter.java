package no.nav.veilarbvedtaksstotte.utils;

import no.nav.common.featuretoggle.UnleashService;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static no.nav.veilarbvedtaksstotte.utils.Toggles.PTO_VEDTAKSSTOTTE_PILOT_TOGGLE;
import static no.nav.veilarbvedtaksstotte.utils.Toggles.VEILARBVEDTAKSSTOTTE_ENABLED_TOGGLE;

public class ToggleFilter implements Filter {

    private final UnleashService unleashService;

    public ToggleFilter(UnleashService unleashService) {
        this.unleashService = unleashService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        if (httpServletRequest.getServletPath().startsWith("/internal") ||
                unleashService.isEnabled(VEILARBVEDTAKSSTOTTE_ENABLED_TOGGLE) ||
                unleashService.isEnabled(PTO_VEDTAKSSTOTTE_PILOT_TOGGLE)) {
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
