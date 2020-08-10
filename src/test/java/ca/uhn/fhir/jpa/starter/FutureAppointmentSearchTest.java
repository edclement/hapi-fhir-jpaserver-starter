package ca.uhn.fhir.jpa.starter;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.ExtensionDt;
import ca.uhn.fhir.model.dstu2.resource.Appointment;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.model.primitive.IntegerDt;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.gclient.NumberClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.test.utilities.JettyUtil;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class FutureAppointmentSearchTest {
  private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(FutureAppointmentSearchTest.class);
  private static IGenericClient ourClient;
  private static FhirContext ourCtx;
  private static int ourPort;
  private static Server ourServer;

  static {
    HapiProperties.forceReload();
    HapiProperties.setProperty(HapiProperties.FHIR_VERSION, "DSTU2");
    HapiProperties.setProperty(HapiProperties.DATASOURCE_URL, "jdbc:h2:mem:dbr2");
  }

  @AfterClass
  public static void afterClass() throws Exception {
    ourServer.stop();
  }

  @BeforeClass
  public static void beforeClass() throws Exception {
    String path = Paths.get("").toAbsolutePath().toString();

    ourLog.info("Project base path is: {}", path);

    ourServer = new Server(0);

    WebAppContext webAppContext = new WebAppContext();
    webAppContext.setContextPath("/hapi-fhir-jpaserver");
    webAppContext.setDescriptor(path + "/src/main/webapp/WEB-INF/web.xml");
    webAppContext.setResourceBase(path + "/target/hapi-fhir-jpaserver-starter");
    webAppContext.setParentLoaderPriority(true);

    ourServer.setHandler(webAppContext);
    ourServer.start();
    ourPort = JettyUtil.getPortForStartedServer(ourServer);

    org.springframework.context.ApplicationContext appCtx = (ApplicationContext) webAppContext.getServletContext()
      .getAttribute("org.springframework.web.context.WebApplicationContext.ROOT");

    ourCtx = appCtx.getBean(FhirContext.class);

    ourCtx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
    ourCtx.getRestfulClientFactory().setSocketTimeout(1200 * 1000);
    String ourServerBase = "http://localhost:" + ourPort + "/hapi-fhir-jpaserver/fhir/";
    ourClient = ourCtx.newRestfulGenericClient(ourServerBase);
    ourClient.registerInterceptor(new LoggingInterceptor(true));
  }

  @Test
  public void testIntegerParamSearch() {
    IParser parser = ourCtx.newJsonParser();
    Patient patient = parser.parseResource(
      Patient.class,
      "{\n" +
        "  \"resourceType\": \"Patient\",\n" +
        "  \"id\": \"Patient1\",\n" +
        "  \"meta\": {\n" +
        "    \"versionId\": \"4\",\n" +
        "    \"lastUpdated\": \"2020-08-07T15:29:19.421-04:00\"\n" +
        "  },\n" +
        "  \"text\": {\n" +
        "  \"status\": \"generated\",\n" +
        "  \"div\": \"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\"><div class=\\\"hapiHeaderText\\\">Patient1 <b>PATIENT1 </b></div><table class=\\\"hapiPropertyTable\\\"><tbody><tr><td>Date of birth</td><td><span>01 January 1981</span></td></tr></tbody></table></div>\"\n" +
        "  },\n" +
        "  \"extension\": [ {\n" +
        "    \"url\": \"http://token\",\n" +
        "    \"valueCode\": \"blue\"\n" +
        "  } ],\n" +
        "  \"active\": true,\n" +
        "  \"name\": [ {\n" +
        "    \"use\": \"official\",\n" +
        "    \"family\": [ \"Patient1\" ],\n" +
        "    \"given\": [ \"Patient1\" ]\n" +
        "  } ],\n" +
        "  \"gender\": \"male\",\n" +
        "  \"birthDate\": \"1981-01-01\"\n" +
        "}"
    );

    Appointment appointment1 = parser.parseResource(
      Appointment.class,
      "{\n" +
        "        \"resourceType\": \"Appointment\",\n" +
        "        \"status\": \"booked\",\n" +
        "        \"type\": {\n" +
        "          \"coding\": [{ \"code\": \"52\", \"display\": \"General Discussion\" }]\n" +
        "        },\n" +
        "        \"start\": \"2021-01-10T09:00:00Z\",\n" +
        "        \"end\": \"2021-01-10T11:00:00Z\",\n" +
        "        \"participant\": [\n" +
        "          {\n" +
        "            \"actor\": {\n" +
        "              \"reference\": \"Patient/Patient1\",\n" +
        "              \"display\": \"Patient1\"\n" +
        "            },\n" +
        "            \"required\": \"required\",\n" +
        "            \"status\": \"accepted\"\n" +
        "          }\n" +
        "        ]\n" +
        "      }"
    );

    Appointment appointment2 = parser.parseResource(
      Appointment.class,
      "{\n" +
        "        \"resourceType\": \"Appointment\",\n" +
        "        \"status\": \"booked\",\n" +
        "        \"type\": {\n" +
        "          \"coding\": [{ \"code\": \"52\", \"display\": \"General Discussion\" }]\n" +
        "        },\n" +
        "        \"start\": \"2021-01-11T09:00:00Z\",\n" +
        "        \"end\": \"2021-01-11T11:00:00Z\",\n" +
        "        \"participant\": [\n" +
        "          {\n" +
        "            \"actor\": {\n" +
        "              \"reference\": \"Patient/Patient1\",\n" +
        "              \"display\": \"Patient1\"\n" +
        "            },\n" +
        "            \"required\": \"required\",\n" +
        "            \"status\": \"accepted\"\n" +
        "          }\n" +
        "        ]\n" +
        "      }"
    );

    // only includes eyecolour extension, persisting the appointment will add
    // the future-appointment-count extension to the patient
    ourClient.update().resource(patient).withId(patient.getId()).execute();

    // triggers the future appointment count interceptor...
    ourClient.create().resource(appointment1).execute();
    ourClient.create().resource(appointment2).execute();

    Bundle eyecolourBundle = ourClient
      .search()
      .forResource(Patient.class)
      .where(new TokenClientParam("eyecolour").exactly().code("blue"))
      .returnBundle(Bundle.class)
      .execute();
    assertEquals(eyecolourBundle.getTotal().intValue(), 1);

    // this passes, patient retrieved from the eyecolour search has a future-appointment-count set
    Patient searchResultPatient = (Patient) eyecolourBundle.getEntry().get(0).getResource();
    List<ExtensionDt> appointmentCountExtensions = searchResultPatient.getUndeclaredExtensionsByUrl("http://integer");
    assertEquals(appointmentCountExtensions.size(), 1);
    assertEquals(appointmentCountExtensions.get(0).getValue().toString(), new IntegerDt(2).toString());

    // this fails, even though we just proved that the single patient has both the eyecolour and future-appointment-count extensions
    Bundle futureAppointmentCountBundle1 = ourClient
      .search()
      .forResource(Patient.class)
      .where(new NumberClientParam("future-appointment-count").exactly().number(2))
      .returnBundle(Bundle.class)
      .execute();
    assertEquals(futureAppointmentCountBundle1.getTotal().intValue(), 1);

    // this fails, even though we just proved that the single patient has both the eyecolour and future-appointment-count extensions
    Bundle futureAppointmentCountBundle2 = ourClient
      .search()
      .forResource(Patient.class)
      .where(new NumberClientParam("future-appointment-count").greaterThan().number(1))
      .returnBundle(Bundle.class)
      .execute();
    assertEquals(futureAppointmentCountBundle2.getTotal().intValue(), 1);
  }
}