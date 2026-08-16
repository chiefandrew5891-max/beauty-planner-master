package com.andrey.beautyplanner.appcontent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.Locales
import com.andrey.beautyplanner.generated.resources.Res
import com.andrey.beautyplanner.generated.resources.guide_auth_image
import com.andrey.beautyplanner.generated.resources.guide_create_appointment_form_image
import com.andrey.beautyplanner.generated.resources.guide_delete_account_button_image
import com.andrey.beautyplanner.generated.resources.guide_delete_account_confirm_image
import com.andrey.beautyplanner.generated.resources.guide_main_screen_image
import com.andrey.beautyplanner.generated.resources.guide_profile_image
import com.andrey.beautyplanner.generated.resources.guide_settings_image
import com.andrey.beautyplanner.generated.resources.guide_statistics_image
import com.andrey.beautyplanner.generated.resources.guide_subscription_image
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun UserGuideScreen() {
    val fontScale = AppSettings.getFontScale()
    val onBg = MaterialTheme.colors.onBackground

    CenteredNarrowContentContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = Locales.t("guide_user_manual"),
                fontSize = (22 * fontScale).sp,
                fontWeight = FontWeight.Bold,
                color = onBg
            )

            Text(
                text = Locales.t("guide_user_manual_subtitle"),
                fontSize = (14 * fontScale).sp,
                color = onBg.copy(alpha = 0.82f)
            )

            Divider()

            GuideSectionTitle(Locales.t("guide_contents"))
            GuideBulletList(
                items = listOf(
                    Locales.t("guide_section_app_overview"),
                    Locales.t("guide_section_access_model"),
                    Locales.t("guide_section_first_launch_sign_in"),
                    Locales.t("guide_section_main_screen"),
                    Locales.t("guide_section_settings_screen"),
                    Locales.t("guide_section_professional_profile"),
                    Locales.t("guide_section_appearance_theme"),
                    Locales.t("guide_section_subscription_access"),
                    Locales.t("guide_section_my_services"),
                    Locales.t("guide_section_unavailable_time_schedule"),
                    Locales.t("guide_section_notifications"),
                    Locales.t("guide_section_security"),
                    Locales.t("guide_section_backup"),
                    Locales.t("guide_section_side_menu"),
                    Locales.t("guide_section_statistics"),
                    Locales.t("guide_section_unpaid_appointments"),
                    Locales.t("guide_section_archive"),
                    Locales.t("guide_section_creating_new_appointment"),
                    Locales.t("guide_section_client_data_autofill"),
                    Locales.t("guide_section_editing_appointment"),
                    Locales.t("guide_section_rescheduling_appointment"),
                    Locales.t("guide_section_deleting_appointment"),
                    Locales.t("guide_section_account_deletion")
                )
            )

            Divider()

            GuideExpandableSection(
                title = Locales.t("guide_section_app_overview"),
                initiallyExpanded = true
            ) {
                GuideParagraph(Locales.t("guide_app_overview_p1"))
                GuideParagraph(Locales.t("guide_app_overview_p2"))
                GuideParagraph(Locales.t("guide_app_overview_p3"))
                GuideBulletList(
                    title = Locales.t("guide_common_core_features"),
                    items = listOf(
                        Locales.t("guide_app_overview_feature_1"),
                        Locales.t("guide_app_overview_feature_2"),
                        Locales.t("guide_app_overview_feature_3"),
                        Locales.t("guide_app_overview_feature_4"),
                        Locales.t("guide_app_overview_feature_5"),
                        Locales.t("guide_app_overview_feature_6"),
                        Locales.t("guide_app_overview_feature_7"),
                        Locales.t("guide_app_overview_feature_8"),
                        Locales.t("guide_app_overview_feature_9"),
                        Locales.t("guide_app_overview_feature_10"),
                        Locales.t("guide_app_overview_feature_11")
                    )
                )
            }

            Divider()

            GuideExpandableSection(
                title = Locales.t("guide_section_access_model")
            ) {
                GuideParagraph(Locales.t("guide_access_model_p1"))
                GuideMiniSectionTitle(Locales.t("guide_common_free_version"))
                GuideParagraph(Locales.t("guide_access_model_free_p1"))
                GuideParagraph(Locales.t("guide_access_model_free_p2"))
                GuideParagraph(Locales.t("guide_access_model_free_p3"))

                GuideMiniSectionTitle(Locales.t("guide_common_premium_subscription"))
                GuideParagraph(Locales.t("guide_access_model_premium_p1"))
                GuideBulletList(
                    items = listOf(
                        Locales.t("guide_access_model_premium_feature_1"),
                        Locales.t("guide_access_model_premium_feature_2"),
                        Locales.t("guide_access_model_premium_feature_3"),
                        Locales.t("guide_access_model_premium_feature_4"),
                        Locales.t("guide_access_model_premium_feature_5")
                    )
                )
            }

            Divider()

            GuideExpandableSection(
                title = Locales.t("guide_section_first_launch_sign_in")
            ) {
                GuideParagraph(Locales.t("guide_first_launch_sign_in_p1"))
                GuideBulletList(
                    title = Locales.t("guide_common_available_sign_in_methods"),
                    items = listOf(
                        Locales.t("guide_first_launch_sign_in_method_1"),
                        Locales.t("guide_first_launch_sign_in_method_2"),
                        Locales.t("guide_first_launch_sign_in_method_3"),
                        Locales.t("guide_first_launch_sign_in_method_4")
                    )
                )
                GuideParagraph(Locales.t("guide_first_launch_sign_in_p2"))
                GuideParagraph(Locales.t("guide_first_launch_sign_in_p3"))

                // IMAGE RESOURCE:
                // File name: guide_auth_image.png
                // Put here: composeApp/src/commonMain/composeResources/drawable/guide_auth_image.png
                GuideImage(Res.drawable.guide_auth_image)
            }

            Divider()

            GuideExpandableSection(
                title = Locales.t("guide_section_main_screen")
            ) {
                GuideParagraph(Locales.t("guide_main_screen_p1"))
                GuideMiniSectionTitle(Locales.t("guide_main_screen_calendar_title"))
                GuideParagraph(Locales.t("guide_main_screen_calendar_body"))
                GuideMiniSectionTitle(Locales.t("guide_main_screen_upcoming_title"))
                GuideParagraph(Locales.t("guide_main_screen_upcoming_body"))
                GuideParagraph(Locales.t("guide_main_screen_p2"))

                // IMAGE RESOURCE:
                // File name: guide_main_screen_image.png
                // Put here: composeApp/src/commonMain/composeResources/drawable/guide_main_screen_image.png
                GuideImage(Res.drawable.guide_main_screen_image)
            }

            Divider()

            GuideExpandableSection(
                title = Locales.t("guide_section_settings_screen")
            ) {
                GuideParagraph(Locales.t("guide_settings_screen_p1"))
                GuideParagraph(Locales.t("guide_settings_screen_p2"))
                GuideBulletList(
                    title = Locales.t("guide_common_main_sections_of_settings"),
                    items = listOf(
                        Locales.t("guide_settings_screen_section_1"),
                        Locales.t("guide_settings_screen_section_2"),
                        Locales.t("guide_settings_screen_section_3"),
                        Locales.t("guide_settings_screen_section_4"),
                        Locales.t("guide_settings_screen_section_5"),
                        Locales.t("guide_settings_screen_section_6"),
                        Locales.t("guide_settings_screen_section_7"),
                        Locales.t("guide_settings_screen_section_8")
                    )
                )

                // IMAGE RESOURCE:
                // File name: guide_settings_image.png
                // Put here: composeApp/src/commonMain/composeResources/drawable/guide_settings_image.png
                GuideImage(Res.drawable.guide_settings_image)
            }

            Divider()

            GuideExpandableSection(
                title = Locales.t("guide_section_professional_profile")
            ) {
                GuideParagraph(Locales.t("guide_professional_profile_p1"))
                GuideParagraph(Locales.t("guide_professional_profile_p2"))
                GuideBulletList(
                    title = Locales.t("guide_common_on_this_screen_the_user_can"),
                    items = listOf(
                        Locales.t("guide_professional_profile_item_1"),
                        Locales.t("guide_professional_profile_item_2"),
                        Locales.t("guide_professional_profile_item_3"),
                        Locales.t("guide_professional_profile_item_4"),
                        Locales.t("guide_professional_profile_item_5"),
                        Locales.t("guide_professional_profile_item_6")
                    )
                )
                GuideParagraph(Locales.t("guide_professional_profile_p3"))

                // IMAGE RESOURCE:
                // File name: guide_profile_image.png
                // Put here: composeApp/src/commonMain/composeResources/drawable/guide_profile_image.png
                GuideImage(Res.drawable.guide_profile_image)
            }

            Divider()

            GuideExpandableSection(
                title = Locales.t("guide_section_appearance_theme")
            ) {
                GuideParagraph(Locales.t("guide_appearance_theme_p1"))
                GuideBulletList(
                    title = Locales.t("guide_common_available_settings"),
                    items = listOf(
                        Locales.t("guide_appearance_theme_item_1"),
                        Locales.t("guide_appearance_theme_item_2"),
                        Locales.t("guide_appearance_theme_item_3"),
                        Locales.t("guide_appearance_theme_item_4"),
                        Locales.t("guide_appearance_theme_item_5")
                    )
                )
                GuideParagraph(Locales.t("guide_appearance_theme_p2"))
                GuideParagraph(Locales.t("guide_appearance_theme_p3"))
            }

            Divider()

            GuideExpandableSection(
                title = Locales.t("guide_section_subscription_access")
            ) {
                GuideParagraph(Locales.t("guide_subscription_access_p1"))
                GuideBulletList(
                    title = Locales.t("guide_common_on_this_screen_the_user_can"),
                    items = listOf(
                        Locales.t("guide_subscription_access_item_1"),
                        Locales.t("guide_subscription_access_item_2"),
                        Locales.t("guide_subscription_access_item_3"),
                        Locales.t("guide_subscription_access_item_4"),
                        Locales.t("guide_subscription_access_item_5")
                    )
                )
                GuideParagraph(Locales.t("guide_subscription_access_p2"))
                GuideParagraph(Locales.t("guide_subscription_access_p3"))

                // IMAGE RESOURCE:
                // File name: guide_subscription_image.png
                // Put here: composeApp/src/commonMain/composeResources/drawable/guide_subscription_image.png
                GuideImage(Res.drawable.guide_subscription_image)
            }

            Divider()

            GuideExpandableSection(
                title = Locales.t("guide_section_my_services")
            ) {
                GuideParagraph(Locales.t("guide_my_services_p1"))
                GuideBulletList(
                    title = Locales.t("guide_common_in_this_section_the_user_can"),
                    items = listOf(
                        Locales.t("guide_my_services_item_1"),
                        Locales.t("guide_my_services_item_2"),
                        Locales.t("guide_my_services_item_3"),
                        Locales.t("guide_my_services_item_4"),
                        Locales.t("guide_my_services_item_5")
                    )
                )
                GuideParagraph(Locales.t("guide_my_services_p2"))
            }

            Divider()

            GuideExpandableSection(
                title = Locales.t("guide_section_unavailable_time_schedule")
            ) {
                GuideParagraph(Locales.t("guide_unavailable_time_schedule_p1"))
                GuideBulletList(
                    title = Locales.t("guide_common_in_this_section_the_user_can"),
                    items = listOf(
                        Locales.t("guide_unavailable_time_schedule_item_1"),
                        Locales.t("guide_unavailable_time_schedule_item_2"),
                        Locales.t("guide_unavailable_time_schedule_item_3")
                    )
                )
                GuideParagraph(Locales.t("guide_unavailable_time_schedule_p2"))
                GuideParagraph(Locales.t("guide_unavailable_time_schedule_p3"))
            }

            Divider()

            GuideExpandableSection(
                title = Locales.t("guide_section_notifications")
            ) {
                GuideParagraph(Locales.t("guide_notifications_p1"))
                GuideBulletList(
                    title = Locales.t("guide_common_the_user_can"),
                    items = listOf(
                        Locales.t("guide_notifications_item_1"),
                        Locales.t("guide_notifications_item_2"),
                        Locales.t("guide_notifications_item_3")
                    )
                )
                GuideBulletList(
                    title = Locales.t("guide_notifications_reminders_title"),
                    items = listOf(
                        Locales.t("guide_notifications_reminder_1"),
                        Locales.t("guide_notifications_reminder_2"),
                        Locales.t("guide_notifications_reminder_3")
                    )
                )
                GuideParagraph(Locales.t("guide_notifications_p2"))
            }

            Divider()

            GuideExpandableSection(
                title = Locales.t("guide_section_security")
            ) {
                GuideParagraph(Locales.t("guide_security_p1"))
                GuideBulletList(
                    title = Locales.t("guide_common_in_this_section_the_user_can"),
                    items = listOf(
                        Locales.t("guide_security_item_1"),
                        Locales.t("guide_security_item_2"),
                        Locales.t("guide_security_item_3"),
                        Locales.t("guide_security_item_4")
                    )
                )
                GuideParagraph(Locales.t("guide_security_p2"))
                GuideParagraph(Locales.t("guide_security_p3"))
            }

            Divider()

            GuideExpandableSection(
                title = Locales.t("guide_section_backup")
            ) {
                GuideParagraph(Locales.t("guide_backup_p1"))
                GuideBulletList(
                    title = Locales.t("guide_common_the_user_can"),
                    items = listOf(
                        Locales.t("guide_backup_item_1"),
                        Locales.t("guide_backup_item_2"),
                        Locales.t("guide_backup_item_3"),
                        Locales.t("guide_backup_item_4")
                    )
                )
                GuideParagraph(Locales.t("guide_backup_p2"))
                GuideParagraph(Locales.t("guide_backup_p3"))
            }

            Divider()

            GuideExpandableSection(
                title = Locales.t("guide_section_side_menu")
            ) {
                GuideParagraph(Locales.t("guide_side_menu_p1"))
                GuideParagraph(Locales.t("guide_side_menu_p2"))
                GuideParagraph(Locales.t("guide_side_menu_p3"))
                GuideBulletList(
                    items = listOf(
                        Locales.t("guide_side_menu_item_1"),
                        Locales.t("guide_side_menu_item_2"),
                        Locales.t("guide_side_menu_item_3"),
                        Locales.t("guide_side_menu_item_4")
                    )
                )
                GuideBulletList(
                    title = Locales.t("guide_common_main_navigation_items"),
                    items = listOf(
                        Locales.t("guide_side_menu_nav_1"),
                        Locales.t("guide_side_menu_nav_2"),
                        Locales.t("guide_side_menu_nav_3"),
                        Locales.t("guide_side_menu_nav_4"),
                        Locales.t("guide_side_menu_nav_5"),
                        Locales.t("guide_side_menu_nav_6")
                    )
                )
            }

            Divider()

            GuideExpandableSection(
                title = Locales.t("guide_section_statistics")
            ) {
                GuideParagraph(Locales.t("guide_statistics_p1"))
                GuideBulletList(
                    title = Locales.t("guide_common_in_this_section_the_user_can"),
                    items = listOf(
                        Locales.t("guide_statistics_item_1"),
                        Locales.t("guide_statistics_item_2"),
                        Locales.t("guide_statistics_item_3"),
                        Locales.t("guide_statistics_item_4"),
                        Locales.t("guide_statistics_item_5"),
                        Locales.t("guide_statistics_item_6")
                    )
                )
                GuideParagraph(Locales.t("guide_statistics_p2"))

                // IMAGE RESOURCE:
                // File name: guide_statistics_image.png
                // Put here: composeApp/src/commonMain/composeResources/drawable/guide_statistics_image.png
                GuideImage(Res.drawable.guide_statistics_image)
            }

            Divider()

            GuideExpandableSection(
                title = Locales.t("guide_section_unpaid_appointments")
            ) {
                GuideParagraph(Locales.t("guide_unpaid_appointments_p1"))
                GuideParagraph(Locales.t("guide_unpaid_appointments_p2"))
                GuideBulletList(
                    title = Locales.t("guide_common_in_this_section_the_user_can"),
                    items = listOf(
                        Locales.t("guide_unpaid_appointments_item_1"),
                        Locales.t("guide_unpaid_appointments_item_2"),
                        Locales.t("guide_unpaid_appointments_item_3")
                    )
                )
                GuideParagraph(Locales.t("guide_unpaid_appointments_p3"))
            }

            Divider()

            GuideExpandableSection(
                title = Locales.t("guide_section_archive")
            ) {
                GuideParagraph(Locales.t("guide_archive_p1"))
                GuideBulletList(
                    title = Locales.t("guide_common_in_this_section_the_user_can"),
                    items = listOf(
                        Locales.t("guide_archive_item_1"),
                        Locales.t("guide_archive_item_2"),
                        Locales.t("guide_archive_item_3"),
                        Locales.t("guide_archive_item_4"),
                        Locales.t("guide_archive_item_5")
                    )
                )
                GuideParagraph(Locales.t("guide_archive_p2"))
            }

            Divider()

            GuideExpandableSection(
                title = Locales.t("guide_section_creating_new_appointment")
            ) {
                GuideParagraph(Locales.t("guide_creating_new_appointment_p1"))
                GuideNumberedList(
                    title = Locales.t("guide_common_basic_workflow"),
                    items = listOf(
                        Locales.t("guide_creating_new_appointment_step_1"),
                        Locales.t("guide_creating_new_appointment_step_2"),
                        Locales.t("guide_creating_new_appointment_step_3"),
                        Locales.t("guide_creating_new_appointment_step_4"),
                        Locales.t("guide_creating_new_appointment_step_5"),
                        Locales.t("guide_creating_new_appointment_step_6"),
                        Locales.t("guide_creating_new_appointment_step_7"),
                        Locales.t("guide_creating_new_appointment_step_8"),
                        Locales.t("guide_creating_new_appointment_step_9")
                    )
                )
                GuideParagraph(Locales.t("guide_creating_new_appointment_p2"))

                // IMAGE RESOURCE:
                // File name: guide_create_appointment_form_image.png
                // Put here: composeApp/src/commonMain/composeResources/drawable/guide_create_appointment_form_image.png
                GuideImage(Res.drawable.guide_create_appointment_form_image)
            }

            Divider()

            GuideExpandableSection(
                title = Locales.t("guide_section_client_data_autofill")
            ) {
                GuideParagraph(Locales.t("guide_client_data_autofill_p1"))
                GuideParagraph(Locales.t("guide_client_data_autofill_p2"))
                GuideParagraph(Locales.t("guide_client_data_autofill_p3"))
                GuideParagraph(Locales.t("guide_client_data_autofill_p4"))
            }

            Divider()

            GuideExpandableSection(
                title = Locales.t("guide_section_editing_appointment")
            ) {
                GuideParagraph(Locales.t("guide_editing_appointment_p1"))
                GuideBulletList(
                    title = Locales.t("guide_common_available_settings"),
                    items = listOf(
                        Locales.t("guide_editing_appointment_item_1"),
                        Locales.t("guide_editing_appointment_item_2"),
                        Locales.t("guide_editing_appointment_item_3"),
                        Locales.t("guide_editing_appointment_item_4"),
                        Locales.t("guide_editing_appointment_item_5"),
                        Locales.t("guide_editing_appointment_item_6")
                    )
                )
                GuideParagraph(Locales.t("guide_editing_appointment_p2"))
            }

            Divider()

            GuideExpandableSection(
                title = Locales.t("guide_section_rescheduling_appointment")
            ) {
                GuideParagraph(Locales.t("guide_rescheduling_appointment_p1"))
                GuideNumberedList(
                    title = Locales.t("guide_common_during_rescheduling_the_user"),
                    items = listOf(
                        Locales.t("guide_rescheduling_appointment_step_1"),
                        Locales.t("guide_rescheduling_appointment_step_2"),
                        Locales.t("guide_rescheduling_appointment_step_3"),
                        Locales.t("guide_rescheduling_appointment_step_4"),
                        Locales.t("guide_rescheduling_appointment_step_5")
                    )
                )
                GuideParagraph(Locales.t("guide_rescheduling_appointment_p2"))
            }

            Divider()

            GuideExpandableSection(
                title = Locales.t("guide_section_deleting_appointment")
            ) {
                GuideParagraph(Locales.t("guide_deleting_appointment_p1"))
                GuideParagraph(Locales.t("guide_deleting_appointment_p2"))
            }

            Divider()

            GuideExpandableSection(
                title = Locales.t("guide_section_account_deletion")
            ) {
                GuideParagraph(Locales.t("guide_account_deletion_p1"))
                GuideNumberedList(
                    title = Locales.t("guide_common_step_by_step_account_deletion_flow"),
                    items = listOf(
                        Locales.t("guide_account_deletion_step_1"),
                        Locales.t("guide_account_deletion_step_2"),
                        Locales.t("guide_account_deletion_step_3"),
                        Locales.t("guide_account_deletion_step_4"),
                        Locales.t("guide_account_deletion_step_5"),
                        Locales.t("guide_account_deletion_step_6"),
                        Locales.t("guide_account_deletion_step_7"),
                        Locales.t("guide_account_deletion_step_8")
                    )
                )
                GuideBulletList(
                    title = Locales.t("guide_common_reauthentication_depends_on_sign_in_method"),
                    items = listOf(
                        Locales.t("guide_account_deletion_reauth_1"),
                        Locales.t("guide_account_deletion_reauth_2"),
                        Locales.t("guide_account_deletion_reauth_3")
                    )
                )
                GuideParagraph(Locales.t("guide_account_deletion_p2"))

                // IMAGE RESOURCE:
                // File name: guide_delete_account_button_image.png
                // Put here: composeApp/src/commonMain/composeResources/drawable/guide_delete_account_button_image.png
                GuideImage(Res.drawable.guide_delete_account_button_image)

                // IMAGE RESOURCE:
                // File name: guide_delete_account_confirm_image.png
                // Put here: composeApp/src/commonMain/composeResources/drawable/guide_delete_account_confirm_image.png
                GuideImage(Res.drawable.guide_delete_account_confirm_image)
            }
        }
    }
}

