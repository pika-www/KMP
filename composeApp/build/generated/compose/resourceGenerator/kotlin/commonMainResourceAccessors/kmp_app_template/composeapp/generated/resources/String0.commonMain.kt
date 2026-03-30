@file:OptIn(InternalResourceApi::class)

package kmp_app_template.composeapp.generated.resources

import kotlin.OptIn
import kotlin.String
import kotlin.collections.MutableMap
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.ResourceItem
import org.jetbrains.compose.resources.StringResource

private const val MD: String = "composeResources/kmp_app_template.composeapp.generated.resources/"

internal val Res.string.back: StringResource by lazy {
      StringResource("string:back", "back", setOf(
        ResourceItem(setOf(), "${MD}values/strings.commonMain.cvr", 10, 20),
      ))
    }

internal val Res.string.label_artist: StringResource by lazy {
      StringResource("string:label_artist", "label_artist", setOf(
        ResourceItem(setOf(), "${MD}values/strings.commonMain.cvr", 31, 28),
      ))
    }

internal val Res.string.label_credits: StringResource by lazy {
      StringResource("string:label_credits", "label_credits", setOf(
        ResourceItem(setOf(), "${MD}values/strings.commonMain.cvr", 60, 33),
      ))
    }

internal val Res.string.label_date: StringResource by lazy {
      StringResource("string:label_date", "label_date", setOf(
        ResourceItem(setOf(), "${MD}values/strings.commonMain.cvr", 94, 26),
      ))
    }

internal val Res.string.label_department: StringResource by lazy {
      StringResource("string:label_department", "label_department", setOf(
        ResourceItem(setOf(), "${MD}values/strings.commonMain.cvr", 121, 40),
      ))
    }

internal val Res.string.label_dimensions: StringResource by lazy {
      StringResource("string:label_dimensions", "label_dimensions", setOf(
        ResourceItem(setOf(), "${MD}values/strings.commonMain.cvr", 162, 40),
      ))
    }

internal val Res.string.label_medium: StringResource by lazy {
      StringResource("string:label_medium", "label_medium", setOf(
        ResourceItem(setOf(), "${MD}values/strings.commonMain.cvr", 203, 28),
      ))
    }

internal val Res.string.label_repository: StringResource by lazy {
      StringResource("string:label_repository", "label_repository", setOf(
        ResourceItem(setOf(), "${MD}values/strings.commonMain.cvr", 232, 40),
      ))
    }

internal val Res.string.label_title: StringResource by lazy {
      StringResource("string:label_title", "label_title", setOf(
        ResourceItem(setOf(), "${MD}values/strings.commonMain.cvr", 273, 27),
      ))
    }

internal val Res.string.no_data_available: StringResource by lazy {
      StringResource("string:no_data_available", "no_data_available", setOf(
        ResourceItem(setOf(), "${MD}values/strings.commonMain.cvr", 301, 49),
      ))
    }

@InternalResourceApi
internal fun _collectCommonMainString0Resources(map: MutableMap<String, StringResource>) {
  map.put("back", Res.string.back)
  map.put("label_artist", Res.string.label_artist)
  map.put("label_credits", Res.string.label_credits)
  map.put("label_date", Res.string.label_date)
  map.put("label_department", Res.string.label_department)
  map.put("label_dimensions", Res.string.label_dimensions)
  map.put("label_medium", Res.string.label_medium)
  map.put("label_repository", Res.string.label_repository)
  map.put("label_title", Res.string.label_title)
  map.put("no_data_available", Res.string.no_data_available)
}
