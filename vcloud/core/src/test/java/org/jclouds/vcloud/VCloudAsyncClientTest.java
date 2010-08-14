/**
 *
 * Copyright (C) 2010 Cloud Conscious, LLC. <info@cloudconscious.com>
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

package org.jclouds.vcloud;

import static org.jclouds.Constants.PROPERTY_API_VERSION;
import static org.jclouds.Constants.PROPERTY_IDENTITY;
import static org.jclouds.Constants.PROPERTY_SESSION_INTERVAL;
import static org.jclouds.vcloud.options.InstantiateVAppTemplateOptions.Builder.processorCount;
import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.jclouds.http.HttpRequest;
import org.jclouds.http.RequiresHttp;
import org.jclouds.http.functions.ParseSax;
import org.jclouds.http.functions.ReleasePayloadAndReturn;
import org.jclouds.rest.ConfiguresRestClient;
import org.jclouds.rest.RestClientTest;
import org.jclouds.rest.RestContextFactory;
import org.jclouds.rest.RestContextFactory.ContextSpec;
import org.jclouds.rest.functions.ReturnNullOnNotFoundOr404;
import org.jclouds.rest.functions.ReturnVoidOnNotFoundOr404;
import org.jclouds.rest.internal.RestAnnotationProcessor;
import org.jclouds.util.Utils;
import org.jclouds.vcloud.config.VCloudRestClientModule;
import org.jclouds.vcloud.domain.NamedResource;
import org.jclouds.vcloud.domain.Organization;
import org.jclouds.vcloud.domain.internal.CatalogImpl;
import org.jclouds.vcloud.domain.internal.CatalogItemImpl;
import org.jclouds.vcloud.domain.internal.NamedResourceImpl;
import org.jclouds.vcloud.domain.internal.OrganizationImpl;
import org.jclouds.vcloud.endpoints.Org;
import org.jclouds.vcloud.filters.SetVCloudTokenCookie;
import org.jclouds.vcloud.internal.VCloudLoginAsyncClient;
import org.jclouds.vcloud.internal.VCloudVersionsAsyncClient;
import org.jclouds.vcloud.internal.VCloudLoginAsyncClient.VCloudSession;
import org.jclouds.vcloud.options.CloneVAppOptions;
import org.jclouds.vcloud.options.InstantiateVAppTemplateOptions;
import org.jclouds.vcloud.xml.CatalogHandler;
import org.jclouds.vcloud.xml.CatalogItemHandler;
import org.jclouds.vcloud.xml.NetworkHandler;
import org.jclouds.vcloud.xml.OrgHandler;
import org.jclouds.vcloud.xml.TaskHandler;
import org.jclouds.vcloud.xml.TasksListHandler;
import org.jclouds.vcloud.xml.VAppHandler;
import org.jclouds.vcloud.xml.VAppTemplateHandler;
import org.jclouds.vcloud.xml.VDCHandler;
import org.testng.annotations.Test;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;

/**
 * Tests behavior of {@code VCloudAsyncClient}
 * 
 * @author Adrian Cole
 */
@Test(groups = "unit", testName = "vcloud.VCloudAsyncClientTest")
public class VCloudAsyncClientTest extends RestClientTest<VCloudAsyncClient> {

   public void testInstantiateVAppTemplateInVDCURI() throws SecurityException, NoSuchMethodException, IOException {
      Method method = VCloudAsyncClient.class.getMethod("instantiateVAppTemplateInVDC", URI.class, URI.class,
            String.class, InstantiateVAppTemplateOptions[].class);
      HttpRequest request = processor.createRequest(method, URI
            .create("https://vcloud.safesecureweb.com/api/v0.8/vdc/1"), URI
            .create("https://vcloud.safesecureweb.com/api/v0.8/vAppTemplate/3"), "my-vapp");

      assertRequestLineEquals(request,
            "POST https://vcloud.safesecureweb.com/api/v0.8/vdc/1/action/instantiateVAppTemplate HTTP/1.1");
      assertNonPayloadHeadersEqual(request, "Accept: application/vnd.vmware.vcloud.vApp+xml\n");
      assertPayloadEquals(request, Utils.toStringAndClose(getClass().getResourceAsStream("/newvapp-hosting.xml")),
            "application/vnd.vmware.vcloud.instantiateVAppTemplateParams+xml", false);

      assertResponseParserClassEquals(method, request, ParseSax.class);
      assertSaxResponseParserClassEquals(method, VAppHandler.class);
      assertExceptionParserClassEquals(method, null);

      checkFilters(request);
   }

