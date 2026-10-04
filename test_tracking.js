const WebSocket = require('ws');

// Using the VPS URL directly for the test
const WS_URL = 'wss://sgdelivery.dpdns.org/ws';

console.log(`[Test] Connecting to ${WS_URL}`);

const driverWs = new WebSocket(WS_URL);
const customerWs = new WebSocket(WS_URL);

let driverConnected = false;
let customerConnected = false;
let testPassed = false;
let receivedUpdates = 0;

const ORDER_ID = 9999;
const DRIVER_ID = 777;

driverWs.on('open', () => {
    console.log('[Driver] Connected.');
    driverConnected = true;
    startTestIfReady();
});

customerWs.on('open', () => {
    console.log('[Customer] Connected.');
    customerConnected = true;
    startTestIfReady();
});

customerWs.on('message', (msg) => {
    const data = JSON.parse(msg.toString());
    if (data.type === 'SUBSCRIBED') {
        console.log(`[Customer] Successfully subscribed to: ${data.channel}`);
    } else if (data.type === 'DRIVER_LOCATION_UPDATE' || data.action === 'DRIVER_LOCATION') {
        const loc = data.location || data.data;
        console.log(`[Customer] Received driver location -> Lat: ${loc.lat}, Lng: ${loc.lng}`);
        receivedUpdates++;
        if (receivedUpdates >= 3) {
            console.log('\n✅ [Test Result] Integration test passed! The customer successfully received 3 real-time location updates broadcasted by the server from the driver.');
            testPassed = true;
            process.exit(0);
        }
    }
});

function startTestIfReady() {
    if (!driverConnected || !customerConnected) return;

    console.log('\n[Test] Both clients connected. Starting sequence...');
    
    // Customer subscribes to the order channel
    customerWs.send(JSON.stringify({
        action: 'SUBSCRIBE_ORDER',
        orderId: ORDER_ID
    }));

    // Driver starts sending locations
    let lat = 36.1485;
    let lng = 3.6905;
    let count = 0;

    const interval = setInterval(() => {
        count++;
        lat += 0.001;
        lng += 0.001;
        console.log(`[Driver] Sending location update ${count} -> Lat: ${lat}, Lng: ${lng}`);
        driverWs.send(JSON.stringify({
            action: 'UPDATE_DRIVER_LOCATION',
            driverId: DRIVER_ID,
            orderId: ORDER_ID,
            lat: lat,
            lng: lng,
            speed: 30,
            heading: 45
        }));

        if (count >= 3) {
            clearInterval(interval);
            setTimeout(() => {
                if (!testPassed) {
                    console.error('❌ [Test Result] Integration test failed. Customer did not receive updates.');
                    process.exit(1);
                }
            }, 3000);
        }
    }, 1000);
}

driverWs.on('error', (err) => { console.error('[Driver] Error:', err); });
customerWs.on('error', (err) => { console.error('[Customer] Error:', err); });
