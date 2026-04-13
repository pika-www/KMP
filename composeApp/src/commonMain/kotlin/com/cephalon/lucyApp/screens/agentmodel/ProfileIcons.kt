package com.cephalon.lucyApp.screens.agentmodel

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// ── 充值账户 (钱包) ──
internal val WalletIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Wallet", defaultWidth = 20.dp, defaultHeight = 20.dp,
        viewportWidth = 20f, viewportHeight = 20f
    ).apply {
        path(
            fill = SolidColor(Color.Black.copy(alpha = 0.4f)),
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(16.1582f, 2.6963f)
            curveTo(17.5375f, 2.6965f, 18.6609f, 3.8484f, 18.6621f, 5.2617f)
            verticalLineTo(14.5537f)
            curveTo(18.6609f, 15.9672f, 17.5375f, 17.1173f, 16.1582f, 17.1191f)
            horizontalLineTo(3.8242f)
            curveTo(2.4446f, 17.119f, 1.3214f, 15.9671f, 1.3203f, 14.5537f)
            verticalLineTo(5.2617f)
            curveTo(1.3206f, 3.8482f, 2.4449f, 2.6976f, 3.8242f, 2.6963f)
            horizontalLineTo(16.1582f)
            close()
            moveTo(3.8242f, 4.4482f)
            curveTo(3.393f, 4.4484f, 3.0406f, 4.8143f, 3.04f, 5.2617f)
            verticalLineTo(14.5527f)
            curveTo(3.0402f, 14.9997f, 3.3933f, 15.3644f, 3.8252f, 15.3652f)
            horizontalLineTo(16.1572f)
            curveTo(16.5887f, 15.3652f, 16.9416f, 14.9995f, 16.9424f, 14.5518f)
            verticalLineTo(12.8955f)
            horizontalLineTo(12.6631f)
            curveTo(11.0568f, 12.8955f, 9.75f, 11.5544f, 9.75f, 9.9082f)
            curveTo(9.7502f, 8.2621f, 11.0571f, 6.9205f, 12.6631f, 6.9199f)
            horizontalLineTo(16.9434f)
            verticalLineTo(5.2617f)
            curveTo(16.9434f, 4.8147f, 16.5901f, 4.4502f, 16.1582f, 4.4492f)
            lineTo(3.8242f, 4.4482f)
            close()
            moveTo(12.6631f, 8.4287f)
            curveTo(12.0034f, 8.4287f, 11.4668f, 9.0848f, 11.4668f, 9.8906f)
            curveTo(11.4669f, 10.6964f, 12.0035f, 11.3525f, 12.6631f, 11.3525f)
            horizontalLineTo(16.9434f)
            verticalLineTo(8.4287f)
            horizontalLineTo(12.6631f)
            close()
            moveTo(13.4521f, 8.8193f)
            curveTo(14.0395f, 8.8193f, 14.5176f, 9.3074f, 14.5176f, 9.9072f)
            curveTo(14.5175f, 10.507f, 14.0384f, 10.9951f, 13.4521f, 10.9951f)
            curveTo(12.865f, 10.9949f, 12.3877f, 10.5068f, 12.3877f, 9.9072f)
            curveTo(12.3877f, 9.3076f, 12.8649f, 8.8196f, 13.4521f, 8.8193f)
            close()
        }
    }.build()
}