   public void testInstantiateVAppTemplateInVDCURIOptions() throws SecurityException, NoSuchMethodException,
         IOException {
      Method method = VCloudAsyncClient.class.getMethod("instantiateVAppTemplateInVDC", URI.class, URI.class,
            String.class, InstantiateVAppTemplateOptions[].class);
      HttpRequest request = processor.createRequest(method, URI
            .create("https://vcloud.safesecureweb.com/api/v0.8/vdc/1"), URI
            .create("https://vcloud.safesecureweb.com/api/v0.8/vAppTemplate/3"), "my-vapp", processorCount(1).memory(
            512).disk(1024).fenceMode("allowInOut").inNetwork(
            URI.create("https://vcloud.safesecureweb.com/network/1990")));

      assertRequestLineEquals(request,
            "POST https://vcloud.safesecureweb.com/api/v0.8/vdc/1/action/instantiateVAppTemplate HTTP/1.1");
      assertNonPayloadHeadersEqual(request, "Accept: application/vnd.vmware.vcloud.vApp+xml\n");
      assertPayloadEquals(request, Utils.toStringAndClose(getClass().getResourceAsStream(
            "/newvapp-hostingcpumemdisk.xml")), "application/vnd.vmware.vcloud.instantiateVAppTemplateParams+xml",
            false);

      assertResponseParserClassEquals(method, request, ParseSax.class);
      assertSaxResponseParserClassEquals(method, VAppHandler.class);
      assertExceptionParserClassEquals(method, null);

      checkFilters(request);
   }

   @Test(expectedExceptions = IllegalArgumentException.class)
   public void testInstantiateVAppTemplateInOrgOptionsIllegalName() throws SecurityException, NoSuchMethodException,
         IOException {
      Method method = VCloudAsyncClient.class.getMethod("instantiateVAppTemplateInVDC", URI.class, URI.class,
            String.class, InstantiateVAppTemplateOptions[].class);
      processor.createRequest(method, URI.create("https://vcloud.safesecureweb.com/api/v0.8/vdc/1"), URI
            .create("https://vcloud.safesecureweb.com/api/v0.8/vdc/1"), "CentOS 01", processorCount(1).memory(512)
            .disk(1024).inNetwork(URI.create("https://vcloud.safesecureweb.com/network/1990")));
   }

   public void testCloneVAppInVDC() throws SecurityException, NoSuchMethodException, IOException {
      Method method = VCloudAsyncClient.class.getMethod("cloneVAppInVDC", URI.class, URI.class, String.class,
            CloneVAppOptions[].class);
      HttpRequest request = processor.createRequest(method, URI
            .create("https://vcloud.safesecureweb.com/api/v0.8/vdc/1"), URI
            .create("https://vcloud.safesecureweb.com/api/v0.8/vapp/4181"), "my-vapp");

      assertRequestLineEquals(request, "POST https://vcloud.safesecureweb.com/api/v0.8/vdc/1/action/cloneVApp HTTP/1.1");
      assertNonPayloadHeadersEqual(request, "Accept: application/vnd.vmware.vcloud.task+xml\n");
      assertPayloadEquals(request, Utils.toStringAndClose(getClass().getResourceAsStream("/cloneVApp-default.xml")),
            "application/vnd.vmware.vcloud.cloneVAppParams+xml", false);

      assertResponseParserClassEquals(method, request, ParseSax.class);
      assertSaxResponseParserClassEquals(method, TaskHandler.class);
      assertExceptionParserClassEquals(method, null);

      checkFilters(request);
   }

   public void testCloneVAppInVDCOptions() throws SecurityException, NoSuchMethodException, IOException {
      Method method = VCloudAsyncClient.class.getMethod("cloneVAppInVDC", URI.class, URI.class, String.class,
            CloneVAppOptions[].class);
      HttpRequest request = processor.createRequest(method, URI
            .create("https://vcloud.safesecureweb.com/api/v0.8/vdc/1"), URI
            .create("https://vcloud.safesecureweb.com/api/v0.8/vapp/201"), "new-linux-server", new CloneVAppOptions()
            .deploy().powerOn().withDescription("The description of the new vApp"));

      assertRequestLineEquals(request, "POST https://vcloud.safesecureweb.com/api/v0.8/vdc/1/action/cloneVApp HTTP/1.1");
      assertNonPayloadHeadersEqual(request, "Accept: application/vnd.vmware.vcloud.task+xml\n");
      assertPayloadEquals(request, Utils.toStringAndClose(getClass().getResourceAsStream("/cloneVApp.xml")),
            "application/vnd.vmware.vcloud.cloneVAppParams+xml", false);

      assertResponseParserClassEquals(method, request, ParseSax.class);
      assertSaxResponseParserClassEquals(method, TaskHandler.class);
      assertExceptionParserClassEquals(method, null);

      checkFilters(request);
   }

   public void testDefaultOrganization() throws SecurityException, NoSuchMethodException, IOException {
      Method method = VCloudAsyncClient.class.getMethod("getDefaultOrganization");
      HttpRequest request = processor.createRequest(method);

      assertRequestLineEquals(request, "GET https://vcloud.safesecureweb.com/api/v0.8/org HTTP/1.1");
      assertNonPayloadHeadersEqual(request, "Accept: application/vnd.vmware.vcloud.org+xml\n");
      assertPayloadEquals(request, null, null, false);

      assertResponseParserClassEquals(method, request, ParseSax.class);
      assertSaxResponseParserClassEquals(method, OrgHandler.class);
      assertExceptionParserClassEquals(method, null);

      checkFilters(request);
   }

