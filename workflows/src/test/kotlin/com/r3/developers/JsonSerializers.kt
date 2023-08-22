package com.r3.developers

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import net.corda.v5.base.types.MemberX500Name

object MemberX500NameSerializer : JsonSerializer<MemberX500Name>() {
    override fun serialize(value: MemberX500Name, generator: JsonGenerator, provider: SerializerProvider?) {
        generator.writeString(value.toString())
    }
}

object MemberX500NameDeserializer : JsonDeserializer<MemberX500Name>() {
    override fun deserialize(parser: JsonParser, ctxt: DeserializationContext?): MemberX500Name {
        return MemberX500Name.parse(parser.text)
    }
}
