package com.example.androidios.navigation

import com.example.androidios.api.AuthRepository
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import kotlinx.serialization.Serializable

class RootComponentImpl(
    componentContext: ComponentContext,
    private val authRepository: AuthRepository
) : RootComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<Config>()

    override val stack: Value<ChildStack<*, RootComponent.Child>> = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = if (authRepository.hasValidToken()) Config.Home else Config.Login,
        handleBackButton = true,
        childFactory = ::createChild
    )

    private fun createChild(
        config: Config,
        childContext: ComponentContext
    ): RootComponent.Child {
        return when (config) {
            Config.Login -> RootComponent.Child.Login(
                component = object : LoginComponent {
                    override fun onLoginSuccess() {
                        navigation.replaceAll(Config.Home)
                    }

                    override fun onForgotPassword() {
                        navigation.push(Config.ForgotPassword)
                    }
                }
            )

            Config.ForgotPassword -> RootComponent.Child.ForgotPassword(
                component = object : ForgotPasswordComponent {
                    override fun onBack() {
                        navigation.pop()
                    }
                }
            )

            Config.Home -> RootComponent.Child.Home(
                component = object : HomeComponent {
                    override fun onLogout() {
                        authRepository.logout()
                        navigation.replaceAll(Config.Login)
                    }

                    override fun onOpenWsTest() {
                        navigation.push(Config.WsTest)
                    }

                    override fun onOpenBrainBoxGuide() {
                        navigation.push(Config.BrainBoxGuide)
                    }
                }
            )

            Config.BrainBoxGuide -> RootComponent.Child.BrainBoxGuide(
                component = object : BrainBoxGuideComponent {
                    override fun onBack() {
                        navigation.pop()
                    }

                    override fun onFinish() {
                        navigation.pop()
                    }
                }
            )

            Config.WsTest -> RootComponent.Child.WsTest(
                component = object : WsTestComponent {
                    override fun onBack() {
                        navigation.pop()
                    }
                }
            )
        }
    }

    @Serializable
    private sealed interface Config {
        @Serializable
        data object Login : Config

        @Serializable
        data object ForgotPassword : Config

        @Serializable
        data object Home : Config

        @Serializable
        data object BrainBoxGuide : Config

        @Serializable
        data object WsTest : Config
    }
}
