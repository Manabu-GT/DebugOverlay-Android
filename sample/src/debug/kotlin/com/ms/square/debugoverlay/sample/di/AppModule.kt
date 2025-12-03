package com.ms.square.debugoverlay.sample.di

import com.ms.square.debugoverlay.extension.okhttp.DebugOverlayNetworkInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

  @Singleton
  @Provides
  fun debugOverlayNetworkInterceptor(): DebugOverlayNetworkInterceptor = DebugOverlayNetworkInterceptor()

  @Singleton
  @Provides
  fun okhttpClient(debugOverlayNetworkInterceptor: DebugOverlayNetworkInterceptor): OkHttpClient =
    OkHttpClient.Builder().addNetworkInterceptor(debugOverlayNetworkInterceptor).build()
}