   public void testOrganization() throws SecurityException, NoSuchMethodException, IOException {
      Method method = VCloudAsyncClient.class.getMethod("getOrganization", URI.class);
      HttpRequest request = processor.createRequest(method, URI
            .create("https://vcloud.safesecureweb.com/api/v0.8/org/1"));

      assertRequestLineEquals(request, "GET https://vcloud.safesecureweb.com/api/v0.8/org/1 HTTP/1.1");
      assertNonPayloadHeadersEqual(request, "Accept: application/vnd.vmware.vcloud.org+xml\n");
      assertPayloadEquals(request, null, null, false);

      assertResponseParserClassEquals(method, request, ParseSax.class);
      assertSaxResponseParserClassEquals(method, OrgHandler.class);
      assertExceptionParserClassEquals(method, ReturnNullOnNotFoundOr404.class);

      checkFilters(request);
   }

   public void testFindOrganizationNamed() throws SecurityException, NoSuchMethodException, IOException {
      Method method = VCloudAsyncClient.class.getMethod("findOrganizationNamed", String.class);
      HttpRequest request = processor.createRequest(method, "org");

      assertRequestLineEquals(request, "GET https://vcloud.safesecureweb.com/api/v0.8/org/1 HTTP/1.1");
      assertNonPayloadHeadersEqual(request, "Accept: application/vnd.vmware.vcloud.org+xml\n");
      assertPayloadEquals(request, null, null, false);

      assertResponseParserClassEquals(method, request, ParseSax.class);
      assertSaxResponseParserClassEquals(method, OrgHandler.class);
      assertExceptionParserClassEquals(method, ReturnNullOnNotFoundOr404.class);

      checkFilters(request);
   }

   public void testDefaultCatalog() throws SecurityException, NoSuchMethodException, IOException {
      Method method = VCloudAsyncClient.class.getMethod("getDefaultCatalog");
      HttpRequest request = processor.createRequest(method);

      assertRequestLineEquals(request, "GET https://vcloud.safesecureweb.com/api/v0.8/catalog HTTP/1.1");
      assertNonPayloadHeadersEqual(request, "Accept: application/vnd.vmware.vcloud.catalog+xml\n");
      assertPayloadEquals(request, null, null, false);

      assertResponseParserClassEquals(method, request, ParseSax.class);
      assertSaxResponseParserClassEquals(method, CatalogHandler.class);
      assertExceptionParserClassEquals(method, null);

      checkFilters(request);
   }

   public void testCatalog() throws SecurityException, NoSuchMethodException, IOException {
      Method method = VCloudAsyncClient.class.getMethod("getCatalog", String.class);
      HttpRequest request = processor.createRequest(method, "1");

      assertRequestLineEquals(request, "GET https://vcloud.safesecureweb.com/api/v0.8/catalog/1 HTTP/1.1");
      assertNonPayloadHeadersEqual(request, "Accept: application/vnd.vmware.vcloud.catalog+xml\n");
      assertPayloadEquals(request, null, null, false);

      assertResponseParserClassEquals(method, request, ParseSax.class);
      assertSaxResponseParserClassEquals(method, CatalogHandler.class);
      assertExceptionParserClassEquals(method, ReturnNullOnNotFoundOr404.class);

      checkFilters(request);
   }

   public void testCatalogInOrg() throws SecurityException, NoSuchMethodException, IOException {
      Method method = VCloudAsyncClient.class.getMethod("findCatalogInOrgNamed", String.class, String.class);
      HttpRequest request = processor.createRequest(method, "org", "catalog");

      assertRequestLineEquals(request, "GET https://vcloud.safesecureweb.com/api/v0.8/catalog/1 HTTP/1.1");
      assertNonPayloadHeadersEqual(request, "Accept: application/vnd.vmware.vcloud.catalog+xml\n");
      assertPayloadEquals(request, null, null, false);

      assertResponseParserClassEquals(method, request, ParseSax.class);
      assertSaxResponseParserClassEquals(method, CatalogHandler.class);
      assertExceptionParserClassEquals(method, ReturnNullOnNotFoundOr404.class);

      checkFilters(request);
   }

   public void testNetwork() throws SecurityException, NoSuchMethodException, IOException {
      Method method = VCloudAsyncClient.class.getMethod("getNetwork", String.class);
      HttpRequest request = processor.createRequest(method, "2");

      assertRequestLineEquals(request, "GET https://vcloud.safesecureweb.com/api/v0.8/network/2 HTTP/1.1");
      assertNonPayloadHeadersEqual(request, "Accept: application/vnd.vmware.vcloud.network+xml\n");
      assertPayloadEquals(request, null, null, false);

      assertResponseParserClassEquals(method, request, ParseSax.class);
      assertSaxResponseParserClassEquals(method, NetworkHandler.class);
      assertExceptionParserClassEquals(method, ReturnNullOnNotFoundOr404.class);

      checkFilters(request);
   }

   public void testCatalogItemURI() throws SecurityException, NoSuchMethodException, IOException {
      Method method = VCloudAsyncClient.class.getMethod("getCatalogItem", URI.class);
      HttpRequest request = processor.createRequest(method, URI
            .create("https://vcloud.safesecureweb.com/api/v0.8/catalogItem/2"));

      assertRequestLineEquals(request, "GET https://vcloud.safesecureweb.com/api/v0.8/catalogItem/2 HTTP/1.1");
      assertNonPayloadHeadersEqual(request, "Accept: application/vnd.vmware.vcloud.catalogItem+xml\n");
      assertPayloadEquals(request, null, null, false);

      assertResponseParserClassEquals(method, request, ParseSax.class);
      assertSaxResponseParserClassEquals(method, CatalogItemHandler.class);
      assertExceptionParserClassEquals(method, ReturnNullOnNotFoundOr404.class);

      checkFilters(request);
   }

