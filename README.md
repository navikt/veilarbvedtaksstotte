# Veilarbvedtaksstotte

## Setup for å kjøre lokalt
1. Installer docker-compose
2. Endre i /etc/hosts slik at "kafka" peker mot "localhost"

## Kjøre appen
```console
# bygge
mvn clean install 

# test
mvn test

# starte
# Kjør main-metoden i no.nav.veilarbvedtaksstotte.VeilarbvedtaksstotteApp.java
# For lokal test kjøring kjør no.nav.veilarbvedtaksstotte.VeilarbvedtaksstotteTestApp.java
# For å kjøre lokalt så husk å starte docker tjenestene først med "docker-compose up -d"
```