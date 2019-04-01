package no.nav.fo.veilarbvedtaksstotte.resources;

import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;

@Component
@Path("/selftest")
@Slf4j
public class ControlledSelfTestResource implements Helsesjekk {

    private static final HelsesjekkMetadata HELSESJEKK_METADATA = new HelsesjekkMetadata(
            ControlledSelfTestResource.class.getSimpleName(),
            ControlledSelfTestResource.class.getSimpleName(),
            ControlledSelfTestResource.class.getName(),
            false
    );

    private State state = new State();

    @GET
    public State getState() {
        return state;
    }

    @PUT
    public State updateState(State state) {
        return this.state = state == null ? new State() : state;
    }

    @PUT
    @Path("/fail")
    public State fail() {
        return state.setFail(true);
    }

    @PUT
    @Path("/ok")
    public State ok() {
        return state.setFail(false);
    }

    @PUT
    @Path("/delay/{delaySeconds}")
    public State delay(@PathParam("delaySeconds") long delaySeconds) {
        return state.setDelay(delaySeconds * 1000L);
    }

    @Override
    public void helsesjekk() throws Throwable {
        Thread.sleep(state.delay);
        if (state.fail) {
            throw new RuntimeException("feil, feil, feil!");
        }
    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        return HELSESJEKK_METADATA;
    }

    @Accessors(chain = true)
    @Setter
    private static class State {
        private long delay;
        private boolean fail;
    }

}
