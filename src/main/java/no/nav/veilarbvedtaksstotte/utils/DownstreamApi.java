package no.nav.veilarbvedtaksstotte.utils;

public class DownstreamApi {
    public final String cluster;
    public final String namespace;
    public final String serviceName;
    public final String serviceEnvironment;

    public DownstreamApi(String cluster, String namespace, String serviceName, String serviceEnvironment) {
        this.cluster = cluster;
        this.namespace = namespace;
        this.serviceName = serviceName;
        this.serviceEnvironment = serviceEnvironment;
    }
}
