import os
import json
import httpx
from mcp.server.mcpserver import MCPServer

BASE_URL = os.getenv("OPSPILOT_BASE_URL", "http://localhost:8081").rstrip("/")

mcp = MCPServer("OpsPilot MCP Server")

async def api_request(method: str, path: str, params=None):
    """Call the existing OpsPilot Spring Boot REST API."""
    async with httpx.AsyncClient(timeout=30.0) as client:
        response = await client.request(
            method,
            f"{BASE_URL}{path}",
            params=params
        )
        response.raise_for_status()
        if not response.content:
            return {"success": True}
        try:
            return response.json()
        except ValueError:
            return {"response": response.text}


@mcp.tool()
async def check_service_health() -> str:
    """Check monitored service health using live Prometheus telemetry."""
    result = await api_request(
        "GET", "/api/telemetry/query", params={"expression": "up"}
    )
    return json.dumps(result, indent=2)


@mcp.tool()
async def detect_incidents() -> str:
    """Detect currently unavailable monitored services and create incidents."""
    result = await api_request("POST", "/api/incidents/detect")
    return json.dumps(result, indent=2)


@mcp.tool()
async def list_incidents() -> str:
    """List incidents recorded by OpsPilot."""
    result = await api_request("GET", "/api/incidents")
    return json.dumps(result, indent=2)


@mcp.tool()
async def investigate_incident(incident_id: int = 0) -> str:
    """Investigate an incident with OpsPilot's existing AI investigation service.
    Supply an incident ID, or use 0 to investigate the latest recorded incident.
    """
    if incident_id < 0:
        return json.dumps({"error": "incident_id must be zero or positive"})

    if incident_id == 0:
        incidents = await api_request("GET", "/api/incidents")

        if not isinstance(incidents, list) or not incidents:
            return json.dumps({
                "message": "No incidents are currently available to investigate."
            })

        latest = max(
            incidents,
            key=lambda item: item.get("id", 0)
        )
        incident_id = int(latest["id"])

    result = await api_request(
        "POST", f"/api/incidents/{incident_id}/investigate"
    )
    return json.dumps(result, indent=2)


@mcp.tool()
async def query_telemetry(expression: str) -> str:
    """Run a read-only Prometheus query through OpsPilot's telemetry API."""
    if not expression or len(expression) > 500:
        return json.dumps({
            "error": "Provide a non-empty Prometheus expression of at most 500 characters."
        })

    result = await api_request(
        "GET", "/api/telemetry/query", params={"expression": expression}
    )
    return json.dumps(result, indent=2)


if __name__ == "__main__":
    mcp.run(transport="streamable-http")
