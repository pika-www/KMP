package com.example.androidios.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry

fun createDefaultComponentContext(): ComponentContext = DefaultComponentContext(
    lifecycle = LifecycleRegistry()
)
