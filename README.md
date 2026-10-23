# OpsPilot --- AI-Assisted SRE Incident Investigation

OpsPilot is a prototype for monitoring microservices, detecting
operational anomalies from Prometheus metrics, and using an AI
investigation workflow to explain incidents and recommend human-reviewed
next steps. It also exposes core SRE operations through a Model Context
Protocol (MCP) server so an MCP-compatible client can call them as
tools.

> **Project status:** Hackathon prototype. The monitoring workload used
> during validation is Spring PetClinic Microservices. The PetClinic
> application is a separate sample workload, not code authored by
> OpsPilot. The Alexa+ experience is a simulation/prototype path, not a
> claim of native Alexa+ device integration.

## What it does

-   **Reads live telemetry:** queries Prometheus for target
    availability, HTTP status counts, request rates, and average/max
    observed latency.
-   **Detects incidents:** creates incidents for unavailable scrape
    targets, elevated HTTP 5xx error rates, elevated HTTP 4xx error
    rates under sufficient traffic, and high average latency.
-   **Deduplicates active incidents:** avoids repeatedly creating the
    same incident while an equivalent incident is already open.
-   **Investigates with Gemini:** summarizes evidence, gives a cautious
    likely-cause assessment, recommends checks/actions, and provides
    verification steps.
-   **Keeps a human in the loop:** AI investigation output includes
    `requiresHumanApproval: true`; it recommends actions rather than
    silently executing remediation.
-   **Provides MCP tools:** exposes health checks, telemetry queries,
    incident detection, incident listing, and incident investigation.

## Architecture

``` text
Spring PetClinic Microservices
   │  actuator/prometheus metrics
   ▼
Prometheus (host port 9091)
   │  PromQL queries
   ▼
OpsPilot Spring Boot API (host port 8081)
   ├── Telemetry API
   ├── Incident detection and deduplication
   ├── Incident REST API
   └── Gemini-powered investigation
          ▲
          │ REST calls
Python MCP server (http://127.0.0.1:8000/mcp)
          │
          ▼
MCP-compatible client / agent

Grafana (host port 3030) can be used to inspect Prometheus metrics.
```

### Main components

  -----------------------------------------------------------------------
  Component                           Responsibility
  ----------------------------------- -----------------------------------
  Spring Boot backend                 Telemetry access, incident
                                      detection, incident storage/API, AI
                                      investigation

  Prometheus                          Scrapes application metrics and
                                      evaluates PromQL queries

  Grafana                             Metrics visualization

  Python MCP server                   Makes OpsPilot capabilities
                                      available as MCP tools

  Gemini API                          Produces structured investigation
                                      analysis

  Spring PetClinic Microservices      Sample microservice workload used
                                      for monitoring and fault scenarios
  -----------------------------------------------------------------------

## Technology

-   Java 21 and Spring Boot (Maven)
-   Prometheus and Grafana
-   Python and the MCP Python SDK
-   Gemini through an OpenAI-compatible API endpoint
-   Spring PetClinic Microservices as the monitored demonstration
    workload

## Repository layout

The project is expected to be organized approximately as follows:

``` text
AmazonBuildShapeHackathon/
├── backend/                         # OpsPilot Spring Boot API
├── mcp-server/
│   ├── server.py                    # MCP server
│   ├── test_mcp.py                  # MCP integration test
│   └── .venv/                       # Local Python virtual environment (not for source control)
├── spring-petclinic-microservices/  # Separate sample monitoring workload
└── README.md
```

Keep secrets, virtual environments, build output, and local environment
files out of source control.

## Prerequisites

-   Java 21
-   Maven (or the Maven wrapper if included in the backend)
-   Python compatible with the installed MCP SDK
-   Docker Desktop / Docker Engine for the sample workload and
    monitoring containers
-   A Gemini API key for AI investigations
-   PowerShell on Windows for the commands below (adapt paths/commands
    for other shells)

## Configuration

Set the Gemini key in the shell used to start the backend. Do not commit
the key or paste it into logs.

``` powershell
$env:GEMINI_API_KEY = "YOUR_GEMINI_API_KEY"
$env:GEMINI_MODEL = "gemini-2.5-flash"
```

