package com.hubspot.imap.imap.folder;

import com.hubspot.imap.grammar.ImapBaseListener;
import com.hubspot.imap.grammar.ImapLexer;
import com.hubspot.imap.grammar.ImapParser;
import com.hubspot.imap.grammar.ImapParser.ArrayContext;
import com.hubspot.imap.grammar.ImapParser.ListresponseContext;
import com.hubspot.imap.imap.exceptions.ResponseParseException;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Folder {
  private static final Logger LOGGER = LoggerFactory.getLogger(Folder.class);

  private final List<FolderAttribute> attributes;
  private String context;
  private String name;

  public Folder() {
    attributes = new ArrayList<>();
  }

  public static Folder parseFromListResponse(String untaggedResponse) throws ResponseParseException {
    return new Folder().parse(untaggedResponse);
  }

  public Folder parse(String untaggedResponse) {
    ImapLexer lexer = new ImapLexer(new ANTLRInputStream(untaggedResponse));
    ImapParser parser = new ImapParser(new CommonTokenStream(lexer));

    parser.addParseListener(new Listener());
    parser.listresponse();
    return this;
  }

  private class Listener extends ImapBaseListener {
    @Override
    public void exitListresponse(ListresponseContext ctx) {
      name = ctx.name.getText();
      context = ctx.context.getText();
    }

    @Override
    public void exitArray(ArrayContext ctx) {
      Optional<FolderAttribute> attributeOptional = FolderAttribute.getAttribute(ctx.value.getText());
      if (attributeOptional.isPresent()) {
        attributes.add(attributeOptional.get());
      }
    }
  }
}
