# frozen_string_literal: true

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

module Selenium
  module WebDriver
    module DriverExtensions
      module HasDevTools
        attr_reader :page_events
        attr_reader :network_events

        #
        # Retrieves connection to DevTools.
        #
        # @return [DevTools]
        #

        def devtools
          version = Integer(capabilities.browser_version.split('.').first)
          @devtools ||= DevTools.new(url: debugger_address, version: version)
        end

        def record_page_events!
          @page_events ||= {}
          devtools.page.enable

          devtools.page.class::EVENTS.each do |event|
            devtools.page.on(event.last) do
              @page_events[event.first] ||= 0
              @page_events[event.first] += 1
            end
          end
        end

        def record_network_events!
          @network_events ||= {}
          devtools.network.enable

          devtools.network.class::EVENTS.each do |event|
            devtools.network.on(event.last) do
              @network_events[event.first] ||= 0
              @network_events[event.first] += 1
            end
          end
        end

      end # HasDevTools
    end # DriverExtensions
  end # WebDriver
end # Selenium
