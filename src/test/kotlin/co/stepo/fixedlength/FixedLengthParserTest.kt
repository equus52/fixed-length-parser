package co.stepo.fixedlength

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.math.BigDecimal

class FixedLengthParserTest {

    @Test
    fun test_parse() {
        val parser = FixedLengthParser(kClass = Sample::class, deserializerMap = mapOf("test" to convert))
        val sample = parser.parse("1234567890")

        assertThat(sample.value1).isEqualTo("12")
        assertThat(sample.value2).isEqualTo("3456convert")
        assertThat(sample.property1).isEqualTo("78")
    }

    data class Sample(@Field(position = 1, length = 2) val value1: BigDecimal,
                      @Field(position = 3, length = 4, deserializer = "test") val value2: String) {
        @Field(position = 7, length = 2) var property1: BigDecimal? = null
    }

    val convert: (String) -> String = { it + "convert" }
}

