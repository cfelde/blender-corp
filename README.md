# blender-corp-web

Blender Corp was is dire need of improving its approach to how it hired, fired, and managed company employees. A model was developed, allowing them to simulate the impact of various measures.

This simulation experiment is within the definition of what we call a Monte Carlo simulation. We do random sampling, put those samples through a set of criteria and operations, and look at the resulting output. Often Monte Carlo simulations just present the final output after a set number of iterations, with a fixed set of parameters. Here however you're able to see the result as it evolves over time, in addition to dynamically change the parameters.

Initially this was all developed in Clojure, but when I thought about presenting this in a blog post it became obvious that it would be of more value to be able to easily interact with the code. It was therefore modified to be developed in ClojureScript instead, so that it would run in your browser. In addition to ClojureScript I'm making heavy use of the async library, wiring various components of the code through channels. This is also how it communicates with the web worker, allowing the core parts of the simulation to run in a separate thread.

## Usage

Please find a working demo of this web app at http://cfelde.com/misc/blender-corp

## How to compile

Use leiningen: lein cljsbuild once

## License

Copyright © 2015 Christian Felde

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
