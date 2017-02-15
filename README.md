# fixed-length-parser

Parses a fixed-length string to an object. The mapping is defined by annotation.


## Usage

Define the mapping by annotating class members with `@Field`.  
In `@Field`, specify start position and length.  
You can also specify deserializer. (optional)  

```kotlin
data class Sample(@Field(position = 1, length = 2) val value1: BigDecimal,
                  @Field(position = 3, length = 4, deserializer = "test") val value2: String) {
    @Field(position = 7, length = 2) var property1: BigDecimal? = null
}
```

Generate a parser with the target class as an argument.  
If necessary, we also use the deserializer map as an argument.

```kotlin
val convert: (String) -> String = { it + "convert" }
val parser = FixedLengthParser(kClass = Sample::class, deserializerMap = mapOf("test" to convert))
val sample = parser.parse("1234567890")
  
assertThat(sample.value1).isEqualTo("12")
assertThat(sample.value2).isEqualTo("3456convert")
assertThat(sample.property1).isEqualTo("78")
```