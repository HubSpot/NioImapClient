package com.hubspot.imap.utils;

/**
 * Utility class to mark when a NIL object has been found in a response.
 * This could be a string or array, but it is not possible to tell without context.
 */
public class NilMarker {
  public static final NilMarker INSTANCE = new NilMarker();
}
