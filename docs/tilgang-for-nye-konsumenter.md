# Tilgang til data

Send oss denne tabellen ferdig utfylt:

| App-navn | Cluster | Namespace | Lenke til behandling i Behandlingskatalogen | Hva skal dataene brukes til? |
| --- | --- | --- | --- | --- |
| `<app-navn>` | `<cluster>` | `<namespace>` | `<lenke>` | `<kort beskrivelse>` |

Eksempel:

| App-navn | Cluster | Namespace | Lenke til behandling i Behandlingskatalogen | Hva skal dataene brukes til? |
| --- | --- | --- | --- | --- |
| min-app | prod-gcp | mitt-team | `<lenke til behandlingen>` | Vise veilederen innsatsgruppen fra brukerens gjeldende vedtak. |

## Hvor legges tilgangen inn?

| Type | Sted |
| --- | --- |
| API | `accessPolicy.inbound.rules` i [produksjonsmanifestet](../.nais/application/application-config-prod.yaml), og ved behov [utviklingsmanifestet](../.nais/application/application-config-dev.yaml). |
| Kafka | `spec.acl` i topicets fil under `kafka/prod` eller `kafka/dev` i [navikt/pto-iac](https://github.com/navikt/pto-iac). |
| BigQuery | I GCP. |

## Oppdater oversikten

Den som legger inn tilgangen, oppdaterer også [konsumentoversikten](konsument-oversikt.md):

- Legg inn app-navn, lenke til behandlingen og kort beskrivelse av databruken.
- Oppgi topic eller visning hvis det gjelder Kafka eller BigQuery.
- Sett «Sist oppdatert» til dagens dato (`YYYY-MM-DD`).
- Under «Tilgang gitt i»: lenk til tilgangsregelen med publisert commit og linjenumre,
  eller skriv `Definert i GCP` for BigQuery.
