package com.cephalon.lucyApp.screens.agentmodel

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// ── 返回 (左箭头 11×17) ──
internal val BackIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Back", defaultWidth = 11.dp, defaultHeight = 17.dp,
        viewportWidth = 11f, viewportHeight = 17f
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.NonZero
        ) {
            // 左指 chevron：与 ChevronRightIcon 水平镜像，缩放至 11×17 视口
            moveTo(8.5f, 1.0f)
            curveTo(8.8f, 1.3f, 8.8f, 1.77f, 8.5f, 2.06f)
            lineTo(3.06f, 7.5f)
            curveTo(2.77f, 7.79f, 2.77f, 8.21f, 3.06f, 8.5f)
            lineTo(8.5f, 14.94f)
            curveTo(8.79f, 15.23f, 8.79f, 15.7f, 8.5f, 16.0f)
            curveTo(8.21f, 16.29f, 7.73f, 16.29f, 7.44f, 16.0f)
            lineTo(2.0f, 9.56f)
            curveTo(1.02f, 8.59f, 1.02f, 7.41f, 2.0f, 6.44f)
            lineTo(7.44f, 1.0f)
            curveTo(7.73f, 0.71f, 8.21f, 0.71f, 8.5f, 1.0f)
            close()
        }
    }.build()
}

// ── 账号 (人形) ──
internal val AccountIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Account", defaultWidth = 15.dp, defaultHeight = 16.dp,
        viewportWidth = 15f, viewportHeight = 16f
    ).apply {
        path(
            fill = SolidColor(Color.Black.copy(alpha = 0.4f)),
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(4.6354f, 4.1451f)
            curveTo(4.636f, 3.4044f, 4.9305f, 2.6942f, 5.4541f, 2.1704f)
            curveTo(5.9778f, 1.6465f, 6.6879f, 1.3518f, 7.4286f, 1.3509f)
            curveTo(8.1693f, 1.3518f, 8.8794f, 1.6465f, 9.403f, 2.1704f)
            curveTo(9.9267f, 2.6942f, 10.2211f, 3.4044f, 10.2217f, 4.1451f)
            curveTo(10.2205f, 4.8855f, 9.9258f, 5.5951f, 9.4022f, 6.1185f)
            curveTo(8.8786f, 6.6418f, 8.1689f, 6.9362f, 7.4286f, 6.9371f)
            curveTo(6.6881f, 6.9362f, 5.9781f, 6.6417f, 5.4545f, 6.1181f)
            curveTo(4.9309f, 5.5944f, 4.6363f, 4.8845f, 4.6354f, 4.144f)
            moveTo(9.984f, 7.4f)
            curveTo(10.4787f, 7.0133f, 10.8789f, 6.5191f, 11.1541f, 5.9547f)
            curveTo(11.4294f, 5.3904f, 11.5725f, 4.7708f, 11.5726f, 4.1429f)
            curveTo(11.5737f, 1.8606f, 9.7143f, 0f, 7.4286f, 0f)
            curveTo(5.1429f, 0f, 3.2834f, 1.8594f, 3.2834f, 4.1451f)
            curveTo(3.2834f, 5.4663f, 3.9074f, 6.6411f, 4.872f, 7.4011f)
            curveTo(2.0343f, 8.4457f, 0f, 11.1703f, 0f, 14.3657f)
            curveTo(0f, 14.5448f, 0.0712f, 14.7166f, 0.1979f, 14.8432f)
            curveTo(0.3245f, 14.9699f, 0.4963f, 15.041f, 0.6754f, 15.041f)
            curveTo(0.8545f, 15.041f, 1.0263f, 14.9699f, 1.153f, 14.8432f)
            curveTo(1.2797f, 14.7166f, 1.3508f, 14.5448f, 1.3509f, 14.3657f)
            curveTo(1.353f, 12.7546f, 1.994f, 11.21f, 3.1334f, 10.0709f)
            curveTo(4.2728f, 8.9317f, 5.8174f, 8.291f, 7.4286f, 8.2891f)
            curveTo(9.0399f, 8.291f, 10.5848f, 8.9319f, 11.7242f, 10.0713f)
            curveTo(12.8636f, 11.2107f, 13.5045f, 12.7555f, 13.5063f, 14.3669f)
            curveTo(13.5063f, 14.4556f, 13.5237f, 14.5434f, 13.5577f, 14.6254f)
            curveTo(13.5916f, 14.7073f, 13.6414f, 14.7818f, 13.7041f, 14.8445f)
            curveTo(13.7668f, 14.9073f, 13.8413f, 14.957f, 13.9232f, 14.991f)
            curveTo(14.0052f, 15.0249f, 14.093f, 15.0424f, 14.1817f, 15.0424f)
            curveTo(14.2704f, 15.0424f, 14.3583f, 15.0249f, 14.4402f, 14.991f)
            curveTo(14.5222f, 14.957f, 14.5966f, 14.9073f, 14.6594f, 14.8445f)
            curveTo(14.7221f, 14.7818f, 14.7718f, 14.7073f, 14.8058f, 14.6254f)
            curveTo(14.8397f, 14.5434f, 14.8572f, 14.4556f, 14.8571f, 14.3669f)
            curveTo(14.8571f, 11.1703f, 12.8229f, 8.4469f, 9.984f, 7.4011f)
        }
    }.build()
}