   public void testFindCatalogItemInOrgCatalogNamed() throws SecurityException, NoSuchMethodException, IOException {
      Method method = VCloudAsyncClient.class.getMethod("findCatalogItemInOrgCatalogNamed", String.class, String.class,
            String.class);
      HttpRequest request = processor.createRequest(method, "org", "catalog", "item");

      assertRequestLineEquals(request, "GET https://vcloud.safesecureweb.com/api/v0.8/catalogItem/1 HTTP/1.1");
      assertNonPayloadHeadersEqual(request, "Accept: application/vnd.vmware.vcloud.catalogItem+xml\n");
      assertPayloadEquals(request, null, null, false);

      assertResponseParserClassEquals(method, request, ParseSax.class);
      assertSaxResponseParserClassEquals(method, CatalogItemHandler.class);
      assertExceptionParserClassEquals(method, ReturnNullOnNotFoundOr404.class);

      checkFilters(request);
   }

   public void testFindVAppTemplate() throws SecurityException, NoSuchMethodException, IOException {
      Method method = VCloudAsyncClient.class.getMethod("findVAppTemplateInOrgCatalogNamed", String.class,
            String.class, String.class);
      HttpRequest request = processor.createRequest(method, "org", "catalog", "template");

      assertRequestLineEquals(request, "GET https://vcloud.safesecureweb.com/api/v0.8/vAppTemplate/2 HTTP/1.1");
      assertNonPayloadHeadersEqual(request, "Accept: application/vnd.vmware.vcloud.vAppTemplate+xml\n");
      assertPayloadEquals(request, null, null, false);

      assertResponseParserClassEquals(method, request, ParseSax.class);
      assertSaxResponseParserClassEquals(method, VAppTemplateHandler.class);
      assertExceptionParserClassEquals(method, ReturnNullOnNotFoundOr404.class);

      checkFilters(request);
   }

   public void testVAppTemplateURI() throws SecurityException, NoSuchMethodException, IOException {
      Method method = VCloudAsyncClient.class.getMethod("getVAppTemplate", URI.class);
      HttpRequest request = processor.createRequest(method, URI
            .create("https://vcloud.safesecureweb.com/api/v0.8/vAppTemplate/2"));

      assertRequestLineEquals(request, "GET https://vcloud.safesecureweb.com/api/v0.8/vAppTemplate/2 HTTP/1.1");
      assertNonPayloadHeadersEqual(request, "Accept: application/vnd.vmware.vcloud.vAppTemplate+xml\n");
      assertPayloadEquals(request, null, null, false);

      assertResponseParserClassEquals(method, request, ParseSax.class);
      assertSaxResponseParserClassEquals(method, VAppTemplateHandler.class);
      assertExceptionParserClassEquals(method, ReturnNullOnNotFoundOr404.class);

      checkFilters(request);
   }

   public void testGetDefaultVDC() throws SecurityException, NoSuchMethodException, IOException {
      Method method = VCloudAsyncClient.class.getMethod("getDefaultVDC");
      HttpRequest request = processor.createRequest(method);

      assertRequestLineEquals(request, "GET https://vcloud.safesecureweb.com/api/v0.8/vdc/1 HTTP/1.1");
      assertNonPayloadHeadersEqual(request, "Accept: application/vnd.vmware.vcloud.vdc+xml\n");
      assertPayloadEquals(request, null, null, false);

      assertResponseParserClassEquals(method, request, ParseSax.class);
      assertSaxResponseParserClassEquals(method, VDCHandler.class);
      assertExceptionParserClassEquals(method, null);

      checkFilters(request);
   }

   public void testFindVDCInOrgNamed() throws SecurityException, NoSuchMethodException, IOException {
      Method method = VCloudAsyncClient.class.getMethod("findVDCInOrgNamed", String.class, String.class);
      HttpRequest request = processor.createRequest(method, "org", "vdc");

      assertRequestLineEquals(request, "GET https://vcloud.safesecureweb.com/api/v0.8/vdc/1 HTTP/1.1");
      assertNonPayloadHeadersEqual(request, "Accept: application/vnd.vmware.vcloud.vdc+xml\n");
      assertPayloadEquals(request, null, null, false);

      assertResponseParserClassEquals(method, request, ParseSax.class);
      assertSaxResponseParserClassEquals(method, VDCHandler.class);
      assertExceptionParserClassEquals(method, ReturnNullOnNotFoundOr404.class);

      checkFilters(request);
   }

   @Test(expectedExceptions = NoSuchElementException.class)
   public void testFindVDCInOrgNamedBadVDC() throws SecurityException, NoSuchMethodException, IOException {
      Method method = VCloudAsyncClient.class.getMethod("findVDCInOrgNamed", String.class, String.class);
      processor.createRequest(method, "org", "vdc1");
   }

   @Test(expectedExceptions = NoSuchElementException.class)
   public void testFindVDCInOrgNamedBadOrg() throws SecurityException, NoSuchMethodException, IOException {
      Method method = VCloudAsyncClient.class.getMethod("findVDCInOrgNamed", String.class, String.class);
      processor.createRequest(method, "org1", "vdc");
   }

