Audit logging av alt som gjøres av menneske i databasen veilarbvedtaksstotte.

1. Konfigurer database-flagg i NAIS-manifestet (Application spec)
   For å aktivere pgAudit på Cloud SQL-instansen, legg til følgende flagg under sqlInstances i applikasjonens YAML-konfigurasjon:

spec:
gcp:
sqlInstances:
- name: veilarbvedtaksstotte
flags:
- name: "cloudsql.enable_pgaudit"
value: "on"
- name: "pgaudit.log"
value: "write,ddl,role" # Eller "all" hvis du også vil logge SELECT-spørringer (lesing)
- name: "pgaudit.log_parameter"
value: "on" # Logger også parametere/verdier som ble sendt inn i spørringene
- name: "pgaudit.log_relation"
value: "on"

Viktig om restart: Endring av disse flaggene vil føre til at Cloud SQL-instansen restarter for at endringene skal tre i kraft. Gjør gjerne dette i et tidsrom med lav trafikk.
Diskplass: pgAudit skriver midlertidig logger til disk før de sendes til Cloud Logging. Det anbefales sterkt å ha Automatic storage increase aktivert på instansen for å unngå at disken går full ved store loggmengder.

2. Kjør denne NAIS CLI-kommandoen i terminal for å provisjonere og aktivere pgAudit-oppsettet i databasen:

nais postgres enable-audit --team obo --environment prod veilarbvedtaksstotte

Dette sørger for at NAIS-plattformen klargjør databasen og oppretter den nødvendige pgaudit-utvidelsen i PostgreSQL.

3. Hvordan skille mellom menneske og maskin (applikasjon)?
   pgAudit sender alle revisjonslogger (audit-logger) direkte til Google Cloud Logging som Data Access-logger.

For å kun se hva et menneske har gjort, bruk filter i Logs Explorer. Siden veilarbvedtaksstotte appen kobler seg til med sin egen dedikerte systembruker, kan du filtrere bort denne systembrukeren for å sitte igjen med manuelle handlinger gjort av utviklere/analytikere via f.eks. IAM-innlogging, Cloud SQL Studio eller personlige brukere:

Du kan søke i Logs Explorer med følgende filter:

logName="projects/obo-prod-fc62/logs/cloudaudit.googleapis.com%2Fdata_access"
resource.type="cloudsql_database"
resource.labels.database_id="obo-prod-fc62:veilarbvedtaksstotte"
NOT protoPayload.request.database_user="veilarbvedtaksstotte"  # Ekskluderer applikasjonens egen bruker

I loggresultatet vil du kunne se nøyaktig:

Hvem som utførte handlingen (protoPayload.request.database_user eller IAM-identitet).
Hva slags SQL-spørring som ble kjørt.
Når det ble utført.