// ── 充值账户 (钱包) ──
internal val WalletIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Wallet", defaultWidth = 16.dp, defaultHeight = 16.dp,
        viewportWidth = 16f, viewportHeight = 16f
    ).apply {
        path(
            fill = SolidColor(Color.Black.copy(alpha = 0.4f)),
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(13.536f, 1.3359f)
            horizontalLineTo(2.464f)
            curveTo(1.4f, 1.3359f, 0.536f, 2.1999f, 0.536f, 3.2639f)
            verticalLineTo(12.7679f)
            curveTo(0.536f, 13.8319f, 1.4f, 14.6959f, 2.464f, 14.6959f)
            horizontalLineTo(13.536f)
            curveTo(14.6f, 14.6959f, 15.464f, 13.8319f, 15.464f, 12.7679f)
            verticalLineTo(3.2639f)
            curveTo(15.464f, 2.1999f, 14.6f, 1.3359f, 13.536f, 1.3359f)
            close()
            moveTo(14.4f, 9.5759f)
            horizontalLineTo(10.096f)
            curveTo(9.232f, 9.5759f, 8.528f, 8.8719f, 8.528f, 8.0079f)
            curveTo(8.528f, 7.1439f, 9.232f, 6.4399f, 10.096f, 6.4399f)
            horizontalLineTo(14.4f)
            verticalLineTo(9.5759f)
            close()
            moveTo(14.4f, 12.7679f)
            curveTo(14.4f, 13.2399f, 14.016f, 13.6319f, 13.536f, 13.6319f)
            horizontalLineTo(2.464f)
            curveTo(1.992f, 13.6319f, 1.6f, 13.2479f, 1.6f, 12.7679f)
            verticalLineTo(3.2639f)
            curveTo(1.6f, 2.7839f, 1.984f, 2.3999f, 2.464f, 2.3999f)
            horizontalLineTo(13.536f)
            curveTo(14.008f, 2.3999f, 14.4f, 2.7839f, 14.4f, 3.2639f)
            verticalLineTo(5.3839f)
            horizontalLineTo(10.096f)
            curveTo(8.648f, 5.3839f, 7.464f, 6.5679f, 7.464f, 8.0159f)
            curveTo(7.464f, 9.4639f, 8.648f, 10.6479f, 10.096f, 10.6479f)
            horizontalLineTo(14.4f)
            verticalLineTo(12.7679f)
            close()
            moveTo(10.472f, 8.0159f)
            curveTo(10.472f, 7.6319f, 10.784f, 7.3199f, 11.168f, 7.3199f)
            curveTo(11.552f, 7.3199f, 11.864f, 7.6319f, 11.864f, 8.0159f)
            curveTo(11.864f, 8.3999f, 11.552f, 8.7119f, 11.168f, 8.7119f)
            curveTo(10.784f, 8.7039f, 10.472f, 8.3999f, 10.472f, 8.0159f)
            close()
        }
    }.build()
}