   public void testFindVDCInOrgNamedNullOrg() throws SecurityException, NoSuchMethodException, IOException {
      Method method = VCloudAsyncClient.class.getMethod("findVDCInOrgNamed", String.class, String.class);
      HttpRequest request = processor.createRequest(method, null, "vdc");

      assertRequestLineEquals(request, "GET https://vcloud.safesecureweb.com/api/v0.8/vdc/1 HTTP/1.1");
      assertNonPayloadHeadersEqual(request, "Accept: application/vnd.vmware.vcloud.vdc+xml\n");
      assertPayloadEquals(request, null, null, false);

      assertResponseParserClassEquals(method, request, ParseSax.class);
      assertSaxResponseParserClassEquals(method, VDCHandler.class);
      assertExceptionParserClassEquals(method, ReturnNullOnNotFoundOr404.class);

      checkFilters(request);
   }

   public void testFindVDCInOrgNamedNullOrgAndVDC() throws SecurityException, NoSuchMethodException, IOException {
      Method method = VCloudAsyncClient.class.getMethod("findVDCInOrgNamed", String.class, String.class);
      HttpRequest request = processor.createRequest(method, null, null);

      assertRequestLineEquals(request, "GET https://vcloud.safesecureweb.com/api/v0.8/vdc/1 HTTP/1.1");
      assertNonPayloadHeadersEqual(request, "Accept: application/vnd.vmware.vcloud.vdc+xml\n");
      assertPayloadEquals(request, null, null, false);

      assertResponseParserClassEquals(method, request, ParseSax.class);
      assertSaxResponseParserClassEquals(method, VDCHandler.class);
      assertExceptionParserClassEquals(method, ReturnNullOnNotFoundOr404.class);

      checkFilters(request);
   }

   public void testGetVDC() throws SecurityException, NoSuchMethodException, IOException {
      Method method = VCloudAsyncClient.class.getMethod("getVDC", URI.class);
      HttpRequest request = processor.createRequest(method, URI
            .create("https://vcloud.safesecureweb.com/api/v0.8/vdc/1"));

      assertRequestLineEquals(request, "GET https://vcloud.safesecureweb.com/api/v0.8/vdc/1 HTTP/1.1");
      assertNonPayloadHeadersEqual(request, "Accept: application/vnd.vmware.vcloud.vdc+xml\n");
      assertPayloadEquals(request, null, null, false);

      assertResponseParserClassEquals(method, request, ParseSax.class);
      assertSaxResponseParserClassEquals(method, VDCHandler.class);
      assertExceptionParserClassEquals(method, ReturnNullOnNotFoundOr404.class);

      checkFilters(request);
   }

   public void testGetDefaultTasksList() throws SecurityException, NoSuchMethodException, IOException {
      Method method = VCloudAsyncClient.class.getMethod("getDefaultTasksList");
      HttpRequest request = processor.createRequest(method);

      assertRequestLineEquals(request, "GET https://vcloud.safesecureweb.com/api/v0.8/taskslist HTTP/1.1");
      assertNonPayloadHeadersEqual(request, "Accept: application/vnd.vmware.vcloud.tasksList+xml\n");
      assertPayloadEquals(request, null, null, false);

      assertResponseParserClassEquals(method, request, ParseSax.class);
      assertSaxResponseParserClassEquals(method, TasksListHandler.class);
      assertExceptionParserClassEquals(method, null);

      checkFilters(request);
   }

   public void testGetTasksList() throws SecurityException, NoSuchMethodException, IOException {
      Method method = VCloudAsyncClient.class.getMethod("getTasksList", String.class);
      HttpRequest request = processor.createRequest(method, "1");

      assertRequestLineEquals(request, "GET https://vcloud.safesecureweb.com/api/v0.8/tasksList/1 HTTP/1.1");
      assertNonPayloadHeadersEqual(request, "Accept: application/vnd.vmware.vcloud.tasksList+xml\n");
      assertPayloadEquals(request, null, null, false);

      assertResponseParserClassEquals(method, request, ParseSax.class);
      assertSaxResponseParserClassEquals(method, TasksListHandler.class);
      assertExceptionParserClassEquals(method, ReturnNullOnNotFoundOr404.class);

      checkFilters(request);
   }

   public void testFindTasksListInOrgNamed() throws SecurityException, NoSuchMethodException, IOException {
      Method method = VCloudAsyncClient.class.getMethod("findTasksListInOrgNamed", String.class, String.class);
      HttpRequest request = processor.createRequest(method, "org", "tasksList");

      assertRequestLineEquals(request, "GET https://vcloud.safesecureweb.com/api/v0.8/tasksList/1 HTTP/1.1");
      assertNonPayloadHeadersEqual(request, "Accept: application/vnd.vmware.vcloud.tasksList+xml\n");
      assertPayloadEquals(request, null, null, false);

      assertResponseParserClassEquals(method, request, ParseSax.class);
      assertSaxResponseParserClassEquals(method, TasksListHandler.class);
      assertExceptionParserClassEquals(method, ReturnNullOnNotFoundOr404.class);

      checkFilters(request);
   }

