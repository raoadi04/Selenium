package org.openqa.selenium.chrome;

import org.junit.After;
import org.junit.Before;
import org.openqa.selenium.devtools.DevTools;


public abstract class DevToolsInfrastructureTest {

  private ChromeDriver chromeDriver;
  private DevTools devTools;
  final String TEST_WEB_SITE_ADDRESS = "https://www.seleniumhq.org/";

  @Before
  public void setUp() {
    chromeDriver = new ChromeDriver();
    devTools = chromeDriver.getDevTools();
    devTools.createSession();
  }


  @After
  public void terminateSession() {
    devTools.close();
    chromeDriver.quit();
  }

  ChromeDriver getChromeDriver() {
    return chromeDriver;
  }

  DevTools getDevTools() {
    return devTools;
  }
}
