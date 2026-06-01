const WebSocket = require('ws');
const { keyboard, Key } = require('@nut-tree-fork/nut-js');
const os = require('os');
const qrcode = require('qrcode-terminal');

const port = 8765;
const wss = new WebSocket.Server({ port });

// Configure nut.js keyboard
keyboard.config.autoDelayMs = 0;

function getLocalIp() {
    const interfaces = os.networkInterfaces();
    for (const name of Object.keys(interfaces)) {
        for (const iface of interfaces[name]) {
            if (iface.family === 'IPv4' && !iface.internal) {
                return iface.address;
            }
        }
    }
    return '127.0.0.1';
}

const ip = getLocalIp();

console.log(`\nYour IP is: ${ip}`);
console.log('Status: Waiting for connection...\n');

console.log('Scan this QR Code in the app to connect:');
qrcode.generate(ip, { small: true });
console.log('\n------------------------------------------\n');

wss.on('connection', (ws, req) => {
    const clientIp = req.socket.remoteAddress;
    console.log(`Client connected: ${clientIp}`);

    ws.on('message', async (message) => {
        const command = message.toString().trim().toLowerCase();

        try {
            if (command === 'up') {
                await keyboard.pressKey(Key.Up);
                await keyboard.releaseKey(Key.Up);
            } else if (command === 'down') {
                await keyboard.pressKey(Key.Down);
                await keyboard.releaseKey(Key.Down);
            } else if (command === 'left') {
                await keyboard.pressKey(Key.Left);
                await keyboard.releaseKey(Key.Left);
            } else if (command === 'right') {
                await keyboard.pressKey(Key.Right);
                await keyboard.releaseKey(Key.Right);
            }
        } catch (error) {
            console.error('Error simulating keypress:', error);
        }
    });

    ws.on('close', () => {
        console.log(`Client disconnected: ${clientIp}`);
        console.log('Status: Waiting for connection...\n');
    });
});
