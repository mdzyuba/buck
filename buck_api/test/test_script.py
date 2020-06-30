#!/usr/bin/env python3
# Copyright (c) Facebook, Inc. and its affiliates.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import argparse
import os
from pathlib import Path


parser = argparse.ArgumentParser()


def run_build(remain_args):
    parser = argparse.ArgumentParser()
    parser.add_argument("--show-output", action="store_true")
    parser.add_argument("target")
    args = parser.parse_args(remain_args)
    target = Path(args.target.replace(":", "/").strip("/"))
    output = Path("./buck-out/gen") / Path(str(abs(hash(args.target)))) / target
    os.makedirs(output.parent)
    with open(output, "w") as f1:
        f1.write("Hello, World!")
    print(args)


FUNCTION_MAP = {"build": run_build}

parser.add_argument("command", choices=FUNCTION_MAP.keys())
(args, remain_args) = parser.parse_known_args()

func = FUNCTION_MAP[args.command]
func(remain_args)
