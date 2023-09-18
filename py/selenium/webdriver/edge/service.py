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
import warnings

from selenium.types import SubprocessStdAlias
from selenium.webdriver.chromium import service


class Service(service.ChromiumService):
    """A Service class that is responsible for the starting and stopping of
    `msedgedriver`.

    :param executable_path: install path of the msedgedriver executable, defaults to `msedgedriver`.
    :param port: Port for the service to run on, defaults to 0 where the operating system will decide.
    :param verbose: (Deprecated) Whether to make the webdriver more verbose (passes the --verbose option to the binary).
        Defaults to False.
    :param log_output: (Optional) int representation of STDOUT/DEVNULL, any IO instance or String path to file.
    :param service_args: (Optional) List of args to be passed to the subprocess when launching the executable.
    :param env: (Optional) Mapping of environment variables for the new process, defaults to `os.environ`.
    """

    def __init__(
        self,
        executable_path: str = None,
        port: int = 0,
        verbose: bool = False,
        log_output: SubprocessStdAlias = None,
        service_args: typing.Optional[typing.List[str]] = None,
        env: typing.Optional[typing.Mapping[str, str]] = None,
        **kwargs,
    ) -> None:
        if service_args is None:
            service_args = []
        self._service_args = service_args

        if verbose:
            warnings.warn(
                "verbose=True is deprecated. Use `service_args=['--verbose', ...]` instead.",
                DeprecationWarning,
                stacklevel=2,
            )
            self._service_args.append("--verbose")
        super().__init__(
            executable_path=executable_path,
            port=port,
            service_args=self.service_args,
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
