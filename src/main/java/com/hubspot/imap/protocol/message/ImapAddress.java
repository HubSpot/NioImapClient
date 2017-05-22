package com.hubspot.imap.protocol.message;

import java.util.List;
import java.util.Optional;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

public interface ImapAddress {
  String getPersonal();
  boolean hasPersonal();
  String getAddress();

  class Builder implements ImapAddress {
    private static final String NIL = "NIL";
    private static final Joiner AT_JOINER = Joiner.on("@").skipNulls();
    private Optional<String> personal = Optional.empty();
    private String address;

    public ImapAddress build() {
      return this;
    }

    /**
     * Parses an email address from a list of strings according to RFC3501:
     *
     *    An address structure is a parenthesized list that describes an
     *    electronic mail address.  The fields of an address structure
     *    are in the following order: personal name, [SMTP]
     *    at-domain-list (source route), mailbox name, and host name.
     *
     * The SMTP at-domain-list is explicitly ignored.
     *
     * @param imapAddress List of address pars from imap FETCH response
     * @return
     */
    public Builder parseFrom(List<String> imapAddress) {
      String personal = imapAddress.get(0);
      if (personal != null) {
        setPersonal(personal);
      }

      String address = AT_JOINER.join(imapAddress.get(2), imapAddress.get(3));
      setAddress(address);

      return this;
    }

    public String getPersonal() {
      return this.personal.orElse(null);
    }

    public boolean hasPersonal() {
      return this.personal.isPresent();
    }

    public Builder setPersonal(String personal) {
      this.personal = Optional.of(personal);
      return this;
    }

    public String getAddress() {
      return this.address;
    }

    public Builder setAddress(String address) {
      this.address = address;
      return this;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("personal", personal)
          .add("address", address)
          .toString();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Builder builder = (Builder) o;
      return Objects.equal(getPersonal(), builder.getPersonal()) &&
          Objects.equal(getAddress(), builder.getAddress());
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(getPersonal(), getAddress());
    }
  }
}
