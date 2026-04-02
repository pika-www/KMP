package com.example.androidios.navigation

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value

interface RootComponent {
    val stack: Value<ChildStack<*, Child>>

    sealed class Child {
        class Login(val component: LoginComponent) : Child()
        class ForgotPassword(val component: ForgotPasswordComponent) : Child()
        class Home(val component: HomeComponent) : Child()
        class BrainBoxGuide(val component: BrainBoxGuideComponent) : Child()
        class WsTest(val component: WsTestComponent) : Child()
        class LocalDeployTest(val component: LocalDeployTestComponent) : Child()
    }
}
