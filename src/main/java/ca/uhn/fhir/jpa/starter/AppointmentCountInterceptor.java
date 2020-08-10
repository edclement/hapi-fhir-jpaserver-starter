package ca.uhn.fhir.jpa.starter;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.model.api.ExtensionDt;
import ca.uhn.fhir.model.dstu2.resource.Appointment;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.model.dstu2.valueset.ResourceTypeEnum;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.IntegerDt;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.ResponseDetails;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.hl7.fhir.instance.model.api.IBaseResource;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

/**
 * Server interceptor that updates a patients future appointment count FHIR extension on appointment changes
 * Implemented based on documentation provided at: https://hapifhir.io/hapi-fhir/docs/interceptors/built_in_server_interceptors.html#logging-logging-interceptor
 * For list of available interceptors, see: https://github.com/jamesagnew/hapi-fhir/blob/v5.0.2/hapi-fhir-server/src/main/java/ca/uhn/fhir/rest/server/interceptor/IServerInterceptor.java
 */
@Interceptor
public class AppointmentCountInterceptor {
  public static final List<String> EXCLUDED_OPERATIONS = Arrays.asList(
    "metadata" // comma separated list of strings
  );
  private static final String RESOURCE_ATTRIBUTE = "RESOURCE";
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AppointmentCountInterceptor.class);

  private final DaoRegistry daoRegistry;

  public AppointmentCountInterceptor(DaoRegistry daoRegistry) {
    this.daoRegistry = daoRegistry;
  }

  @Hook(Pointcut.SERVER_OUTGOING_RESPONSE)
  public boolean outgoingResponse (
    RequestDetails requestDetails,
    ResponseDetails responseDetails,
    HttpServletRequest servletRequest,
    HttpServletResponse servletResponse
  ) {
    IBaseResource resource = responseDetails.getResponseResource();
    if (resource == null) {
      logger.warn(String.format("Unable to get resource data for %s request %s", servletRequest.getMethod(), servletRequest.getRequestURI()));
      return true;
    } else if (!(resource instanceof Appointment)) {
      return true; // not an appointment, nothing to do
    }

    Appointment appointment = (Appointment)resource;
    for (Appointment.Participant participant: appointment.getParticipant()) {
      if (participant.getActor().getReference().getResourceType().equals(ResourceTypeEnum.PATIENT.getCode())) {
        String patientId = participant.getActor().getReference().toString();
        try {
          updatePatientFutureAppointmentCount(patientId);
          logger.info(String.format("successfully updated future appointment count for patient '%s'", patientId));
        } catch (Exception exception) {
          logger.error(String.format("failed to update patient future appointment count for patient '%s'", patientId), exception);
        }
      }
    }
    return true;
  }

  private void updatePatientFutureAppointmentCount(String patientId) {
    SearchParameterMap params = new SearchParameterMap();
    params.add(Appointment.SP_DATE, new DateParam(ParamPrefixEnum.GREATERTHAN, DateTimeDt.withCurrentTime()));
    params.add(Appointment.SP_ACTOR, new ReferenceParam(patientId));
    params.setLoadSynchronous(true);
    IBundleProvider bundle = daoRegistry.getResourceDao(Appointment.class).search(params);
    setPatientFutureAppointmentCount(patientId, bundle.size());
  }

  private void setPatientFutureAppointmentCount(String patientId, int count) {
    IFhirResourceDao<Patient> patientDao = daoRegistry.getResourceDao(Patient.class);
    Patient patient = new Patient();
    patient.setId(patientId);
    patient = patientDao.read(patient.getIdElement());
    List<ExtensionDt> extensions = patient.getUndeclaredExtensionsByUrl("http://integer");
    if (extensions == null || extensions.size() == 0) {
      ExtensionDt futureAppointmentCount = new ExtensionDt(
        false, // https://www.hl7.org/fhir/conformance-rules.html#modifier
        "http://integer",
        new IntegerDt(count)
      );
      patient.addUndeclaredExtension(futureAppointmentCount);
    } else {
      ExtensionDt extension = extensions.get(0);
      extension.setValue(new IntegerDt(count));
    }
    patientDao.update(patient);
  }
}
