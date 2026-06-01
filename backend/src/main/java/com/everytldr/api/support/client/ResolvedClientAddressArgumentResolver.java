package com.everytldr.api.support.client;

import jakarta.servlet.http.HttpServletRequest;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Stream;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class ResolvedClientAddressArgumentResolver implements HandlerMethodArgumentResolver {
  private final String hashSecret;

  public ResolvedClientAddressArgumentResolver(String hashSecret) {
    if (hashSecret == null || hashSecret.isBlank()) {
      throw new IllegalStateException("client address hash secret must not be blank");
    }
    this.hashSecret = hashSecret;
  }

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.hasParameterAnnotation(ResolvedClientAddress.class)
        && ClientAddress.class.equals(parameter.getParameterType());
  }

  @Override
  public Object resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {
    HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
    if (request == null) {
      throw new ClientAddressExceptions.Unavailable();
    }

    String rawIp = resolveRawIp(request);
    InetAddress address = parseIp(rawIp);
    String normalizedIp = address.getHostAddress();

    return new ClientAddress(normalizedIp, hashClientIp(normalizedIp), maskIp(address));
  }

  private static String resolveRawIp(HttpServletRequest request) {
    return Stream.of(
            request.getHeader("X-Vercel-Forwarded-For"),
            request.getHeader("X-Forwarded-For"),
            request.getHeader("X-Real-IP"))
        .filter(header -> header != null && !header.isBlank())
        .map(header -> header.split(",", 2)[0].trim())
        .filter(candidate -> !candidate.isBlank())
        .findFirst()
        .orElseThrow(ClientAddressExceptions.Unavailable::new);
  }

  private static InetAddress parseIp(String rawIp) {
    if (rawIp == null || rawIp.isBlank()) {
      throw new ClientAddressExceptions.Unavailable();
    }

    try {
      InetAddress address = InetAddress.getByName(rawIp);
      if (address instanceof Inet6Address) {
        return address;
      }
      if (!rawIp.equals(address.getHostAddress())) {
        throw new ClientAddressExceptions.Unavailable();
      }
      return address;
    } catch (UnknownHostException e) {
      throw new ClientAddressExceptions.Unavailable();
    }
  }

  private String hashClientIp(String value) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(hashSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] bytes = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder out = new StringBuilder(bytes.length * 2);
      for (byte b : bytes) {
        out.append(String.format("%02x", b));
      }
      return out.toString();
    } catch (InvalidKeyException | NoSuchAlgorithmException e) {
      throw new IllegalStateException("Unable to hash client address", e);
    }
  }

  private static String maskIp(InetAddress address) {
    byte[] bytes = address.getAddress();
    if (address instanceof Inet6Address) {
      return "%x:%x:%x"
          .formatted(
              toUnsignedShort(bytes[0], bytes[1]),
              toUnsignedShort(bytes[2], bytes[3]),
              toUnsignedShort(bytes[4], bytes[5]));
    }
    return "%d.%d".formatted(Byte.toUnsignedInt(bytes[0]), Byte.toUnsignedInt(bytes[1]));
  }

  private static int toUnsignedShort(byte high, byte low) {
    return (Byte.toUnsignedInt(high) << 8) | Byte.toUnsignedInt(low);
  }
}
