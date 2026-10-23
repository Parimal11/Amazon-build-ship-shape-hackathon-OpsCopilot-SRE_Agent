import asyncio
import json
from mcp import Client


async def main():
    async with Client("http://127.0.0.1:8000/mcp") as client:
        # 1. Discover available tools
        result = await client.list_tools()
        print("Discovered MCP tools:")
        for tool in result.tools:
            print(f"- {tool.name}: {tool.description}")

        # 2. Check service health
        print("\n=== 1. SERVICE HEALTH ===")
        result = await client.call_tool("check_service_health", {})
        for item in result.content:
            if hasattr(item, "text"):
                print(item.text)

        # 3. Query telemetry
        print("\n=== 2. TELEMETRY QUERY ===")
        result = await client.call_tool(
            "query_telemetry", {"expression": "up"}
        )
        for item in result.content:
            if hasattr(item, "text"):
                print(item.text)

        # 4. Detect incidents
        print("\n=== 3. INCIDENT DETECTION ===")
        result = await client.call_tool("detect_incidents", {})
        for item in result.content:
            if hasattr(item, "text"):
                print(item.text)

        # 5. List incidents
        print("\n=== 4. INCIDENT LIST ===")
        result = await client.call_tool("list_incidents", {})
        for item in result.content:
            if hasattr(item, "text"):
                print(item.text)

        # 6. Investigate latest incident (0 means latest)
        print("\n=== 5. AI INVESTIGATION ===")
        result = await client.call_tool(
            "investigate_incident", {"incident_id": 0}
        )
        for item in result.content:
            if hasattr(item, "text"):
                print(item.text)


if __name__ == "__main__":
    asyncio.run(main())
