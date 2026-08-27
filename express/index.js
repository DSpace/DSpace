// const express = require('express'),
// app = express();

// app.use(express.urlencoded({ extended: true }))
// app.use(express.json())

// app.get('/', 
//    (req, res) => res.send('Dockerizing Node Application'))

// app.listen(3000, 
//    () => console.log(`[bootup]: Server is running at port: 3000`));

const express = require('express');
const client = require('prom-client');

const app = express();
app.use(express.json());

// Create a registry for Prometheus metrics
const register = new client.Registry();
client.collectDefaultMetrics({ register });

// Define custom metrics
const loadTimeHistogram = new client.Histogram({
  name: 'frontend_load_time_seconds',
  help: 'Time taken for front-end load events',
  buckets: [0.5, 1, 2, 3, 5, 10], // Exemplary bucket values in seconds
  registers: [register],
});

const domContentLoadedHistogram = new client.Histogram({
  name: 'frontend_dom_content_loaded_seconds',
  help: 'DOM content loaded event time',
  buckets: [0.5, 1, 2, 3, 5, 10],
  registers: [register],
});

// Endpoint to receive metrics from the frontend
app.post('/metrics', (req, res) => {
  const metrics = req.body;
  console.log('Received metrics:', metrics);

  // Update histograms with collected metrics
  if (metrics.loadTime) {
    loadTimeHistogram.observe(metrics.loadTime / 1000); // Convert ms to s
  }
  if (metrics.domContentLoaded) {
    domContentLoadedHistogram.observe(metrics.domContentLoaded / 1000);
  }

  res.status(200).send('Metrics received');
});

// Endpoint for Prometheus to scrape metrics
app.get('/metrics', async (req, res) => {
  res.set('Content-Type', register.contentType);
  res.end(await register.metrics());
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server listening on http://localhost:${PORT}`);
});

