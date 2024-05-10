# Veilarbvedtaksstotte

Backend-tjeneste for fatting av 14a vedtak.

Funksjonalitet:

- Opprette og oppdatere utkast til 14a vedtak
- Fatte 14a vedtak med journalføring og distribusjon av brev
- API for kvalitetssikringsprosess, oversikt og søk
- API for meldinger relatert til et utkast
- Integrert funksjonalitet og API for utrulling av løsningen
- API for konsumering av data fra 14a vedtak (innsatsgruppe og hovedmål)

Swagger: /veilarbvedtaksstotte/internal/swagger-ui/index.html

## DB Creds

Dev: 
```console
vault read postgresql/preprod-fss/creds/veilarbvedtaksstotte-fss15-q1-admin
```

Prod: 
```console
vault read postgresql/prod-fss/creds/veilarbvedtaksstotte-fss13-p-readonly
```

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