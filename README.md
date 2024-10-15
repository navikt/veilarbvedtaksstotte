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

## Kode generert av GitHub Copilot

Dette repoet bruker GitHub Copilot til å generere kode.
