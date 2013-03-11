/*
Copyright 2007-2012 Selenium committers
Portions copyright 2011-2012 Software Freedom Conservancy

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

package org.openqa.selenium;

import org.junit.Test;
import org.openqa.selenium.testing.Ignore;
import org.openqa.selenium.testing.JUnit4TestBase;

import static org.junit.Assert.assertEquals;
import static org.openqa.selenium.testing.Ignore.Driver.HTMLUNIT;
import static org.openqa.selenium.testing.Ignore.Driver.IE;
import static org.openqa.selenium.testing.Ignore.Driver.OPERA;
import static org.openqa.selenium.testing.Ignore.Driver.OPERA_MOBILE;
import static org.openqa.selenium.testing.Ignore.Driver.SELENESE;

@Ignore(value = {HTMLUNIT, IE, OPERA, OPERA_MOBILE, SELENESE},
        reason = "HtmlUnit: SVG interaction is only implemented in rendered browsers")
public class SvgDocumentTest extends JUnit4TestBase {

  @Test
  public void testClickOnSvgElement() {
    driver.get(pages.svgTestPage);
    WebElement rect = driver.findElement(By.id("rect"));

    assertEquals("blue", rect.getAttribute("fill"));
    rect.click();
    assertEquals("green", rect.getAttribute("fill"));
  }

}
