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

require_relative '../spec_helper'

module Selenium
  module WebDriver
    module Remote
      describe Driver, exclusive: {driver: :remote} do
        it 'exposes session_id' do
          expect(driver.session_id).to be_a(String)
        end

        it 'exposes remote status' do
          expect(driver.status).to be_a(Hash)
        end

        it 'uses a default file detector' do
          driver.navigate.to url_for('upload.html')

          driver.find_element(id: 'upload').send_keys(__FILE__)
          driver.find_element(id: 'go').submit
          wait.until { driver.find_element(id: 'upload_label').displayed? }

          driver.switch_to.frame('upload_target')
          wait.until { driver.find_element(xpath: '//body') }

          body = driver.find_element(xpath: '//body')
          expect(body.text.scan('Licensed to the Software Freedom Conservancy').count).to eq(2)
        end

        it 'lists downloads' do
          reset_driver!(enable_downloads: true) do |driver|
            driver.navigate.to url_for('downloads/download.html')
            driver.find_element(id: 'file-1').click
            driver.find_element(id: 'file-2').click
            file_names = %w[file_1.txt file_2.jpg]

            expect {
              wait.until { driver.downloadable_files == file_names }
            }.not_to raise_exception
          end
        end

        it 'downloads a file' do
          target_directory = Dir.mktmpdir 'se_download'
          at_exit { FileUtils.rm_f(target_directory) }

          reset_driver!(enable_downloads: true) do |driver|
            driver.navigate.to url_for('downloads/download.html')
            driver.find_element(id: 'file-1').click

            loop do
              @files = driver.downloadable_files
              break unless @files.empty?
            end

            file_name = @files.first
            driver.download_file(file_name, target_directory)

            file_content = File.read("#{target_directory}/#{file_name}").strip
            expect(file_content).to eq('Hello, World!')
          end
        end

        it 'deletes downloadable files' do
          reset_driver!(enable_downloads: true) do |driver|
            driver.navigate.to url_for('downloads/download.html')
            driver.find_element(id: 'file-1').click
            wait.until { !driver.downloadable_files.empty? }

            driver.delete_downloadable_files

            expect(driver.downloadable_files).to be_empty
          end
        end
      end
    end # Remote
  end # WebDriver
end # Selenium