The application has used `gemini-2.5-flash` as the model fallback during
validation. If your local configuration already defines the provider,
model, or API base URL, retain that working configuration.

Prometheus is expected to be reachable by the backend at
`http://localhost:9091`. The MCP server calls the OpsPilot backend at
`http://localhost:8081`. The MCP endpoint is
`http://127.0.0.1:8000/mcp`.

## Run locally on Windows

Start each component in its own terminal. Use the existing project
configuration as the source of truth if any local port or command
differs.

### 1. Start the sample workload and monitoring stack

``` powershell
cd D:\AmazonBuildShapeHackathon\spring-petclinic-microservices
docker compose up -d
```

The validated local ports are:

-   PetClinic API gateway: `http://localhost:8080`
-   Prometheus: `http://localhost:9091`
-   Grafana: `http://localhost:3030`

Prometheus should scrape the gateway, customers, visits, vets, and
Prometheus targets. Check target health at
`http://localhost:9091/api/v1/targets`.

### 2. Start the OpsPilot backend

Open another terminal:

``` powershell
cd D:\AmazonBuildShapeHackathon\backend
$env:GEMINI_API_KEY = "YOUR_GEMINI_API_KEY"
$env:GEMINI_MODEL = "gemini-2.5-flash"
mvn spring-boot:run
```

If the project uses a configured Maven wrapper, use
`.\mvnw.cmd spring-boot:run` instead. The backend should be available at
`http://localhost:8081`.

### 3. Start the MCP server

Open another terminal:

``` powershell
cd D:\AmazonBuildShapeHackathon\mcp-server
.\.venv\Scripts\python.exe .\server.py
```

The MCP server listens at `http://127.0.0.1:8000/mcp`. Keep the server
process running while using an MCP client.

### 4. Validate the MCP integration

In another PowerShell terminal:

``` powershell
cd D:\AmazonBuildShapeHackathon\mcp-server
.\.venv\Scripts\python.exe .\test_mcp.py
```

The test script discovers the tools and exercises service health,
telemetry querying, incident detection, incident listing, and AI
investigation. AI investigation requires a valid Gemini key and network
access.

## REST API

The following endpoints were validated during development:

  --------------------------------------------------------------------------------------
  Method                  Endpoint                               Purpose
  ----------------------- -------------------------------------- -----------------------
  `GET`                   `/api/telemetry/summary`               Returns a telemetry
                                                                 summary from Prometheus

  `GET`                   `/api/telemetry/query?expression=up`   Runs a supported
                                                                 read-only Prometheus
                                                                 query

  `GET`                   `/api/incidents`                       Lists recorded
                                                                 incidents

  `POST`                  `/api/incidents/detect`                Runs incident detection

  `POST`                  `/api/incidents/{id}/investigate`      Runs AI investigation
                                                                 for an incident
  --------------------------------------------------------------------------------------

Example PowerShell requests:

``` powershell
Invoke-RestMethod "http://localhost:8081/api/telemetry/summary" |
    ConvertTo-Json -Depth 8

Invoke-RestMethod "http://localhost:8081/api/incidents" |
    ConvertTo-Json -Depth 8

Invoke-RestMethod -Method Post `
    -Uri "http://localhost:8081/api/incidents/detect" |
    ConvertTo-Json -Depth 8

Invoke-RestMethod -Method Post `
    -Uri "http://localhost:8081/api/incidents/2/investigate" |
    ConvertTo-Json -Depth 10
```

Replace incident ID `2` with an ID returned by the incident list.

## MCP tools

The MCP server exposes these tools:

  -----------------------------------------------------------------------
  Tool                                Description
  ----------------------------------- -----------------------------------
  `check_service_health`              Checks monitored target health
                                      using live Prometheus telemetry

  `query_telemetry`                   Runs a read-only Prometheus query
                                      through OpsPilot

  `detect_incidents`                  Detects incidents from current
                                      telemetry

  `list_incidents`                    Lists recorded incidents

  `investigate_incident`              Investigates a supplied incident
                                      ID; `0` selects the latest incident
  -----------------------------------------------------------------------

The MCP endpoint uses the MCP session and streaming protocol. Opening
`/mcp` in a browser or making a generic GET request is not a valid
substitute for an MCP client session.

