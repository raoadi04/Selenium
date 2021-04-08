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

'use strict'

const assert = require('assert')
const http = require('http')
const url = require('url')
const tls = require('tls')
const fs = require('fs')

const HttpClient = require('../../http').HttpClient
const HttpRequest = require('../../lib/http').Request
const Server = require('../../lib/test/httpserver').Server

describe('HttpClient', function () {
  const server = new Server(function (req, res) {
    // eslint-disable-next-line node/no-deprecated-api
    const parsedUrl = url.parse(req.url)

    if (req.method == 'GET' && req.url == '/echo') {
      res.writeHead(200)
      res.end(JSON.stringify(req.headers))
    } else if (req.method == 'GET' && req.url == '/redirect') {
      res.writeHead(303, { Location: server.url('/hello') })
      res.end()
    } else if (req.method == 'GET' && req.url == '/hello') {
      res.writeHead(200, { 'content-type': 'text/plain' })
      res.end('hello, world!')
    } else if (req.method == 'GET' && req.url == '/chunked') {
      res.writeHead(200, {
        'content-type': 'text/html; charset=utf-8',
        'transfer-encoding': 'chunked',
      })
      res.write('<!DOCTYPE html>')
      setTimeout(() => res.end('<h1>Hello, world!</h1>'), 20)
    } else if (req.method == 'GET' && req.url == '/badredirect') {
      res.writeHead(303, {})
      res.end()
    } else if (req.method == 'GET' && req.url == '/protected') {
      const denyAccess = function () {
        res.writeHead(401, { 'WWW-Authenticate': 'Basic realm="test"' })
        res.end('Access denied')
      }

      // eslint-disable-next-line no-useless-escape
      const basicAuthRegExp = /^\s*basic\s+([a-z0-9\-\._~\+\/]+)=*\s*$/i
      const auth = req.headers.authorization
      const match = basicAuthRegExp.exec(auth || '')
      if (!match) {
        denyAccess()
        return
      }

      const userNameAndPass = Buffer.from(match[1], 'base64').toString()
      const parts = userNameAndPass.split(':', 2)
      if (parts[0] !== 'genie' && parts[1] !== 'bottle') {
        denyAccess()
        return
      }

      res.writeHead(200, { 'content-type': 'text/plain' })
      res.end('Access granted!')
    } else if (
      req.method == 'GET' &&
      parsedUrl.pathname &&
      parsedUrl.pathname.endsWith('/proxy')
    ) {
      let headers = Object.assign({}, req.headers)
      headers['x-proxy-request-uri'] = req.url
      res.writeHead(200, headers)
      res.end()
    } else if (
      req.method == 'GET' &&
      parsedUrl.pathname &&
      parsedUrl.pathname.endsWith('/proxy/redirect')
    ) {
      let path = `/proxy${parsedUrl.search || ''}${parsedUrl.hash || ''}`
      res.writeHead(303, { Location: path })
      res.end()
    } else {
      res.writeHead(404, {})
      res.end()
    }
  })

  server.setConnectHandler(function(req, sock, head) {
    if (req.method == 'CONNECT' && req.url == "another.server.com") {
      sock.write('HTTP/1.1 200 Connection Established\r\n\r\n')
      const keyData = fs.readFileSync("test/http/server-key.pem")
      const certData = fs.readFileSync("test/http/server-cert.pem")
      const tlsConfig = {
        key: keyData,
        cert: certData,
        requestCert: false
      }
      const tlsServer = tls.createServer(tlsConfig, (encSocket) => {
        encSocket.write("HTTP/1.1 200 OK\r\n"
                        + "Host: another.server.com\r\n\r\n"
                        + "Hello from TLS server.")
        encSocket.end()
      })
      tlsServer.emit('connection', sock)
    } else {
      sock.end()
    }
  })

  before(function () {
    return server.start()
  })

  after(function () {
    return server.stop()
  })

  it('can send a basic HTTP request', function () {
    const request = new HttpRequest('GET', '/echo')
    request.headers.set('Foo', 'Bar')

    const agent = new http.Agent()
    agent.maxSockets = 1 // Only making 1 request.

    const client = new HttpClient(server.url(), agent)
    return client.send(request).then(function (response) {
      assert.strictEqual(200, response.status)

      const headers = JSON.parse(response.body)
      assert.strictEqual(headers['content-length'], '0')
      assert.strictEqual(headers['connection'], 'keep-alive')
      assert.strictEqual(headers['host'], server.host())

      const regex = /^selenium\/.* \(js (windows|mac|linux)\)$/
      assert.ok(
        regex.test(headers['user-agent']),
        `${headers['user-agent']} does not match ${regex}`
      )

      assert.strictEqual(request.headers.get('Foo'), 'Bar')
      assert.strictEqual(
        request.headers.get('Accept'),
        'application/json; charset=utf-8'
      )
    })
  })

  it('uses CONNECT method to access HTTPS pages', function() {
    const request = new HttpRequest('GET', '/proxy')
    const client = new HttpClient(
      'https://another.server.com', undefined, server.url())
    client.doCertificateCheck = false

    return client.send(request).then(function(response) {
      assert.strictEqual(200, response.status)
      assert.strictEqual(response.headers.get('host'), 'another.server.com')
      assert.strictEqual(response.body, 'Hello from TLS server.')
    })
  })

  it('handles chunked responses', function () {
    let request = new HttpRequest('GET', '/chunked')

    let client = new HttpClient(server.url())
    return client.send(request).then((response) => {
      assert.strictEqual(200, response.status)
      assert.strictEqual(response.body, '<!DOCTYPE html><h1>Hello, world!</h1>')
    })
  })

  it('can use basic auth', function () {
    // eslint-disable-next-line node/no-deprecated-api
    const parsed = url.parse(server.url())
    parsed.auth = 'genie:bottle'

    const client = new HttpClient(url.format(parsed))
    const request = new HttpRequest('GET', '/protected')
    return client.send(request).then(function (response) {
      assert.strictEqual(200, response.status)
      assert.strictEqual(response.headers.get('content-type'), 'text/plain')
      assert.strictEqual(response.body, 'Access granted!')
    })
  })

  it('fails requests missing required basic auth', function () {
    const client = new HttpClient(server.url())
    const request = new HttpRequest('GET', '/protected')
    return client.send(request).then(function (response) {
      assert.strictEqual(401, response.status)
      assert.strictEqual(response.body, 'Access denied')
    })
  })

  it('automatically follows redirects', function () {
    const request = new HttpRequest('GET', '/redirect')
    const client = new HttpClient(server.url())
    return client.send(request).then(function (response) {
      assert.strictEqual(200, response.status)
      assert.strictEqual(response.headers.get('content-type'), 'text/plain')
      assert.strictEqual(response.body, 'hello, world!')
    })
  })

  it('handles malformed redirect responses', function () {
    const request = new HttpRequest('GET', '/badredirect')
    const client = new HttpClient(server.url())
    return client.send(request).then(assert.fail, function (err) {
      assert.ok(
        /Failed to parse "Location"/.test(err.message),
        'Not the expected error: ' + err.message
      )
    })
  })

  describe('with proxy', function () {
    it('sends request to proxy with absolute URI', function () {
      const request = new HttpRequest('GET', '/proxy')
      const client = new HttpClient(
        'http://another.server.com',
        undefined,
        server.url()
      )
      return client.send(request).then(function (response) {
        assert.strictEqual(200, response.status)
        assert.strictEqual(response.headers.get('host'), 'another.server.com')
        assert.strictEqual(
          response.headers.get('x-proxy-request-uri'),
          'http://another.server.com/proxy'
        )
      })
    })

    it('uses proxy when following redirects', function () {
      const request = new HttpRequest('GET', '/proxy/redirect')
      const client = new HttpClient(
        'http://another.server.com',
        undefined,
        server.url()
      )
      return client.send(request).then(function (response) {
        assert.strictEqual(200, response.status)
        assert.strictEqual(response.headers.get('host'), 'another.server.com')
        assert.strictEqual(
          response.headers.get('x-proxy-request-uri'),
          'http://another.server.com/proxy'
        )
      })
    })

    it('includes search and hash in redirect URI', function () {
      const request = new HttpRequest('GET', '/proxy/redirect?foo#bar')
      const client = new HttpClient(
        'http://another.server.com',
        undefined,
        server.url()
      )
      return client.send(request).then(function (response) {
        assert.strictEqual(200, response.status)
        assert.strictEqual(response.headers.get('host'), 'another.server.com')
        assert.strictEqual(
          response.headers.get('x-proxy-request-uri'),
          'http://another.server.com/proxy?foo#bar'
        )
      })
    })
  })
})
