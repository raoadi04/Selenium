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

package org.openqa.selenium.remote;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.ImmutableCapabilities;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.html5.WebStorage;
import org.openqa.selenium.internal.Require;
import org.openqa.selenium.support.decorators.Decorated;
import org.openqa.selenium.support.decorators.WebDriverDecorator;
import org.openqa.selenium.support.events.EventFiringDecorator;
import org.openqa.selenium.support.events.WebDriverListener;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.openqa.selenium.remote.DriverCommand.FIND_ELEMENT;

@Tag("UnitTests")
class AugmenterTest {

  private org.openqa.selenium.remote.Augmenter getAugmenter() {
    return new org.openqa.selenium.remote.Augmenter();
  }

  @Test
  void shouldAddInterfaceFromCapabilityIfNecessary() {
    final Capabilities caps = new ImmutableCapabilities("magic.numbers", true);
    WebDriver driver = new org.openqa.selenium.remote.RemoteWebDriver(new StubExecutor(caps), caps);

    WebDriver returned =
        getAugmenter()
            .addDriverAugmentation("magic.numbers", org.openqa.selenium.remote.HasMagicNumbers.class, (c, exe) -> () -> 42)
            .augment(driver);

    assertThat(returned).isNotSameAs(driver);
    assertThat(returned).isInstanceOf(org.openqa.selenium.remote.HasMagicNumbers.class);
  }

  @Test
  void shouldNotAddInterfaceWhenBooleanValueForItIsFalse() {
    Capabilities caps = new ImmutableCapabilities("magic.numbers", false);
    WebDriver driver = new org.openqa.selenium.remote.RemoteWebDriver(new StubExecutor(caps), caps);

    WebDriver returned =
        getAugmenter()
            .addDriverAugmentation("magic.numbers", org.openqa.selenium.remote.HasMagicNumbers.class, (c, exe) -> () -> 42)
            .augment(driver);

    assertThat(returned).isSameAs(driver);
    assertThat(returned).isNotInstanceOf(org.openqa.selenium.remote.HasMagicNumbers.class);
  }

  @Test
  void shouldNotUseNonMatchingInterfaces() {
    Capabilities caps = new ImmutableCapabilities("magic.numbers", true);
    WebDriver driver = new org.openqa.selenium.remote.RemoteWebDriver(new StubExecutor(caps), caps);

    WebDriver returned =
        getAugmenter()
            .addDriverAugmentation("magic.numbers", org.openqa.selenium.remote.HasMagicNumbers.class, (c, exe) -> () -> 42)
            .augment(driver);
    assertThat(returned).isNotInstanceOf(WebStorage.class);
  }

  @Test
  void shouldDelegateToHandlerIfAdded() {
    Capabilities caps = new ImmutableCapabilities("foo", true);
    WebDriver driver = new org.openqa.selenium.remote.RemoteWebDriver(new StubExecutor(caps), caps);

    WebDriver returned =
        getAugmenter()
            .addDriverAugmentation("foo", MyInterface.class, (c, exe) -> () -> "Hello World")
            .augment(driver);

    String text = ((MyInterface) returned).getHelloWorld();
    assertThat(text).isEqualTo("Hello World");
  }

  @Test
  void shouldDelegateUnmatchedMethodCallsToDriverImplementation() {
    Capabilities caps = new ImmutableCapabilities("magic.numbers", true);
    StubExecutor stubExecutor = new StubExecutor(caps);
    stubExecutor.expect(org.openqa.selenium.remote.DriverCommand.GET_TITLE, new HashMap<>(), "Title");
    WebDriver driver = new org.openqa.selenium.remote.RemoteWebDriver(stubExecutor, caps);

    WebDriver returned =
        getAugmenter()
            .addDriverAugmentation("magic.numbers", org.openqa.selenium.remote.HasMagicNumbers.class, (c, exe) -> () -> 42)
            .augment(driver);

    assertThat(returned.getTitle()).isEqualTo("Title");
  }

  @Test
  void proxyShouldNotAppearInStackTraces() {
    // This will force the class to be enhanced
    final Capabilities caps = new ImmutableCapabilities("magic.numbers", true);

    DetonatingDriver driver = new DetonatingDriver();
    driver.setCapabilities(caps);

    WebDriver returned =
        getAugmenter()
            .addDriverAugmentation("magic.numbers", org.openqa.selenium.remote.HasMagicNumbers.class, (c, exe) -> () -> 42)
            .augment(driver);

    assertThatExceptionOfType(NoSuchElementException.class)
        .isThrownBy(() -> returned.findElement(By.id("ignored")));
  }