   public void testDeployVApp() throws SecurityException, NoSuchMethodException, IOException {
      Method method = VCloudAsyncClient.class.getMethod("deployVApp", String.class);
      HttpRequest request = processor.createRequest(method, 1);

      assertRequestLineEquals(request, "POST https://vcloud.safesecureweb.com/api/v0.8/vApp/1/action/deploy HTTP/1.1");
      assertNonPayloadHeadersEqual(request, "Accept: application/vnd.vmware.vcloud.task+xml\n");
      assertPayloadEquals(request, null, null, false);

      assertResponseParserClassEquals(method, request, ParseSax.class);
      assertSaxResponseParserClassEquals(method, TaskHandler.class);
      assertExceptionParserClassEquals(method, null);

      checkFilters(request);
   }

   public void testGetVApp() throws SecurityException, NoSuchMethodException, IOException {
      Method method = VCloudAsyncClient.class.getMethod("getVApp", String.class);
      HttpRequest request = processor.createRequest(method, 1);

      assertRequestLineEquals(request, "GET https://vcloud.safesecureweb.com/api/v0.8/vApp/1 HTTP/1.1");
      assertNonPayloadHeadersEqual(request, "Accept: application/vnd.vmware.vcloud.vApp+xml\n");
      assertPayloadEquals(request, null, null, false);

      assertResponseParserClassEquals(method, request, ParseSax.class);
      assertSaxResponseParserClassEquals(method, VAppHandler.class);
      assertExceptionParserClassEquals(method, ReturnNullOnNotFoundOr404.class);

      checkFilters(request);
   }

   public void testUndeployVApp() throws SecurityException, NoSuchMethodException, IOException {
      Method method = VCloudAsyncClient.class.getMethod("undeployVApp", String.class);
      HttpRequest request = processor.createRequest(method, 1);

      assertRequestLineEquals(request, "POST https://vcloud.safesecureweb.com/api/v0.8/vApp/1/action/undeploy HTTP/1.1");
      assertNonPayloadHeadersEqual(request, "Accept: application/vnd.vmware.vcloud.task+xml\n");
      assertPayloadEquals(request, null, null, false);

      assertResponseParserClassEquals(method, request, ParseSax.class);
      assertSaxResponseParserClassEquals(method, TaskHandler.class);
      assertExceptionParserClassEquals(method, null);

      checkFilters(request);
   }

   public void testDeleteVApp() throws SecurityException, NoSuchMethodException, IOException {
      Method method = VCloudAsyncClient.class.getMethod("deleteVApp", String.class);
      HttpRequest request = processor.createRequest(method, 1);

      assertRequestLineEquals(request, "DELETE https://vcloud.safesecureweb.com/api/v0.8/vApp/1 HTTP/1.1");
      assertNonPayloadHeadersEqual(request, "");
      assertPayloadEquals(request, null, null, false);

      assertResponseParserClassEquals(method, request, ReleasePayloadAndReturn.class);
      assertSaxResponseParserClassEquals(method, null);
      assertExceptionParserClassEquals(method, ReturnVoidOnNotFoundOr404.class);

      checkFilters(request);
   }

   public void testPowerOn() throws SecurityException, NoSuchMethodException, IOException {
      Method method = VCloudAsyncClient.class.getMethod("powerOnVApp", String.class);
      HttpRequest request = processor.createRequest(method, 1);

      assertRequestLineEquals(request,
            "POST https://vcloud.safesecureweb.com/api/v0.8/vApp/1/power/action/powerOn HTTP/1.1");
      assertNonPayloadHeadersEqual(request, "Accept: application/vnd.vmware.vcloud.task+xml\n");
      assertPayloadEquals(request, null, null, false);

      assertResponseParserClassEquals(method, request, ParseSax.class);
      assertSaxResponseParserClassEquals(method, TaskHandler.class);
      assertExceptionParserClassEquals(method, null);

      checkFilters(request);
   }

   public void testPowerOff() throws SecurityException, NoSuchMethodException, IOException {
      Method method = VCloudAsyncClient.class.getMethod("powerOffVApp", String.class);
      HttpRequest request = processor.createRequest(method, 1);

      assertRequestLineEquals(request,
            "POST https://vcloud.safesecureweb.com/api/v0.8/vApp/1/power/action/powerOff HTTP/1.1");
      assertNonPayloadHeadersEqual(request, "Accept: application/vnd.vmware.vcloud.task+xml\n");
      assertPayloadEquals(request, null, null, false);

      assertResponseParserClassEquals(method, request, ParseSax.class);
      assertSaxResponseParserClassEquals(method, TaskHandler.class);
      assertExceptionParserClassEquals(method, null);

      checkFilters(request);
   }

   public void testReset() throws SecurityException, NoSuchMethodException, IOException {
      Method method = VCloudAsyncClient.class.getMethod("resetVApp", String.class);
      HttpRequest request = processor.createRequest(method, 1);

      assertRequestLineEquals(request,
            "POST https://vcloud.safesecureweb.com/api/v0.8/vApp/1/power/action/reset HTTP/1.1");
      assertNonPayloadHeadersEqual(request, "Accept: application/vnd.vmware.vcloud.task+xml\n");
      assertPayloadEquals(request, null, null, false);

      assertResponseParserClassEquals(method, request, ParseSax.class);
      assertSaxResponseParserClassEquals(method, TaskHandler.class);
      assertExceptionParserClassEquals(method, null);

      checkFilters(request);
   }

