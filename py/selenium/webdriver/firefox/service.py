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
import typing

from selenium.types import SubprocessStdAlias
from selenium.webdriver.common import service
from selenium.webdriver.common import utils


class Service(service.Service):
    """A Service class that is responsible for the starting and stopping of
    `geckodriver`.

    :param executable_path: install path of the geckodriver executable, defaults to `geckodriver`.
    :param port: Port for the service to run on, defaults to 0 where the operating system will decide.
    :param service_args: (Optional) List of args to be passed to the subprocess when launching the executable.
    :param log_output: (Optional) int representation of STDOUT/DEVNULL, any IO instance or String path to file.
    :param env: (Optional) Mapping of environment variables for the new process, defaults to `os.environ`.
    """

    def __init__(
        self,
        executable_path: str = None,
        port: int = 0,
        service_args: typing.Optional[typing.List[str]] = None,
        log_output: SubprocessStdAlias = None,
        env: typing.Optional[typing.Mapping[str, str]] = None,
        **kwargs,
    ) -> None:
        if service_args is None:
            service_args = []
        self._service_args = service_args

        # Set a port for CDP
        if "--connect-existing" not in self._service_args:
            self._service_args.append("--websocket-port")
            self._service_args.append(f"{utils.free_port()}")

        super().__init__(
            executable=executable_path,
            port=port,
            log_output=log_output,
            env=env,
            **kwargs,
        )

    @property
    def service_args(self):
        return self._service_args

    @service_args.setter
    def service_args(self, value):
        if not isinstance(value, list):
            raise TypeError("service args must be a list")
        self._service_args.extend(value)

    def command_line_args(self) -> typing.List[str]:
        return ["--port", f"{self.port}"] + self._service_args