  @Test
  void shouldCopyFieldsFromTemplateInstanceIntoChildInstance() {
    ChildRemoteDriver driver = new ChildRemoteDriver();
    org.openqa.selenium.remote.HasMagicNumbers holder = (org.openqa.selenium.remote.HasMagicNumbers) getAugmenter().augment(driver);

    assertThat(holder.getMagicNumber()).isEqualTo(3);
  }

  @Test
  void shouldNotChokeOnFinalFields() {
    WithFinals withFinals = new WithFinals();
    getAugmenter().augment(withFinals);
  }

  @Test
  void shouldAllowReflexiveCalls() {
    Capabilities caps = new ImmutableCapabilities("find by magic", true);
    StubExecutor executor = new StubExecutor(caps);
    final WebElement element = mock(WebElement.class);
    executor.expect(FIND_ELEMENT, ImmutableMap.of("using", "magic", "value", "cheese"), element);

    WebDriver driver = new org.openqa.selenium.remote.RemoteWebDriver(executor, caps);
    WebDriver returned =
        getAugmenter()
            .addDriverAugmentation(
                "find by magic", FindByMagic.class, (c, exe) -> magicWord -> element)
            .augment(driver);

    // No exception is a Good Thing
    WebElement seen = returned.findElement(new ByMagic("cheese"));
    assertThat(seen).isSameAs(element);
  }

  @Test
  void shouldAugmentMultipleInterfaces() {
    final Capabilities caps =
        new ImmutableCapabilities(
            "magic.numbers", true,
            "numbers", true);
    WebDriver driver = new org.openqa.selenium.remote.RemoteWebDriver(new StubExecutor(caps), caps);

    WebDriver returned =
        getAugmenter()
            .addDriverAugmentation("magic.numbers", org.openqa.selenium.remote.HasMagicNumbers.class, (c, exe) -> () -> 42)
            .addDriverAugmentation(
                "numbers",
                HasNumbers.class,
                (c, exe) ->
                    webDriver -> {
                      Require.precondition(
                          webDriver instanceof org.openqa.selenium.remote.HasMagicNumbers,
                          "Driver must implement HasMagicNumbers");
                      return ((org.openqa.selenium.remote.HasMagicNumbers) webDriver).getMagicNumber();
                    })
            .augment(driver);

    assertThat(returned).isNotSameAs(driver);
    assertThat(returned).isInstanceOf(org.openqa.selenium.remote.HasMagicNumbers.class);
    assertThat(returned).isInstanceOf(HasNumbers.class);

    int number = ((HasNumbers) returned).getNumbers(returned);
    assertThat(number).isEqualTo(42);
  }

  @Test
  void shouldDecorateAugmentedWebDriver() {
    final Capabilities caps =
        new ImmutableCapabilities(
            "magic.numbers", true,
            "numbers", true);
    WebDriver driver = new org.openqa.selenium.remote.RemoteWebDriver(new StubExecutor(caps), caps);

    WebDriver augmented =
        getAugmenter()
            .addDriverAugmentation("magic.numbers", org.openqa.selenium.remote.HasMagicNumbers.class, (c, exe) -> () -> 42)
            .addDriverAugmentation(
                "numbers",
                HasNumbers.class,
                (c, exe) ->
                    webDriver -> {
                      Require.precondition(
                          webDriver instanceof org.openqa.selenium.remote.HasMagicNumbers,
                          "Driver must implement HasMagicNumbers");
                      return ((org.openqa.selenium.remote.HasMagicNumbers) webDriver).getMagicNumber();
                    })
            .augment(driver);

    WebDriver decorated = new ModifyTitleWebDriverDecorator().decorate(augmented);

    assertThat(decorated).isNotSameAs(driver);

    assertThat(augmented).isNotSameAs(decorated);
    assertThat(decorated).isInstanceOf(HasNumbers.class);

    String title = decorated.getTitle();

    assertThat(title).isEqualTo("title");

    int number = ((HasNumbers) decorated).getNumbers(decorated);
    assertThat(number).isEqualTo(42);
  }