// ── 我的设备 (服务器) ──
internal val DevicesIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Devices", defaultWidth = 16.dp, defaultHeight = 16.dp,
        viewportWidth = 16f, viewportHeight = 16f
    ).apply {
        path(
            fill = SolidColor(Color.Black.copy(alpha = 0.4f)),
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(11.9199f, 8.6892f)
            curveTo(13.8049f, 8.6893f, 15.3389f, 10.1767f, 15.3389f, 12.0056f)
            curveTo(15.3388f, 13.8332f, 13.8049f, 15.32f, 11.9199f, 15.3201f)
            horizontalLineTo(4.0908f)
            curveTo(2.2059f, 15.3201f, 0.672f, 13.8338f, 0.6719f, 12.0056f)
            curveTo(0.6719f, 10.1773f, 2.2058f, 8.6892f, 4.0908f, 8.6892f)
            horizontalLineTo(11.9199f)
            close()
            moveTo(4.0908f, 9.9099f)
            curveTo(2.8776f, 9.9099f, 1.8899f, 10.8498f, 1.8897f, 12.0056f)
            curveTo(1.8897f, 13.1604f, 2.8774f, 14.1003f, 4.0908f, 14.1003f)
            horizontalLineTo(11.9199f)
            curveTo(13.1326f, 14.1003f, 14.12f, 13.1609f, 14.1201f, 12.0056f)
            curveTo(14.1201f, 10.8496f, 13.1327f, 9.909f, 11.9199f, 9.9089f)
            lineTo(4.0908f, 9.9099f)
            close()
            moveTo(4.6416f, 11.0535f)
            curveTo(5.1473f, 11.0537f, 5.5576f, 11.4637f, 5.5576f, 11.9695f)
            curveTo(5.5574f, 12.475f, 5.1472f, 12.8853f, 4.6416f, 12.8855f)
            curveTo(4.1359f, 12.8855f, 3.7258f, 12.4752f, 3.7256f, 11.9695f)
            curveTo(3.7256f, 11.4636f, 4.1357f, 11.0535f, 4.6416f, 11.0535f)
            close()
            moveTo(11.9189f, 0.6794f)
            curveTo(13.8039f, 0.6794f, 15.3378f, 2.1666f, 15.3379f, 3.9949f)
            curveTo(15.3379f, 5.8232f, 13.8043f, 7.3103f, 11.9199f, 7.3103f)
            horizontalLineTo(4.0898f)
            curveTo(2.2055f, 7.3103f, 0.6719f, 5.8225f, 0.6719f, 3.9949f)
            curveTo(0.672f, 2.1673f, 2.2055f, 0.6795f, 4.0898f, 0.6794f)
            horizontalLineTo(11.9189f)
            close()
            moveTo(4.0898f, 1.8992f)
            curveTo(2.8766f, 1.8992f, 1.8897f, 2.8396f, 1.8897f, 3.9949f)
            curveTo(1.8897f, 5.1503f, 2.8765f, 6.0905f, 4.0898f, 6.0906f)
            horizontalLineTo(11.9189f)
            curveTo(13.1317f, 6.0906f, 14.1191f, 5.1503f, 14.1191f, 3.9949f)
            curveTo(14.1191f, 2.8395f, 13.1326f, 1.8992f, 11.9199f, 1.8992f)
            horizontalLineTo(4.0898f)
            close()
            moveTo(11.3584f, 3.115f)
            curveTo(11.8642f, 3.115f, 12.2744f, 3.5252f, 12.2744f, 4.031f)
            curveTo(12.2743f, 4.5368f, 11.8642f, 4.947f, 11.3584f, 4.947f)
            curveTo(10.8526f, 4.947f, 10.4425f, 4.5368f, 10.4424f, 4.031f)
            curveTo(10.4424f, 3.5251f, 10.8525f, 3.115f, 11.3584f, 3.115f)
            close()
        }
    }.build()
}

