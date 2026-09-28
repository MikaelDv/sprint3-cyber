# Observabilidade e monitoramento (Sprint 3 - DevSecOps)

Stack local que gera as evidências da Atividade 1 (logs, métricas, dashboards e alertas):

| Serviço | Endereço | Para quê |
|---------|----------|----------|
| MySQL 8.4 | `localhost:3306` | Banco da API (usuário `carsearch_app`, não root) |
| Prometheus | http://localhost:9090 | Coleta `/actuator/prometheus` da API a cada 10 s |
| Loki + Alloy | `localhost:3100` | Coleta `logs/application.log`, `audit.log` e `security.log` |
| Grafana | http://localhost:3000 (admin / `sprint3`) | 3 dashboards + 10 regras de alerta já provisionados |

## Pré-requisitos (Ubuntu)

```bash
sudo apt install -y docker.io docker-compose-v2 openjdk-21-jdk
sudo usermod -aG docker $USER   # depois faça logout/login
```

## Passo a passo para gerar os prints

Na raiz do projeto:

```bash
# 1. Configuração local da API (usa o MySQL do Docker)
cp .env.example .env
mkdir -p logs                    # precisa existir antes do Alloy montar a pasta

# 2. Banco + monitoramento
cd monitoring && docker compose up -d && cd ..

# 3. API (deixe este terminal aberto)
./mvnw spring-boot:run           # se der "Permissão negada": chmod +x mvnw

# 4. Em outro terminal: tráfego normal + ataques (~3 min)
python3 scripts/simular_ataques.py
```

Aguarde 1 minuto e abra http://localhost:3000 → **Dashboards → Car Search App**.

| Figura | Onde |
|--------|------|
| 1 - Segurança | Dashboard *Car Search App - Segurança* |
| 2 - API | Dashboard *Car Search App - API* |
| 3 - IA e Dados | Dashboard *Car Search App - IA e Dados* |
| 4 - Logs | *Explore* → Loki → `{job="challenge-ford", filename=~".*security.log"}` |
| 5 - Trilha de auditoria | Painel *Trilha de auditoria* do dashboard de Segurança |
| 6 - Alertas | *Alerting → Alert rules* → expanda a pasta *Car Search App* (8 regras em *Firing*) |

Tire os prints nos primeiros 2 minutos depois que o script terminar: os alertas de
janela curta (5 min) voltam a *Normal* em seguida. Para capturá-los de novo, rode o
script outra vez.

### Receber os alertas no Discord (opcional)

Em *Alerting → Contact points*, crie um contact point do tipo **Discord** com a URL
de um webhook do seu servidor e, em *Notification policies*, defina-o como padrão.

## Parar / limpar

```bash
cd monitoring
docker compose down        # para os containers e mantém os dados
docker compose down -v     # apaga também banco, métricas e logs coletados
```

## Segurança

- As senhas deste `docker-compose.yml` e do `.env.example` são só para ambiente local.
- O `/actuator/prometheus` só responde para loopback e redes privadas
  (`security.metrics.allowed-networks`); de fora retorna 401.
- O Grafana acessa o MySQL com o usuário `grafana_ro` (somente SELECT).
