package org.icpc.tools.contest.model.internal;

import java.io.Reader;
import java.io.StringReader;
import java.text.ParseException;

import org.icpc.tools.contest.model.feed.RelativeTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

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
		Info info = YamlParser.parseInfo(reader, true);
		assertThat(info.getFormalName()).isEqualTo("Example contest");
		assertThat(info.getName()).isEqualTo("example19");
		assertThat(info.getStartTime()).isEqualTo(1573981200000L);
		assertThat(info.getDuration()).isEqualTo(7 * 3600 * 1000);
		assertThat(info.getFreezeDuration()).isEqualTo((60 + 23) * 60 * 1000);
		assertThat(info.getPenaltyTime()).isEqualTo(17);
	}

	@Test
	public void testParseInfoWithSeconds() throws Exception {
		String exampleYaml = "name:                     Example contest\n"
				+ "short-name:               example19\n"
				+ "start-time:               2019-11-17T10:00:11+03:30\n"
				+ "duration:                 7:00:22\n"
				+ "scoreboard-freeze-length: 1:23:33\n"
				+ "penalty-time:             17\n";
		Reader reader = new StringReader(exampleYaml);
		Info info = YamlParser.parseInfo(reader, false);
		assertThat(info.getStartTime()).isEqualTo(1573972211000L); // 1573974011000L);
		assertThat(info.getDuration()).isEqualTo((7 * 3600 + 22) * 1000);
		assertThat(info.getFreezeDuration()).isEqualTo(((60 + 23) * 60 + 33) * 1000);
	}

	@Test
	public void testParseInfoWithoutPenaltyTime() throws Exception {
		String exampleYaml = "name:                     Example contest\n"
				+ "short-name:               example19\n"
				+ "start-time:               2019-11-17T10:00:11+03:30\n"
				+ "duration:                 7:00:22\n"
				+ "scoreboard-freeze-length: 1:23:33\n";
		Reader reader = new StringReader(exampleYaml);
		Info info = YamlParser.parseInfo(reader, false);
		boolean npe = false;
		try {
			assertThat(info.getPenaltyTime()).isEqualTo(20);
		} catch (NullPointerException e) {
			// TODO: use assertThrows, @since JUnit 4.13
			npe = true;
		}
		assertThat(npe).isTrue();
	}

	@Test
	public void testParseTime() throws Exception {
		assertThat(parseTime("5:00:00")).isEqualTo(5 * 3_600_000);
		assertThat(parseTime("05:00:00")).isEqualTo(5 * 3_600_000);
		assertThat(parseTime("2:30:00")).isEqualTo(2 * 3_600_000 + 30 * 60_000);
		assertThat(parseTime("1:23:42")).isEqualTo(1 * 3_600_000 + 23 * 60_000 + 42_000);
	}

	@Test(expected = Exception.class)
	public void testParseTimeWithoutSeconds() throws Exception {
		parseTime("5:00");
	}

	private int parseTime(String s) throws ParseException {
		return RelativeTime.parse(s);
	}
}