// ── 添加新设备 (加号圆圈) ──
internal val AddDeviceIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "AddDevice", defaultWidth = 16.dp, defaultHeight = 16.dp,
        viewportWidth = 16f, viewportHeight = 16f
    ).apply {
        path(
            fill = SolidColor(Color.Black.copy(alpha = 0.4f)),
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(8.0004f, 0.3333f)
            curveTo(12.2716f, 0.3334f, 15.6664f, 3.7289f, 15.6664f, 8.0002f)
            curveTo(15.6662f, 12.2714f, 12.2715f, 15.6661f, 8.0004f, 15.6663f)
            curveTo(3.7291f, 15.6663f, 0.3335f, 12.2715f, 0.3334f, 8.0002f)
            curveTo(0.3334f, 3.7288f, 3.7289f, 0.3333f, 8.0004f, 0.3333f)
            close()
            moveTo(8.0004f, 1.429f)
            curveTo(4.3861f, 1.429f, 1.4291f, 4.386f, 1.4291f, 8.0002f)
            curveTo(1.4293f, 11.6144f, 4.3862f, 14.5715f, 8.0004f, 14.5715f)
            curveTo(11.6144f, 14.5714f, 14.5715f, 11.6143f, 14.5717f, 8.0002f)
            curveTo(14.5717f, 4.3861f, 11.6145f, 1.4291f, 8.0004f, 1.429f)
            close()
            moveTo(8.0004f, 3.6194f)
            curveTo(8.3287f, 3.6195f, 8.5471f, 3.838f, 8.5472f, 4.1663f)
            verticalLineTo(7.4524f)
            horizontalLineTo(11.8334f)
            curveTo(12.1619f, 7.4524f, 12.3812f, 7.6717f, 12.3812f, 8.0002f)
            curveTo(12.3811f, 8.3286f, 12.1618f, 8.5471f, 11.8334f, 8.5471f)
            horizontalLineTo(8.5472f)
            verticalLineTo(11.8333f)
            curveTo(8.5472f, 12.1617f, 8.3288f, 12.381f, 8.0004f, 12.3811f)
            curveTo(7.6718f, 12.3811f, 7.4525f, 12.1618f, 7.4525f, 11.8333f)
            verticalLineTo(8.5471f)
            horizontalLineTo(4.1664f)
            curveTo(3.8381f, 8.547f, 3.6197f, 8.3285f, 3.6195f, 8.0002f)
            curveTo(3.6195f, 7.6718f, 3.838f, 7.4526f, 4.1664f, 7.4524f)
            horizontalLineTo(7.4525f)
            verticalLineTo(4.1663f)
            curveTo(7.4527f, 3.8379f, 7.6719f, 3.6194f, 8.0004f, 3.6194f)
            close()
        }
    }.build()
}