@Composable
private fun GuideExpandableSection(
    title: String,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit
) {
    val fontScale = AppSettings.getFontScale()
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = (18 * fontScale).sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.onSurface,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = if (expanded) {
                    Icons.Default.KeyboardArrowUp
                } else {
                    Icons.Default.KeyboardArrowDown
                },
                contentDescription = null,
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.75f)
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun GuideImage(drawable: DrawableResource) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Image(
            painter = painterResource(drawable),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(14.dp)),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun GuideSectionTitle(text: String) {
    val fontScale = AppSettings.getFontScale()
    Text(
        text = text,
        fontSize = (18 * fontScale).sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colors.onSurface
    )
}

@Composable
private fun GuideMiniSectionTitle(text: String) {
    val fontScale = AppSettings.getFontScale()
    Text(
        text = text,
        fontSize = (15 * fontScale).sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colors.onSurface.copy(alpha = 0.90f)
    )
}

@Composable
private fun GuideParagraph(text: String) {
    val fontScale = AppSettings.getFontScale()
    Text(
        text = text,
        fontSize = (14 * fontScale).sp,
        lineHeight = (20 * fontScale).sp,
        color = MaterialTheme.colors.onSurface.copy(alpha = 0.82f)
    )
}

@Composable
private fun GuideBulletList(
    title: String? = null,
    items: List<String>
) {
    val fontScale = AppSettings.getFontScale()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                fontSize = (14 * fontScale).sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.90f)
            )
        }

        items.forEach { item ->
            Text(
                text = "• $item",
                fontSize = (14 * fontScale).sp,
                lineHeight = (20 * fontScale).sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.82f)
            )
        }
    }
}

@Composable
private fun GuideNumberedList(
    title: String? = null,
    items: List<String>
) {
    val fontScale = AppSettings.getFontScale()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                fontSize = (14 * fontScale).sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.90f)
            )
        }

        items.forEachIndexed { index, item ->
            Text(
                text = "${index + 1}. $item",
                fontSize = (14 * fontScale).sp,
                lineHeight = (20 * fontScale).sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.82f)
            )
        }
    }
}