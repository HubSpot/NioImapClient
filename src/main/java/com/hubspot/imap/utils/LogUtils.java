package com.hubspot.imap.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogUtils {

  public static Logger loggerWithName(Class klass, String name) {
    return LoggerFactory.getLogger(String.format("%s.%s", klass.getName(), name));
  }
}