// ── 我的 NAS ──
internal val NasIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Nas", defaultWidth = 20.dp, defaultHeight = 20.dp,
        viewportWidth = 20f, viewportHeight = 20f
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.Black.copy(alpha = 0.4f)),
            strokeLineWidth = 1.66667f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(14.499f, 2.9468f)
            curveTo(15.0326f, 2.9646f, 15.5427f, 3.1688f, 15.9424f, 3.5239f)
            horizontalLineTo(15.9434f)
            curveTo(16.3273f, 3.8657f, 16.5839f, 4.3262f, 16.6768f, 4.8306f)
            lineTo(18.583f, 13.0513f)
            curveTo(18.6781f, 13.3253f, 18.75f, 13.6456f, 18.75f, 13.9634f)
            curveTo(18.75f, 15.6634f, 17.3641f, 17.0532f, 15.6465f, 17.0532f)
            horizontalLineTo(4.3535f)
            curveTo(2.6367f, 17.0532f, 1.25f, 15.6632f, 1.25f, 13.9624f)
            curveTo(1.25f, 13.7003f, 1.2801f, 13.3716f, 1.3897f, 13.0474f)
            lineTo(3.2949f, 4.8579f)
            curveTo(3.4848f, 3.8298f, 4.3553f, 2.9902f, 5.4697f, 2.9468f)
            horizontalLineTo(14.499f)
            close()
            moveTo(4.3535f, 13.6948f)
            curveTo(4.204f, 13.6948f, 4.0647f, 13.826f, 4.0645f, 13.9907f)
            verticalLineTo(14.0015f)
            curveTo(4.064f, 14.0391f, 4.0708f, 14.0769f, 4.085f, 14.1118f)
            curveTo(4.092f, 14.1293f, 4.101f, 14.146f, 4.1113f, 14.1616f)
            lineTo(4.1475f, 14.2046f)
            lineTo(4.1484f, 14.2056f)
            curveTo(4.1739f, 14.2313f, 4.2038f, 14.2524f, 4.2373f, 14.2661f)
            curveTo(4.2709f, 14.2799f, 4.3075f, 14.2861f, 4.3438f, 14.2856f)
            horizontalLineTo(13.5283f)
            curveTo(13.5098f, 14.1867f, 13.5002f, 14.086f, 13.501f, 13.9849f)
            curveTo(13.5014f, 13.8857f, 13.5115f, 13.7889f, 13.5293f, 13.6948f)
            horizontalLineTo(4.3535f)
            close()
        }
    }.build()
}

// ── 我的设备 ──
internal val DevicesIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Devices", defaultWidth = 20.dp, defaultHeight = 20.dp,
        viewportWidth = 20f, viewportHeight = 20f
    ).apply {
        path(
            fill = SolidColor(Color.Black.copy(alpha = 0.4f)),
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(14.9004f, 10.8613f)
            curveTo(17.2564f, 10.8615f, 19.1737f, 12.7209f, 19.1738f, 15.0068f)
            curveTo(19.1738f, 17.2913f, 17.2565f, 19.1502f, 14.9004f, 19.1504f)
            horizontalLineTo(5.1133f)
            curveTo(2.757f, 19.1503f, 0.8398f, 17.2922f, 0.8398f, 15.0068f)
            curveTo(0.84f, 12.7216f, 2.7571f, 10.8614f, 5.1133f, 10.8613f)
            horizontalLineTo(14.9004f)
            close()
            moveTo(5.1133f, 12.3877f)
            curveTo(3.5966f, 12.3877f, 2.3633f, 13.5628f, 2.3633f, 15.0078f)
            curveTo(2.3633f, 16.4513f, 3.5966f, 17.6259f, 5.1133f, 17.626f)
            horizontalLineTo(14.9004f)
            curveTo(16.4162f, 17.6258f, 17.6504f, 16.451f, 17.6504f, 15.0068f)
            curveTo(17.6503f, 13.562f, 16.4161f, 12.3869f, 14.9004f, 12.3867f)
            lineTo(5.1133f, 12.3877f)
            close()
            moveTo(5.8018f, 13.8164f)
            curveTo(6.4339f, 13.8166f, 6.9463f, 14.3297f, 6.9463f, 14.9619f)
            curveTo(6.9461f, 15.594f, 6.4338f, 16.1063f, 5.8018f, 16.1064f)
            curveTo(5.1695f, 16.1064f, 4.6564f, 15.5941f, 4.6563f, 14.9619f)
            curveTo(4.6563f, 14.3296f, 5.1694f, 13.8164f, 5.8018f, 13.8164f)
            close()
            moveTo(14.8994f, 0.8496f)
            curveTo(17.2556f, 0.8497f, 19.1728f, 2.7088f, 19.1729f, 4.9941f)
            curveTo(19.1729f, 7.2794f, 17.2557f, 9.1375f, 14.9004f, 9.1377f)
            horizontalLineTo(5.1123f)
            curveTo(2.7569f, 9.1375f, 0.8398f, 7.2787f, 0.8398f, 4.9941f)
            curveTo(0.8399f, 2.7096f, 2.7569f, 0.8498f, 5.1123f, 0.8496f)
            horizontalLineTo(14.8994f)
            close()
            moveTo(5.1123f, 2.375f)
            curveTo(3.5957f, 2.3752f, 2.3623f, 3.55f, 2.3623f, 4.9941f)
            curveTo(2.3623f, 6.4383f, 3.5957f, 7.6131f, 5.1123f, 7.6133f)
            horizontalLineTo(14.8994f)
            curveTo(16.4153f, 7.6132f, 17.6494f, 6.4384f, 17.6494f, 4.9941f)
            curveTo(17.6494f, 3.55f, 16.4162f, 2.3752f, 14.9004f, 2.375f)
            horizontalLineTo(5.1123f)
            close()
            moveTo(14.1982f, 3.8936f)
            curveTo(14.8304f, 3.8938f, 15.3428f, 4.4069f, 15.3428f, 5.0391f)
            curveTo(15.3426f, 5.6711f, 14.8303f, 6.1834f, 14.1982f, 6.1836f)
            curveTo(13.566f, 6.1836f, 13.0529f, 5.6712f, 13.0527f, 5.0391f)
            curveTo(13.0527f, 4.4067f, 13.5659f, 3.8936f, 14.1982f, 3.8936f)
            close()
        }
    }.build()
}

