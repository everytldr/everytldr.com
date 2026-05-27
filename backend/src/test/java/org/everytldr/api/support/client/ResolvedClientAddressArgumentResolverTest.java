package org.everytldr.api.support.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

class ResolvedClientAddressArgumentResolverTest {
  private final ResolvedClientAddressArgumentResolver resolver =
      new ResolvedClientAddressArgumentResolver("test-secret");

  @Test
  void supportsRequiresAnnotationAndClientAddressType() throws NoSuchMethodException {
    assertThat(resolver.supportsParameter(parameterOf("annotated", ClientAddress.class))).isTrue();
    assertThat(resolver.supportsParameter(parameterOf("unannotated", ClientAddress.class)))
        .isFalse();
    assertThat(resolver.supportsParameter(parameterOf("wrongType", String.class))).isFalse();
  }

  @Test
  void resolvesVercelForwardedForBeforeOtherHeaders() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("X-Vercel-Forwarded-For", "203.0.113.42");
    request.addHeader("X-Forwarded-For", "198.51.100.8");

    ClientAddress address = resolve(request);

    assertThat(address.rawIp()).isEqualTo("203.0.113.42");
    assertThat(address.maskedIp()).isEqualTo("203.0");
    assertThat(address.ipHash()).hasSize(64);
  }

  @Test
  void resolvesFirstForwardedForIp() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("X-Forwarded-For", "198.51.100.8, 10.0.0.1");

    ClientAddress address = resolve(request);

    assertThat(address.rawIp()).isEqualTo("198.51.100.8");
    assertThat(address.maskedIp()).isEqualTo("198.51");
  }

  @Test
  void masksIpv6ToFirstThreeHextets() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("X-Forwarded-For", "2001:db8:abcd:1234::1");

    ClientAddress address = resolve(request);

    assertThat(address.rawIp()).isEqualTo("2001:db8:abcd:1234:0:0:0:1");
    assertThat(address.maskedIp()).isEqualTo("2001:db8:abcd");
  }

  @Test
  void failsClosedWhenForwardedIpIsMissingOrInvalid() {
    assertThatThrownBy(() -> resolve(new MockHttpServletRequest()))
        .isInstanceOf(ClientAddressExceptions.Unavailable.class)
        .hasMessageContaining("client ip unavailable");

    MockHttpServletRequest invalid = new MockHttpServletRequest();
    invalid.addHeader("X-Forwarded-For", "not-an-ip");
    assertThatThrownBy(() -> resolve(invalid))
        .isInstanceOf(ClientAddressExceptions.Unavailable.class)
        .hasMessageContaining("client ip unavailable");
  }

  private ClientAddress resolve(MockHttpServletRequest request) throws Exception {
    return (ClientAddress)
        resolver.resolveArgument(
            parameterOf("annotated", ClientAddress.class),
            null,
            new ServletWebRequest(request),
            null);
  }

  private static MethodParameter parameterOf(String methodName, Class<?> paramType)
      throws NoSuchMethodException {
    Method method = Fixtures.class.getDeclaredMethod(methodName, paramType);
    return new MethodParameter(method, 0);
  }

  @SuppressWarnings("unused")
  private static class Fixtures {
    void annotated(@ResolvedClientAddress ClientAddress clientAddress) {}

    void unannotated(ClientAddress clientAddress) {}

    void wrongType(@ResolvedClientAddress String clientAddress) {}
  }
}
