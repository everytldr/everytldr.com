package com.everytldr.api.support.visitor;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.Cookie;
import java.lang.reflect.Method;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.ServletWebRequest;

class ResolvedAnonymousVisitorArgumentResolverTest {
  private static final String COOKIE_NAME = "everytldr_visitor";

  private final ResolvedAnonymousVisitorArgumentResolver resolver =
      new ResolvedAnonymousVisitorArgumentResolver(
          new AnonymousVisitorProperties(COOKIE_NAME, Duration.ofDays(365), true, "test-secret"));

  @Test
  void supportsRequiresAnnotationAndAnonymousVisitorType() throws NoSuchMethodException {
    assertThat(resolver.supportsParameter(parameterOf("annotated", AnonymousVisitor.class)))
        .isTrue();
    assertThat(resolver.supportsParameter(parameterOf("unannotated", AnonymousVisitor.class)))
        .isFalse();
    assertThat(resolver.supportsParameter(parameterOf("wrongType", String.class))).isFalse();
  }

  @Test
  void createsVisitorCookieWhenMissing() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();

    AnonymousVisitor visitor = resolve(new MockHttpServletRequest(), response);

    assertThat(visitor.visitorHash()).hasSize(64);
    assertThat(response.getHeader("Set-Cookie"))
        .startsWith(COOKIE_NAME + "=")
        .contains("Path=/")
        .contains("Max-Age=31536000")
        .contains("HttpOnly")
        .contains("Secure")
        .contains("SameSite=Lax");
  }

  @Test
  void reusesValidVisitorCookieWithoutIssuingAnotherCookie() throws Exception {
    MockHttpServletResponse initialResponse = new MockHttpServletResponse();
    AnonymousVisitor initialVisitor = resolve(new MockHttpServletRequest(), initialResponse);
    String visitorId = extractVisitorId(initialResponse.getHeader("Set-Cookie"));

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(new Cookie(COOKIE_NAME, visitorId));
    MockHttpServletResponse response = new MockHttpServletResponse();
    AnonymousVisitor reusedVisitor = resolve(request, response);

    assertThat(reusedVisitor).isEqualTo(initialVisitor);
    assertThat(response.getHeader("Set-Cookie")).isNull();
  }

  @Test
  void replacesInvalidVisitorCookie() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(new Cookie(COOKIE_NAME, "invalid"));
    MockHttpServletResponse response = new MockHttpServletResponse();

    AnonymousVisitor visitor = resolve(request, response);

    assertThat(visitor.visitorHash()).hasSize(64);
    assertThat(response.getHeader("Set-Cookie")).startsWith(COOKIE_NAME + "=");
  }

  private AnonymousVisitor resolve(MockHttpServletRequest request, MockHttpServletResponse response)
      throws Exception {
    return (AnonymousVisitor)
        resolver.resolveArgument(
            parameterOf("annotated", AnonymousVisitor.class),
            null,
            new ServletWebRequest(request, response),
            null);
  }

  private static String extractVisitorId(String setCookie) {
    return setCookie.substring(COOKIE_NAME.length() + 1, setCookie.indexOf(';'));
  }

  private static MethodParameter parameterOf(String methodName, Class<?> parameterType)
      throws NoSuchMethodException {
    Method method = Fixtures.class.getDeclaredMethod(methodName, parameterType);
    return new MethodParameter(method, 0);
  }

  @SuppressWarnings("unused")
  private static class Fixtures {
    void annotated(@ResolvedAnonymousVisitor AnonymousVisitor visitor) {}

    void unannotated(AnonymousVisitor visitor) {}

    void wrongType(@ResolvedAnonymousVisitor String visitor) {}
  }
}
