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
public class PageSizeTest {

  // Defaults assertion
  private static double HEIGHT = 21.59;
  private static double WIDTH = 27.94;

  @Test
  public void setsDefaultHeightWidth() {
    PageSize pageSize = new PageSize();

    assertThat(pageSize.getHeight()).isEqualTo(HEIGHT);
    assertThat(pageSize.getWidth()).isEqualTo(WIDTH);
  }

  @Test
  public void returnsMapOfPageSize() {
    PageSize pageSize = new PageSize();
    pageSize.setHeight(11.0);
    pageSize.setWidth(12.0);

    Map<String, Double> pageSizeMap = pageSize.toJson();

    assertThat(pageSizeMap).containsEntry("height", pageSize.getHeight());
    assertThat(pageSizeMap).containsEntry("width", pageSize.getWidth());
  }
}