   public void testSuspend() throws SecurityException, NoSuchMethodException, IOException {
      Method method = VCloudAsyncClient.class.getMethod("suspendVApp", String.class);
      HttpRequest request = processor.createRequest(method, 1);

      assertRequestLineEquals(request,
            "POST https://vcloud.safesecureweb.com/api/v0.8/vApp/1/power/action/suspend HTTP/1.1");
      assertNonPayloadHeadersEqual(request, "Accept: application/vnd.vmware.vcloud.task+xml\n");
      assertPayloadEquals(request, null, null, false);

      assertResponseParserClassEquals(method, request, ParseSax.class);
      assertSaxResponseParserClassEquals(method, TaskHandler.class);
      assertExceptionParserClassEquals(method, null);

      checkFilters(request);
   }

   public void testShutdown() throws SecurityException, NoSuchMethodException, IOException {
      Method method = VCloudAsyncClient.class.getMethod("shutdownVApp", String.class);
      HttpRequest request = processor.createRequest(method, 1);

      assertRequestLineEquals(request,
            "POST https://vcloud.safesecureweb.com/api/v0.8/vApp/1/power/action/shutdown HTTP/1.1");
      assertNonPayloadHeadersEqual(request, "");
      assertPayloadEquals(request, null, null, false);

      assertResponseParserClassEquals(method, request, ReleasePayloadAndReturn.class);
      assertSaxResponseParserClassEquals(method, null);
      assertExceptionParserClassEquals(method, null);

      checkFilters(request);
   }

   public void testGetTask() throws SecurityException, NoSuchMethodException, IOException {
      Method method = VCloudAsyncClient.class.getMethod("getTask", String.class);
      HttpRequest request = processor.createRequest(method, "1");

      assertRequestLineEquals(request, "GET https://vcloud.safesecureweb.com/api/v0.8/task/1 HTTP/1.1");
      assertNonPayloadHeadersEqual(request, "Accept: application/vnd.vmware.vcloud.task+xml\n");
      assertPayloadEquals(request, null, null, false);

      assertResponseParserClassEquals(method, request, ParseSax.class);
      assertSaxResponseParserClassEquals(method, TaskHandler.class);
      assertExceptionParserClassEquals(method, ReturnNullOnNotFoundOr404.class);

      checkFilters(request);
   }

   public void testCancelTask() throws SecurityException, NoSuchMethodException, IOException {
      Method method = VCloudAsyncClient.class.getMethod("cancelTask", String.class);
      HttpRequest request = processor.createRequest(method, "1");

      assertRequestLineEquals(request, "POST https://vcloud.safesecureweb.com/api/v0.8/task/1/action/cancel HTTP/1.1");
      assertNonPayloadHeadersEqual(request, "");
      assertPayloadEquals(request, null, null, false);

      assertResponseParserClassEquals(method, request, ReleasePayloadAndReturn.class);
      assertSaxResponseParserClassEquals(method, null);
      assertExceptionParserClassEquals(method, null);

      checkFilters(request);
   }

   @Override
   protected void checkFilters(HttpRequest request) {
      assertEquals(request.getFilters().size(), 1);
      assertEquals(request.getFilters().get(0).getClass(), SetVCloudTokenCookie.class);
   }

   @Override
   protected TypeLiteral<RestAnnotationProcessor<VCloudAsyncClient>> createTypeLiteral() {
      return new TypeLiteral<RestAnnotationProcessor<VCloudAsyncClient>>() {
      };
   }

   @Override
   protected Module createModule() {
      return new VCloudRestClientModuleExtension();
   }

   @Override
   public ContextSpec<?, ?> createContextSpec() {
      Properties overrides = new Properties();
      overrides.setProperty("vcloud.endpoint", "https://vcloud.safesecureweb.com/api/v0.8");
      return new RestContextFactory().createContextSpec("vcloud", "identity", "credential", overrides);
   }

   @RequiresHttp
   @ConfiguresRestClient
   public static class VCloudRestClientModuleExtension extends VCloudRestClientModule {
      @Override
      protected URI provideAuthenticationURI(VCloudVersionsAsyncClient versionService,
            @Named(PROPERTY_API_VERSION) String version) {
         return URI.create("https://vcloud.safesecureweb.com/api/v0.8/login");
      }

      @Override
      protected void configure() {
         super.configure();
         bind(OrganizationMapSupplier.class).to(TestOrganizationMapSupplier.class);
         bind(OrganizationCatalogSupplier.class).to(TestOrganizationCatalogItemMapSupplier.class);
         bind(OrganizationCatalogItemSupplier.class).to(TestOrganizationCatalogItemSupplier.class);
      }

      @Override
      protected URI provideOrg(@Org Iterable<NamedResource> orgs) {
         return URI.create("https://vcloud.safesecureweb.com/api/v0.8/org");

      }

      @Override
      protected String provideOrgName(@Org Iterable<NamedResource> orgs) {
         return "org";
      }

