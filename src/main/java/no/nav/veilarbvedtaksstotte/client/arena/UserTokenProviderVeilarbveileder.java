package no.nav.veilarbvedtaksstotte.client.arena;

import no.nav.veilarbvedtaksstotte.service.AuthService;
import no.nav.veilarbvedtaksstotte.utils.DownstreamApi;

import java.util.function.Supplier;

public class UserTokenProviderVeilarbveileder {
    private final Supplier<String> supplier;

    public UserTokenProviderVeilarbveileder(AuthService authService, String cluster) {
        supplier = authService.contextAwareUserTokenSupplier(new DownstreamApi(cluster, "pto","veilarbveileder"));
    }

    public UserTokenProviderVeilarbveileder(Supplier<String> supplier) {
        this.supplier = supplier;
    }

    public Supplier<String> get() {
        return supplier;
    }
}
