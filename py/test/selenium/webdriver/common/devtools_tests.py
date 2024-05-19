# Licensed to the Software Freedom Conservancy (SFC) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The SFC licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
import pytest

from selenium.webdriver.common.by import By
from selenium.webdriver.common.log import Log
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait


@pytest.mark.xfail_safari
def test_check_console_messages(driver, pages):
    devtools, connection = driver.start_devtools()
    console_api_calls = []

    connection.execute(devtools.runtime.enable())
    connection.on(devtools.runtime.ConsoleAPICalled, console_api_calls.append)
    driver.execute_script("console.log('I love cheese')")
    driver.execute_script("console.error('I love bread')")
    WebDriverWait(driver, 5).until(lambda _: len(console_api_calls) == 2)

    assert console_api_calls[0].type_ == "log"
    assert console_api_calls[0].args[0].value == "I love cheese"
    assert console_api_calls[1].type_ == "error"
    assert console_api_calls[1].args[0].value == "I love bread"
