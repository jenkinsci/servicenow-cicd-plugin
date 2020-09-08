package io.jenkins.plugins.servicenow.parameter;

import hudson.model.FreeStyleProject;
import hudson.model.ParametersDefinitionProperty;
import hudson.util.FormValidation;
import io.jenkins.plugins.servicenow.Messages;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import javax.servlet.ServletException;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class ServiceNowParameterDefinitionIntegrationTest {

    private FreeStyleProject project;

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void testSuccessfullyCreatedParameter() throws IOException, ServletException {
        // given
        project = jenkins.createFreeStyleProject("testServiceNowParameter");
        ServiceNowParameterDefinition serviceNowParameter = new ServiceNowParameterDefinition(null, null,
                TestData.instanceForPublishedAppUrl_correct, null, TestData.instanceForInstalledAppUrl_correct, null, null, null, null);
        project.addProperty(new ParametersDefinitionProperty(serviceNowParameter));

        // when
        FormValidation resultValidationOfPublishedInstance = serviceNowParameter.getDescriptor().doCheckInstanceForPublishedAppUrl(TestData.instanceForPublishedAppUrl_correct);
        FormValidation resultValidationOfInstalledInstance = serviceNowParameter.getDescriptor().doCheckInstanceForPublishedAppUrl(TestData.instanceForInstalledAppUrl_correct);

        // then
        assertThat(resultValidationOfPublishedInstance).isEqualTo(FormValidation.ok());
        assertThat(resultValidationOfInstalledInstance).isEqualTo(FormValidation.ok());
    }

    @Test
    public void testWronglyCreatedParameter() throws IOException, ServletException {
        // given
        project = jenkins.createFreeStyleProject("testServiceNowParameter");
        ServiceNowParameterDefinition serviceNowParameter = new ServiceNowParameterDefinition(null, null,
                TestData.instanceForPublishedAppUrl_incorrect, null, TestData.instanceForInstalledAppUrl_incorrect, null, null, null, null);
        project.addProperty(new ParametersDefinitionProperty(serviceNowParameter));

        // when
        FormValidation resultValidationOfPublishedInstance = serviceNowParameter.getDescriptor().doCheckInstanceForPublishedAppUrl(TestData.instanceForPublishedAppUrl_incorrect);
        FormValidation resultValidationOfInstalledInstance = serviceNowParameter.getDescriptor().doCheckInstanceForPublishedAppUrl(TestData.instanceForInstalledAppUrl_incorrect);

        // then
        assertThat(resultValidationOfPublishedInstance.kind).isEqualTo(FormValidation.Kind.ERROR);
        assertThat(resultValidationOfPublishedInstance.getMessage()).isEqualTo(Messages.ServiceNowParameterDefinition_DescriptorImpl_errors_wrongInstanceForPublishedAppUrl());
        assertThat(resultValidationOfInstalledInstance.kind).isEqualTo(FormValidation.Kind.ERROR);
        assertThat(resultValidationOfInstalledInstance.getMessage()).isEqualTo(Messages.ServiceNowParameterDefinition_DescriptorImpl_errors_wrongInstanceForPublishedAppUrl());
    }

    private interface TestData {
        String instanceForPublishedAppUrl_correct = "https://publish-instance.service-now.com";
        String instanceForInstalledAppUrl_correct = "https://install-instance.service-now.com";
        String instanceForPublishedAppUrl_incorrect = "publish-instance.service-now.com";
        String instanceForInstalledAppUrl_incorrect = "install-instance.service-now.com";
    }
}
