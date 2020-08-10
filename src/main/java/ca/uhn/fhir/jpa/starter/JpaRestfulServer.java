package ca.uhn.fhir.jpa.starter;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.searchparam.registry.ISearchParamRegistry;
import ca.uhn.fhir.model.dstu2.resource.SearchParameter;
import ca.uhn.fhir.model.dstu2.valueset.XPathUsageTypeEnum;
import org.springframework.context.ApplicationContext;

import javax.servlet.ServletException;

public class JpaRestfulServer extends BaseJpaRestfulServer {

  private static final long serialVersionUID = 1L;

  @Override
  protected void initialize() throws ServletException {
    super.initialize();

    org.springframework.context.ApplicationContext appCtx = (ApplicationContext) getServletContext()
      .getAttribute("org.springframework.web.context.WebApplicationContext.ROOT");
    DaoRegistry daoRegistry = appCtx.getBean(DaoRegistry.class);

    AppointmentCountInterceptor appointmentCountInterceptor = new AppointmentCountInterceptor(daoRegistry);
    this.registerInterceptor(appointmentCountInterceptor);

    // This DOES NOT work - NUMBER type search param
    SearchParameter numberParameter = new ca.uhn.fhir.model.dstu2.resource.SearchParameter();
    numberParameter.setName("Future Appointment Count");
    numberParameter.setCode("future-appointment-count");
    numberParameter.setDescription("Count of future appointments for the patient");
    numberParameter.setUrl("http://integer"); // not what I'm really using as the url
    numberParameter.setStatus(ca.uhn.fhir.model.dstu2.valueset.ConformanceResourceStatusEnum.ACTIVE);
    numberParameter.setBase(ca.uhn.fhir.model.dstu2.valueset.ResourceTypeEnum.PATIENT);
    numberParameter.setType(ca.uhn.fhir.model.dstu2.valueset.SearchParamTypeEnum.NUMBER);
    numberParameter.setXpathUsage(XPathUsageTypeEnum.NORMAL);
    numberParameter.setXpath("Patient.extension('http://integer')"); // not what I'm really using as the url

    // This DOES work - TOKEN type search param
    SearchParameter tokenParameter = new ca.uhn.fhir.model.dstu2.resource.SearchParameter();
    tokenParameter.setName("Eye Colour");
    tokenParameter.setCode("eyecolour");
    tokenParameter.setDescription("Eye colour of the patient");
    tokenParameter.setUrl("http://token"); // not what I'm really using as the url
    tokenParameter.setStatus(ca.uhn.fhir.model.dstu2.valueset.ConformanceResourceStatusEnum.ACTIVE);
    tokenParameter.setBase(ca.uhn.fhir.model.dstu2.valueset.ResourceTypeEnum.PATIENT);
    tokenParameter.setType(ca.uhn.fhir.model.dstu2.valueset.SearchParamTypeEnum.TOKEN);
    tokenParameter.setXpathUsage(XPathUsageTypeEnum.NORMAL);
    tokenParameter.setXpath("Patient.extension('http://token')"); // not what I'm really using as the url

    IFhirResourceDao<SearchParameter> searchParamDao = daoRegistry.getResourceDao(SearchParameter.class);
    searchParamDao.create(numberParameter);
    searchParamDao.create(tokenParameter);

    ISearchParamRegistry searchParamRegistry = appCtx.getBean(ISearchParamRegistry.class);
    searchParamRegistry.forceRefresh();
  }

}
