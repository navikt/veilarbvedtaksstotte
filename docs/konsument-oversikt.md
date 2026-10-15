---
ai_instructions: |
  Formål: Vedlikehold en oversikt over hvem som bruker data fra veilarbvedtaksstotte,
  hva dataene brukes til, og hvor tilgangen er gitt.

  Ved lesing:
  - Les hele fila, inkludert forbehold, før du tolker eller endrer en oppføring.
  - Skill mellom tildelt tilgang, databruk bekreftet i kildekode og bekreftet kjøring
    i produksjon. En tilgangsregel eller en klient i koden beviser ikke aktiv bruk.
  - "Sist oppdatert" gjelder oppføringen, ikke tidspunktet da tilgangen ble gitt.

  Ved oppdatering:
  - Behold inndelingen i API, Kafka og BigQuery og de eksisterende tabellkolonnene.
  - Kartlegg API-tilgang fra produksjonsmanifestets accessPolicy.inbound og
    Kafka-lesetilgang fra produksjons-ACL-ene i navikt/pto-iac/kafka/prod.
  - Identifiser applikasjoner med metadata.name i Nais-manifest, ikke bare reponavn.
    Følg eventuelle malvariabler til produksjonsverdiene.
  - Undersøk relevante repoer på GitHub; ikke forutsett at de finnes lokalt.
  - Følg videresendte data frem til konkret bruk. Beskriv formålet kort i selve
    tabellen, ikke bare transporten eller en detaljert teknisk kjede utenfor tabellen.
  - Marker usikkerhet uttrykkelig. Ikke fremstill lesetilgang som bekreftet bruk.
  - Bruk dagens dato i formatet YYYY-MM-DD for oppføringer du oppdaterer.
    Ikke endre datoen på urørte oppføringer.
  - "Tilgang gitt i" skal vise selve tilgangsregelen, ikke klientkode eller lesekode.
    Bruk GitHub-lenker med en bekreftet, publisert commit-SHA og riktige linjenumre.
    Ved nye eller endrede lenker, bruk siste tilgjengelige publiserte revisjon av
    den relevante fila. Ikke bruk lokale upubliserte commit-er eller lenker til main.
  - BigQuery-tilgangene er definert i GCP. Behold "Definert i GCP" med mindre en
    konkret tilgangsregel kan dokumenteres.
  - Behold kjente lenker til behandlinger i Behandlingskatalogen. Ikke gjett nye
    koblinger; bruk "Ikke oppgitt" når relevant behandling er ukjent.
  - Skriv korte og lettfattelige beskrivelser på bokmål. Ikke legg til en
    "Kilder"-kolonne. Bevar øvrige oppføringer og brukerens endringer.
---

# Konsumentoversikt

Oversikt over konsumenter av data som veilarbvedtaksstotte eksponerer, lenke til relevant behandling i
Behandlingskatalogen og kort beskrivelse av hvordan data brukes.

«Sist oppdatert» angir når oppføringen sist ble oppdatert, ikke når tilgangen ble gitt.
«Tilgang gitt i» peker til tilgangsregelen der den er funnet.

## API

