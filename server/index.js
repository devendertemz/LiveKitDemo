require('dotenv').config();
const express = require('express');
const { AccessToken } = require('livekit-server-sdk');

const app = express();

app.get('/token', async (req, res) => {
  const { room, identity } = req.query;

  if (!room || !identity) {
    return res.status(400).json({ error: 'room and identity query params are required' });
  }

  const at = new AccessToken(process.env.LIVEKIT_API_KEY, process.env.LIVEKIT_API_SECRET, {
    identity: String(identity),
    ttl: '6h',
  });
  at.addGrant({ roomJoin: true, room: String(room) });

  const token = await at.toJwt();
  res.json({ token, serverUrl: process.env.LIVEKIT_URL });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, '0.0.0.0', () => {
  console.log(`Token server listening on http://0.0.0.0:${PORT}`);
});
