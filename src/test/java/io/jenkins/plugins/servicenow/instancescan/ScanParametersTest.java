package io.jenkins.plugins.servicenow.instancescan;

import junit.framework.TestCase;
import org.hamcrest.Matchers;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ScanParametersTest {

    @Test
    public void shouldAddParameters() {
        // given
        String param1 = "value1";
        String param2 = "value2";

        // when
        String[] result = ScanParameters.params().add(param1).add(param2).build();

        // then
        assertThat(result, Matchers.notNullValue());
        assertThat(result.length, is(2));
        assertThat(result[0], is(param1));
        assertThat(result[1], is(param2));
    }

    @Test
    public void shouldAcceptEmptyParameter() {
        // given
        String param1 = null;

        // when
        String[] result = ScanParameters.params().add(param1).build();

        // then
        assertThat(result, Matchers.notNullValue());
        assertThat(result.length, is(1));
        assertThat(result[0], Matchers.nullValue());
    }

    @Test
    public void shouldReturnEmptyArrayIfNoParameters() {
        // given

        // when
        String[] result = ScanParameters.params().build();

        // then
        assertThat(result, Matchers.notNullValue());
        assertThat(result.length, is(0));
    }
}