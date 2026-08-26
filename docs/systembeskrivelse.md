# Systembeskrivelse: Oppfølgingsvedtak § 14 a og Kvalitetssikring § 14 a

> Sist oppdatert: 2026-08-26  
> Eier: Team OBO  
> Kontakt: [#team-obo-arbeidsoppfølging (Slack)](https://nav-it.slack.com/archives/C02G0292ULW)

## Omfang og avgrensning

Oppfølgingsvedtak § 14 a og Kvalitetssikring § 14 a er et system for NAV-veiledere som brukes til å opprette utkast til
og fatte oppfølgingsvedtak etter Nav-loven § 14 a, samt støtte for kvalitetssikring på vedtak som krever det. Systemet
håndterer journalføring og brevdistribusjon, og eksponerer API-er slik at andre Nav-tjenester kan lese gjeldende og
historiske vedtak.

Systemet består av følgende hovedkomponenter:

* `veilarbvedtaksstotte`: backend som eier forretningslogikken
* `veilarbvedtaksstotte (database)`: PostgreSQL database for `veilarbvedtaksstotte`, Google Cloud SQL instans
  provisjonert og managed av Nais
* `14a_vedtak_statistikk`: BigQuery datasett for `veilarbvedtaksstotte`, Google Cloud BigQuery instans provisjonert og
  managed av Nais
* `gjeldende-14a-vedtak-v1`, `siste-14a-vedtak-v1`, `vedtak-14a-fattet-dvh-v1`, `vedtak-14a-statusendring-v1`,
  `vedtak-sendt-v1`: Aiven Kafka topics som `veilarbvedtaksstotte` produserer til, provisjonert og managed av Nais
* `veilarbvedtaksstottefs`: brukerflate/-grensesnitt som brukes for oppretting av utkast og fatting av oppfølgingsvedtak
* `beslutteroversikt`: brukerflate/-grensesnitt som brukes for å gi oversikt over vedtak som krever kvalitetssikring og
  status for vedtak med pågående kvalitetssikring

I tillegg er følgende støttekomponenter relevante:

* `veilarbpersonflatefs`: micro-frontend container hvor `veilarbvedtaksstottefs` er hostet sammen med flere andre
  micro-frontends

## Kjøretidsmiljø

| Komponent                | Språk og rammeverk                  | Plattform          | Cluster og namespace    | Ingress(er)                                                                                               | Containerisering                                                                |
|--------------------------|-------------------------------------|--------------------|-------------------------|-----------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------|
| `veilarbvedtaksstotte`   | Java/Kotlin, Spring Boot 4, Java 21 | Nais GCP           | dev-gcp / prod-gcp, obo | veilarbvedtaksstotte.intern.dev.nav.no, veilarbvedtaksstotte.intern.nav.no                                | Docker-image basert på Chainguard security-hardened distroless JRE (OpenJDK 21) |
| `veilarbvedtaksstottefs` | JavaScript/TypeScript, React        | Nais CDN           | -                       | -                                                                                                         | -                                                                               |
| `beslutteroversikt`      | JavaScript/TypeScript, React        | Nais CDN, Nais GCP | dev-gcp / prod-gcp, obo | beslutteroversikt.intern.dev.nav.no, beslutteroversikt.ansatt.dev.nav.no, beslutteroversikt.intern.nav.no | Docker-image basert på poao-frontend                                            |

## Sikkerhetsmekanismer

- **Nais nettverkspolicy (`accessPolicy`):** Kun eksplisitt listede applikasjoner har nettverkstilgang inn og ut.
  Håndheves på plattformnivå med Kubernetes NetworkPolicy og mTLS.
- **`no.nav.common.auth` (OidcAuthenticationFilter):** Alle kall mot `/api/*` får token-signatur og -utløp validert av et
  OIDC-filter fra Navs felles `common-java-modules`. Filteret setter autentiseringskonteksten som applikasjonskoden
  bygger videre på.
- **Azure AD og TokenX (Nais):** Nais provisjonerer og roterer applikasjonshemmeligheter automatisk, og injiserer dem
  som miljøvariabler i containeren (`AZURE_APP_CLIENT_ID`, `AZURE_APP_CLIENT_SECRET`,
  `AZURE_OPENID_CONFIG_TOKEN_ENDPOINT`). Applikasjonen leser disse ved oppstart for å hente token på vegne av
  veiledere (OBO) og som systembruker (M2M).
- **`poao-tilgang`:** All tilgangskontroll for veileders tilgang til en bestemt bruker og NAV-enhet er delegert hit.
  Applikasjonen gjør policyoppslag og respekterer avgjørelsen.
- **Token-claim-validering (`AuthService`):**
    - Veileder: `UserRole.INTERN` kreves; `oid`-claim brukes som veileder-UUID mot poao-tilgang; `NAVident` som
      veilederidentifikator
    - Systembruker (M2M): `erSystemBruker()` + krav om `access_as_application` i `roles`-claim; spesifikke roller som
      `gjeldende-14a-vedtak` eller `siste-14a-vedtak` kreves for visse endepunkter
    - Innbygger: `UserRole.EKSTERN` via TokenX; `pid`-claim brukes som fødselsnummer; `acr`-claim valideres til `Level4`
    - Ansvarlig veileder-sjekk: Kun veilederen som er registrert som ansvarlig for et vedtak kan utføre visse handlinger
      på det.
- **Enhetstilgangskontroll:** For vedtakshandlinger sjekkes det at veileder har tilgang til brukerens oppfølgingsenhet.

## Brukerkontotyper og tilgangskontroll

| Type            | Eksempel                                    | Autentisering                    | Autorisasjon                                                                                                                       | MFA                   |
|-----------------|---------------------------------------------|----------------------------------|------------------------------------------------------------------------------------------------------------------------------------|-----------------------|
| NAV-veileder    | Veileder som oppretter og fatter vedtak     | Azure AD (`UserRole.INTERN`)     | Delegert til poao-tilgang: tilgang til bruker (lese/skrive) og NAV-enhet; eksplisitt sjekk på ansvarlig veileder i koden           | Ja                    |
| Kvalitetssikrer | Veileder som gjennomfører kollegaveiledning | Azure AD (`UserRole.INTERN`)     | Samme som veileder, pluss eksplisitt sjekk på beslutterrolle i koden                                                               | Ja                    |
| Innbygger       | Person som leser eget vedtak                | TokenX (`UserRole.EKSTERN`)      | Delegert til poao-tilgang: innbygger kan kun se egne data; krever sikkerhetsnivå 4 (`acr=Level4`)                                  | Ja (ID-porten nivå 4) |
| Systembruker    | Intern Nav-tjeneste (M2M)                   | Azure AD M2M (`UserRole.SYSTEM`) | `access_as_application`-rolle i token; spesifikke roller som `gjeldende-14a-vedtak` eller `siste-14a-vedtak` for visse endepunkter | —                     |

## Avhengigheter

Primært er det intern kommunikasjon vha. service discovery (GCP). Noen systemer nås via ingresser - dette er hovedsaklig
snakk om Nav on-prem tjenester. I alle tilfeller brukes on-behalf-of eller machine-to-machine token flow ihht. til Nais
anbefalte patterns (se: https://doc.nav.cloud.nais.io/auth/).

### Innkommende (service discovery)

* veilarbportefoljeflatefs
* veilarbportefolje
* veilarbpersonflate
* inngar
* beslutteroversikt
* mulighetsrommet-api
* poao-admin
* amt-person-service
* modiapersonoversikt-api
* tiltaksgjennomforing-api
* ismeroppfolging
* arbeidssokerregistrering-for-veileder

### Utgående (service discovery)

* egenvurdering-dialog-tjeneste
* logging
* norg2
* paw-arbeidssoekerregisteret-api-oppslag-v2
* poao-tilgang
* pto-pdfgen
* veilarboppfolging
* veilarbperson
* veilarbveileder
* kabal-api

### Utgående (ingress)

* dokarkiv
* dokdistfordeling
* dokdistkanal
* pdl-api
* regoppslag
* saf
* obo-unleash-api
* veilarbarena

## Secrets-håndtering

- **Azure AD og TokenX:** Nais provisjonerer og roterer applikasjonshemmeligheter automatisk. Tilgjengelig som
  miljøvariabler i containeren — applikasjonen leser dem ved oppstart.
- **Database (PostgreSQL):** Nais injiserer tilkoblingsopplysninger (`DB_*`) via Cloud SQL Auth Proxy og Workload
  Identity. Rotasjon håndteres av GCP og Nais.
- **Kafka Schema Registry:** Nais injiserer hemmeligheter (`KAFKA_SCHEMA_REGISTRY_USER`,
  `KAFKA_SCHEMA_REGISTRY_PASSWORD`) som miljøvariabler.
- **Unleash API-token:** Administrert av Nais Unleash-operatøren som en `ApiToken`-ressurs. Hemmeligheten opprettes og
  roteres automatisk av plattformen og gjøres tilgjengelig som miljøvariabel `veilarbvedtaksstotte-unleash-api-token`.
- **Sertifikater og mTLS:** Kafka-sertifikater og intern cluster-kommunikasjon håndteres av Nais/Istio — ingen manuell
  sertifikathåndtering.

## Logging og monitorering

- **Auditlogg:** Logges i CEF-format via `no.nav.common.audit-log` til en dedikert auditlog-appender. Logger veilederens
  NAV-ident som aktør og brukerens fødselsnummer som ressurs. Logges kun for interne brukere (NAV-ansatte).
  Fødselsnummer inngår som tiltenkt i auditloggen — CEF-formatet er beregnet på dette — men logges ikke i den ordinære
  applikasjonsloggen. Auditlogging skjer ved følgende hendelser:
    - Henting av vedtaksutkast for en person
    - Henting av fattede vedtak for en person
    - Henting av gjeldende § 14 a-vedtak
    - Henting av siste § 14 a-vedtak
    - Henting av PDF-versjon av fattet vedtak
    - Henting av øyeblikksbilder (CV, arbeidssøkerregistrering, egenvurdering) knyttet til et vedtak
    - Henting av PDF-versjon av øyeblikksbilder
- **Applikasjonslogg:** JSON-format via Logback til stdout, videresendt til Elastic og Loki av Nais.
- **Metrikker og varsling:** Prometheus-metrikker eksponert på `/internal/prometheus`. Egne forretningsmetrikker
  overvåkes av PrometheusRule-varsler:
    - Applikasjon nede (0 tilgjengelige replicas) → kritisk
    - Høy andel HTTP 5XX (> 1 % over 5 min) → kritisk
    - Fattet vedtak ikke journalført → kritisk
    - Journalførte dokumenter ikke distribuert → kritisk
    - Distribusjon av journalpost feilet → kritisk
    - Høy andel HTTP 4XX (> 10 % over 5 min) → advarsel
- **Tracing:** OpenTelemetry auto-instrumentering aktivert av Nais.
- **Oppbevaringstid logger:** Ukjent (ikke spesifisert av Nais)

## Datasikring

- **Kryptering under transport:** All kommunikasjon bruker HTTPS og mTLS. Se [doc.nais.io](https://doc.nais.io) for
  gjeldende detaljer om nettverkssikkerhet, ingress-kontrollere og cluster-infrastruktur.
- **Kryptering i ro:** PostgreSQL og BigQuery krypteres av GCP. Se [doc.nais.io](https://doc.nais.io) for gjeldende
  oppsett.
- **Tilgangskontroll til datalagrene:**
    - Database: Kun applikasjonen selv (via Cloud SQL Auth Proxy) og en dedikert BigQuery-eksportbruker har
      databasetilgang. Styres av GCP IAM og Nais Workload Identity.
    - BigQuery: Applikasjonen har lese- og skrivetilgang til eget datasett (`14a_vedtak_statistikk`). Tilgang til views
      for andre team styres vha. egne tilganger.
- **HA og backup:**
    - 2–4 replicas i produksjon sikrer tilgjengelighet ved pod-feil
    - GCP CloudSQL tar automatisk backup nattlig kl. 03:00 (standard Nais-oppsett). 7 sikkerhetskopier beholdes som
      standard. I tillegg kjøres daglig fullstendig sikkerhetskopi til on-prem kl. 05:00 som katastrofeberedskap.
      Se [doc.nais.io](https://doc.nais.io) for gjeldende detaljer.

## Gjenoppretting

| Scenario                         | Prosedyre                                                                                                                                                         | Ansvarlig                      |
|----------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------|
| Kodefeil i prod                  | Som regel forsøkes det å fikse feil og ta ut ny deploy. Revert benyttes kun ved kritiske feil eller omfattende fikser som ikke lar seg løse innenfor rimelig tid. | Team OBO (innenfor arbeidstid) |
| Vedtak ikke journalført          | Automatisk nytt forsøk hvert 10. minutt. Ved vedvarende feil: undersøk scheduler og Dokarkiv-tilgang                                                              | Team OBO (innenfor arbeidstid) |
| Datatap i database               | Gjenopprett fra GCP CloudSQL-backup. Se [doc.nais.io](https://doc.nais.io) for fremgangsmåte                                                                      | Team OBO + Nais-plattform      |
| Kompromittert hemmelighet        | Roter hemmelighet i Nais/Azure AD, re-deploy                                                                                                                      | Team OBO + sikkerhetskontakt   |
| Tap av Azure AD-tilganger        | Gjenoppretting via Azure AD-administrasjon                                                                                                                        | IT-support                     |
| Feil i saksstatistikk (BigQuery) | Resending via `SakStatistikkResendingService`. Varsle Team Sak og Team Oppfølging for verifisering                                                                | Team OBO (innenfor arbeidstid) |

## Referanser

* [Nais-dokumentasjon](https://doc.nais.io)
* [sikkerhet.nav.no](https://sikkerhet.nav.no)
* [veilarbvedtaksstotte (koderepository) - GitHub](https://github.com/navikt/veilarbvedtaksstotte)
* [veilarbvedtaksstottefs (koderepository) - GitHub](https://github.com/navikt/veilarbvedtaksstottefs)
* [beslutteroversikt (koderepository) - GitHub](https://github.com/navikt/beslutteroversikt)
* [veilarbpersonflatefs (koderepository) - GitHub](https://github.com/navikt/veilarbpersonflatefs)
* [poao-tilgang (koderepository) - GitHub](https://github.com/navikt/poao-tilgang)
* [common-java-modules (koderepository) - GitHub](https://github.com/navikt/common-java-modules)
