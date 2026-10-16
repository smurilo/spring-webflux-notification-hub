# WebFlux Notification Hub

Hub **reativo e multi-tenant** de notificações (SMS, e-mail e push) para hospitais e órgãos públicos: confirmação de consultas, códigos de acesso e atualização de protocolos. Autenticação via **Keycloak (OAuth2/JWT)**, fila priorizada no **RabbitMQ**, **rate limit distribuído no Redis** e **observabilidade completa** com OpenTelemetry, Jaeger, Prometheus e Grafana.

> Segmentos: **Saúde / Setor Público**

## Stack
Java 21 • Spring Boot 3.4 • **Spring WebFlux / Project Reactor** • Spring Security OAuth2 Resource Server • **Keycloak** • **RabbitMQ** (prioridade + DLQ + retry) • **Redis reativo** • Micrometer • **OpenTelemetry → Jaeger** • **Prometheus + Grafana** • Server-Sent Events • Maven • Docker Compose • JUnit 5 • WebTestClient • Mockito

## Fluxo

```
 cliente ──JWT──► POST /api/notifications ──► rate limit (Redis, por tenant) ──► RabbitMQ (fila com prioridade)
                                                     │ 429 se exceder                    │
                                                                                         ▼
 GET /api/notifications/stream (SSE) ◄── status em tempo real ◄── NotificationDispatcher ──► SMS | E-mail | Push
                                                                         │ erro permanente
                                                                         └──► DLQ (notifications.dispatch.dlq)
```

### Destaques
- **Tenant extraído do token**, nunca do payload: um cliente não consome a cota de outro.
- **Rate limit distribuído** (janela fixa no Redis): o limite vale com várias instâncias.
- **Prioridade na fila**: códigos de acesso (`HIGH`) passam na frente de lembretes.
- **Retry com backoff exponencial** para falhas transitórias e **DLQ** para erros permanentes (template inválido).
- **Não bloqueia o event loop**: o `RabbitTemplate` (bloqueante) roda em `boundedElastic`.
- **Métricas de negócio** (`notifications.sent`/`failed` por canal e tenant) e **tracing distribuído**.

## Como executar
```bash
docker compose up --build

TOKEN=$(curl -s -d grant_type=client_credentials -d client_id=hospital-a -d client_secret=hospital-a-secret \
  localhost:8180/realms/notify/protocol/openid-connect/token | jq -r .access_token)

curl -N -H "Authorization: Bearer $TOKEN" localhost:8080/api/notifications/stream &   # acompanha os status

curl -X POST localhost:8080/api/notifications -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"channel":"SMS","recipient":"+5582999990000","template":"consulta-confirmada","priority":"NORMAL",
       "variables":{"nome":"Ana","especialidade":"Cardiologia","data":"12/11 às 9h"}}'
```
Painéis: RabbitMQ `:15672` (guest/guest) • Jaeger `:16686` • Prometheus `:9090` • Grafana `:3000`.

## Testes
```bash
mvn test   # renderização de templates, segurança por scope, rate limit (429) e publicação na fila
```
