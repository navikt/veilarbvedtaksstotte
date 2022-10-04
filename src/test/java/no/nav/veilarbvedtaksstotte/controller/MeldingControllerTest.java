package no.nav.veilarbvedtaksstotte.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.veilarbvedtaksstotte.controller.dto.OpprettDialogMeldingDTO;
import no.nav.veilarbvedtaksstotte.service.MeldingService;
import no.nav.veilarbvedtaksstotte.service.VedtakService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@WebMvcTest(controllers = MeldingController.class)
public class MeldingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MeldingService meldingService;

    @MockBean
    private VedtakService vedtakService;

    @Test
    public void hentDialogMeldinger__skal_feile_hvis_vedtak_fattet() throws Exception {
        when(vedtakService.erFattet(2)).thenReturn(true);

        MockHttpServletResponse response = mockMvc.perform(get("/api/meldinger").queryParam("vedtakId", "2"))
                .andReturn().getResponse();

        assertEquals(400, response.getStatus());

        verify(meldingService, never()).hentMeldinger(2);
    }

    @Test
    public void opprettDialogMelding__skal_feile_hvis_vedtak_fattet() throws Exception {
        when(vedtakService.erFattet(2)).thenReturn(true);

        MockHttpServletResponse response = mockMvc.perform(
                post("/api/meldinger")
                        .queryParam("vedtakId", "2")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(new OpprettDialogMeldingDTO()))
        ).andReturn().getResponse();

        assertEquals(400, response.getStatus());

        verify(meldingService, never()).opprettBrukerDialogMelding(eq(2), anyString());
    }

}