// ── 配置 WIFI ──
internal val WifiConfigIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "WifiConfig", defaultWidth = 16.dp, defaultHeight = 16.dp,
        viewportWidth = 16f, viewportHeight = 16f
    ).apply {
        path(
            fill = SolidColor(Color.Black.copy(alpha = 0.4f)),
            pathFillType = PathFillType.EvenOdd
        ) {
            moveTo(7.9996f, 11.682f)
            curveTo(8.4885f, 11.6821f, 8.9165f, 12.1102f, 8.9166f, 12.599f)
            curveTo(8.9166f, 13.0879f, 8.4885f, 13.516f, 7.9996f, 13.516f)
            curveTo(7.5109f, 13.5158f, 7.0836f, 13.0878f, 7.0836f, 12.599f)
            curveTo(7.0837f, 12.1103f, 7.5109f, 11.6823f, 7.9996f, 11.682f)
            close()
            moveTo(4.8834f, 9.9106f)
            curveTo(6.5945f, 8.1995f, 9.4057f, 8.1994f, 11.1168f, 9.9106f)
            curveTo(11.361f, 10.155f, 11.3612f, 10.5216f, 11.1168f, 10.766f)
            curveTo(10.9946f, 10.8882f, 10.8723f, 10.9496f, 10.6891f, 10.9496f)
            curveTo(10.5058f, 10.9496f, 10.3835f, 10.8881f, 10.2613f, 10.766f)
            curveTo(9.0391f, 9.5438f, 7.0222f, 9.5438f, 5.7389f, 10.766f)
            curveTo(5.4945f, 11.0103f, 5.1278f, 11.0103f, 4.8834f, 10.766f)
            curveTo(4.639f, 10.5216f, 4.6391f, 10.155f, 4.8834f, 9.9106f)
            close()
            moveTo(7.9996f, 5.5717f)
            curveTo(9.9552f, 5.5717f, 11.789f, 6.3048f, 13.1334f, 7.7104f)
            curveTo(13.3778f, 7.9548f, 13.3778f, 8.3214f, 13.1334f, 8.5658f)
            curveTo(13.0112f, 8.688f, 12.889f, 8.7494f, 12.7057f, 8.7494f)
            curveTo(12.5223f, 8.7494f, 12.4002f, 8.688f, 12.2779f, 8.5658f)
            curveTo(11.1168f, 7.4047f, 9.5885f, 6.7934f, 7.9996f, 6.7934f)
            curveTo(6.4109f, 6.7935f, 4.8833f, 7.4049f, 3.7223f, 8.5658f)
            curveTo(3.4779f, 8.8103f, 3.1113f, 8.8102f, 2.8668f, 8.5658f)
            curveTo(2.6224f, 8.3214f, 2.6224f, 7.9548f, 2.8668f, 7.7104f)
            curveTo(4.2112f, 6.305f, 6.0442f, 5.5718f, 7.9996f, 5.5717f)
            close()
            moveTo(1.5221f, 4.7768f)
            curveTo(5.5554f, 1.5379f, 11.4222f, 1.7823f, 15.15f, 5.3266f)
            curveTo(15.3944f, 5.571f, 15.3943f, 5.9376f, 15.15f, 6.182f)
            curveTo(15.0278f, 6.3654f, 14.9056f, 6.4271f, 14.7223f, 6.4272f)
            curveTo(14.6001f, 6.4271f, 14.4168f, 6.3658f, 14.2945f, 6.2436f)
            curveTo(10.9946f, 3.1269f, 5.8f, 2.8822f, 2.2555f, 5.7543f)
            curveTo(2.0722f, 5.8765f, 1.889f, 6.0603f, 1.7057f, 6.2436f)
            curveTo(1.4613f, 6.488f, 1.0947f, 6.4878f, 0.8502f, 6.2436f)
            curveTo(0.6058f, 5.9991f, 0.6058f, 5.6325f, 0.8502f, 5.3881f)
            curveTo(1.0946f, 5.1437f, 1.2777f, 4.9601f, 1.5221f, 4.7768f)
            close()
        }
    }.build()
}

