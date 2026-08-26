package com.canni.runpod.di

import com.canni.runpod.data.api.RunPodApi
import com.canni.runpod.data.api.RunPodGraphQLApi
import com.canni.runpod.data.auth.ApiKeyStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

data class RunPodConfig(
    val baseUrl: String,
)

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideConfig(): RunPodConfig = RunPodConfig("https://api.runpod.io/v2/")

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(keyStore: ApiKeyStore): OkHttpClient =
        baseBuilder(keyStore)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    @Named("sse")
    fun provideSseClient(keyStore: ApiKeyStore): OkHttpClient =
        baseBuilder(keyStore)
            .readTimeout(0, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json, config: RunPodConfig): Retrofit =
        Retrofit.Builder()
            .baseUrl(config.baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideApi(retrofit: Retrofit): RunPodApi =
        retrofit.create(RunPodApi::class.java)

    @Provides
    @Singleton
    @Named("graphql")
    fun provideGraphQLRetrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.runpod.io/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideGraphQLApi(@Named("graphql") graphQLRetrofit: Retrofit): RunPodGraphQLApi =
        graphQLRetrofit.create(RunPodGraphQLApi::class.java)

    private fun baseBuilder(keyStore: ApiKeyStore): OkHttpClient.Builder =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder().removeHeader("Authorization").build())
            }
            .addInterceptor(AppLoggingInterceptor())
            .addInterceptor { chain ->
                val key = keyStore.apiKey
                val request = if (key.isEmpty()) {
                    chain.request()
                } else {
                    chain.request().newBuilder()
                        .header("Authorization", "Bearer $key")
                        .build()
                }
                chain.proceed(request)
            }
}