      @Override
      protected URI provideCatalog(Organization org, @Named(PROPERTY_IDENTITY) String user) {
         return URI.create("https://vcloud.safesecureweb.com/api/v0.8/catalog");

      }

      @Override
      protected Organization provideOrganization(VCloudClient discovery) {
         return null;
      }

      @Override
      protected Iterable<NamedResource> provideOrgs(Supplier<VCloudSession> cache, @Named(PROPERTY_IDENTITY) String user) {
         return null;
      }

      @Override
      protected URI provideDefaultTasksList(Organization org) {
         return URI.create("https://vcloud.safesecureweb.com/api/v0.8/taskslist");
      }

      @Override
      protected URI provideDefaultVDC(Organization org) {
         return URI.create("https://vcloud.safesecureweb.com/api/v0.8/vdc/1");
      }

      @Override
      protected URI provideDefaultNetwork(VCloudClient client) {
         return URI.create("https://vcloud.safesecureweb.com/network/1990");
      }

      @Override
      protected Supplier<VCloudSession> provideVCloudTokenCache(@Named(PROPERTY_SESSION_INTERVAL) long seconds,
            final VCloudLoginAsyncClient login) {
         return Suppliers.<VCloudSession> ofInstance(new VCloudSession() {

            @Override
            public Map<String, NamedResource> getOrgs() {
               return ImmutableMap.<String, NamedResource> of("org", new NamedResourceImpl("1", "org",
                     VCloudMediaType.ORG_XML, URI.create("https://vcloud.safesecureweb.com/api/v0.8/org/1")));
            }

            @Override
            public String getVCloudToken() {
               return "token";
            }

         });

      }

      @Singleton
      public static class TestOrganizationMapSupplier extends OrganizationMapSupplier {
         @Inject
         protected TestOrganizationMapSupplier() {
            super(null, null);
         }

         @Override
         public Map<String, Organization> get() {
            return ImmutableMap.<String, Organization> of("org", new OrganizationImpl("1", "org", URI
                  .create("https://vcloud.safesecureweb.com/api/v0.8/org/1"), ImmutableMap.<String, NamedResource> of(
                  "catalog", new NamedResourceImpl("1", "catalog", VCloudMediaType.CATALOG_XML, URI
                        .create("https://vcloud.safesecureweb.com/api/v0.8/catalog/1"))), ImmutableMap
                  .<String, NamedResource> of("vdc", new NamedResourceImpl("1", "vdc", VCloudMediaType.VDC_XML, URI
                        .create("https://vcloud.safesecureweb.com/api/v0.8/vdc/1"))), ImmutableMap
                  .<String, NamedResource> of("tasksList", new NamedResourceImpl("1", "tasksList",
                        VCloudMediaType.TASKSLIST_XML, URI
                              .create("https://vcloud.safesecureweb.com/api/v0.8/tasksList/1")))));
         }
      }

      @Singleton
      public static class TestOrganizationCatalogItemMapSupplier extends OrganizationCatalogSupplier {
         @Inject
         protected TestOrganizationCatalogItemMapSupplier() {
            super(null, null);
         }

         @Override
         public Map<String, Map<String, ? extends org.jclouds.vcloud.domain.Catalog>> get() {
            return ImmutableMap.<String, Map<String, ? extends org.jclouds.vcloud.domain.Catalog>> of("org",

            ImmutableMap.<String, org.jclouds.vcloud.domain.Catalog> of("catalog", new CatalogImpl("1", "catalog", URI
                  .create("https://vcloud.safesecureweb.com/api/v0.8/catalog/1"), "description", ImmutableMap
                  .<String, NamedResource> of("item", new NamedResourceImpl("1", "item",
                        "application/vnd.vmware.vcloud.catalogItem+xml", URI
                              .create("https://vcloud.safesecureweb.com/api/v0.8/catalogItem/1")), "template",
                        new NamedResourceImpl("2", "template", "application/vnd.vmware.vcloud.vAppTemplate+xml", URI
                              .create("https://vcloud.safesecureweb.com/api/v0.8/catalogItem/2"))))));
         }
      }

      @Singleton
      public static class TestOrganizationCatalogItemSupplier extends OrganizationCatalogItemSupplier {
         protected TestOrganizationCatalogItemSupplier() {
            super(null, null);
         }

         @Override
         public Map<String, Map<String, Map<String, ? extends org.jclouds.vcloud.domain.CatalogItem>>> get() {
            return ImmutableMap.<String, Map<String, Map<String, ? extends org.jclouds.vcloud.domain.CatalogItem>>> of(
                  "org", ImmutableMap.<String, Map<String, ? extends org.jclouds.vcloud.domain.CatalogItem>> of(
                        "catalog", ImmutableMap.<String, org.jclouds.vcloud.domain.CatalogItem> of("template",
                              new CatalogItemImpl("2", "template", URI
                                    .create("https://vcloud.safesecureweb.com/api/v0.8/catalogItem/2"), "description",
                                    new NamedResourceImpl("2", "template",
                                          "application/vnd.vmware.vcloud.vAppTemplate+xml", URI
                                                .create("https://vcloud.safesecureweb.com/api/v0.8/vAppTemplate/2")),
                                    ImmutableMap.<String, String> of()))));

         }
      }

   }

}
