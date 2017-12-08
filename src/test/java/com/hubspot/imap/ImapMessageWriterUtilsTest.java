package com.hubspot.imap;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.hubspot.imap.utils.ImapMessageWriterUtils;

public class ImapMessageWriterUtilsTest {

  @Test
  public void replacesLineWithCLRF() throws Exception {
    String withN = "This is \n a test \n";
    String withR = "This is \r a test \r";
    String correct = "This is \r\n a test \r\n";
    String incorrect = "This is a test";

    assertThat(ImapMessageWriterUtils.terminateLinesWithCRLF(withN)).isEqualTo(correct);
    assertThat(ImapMessageWriterUtils.terminateLinesWithCRLF(withR)).isEqualTo(correct);
    assertThat(ImapMessageWriterUtils.terminateLinesWithCRLF(withN)).isNotEqualTo(withN);
    assertThat(ImapMessageWriterUtils.terminateLinesWithCRLF(withR)).isNotEqualTo(withR);
    assertThat(ImapMessageWriterUtils.terminateLinesWithCRLF(withN)).isNotEqualTo(incorrect);
    assertThat(ImapMessageWriterUtils.terminateLinesWithCRLF(withR)).isNotEqualTo(incorrect);
  }

  @Test
  public void replacesLineWithCLRFComplex() throws Exception {
    String withBoth = "Th\r\nis is \n\n a te\rst \n";
    String withReverse = "Th\r\nis is \n\r a te\nst \r";
    String correct = "Th\r\nis is \r\n\r\n a te\r\nst \r\n";
    String incorrect = "This is a test";

    assertThat(ImapMessageWriterUtils.terminateLinesWithCRLF(withBoth)).isEqualTo(correct);
    assertThat(ImapMessageWriterUtils.terminateLinesWithCRLF(withReverse)).isEqualTo(correct);
    assertThat(ImapMessageWriterUtils.terminateLinesWithCRLF(withBoth)).isNotEqualTo(withBoth);
    assertThat(ImapMessageWriterUtils.terminateLinesWithCRLF(withReverse)).isNotEqualTo(withReverse);
    assertThat(ImapMessageWriterUtils.terminateLinesWithCRLF(withBoth)).isNotEqualTo(incorrect);
    assertThat(ImapMessageWriterUtils.terminateLinesWithCRLF(withReverse)).isNotEqualTo(incorrect);
  }
}
