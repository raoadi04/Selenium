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

import unittest
# Disabled because the site is down
#import test_ajax_jsf
import test_default_server
import test_google
import test_i18n
import test_remote_connection
import sys

def suite():
    return unittest.TestSuite((\
#        unittest.makeSuite(test_ajax_jsf.TestAjaxJSF),
        unittest.makeSuite(test_default_server.TestDefaultServer),
        unittest.makeSuite(test_google.TestGoogle),
        unittest.makeSuite(test_i18n.TestI18n),
        ))

if __name__ == "__main__":
    result = unittest.TextTestRunner(verbosity=2).run(suite())
    sys.exit(not result.wasSuccessful())
