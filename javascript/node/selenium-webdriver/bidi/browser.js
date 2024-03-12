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

const {WebElement} = require('../lib/webdriver')

class Browser {
  constructor(driver) {
    this._driver = driver
  }

  async init() {
    if (!(await this._driver.getCapabilities()).get('webSocketUrl')) {
      throw Error('WebDriver instance must support BiDi protocol')
    }

    this.bidi = await this._driver.getBidi()
  }

  async createUserContext() {
    const command = {
      method: 'browser.createUserContext',
      params: {},
    }

    let response = await this.bidi.send(command)

    return response.result.userContext
  }

  async getUserContexts() {
    const command = {
      method: 'browser.getUserContexts',
      params: {},
    }

    let response = await this.bidi.send(command)

    let userContexts = []

    let userContextsArray = response.result.userContexts

    for (let userContextJson of userContextsArray) {
      userContexts.push(userContextJson.userContext)
    }

    return userContexts
  }

  async removeUserContext(userContext) {
    const command = {
      method: 'browser.removeUserContext',
      params: { userContext: userContext },
    }

    await this.bidi.send(command)
  }

}

async function getBrowserInstance(driver) {
  let instance = new Browser(driver)
  await instance.init()
  return instance
}

module.exports = getBrowserInstance
