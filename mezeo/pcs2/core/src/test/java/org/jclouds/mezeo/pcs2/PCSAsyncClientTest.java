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
package org.jclouds.mezeo.pcs2;

import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.HttpHeaders;

import org.jclouds.blobstore.binders.BindBlobToMultipartFormTest;
import org.jclouds.blobstore.functions.ReturnNullOnKeyNotFound;
import org.jclouds.http.HttpRequest;
import org.jclouds.http.RequiresHttp;
import org.jclouds.http.filters.BasicAuthentication;
import org.jclouds.http.functions.CloseContentAndReturn;
import org.jclouds.http.functions.ParseSax;
import org.jclouds.http.functions.ParseURIFromListOrLocationHeaderIf20x;
import org.jclouds.http.functions.ReturnInputStream;
import org.jclouds.mezeo.pcs2.PCSCloudAsyncClient.Response;
import org.jclouds.mezeo.pcs2.blobstore.functions.BlobToPCSFile;
import org.jclouds.mezeo.pcs2.config.PCSRestClientModule;
import org.jclouds.mezeo.pcs2.domain.PCSFile;
import org.jclouds.mezeo.pcs2.functions.AddMetadataItemIntoMap;
import org.jclouds.mezeo.pcs2.options.PutBlockOptions;
import org.jclouds.mezeo.pcs2.xml.ContainerHandler;
import org.jclouds.mezeo.pcs2.xml.FileHandler;
import org.jclouds.rest.AsyncClientFactory;
import org.jclouds.rest.ConfiguresRestClient;
import org.jclouds.rest.RestClientTest;
import org.jclouds.rest.RestContextFactory;
import org.jclouds.rest.RestContextFactory.ContextSpec;
import org.jclouds.rest.functions.MapHttp4xxCodesToExceptions;
import org.jclouds.rest.functions.ReturnVoidOnNotFoundOr404;
import org.jclouds.rest.internal.RestAnnotationProcessor;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;

/**
 * Tests behavior of {@code PCSClient}
 * 
 * @author Adrian Cole
 */
@Test(groups = "unit", testName = "pcs2.PCSClientTest")
public class PCSAsyncClientTest extends RestClientTest<PCSAsyncClient> {

   public void testList() throws SecurityException, NoSuchMethodException, IOException {
      Method method = PCSAsyncClient.class.getMethod("list");
      HttpRequest request = processor.createRequest(method, new Object[] {});

      assertRequestLineEquals(request, "GET http://root HTTP/1.1");
      assertHeadersEqual(request, "X-Cloud-Depth: 2\n");
      assertPayloadEquals(request, null);

      assertResponseParserClassEquals(method, request, ParseSax.class);
      assertSaxResponseParserClassEquals(method, ContainerHandler.class);
      assertExceptionParserClassEquals(method, null);

      checkFilters(request);
   }

   public void testCreateContainer() throws SecurityException, NoSuchMethodException, IOException {
      Method method = PCSAsyncClient.class.getMethod("createContainer", String.class);
      HttpRequest request = processor.createRequest(method, new Object[] { "container" });

      assertRequestLineEquals(request, "POST http://root/contents HTTP/1.1");
      assertHeadersEqual(request,
               "Content-Length: 45\nContent-Type: application/vnd.csp.container-info+xml\n");
      assertPayloadEquals(request, "<container><name>container</name></container>");

      assertResponseParserClassEquals(method, request, ParseURIFromListOrLocationHeaderIf20x.class);
      assertSaxResponseParserClassEquals(method, null);
      assertExceptionParserClassEquals(method, null);

      checkFilters(request);

   }

   public void testDeleteContainer() throws SecurityException, NoSuchMethodException {
      Method method = PCSAsyncClient.class.getMethod("deleteContainer", URI.class);
      HttpRequest request = processor.createRequest(method, new Object[] { URI
               .create("http://localhost/container/1234") });
      assertEquals(request.getRequestLine(), "DELETE http://localhost/container/1234 HTTP/1.1");
      assertEquals(request.getHeaders().size(), 0);
      assertEquals(processor.createResponseParser(method, request).getClass(),
               CloseContentAndReturn.class);
      assertEquals(RestAnnotationProcessor.getSaxResponseParserClassOrNull(method), null);
      assertEquals(request.getFilters().size(), 1);
      assertEquals(request.getFilters().get(0).getClass(), BasicAuthentication.class);
      assertEquals(processor
               .createExceptionParserOrThrowResourceNotFoundOn404IfNoAnnotation(method).getClass(),
               ReturnVoidOnNotFoundOr404.class);
   }

