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
    #
    # The main class through which you control the browser.
    #
    # @see SearchContext
    # @see Navigation
    # @see TargetLocator
    # @see Options
    #

    class Driver
      include SearchContext
      include TakesScreenshot

      class << self
        #
        # @api private
        #
        # @see Selenium::WebDriver.for
        #
        # @return [Driver]
        #

        def for(browser, opts = {})
          case browser
          when :chrome
            Chrome::Driver.new(**opts)
          when :internet_explorer, :ie
            IE::Driver.new(**opts)
          when :safari
            Safari::Driver.new(**opts)
          when :firefox, :ff
            Firefox::Driver.new(**opts)
          when :edge, :microsoftedge, :msedge
            Edge::Driver.new(**opts)
          when :remote
            Remote::Driver.new(**opts)
          else
            raise ArgumentError, "unknown driver: #{browser.inspect}"
          end
        end
      end

      #
      # A new Driver instance with the given bridge.
      # End users should use Selenium::WebDriver.for instead of using this directly.
      #
      # @api private
      #

      def initialize(bridge: nil, listener: nil, **opts)
        @service_manager = nil
        @devtools = nil
        @bidi = nil
        bridge ||= create_bridge(**opts)
        add_extensions(bridge.browser)
        @bridge = listener ? Support::EventFiringBridge.new(bridge, listener) : bridge
      end

      def inspect
        format '#<%<class>s:0x%<hash>x browser=%<browser>s>', class: self.class, hash: hash * 2,
                                                              browser: bridge.browser.inspect
      end

      #
      # information about whether a remote end is in a state in which it can create new sessions,
      # and may include additional meta information.
      #
      # @return [Hash]
      #
      def status
        @bridge.status
      end

      #
      # @return [Navigation]
      # @see Navigation
      #

      def navigate
        @navigate ||= WebDriver::Navigation.new(bridge)
      end

      #
      # @return [TargetLocator]
      # @see TargetLocator
      #

      def switch_to
        @switch_to ||= WebDriver::TargetLocator.new(bridge)
      end

      #
      # @return [Manager]
      # @see Manager
      #

      def manage
        bridge.manage
      end

      #
      # @return [ActionBuilder]
      # @see ActionBuilder
      #

      def action(**opts)
        bridge.action(**opts)
      end

      #
      # Opens the specified URL in the browser.
      #

      def get(url)
        navigate.to(url)
      end

      #
      # Get the URL of the current page
      #
      # @return [String]
      #

      def current_url
        bridge.url
      end

      #
      # Get the title of the current page
      #
      # @return [String]
      #

      def title
        bridge.title
      end

      #
      # Get the source of the current page
      #
      # @return [String]
      #

      def page_source
        bridge.page_source
      end

      #
      # Quit the browser
      #

      def quit
        bridge.quit
      ensure
        @service_manager&.stop
        @devtools&.close
        @bidi&.close
      end

      #
      # Close the current window, or the browser if no windows are left.
      #

      def close
        open_window_handles = bridge.close
        # If no top-level browsing contexts are open after calling close,
        # it indicates that the WebDriver session is closed.
        # If the WebDriver session is closed, the BiDi session also needs to be closed.
        @bidi&.close if open_window_handles.empty?
        open_window_handles
      end

      #
      # Get the window handles of open browser windows.
      #
      # @return [Array]
      # @see TargetLocator#window
      #

      def window_handles
        bridge.window_handles
      end

      #
      # Get the current window handle
      #
      # @return [String]
      #

      def window_handle
        bridge.window_handle
      end

      #
      # Execute the given JavaScript
      #
      # @param [String] script
      #   JavaScript source to execute
      # @param [WebDriver::Element, Integer, Float, Boolean, NilClass, String, Array] args
      #   Arguments will be available in the given script in the 'arguments' pseudo-array.
      #
      # @return [WebDriver::Element,Integer,Float,Boolean,NilClass,String,Array]
      #   The value returned from the script.
      #

      def execute_script(script, *args)
        bridge.execute_script(script, *args)
      end

      # Execute an asynchronous piece of JavaScript in the context of the
      # currently selected frame or window. Unlike executing
      # execute_script (synchronous JavaScript), scripts
      # executed with this method must explicitly signal they are finished by
      # invoking the provided callback. This callback is always injected into the
      # executed function as the last argument.
      #
      # @param [String] script
      #   JavaScript source to execute
      # @param [WebDriver::Element,Integer, Float, Boolean, NilClass, String, Array] args
      #   Arguments to the script. May be empty.
      #
      # @return [WebDriver::Element,Integer,Float,Boolean,NilClass,String,Array]
      #

      def execute_async_script(script, *args)
        bridge.execute_async_script(script, *args)
      end

      #
      # @return [VirtualAuthenticator]
      # @see VirtualAuthenticator
      #

      def add_virtual_authenticator(options)
        bridge.add_virtual_authenticator(options)
      end

      #-------------------------------- sugar  --------------------------------

      #
      #   driver.first(id: 'foo')
      #

      alias first find_element

      #
      #   driver.all(class: 'bar') #=> [#<WebDriver::Element:0x1011c3b88, ...]
      #

      alias all find_elements

      #
      #   driver.script('function() { ... };')
      #

      alias script execute_script

      # Get the first element matching the given selector. If given a
      # String or Symbol, it will be used as the id of the element.
      #
      # @param  [String,Hash] sel id or selector
      # @return [WebDriver::Element]
      #
      # Examples:
      #
      #   driver['someElementId']    #=> #<WebDriver::Element:0x1011c3b88>
      #   driver[:tag_name => 'div'] #=> #<WebDriver::Element:0x1011c3b88>
      #

      def [](sel)
        sel = {id: sel} if sel.is_a?(String) || sel.is_a?(Symbol)

        find_element sel
      end

      def browser
        bridge&.browser
      end

      def capabilities
        bridge.capabilities
      end

      #
      # @api private
      # @see SearchContext
      #

      def ref
        [:driver, nil]
      end

      private

      attr_reader :bridge

      def create_bridge(caps:, url:, http_client: nil)
        Remote::Bridge.new(http_client: http_client, url: url).tap do |bridge|
          bridge.create_session(caps)
        end
      end

      def process_options(options, capabilities)
        if options && capabilities
          msg = "Don't use both :options and :capabilities when initializing #{self.class}, prefer :options"
          raise ArgumentError, msg
        end

        options ? options.as_json : deprecate_capabilities(capabilities)
      end

      def deprecate_capabilities(capabilities)
        unless is_a?(Remote::Driver)
          WebDriver.logger.deprecate("The :capabilities parameter for #{self.class}",
                                     ":options argument with an instance of #{self.class}",
                                     id: :capabilities)
        end
        generate_capabilities(capabilities)
      end

      def generate_capabilities(capabilities)
        Array(capabilities).map { |cap|
          if cap.is_a? Symbol
            cap = WebDriver::Options.send(cap)
          elsif !cap.respond_to? :as_json
            msg = ":capabilities parameter only accepts objects responding to #as_json which #{cap.class} does not"
            raise ArgumentError, msg
          end
          cap.as_json
        }.inject(:merge)
      end

      def service_url(service)
        @service_manager = service.launch
        @service_manager.uri
      end

      def screenshot
        bridge.screenshot
      end

      def add_extensions(browser)
        extensions = case browser
                     when :chrome, :msedge
                       Chromium::Driver::EXTENSIONS
                     when :firefox
                       Firefox::Driver::EXTENSIONS
                     when :safari, :safari_technology_preview
                       Safari::Driver::EXTENSIONS
                     when :ie, :internet_explorer
                       IE::Driver::EXTENSIONS
                     else
                       []
                     end
        extensions.each { |extension| extend extension }
      end
    end # Driver
  end # WebDriver
end # Selenium
