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
from selenium.webdriver.chrome.webdriver import WebDriver as ChromiumDriver
from .options import Options


class OperaDriver(ChromiumDriver):
    """Controls the new OperaDriver and allows you
    to drive the Opera browser based on Chromium.
    
    This will automatically search through the $PATH for the driver's binary.
    This behavior can be overridden either at an instance level by passing 
    executable_path at the time of instantiation, or at the class level by
    setting the driver_path attribute of the class. The executable_path argument
    will be prioritized over the class's driver_path attribute, if it's set.
    """

    driver_path = "operadriver"

    def __init__(self, executable_path=None, port=0,
                 options=None, service_args=None,
                 desired_capabilities=None, service_log_path=None,
                 opera_options=None, keep_alive=True):
        """
        Creates a new instance of the operadriver.

        Starts the service and then creates new instance of operadriver.

        :Args:
         - executable_path - path to the executable. If the default is used
                             it assumes the executable is provided by the class
                             or is in the $PATH
         - port - port you would like the service to run, if left as 0,
                  a free port will be found.
         - options: this takes an instance of OperaOptions
         - service_args - List of args to pass to the driver service
         - desired_capabilities: Dictionary object with non-browser specific
         - service_log_path - Where to log information from the driver.
           capabilities only, such as "proxy" or "loggingPref".
        """
        self.driver_path = executable_path or self.driver_path

        ChromiumDriver.__init__(
            self,
            executable_path=self.driver_path,
            port=port,
            options=options,
            service_args=service_args,
            desired_capabilities=desired_capabilities,
            service_log_path=service_log_path,
            keep_alive=keep_alive,
        )

    def create_options(self):
        return Options()


class WebDriver(OperaDriver):
    class ServiceType:
        CHROMIUM = 2

    def __init__(self,
                 desired_capabilities=None,
                 executable_path=None,
                 port=0,
                 service_log_path=None,
                 service_args=None,
                 options=None):
        OperaDriver.__init__(self, executable_path=executable_path,
                             port=port, options=options,
                             service_args=service_args,
                             desired_capabilities=desired_capabilities,
                             service_log_path=service_log_path)