   public void testListURI() throws SecurityException, NoSuchMethodException {
      Method method = PCSAsyncClient.class.getMethod("list", URI.class);
      HttpRequest request = processor.createRequest(method, new Object[] { URI
               .create("http://localhost/mycontainer") });
      assertEquals(request.getRequestLine(), "GET http://localhost/mycontainer HTTP/1.1");
      assertEquals(request.getHeaders().size(), 1);
      assertEquals(request.getHeaders().get("X-Cloud-Depth"), Collections.singletonList("2"));
      assertEquals(processor.createResponseParser(method, request).getClass(), ParseSax.class);
      assertEquals(RestAnnotationProcessor.getSaxResponseParserClassOrNull(method),
               ContainerHandler.class);
      assertEquals(request.getFilters().size(), 1);
      assertEquals(request.getFilters().get(0).getClass(), BasicAuthentication.class);
      assertEquals(processor
               .createExceptionParserOrThrowResourceNotFoundOn404IfNoAnnotation(method).getClass(),
               MapHttp4xxCodesToExceptions.class);
   }

   public void testGetFileInfo() throws SecurityException, NoSuchMethodException {
      Method method = PCSAsyncClient.class.getMethod("getFileInfo", URI.class);
      HttpRequest request = processor.createRequest(method, new Object[] { URI
               .create("http://localhost/myfile") });
      assertEquals(request.getRequestLine(), "GET http://localhost/myfile HTTP/1.1");
      assertEquals(request.getHeaders().size(), 1);
      assertEquals(request.getHeaders().get("X-Cloud-Depth"), Collections.singletonList("2"));
      assertEquals(processor.createResponseParser(method, request).getClass(), ParseSax.class);
      assertEquals(RestAnnotationProcessor.getSaxResponseParserClassOrNull(method),
               FileHandler.class);
      assertEquals(request.getFilters().size(), 1);
      assertEquals(request.getFilters().get(0).getClass(), BasicAuthentication.class);
      assertEquals(processor
               .createExceptionParserOrThrowResourceNotFoundOn404IfNoAnnotation(method).getClass(),
               ReturnNullOnKeyNotFound.class);
   }

   public void testUploadFile() throws SecurityException, NoSuchMethodException, IOException {
      Method method = PCSAsyncClient.class.getMethod("uploadFile", URI.class, PCSFile.class);
      HttpRequest request = processor.createRequest(method, new Object[] {
               URI.create("http://localhost/mycontainer"),
               blobToPCSFile.apply(BindBlobToMultipartFormTest.TEST_BLOB) });

      assertRequestLineEquals(request, "POST http://localhost/mycontainer/contents HTTP/1.1");
      assertHeadersEqual(request,
               "Content-Length: 113\nContent-Type: multipart/form-data; boundary=--JCLOUDS--\n");
      assertPayloadEquals(request, BindBlobToMultipartFormTest.EXPECTS);

      assertResponseParserClassEquals(method, request, ParseURIFromListOrLocationHeaderIf20x.class);
      assertSaxResponseParserClassEquals(method, null);
      assertExceptionParserClassEquals(method, null);

      checkFilters(request);

   }

   public void testUploadBlock() throws SecurityException, NoSuchMethodException, IOException {
      Method method = PCSAsyncClient.class.getMethod("uploadBlock", URI.class, PCSFile.class, Array
               .newInstance(PutBlockOptions.class, 0).getClass());
      HttpRequest request = processor.createRequest(method, new Object[] {
               URI.create("http://localhost/mycontainer"),
               blobToPCSFile.apply(BindBlobToMultipartFormTest.TEST_BLOB) });

      assertRequestLineEquals(request, "PUT http://localhost/mycontainer/content HTTP/1.1");
      assertHeadersEqual(request, "Content-Length: 5\n");
      assertPayloadEquals(request, "hello");

      assertResponseParserClassEquals(method, request, CloseContentAndReturn.class);
      assertSaxResponseParserClassEquals(method, null);
      assertExceptionParserClassEquals(method, null);

      checkFilters(request);
   }

   public void testDownloadFile() throws SecurityException, NoSuchMethodException, IOException {
      Method method = PCSAsyncClient.class.getMethod("downloadFile", URI.class);
      HttpRequest request = processor.createRequest(method, new Object[] { URI
               .create("http://localhost/container") });

      assertRequestLineEquals(request, "GET http://localhost/container/content HTTP/1.1");
      assertHeadersEqual(request, "");
      assertPayloadEquals(request, null);

      assertResponseParserClassEquals(method, request, ReturnInputStream.class);
      assertSaxResponseParserClassEquals(method, null);
      assertExceptionParserClassEquals(method, ReturnNullOnKeyNotFound.class);

      checkFilters(request);

   }

