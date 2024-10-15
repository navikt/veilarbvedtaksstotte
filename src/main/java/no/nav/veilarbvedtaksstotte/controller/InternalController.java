package no.nav.veilarbvedtaksstotte.controller;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.health.selftest.SelfTestChecks;
import no.nav.common.health.selftest.SelfTestUtils;
import no.nav.common.health.selftest.SelftTestCheckResult;
import no.nav.common.health.selftest.SelftestHtmlGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static no.nav.common.health.selftest.SelfTestUtils.checkAllParallel;

@Slf4j
@RestController
@RequestMapping("/internal")
public class InternalController {

    private final SelfTestChecks selfTestChecks;

    @Autowired
    public InternalController(SelfTestChecks selfTestChecks) {
        this.selfTestChecks = selfTestChecks;
    }

    @GetMapping("/selftest")
    public ResponseEntity selftest() {
        List<SelftTestCheckResult> checkResults = checkAllParallel(selfTestChecks.getSelfTestChecks());
        String html = SelftestHtmlGenerator.generate(checkResults);
        int status = SelfTestUtils.findHttpStatusCode(checkResults, true);

        return ResponseEntity
                .status(status)
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }
}
