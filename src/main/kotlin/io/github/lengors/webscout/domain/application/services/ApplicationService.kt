package io.github.lengors.webscout.domain.application.services

import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Service
import kotlin.reflect.KClass

@Service
object ApplicationService : ApplicationContextAware {
    private lateinit var applicationContext: ApplicationContext

    fun <T : Any> getBean(type: KClass<T>): T = applicationContext.getBean(type.java)

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        this.applicationContext = applicationContext
    }
}

inline fun <reified T : Any> ApplicationService.getBean(): T = getBean(T::class)