## Detection logic

The implemented detection paths include:

-   **Unavailable target:** based on Prometheus `up` telemetry.
-   **HTTP 5xx rate:** creates a high-severity incident when the
    measured rate exceeds 5%.
-   **HTTP 4xx rate:** creates a medium-severity incident when the
    measured rate exceeds 10% and traffic is sufficient (at least 0.1
    requests/second).
-   **Average latency:** creates a high-severity incident when average
    observed latency exceeds 0.5 seconds.

Error rates and latency are calculated from Prometheus request counters
and duration sum/count metrics over a five-minute rate window.
Thresholds are prototype defaults and should be tuned for the service's
normal traffic and SLOs.

**Latency limitation:** the current sample workload did not expose
histogram buckets for `http_server_requests_seconds_bucket` during
validation, so p95/p99 latency was not available. The prototype reports
average and maximum recorded latency instead.

## AI investigation and safety

The AI investigation response includes:

-   Incident summary and observed facts
-   Likely cause and confidence level
-   Reasoning based on available evidence
-   Recommended actions
-   Verification steps
-   `requiresHumanApproval: true`

The model is instructed to avoid asserting an unverified root cause when
the available evidence is limited. In the validated 5xx scenario, it
correctly returned low confidence and recommended checking gateway logs,
downstream health, and recent deployment/configuration changes.

OpsPilot currently performs **AI-assisted diagnosis and
recommendations**, not autonomous production remediation. The demo's
generated 4xx and 5xx responses validate detection behavior; they do not
prove automatic recovery.

## Validation performed

The following local checks passed during development:

-   `mvn -q test` for the backend
-   Prometheus scrape targets reporting healthy
-   Telemetry summary returning availability, status counts, error
    rates, request rate, and latency information
-   A generated HTTP 4xx scenario detected above the 10% threshold
-   A generated HTTP 503 scenario detected above the 5% threshold
-   Repeated detection did not create duplicate active incidents
-   AI investigation returned facts, low-confidence cause assessment,
    recommended actions, verification steps, and human-approval flag
-   MCP client discovered all five tools and exercised health,
    telemetry, detection, listing, and investigation

The sample incidents in a local database are demonstration/test data and
may differ after a restart or new test run.

## Troubleshooting

### MCP returns `Missing session ID` or `Client must accept text/event-stream`

The `/mcp` endpoint expects an MCP client session. Use the MCP test
client rather than a generic browser or plain `Invoke-WebRequest` call:

``` powershell
.\.venv\Scripts\python.exe .\test_mcp.py
```

### Prometheus has no matching error-rate result

Check that targets are up at `http://localhost:9091/api/v1/targets`. New
counter series may need additional scrapes before a range query such as
`rate(...[5m])` returns a value. Confirm the raw metric exists at the
monitored application's `/actuator/prometheus` endpoint.

### AI investigation fails

Confirm that `GEMINI_API_KEY` is set in the backend's terminal, the
configured model is available, and outbound network access is working.
Never commit the API key.

### Backend tests

From the backend folder:

``` powershell
mvn -q test
```

## Security and scope notes

-   Treat this as a hackathon/demo prototype, not a production-ready
    incident management platform.
-   Do not expose local management endpoints or the MCP server publicly
    without authentication, authorization, and appropriate network
    controls.
-   Keep API keys and credentials out of source control.
-   Human review is required before taking remediation actions.
-   Preserve the license and notices of the separate Spring PetClinic
    sample project.

## Future improvements

-   Add structured application log and distributed trace context to AI
    investigations.
-   Add histogram buckets to support p95/p99 latency analysis.
-   Add service-specific SLOs, configurable thresholds, and incident
    lifecycle controls.
-   Improve incident correlation across dependent services.
-   Add authentication and authorization for sensitive APIs and MCP
    tools.
-   Add richer demo observability and repeatable automated integration
    tests.

## Acknowledgements

OpsPilot uses Prometheus/Grafana for observability, Gemini for
AI-assisted investigation, the MCP Python SDK for tool exposure, and
Spring PetClinic Microservices as a separate demonstration workload.
Follow the applicable licenses and documentation for each dependency.
