package com.cephalon.lucyApp.navigation

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value

interface RootComponent {
    val stack: Value<ChildStack<*, Child>>

    sealed class Child {
        class Login(val component: LoginComponent) : Child()
        class Home(val component: HomeComponent) : Child()
        class SdkTest(val component: SdkTestComponent) : Child()
        class BrainBoxGuide(val component: BrainBoxGuideComponent) : Child()
        class WsTest(val component: WsTestComponent) : Child()
        class AgentModel(val component: AgentModelComponent) : Child()
        class ScanBindChannel(val component: ScanBindChannelComponent) : Child()
        class Nas(val component: NasComponent) : Child()
    }
}
