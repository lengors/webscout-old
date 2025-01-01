package io.github.lengors.webscout.domain.collections

import org.springframework.util.CollectionUtils
import org.springframework.util.MultiValueMap

fun <K, V> Map<K, V>.asMultiValueMap(): MultiValueMap<K, V> = CollectionUtils.toMultiValueMap(mapEachValue(::listOf))

inline fun <K, V, R> Map<out K, V>.mapEachKey(transform: (K) -> R): Map<R, V> = mapKeys { transform(it.key) }

inline fun <K, V, R> Map<out K, V>.mapEachValue(transform: (V) -> R): Map<K, R> = mapValues { transform(it.value) }
