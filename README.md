# Veilarbvedtaksstotte

Backend-tjeneste for fatting av 14a vedtak.  

Funksjonalitet:

- Opprette og oppdatere utkast til 14a vedtak
- Fatte 14a vedtak med journalføring og distribusjon av brev
- API for kvalitetssikringsprosess, oversikt og søk
- API for meldinger relatert til et utkast
- API for konsumering av data fra 14a vedtak (innsatsgruppe og hovedmål)

Swagger: [/veilarbvedtaksstotte/internal/swagger-ui/index.html](https://veilarbvedtaksstotte.intern.dev.nav.no/veilarbvedtaksstotte/internal/swagger-ui/index.html)

Dev-miljøet har ikke lenger noen tilknytning til q1, kun q2.

## DB Creds


## Kjøre appen

```console
# bygge
mvn clean install 

# test
mvn test

# starte
# Kjør main-metoden i no.nav.veilarbvedtaksstotte.VeilarbvedtaksstotteApp.java
# For lokal test kjøring kjør no.nav.veilarbvedtaksstotte.VeilarbvedtaksstotteTestApp.java
```

## Journalføring detaljer

Sjekk dokumentasjon [her](Journalforing.md)

## Utprøving av tilgangsmaskinen

Unleash-bryteren `veilarbvedtaksstotte.tilgangsmaskin_integrasjon` styrer de nye
tilgangsendringene. Når den er av eller ikke finnes, brukes tidligere søk,
sensurering og kontroll av enhetstilgang. De nye endepunktene
`/api/beslutteroversikt/enheter` og `/api/v1/tilgang/*` svarer da med 404.

Når bryteren er på, kan søket i dev hente utkast fra valgte enheter uten
enhetstilgang. Persontilgang vurderes med kjerneregler, og oppfølgingstilgang,
adressebeskyttelse og skjerming kontrolleres fortsatt. Vikafossen krever kode 6-tilgang.
Den midlertidige utelatelsen av enhetstilgangskontrollen i `AuthService` aktiveres
også utenfor produksjon. Enhetslisten er fortsatt begrenset til dev.
`/api/v1/tilgang/*` krever i tillegg `TILGANGSMASKIN_ENKELTTILGANG_ENABLED=true`.

## Kode generert av GitHub Copilot

Dette repoet bruker GitHub Copilot til å generere kode.
