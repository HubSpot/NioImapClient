package com.hubspot.imap.imap.folder;

import com.hubspot.imap.grammar.ImapBaseListener;
import com.hubspot.imap.grammar.ImapLexer;
import com.hubspot.imap.grammar.ImapParser;
import com.hubspot.imap.grammar.ImapParser.ArrayContext;
import com.hubspot.imap.grammar.ImapParser.ListresponseContext;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface Folder {
  List<FolderAttribute> getAttributes();
  String getContext();
  String getName();

  class Builder implements Folder {

    private final List<FolderAttribute> attributes = new ArrayList<>();
    private String context;
    private String name;

    public Folder parseFrom(String untaggedResponse) {
      ImapLexer lexer = new ImapLexer(new ANTLRInputStream(untaggedResponse));
      ImapParser parser = new ImapParser(new CommonTokenStream(lexer));

      parser.addParseListener(new Listener());
      parser.listresponse();
      return build();
    }

    public Folder build() {
      return this;
    }

    public List<FolderAttribute> getAttributes() {
      return this.attributes;
    }

    public Builder addAttribute(FolderAttribute attribute) {
      attributes.add(attribute);
      return this;
    }

    public String getContext() {
      return this.context;
    }

    public Builder setContext(String context) {
      this.context = context;
      return this;
    }

    public String getName() {
      return this.name;
    }

    public Builder setName(String name) {
      this.name = name;
      return this;
    }

    private class Listener extends ImapBaseListener {
      @Override
      public void exitListresponse(ListresponseContext ctx) {
        setName(ctx.name.getText());
        setContext(ctx.context.getText());
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
}
