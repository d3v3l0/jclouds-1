/**
 *
 * Copyright (C) 2009 Cloud Conscious, LLC. <info@cloudconscious.com>
 *
 * ====================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ====================================================================
 */
package org.jclouds.aws.ec2.services;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;

import org.jclouds.aws.ec2.xml.DescribeKeyPairsResponseHandler;
import org.jclouds.http.HttpRequest;
import org.jclouds.http.functions.CloseContentAndReturn;
import org.jclouds.http.functions.ParseSax;
import org.jclouds.rest.internal.RestAnnotationProcessor;
import org.testng.annotations.Test;

import com.google.inject.TypeLiteral;

/**
 * Tests behavior of {@code KeyPairAsyncClient}
 * 
 * @author Adrian Cole
 */
@Test(groups = "unit", testName = "ec2.KeyPairAsyncClientTest")
public class KeyPairAsyncClientTest extends BaseEC2AsyncClientTest<KeyPairAsyncClient> {

   public void testDeleteKeyPair() throws SecurityException, NoSuchMethodException, IOException {
      Method method = KeyPairAsyncClient.class.getMethod("deleteKeyPairInRegion", String.class,
               String.class);
      HttpRequest request = processor.createRequest(method, null, "mykey");

      assertRequestLineEquals(request, "POST https://ec2.us-east-1.amazonaws.com/ HTTP/1.1");
      assertHeadersEqual(
               request,
               "Content-Length: 53\nContent-Type: application/x-www-form-urlencoded\nHost: ec2.us-east-1.amazonaws.com\n");
      assertPayloadEquals(request, "Version=2009-11-30&Action=DeleteKeyPair&KeyName=mykey");

      assertResponseParserClassEquals(method, request, CloseContentAndReturn.class);
      assertSaxResponseParserClassEquals(method, null);
      assertExceptionParserClassEquals(method, null);

      checkFilters(request);
   }

   public void testDescribeKeyPairs() throws SecurityException, NoSuchMethodException, IOException {
      Method method = KeyPairAsyncClient.class.getMethod("describeKeyPairsInRegion", String.class,
               Array.newInstance(String.class, 0).getClass());
      HttpRequest request = processor.createRequest(method, (String) null);

      assertRequestLineEquals(request, "POST https://ec2.us-east-1.amazonaws.com/ HTTP/1.1");
      assertHeadersEqual(
               request,
               "Content-Length: 42\nContent-Type: application/x-www-form-urlencoded\nHost: ec2.us-east-1.amazonaws.com\n");
      assertPayloadEquals(request, "Version=2009-11-30&Action=DescribeKeyPairs");

      assertResponseParserClassEquals(method, request, ParseSax.class);
      assertSaxResponseParserClassEquals(method, DescribeKeyPairsResponseHandler.class);
      assertExceptionParserClassEquals(method, null);

      checkFilters(request);
   }

   public void testDescribeKeyPairsArgs() throws SecurityException, NoSuchMethodException,
            IOException {
      Method method = KeyPairAsyncClient.class.getMethod("describeKeyPairsInRegion", String.class,
               Array.newInstance(String.class, 0).getClass());
      HttpRequest request = processor.createRequest(method, null, "1", "2");

      assertRequestLineEquals(request, "POST https://ec2.us-east-1.amazonaws.com/ HTTP/1.1");
      assertHeadersEqual(
               request,
               "Content-Length: 42\nContent-Type: application/x-www-form-urlencoded\nHost: ec2.us-east-1.amazonaws.com\n");
      assertPayloadEquals(request,
               "Version=2009-11-30&Action=DescribeKeyPairs&KeyName.1=1&KeyName.2=2");

      assertResponseParserClassEquals(method, request, ParseSax.class);
      assertSaxResponseParserClassEquals(method, DescribeKeyPairsResponseHandler.class);
      assertExceptionParserClassEquals(method, null);

      checkFilters(request);
   }

   @Override
   protected TypeLiteral<RestAnnotationProcessor<KeyPairAsyncClient>> createTypeLiteral() {
      return new TypeLiteral<RestAnnotationProcessor<KeyPairAsyncClient>>() {
      };
   }

}
