package io.github.lengors.webscout.domain.qualities.models

enum class NoiseLevel {
    A,
    B,
    C,
    ;

    companion object {
        fun valueOrNull(name: String): NoiseLevel? =
            try {
                NoiseLevel.valueOf(name)
            } catch (_: IllegalArgumentException) {
                null
            }
    }
}
