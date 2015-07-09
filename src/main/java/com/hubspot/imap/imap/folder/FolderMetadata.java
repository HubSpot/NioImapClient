package com.hubspot.imap.imap.folder;

import com.hubspot.imap.grammar.ImapBaseListener;
import com.hubspot.imap.grammar.ImapLexer;
import com.hubspot.imap.grammar.ImapParser;
import com.hubspot.imap.grammar.ImapParser.ArrayContext;
import com.hubspot.imap.grammar.ImapParser.ListContext;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public interface FolderMetadata {
  List<FolderAttribute> getAttributes();
  String getContext();
  String getName();

  class Builder implements FolderMetadata {

    private final List<FolderAttribute> attributes = new ArrayList<>();
    private String context;
    private String name;

    public FolderMetadata parseFrom(String untaggedResponse) {
      ImapLexer lexer = new ImapLexer(new ANTLRInputStream(untaggedResponse));
      ImapParser parser = new ImapParser(new CommonTokenStream(lexer));

      parser.addParseListener(new Listener());
      parser.list();
      return build();
    }

    public FolderMetadata build() {
      return this;
    }

    public List<FolderAttribute> getAttributes() {
      return this.attributes;
    }

    public Builder addAttribute(FolderAttribute attribute) {
      attributes.add(attribute);
      return this;
    }

    public Builder addAllAttributes(List<FolderAttribute> attributes) {
      this.attributes.addAll(attributes);
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
      public void exitList(ListContext ctx) {
        setName(ctx.name.getText());
        setContext(ctx.context.getText());
      }

      @Override
      public void exitArray(ArrayContext ctx) {
        addAllAttributes(
            ctx.value.stream()
                .map(sc -> FolderAttribute.getAttribute(sc.getText()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList())
        );
      }
    }
  }
}
