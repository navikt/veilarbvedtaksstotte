# gjør det mulig å bytte base-image slik at vi får bygd både innenfor og utenfor NAV
ARG BASE_IMAGE_PREFIX=""
FROM ${BASE_IMAGE_PREFIX}maven as builder

ADD / /source
WORKDIR /source
RUN mvn package -DskipTests

FROM navikt/java:8-appdynamics
COPY --from=builder /source/target/veilarbvedtaksstotte /app