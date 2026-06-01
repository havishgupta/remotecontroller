const WebSocket = require('ws');
const { keyboard, Key } = require('@nut-tree-fork/nut-js');

const port = 8765;
const wss = new WebSocket.Server({ port });

// Configure nut.js keyboard
keyboard.config.autoDelayMs = 0;

console.log(`WebSocket server listening on ws://0.0.0.0:${port}`);
console.log('Waiting for connections from the Android remote...');

wss.on('connection', (ws, req) => {
    const clientIp = req.socket.remoteAddress;
    console.log(`Client connected: ${clientIp}`);

    ws.on('message', async (message) => {
        const command = message.toString().trim().toLowerCase();
        console.log(`Received command: ${command}`);

        try {
            if (command === 'next') {
                await keyboard.pressKey(Key.Right);
                await keyboard.releaseKey(Key.Right);
                console.log('Simulated: Right Arrow');
            } else if (command === 'prev') {
                await keyboard.pressKey(Key.Left);
                await keyboard.releaseKey(Key.Left);
                console.log('Simulated: Left Arrow');
            } else {
                console.log(`Unknown command: ${command}`);
            }
        } catch (error) {
            console.error('Error simulating keypress:', error);
        }
    });

    ws.on('close', () => {
        console.log(`Client disconnected: ${clientIp}`);
    });
});
