# inferenceql.chat.webapp

![Stability: Experimental](https://img.shields.io/badge/stability-experimental-orange.svg)

## Prerequisites

- [pnpm](https://pnpm.io/installation)
- [clojure](https://clojure.org/guides/install_clojure)

## Usage

This tool requires that the environment variable `OPENAI_API_KEY` is set to an valid [OpenAI](https://openai.com/) API key. To retrieve an OpenAI API key [log in](https://platform.openai.com/login/) and visit the [API keys](https://platform.openai.com/account/api-keys) page.

Set the database and schema paths at the top of `src/user.clj`, then:

``` shell
pnpm install --shamefully-hoist
clj -A:dev
user=> (go)
```
