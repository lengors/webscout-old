package io.github.lengors.webscout.domain.validators

fun interface Validator {
    fun isValid(input: String): Boolean
}