// ── 意见反馈 ──
internal val FeedbackIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Feedback", defaultWidth = 16.dp, defaultHeight = 16.dp,
        viewportWidth = 16f, viewportHeight = 16f
    ).apply {
        path(
            fill = SolidColor(Color.Black.copy(alpha = 0.4f)),
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(11.9908f, 14.9955f)
            horizontalLineTo(2.9763f)
            curveTo(2.1473f, 14.9955f, 1.4744f, 14.3225f, 1.4744f, 13.4935f)
            verticalLineTo(3.4804f)
            curveTo(1.4744f, 2.6514f, 2.1473f, 1.9785f, 2.9763f, 1.9785f)
            horizontalLineTo(9.9854f)
            curveTo(10.2627f, 1.9785f, 10.4861f, 2.2019f, 10.4861f, 2.4791f)
            curveTo(10.4861f, 2.7564f, 10.2627f, 2.9798f, 9.9854f, 2.9798f)
            horizontalLineTo(2.9763f)
            curveTo(2.7018f, 2.9798f, 2.4756f, 3.2059f, 2.4756f, 3.4804f)
            verticalLineTo(13.4935f)
            curveTo(2.4756f, 13.7708f, 2.7018f, 13.9942f, 2.9763f, 13.9942f)
            horizontalLineTo(11.988f)
            curveTo(12.2653f, 13.9942f, 12.4887f, 13.7707f, 12.4887f, 13.4935f)
            verticalLineTo(5.4804f)
            curveTo(12.4887f, 5.2032f, 12.7121f, 4.9797f, 12.9893f, 4.9797f)
            curveTo(13.2666f, 4.9797f, 13.49f, 5.2032f, 13.49f, 5.4804f)
            verticalLineTo(13.4908f)
            curveTo(13.4927f, 14.3199f, 12.8171f, 14.9955f, 11.9908f, 14.9955f)
            close()
        }
        path(
            fill = SolidColor(Color.Black.copy(alpha = 0.4f)),
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(9.0488f, 6.9822f)
            curveTo(8.9196f, 6.9822f, 8.7931f, 6.9338f, 8.6935f, 6.8341f)
            curveTo(8.497f, 6.6376f, 8.497f, 6.3228f, 8.6935f, 6.1262f)
            lineTo(13.6704f, 1.152f)
            curveTo(13.8669f, 0.9555f, 14.1819f, 0.9555f, 14.3783f, 1.152f)
            curveTo(14.5748f, 1.3485f, 14.5748f, 1.6634f, 14.3783f, 1.8599f)
            lineTo(9.4014f, 6.8368f)
            curveTo(9.3045f, 6.9338f, 9.1753f, 6.9822f, 9.0488f, 6.9822f)
            close()
        }
        path(
            fill = SolidColor(Color.Black.copy(alpha = 0.4f)),
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(6.9815f, 6.9823f)
            horizontalLineTo(3.9776f)
            curveTo(3.7004f, 6.9823f, 3.4769f, 6.7589f, 3.4769f, 6.4816f)
            curveTo(3.4769f, 6.2044f, 3.7004f, 5.981f, 3.9776f, 5.981f)
            horizontalLineTo(6.9815f)
            curveTo(7.2588f, 5.981f, 7.4822f, 6.2044f, 7.4822f, 6.4816f)
            curveTo(7.4821f, 6.7589f, 7.2588f, 6.9823f, 6.9815f, 6.9823f)
            close()
        }
        path(
            fill = SolidColor(Color.Black.copy(alpha = 0.4f)),
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(9.9881f, 9.9862f)
            horizontalLineTo(3.9776f)
            curveTo(3.7004f, 9.9862f, 3.4769f, 9.7628f, 3.4769f, 9.4855f)
            curveTo(3.4769f, 9.2083f, 3.7004f, 8.9849f, 3.9776f, 8.9849f)
            horizontalLineTo(9.9854f)
            curveTo(10.2627f, 8.9849f, 10.4861f, 9.2083f, 10.4861f, 9.4855f)
            curveTo(10.4861f, 9.7628f, 10.2627f, 9.9862f, 9.9881f, 9.9862f)
            close()
        }
    }.build()
}

