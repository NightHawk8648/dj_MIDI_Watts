const net = require('node:net');

function createProxy(port, pipePath, label) {
  const server = net.createServer((tcpSocket) => {
    console.log(`[${label}] TCP client connected`);
    const pipeSocket = net.createConnection(pipePath);
    
    pipeSocket.on('connect', () => {
      console.log(`[${label}] Connected to named pipe`);
      tcpSocket.pipe(pipeSocket, { end: false });
      pipeSocket.pipe(tcpSocket, { end: false });
    });
    
    pipeSocket.on('error', (err) => {
      console.error(`[${label}] Pipe socket error:`, err.message);
      tcpSocket.destroy();
    });
    
    tcpSocket.on('error', (err) => {
      console.error(`[${label}] TCP socket error:`, err.message);
      pipeSocket.destroy();
    });
    
    pipeSocket.on('close', () => {
      tcpSocket.end();
    });
    
    tcpSocket.on('close', () => {
      pipeSocket.end();
    });
  });

  server.listen(port, '127.0.0.1', () => {
    console.log(`[${label}] TCP proxy listening on 127.0.0.1:${port}`);
  });
}

createProxy(9090, '\\\\.\\pipe\\datacloud-mcp-notebooks-antigravityide', 'Notebooks');
createProxy(9091, '\\\\.\\pipe\\datacloud-mcp-visualization-antigravityide', 'Visualization');
