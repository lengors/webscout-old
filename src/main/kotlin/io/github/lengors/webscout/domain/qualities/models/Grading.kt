package io.github.lengors.webscout.domain.qualities.models

enum class Grading {
    A,
    B,
    C,
    D,
    E,
    ;

    companion object {
        fun valueOrNull(name: String): Grading? =
            try {
                Grading.valueOf(name)
            } catch (_: IllegalArgumentException) {
                null
            }
    }
}
