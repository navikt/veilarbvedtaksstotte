package no.nav.fo.veilarbvedtaksstotte.resources;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Component
@Path("/")
@Slf4j
public class HelloWorldResource {

    @GET
    @Path("/hello")
    public String hello() {
        return "World";
    }

}
