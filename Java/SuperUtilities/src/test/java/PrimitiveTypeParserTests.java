import com.nuix.superutilities.misc.PrimitiveTypeParser;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PrimitiveTypeParserTests extends TestFoundation {
    @Test
    public void testTypeParsing() throws Exception {
        PrimitiveTypeParser standardTypeParser = PrimitiveTypeParser.getStandard();

        // Since we are testing some without millis precision, we trim millis off so
        // round trip still succeeds
        DateTime now = DateTime.now(DateTimeZone.UTC).millisOfSecond().setCopy(0);
        List<String> formatStrings = List.of(
                "yyyy-MM-dd'T'HH:mm:ss.SSSZZ",
                "yyyy-MM-dd'T'HH:mm:ss'Z'"
        );

        for (String formatString : formatStrings) {
            log.info(formatString + " => " + now.toString(formatString));
        }

        for (String formatString : formatStrings) {
            String inputString = now.toString(formatString);
            Object parsedObject = standardTypeParser.parse(inputString);
            assertNotNull(parsedObject);
            assertInstanceOf(DateTime.class, parsedObject);
            assertTrue(now.isEqual((DateTime) parsedObject),
                    String.format("Format String: %s, Expected: %s, Got: %s",
                            formatString, now, ((DateTime) parsedObject)));
        }

        List<String> additionalDates = List.of(
                "2022-04-20T17:12:37Z"
        );

        for (String dateToParse : additionalDates) {
            Object parsedDate = standardTypeParser.parse(dateToParse);
            assertTrue(parsedDate instanceof DateTime, String.format("%s did not parse into a DateTime", dateToParse));
        }

        assertEquals("cat", standardTypeParser.parse("cat"));
        assertEquals("  cat  ", standardTypeParser.parse("  cat  "));
        assertEquals("dog", standardTypeParser.parseWithFallback("", "dog"));
        assertEquals(0, standardTypeParser.parseWithFallback("bird", 0));

        assertEquals(true, standardTypeParser.parse("true"));
        assertEquals(false, standardTypeParser.parse("false"));
        assertEquals(true, standardTypeParser.parse("True"));
        assertEquals(false, standardTypeParser.parse("False"));
        assertEquals(true, standardTypeParser.parse("  True  "));
        assertEquals(false, standardTypeParser.parse("  False  "));

        assertEquals(0L, standardTypeParser.parse("0"));
        assertEquals(0L, standardTypeParser.parse("-0"));
        assertEquals(0L, standardTypeParser.parse("+0"));
        assertEquals(123456789L, standardTypeParser.parse("123456789"));
        assertEquals(123456789L, standardTypeParser.parse("0123456789"));
        assertEquals(123456789L, standardTypeParser.parse("+0123456789"));
        assertEquals(-123456789L, standardTypeParser.parse("-0123456789"));

        assertEquals(0L, standardTypeParser.parse("  0  "));
        assertEquals(0L, standardTypeParser.parse("  -0  "));
        assertEquals(0L, standardTypeParser.parse("  +0  "));
        assertEquals(123456789L, standardTypeParser.parse("  123456789  "));
        assertEquals(123456789L, standardTypeParser.parse("  0123456789  "));
        assertEquals(123456789L, standardTypeParser.parse("  +0123456789  "));
        assertEquals(-123456789L, standardTypeParser.parse("  -0123456789  "));

        assertEquals(0.0d, standardTypeParser.parse("0."));
        assertEquals(-0.0d, standardTypeParser.parse("-0."));
        assertEquals(0.0d, standardTypeParser.parse("+0."));
        assertEquals(0.0d, standardTypeParser.parse("0.0"));
        assertEquals(0.0d, standardTypeParser.parse("+0.0"));
        assertEquals(-0.0d, standardTypeParser.parse("-0.0"));
        assertEquals(123456789d, standardTypeParser.parse("123456789."));
        assertEquals(123456789d, standardTypeParser.parse("+123456789."));
        assertEquals(-123456789d, standardTypeParser.parse("-123456789."));
        assertEquals(123456789d, standardTypeParser.parse("123456789.0"));
        assertEquals(123456789d, standardTypeParser.parse("+123456789.0"));
        assertEquals(-123456789d, standardTypeParser.parse("-123456789.0"));
        assertEquals(123456789d, standardTypeParser.parse("123456789.000"));
        assertEquals(123456789d, standardTypeParser.parse("+123456789.000"));
        assertEquals(-123456789d, standardTypeParser.parse("-123456789.000"));
        assertEquals(123456789.314d, standardTypeParser.parse("123456789.314"));
        assertEquals(123456789.314d, standardTypeParser.parse("+123456789.314"));
        assertEquals(-123456789.314d, standardTypeParser.parse("-123456789.314"));

        assertEquals(0.0d, standardTypeParser.parse("  0.  "));
        assertEquals(-0.0d, standardTypeParser.parse("  -0.  "));
        assertEquals(0.0d, standardTypeParser.parse("  +0.  "));
        assertEquals(0.0d, standardTypeParser.parse("  0.0  "));
        assertEquals(0.0d, standardTypeParser.parse("  +0.0  "));
        assertEquals(-0.0d, standardTypeParser.parse("  -0.0  "));
        assertEquals(123456789d, standardTypeParser.parse("  123456789.  "));
        assertEquals(123456789d, standardTypeParser.parse("  +123456789.  "));
        assertEquals(-123456789d, standardTypeParser.parse("  -123456789.  "));
        assertEquals(123456789d, standardTypeParser.parse("  123456789.0  "));
        assertEquals(123456789d, standardTypeParser.parse("  +123456789.0  "));
        assertEquals(-123456789d, standardTypeParser.parse("  -123456789.0  "));
        assertEquals(123456789d, standardTypeParser.parse("  123456789.000  "));
        assertEquals(123456789d, standardTypeParser.parse("  +123456789.000  "));
        assertEquals(-123456789d, standardTypeParser.parse("  -123456789.000  "));
        assertEquals(123456789.314d, standardTypeParser.parse("  123456789.314  "));
        assertEquals(123456789.314d, standardTypeParser.parse("  +123456789.314  "));
        assertEquals(-123456789.314d, standardTypeParser.parse("  -123456789.314  "));
    }
}
