const net = require('node:net');

const PORT = Number.parseInt(process.argv[2], 10);
if (!PORT) {
  console.error("Usage: node mcp_tcp_client_proxy.js <port>");
  process.exit(1);
}

const client = net.createConnection(PORT, '127.0.0.1');

client.on('connect', () => {
  process.stdin.pipe(client, { end: false });
  client.pipe(process.stdout);
});

client.on('error', (err) => {
  console.error(`[MCP TCP Proxy] Connection error to port ${PORT}: ${err.message}`);
  process.exit(1);
});

client.on('end', () => {
  process.exit(0);
});
