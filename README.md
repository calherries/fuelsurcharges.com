# fuelsurcharges.com

A full-stack Clojure web app for shippers and carriers to track and audit the fuel surcharges on their shipments.

This site is live in production, at www.fuelsurcharges.com

The following libraries are used:
* Http-kit for the web server
* Ring for web server abstraction
* Mount for managing stateful components
* Reitit for routing
* Muuntaja for HTTP format endcoding and decoding
* Struct for shared schema validation on the front-end and back-end
* Reagent and Re-frame for the front-end framework
* Sente for WebSockets

## Running

To start a web server for the application, run:

    lein run
