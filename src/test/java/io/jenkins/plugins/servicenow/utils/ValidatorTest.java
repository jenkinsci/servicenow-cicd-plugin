package io.jenkins.plugins.servicenow.utils;

import junit.framework.TestCase;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ValidatorTest extends TestCase {

    @Test
    public void testUrlValidationWithSuccess() {
        // given
        String url = "http://test.com";

        // when
        boolean result = Validator.validateInstanceUrl(url);

        // then
        assertThat(result).isTrue();
    }

    @Test
    public void testSSLUrlValidationWithSuccess() {
        // given
        String url = "https://test.com";

        // when
        boolean result = Validator.validateInstanceUrl(url);

        // then
        assertThat(result).isTrue();
    }

    @Test
    public void testUrlValidationWithEmptyUrl() {
        // given
        String url = "";

        // when
        boolean result = Validator.validateInstanceUrl(url);

        // then
        assertThat(result).isTrue();
    }

    @Test
    public void testUrlValidationWithNull() {
        // given
        String url = null;

        // when
        boolean result = Validator.validateInstanceUrl(url);

        // then
        assertThat(result).isTrue();
    }

    @Test
    public void testUrlValidationWithBadUrl() {
        // given
        String url = "test.com";

        // when
        boolean result = Validator.validateInstanceUrl(url);

        // then
        assertThat(result).isFalse();
    }

}