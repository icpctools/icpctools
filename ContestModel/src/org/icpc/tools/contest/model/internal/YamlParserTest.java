package org.icpc.tools.contest.model.internal;

import java.io.Reader;
import java.io.StringReader;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public class YamlParserTest {
	 @Test
	 public void testParseInfo() throws Exception {
		  String exampleYaml = "name:                     Example contest\n"
				  + "short-name:               example19\n"
				  + "start-time:               2019-11-17T10:00:00+01:00\n"
				  + "duration:                 7:00:00\n"
				  + "scoreboard-freeze-length: 1:23:00\n"
				  + "penalty-time:             17\n";
		  Reader reader = new StringReader(exampleYaml);
		  Info info = YamlParser.parseInfo(reader);
		  assertThat(info.getFormalName()).isEqualTo("Example contest");
		  assertThat(info.getName()).isEqualTo("example19");
		  assertThat(info.getStartTime()).isEqualTo(1573981200000L);
		  assertThat(info.getDuration()).isEqualTo(7*3600*1000);
		  assertThat(info.getFreezeDuration()).isEqualTo((60+23) * 60 * 1000);
		  assertThat(info.getPenaltyTime()).isEqualTo(17);
	 }
}