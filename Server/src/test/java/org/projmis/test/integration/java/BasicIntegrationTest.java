package org.projmis.test.integration.java;/*
 * Copyright 2013 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * @author <a href="http://tfox.org">Tim Fox</a>
 */

import org.junit.Test;
import org.vertx.testtools.TestVerticle;
import org.vertx.testtools.VertxAssert;

/**
 * Simple integration test which shows tests deploying other verticles, using the Vert.x API etc
 */
public class BasicIntegrationTest extends TestVerticle {

    @Test
    public void testDummy() {
        VertxAssert.testComplete();
    }
  /*@Test
  public void testHTTP() {
    // Create an HTTP server which just sends back OK response immediately
    vertx.createHttpServer().requestHandler(new Handler<HttpServerRequest>() {
      public void handle(HttpServerRequest req) {
        req.response().end();
      }
    }).listen(8181, new AsyncResultHandler<HttpServer>() {
      @Override
      public void handle(AsyncResult<HttpServer> asyncResult) {
        assertTrue(asyncResult.succeeded());
        // The server is listening so send an HTTP request
        vertx.createHttpClient().setPort(8181).getNow("/",new Handler<HttpClientResponse>() {
          @Override
          public void handle(HttpClientResponse resp) {
            assertEquals(200, resp.statusCode());
            testComplete();
          }
        });
      }
    });
  }*/

  /*@Test
  public void testDeployArbitraryVerticle() {
    assertEquals("bar", "bar");
    container.deployVerticle(SomeVerticle.class.getName());
  }*/

  /*@Test
  public void testCompleteOnTimer() {
    vertx.setTimer(1000, new Handler<Long>() {
      @Override
      public void handle(Long timerID) {
        assertNotNull(timerID);

        // This demonstrates how tests are asynchronous - the timer does not fire until 1 second later -
        // which is almost certainly after the test method has completed.
        testComplete();
      }
    });
  }*/


}