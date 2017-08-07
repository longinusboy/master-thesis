package org.projmis.celltoweridresolver.test.integration.java;/*
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
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.testtools.TestVerticle;

import static org.vertx.testtools.VertxAssert.*;

/**
 * Example Java integration test that deploys the module that this project builds.
 *
 * Quite often in integration tests you want to deploy the same module for all tests and you don't want tests
 * to start before the module has been deployed.
 *
 * This test demonstrates how to do that.
 */
public class ModuleIntegrationTest extends TestVerticle {

    @Test
    public void testPing() {
        container.logger().info("in testPing()");
        System.out.println("A entrar");

        JsonObject query = new JsonObject()
                .putString("action", "resolve")
                .putNumber("mcc", 268)
                .putNumber("mnc", 1)
                .putNumber("lac", 18)
                .putNumber("cid", 33972);
        
        /*JsonObject query = new JsonObject()
		        .putString("action", "resolve")
		        .putNumber("mcc", 268)
		        .putNumber("mnc", 6)
		        .putNumber("lac", 8350)
		        .putNumber("cid", 3176164);*/

        vertx.eventBus().sendWithTimeout("cellid-resolver", query, 1000 * 60 * 10, new Handler<AsyncResult<Message<JsonObject>>>() {

			@Override
			public void handle(AsyncResult<Message<JsonObject>> event) {
				if (event.succeeded()) {
					System.out.println(event.result().body().encode());
				} else {
					System.out.println("failed");
				}
				testComplete();
			}
		});
    }

    @Override
    public void start() {
        initialize();

        container.deployModule(System.getProperty("vertx.modulename"), new AsyncResultHandler<String>() {
            @Override
            public void handle(AsyncResult<String> asyncResult) {
                if (asyncResult.failed()) {
                    System.out.println("Falha no modulo "+asyncResult.cause());
                    container.logger().error(asyncResult.cause());
                }
                assertTrue(asyncResult.succeeded());
                System.out.println("Module deploy succeeded");
                assertNotNull("deploymentID should not be null", asyncResult.result());
                // If deployed correctly then start the tests!
                try {
                    Thread.sleep(20000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                startTests();
            }
        });
    }

}
