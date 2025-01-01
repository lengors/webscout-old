package io.github.lengors.webscout.integrations.duckling.client

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.lengors.webscout.domain.mappers.asMultiValueMap
import io.github.lengors.webscout.integrations.duckling.models.DucklingDimension
import io.github.lengors.webscout.integrations.duckling.models.DucklingRequest
import io.github.lengors.webscout.integrations.duckling.models.DucklingResponse
import io.github.lengors.webscout.integrations.duckling.properties.DucklingClientProperties
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import kotlin.reflect.cast

@Component
class DucklingClient(
    webClientBuilder: WebClient.Builder,
    restTemplateBuilder: RestTemplateBuilder,
    ducklingClientProperties: DucklingClientProperties,
    private val objectMapper: ObjectMapper,
) {
    companion object {
        const val PARSE_ENDPOINT = "/parse"
    }

    private val restTemplate: RestTemplate by lazy {
        restTemplateBuilder
            .requestFactory(JdkClientHttpRequestFactory::class.java)
            .rootUri(ducklingClientProperties.url)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build()
    }

    private val webClient: WebClient by lazy {
        webClientBuilder
            .baseUrl(ducklingClientProperties.url)
            .build()
    }

    fun <T : DucklingDimension, U : DucklingResponse<T, *>, V : DucklingRequest<T, U>> parse(request: V): U? =
        restTemplate
            .postForObject(
                PARSE_ENDPOINT,
                objectMapper.asMultiValueMap(request),
                request.responseType.java.arrayType(),
            )?.let(Array::class::cast)
            ?.firstOrNull()
            ?.let(request.responseType::cast)

    suspend fun <T : DucklingDimension, U : DucklingResponse<T, *>, V : DucklingRequest<T, U>> parseAsync(request: V): U? =
        webClient
            .post()
            .uri(PARSE_ENDPOINT)
            .accept(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(objectMapper.asMultiValueMap(request)))
            .retrieve()
            .bodyToFlux(request.responseType.java)
            .awaitFirstOrNull()
}
