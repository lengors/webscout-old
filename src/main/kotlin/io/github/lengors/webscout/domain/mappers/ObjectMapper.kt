package io.github.lengors.webscout.domain.mappers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import io.github.lengors.webscout.domain.collections.asMultiValueMap
import org.springframework.util.MultiValueMap

fun ObjectMapper.asMap(from: Any?): Map<String, String> = convertValue<Map<String, String>>(from)

fun ObjectMapper.asMultiValueMap(from: Any?): MultiValueMap<String, String> = asMap(from).asMultiValueMap()
