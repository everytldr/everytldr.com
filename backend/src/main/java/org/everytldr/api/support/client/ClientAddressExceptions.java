package org.everytldr.api.support.client;

public final class ClientAddressExceptions {
  private ClientAddressExceptions() {}

  public static class Unavailable extends RuntimeException {
    public Unavailable() {
      super("client ip unavailable");
    }
  }
}
