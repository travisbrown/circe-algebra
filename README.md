# circe-algebra

[![Build status](https://img.shields.io/travis/travisbrown/circe-algebra/master.svg)](https://travis-ci.org/travisbrown/circe-algebra)
[![Coverage status](https://img.shields.io/codecov/c/github/travisbrown/circe-algebra/master.svg)](https://codecov.io/github/travisbrown/circe-algebra)
[![Gitter](https://img.shields.io/badge/gitter-join%20chat-green.svg)](https://gitter.im/circe/circe)
[![Maven Central](https://img.shields.io/maven-central/v/io.circe/circe-algebra_2.11.svg)](https://maven-badges.herokuapp.com/maven-central/io.circe/circe-algebra_2.11)

This project contains a set of experiments related to [some proposed changes][circe-228] to [circe][circe]'s JSON
decoding mechanism.

The key idea is that instead of working directly with [cursors][hcursor] for decoding (an approach that circe inherited
from [Argonaut][argonaut] but that I've never really been 100% on board with), we write decoders using a small algebra
of navigation and reading operations. This algebra can then be interpreted in various contexts, where "context"
includes both the underlying document representation and the result type.

This proposal has four major advantages over the cursor-based approach:

1. It's a better fit for the problem. Cursors are too powerful—in order to be generally useful they support operations
   that aren't needed for decoding.
2. It makes it possible to use the same decoders for multiple underlying representations. These could include JSON ASTs
   from other libraries, or even something like a BSON representation.
3. It makes it easier to get fine-tuned result types from the same decoder. You don't need history in your error
   messages? Choose an interpreter that doesn't track history. You want error accumulation? Choose an error-accumulating
   interpreter. You just want stuff to be fast and don't mind exceptions getting thrown all over the place? Write an
   interpreter that returns everything in `cats.Id`.
4. It makes it easier to optimize decoders, since they're just a data structure that you can inspect and rearrange
   however you want.

The code here is currently a work in progress—everything is subject to change and none of this may ever make it back
into circe-core.

## License

circe-algebra is licensed under the **[Apache License, Version 2.0][apache]** (the
"License"); you may not use this software except in compliance with the License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

[apache]: http://www.apache.org/licenses/LICENSE-2.0
[argonaut]: http://argonaut.io/
[circe]: http://circe.io/
[circe-228]: https://github.com/circe/circe/issues/228
[hcursor]: https://circe.github.io/circe/api/io/circe/HCursor.html
