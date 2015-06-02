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

package org.openqa.selenium;

import java.util.List;

public interface SearchContext<E extends WebElement> {
  /**
   * Find all elements within the current context using the given mechanism.
   *
   * @param by The locating mechanism to use
   * @return A list of all {@link WebElement}s, or an empty list if nothing matches
   * @see org.openqa.selenium.By
   */
<<<<<<< HEAD
  List<E> findElements(By by);
=======
  List<? extends WebElement> findElements(By by);
>>>>>>> 958f7edb5a2f3cbbf29f521bf9a928922bf50322


  /**
   * Find the first {@link WebElement} using the given method.
   *
   * @param by The locating mechanism
   * @return The first matching element on the current context
   * @throws NoSuchElementException If no matching elements are found
   */
  E findElement(By by);
}
