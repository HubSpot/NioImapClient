package com.hubspot.imap.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.james.mime4j.dom.MessageWriter;
import org.apache.james.mime4j.message.DefaultMessageWriter;

import com.hubspot.imap.protocol.message.ImapMessage;
import com.hubspot.imap.protocol.message.UnfetchedFieldException;

public class ImapMessageWriterUtils {

  public static String messageBodyToString(ImapMessage imapMessage) throws UnfetchedFieldException, IOException {
    MessageWriter writer = new DefaultMessageWriter();
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    writer.writeMessage(imapMessage.getBody(), outputStream);
    return outputStream.toString(imapMessage.getBody().getCharset());
  }

  public static String messageBodyToStringCRLF(ImapMessage imapMessage) throws UnfetchedFieldException, IOException {
    return terminateLinesWithCRLF(messageBodyToString(imapMessage));
  }

  public static String terminateLinesWithCRLF(String messageString) {
    return messageString.replaceAll("((?<!\\r)\\n|\\r(?!\\n))", "\r\n");
  }
}