   public void testDeleteFile() throws SecurityException, NoSuchMethodException, IOException {
      Method method = PCSAsyncClient.class.getMethod("deleteFile", URI.class);
      HttpRequest request = processor.createRequest(method, new Object[] { URI
               .create("http://localhost/contents/file") });
      assertEquals(request.getRequestLine(), "DELETE http://localhost/contents/file HTTP/1.1");
      assertEquals(request.getHeaders().size(), 0);
      assertEquals(processor.createResponseParser(method, request).getClass(),
               CloseContentAndReturn.class);
      assertEquals(RestAnnotationProcessor.getSaxResponseParserClassOrNull(method), null);
      assertEquals(request.getFilters().size(), 1);
      assertEquals(request.getFilters().get(0).getClass(), BasicAuthentication.class);
      assertEquals(processor
               .createExceptionParserOrThrowResourceNotFoundOn404IfNoAnnotation(method).getClass(),
               ReturnVoidOnNotFoundOr404.class);
   }

   public void testPutMetadata() throws SecurityException, NoSuchMethodException {
      Method method = PCSAsyncClient.class.getMethod("putMetadataItem", URI.class, String.class,
               String.class);
      HttpRequest request = processor.createRequest(method, new Object[] {
               URI.create("http://localhost/contents/file"), "pow", "bar" });
      assertEquals(request.getRequestLine(),
               "PUT http://localhost/contents/file/metadata/pow HTTP/1.1");
      assertEquals(request.getMethod(), HttpMethod.PUT);
      assertEquals(request.getHeaders().size(), 2);
      assertEquals(request.getHeaders().get(HttpHeaders.CONTENT_LENGTH), Collections
               .singletonList(request.getPayload().toString().getBytes().length + ""));
      assertEquals(request.getHeaders().get(HttpHeaders.CONTENT_TYPE), Collections
               .singletonList("application/unknown"));
      assertEquals("bar", request.getPayload().getRawContent());
      assertEquals(processor
               .createExceptionParserOrThrowResourceNotFoundOn404IfNoAnnotation(method).getClass(),
               MapHttp4xxCodesToExceptions.class);
      assertEquals(processor.createResponseParser(method, request).getClass(),
               CloseContentAndReturn.class);
   }

   public void testAddEntryToMap() throws SecurityException, NoSuchMethodException {
      Method method = PCSAsyncClient.class.getMethod("addMetadataItemToMap", URI.class,
               String.class, Map.class);

      HttpRequest request = processor.createRequest(method, new Object[] {
               URI.create("http://localhost/pow"), "newkey", ImmutableMap.of("key", "value") });
      assertEquals(request.getRequestLine(), "GET http://localhost/pow/metadata/newkey HTTP/1.1");

      assertEquals(request.getHeaders().size(), 0);
      assertEquals(processor
               .createExceptionParserOrThrowResourceNotFoundOn404IfNoAnnotation(method).getClass(),
               MapHttp4xxCodesToExceptions.class);
      assertEquals(processor.createResponseParser(method, request).getClass(),
               AddMetadataItemIntoMap.class);
   }

   private BlobToPCSFile blobToPCSFile;

   @Override
   protected void checkFilters(HttpRequest request) {
      assertEquals(request.getFilters().size(), 1);
      assertEquals(request.getFilters().get(0).getClass(), BasicAuthentication.class);
   }

   @Override
   protected TypeLiteral<RestAnnotationProcessor<PCSAsyncClient>> createTypeLiteral() {
      return new TypeLiteral<RestAnnotationProcessor<PCSAsyncClient>>() {
      };
   }

   @BeforeClass
   @Override
   protected void setupFactory() throws IOException {
      super.setupFactory();
      blobToPCSFile = injector.getInstance(BlobToPCSFile.class);
   }

   @Override
   protected Module createModule() {
      return new TestPCSRestClientModule();
   }

   @RequiresHttp
   @ConfiguresRestClient
   private static final class TestPCSRestClientModule extends PCSRestClientModule {
      @Override
      protected void configure() {
         super.configure();
      }

      @Override
      protected Response provideCloudResponse(AsyncClientFactory factory) {
         return null;
      }

      @Override
      protected URI provideRootContainerUrl(Response response) {
         return URI.create("http://root");
      }
   }

   @Override
   public ContextSpec<PCSClient, PCSAsyncClient> createContextSpec() {
      Properties properties = new Properties();
      properties.setProperty("pcs.apiversion", "foo");
      properties.setProperty("pcs.endpoint", "http://goo");
      return new RestContextFactory()
               .createContextSpec("pcs", "identity", "credential", properties);
   }
}