| Konsument | Beskrivelse av bruk | Lenke til behandling | Sist oppdatert | Tilgang gitt i |
| --- | --- | --- | --- | --- |
| veilarbportefolje | Filtrering og visning av data om en person har/ikke har utkast til oppfølgingsvedtak, har/ikke har gjeldende oppfølgingsvedtak, status på utkast, innsatsgruppe, hovedmål. | https://behandlingskatalog.intern.nav.no/process/team/b8dac7f7-2ffb-4b85-a536-644487e53758/809966f6-118e-43ed-8625-d81d0c40ad37 | 2026-10-15 | [Nais-regel](https://github.com/navikt/veilarbvedtaksstotte/blob/454caf3ba23b88cf608cfb89cbd92ef74e556a4f/.nais/application/application-config-prod.yaml#L150-L154) |
| veilarbpersonflate | Visning av etikett i "personlinja/visittkortet" som sier om en person trenger oppfølgingsvedtak (dvs. ikke har et gjeldende et). Sjekk på om person har utkast til oppfølgingsvedtak og vise melding om dette dersom veileder velger å avslutte oppfølging. | https://behandlingskatalog.intern.nav.no/process/team/b8dac7f7-2ffb-4b85-a536-644487e53758/33ed694a-0861-4bd8-a1d4-3c9c54e30a5a | 2026-10-15 | [Nais-regel](https://github.com/navikt/veilarbvedtaksstotte/blob/454caf3ba23b88cf608cfb89cbd92ef74e556a4f/.nais/application/application-config-prod.yaml#L155-L156) |
| inngar | Viser Visittkort-komponenten. `veilarbvisittkortfs` henter gjeldende § 14 a-vedtak via Inngars proxy og viser etiketten «Trenger oppfølgingsvedtak § 14 a» når personen er under oppfølging uten vedtak. | Ikke oppgitt | 2026-10-15 | [Nais-regel](https://github.com/navikt/veilarbvedtaksstotte/blob/454caf3ba23b88cf608cfb89cbd92ef74e556a4f/.nais/application/application-config-prod.yaml#L157-L158) |
| beslutteroversikt | Viser en oversikt over brukere under kvalitetssikring av § 14 a-vedtak. Søk kan filtreres på enhet, status, veileder og navn eller fødselsnummer. | Ikke oppgitt | 2026-10-15 | [Nais-regel](https://github.com/navikt/veilarbvedtaksstotte/blob/454caf3ba23b88cf608cfb89cbd92ef74e556a4f/.nais/application/application-config-prod.yaml#L159-L160) |
| mulighetsrommet-api | Henter gjeldende § 14 a-vedtak som del av brukerdata i veilederflaten. Hvis brukeren mangler vedtak, vises et varsel om at hen ikke kan meldes på tiltak. | Ikke oppgitt | 2026-10-15 | [Nais-regel](https://github.com/navikt/veilarbvedtaksstotte/blob/454caf3ba23b88cf608cfb89cbd92ef74e556a4f/.nais/application/application-config-prod.yaml#L161-L162) |
| poao-admin | Støtter administrasjon av § 14 a-vedtak: oppslag, sletting eller sladding av vedtak og republisering av vedtak eller hendelser. | Ikke oppgitt | 2026-10-15 | [Nais-regel](https://github.com/navikt/veilarbvedtaksstotte/blob/454caf3ba23b88cf608cfb89cbd92ef74e556a4f/.nais/application/application-config-prod.yaml#L163-L165) |
| amt-person-service | Har en HTTP-klient som henter innsatsgruppen fra gjeldende vedtak og oversetter den til tjenestens eldre V1-format. Et produksjonskallpunkt for HTTP-klienten er ikke funnet. Kafka-konsumeringen er beskrevet nedenfor. | Ikke oppgitt | 2026-10-15 | [Nais-regel](https://github.com/navikt/veilarbvedtaksstotte/blob/454caf3ba23b88cf608cfb89cbd92ef74e556a4f/.nais/application/application-config-prod.yaml#L166-L170) |
| modiapersonoversikt-api | Eksponerer gjeldende innsatsgruppe, hovedmål og vedtaksdato for oppfølgingsdelen av personoversikten. | https://behandlingskatalog.intern.nav.no/process/team/7ec4b786-ddf7-4c56-ab2f-3c3edeb0d33f/4ff22ca5-c66a-4127-9271-9de34800c8dd | 2026-10-15 | [Nais-regel](https://github.com/navikt/veilarbvedtaksstotte/blob/454caf3ba23b88cf608cfb89cbd92ef74e556a4f/.nais/application/application-config-prod.yaml#L172-L173) |
| tiltaksgjennomforing-api | Bruker innsatsgruppen sammen med oppfølgingsstatus for å sjekke om deltakeren kvalifiserer for valgt tiltakstype. Sommerjobb er unntatt fra denne kontrollen. | https://behandlingskatalog.ansatt.nav.no/process/team/0150fd7c-df30-43ee-944e-b152d74c64d6/780e3168-cf58-46ec-9764-a6cdd3da3b8f | 2026-10-15 | [Nais-regel](https://github.com/navikt/veilarbvedtaksstotte/blob/454caf3ba23b88cf608cfb89cbd92ef74e556a4f/.nais/application/application-config-prod.yaml#L175-L180) |
| ismeroppfolging | Sjekker om personen allerede har et gjeldende § 14 a-vedtak når kandidater til kartleggingsspørsmål velges. Personer med vedtak blir ikke kandidater. | https://behandlingskatalog.intern.nav.no/process/team/02e803bf-0114-4ad2-8871-685b5c3035ba/60868e2d-b151-47a3-a1a6-1d896fc3a88d | 2026-10-15 | [Nais-regel](https://github.com/navikt/veilarbvedtaksstotte/blob/454caf3ba23b88cf608cfb89cbd92ef74e556a4f/.nais/application/application-config-prod.yaml#L182-L187) |
| arbeidssokerregistrering-for-veileder | Viser Visittkort-komponenten. `veilarbvisittkortfs` henter gjeldende § 14 a-vedtak via appens proxy og viser etiketten «Trenger oppfølgingsvedtak § 14 a» når personen er under oppfølging uten vedtak. | Ikke oppgitt | 2026-10-15 | [Nais-regel](https://github.com/navikt/veilarbvedtaksstotte/blob/454caf3ba23b88cf608cfb89cbd92ef74e556a4f/.nais/application/application-config-prod.yaml#L188-L192) |

## Kafka

Oversikten tar utgangspunkt i lesetilgangene i produksjons-ACL-ene i
[pto-iac/kafka/prod][kafka-acl]. Lesetilgang betyr ikke nødvendigvis at applikasjonen faktisk konsumerer
meldinger nå. Der bare tilgang eller et dokumentert formål er funnet, er dette angitt.
Applikasjonsnavn er kontrollert mot `metadata.name` i Nais-manifest der slike manifest er funnet.

| Konsument | Topic | Beskrivelse av bruk | Lenke til behandling | Sist oppdatert | Tilgang gitt i |
| --- | --- | --- | --- | --- | --- |
| veilarbportefolje | `pto.siste-14a-vedtak-v1`, `pto.vedtak-14a-statusendring-v1` | Har lesetilgang til begge topicene. Koden for siste vedtak lagrer innsatsgruppe, hovedmål og vedtaksdato og oppdaterer søkeindeksen for brukere under oppfølging. Dette understøtter filtrering og visning i porteføljen. | https://behandlingskatalog.intern.nav.no/process/team/b8dac7f7-2ffb-4b85-a536-644487e53758/809966f6-118e-43ed-8625-d81d0c40ad37 | 2026-10-15 | [Siste vedtak](https://github.com/navikt/pto-iac/blob/ff99244c491bb939631f60bf18c4f05285355c6f/kafka/prod/siste-14a-vedtak-v1-topic.yaml#L19-L21), [Statusendring](https://github.com/navikt/pto-iac/blob/ff99244c491bb939631f60bf18c4f05285355c6f/kafka/prod/vedtak-14a-statusendring-v1-topic.yaml#L20-L22) |
| amt-person-service | `pto.siste-14a-vedtak-v1` | Leser innsatsgruppe og aktør-ID. Finner personen og oppdaterer innsatsgruppen for eksisterende Nav-brukere i tjenesten for Deltakeroversikten. Dette er Kafka-bruk, i tillegg til API-klienten omtalt ovenfor. | Ikke oppgitt | 2026-10-15 | [Siste vedtak](https://github.com/navikt/pto-iac/blob/ff99244c491bb939631f60bf18c4f05285355c6f/kafka/prod/siste-14a-vedtak-v1-topic.yaml#L31-L33) |
| toi-siste-14a-vedtak | `pto.siste-14a-vedtak-v1` | Sender siste vedtak videre til TOI. Innsatsgruppe og hovedmål lagres i kandidatindeksen og brukes til filtrering av kandidatsøk i Rekrutteringsbistand, ikke til å avgjøre kandidatens synlighet. | Ikke oppgitt | 2026-10-15 | [Siste vedtak](https://github.com/navikt/pto-iac/blob/ff99244c491bb939631f60bf18c4f05285355c6f/kafka/prod/siste-14a-vedtak-v1-topic.yaml#L28-L30) |
| paw-microfrontend-toggler | `pto.siste-14a-vedtak-v1` | Har lesetilgang. README beskriver bruk av fattet vedtak til å deaktivere behovsvurderingskomponenten `aia-behovsvurdering` på Min side. Aktiv konsumering av vedtak er ikke bekreftet i den undersøkte produksjonskoden. | Ikke oppgitt | 2026-10-15 | [Siste vedtak](https://github.com/navikt/pto-iac/blob/ff99244c491bb939631f60bf18c4f05285355c6f/kafka/prod/siste-14a-vedtak-v1-topic.yaml#L34-L36) |
| paw-arbeidssoekerregisteret-api-mine-stillinger | `pto.siste-14a-vedtak-v1` | Bruker innsatsgruppe og vedtaksdato til å oppdatere flagget for standard innsats i en eksisterende brukerprofil. Oppdateringen krever en aktiv arbeidssøkerperiode og et vedtak nyere enn profileringen. | Ikke oppgitt | 2026-10-15 | [Siste vedtak](https://github.com/navikt/pto-iac/blob/ff99244c491bb939631f60bf18c4f05285355c6f/kafka/prod/siste-14a-vedtak-v1-topic.yaml#L37-L39) |
| arena-innsatsbehov | `pto.vedtak-sendt-v1` | Oppdaterer innsatsbehov i Arena med innsatsgruppe, enhet og hovedmål fra vedtakshendelsen. | Ikke oppgitt | 2026-10-15 | [Sendt vedtak](https://github.com/navikt/pto-iac/blob/ff99244c491bb939631f60bf18c4f05285355c6f/kafka/prod/vedtak-sendt-v1-topic.yaml#L20-L22) |
| dvh-kartlegging-konsument | `pto.vedtak-14a-fattet-dvh-v1` | Har lesetilgang til hendelser om fattede vedtak. Eget manifest og aktiv konsumering er ikke bekreftet. Arbeidsflyten for `dvh_arb_kartlegging` omtaler en gammel Kafka-konsument som avslått og bruker nå en BigQuery-innlasting; det er ikke bekreftet om kommentaren gjelder denne identiteten. | Ikke oppgitt | 2026-10-15 | [DVH](https://github.com/navikt/pto-iac/blob/ff99244c491bb939631f60bf18c4f05285355c6f/kafka/prod/vedtak-14a-fattet-dvh-v1.yaml#L26-L28) |
| teamoppfolging-kafka | `pto.vedtak-14a-fattet-dvh-v1` | Teknisk Kafka-identitet definert som `AivenApplication`, ikke en egen kjørende applikasjon. Identiteten gir Team oppfølging Kafka-tilgang. Hvilken jobb som eventuelt bruker den til å lese dette topicet, er ikke bekreftet. | Ikke oppgitt | 2026-10-15 | [DVH](https://github.com/navikt/pto-iac/blob/ff99244c491bb939631f60bf18c4f05285355c6f/kafka/prod/vedtak-14a-fattet-dvh-v1.yaml#L29-L31) |
| pto-kafka-manager | `pto.siste-14a-vedtak-v1`, `pto.vedtak-14a-statusendring-v1`, `pto.vedtak-sendt-v1`, `pto.vedtak-14a-fattet-dvh-v1` | Har teknisk lesetilgang i ACL-ene. Konkret bruk og aktiv konsumering er ikke bekreftet. | Ikke oppgitt | 2026-10-15 | [Siste vedtak](https://github.com/navikt/pto-iac/blob/ff99244c491bb939631f60bf18c4f05285355c6f/kafka/prod/siste-14a-vedtak-v1-topic.yaml#L22-L24), [Statusendring](https://github.com/navikt/pto-iac/blob/ff99244c491bb939631f60bf18c4f05285355c6f/kafka/prod/vedtak-14a-statusendring-v1-topic.yaml#L23-L25), [Sendt vedtak](https://github.com/navikt/pto-iac/blob/ff99244c491bb939631f60bf18c4f05285355c6f/kafka/prod/vedtak-sendt-v1-topic.yaml#L23-L25), [DVH](https://github.com/navikt/pto-iac/blob/ff99244c491bb939631f60bf18c4f05285355c6f/kafka/prod/vedtak-14a-fattet-dvh-v1.yaml#L20-L22) |
| obo-kafka-manager | `pto.siste-14a-vedtak-v1`, `pto.vedtak-14a-statusendring-v1`, `pto.vedtak-sendt-v1`, `pto.vedtak-14a-fattet-dvh-v1`, `obo.gjeldende-14a-vedtak-v1` | Har teknisk lesetilgang i ACL-ene. Er eneste leser med tilgang til `obo.gjeldende-14a-vedtak-v1` i den undersøkte ACL-en. Konkret bruk og aktiv konsumering er ikke bekreftet. | Ikke oppgitt | 2026-10-15 | [Siste vedtak](https://github.com/navikt/pto-iac/blob/ff99244c491bb939631f60bf18c4f05285355c6f/kafka/prod/siste-14a-vedtak-v1-topic.yaml#L25-L27), [Statusendring](https://github.com/navikt/pto-iac/blob/ff99244c491bb939631f60bf18c4f05285355c6f/kafka/prod/vedtak-14a-statusendring-v1-topic.yaml#L26-L28), [Sendt vedtak](https://github.com/navikt/pto-iac/blob/ff99244c491bb939631f60bf18c4f05285355c6f/kafka/prod/vedtak-sendt-v1-topic.yaml#L26-L28), [DVH](https://github.com/navikt/pto-iac/blob/ff99244c491bb939631f60bf18c4f05285355c6f/kafka/prod/vedtak-14a-fattet-dvh-v1.yaml#L23-L25), [Gjeldende vedtak](https://github.com/navikt/pto-iac/blob/ff99244c491bb939631f60bf18c4f05285355c6f/kafka/prod/obo/gjeldende-14a-vedtak-v1-topic.yaml#L19-L21) |
| veilarbvedtaksstotte (intern konsumering) | `pto.vedtak-14a-statusendring-v1` | Leser egne statusendringer for å rapportere asynkront til Datavarehuset. Ved `VEDTAK_SENDT` publiseres en melding på `pto.vedtak-14a-fattet-dvh-v1`. | Ikke oppgitt | 2026-10-15 | [Statusendring](https://github.com/navikt/pto-iac/blob/ff99244c491bb939631f60bf18c4f05285355c6f/kafka/prod/vedtak-14a-statusendring-v1-topic.yaml#L17-L19) |

## BigQuery

Følgende jobber er funnet i GitHub-kode som leser visningene i
`obo-prod-fc62.14a_vedtak_statistikk`. Dette er datavarehusjobber og overvåkingskode, ikke
nødvendigvis egne Nais-applikasjoner. Koden viser databruken, men bekrefter ikke at jobbene kjører i
produksjon nå. Oversikten er ikke en fullstendig kartlegging av BigQuery-tilganger eller spørringslogger.
Tilgangene er definert i GCP.
Databruken er dokumentert i
[innlastingen for oppfølgingsstatistikk][bq-oppfolging],
[innlastingskonfigurasjonen for saksbehandlingsstatistikk][bq-sak] og
[datakvalitetsrapporten][bq-kvalitet-kode].

| Konsument / jobb | Visning | Beskrivelse av bruk | Lenke til behandling | Sist oppdatert | Tilgang gitt i |
| --- | --- | --- | --- | --- | --- |
| `dvh_arb_kartlegging` / `deltalast_14a_vedtak` | `statistikk_dvh_oppfolging` | Leser nye endringer med blant annet aktør-ID, innsatsgruppe, hovedmål, vedtaksdato, oppfølgingsperiode, enhet og behandlingsstatus. Laster dem til Oracle-tabellen `dvh_arb_kartlegging.fk_hist_fattet_vedtak_14a` som grunnlag for statistikk og styringsinformasjon om oppfølging av arbeidssøkere. Airflow-arbeidsflyten har `SCHEDULE = None`; eventuell ekstern planlegging er ikke bekreftet. | Ikke oppgitt | 2026-10-15 | Definert i GCP |
| `p14a-bql` i datavarehuskomponenten for saksbehandlingsstatistikk | `statistikk_dvh_sak` | Konfigurasjonen definerer innlasting til Oracle-tabellen `dvh_bql_sbs.p14a_behandling_l` for saksbehandlingsstatistikk. Omfatter blant annet sak, behandling, status, resultat, tidspunkt og ansvarlig enhet. Dette er navnet i innlastingskonfigurasjonen, ikke et bekreftet Nais-applikasjonsnavn. | Ikke oppgitt | 2026-10-15 | Definert i GCP |
| Team oppfølgings datakvalitetsrapport (`datastory_datakvalitet`) | `statistikk_dvh_oppfolging` | Teller vedtak og personer per behandlingsstatus og sammenligner tallene med Datavarehuset. Lager en rapport som viser forskjeller mellom BigQuery og Datavarehuset. | Ikke oppgitt | 2026-10-15 | Definert i GCP |

[kafka-acl]: https://github.com/navikt/pto-iac/tree/ff99244c491bb939631f60bf18c4f05285355c6f/kafka/prod
[bq-oppfolging]: https://github.com/navikt/dvh_arb_kartlegging/blob/0de30163ac49ea7d0e672f88e33fd91d7d6876fc/etl/deltalast_14a_vedtak.py
[bq-sak]: https://github.com/navikt/dvh_saksbehandlingsstatistikk/blob/ac10c6a1960ac60663783d9d63a37ecf21fd0d62/patch/dvh_bql_sbs/Script/VIP-4255_p14a_konfigurasjon.sql
[bq-kvalitet-kode]: https://github.com/navikt/teamoppfolging-dags/blob/9fe9de0fb93f81b9d2db3c549bd7f8a512f28e8c/overvaaking/datastory_datakvalitet/main.py