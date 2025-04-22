# Flow Experiments

Early experiments with [clojure.core.async.flow][flow].

`flow` is still alpha status at the time of writing, and may be
subject to change.

## Introduction

This project is still a work in progress, but I'm working to assemble
some interesting examples of how flow could be used in various
simplified but (hopefully) representative scenarios.

The various examples are in `src/flow`:

  - **`onion.clj`**: a mock implementation of an onion router (à la
    [Tor][]), where a message is wrapped with multiple layers of routing
    information, and nodes blindly forward the message until it reaches
    its destination.  Demonstrates how procs can dynamically emit
    messages regardless of predefined `:conns`, as well as dynamically
    constructing a flow definition.
    
  - **`erlang.clj`**: explores how the `flow` model compares to Erlang's
    CSP / message model.
    
  - **`sse.clj`**: looks at how a flow can be wired into an HTTP-streaming
    / SSE handler to provide multi-client presence communication.
    A WebSocket implementation could be almost identical, but would
    require a slightly richer client.
    
  Soon:
    
  - **crawler.clj**: a simple webcrawler, demonstrating parallel
    blocking operation handling.

## License

Copyright © Cameron Desautels, 2025

Available under the terms of the Eclipse Public License 2.0, see
LICENSE.

[flow]: https://github.com/clojure/core.async/blob/master/doc/flow.md
[tor]: https://www.torproject.org/
