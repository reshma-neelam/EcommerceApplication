package com.scaler.paymentprocessor.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class StripeConfigTest {

    private StripeConfig configWithKey(String key) {
        StripeProperties props = new StripeProperties();
        props.getStripe().setSecretKey(key);
        return new StripeConfig(props);
    }

    @Test
    void init_liveKey_failsFast() {
        StripeConfig config = configWithKey("sk_live_realmoney");

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(config, "init"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void init_testKey_isAccepted() {
        StripeConfig config = configWithKey("sk_test_dummy");

        assertThatCode(() -> ReflectionTestUtils.invokeMethod(config, "init")).doesNotThrowAnyException();
    }
}
