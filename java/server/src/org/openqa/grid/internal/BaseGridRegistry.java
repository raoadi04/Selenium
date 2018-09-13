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

package org.openqa.grid.internal;

import org.openqa.grid.web.Hub;
import org.openqa.selenium.remote.http.HttpClient;

import java.net.URL;

public abstract class BaseGridRegistry implements GridRegistry {
  protected HttpClient.Factory httpClientFactory;

  // The following needs to be volatile because we expose a public setters
  protected volatile Hub hub;

  public BaseGridRegistry(Hub hub) {
    this.hub = hub;
  }

  /**
   * @see GridRegistry#getHub()
   */
  public Hub getHub() {
    return hub;
  }

  /**
   * @see GridRegistry#setHub(Hub)
   */
  public void setHub(Hub hub) {
    this.hub = hub;
  }

  /**
   * @return the {@link HttpClient.Factory} to use.
   * @deprecated use {@link BaseGridRegistry#getHttpClient(URL,int,int)}
   */
  @Override
  public HttpClient getHttpClient(URL url) {
    if (httpClientFactory == null) {
      httpClientFactory = HttpClient.Factory.createDefault();
    }
    return httpClientFactory.createClient(url);
  }

  @Override
  public HttpClient getHttpClient(URL url, int connectionTimeout, int readTimeout) {
    if (httpClientFactory == null) {
      httpClientFactory = HttpClient.Factory.createConfigured(connectionTimeout, readTimeout);
    }
    return httpClientFactory.createClient(url);
  }
}