  @Test
  void shouldAugmentDecoratedWebDriver() {
    final Capabilities caps =
      new ImmutableCapabilities(
        "magic.numbers", true,
        "numbers", true);
    WebDriver driver = new org.openqa.selenium.remote.RemoteWebDriver(new StubExecutor(caps), caps);
    WebDriver eventFiringDecorate = new EventFiringDecorator<>(new WebDriverListener() {
      @Override
      public void beforeAnyCall(Object target, Method method, Object[] args) {
        System.out.println("Bazinga!");
      }
    }).decorate(driver);

    WebDriver modifyTitleDecorate = new ModifyTitleWebDriverDecorator().decorate(eventFiringDecorate);

    WebDriver augmented = getAugmenter()
        .addDriverAugmentation("magic.numbers", org.openqa.selenium.remote.HasMagicNumbers.class, (c, exe) -> () -> 42)
        .augment(modifyTitleDecorate);

    assertThat(modifyTitleDecorate).isNotSameAs(driver);

    assertThat(((HasMagicNumbers) augmented).getMagicNumber()).isEqualTo(42);
    assertThat(augmented.getTitle()).isEqualTo("title");

    assertThat(augmented).isNotSameAs(modifyTitleDecorate);
    assertThat(augmented).isInstanceOf(Decorated.class);
  }


  private static class ByMagic extends By {

    private final String magicWord;

    public ByMagic(String magicWord) {
      this.magicWord = magicWord;
    }

    @Override
    public List<WebElement> findElements(SearchContext context) {
      return Collections.singletonList(((FindByMagic) context).findByMagic(magicWord));
    }
  }

  public interface FindByMagic {

    WebElement findByMagic(String magicWord);
  }

  protected static class StubExecutor implements org.openqa.selenium.remote.CommandExecutor {

    private final Capabilities capabilities;
    private final List<Data> expected = new ArrayList<>();

    protected StubExecutor(Capabilities capabilities) {
      this.capabilities = capabilities;
    }

    @Override
    public org.openqa.selenium.remote.Response execute(Command command) {
      if (org.openqa.selenium.remote.DriverCommand.NEW_SESSION.equals(command.getName())) {
        org.openqa.selenium.remote.Response response = new org.openqa.selenium.remote.Response(new org.openqa.selenium.remote.SessionId("foo"));
        response.setValue(capabilities.asMap());
        return response;
      }

      for (Data possibleMatch : expected) {
        if (possibleMatch.commandName.equals(command.getName())
            && possibleMatch.args.equals(command.getParameters())) {
          org.openqa.selenium.remote.Response response = new org.openqa.selenium.remote.Response(new org.openqa.selenium.remote.SessionId("foo"));
          response.setValue(possibleMatch.returnValue);
          return response;
        }
      }

      throw new AssertionError("Unexpected method invocation: " + command);
    }

    public void expect(String commandName, Map<String, ?> args, Object returnValue) {
      expected.add(new Data(commandName, args, returnValue));
    }

    private static class Data {

      public String commandName;
      public Map<String, ?> args;
      public Object returnValue;

      public Data(String commandName, Map<String, ?> args, Object returnValue) {
        this.commandName = commandName;
        this.args = args;
        this.returnValue = returnValue;
      }
    }
  }

  public interface MyInterface {

    String getHelloWorld();
  }

  public static class DetonatingDriver extends org.openqa.selenium.remote.RemoteWebDriver {

    private Capabilities caps;

    public void setCapabilities(Capabilities caps) {
      this.caps = caps;
    }

    @Override
    public Capabilities getCapabilities() {
      return caps;
    }

    @Override
    public WebElement findElement(By locator) {
      if (locator instanceof By.Remotable) {
        if ("id".equals(((By.Remotable) locator).getRemoteParameters().using())) {
          throw new NoSuchElementException("Boom");
        }
      }
      return null;
    }
  }

  public interface HasNumbers {

    int getNumbers(WebDriver driver);
  }

  public static class ChildRemoteDriver extends org.openqa.selenium.remote.RemoteWebDriver implements org.openqa.selenium.remote.HasMagicNumbers {

    private int magicNumber = 3;

    @Override
    public Capabilities getCapabilities() {
      return new FirefoxOptions();
    }

    @Override
    public int getMagicNumber() {
      return magicNumber;
    }
  }

  public static class WithFinals extends org.openqa.selenium.remote.RemoteWebDriver {

    public final String finalField = "FINAL";

    @Override
    public Capabilities getCapabilities() {
      return new ImmutableCapabilities();
    }
  }

  private static class ModifyTitleWebDriverDecorator extends WebDriverDecorator<WebDriver> {

    @Override
    public Object call(Decorated<?> target, Method method, Object[] args) throws Throwable {
      if (method.getDeclaringClass().equals(HasCapabilities.class)) {
        return new ImmutableCapabilities("magic.numbers", true);
      }

      if (method.getName().equals("getTitle")) {
        return "title";
      }

      return super.call(target, method, args);
    }
  }
}
