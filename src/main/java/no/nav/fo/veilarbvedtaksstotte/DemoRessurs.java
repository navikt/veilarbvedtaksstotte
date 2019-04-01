package no.nav.fo.veilarbvedtaksstotte;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.time.Duration;

@Component
@Path("/")
@Slf4j
public class DemoRessurs {

    @GET
    public String get() {
        return ok();
    }

    @GET
    @Path("/ok")
    public String ok() {
        return "alt ok!";
    }

    @GET
    @Path("/slow")
    @SneakyThrows
    public String slow() {
        long millis = Duration.ofMinutes(5).toMillis();
        log.info("sleeping {}ms", millis);
        Thread.sleep(millis);
        return "alt ok!";
    }

    @POST
    @Path("/pipe")
    public String pipe(String content) {
        return content;
    }

    @GET
    @Path("/feil")
    public String feil() {
        throw new IllegalStateException("feil!");
    }

}