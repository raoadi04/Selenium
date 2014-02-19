/*
Copyright 2012 Selenium committers
Copyright 2012 Software Freedom Conservancy

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


package org.openqa.selenium.chrome;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;

import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriverCommand;
import org.openqa.selenium.remote.Command;
import org.openqa.selenium.remote.CommandInfo;
import org.openqa.selenium.remote.HttpCommandExecutor;
import org.openqa.selenium.remote.Response;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URL;
import java.util.Map;
import java.util.HashMap;

/**
 * A specialized {@link HttpCommandExecutor} that will use a {@link ChromeDriverService} that lives
 * and dies with a single WebDriver session. The service will be restarted upon each new session
 * request and shutdown after each quit command.
 */
class ChromeCommandExecutor extends HttpCommandExecutor {

  private final ChromeDriverService service;

  private final static Map<String, CommandInfo> chromeCommandsNameToUrl = ImmutableMap.of(
      ChromeDriverCommand.LAUNCH_APP,
      post("/session/:sessionId/chromium/launch_app"));

  /**
   * Creates a new ChromeCommandExecutor which will communicate with the chromedriver as configured
   * by the given {@code service}.
   *
   * @param service The ChromeDriverService to send commands to.
   */
  public ChromeCommandExecutor(ChromeDriverService service) {
    super(chromeCommandsNameToUrl, service.getUrl());
    this.service = service;
  }

  /**
   * Creates a new ChromeCommandExecutor which will communicate with the chromedriver as configured
   * by the given {@code service}.
   * 
   * @param service The ChromeDriverService to send commands to.
   */
  public ChromeCommandExecutor(URL serviceUrl) {
    super(chromeCommandsNameToUrl, serviceUrl);
    this.service = null;
  }

  /**
   * Sends the {@code command} to the chromedriver server for execution. The server will be started
   * if requesting a new session. Likewise, if terminating a session, the server will be shutdown
   * once a response is received.
   * 
   * @param command The command to execute.
   * @return The command response.
   * @throws IOException If an I/O error occurs while sending the command.
   */
  @Override
  public Response execute(Command command) throws IOException {
    if (ChromeDriverCommand.NEW_SESSION.equals(command.getName()) &&
        this.service != null) {
      service.start();
    }

    try {
      return super.execute(command);
    } catch (Throwable t) {
      Throwable rootCause = Throwables.getRootCause(t);
      if (rootCause instanceof ConnectException &&
          "Connection refused".equals(rootCause.getMessage()) &&
          !service.isRunning()) {
        throw new WebDriverException("The chromedriver server has unexpectedly died!", t);
      }
      Throwables.propagateIfPossible(t);
      throw new WebDriverException(t);
    } finally {
      if (ChromeDriverCommand.QUIT.equals(command.getName()) &&
          this.service != null) {
        service.stop();
      }
    }
  }
}