// ── 清除缓存 ──
internal val ClearCacheIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "ClearCache", defaultWidth = 16.dp, defaultHeight = 16.dp,
        viewportWidth = 16f, viewportHeight = 16f
    ).apply {
        path(
            fill = SolidColor(Color.Black.copy(alpha = 0.4f)),
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(8f, 0.5452f)
            curveTo(9.1202f, 0.5452f, 10.0303f, 1.4452f, 10.0484f, 2.5623f)
            lineTo(10.0487f, 2.5962f)
            verticalLineTo(5.1861f)
            horizontalLineTo(13.5556f)
            curveTo(14.1631f, 5.1861f, 14.6567f, 5.6742f, 14.6666f, 6.2801f)
            lineTo(14.6667f, 6.2985f)
            verticalLineTo(7.55f)
            curveTo(14.6667f, 8.1582f, 14.1791f, 8.6524f, 13.574f, 8.6622f)
            lineTo(13.5556f, 8.6624f)
            horizontalLineTo(13.1975f)
            lineTo(14.0375f, 13.9812f)
            curveTo(14.1324f, 14.582f, 13.7279f, 15.1464f, 13.1317f, 15.2507f)
            lineTo(13.1135f, 15.2537f)
            curveTo(13.0619f, 15.2619f, 13.0097f, 15.2664f, 12.9575f, 15.2673f)
            lineTo(12.94f, 15.2674f)
            horizontalLineTo(3.0601f)
            curveTo(2.4464f, 15.2674f, 1.949f, 14.7693f, 1.949f, 14.155f)
            curveTo(1.949f, 14.1026f, 1.9526f, 14.0503f, 1.96f, 13.9985f)
            lineTo(1.9626f, 13.9812f)
            lineTo(2.8025f, 8.6624f)
            horizontalLineTo(2.4445f)
            curveTo(1.837f, 8.6624f, 1.3433f, 8.1743f, 1.3335f, 7.5683f)
            lineTo(1.3334f, 7.5499f)
            verticalLineTo(6.2985f)
            curveTo(1.3334f, 5.6903f, 1.8209f, 5.196f, 2.4261f, 5.1862f)
            lineTo(2.4445f, 5.1861f)
            horizontalLineTo(5.9514f)
            verticalLineTo(2.5962f)
            curveTo(5.9514f, 1.4748f, 6.8504f, 0.5636f, 7.9662f, 0.5454f)
            lineTo(8f, 0.5452f)
            close()
            moveTo(12.0726f, 8.6624f)
            horizontalLineTo(3.9275f)
            lineTo(3.0601f, 14.155f)
            horizontalLineTo(5.0939f)
            lineTo(5.5908f, 11.0141f)
            curveTo(5.6447f, 10.6733f, 5.9647f, 10.4407f, 6.3056f, 10.4946f)
            curveTo(6.6467f, 10.5487f, 6.8794f, 10.8689f, 6.8254f, 11.2099f)
            lineTo(6.3595f, 14.155f)
            horizontalLineTo(12.94f)
            lineTo(12.0726f, 8.6624f)
            close()
            moveTo(5.9514f, 6.2985f)
            horizontalLineTo(2.4445f)
            verticalLineTo(7.55f)
            horizontalLineTo(13.5556f)
            verticalLineTo(6.2985f)
            lineTo(10.0487f, 6.2985f)
            verticalLineTo(6.3158f)
            horizontalLineTo(5.9514f)
            verticalLineTo(6.2985f)
            close()
            moveTo(8f, 1.6576f)
            curveTo(7.4875f, 1.6576f, 7.0709f, 2.0695f, 7.0627f, 2.5807f)
            lineTo(7.0625f, 2.5962f)
            verticalLineTo(5.1861f)
            horizontalLineTo(8.9375f)
            verticalLineTo(2.5962f)
            curveTo(8.9375f, 2.0881f, 8.5344f, 1.6744f, 8.0308f, 1.6581f)
            lineTo(8.0155f, 1.6577f)
            lineTo(8f, 1.6576f)
            close()
        }
    }.build()
}

// ── 右侧箭头 ──
internal val ChevronRightIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "ChevronRight", defaultWidth = 12.dp, defaultHeight = 24.dp,
        viewportWidth = 12f, viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.Black.copy(alpha = 0.4f)),
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(4.5061f, 5.7824f)
            lineTo(4.5381f, 5.8127f)
            lineTo(9.4879f, 10.7624f)
            curveTo(10.1571f, 11.4316f, 10.171f, 12.5079f, 9.5297f, 13.194f)
            lineTo(9.4879f, 13.2373f)
            lineTo(4.5381f, 18.187f)
            curveTo(4.4093f, 18.3159f, 4.2452f, 18.3881f, 4.0768f, 18.4035f)
            lineTo(4.0308f, 18.4063f)
            horizontalLineTo(3.9848f)
            curveTo(3.8006f, 18.4007f, 3.6181f, 18.3276f, 3.4775f, 18.187f)
            curveTo(3.195f, 17.9046f, 3.185f, 17.4529f, 3.4472f, 17.1584f)
            lineTo(3.4775f, 17.1264f)
            lineTo(8.0737f, 12.5302f)
            curveTo(8.3561f, 12.2477f, 8.3662f, 11.7961f, 8.1039f, 11.5016f)
            lineTo(8.0737f, 11.4695f)
            lineTo(3.4775f, 6.8733f)
            curveTo(3.1846f, 6.5804f, 3.1846f, 6.1055f, 3.4775f, 5.8127f)
            curveTo(3.6187f, 5.6714f, 3.8022f, 5.5983f, 3.9873f, 5.5933f)
            lineTo(4.0335f, 5.5934f)
            curveTo(4.2031f, 5.5992f, 4.3711f, 5.6622f, 4.5061f, 5.7824f)
            close()
        }
    }.build()
}

