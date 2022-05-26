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

package org.openqa.selenium.bidi.log;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.openqa.selenium.testing.Safely.safelyCall;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.bidi.BiDi;
import org.openqa.selenium.environment.webserver.AppServer;
import org.openqa.selenium.environment.webserver.NettyAppServer;
import org.openqa.selenium.environment.webserver.Page;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class BiDiLogTest {

  private AppServer server;
  private FirefoxDriver driver;
  String page;

  @Before
  public void setUp() {
    FirefoxOptions options = new FirefoxOptions();

    FirefoxBinary binary = new FirefoxBinary(FirefoxBinary.Channel.BETA);
    options.setBinary(binary);
    options.setCapability("webSocketUrl", true);

    driver = new FirefoxDriver(options);

    server = new NettyAppServer();
    server.start();
  }

  @Ignore
  @Test
  public void canListenToConsoleLog()
    throws InterruptedException, ExecutionException, TimeoutException {
    page = server.create(new Page()
                           .withBody("<div id='button' onclick='helloWorld()'>click me</div>")
                           .withScripts("function helloWorld() { console.log('Hello, world!') }"));

    driver.get(page);

    CompletableFuture<LogEntry> future = new CompletableFuture<>();

    BiDi biDi = driver.getBiDi();

    biDi.addListener(Log.entryAdded(), future::complete);

    driver.findElement(By.id("button")).click();
    LogEntry logEntry = future.get(5, TimeUnit.SECONDS);

    assertThat(logEntry.getConsoleLogEntry().isPresent()).isTrue();

    ConsoleLogEntry consoleLogEntry = logEntry.getConsoleLogEntry().get();
    assertThat(consoleLogEntry.getText()).isEqualTo("Hello, world!");
    assertThat(consoleLogEntry.getRealm()).isNull();
    assertThat(consoleLogEntry.getArgs().size()).isEqualTo(1);
    assertThat(consoleLogEntry.getType()).isEqualTo("console");
    assertThat(consoleLogEntry.getLevel()).isEqualTo(BaseLogEntry.LogLevel.INFO);
    assertThat(consoleLogEntry.getMethod()).isEqualTo("log");
    assertThat(consoleLogEntry.getStackTrace()).isNull();
  }

  @Ignore
  @Test
  public void canListenToJavascriptLog()
    throws InterruptedException, ExecutionException, TimeoutException {
    page = server.create(new Page()
                           .withBody("<div id='button' onclick='createError()'>click me</div>")
                           .withScripts(
                             "function createError() { throw new Error('Not working') }"));

    driver.get(page);

    CompletableFuture<LogEntry> future = new CompletableFuture<>();

    BiDi biDi = driver.getBiDi();

    biDi.addListener(Log.entryAdded(), future::complete);

    driver.findElement(By.id("button")).click();
    LogEntry logEntry = future.get(5, TimeUnit.SECONDS);

    assertThat(logEntry.getJavascriptLogEntry().isPresent()).isTrue();

    GenericLogEntry javascriptLogEntry = logEntry.getJavascriptLogEntry().get();
    assertThat(javascriptLogEntry.getText()).isEqualTo("Error: Not working");
    assertThat(javascriptLogEntry.getType()).isEqualTo("javascript");
    assertThat(javascriptLogEntry.getLevel()).isEqualTo(BaseLogEntry.LogLevel.ERROR);
  }

  @Ignore
  @Test
  public void canRetrieveStacktraceForALog()
    throws InterruptedException, ExecutionException, TimeoutException {
    page = server.create(new Page()
                           .withBody("<div id='button' onclick='bar()'>click me</div>")
                           .withScripts(
                             " function foo() { throw new Error('Not working'); } \n"
                             + "function bar() { foo(); }"));

    driver.get(page);

    CompletableFuture<LogEntry> future = new CompletableFuture<>();

    BiDi biDi = driver.getBiDi();

    biDi.addListener(Log.entryAdded(), future::complete);

    driver.findElement(By.id("button")).click();
    LogEntry logEntry = future.get(5, TimeUnit.SECONDS);

    assertThat(logEntry.getJavascriptLogEntry().isPresent()).isTrue();
    StackTrace stackTrace = logEntry.getJavascriptLogEntry().get().getStackTrace();
    assertThat(stackTrace).isNotNull();
    assertThat(stackTrace.getCallFrames().size()).isEqualTo(4);
  }

  @After
  public void quitDriver() {
    if (driver != null) {
      driver.quit();
    }
    safelyCall(server::stop);
  }
}
