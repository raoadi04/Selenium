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
import warnings
# Included so `from selenium.webdriver.edge.webdriver import ..., DesiredCapabilities`
# still works
from selenium.webdriver.common.desired_capabilities import DesiredCapabilities  # noqa
from selenium.webdriver.chromium.webdriver import ChromiumDriver
from .options import Options
from .service import Service


DEFAULT_PORT = 0
DEFAULT_SERVICE_LOG_PATH = None


class WebDriver(ChromiumDriver):

    def __init__(self, executable_path='MicrosoftWebDriver.exe',
                 capabilities=None, port=DEFAULT_PORT, verbose=False,
                 service_log_path=None, log_path=DEFAULT_SERVICE_LOG_PATH,
                 service=None, options=None, keep_alive=False, is_legacy=True,
                 service_args=None, desired_capabilities=None):
        """
        Creates a new instance of the edge driver.
        Starts the service and then creates new instance of edge driver.

        :Args:
         - executable_path - Deprecated: path to the executable. If the default is used it assumes the executable is in the $PATH
         - capabilities - Deprecated: Alias for desired_capabilities
         - port - Deprecated: port you would like the service to run, if left as 0, a free port will be found.
         - verbose - whether to set verbose logging in the service. Only available in Legacy Mode
         - service_log_path - Deprecated: Where to log information from the driver.
         - log_path - Deprecated: Alias for service_log_path
         - service - A `Service` instance
         - options - An `Options` instance
         - keep_alive - Whether to configure EdgeRemoteConnection to use HTTP keep-alive.
         - is_legacy - Whether to use MicrosoftWebDriver.exe (legacy) or MSEdgeDriver.exe (chromium-based). Defaults to True.
         - service_args - Deprecated: List of args to pass to the driver service
         - desired_capabilities - Deprecated: Dictionary object with non-browser specific capabilities only,
           such as "proxy" or "loggingPref". Only available in Legacy mode
         """
        if log_path is not DEFAULT_SERVICE_LOG_PATH:
            warnings.warn('log_path has been deprecated, please pass in a Service object',
                          DeprecationWarning, stacklevel=2)
        if service_args is not None:
            warnings.warn('service_args has been deprecated, please pass in a Service object',
                          DeprecationWarning, stacklevel=2)
        if not is_legacy:
            executable_path = "msedgedriver"

        service = service or Service(executable_path,
                                     port=port,
                                     verbose=verbose,
                                     log_path=service_log_path or log_path,
                                     service_args=service_args,
                                     is_legacy=is_legacy)

        super(WebDriver, self).__init__(
            executable_path,
            port,
            options,
            service_args,
            capabilities or desired_capabilities,
            service_log_path,
            service,
            keep_alive)

    def create_options(self):
        return Options()