// ── 意见反馈 ──
internal val FeedbackIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Feedback", defaultWidth = 20.dp, defaultHeight = 20.dp,
        viewportWidth = 20f, viewportHeight = 20f
    ).apply {
        path(
            fill = SolidColor(Color.Black.copy(alpha = 0.4f)),
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(13.3096f, 1.2407f)
            curveTo(13.8306f, 1.241f, 14.2451f, 1.6551f, 14.2451f, 2.1763f)
            curveTo(14.2451f, 2.6974f, 13.8306f, 3.1116f, 13.3096f, 3.1118f)
            horizontalLineTo(3.9395f)
            curveTo(3.7256f, 3.1118f, 3.5381f, 3.2993f, 3.5381f, 3.5132f)
            verticalLineTo(16.8931f)
            curveTo(3.5382f, 17.1201f, 3.7123f, 17.2935f, 3.9395f, 17.2935f)
            horizontalLineTo(15.9824f)
            curveTo(16.2095f, 17.2935f, 16.3836f, 17.1201f, 16.3838f, 16.8931f)
            verticalLineTo(6.186f)
            curveTo(16.3838f, 5.667f, 16.7943f, 5.254f, 17.3125f, 5.2505f)
            curveTo(17.8306f, 5.2541f, 18.2412f, 5.6671f, 18.2412f, 6.186f)
            verticalLineTo(16.8931f)
            curveTo(18.2545f, 17.4945f, 18.0136f, 18.0689f, 17.5859f, 18.4966f)
            curveTo(17.1582f, 18.9242f, 16.5972f, 19.1655f, 15.9824f, 19.1655f)
            horizontalLineTo(3.9395f)
            curveTo(2.6831f, 19.1655f, 1.6672f, 18.1494f, 1.667f, 16.8931f)
            verticalLineTo(3.5132f)
            curveTo(1.667f, 2.2567f, 2.683f, 1.2407f, 3.9395f, 1.2407f)
            horizontalLineTo(13.3096f)
            close()
            moveTo(13.3096f, 11.4302f)
            curveTo(13.8306f, 11.4304f, 14.2451f, 11.8456f, 14.2451f, 12.3667f)
            curveTo(14.245f, 12.6071f, 14.1644f, 12.834f, 13.9775f, 13.021f)
            curveTo(13.7905f, 13.208f, 13.5633f, 13.3021f, 13.3096f, 13.3022f)
            horizontalLineTo(5.2754f)
            curveTo(4.7544f, 13.302f, 4.3401f, 12.8876f, 4.3398f, 12.3667f)
            curveTo(4.3398f, 11.8455f, 4.7543f, 11.4304f, 5.2754f, 11.4302f)
            horizontalLineTo(13.3096f)
            close()
            moveTo(9.2861f, 6.5874f)
            curveTo(9.8073f, 6.5876f, 10.2217f, 7.0018f, 10.2217f, 7.523f)
            curveTo(10.2217f, 8.0441f, 9.8073f, 8.4583f, 9.2861f, 8.4585f)
            horizontalLineTo(5.2754f)
            curveTo(4.7543f, 8.4583f, 4.3399f, 8.0441f, 4.3398f, 7.523f)
            curveTo(4.3398f, 7.0018f, 4.7543f, 6.5876f, 5.2754f, 6.5874f)
            horizontalLineTo(9.2861f)
            close()
            moveTo(17.8115f, 0.8315f)
            curveTo(18.059f, 0.8316f, 18.2962f, 0.9295f, 18.4727f, 1.103f)
            curveTo(18.8335f, 1.4639f, 18.8335f, 2.0654f, 18.4727f, 2.4263f)
            lineTo(12.708f, 8.1909f)
            curveTo(12.5343f, 8.3647f, 12.3067f, 8.4585f, 12.0527f, 8.4585f)
            curveTo(11.8674f, 8.4574f, 11.6861f, 8.4016f, 11.5322f, 8.2983f)
            curveTo(11.3785f, 8.1951f, 11.2587f, 8.0484f, 11.1875f, 7.8774f)
            curveTo(11.1163f, 7.7065f, 11.0968f, 7.5183f, 11.1318f, 7.3364f)
            curveTo(11.1669f, 7.1545f, 11.2551f, 6.9873f, 11.3848f, 6.855f)
            lineTo(17.1494f, 1.103f)
            curveTo(17.3259f, 0.9295f, 17.564f, 0.8315f, 17.8115f, 0.8315f)
            close()
        }
    }.build()
}

