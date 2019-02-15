package com.worldturner.medeia.schema.validation

import com.worldturner.medeia.api.FailedValidationResult
import com.worldturner.medeia.api.OkValidationResult
import com.worldturner.medeia.api.ValidationResult
import com.worldturner.medeia.parser.JsonTokenData
import com.worldturner.medeia.parser.JsonTokenLocation
import com.worldturner.medeia.parser.JsonTokenType.VALUE_TEXT
import com.worldturner.medeia.pointer.JsonPointer
import com.worldturner.medeia.schema.validation.stream.SchemaValidatorInstance
import java.net.URI
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.format.DateTimeParseException
import java.util.regex.PatternSyntaxException

val formatRegexes = emptyMap<String, Regex>()

class FormatValidator(
    val format: String
) : SchemaValidator, SchemaValidatorInstance {
    override fun createInstance(startLevel: Int): SchemaValidatorInstance = this

    override fun recordUnknownRefs(unknownRefs: MutableCollection<URI>) = Unit

    override fun validate(token: JsonTokenData, location: JsonTokenLocation): ValidationResult? {
        if (token.type != VALUE_TEXT) {
            return OkValidationResult
        }
        val string = token.text!!
        return if (format in formatRegexes) {
            if (formatRegexes[format]?.matchEntire(string) != null)
                OkValidationResult
            else
                failedValidation(string, location)
        } else when (format) {
            "regex" -> try {
                Regex(string).let { OkValidationResult }
            } catch (e: PatternSyntaxException) {
                failedValidation(string, location, e)
            }
            "uri-reference", "uri", "iri-reference", "iri" ->
                try {
                    URI.create(string).let { OkValidationResult }
                } catch (e: IllegalArgumentException) {
                    failedValidation(string, location, e)
                }
            "json-pointer" ->
                try {
                    JsonPointer(string).let { OkValidationResult }
                } catch (e: IllegalArgumentException) {
                    failedValidation(string, location, e)
                }
            "date" ->
                try {
                    LocalDate.parse(string).let { OkValidationResult }
                } catch (e: DateTimeParseException) {
                    failedValidation(string, location, e)
                }
            "time" ->
                try {
                    OffsetTime.parse(string).let { OkValidationResult }
                } catch (e: DateTimeParseException) {
                    failedValidation(string, location, e)
                }
            "date-time" ->
                try {
                    OffsetDateTime.parse(string).let { OkValidationResult }
                } catch (e: DateTimeParseException) {
                    failedValidation(string, location, e)
                }
            "ipv4" ->
                if (string.isIpv4())
                    OkValidationResult
                else
                    failedValidation(string, location)
            "ipv6" ->
                if (string.isIpv6())
                    OkValidationResult
                else
                    failedValidation(string, location)
            "hostname" ->
                if (string.isHostname())
                    OkValidationResult
                else
                    failedValidation(string, location)
            else -> OkValidationResult
        }
    }

    fun failedValidation(string: String, location: JsonTokenLocation) =
        FailedValidationResult(
            location = location,
            rule = "format",
            message = "Invalid $format '$string'"
        )

    fun failedValidation(string: String, location: JsonTokenLocation, e: Exception) =
        FailedValidationResult(
            location = location,
            rule = "format",
            message = "Invalid $format '$string': ${e.message}"
        )

    companion object {
        fun create(format: String?): FormatValidator? =
            format?.let { FormatValidator(format) }
    }
}

// RFC1034 3.1 & 3.5
private fun String.isHostname(): Boolean {
    if (this.length > 255) return false
    val labels = this.split('.')
    if (!labels.all { it.isHostnameLabel() }) return false
    // Labels must have at least one character except for the last label
    // but if there is only one label it has to be non-empty
    val notEmptyUpTo = if (labels.size == 1) 1 else labels.size - 1
    for (i in 0 until notEmptyUpTo)
        if (labels[i].isEmpty()) return false
    return true
}

// RFC1034 3.1 & 3.5
private fun String.isHostnameLabel(): Boolean {
    val length = this.length
    if (length > 63) return false
    for (i in 0 until length) {
        if (i == 0) {
            when (this[i]) {
                in 'a'..'z', in 'A'..'Z' -> {
                }
                else -> return false
            }
        } else if (i == length - 1) {
            when (this[i]) {
                in '0'..'9', in 'a'..'z', in 'A'..'Z' -> {
                }
                else -> return false
            }
        } else {
            when (this[i]) {
                in '0'..'9', in 'a'..'z', in 'A'..'Z', '-' -> {
                }
                else -> return false
            }
        }
    }
    return true
}

private fun String.isIpv4(): Boolean {
    val decbytes = this.split('.')
    if (decbytes.size != 4) return false
    if (!decbytes.all { it.isDecbyte() }) return false
    if (!decbytes.map { it.toInt() }.all { it in 0..255 }) return false
    return true
}

private fun String.isIpv6(): Boolean {
    val xs = this.split(':')
    if (xs.size > 8) return false
    if (!xs.all { it.isIpv6Hex() }) return false
    if (!xs.map { if (it.isEmpty()) 0 else it.toInt(16) }.all { it in 0..65535 }) return false
    return true
}

private fun String.isDecbyte(): Boolean {
    val l = this.length
    if (l == 0) return false
    for (i in 0 until l) {
        if (this[i] !in '0'..'9') return false
    }
    return true
}

private fun String.isIpv6Hex(): Boolean {
    val l = this.length
    for (i in 0 until l) {
        when (this[i]) {
            in '0'..'9', in 'a'..'f', in 'A'..'F' -> {
            }
            else -> return false
        }
    }
    return true
}