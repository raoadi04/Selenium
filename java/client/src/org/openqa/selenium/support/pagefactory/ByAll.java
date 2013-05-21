/*
Copyright 2007-2013 Selenium committers

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package org.openqa.selenium.support.pagefactory;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Mechanism used to locate elements within a document using a series of  lookups. This class will
 * find all DOM elements that matches any of the locators in sequence, e.g.
 *
 * <pre>
 * driver.findElements(new ByAll(by1, by2))
 * </pre>
 *
 * will find all elements that match <var>by1</var> and then all elements that match <var>by2</var>
 */
public class ByAll extends By {

  private By[] bys;

  public ByAll(By... bys) {
    this.bys = bys;
  }

  @Override
  public WebElement findElement(SearchContext context) {
    List<WebElement> elements = findElements(context);
    if (elements.isEmpty()) {
      throw new NoSuchElementException("Cannot locate an element using " + toString());
    }
    return elements.get(0);
  }

  @Override
  public List<WebElement> findElements(SearchContext context) {
    List<WebElement> elems = new ArrayList<WebElement>();
    for (By by : bys) {
      elems.addAll(by.findElements(context));
    }

    return elems;
  }

  @Override
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder("By.all(");
    stringBuilder.append("{");

    boolean first = true;
    for (By by : bys) {
      stringBuilder.append((first ? "" : ",")).append(by);
      first = false;
    }
    stringBuilder.append("})");
    return stringBuilder.toString();
  }

}