// ── 清除缓存 ──
internal val ClearCacheIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "ClearCache", defaultWidth = 20.dp, defaultHeight = 20.dp,
        viewportWidth = 20f, viewportHeight = 20f
    ).apply {
        path(
            fill = SolidColor(Color.Black.copy(alpha = 0.4f)),
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(12.6669f, 3.2844f)
            curveTo(12.3114f, 2.9291f, 11.8294f, 2.7295f, 11.3268f, 2.7295f)
            curveTo(10.8242f, 2.7295f, 10.3422f, 2.9291f, 9.9867f, 3.2844f)
            lineTo(8.8551f, 4.4141f)
            curveTo(8.6775f, 4.5919f, 8.5777f, 4.8329f, 8.5777f, 5.0842f)
            curveTo(8.5777f, 5.3355f, 8.6775f, 5.5765f, 8.8551f, 5.7542f)
            lineTo(14.8864f, 11.7855f)
            lineTo(16.6871f, 9.9848f)
            curveTo(17.0424f, 9.6294f, 17.2421f, 9.1474f, 17.2421f, 8.6448f)
            curveTo(17.2421f, 8.1422f, 17.0424f, 7.6601f, 16.6871f, 7.3047f)
            lineTo(12.6659f, 3.2844f)
            horizontalLineTo(12.6669f)
            close()
            moveTo(9.9867f, 16.6871f)
            lineTo(13.5473f, 13.1256f)
            lineTo(6.846f, 6.4252f)
            curveTo(6.6697f, 6.2492f, 6.5299f, 6.0401f, 6.4345f, 5.81f)
            curveTo(6.3392f, 5.5799f, 6.2901f, 5.3333f, 6.2901f, 5.0842f)
            curveTo(6.2901f, 4.8351f, 6.3392f, 4.5884f, 6.4345f, 4.3583f)
            curveTo(6.5299f, 4.1282f, 6.6697f, 3.9192f, 6.846f, 3.7431f)
            lineTo(8.6466f, 1.9434f)
            curveTo(9.3575f, 1.2327f, 10.3216f, 0.8335f, 11.3268f, 0.8335f)
            curveTo(12.332f, 0.8335f, 13.2961f, 1.2327f, 14.0069f, 1.9434f)
            lineTo(18.0291f, 5.9646f)
            curveTo(18.3811f, 6.3166f, 18.6604f, 6.7345f, 18.8509f, 7.1945f)
            curveTo(19.0414f, 7.6544f, 19.1395f, 8.1474f, 19.1395f, 8.6452f)
            curveTo(19.1395f, 9.1431f, 19.0414f, 9.636f, 18.8509f, 10.096f)
            curveTo(18.6604f, 10.5559f, 18.3811f, 10.9738f, 18.0291f, 11.3259f)
            lineTo(12.084f, 17.27f)
            horizontalLineTo(18.2186f)
            curveTo(18.3431f, 17.27f, 18.4663f, 17.2945f, 18.5813f, 17.3421f)
            curveTo(18.6963f, 17.3897f, 18.8008f, 17.4595f, 18.8888f, 17.5475f)
            curveTo(18.9768f, 17.6356f, 19.0466f, 17.74f, 19.0942f, 17.855f)
            curveTo(19.1418f, 17.97f, 19.1663f, 18.0932f, 19.1663f, 18.2177f)
            curveTo(19.1663f, 18.3421f, 19.1418f, 18.4654f, 19.0942f, 18.5804f)
            curveTo(19.0466f, 18.6953f, 18.9768f, 18.7998f, 18.8888f, 18.8878f)
            curveTo(18.8008f, 18.9758f, 18.6963f, 19.0456f, 18.5813f, 19.0933f)
            curveTo(18.4663f, 19.1409f, 18.3431f, 19.1654f, 18.2186f, 19.1654f)
            horizontalLineTo(9.6891f)
            curveTo(9.5545f, 19.1655f, 9.4213f, 19.1367f, 9.2987f, 19.0811f)
            curveTo(8.6992f, 19.1865f, 8.0832f, 19.1458f, 7.5028f, 18.9625f)
            curveTo(6.9224f, 18.7792f, 6.3948f, 18.4587f, 5.9646f, 18.0281f)
            lineTo(1.9434f, 14.007f)
            curveTo(1.5914f, 13.6549f, 1.3121f, 13.237f, 1.1216f, 12.7771f)
            curveTo(0.9311f, 12.3171f, 0.833f, 11.8242f, 0.833f, 11.3263f)
            curveTo(0.833f, 10.8285f, 0.9311f, 10.3355f, 1.1216f, 9.8756f)
            curveTo(1.3121f, 9.4156f, 1.5914f, 8.9977f, 1.9434f, 8.6457f)
            lineTo(4.1649f, 6.4252f)
            curveTo(4.3427f, 6.2475f, 4.5838f, 6.1477f, 4.8352f, 6.1478f)
            curveTo(5.0866f, 6.1479f, 5.3277f, 6.2478f, 5.5054f, 6.4257f)
            curveTo(5.6831f, 6.6035f, 5.7829f, 6.8446f, 5.7828f, 7.096f)
            curveTo(5.7827f, 7.3474f, 5.6828f, 7.5885f, 5.5049f, 7.7662f)
            lineTo(3.2844f, 9.9867f)
            curveTo(2.9291f, 10.3422f, 2.7295f, 10.8242f, 2.7295f, 11.3268f)
            curveTo(2.7295f, 11.8294f, 2.9291f, 12.3114f, 3.2844f, 12.6669f)
            lineTo(7.3047f, 16.6881f)
            curveTo(7.4807f, 16.8642f, 7.6897f, 17.0038f, 7.9197f, 17.0991f)
            curveTo(8.1497f, 17.1944f, 8.3962f, 17.2435f, 8.6452f, 17.2435f)
            curveTo(8.8942f, 17.2435f, 9.1407f, 17.1944f, 9.3707f, 17.0991f)
            curveTo(9.6008f, 17.0038f, 9.8098f, 16.8642f, 9.9858f, 16.6881f)
            lineTo(9.9867f, 16.6871f)
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
