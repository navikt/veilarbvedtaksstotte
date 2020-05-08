package no.nav.veilarbvedtaksstotte.controller;

import no.nav.veilarbvedtaksstotte.service.BeslutterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/{fnr}/beslutter")
public class BeslutterController {

	private final BeslutterService beslutterService;

	@Autowired
	public BeslutterController(BeslutterService beslutterService) {
		this.beslutterService = beslutterService;
	}

	@PostMapping("/start")
	public void startBeslutterProsess(@PathVariable("fnr") String fnr) {
		beslutterService.startBeslutterProsess(fnr);
	}

	@PostMapping("/bliBeslutter")
	public void bliBeslutter(@PathVariable("fnr") String fnr) {
		beslutterService.bliBeslutter(fnr);
	}

	@PostMapping("/godkjenn")
	public void godkjennVedtak(@PathVariable("fnr") String fnr) {
		beslutterService.setGodkjentAvBeslutter(fnr);
	}

	@PutMapping("/status")
	public void oppdaterBeslutterProsessStatus(@PathVariable("fnr") String fnr) {
		beslutterService.oppdaterBeslutterProsessStatus(fnr);
	}

}
