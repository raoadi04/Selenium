// Licensed to the Software Freedom Conservancy (SFC) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The SFC licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.openqa.selenium.bidi;

import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.json.Json;
import org.openqa.selenium.json.JsonInput;
import org.openqa.selenium.json.TypeToken;

public class Browser {
  private final BiDi bidi;

  private static final Json JSON = new Json();

  private final Function<JsonInput, String> userContextInfoMapper =
      jsonInput -> {
        Map<String, Object> response = jsonInput.read(Map.class);
        try (StringReader reader = new StringReader(JSON.toJson(response.get("userContext")));
            JsonInput input = JSON.newInput(reader)) {
          return input.read(String.class);
        }
      };

  private final Function<JsonInput, List<String>> userContextsInfoMapper =
      jsonInput -> {
        Map<String, Object> response = jsonInput.read(Map.class);
        try (StringReader reader = new StringReader(JSON.toJson(response.get("userContexts")));
            JsonInput input = JSON.newInput(reader)) {
          return input.read(new TypeToken<List<String>>() {}.getType());
        }
      };

  public Browser(WebDriver driver) {
    this.bidi = ((HasBiDi) driver).getBiDi();
  }

  public String createUserContext() {
    return bidi.send(new Command<>("browser.createUserContext", Map.of(), userContextInfoMapper));
  }

  public List<String> getUserContexts() {
    return bidi.send(new Command<>("browser.getUserContexts", Map.of(), userContextsInfoMapper));
  }

  public String removeUserContext(String userContext) {
    return bidi.send(
        new Command<>(
            "browser.removeUserContext",
            Map.of("userContext", userContext),
            userContextInfoMapper));
  }
}
