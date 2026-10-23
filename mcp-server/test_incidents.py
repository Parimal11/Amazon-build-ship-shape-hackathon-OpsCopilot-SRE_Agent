import asyncio
from mcp import Client

async def main():
    async with Client("http://127.0.0.1:8000/mcp") as client:
        result = await client.call_tool("list_incidents", {})
        print("Incident listing result:")
        for item in result.content:
            if hasattr(item, "text"):
                print(item.text)

asyncio.run(main())
