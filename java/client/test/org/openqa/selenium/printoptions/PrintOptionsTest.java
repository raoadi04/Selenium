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

package org.openqa.selenium.printoptions;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import static org.assertj.core.api.Assertions.assertThat;

import org.openqa.selenium.testing.UnitTests;

import java.util.Map;

@Category(UnitTests.class)
public class PrintOptionsTest {

  @Test
  public void setsDefaultValues() {
    PrintOptions printOptions = new PrintOptions();

    assertThat(printOptions.getScale()).isEqualTo(1.0);
    assertThat(printOptions.getBackground()).isEqualTo(false);
    assertThat(printOptions.getShrinkToFit()).isEqualTo(true);
  }

  @Test
  public void returnsMapDefaultValues() {
    PrintOptions printOptions = new PrintOptions();

    Map<String, Object> printOptionsMap = printOptions.to_json();

    assertThat(printOptionsMap.get("scale")).isEqualTo(printOptions.getScale());
    assertThat(printOptionsMap.get("background")).isEqualTo(printOptions.getBackground());
    assertThat(printOptionsMap.get("shrinkToFit")).isEqualTo(printOptions.getShrinkToFit());
  }

  @Test
  public void setsValuesAsPassed() {
    PrintOptions printOptions = new PrintOptions();

    printOptions.setBackground(true);
    printOptions.setScale(1.5);
    printOptions.setShrinkToFit(false);

    Map<String, Object> printOptionsMap = printOptions.to_json();

    assertThat(printOptionsMap.get("background")).isEqualTo(printOptions.getBackground());
    assertThat(printOptionsMap.get("scale")).isEqualTo(printOptions.getScale());
    assertThat(printOptionsMap.get("shrinkToFit")).isEqualTo(printOptions.getShrinkToFit());
  }
}
