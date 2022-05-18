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

package org.openqa.selenium.grid.node.docker;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;


import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;
import org.openqa.selenium.docker.Device;
import org.openqa.selenium.grid.config.Config;

@RunWith(Parameterized.class)
public class DockerOptionsTest {

  private final Config config;

  private final List<Device> devicesExpected;

  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return asList(new Object[][]{
      {
        configuredDeviceMapping(asList("/dev/kvm:/dev/kvm")),
        asList(device("/dev/kvm", "/dev/kvm"))
      },
      {
        configuredDeviceMapping(asList("/dev/sda2:/dev/sda2")),
        asList(device("/dev/sda2", "/dev/sda2"))
      },
      {
        configuredDeviceMapping(asList("/dev/sda:/dev/xvdc:r")),
        asList(device("/dev/sda", "/dev/xvdc", "r"))
      },
      {
        configuredDeviceMapping(asList("/dev/kvm:/dev/kvm", "/dev/bus/usb:/dev/bus/usb:r")),
        asList(device("/dev/kvm", "/dev/kvm"), device("/dev/bus/usb", "/dev/bus/usb", "r"))
      },
      {
        configuredDeviceMapping(asList(" /dev/kvm:/dev/kvm ")),
        asList(device("/dev/kvm", "/dev/kvm"))
      }
    });
  }

  public DockerOptionsTest(Config config, List<Device> devicesExpected) {
    this.config = config;
    this.devicesExpected = devicesExpected;
  }

  @Test
  public void shouldReturnOnlyExpectedDeviceMappings() {
    List<Device> returnedDevices = new DockerOptions(config).getDevicesMapping();
    assertThat(devicesExpected.equals(returnedDevices))
      .describedAs("Expected %s but was %s", devicesExpected, returnedDevices).isTrue();
  }

  private static Device device(String pathOnHost, String pathInContainer) {
    return device(pathOnHost, pathInContainer, "");
  }

  private static Device device(String pathOnHost, String pathInContainer,
    String cgroupPermissions) {
    return Device.device(pathOnHost, pathInContainer, cgroupPermissions);
  }

  private static Config configuredDeviceMapping(List<String> deviceMapping) {
    Config config = Mockito.mock(Config.class);
    Mockito.when(config.getAll("docker", "devices")).thenReturn(Optional.of(deviceMapping));
    return config;
  }
}