// ── 设备盒子 icon (19×15) ──
internal val MyDeviceBoxIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "MyDeviceBox", defaultWidth = 19.dp, defaultHeight = 15.dp,
        viewportWidth = 19f, viewportHeight = 15f
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(18.4116f, 11.1369f)
            lineTo(16.1658f, 1.4532f)
            curveTo(16.1036f, 1.058f, 15.9062f, 0.6965f, 15.6073f, 0.4305f)
            curveTo(15.3088f, 0.1652f, 14.9281f, 0.0133f, 14.5302f, 0f)
            lineTo(4.0101f, 0f)
            curveTo(3.1757f, 0.0325f, 2.5018f, 0.6779f, 2.3738f, 1.4857f)
            lineTo(0.1287f, 11.1362f)
            curveTo(0.0325f, 11.3949f, 0f, 11.6861f, 0f, 11.9441f)
            curveTo(0f, 13.3973f, 1.1872f, 14.5918f, 2.663f, 14.5918f)
            lineTo(15.9091f, 14.5918f)
            curveTo(17.3855f, 14.5918f, 18.5714f, 13.3973f, 18.5714f, 11.9447f)
            curveTo(18.5714f, 11.6861f, 18.5078f, 11.3956f, 18.4116f, 11.1369f)
            close()
            moveTo(15.9091f, 13.3005f)
            lineTo(2.663f, 13.3005f)
            curveTo(2.4894f, 13.3025f, 2.3171f, 13.2697f, 2.1565f, 13.2038f)
            curveTo(1.9958f, 13.1379f, 1.85f, 13.0404f, 1.7278f, 12.9171f)
            curveTo(1.605f, 12.7936f, 1.5081f, 12.6469f, 1.4428f, 12.4855f)
            curveTo(1.3774f, 12.3241f, 1.3449f, 12.1513f, 1.3471f, 11.9772f)
            curveTo(1.3471f, 11.2344f, 1.9573f, 10.6527f, 2.663f, 10.6527f)
            lineTo(15.9091f, 10.6527f)
            curveTo(16.6473f, 10.6527f, 17.2243f, 11.2344f, 17.2243f, 11.9772f)
            curveTo(17.2568f, 12.6869f, 16.6473f, 13.3005f, 15.9091f, 13.3005f)
            close()
            moveTo(15.2359f, 11.5567f)
            curveTo(15.1266f, 11.5554f, 15.0205f, 11.5942f, 14.9378f, 11.6657f)
            curveTo(14.8551f, 11.7371f, 14.8014f, 11.8364f, 14.7868f, 11.9447f)
            curveTo(14.7857f, 12.0537f, 14.824f, 12.1594f, 14.8947f, 12.2423f)
            curveTo(14.9654f, 12.3252f, 15.0637f, 12.3797f, 15.1715f, 12.3958f)
            lineTo(15.2359f, 12.3958f)
            curveTo(15.346f, 12.3935f, 15.4511f, 12.3487f, 15.5289f, 12.2707f)
            curveTo(15.6067f, 12.1926f, 15.6512f, 12.0874f, 15.6531f, 11.9772f)
            curveTo(15.6531f, 11.7186f, 15.4926f, 11.5567f, 15.2359f, 11.5567f)
            close()
            moveTo(14.3696f, 11.9772f)
            curveTo(14.3696f, 11.4931f, 14.7543f, 11.1057f, 15.2359f, 11.1057f)
            curveTo(15.7167f, 11.1057f, 16.1021f, 11.4931f, 16.1021f, 11.9772f)
            curveTo(16.1021f, 12.4614f, 15.7174f, 12.8488f, 15.2359f, 12.8488f)
            curveTo(15.1216f, 12.849f, 15.0085f, 12.8265f, 14.9029f, 12.7827f)
            curveTo(14.7974f, 12.7389f, 14.7016f, 12.6746f, 14.6211f, 12.5936f)
            curveTo(14.5405f, 12.5125f, 14.4768f, 12.4164f, 14.4337f, 12.3106f)
            curveTo(14.3905f, 12.2048f, 14.3688f, 12.0915f, 14.3696f, 11.9772f)
            close()
        }
    }.build()
}
