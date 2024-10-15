package no.nav.veilarbvedtaksstotte.controller;

import no.nav.veilarbvedtaksstotte.service.BeslutterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/beslutter")
public class BeslutterController {

	private final BeslutterService beslutterService;

	@Autowired
	public BeslutterController(BeslutterService beslutterService) {
		this.beslutterService = beslutterService;
	}

	@PostMapping("/start")
	public void startBeslutterProsess(@RequestParam("vedtakId") long vedtakId) {
		beslutterService.startBeslutterProsess(vedtakId);
	}

	@PostMapping("/avbryt")
	public void avbrytBeslutterProsess(@RequestParam("vedtakId") long vedtakId) {
		beslutterService.avbrytBeslutterProsess(vedtakId);
	}

	@PostMapping("/bliBeslutter")
	public void bliBeslutter(@RequestParam("vedtakId") long vedtakId) {
		beslutterService.bliBeslutter(vedtakId);
	}

	@PostMapping("/godkjenn")
	public void godkjennVedtak(@RequestParam("vedtakId") long vedtakId) {
		beslutterService.setGodkjentAvBeslutter(vedtakId);
	}

	@PutMapping("/status")
	public void oppdaterBeslutterProsessStatus(@RequestParam("vedtakId") long vedtakId) {
		beslutterService.oppdaterBeslutterProsessStatus(vedtakId);
	}

}
