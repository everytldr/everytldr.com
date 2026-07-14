package com.everytldr.api.support.visitor;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class ResolvedAnonymousVisitorArgumentResolver implements HandlerMethodArgumentResolver {
  private static final int VISITOR_ID_BYTE_LENGTH = 32;

  private final AnonymousVisitorProperties properties;
  private final SecureRandom secureRandom = new SecureRandom();

  public ResolvedAnonymousVisitorArgumentResolver(AnonymousVisitorProperties properties) {
    this.properties = Objects.requireNonNull(properties, "properties must not be null");
  }

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.hasParameterAnnotation(ResolvedAnonymousVisitor.class)
        && AnonymousVisitor.class.equals(parameter.getParameterType());
  }

  @Override
  public Object resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {
    HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
    HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);
    if (request == null || response == null) {
      throw new IllegalStateException("anonymous visitor requires an HTTP request and response");
    }

    String visitorId = findVisitorId(request);
    if (!isValidVisitorId(visitorId)) {
      visitorId = createVisitorId();
      addVisitorCookie(response, visitorId);
    }
    return new AnonymousVisitor(hashVisitorId(visitorId));
  }

  private String findVisitorId(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return null;
    }
    for (Cookie cookie : cookies) {
      if (properties.cookieName().equals(cookie.getName())) {
        return cookie.getValue();
      }
    }
    return null;
  }

  private static boolean isValidVisitorId(String value) {
    if (value == null) {
      return false;
    }
    try {
      return Base64.getUrlDecoder().decode(value).length == VISITOR_ID_BYTE_LENGTH;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  private String createVisitorId() {
    byte[] bytes = new byte[VISITOR_ID_BYTE_LENGTH];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private void addVisitorCookie(HttpServletResponse response, String visitorId) {
    ResponseCookie cookie =
        ResponseCookie.from(properties.cookieName(), visitorId)
            .path("/")
            .maxAge(properties.cookieMaxAge())
            .httpOnly(true)
            .secure(properties.cookieSecure())
            .sameSite("Lax")
            .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }

  private String hashVisitorId(String visitorId) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(
          new SecretKeySpec(
              properties.hashSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return HexFormat.of().formatHex(mac.doFinal(visitorId.getBytes(StandardCharsets.UTF_8)));
    } catch (InvalidKeyException | NoSuchAlgorithmException e) {
      throw new IllegalStateException("Unable to hash anonymous visitor", e);
    }
  }
}
