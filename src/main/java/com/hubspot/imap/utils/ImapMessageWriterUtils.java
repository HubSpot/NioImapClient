package com.hubspot.imap.utils;

import com.hubspot.imap.protocol.message.ImapMessage;
import com.hubspot.imap.protocol.message.UnfetchedFieldException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.james.mime4j.dom.MessageWriter;
import org.apache.james.mime4j.message.DefaultMessageWriter;

public class ImapMessageWriterUtils {

  public static String messageBodyToString(ImapMessage imapMessage)
    throws UnfetchedFieldException, IOException {
    MessageWriter writer = new DefaultMessageWriter();
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    CRLFOutputStream crlfOutputStream = new CRLFOutputStream(outputStream);

    writer.writeMessage(imapMessage.getBody(), crlfOutputStream);
    return outputStream.toString(imapMessage.getBody().getCharset());
  }
}
