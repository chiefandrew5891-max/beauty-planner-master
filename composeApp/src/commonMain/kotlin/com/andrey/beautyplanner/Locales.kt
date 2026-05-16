package com.andrey.beautyplanner

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object Locales {

    var currentLanguage by mutableStateOf(
        AppSettings.languageCodes[AppSettings.selectedLanguage] ?: "ru"
    )

    private val strings = mapOf(
        "ru" to mapOf(
            "app_name" to "Beauty Planner",
            "nav_main" to "Главная",
            "nav_settings" to "Настройки",
            "nav_day" to "Детали дня",
            "nav_menu" to "Меню",
            "nav_stats" to "Статистика",
            "nav_feedback" to "Обратная связь",
            "save" to "Сохранить",
            "cancel" to "Отмена",
            "close" to "Закрыть",
            "confirm" to "Подтвердить",
            "yes" to "Да",

            "client_name" to "Имя клиента",
            "phone" to "Телефон",
            "service" to "Процедура",
            "price" to "Стоимость",
            "free" to "Свободно",

            // Services
            "service_gel_polish" to "Гель-лак",
            "service_gel_strengthening" to "Укрепление гелем",
            "service_nail_extensions" to "Наращивание",
            "service_lash_extensions" to "Наращивание ресниц",

            // ✅ added
            "service_correction" to "Коррекция",
            "service_repair" to "Ремонт",
            "service_other" to "Другое",

            "all_appointments_list" to "Список всех записей:",
            "no_appointments" to "Нет записей",

            "upcoming_appointments_list" to "Предстоящие записи:",
            "no_upcoming_appointments" to "Нет предстоящих записей",
            "view_appointment_title" to "Просмотр записи",
            "quick_actions_title" to "Быстрые действия",

            "language_label" to "Язык",
            "theme_label" to "Тема",
            "font_size_label" to "Размер шрифта",
            "theme_light" to "Светлая",
            "theme_dark" to "Тёмная",
            "font_small" to "Мелкий",
            "font_medium" to "Средний",
            "font_large" to "Крупный",

            "backup_section" to "Резервное копирование",
            "export_db" to "Экспорт",
            "import_db" to "Импорт",
            "backup_file_name" to "Имя файла",
            "backup_export_name_hint" to "Введите имя файла резервной копии. Расширение .json добавится автоматически.",
            "backup_extension_note" to "Формат: .json (подходит для Android и iOS)",
            "backup_import_confirm_text" to "Импортировать выбранный файл? Текущие записи будут заменены.",
            "import_btn" to "Импорт",
            "import_invalid_json" to "Не удалось импортировать: проверьте JSON (пустой или неверный формат).",
            "backup_import_error_empty" to "Файл пустой или не удалось прочитать содержимое.",
            "backup_import_error_read" to "Не удалось прочитать файл.",
            "backup_import_error_no_activity" to "Не удалось открыть проводник (нет Activity).",
            "backup_import_error_no_vc" to "Не удалось открыть проводник.",

            // Support phone edit flow
            "support_phone_edit" to "Изменить",
            "support_phone_save" to "Сохранить",
            "support_phone_edit_confirm_title" to "Изменение номера",
            "support_phone_edit_confirm_text" to "Вы уверены, что хотите изменить номер службы поддержки?",
            "support_phone_edit_confirm_yes" to "Да, изменить",

            "privacy_policy" to "Политика конфиденциальности",

            "delete_title" to "Удаление",
            "delete_btn" to "Удалить",
            "delete_confirm_prefix" to "Удалить запись",
            "delete_confirm_at" to "на",
            "continue_question" to "Продолжить?",

            "transfer_appt" to "Перенести запись",
            "transfer_title" to "Перенос записи",
            "transfer_choose_time" to "Выберите время",
            "transfer_confirm" to "Перенести",

            "transfer_conflict_title" to "Время занято",
            "transfer_conflict_text" to "Это время уже занято другой записью. Вы хотите перенести текущую запись сюда и затем переназначить вторую запись?",
            "transfer_conflict_a" to "Переносим",
            "transfer_conflict_b" to "Занято",
            "transfer_agree" to "Согласовать",

            "reschedule_title_for" to "Переназначить запись для",
            "reschedule_choose_time" to "Выберите новое время",
            "reschedule_confirm" to "Сохранить",

            "start_time" to "Начало",
            "end_time" to "Конец",
            "duration_hours" to "Длительность (ч)",

            "notifications_section" to "Уведомления",
            "notifications_enabled" to "Включить уведомления",
            "notif_sound_label" to "Звук уведомления",
            "notif_sound_default" to "По умолчанию",
            "notif_sound_silent" to "Без звука",
            "reminders_when" to "Напоминать за:",
            "remind_days" to "Дни",
            "remind_hours" to "Часы",
            "remind_summary" to "Итог",
            "remind_off" to "Выключено",

            // Support / feedback
            "support_section" to "Служба поддержки",
            "support_phone_label" to "Телефон поддержки",
            "support_phone_hint" to "Введите номер (например: +39 123 456 789 или +7 999 000-00-00)",
            "support_phone_empty" to "Не указан",
            "support_feedback_text" to "Если у вас неполадки — позвоните в поддержку по номеру ниже.",
            "support_call" to "Позвонить",
            // User name satting
            "user_name_label" to "Имя пользователя",
            "user_name_hint" to "Введите имя",
            "contacts_permission_hint" to "Для автопоиска по контактам нужно разрешение на доступ к контактам. Его можно выдать в настройках устройства.",
            // Stats
            "stats_period_day" to "День",
            "stats_period_week" to "Неделя",
            "stats_period_month" to "Месяц",
            "stats_period_year" to "Год",
            "stats_range" to "Период",
            "stats_revenue" to "Выручка",
            "stats_count" to "Количество записей",
            "stats_hours" to "Отработано часов",
            "stats_top_services" to "Процедуры",
            "stats_procedures_done" to "Проведено",
            "stats_empty" to "Нет данных за выбранный период",
            "stats_unknown_service" to "Без названия",
            "stats_filters" to "Фильтры",
            "stats_period_custom" to "Период",
            "stats_custom_range" to "Выбор диапазона",
            "stats_date_from" to "От",
            "stats_date_to" to "До",
            "stats_pick_start_date" to "Выберите начальную дату",
            "stats_pick_end_date" to "Выберите конечную дату",

            // Completed Record Card
            "view" to "Просмотр",
            "edit" to "Изменить",
            "edit_appointment_title" to "Изменение записи",
            "edit_appointment_confirm" to "Вы уверены, что хотите изменить эту запись?",
            "conflict_time_title" to "Конфликт времени",
            "shift" to "Сдвинуть",

            // Content descriptions
            "cd_menu" to "Меню",
            "cd_back" to "Назад",
            "cd_settings" to "Настройки",

            // ✅ Appointment status (NEW)
            "appt_status_label" to "Статус",
            "appt_status_waiting" to "Ожидается",
            "appt_status_in_progress" to "Выполняется",
            "appt_status_done" to "Выполнено",
            "appt_status_canceled" to "Отменено",

            // Currency
            "currency_eur" to "€",
            "splash_for" to "для",

            // --------- Security / PIN ---------
            "security_section" to "Безопасность",
            "pin_enabled" to "Запрашивать PIN",
            "pin_set" to "Установить PIN",
            "pin_change" to "Изменить PIN",
            "pin_remove" to "Удалить PIN",
            "pin_label" to "PIN (4–8 цифр)",
            "pin_invalid_format" to "Введите PIN из 4–8 цифр",
            "pin_required" to "Требуется PIN",
            "pin_wrong" to "Неверный PIN",

            "export_requires_pin" to "Для экспорта требуется PIN.",
            "import_requires_pin" to "Для импорта требуется PIN.",

            "clear_db" to "Очистить базу данных",
            "clear_db_title" to "Очистка базы",
            "clear_db_requires_pin" to "Чтобы очистить базу данных, введите PIN.",
            "clear_db_warning_backup" to "Рекомендуем сначала сделать резервную копию базы данных. Сделать копию сейчас?",
            "clear_db_make_backup" to "Сделать копию",
            "clear_db_skip_backup" to "Очистить без копии",
            "clear_db_confirm" to "Вы уверены? Все записи будут удалены без возможности восстановления.",

            "unlock_title" to "Введите PIN",
            "unlock_text" to "Для доступа к приложению требуется PIN.",

            // months + weekdays
            "month_jan" to "Январь", "month_feb" to "Февраль", "month_mar" to "Март",
            "month_apr" to "Апрель", "month_may" to "Май", "month_jun" to "Июнь",
            "month_jul" to "Июль", "month_aug" to "Август", "month_sep" to "Сентябрь",
            "month_oct" to "Октябрь", "month_nov" to "Ноябрь", "month_dec" to "Декабрь",

            "month_jan_gen" to "января",
            "month_feb_gen" to "февраля",
            "month_mar_gen" to "марта",
            "month_apr_gen" to "апреля",
            "month_may_gen" to "мая",
            "month_jun_gen" to "июня",
            "month_jul_gen" to "июля",
            "month_aug_gen" to "августа",
            "month_sep_gen" to "сентября",
            "month_oct_gen" to "октября",
            "month_nov_gen" to "ноября",
            "month_dec_gen" to "декабря",

            "mon" to "Пн", "tue" to "Вт", "wed" to "Ср", "thu" to "Чт", "fri" to "Пт", "sat" to "Сб", "sun" to "Вс"
        ),

        "en" to mapOf(
            "app_name" to "Beauty Planner",
            "nav_main" to "Main",
            "nav_settings" to "Settings",
            "nav_day" to "Day Details",
            "nav_menu" to "Menu",
            "nav_stats" to "Statistics",
            "nav_feedback" to "Support",
            "save" to "Save",
            "cancel" to "Cancel",
            "close" to "Close",
            "confirm" to "Confirm",
            "yes" to "Yes",

            "client_name" to "Client Name",
            "phone" to "Phone",
            "service" to "Service",
            "price" to "Price",
            "free" to "Free",

            "service_gel_polish" to "Gel polish",
            "service_gel_strengthening" to "Gel strengthening",
            "service_nail_extensions" to "Nail extensions",
            "service_lash_extensions" to "Lash extensions",

            "service_correction" to "Correction",
            "service_repair" to "Repair",
            "service_other" to "Other",

            "all_appointments_list" to "All appointments list:",
            "no_appointments" to "No appointments",

            "upcoming_appointments_list" to "Upcoming appointments:",
            "no_upcoming_appointments" to "No upcoming appointments",
            "view_appointment_title" to "Appointment details",
            "quick_actions_title" to "Quick actions",

            "language_label" to "Language",
            "theme_label" to "Theme",
            "font_size_label" to "Font Size",
            "theme_light" to "Light",
            "theme_dark" to "Dark",
            "font_small" to "Small",
            "font_medium" to "Medium",
            "font_large" to "Large",

            "backup_section" to "Backup",
            "export_db" to "Export",
            "import_db" to "Import",
            "backup_file_name" to "File name",
            "backup_export_name_hint" to "Enter backup file name. .json extension will be added automatically.",
            "backup_extension_note" to "Format: .json (works on Android and iOS)",
            "backup_import_confirm_text" to "Import selected file? Current appointments will be replaced.",
            "import_btn" to "Import",
            "import_invalid_json" to "Import failed: please check the JSON (empty or invalid format).",
            "backup_import_error_empty" to "File is empty or cannot be read.",
            "backup_import_error_read" to "Cannot read file.",
            "backup_import_error_no_activity" to "Cannot open file picker (no Activity).",
            "backup_import_error_no_vc" to "Cannot open file picker.",

            "support_phone_edit" to "Edit",
            "support_phone_save" to "Save",
            "support_phone_edit_confirm_title" to "Edit phone",
            "support_phone_edit_confirm_text" to "Are you sure you want to change support phone number?",
            "support_phone_edit_confirm_yes" to "Yes",

            "privacy_policy" to "Privacy Policy",

            "delete_title" to "Delete",
            "delete_btn" to "Delete",
            "delete_confirm_prefix" to "Delete appointment for",
            "delete_confirm_at" to "at",
            "continue_question" to "Continue?",

            "transfer_appt" to "Transfer appointment",
            "transfer_title" to "Transfer appointment",
            "transfer_choose_time" to "Choose time",
            "transfer_confirm" to "Transfer",

            "transfer_conflict_title" to "Time is busy",
            "transfer_conflict_text" to "This time is already booked. Do you want to move the current appointment here and then reschedule the other one?",
            "transfer_conflict_a" to "Moving",
            "transfer_conflict_b" to "Booked",
            "transfer_agree" to "Agree",

            "reschedule_title_for" to "Reschedule appointment for",
            "reschedule_choose_time" to "Choose new time",
            "reschedule_confirm" to "Save",

            "start_time" to "Start time",
            "end_time" to "End",
            "duration_hours" to "Duration (h)",

            "notifications_section" to "Notifications",
            "notifications_enabled" to "Enable notifications",
            "notif_sound_label" to "Notification sound",
            "notif_sound_default" to "Default",
            "notif_sound_silent" to "Silent",
            "reminders_when" to "Remind before:",
            "remind_days" to "Days",
            "remind_hours" to "Hours",
            "remind_summary" to "Summary",
            "remind_off" to "Off",

            "support_section" to "Support",
            "support_phone_label" to "Support phone",
            "support_phone_hint" to "Enter phone number (e.g. +39 123 456 789)",
            "support_phone_empty" to "Not set",
            "support_feedback_text" to "If something is not working, call support using the number below.",
            "support_call" to "Call",
            // User name satting
            "user_name_label" to "User name",
            "user_name_hint" to "Enter your name",
            "contacts_permission_hint" to "Contact access permission is required for autocomplete. You can enable it in device settings.",

            "stats_period_day" to "Day",
            "stats_period_week" to "Week",
            "stats_period_month" to "Month",
            "stats_period_year" to "Year",
            "stats_range" to "Range",
            "stats_revenue" to "Revenue",
            "stats_count" to "Appointments",
            "stats_hours" to "Hours worked",
            "stats_top_services" to "Services",
            "stats_procedures_done" to "Done",
            "stats_empty" to "No data for selected period",
            "stats_unknown_service" to "Unnamed",
            "stats_filters" to "Filters",
            "stats_period_custom" to "Custom",
            "stats_custom_range" to "Select range",
            "stats_date_from" to "From",
            "stats_date_to" to "To",
            "stats_pick_start_date" to "Select start date",
            "stats_pick_end_date" to "Select end date",

            "view" to "View",
            "edit" to "Edit",
            "edit_appointment_title" to "Edit appointment",
            "edit_appointment_confirm" to "Are you sure you want to edit this appointment?",
            "conflict_time_title" to "Time conflict",
            "shift" to "Shift",

            "cd_menu" to "Menu",
            "cd_back" to "Back",
            "cd_settings" to "Settings",

            // ✅ Appointment status (NEW)
            "appt_status_label" to "Status",
            "appt_status_waiting" to "Waiting",
            "appt_status_in_progress" to "In progress",
            "appt_status_done" to "Done",
            "appt_status_canceled" to "Canceled",

            "currency_eur" to "€",
            "splash_for" to "for",

            "security_section" to "Security",
            "pin_enabled" to "Require PIN",
            "pin_set" to "Set PIN",
            "pin_change" to "Change PIN",
            "pin_remove" to "Remove PIN",
            "pin_label" to "PIN (4–8 digits)",
            "pin_invalid_format" to "Enter a 4–8 digit PIN",
            "pin_required" to "PIN required",
            "pin_wrong" to "Wrong PIN",

            "export_requires_pin" to "PIN is required to export.",
            "import_requires_pin" to "PIN is required to import.",

            "clear_db" to "Clear database",
            "clear_db_title" to "Clear database",
            "clear_db_requires_pin" to "To clear the database, enter PIN.",
            "clear_db_warning_backup" to "We recommend creating a backup first. Create a backup now?",
            "clear_db_make_backup" to "Create backup",
            "clear_db_skip_backup" to "Clear without backup",
            "clear_db_confirm" to "Are you sure? All appointments will be deleted and cannot be restored.",

            "unlock_title" to "Enter PIN",
            "unlock_text" to "PIN is required to access the app.",

            "month_jan" to "January", "month_feb" to "February", "month_mar" to "March",
            "month_apr" to "April", "month_may" to "May", "month_jun" to "June",
            "month_jul" to "July", "month_aug" to "August", "month_sep" to "September",
            "month_oct" to "October", "month_nov" to "November", "month_dec" to "December",

            "month_jan_gen" to "January",
            "month_feb_gen" to "February",
            "month_mar_gen" to "March",
            "month_apr_gen" to "April",
            "month_may_gen" to "May",
            "month_jun_gen" to "June",
            "month_jul_gen" to "July",
            "month_aug_gen" to "August",
            "month_sep_gen" to "September",
            "month_oct_gen" to "October",
            "month_nov_gen" to "November",
            "month_dec_gen" to "December",

            "mon" to "Mon", "tue" to "Tue", "wed" to "Wed", "thu" to "Thu", "fri" to "Fri", "sat" to "Sat", "sun" to "Sun"
        ),

        // Итальянский и украинский блоки оставляю как у тебя, но добавляю статусы в конец каждого.
        "it" to mapOf(
            "app_name" to "Beauty Planner",
            "nav_main" to "Home",
            "nav_settings" to "Impostazioni",
            "nav_day" to "Dettagli",
            "nav_menu" to "Menu",
            "nav_stats" to "Statistiche",
            "nav_feedback" to "Supporto",
            "save" to "Salva",
            "cancel" to "Annulla",
            "close" to "Chiudi",
            "confirm" to "Conferma",
            "yes" to "Sì",

            "client_name" to "Nome cliente",
            "phone" to "Telefono",
            "service" to "Procedura",
            "price" to "Prezzo",
            "free" to "Libero",

            "service_gel_polish" to "Smalto gel",
            "service_gel_strengthening" to "Rinforzo con gel",
            "service_nail_extensions" to "Ricostruzione unghie",
            "service_lash_extensions" to "Extension ciglia",

            "service_correction" to "Correzione",
            "service_repair" to "Riparazione",
            "service_other" to "Altro",

            "all_appointments_list" to "Elenco appuntamenti:",
            "no_appointments" to "Nessun appuntamento",

            "upcoming_appointments_list" to "Prossimi appuntamenti:",
            "no_upcoming_appointments" to "Nessun appuntamento futuro",
            "view_appointment_title" to "Dettagli appuntamento",
            "quick_actions_title" to "Azioni rapide",

            "language_label" to "Lingua",
            "theme_label" to "Tema",
            "font_size_label" to "Dimensione carattere",
            "theme_light" to "Chiaro",
            "theme_dark" to "Scuro",
            "font_small" to "Piccolo",
            "font_medium" to "Medio",
            "font_large" to "Grande",

            "backup_section" to "Backup",
            "export_db" to "Esporta",
            "import_db" to "Importa",
            "backup_file_name" to "Nome file",
            "backup_export_name_hint" to "Inserisci il nome del file di backup. L'estensione .json verrà aggiunta automaticamente.",
            "backup_extension_note" to "Formato: .json (Android e iOS)",
            "backup_import_confirm_text" to "Importare il file selezionato? Gli appuntamenti attuali verranno sostituiti.",
            "import_btn" to "Importa",
            "import_invalid_json" to "Importazione non riuscita: controlla il JSON (vuoto o formato non valido).",
            "backup_import_error_empty" to "Il file è vuoto o non può essere letto.",
            "backup_import_error_read" to "Impossibile leggere il file.",
            "backup_import_error_no_activity" to "Impossibile aprire il selettore file.",
            "backup_import_error_no_vc" to "Impossibile aprire il selettore file.",

            "support_phone_edit" to "Modifica",
            "support_phone_save" to "Salva",
            "support_phone_edit_confirm_title" to "Modifica numero",
            "support_phone_edit_confirm_text" to "Vuoi modificare il numero di supporto?",
            "support_phone_edit_confirm_yes" to "Sì",

            "privacy_policy" to "Privacy Policy",

            "delete_title" to "Elimina",
            "delete_btn" to "Elimina",
            "delete_confirm_prefix" to "Eliminare appuntamento per",
            "delete_confirm_at" to "alle",
            "continue_question" to "Continuare?",

            "transfer_appt" to "Sposta appuntamento",
            "transfer_title" to "Sposta appuntamento",
            "transfer_choose_time" to "Scegli l'orario",
            "transfer_confirm" to "Sposta",

            "transfer_conflict_title" to "Orario occupato",
            "transfer_conflict_text" to "Questo orario è già prenotato. Vuoi spostare l'appuntamento qui e poi riprogrammare l'altro?",
            "transfer_conflict_a" to "Spostiamo",
            "transfer_conflict_b" to "Occupato",
            "transfer_agree" to "Concorda",

            "reschedule_title_for" to "Riprogrammare appuntamento per",
            "reschedule_choose_time" to "Scegli nuovo orario",
            "reschedule_confirm" to "Salva",

            "start_time" to "Ora inizio",
            "end_time" to "Fine",
            "duration_hours" to "Durata (h)",

            "notifications_section" to "Notifiche",
            "notifications_enabled" to "Abilita notifiche",
            "notif_sound_label" to "Suono notifica",
            "notif_sound_default" to "Predefinito",
            "notif_sound_silent" to "Silenzioso",
            "reminders_when" to "Ricorda prima:",
            "remind_days" to "Giorni",
            "remind_hours" to "Ore",
            "remind_summary" to "Riepilogo",
            "remind_off" to "Disattivato",

            "support_section" to "Supporto",
            "support_phone_label" to "Telefono supporto",
            "support_phone_hint" to "Inserisci numero (es. +39 123 456 789)",
            "support_phone_empty" to "Non impostato",
            "support_feedback_text" to "Se qualcosa non funziona, chiama il supporto usando il numero qui sotto.",
            "support_call" to "Chiama",
            // User name satting
            "user_name_label" to "Nome utent",
            "user_name_hint" to "Inserisci il tuo nome",
            "contacts_permission_hint" to "Per la ricerca automatica serve il permesso di accesso ai contatti. Puoi abilitarlo nelle impostazioni del dispositivo.",

            "stats_period_day" to "Giorno",
            "stats_period_week" to "Settimana",
            "stats_period_month" to "Mese",
            "stats_period_year" to "Anno",
            "stats_range" to "Periodo",
            "stats_revenue" to "Incasso",
            "stats_count" to "Appuntamenti",
            "stats_hours" to "Ore lavorate",
            "stats_top_services" to "Servizi",
            "stats_procedures_done" to "Eseguiti",
            "stats_empty" to "Nessun dato per il periodo selezionato",
            "stats_unknown_service" to "Senza nome",
            "stats_filters" to "Filtri",
            "stats_period_custom" to "Personalizzato",
            "stats_custom_range" to "Seleziona intervallo",
            "stats_date_from" to "Da",
            "stats_date_to" to "A",
            "stats_pick_start_date" to "Seleziona la data iniziale",
            "stats_pick_end_date" to "Seleziona la data finale",

            "view" to "Visualizza",
            "edit" to "Modifica",
            "edit_appointment_title" to "Modifica appuntamento",
            "edit_appointment_confirm" to "Vuoi davvero modificare questo appuntamento?",
            "conflict_time_title" to "Conflitto di orario",
            "shift" to "Sposta",

            "cd_menu" to "Menu",
            "cd_back" to "Indietro",
            "cd_settings" to "Impostazioni",

            "currency_eur" to "€",
            "splash_for" to "per",

            "security_section" to "Sicurezza",
            "pin_enabled" to "Richiedi PIN",
            "pin_set" to "Imposta PIN",
            "pin_change" to "Cambia PIN",
            "pin_remove" to "Rimuovi PIN",
            "pin_label" to "PIN (4–8 cifre)",
            "pin_invalid_format" to "Inserisci un PIN di 4–8 cifre",
            "pin_required" to "PIN richiesto",
            "pin_wrong" to "PIN errato",

            "export_requires_pin" to "È richiesto il PIN per esportare.",
            "import_requires_pin" to "È richiesto il PIN per importare.",

            "clear_db" to "Svuota database",
            "clear_db_title" to "Svuota database",
            "clear_db_requires_pin" to "Per svuotare il database inserisci il PIN.",
            "clear_db_warning_backup" to "Consigliamo di creare prima un backup. Creare un backup ora?",
            "clear_db_make_backup" to "Crea backup",
            "clear_db_skip_backup" to "Svuota senza backup",
            "clear_db_confirm" to "Sei sicuro? Tutti gli appuntamenti verranno eliminati e non potranno essere ripristinati.",

            "unlock_title" to "Inserisci PIN",
            "unlock_text" to "È richiesto un PIN per accedere all'app.",

            "month_jan" to "Gennaio", "month_feb" to "Febbraio", "month_mar" to "Marzo",
            "month_apr" to "Aprile", "month_may" to "Maggio", "month_jun" to "Giugno",
            "month_jul" to "Luglio", "month_aug" to "Agosto", "month_sep" to "Settembre",
            "month_oct" to "Ottobre", "month_nov" to "Novembre", "month_dec" to "Dicembre",

            "month_jan_gen" to "Gennaio",
            "month_feb_gen" to "Febbraio",
            "month_mar_gen" to "Marzo",
            "month_apr_gen" to "Aprile",
            "month_may_gen" to "Maggio",
            "month_jun_gen" to "Giugno",
            "month_jul_gen" to "Luglio",
            "month_aug_gen" to "Agosto",
            "month_sep_gen" to "Settembre",
            "month_oct_gen" to "Ottobre",
            "month_nov_gen" to "Novembre",
            "month_dec_gen" to "Dicembre",

            "mon" to "Lun", "tue" to "Mar", "wed" to "Mer", "thu" to "Gio", "fri" to "Ven", "sat" to "Sab", "sun" to "Dom",

            // ✅ Appointment status (NEW)
            "appt_status_label" to "Stato",
            "appt_status_waiting" to "In attesa",
            "appt_status_in_progress" to "In corso",
            "appt_status_done" to "Completato",
            "appt_status_canceled" to "Annullato"
        ),

        "uk" to mapOf(
            "app_name" to "Beauty Planner",
            "nav_main" to "Головна",
            "nav_settings" to "Налаштування",
            "nav_day" to "Деталі дня",
            "nav_menu" to "Меню",
            "nav_stats" to "Статистика",
            "nav_feedback" to "Звʼязок",
            "save" to "Зберегти",
            "cancel" to "Скасувати",
            "close" to "Закрити",
            "confirm" to "Підтвердити",
            "yes" to "Так",

            "client_name" to "Ім'я клієнта",
            "phone" to "Телефон",
            "service" to "Процедура",
            "price" to "Вартість",
            "free" to "Вільно",

            "service_gel_polish" to "Гель-лак",
            "service_gel_strengthening" to "Зміцнення гелем",
            "service_nail_extensions" to "Нарощування",
            "service_lash_extensions" to "Нарощування вій",

            "service_correction" to "Корекція",
            "service_repair" to "Ремонт",
            "service_other" to "Інше",

            "all_appointments_list" to "Список всіх записів:",
            "no_appointments" to "Немає записів",

            "upcoming_appointments_list" to "Майбутні записи:",
            "no_upcoming_appointments" to "Немає майбутніх записів",
            "view_appointment_title" to "Перегляд запису",
            "quick_actions_title" to "Швидкі дії",

            "language_label" to "Мова",
            "theme_label" to "Тема",
            "font_size_label" to "Розмір шрифту",
            "theme_light" to "Світла",
            "theme_dark" to "Темна",
            "font_small" to "Дрібний",
            "font_medium" to "Середній",
            "font_large" to "Великий",

            "backup_section" to "Резервне копіювання",
            "export_db" to "Експорт",
            "import_db" to "Імпорт",
            "backup_file_name" to "Назва файлу",
            "backup_export_name_hint" to "Введіть наз��у файлу резервної копії. Розширення .json додасться автоматично.",
            "backup_extension_note" to "Формат: .json (Android та iOS)",
            "backup_import_confirm_text" to "Імпортувати вибраний файл? Поточні записи буде замінено.",
            "import_btn" to "Імпорт",
            "import_invalid_json" to "Не вдалося імпортувати: перевірте JSON (порожній або невірний формат).",
            "backup_import_error_empty" to "Файл порожній або не вдалося прочитати.",
            "backup_import_error_read" to "Не вдалося прочитати файл.",
            "backup_import_error_no_activity" to "Не вдалося відкрити провідник.",
            "backup_import_error_no_vc" to "Не вдалося відкрити провідник.",

            "support_phone_edit" to "Змінити",
            "support_phone_save" to "Зберегти",
            "support_phone_edit_confirm_title" to "Зміна номера",
            "support_phone_edit_confirm_text" to "Ви впевнені, що хочете змінити номер підтримки?",
            "support_phone_edit_confirm_yes" to "Так",

            "privacy_policy" to "Політика конфіденційності",

            "delete_title" to "Видалення",
            "delete_btn" to "Видалити",
            "delete_confirm_prefix" to "Видалити запис",
            "delete_confirm_at" to "о",
            "continue_question" to "Продовжити?",

            "transfer_appt" to "Перенести запис",
            "transfer_title" to "Перенесення запису",
            "transfer_choose_time" to "Оберіть час",
            "transfer_confirm" to "Перенести",

            "transfer_conflict_title" to "Час зайнято",
            "transfer_conflict_text" to "Цей час уже зайнятий. Перенести запис сюди і потім переназначити інший?",
            "transfer_conflict_a" to "Переносимо",
            "transfer_conflict_b" to "Зайнято",
            "transfer_agree" to "Погодити",

            "reschedule_title_for" to "Пер��назначити запис для",
            "reschedule_choose_time" to "Оберіть новий час",
            "reschedule_confirm" to "Зберегти",

            "start_time" to "Початок",
            "end_time" to "Кінець",
            "duration_hours" to "Тривалість (год)",

            "notifications_section" to "Сповіщення",
            "notifications_enabled" to "Увімкнути сповіщення",
            "notif_sound_label" to "Звук сповіщення",
            "notif_sound_default" to "За замовчуванням",
            "notif_sound_silent" to "Без звуку",
            "reminders_when" to "Нагадувати за:",
            "remind_days" to "Дні",
            "remind_hours" to "Години",
            "remind_summary" to "Підсумок",
            "remind_off" to "Вимкнено",

            "support_section" to "Підтримка",
            "support_phone_label" to "Телефон підтримки",
            "support_phone_hint" to "Введіть номер (наприклад: +39 123 456 789)",
            "support_phone_empty" to "Не вказано",
            "support_feedback_text" to "Якщо щось не працює — зателефонуйте в підтримку за номером нижче.",
            "support_call" to "Подзвонити",
            // User name satting
            "user_name_label" to "Ім’я користувача",
            "user_name_hint" to "Введіть ім’я",
            "contacts_permission_hint" to "Для автопошуку потрібен дозвіл на доступ до контактів. Його можна надати в налаштуваннях пристрою.",

            "stats_period_day" to "День",
            "stats_period_week" to "Тиждень",
            "stats_period_month" to "Місяць",
            "stats_period_year" to "Рік",
            "stats_range" to "Період",
            "stats_revenue" to "Виручка",
            "stats_count" to "Кількість записів",
            "stats_hours" to "Відпрацьовано годин",
            "stats_top_services" to "Процедури",
            "stats_procedures_done" to "Проведено",
            "stats_empty" to "Немає даних за вибраний період",
            "stats_unknown_service" to "Без назви",
            "stats_filters" to "Фільтри",
            "stats_period_custom" to "Власний",
            "stats_custom_range" to "Вибір діапазону",
            "stats_date_from" to "Від",
            "stats_date_to" to "До",
            "stats_pick_start_date" to "Оберіть початкову дату",
            "stats_pick_end_date" to "Оберіть кінцеву дату",

            "view" to "Перегляд",
            "edit" to "Змінити",
            "edit_appointment_title" to "Зміна запису",
            "edit_appointment_confirm" to "Ви впевнені, що хочете змінити цей запис?",
            "conflict_time_title" to "Конфлікт часу",
            "shift" to "Зсунути",

            "cd_menu" to "Меню",
            "cd_back" to "Назад",
            "cd_settings" to "Налаштування",

            // ✅ Appointment status (NEW)
            "appt_status_label" to "Статус",
            "appt_status_done" to "Виконано",
            "appt_status_waiting" to "Очікується",
            "appt_status_in_progress" to "Виконується",
            "appt_status_canceled" to "Скасовано",

            "currency_eur" to "€",
            "splash_for" to "для",

            "security_section" to "Безпека",
            "pin_enabled" to "Запитувати PIN",
            "pin_set" to "Встановити PIN",
            "pin_change" to "Змінити PIN",
            "pin_remove" to "Видалити PIN",
            "pin_label" to "PIN (4–8 цифр)",
            "pin_invalid_format" to "Введіть PIN з 4–8 цифр",
            "pin_required" to "Потрібен PIN",
            "pin_wrong" to "Невірний PIN",

            "export_requires_pin" to "Для експорту потрібен PIN.",
            "import_requires_pin" to "Для імпорту потрібен PIN.",

            "clear_db" to "Очистити базу даних",
            "clear_db_title" to "Очищення бази",
            "clear_db_requires_pin" to "Щоб очистити базу даних, введіть PIN.",
            "clear_db_warning_backup" to "Рекомендуємо спочатку зробити резервну копію. Зробити копію зараз?",
            "clear_db_make_backup" to "Зробити копію",
            "clear_db_skip_backup" to "Очистити без копії",
            "clear_db_confirm" to "Ви впевнені? Усі записи буде видалено без можливості відновлення.",

            "unlock_title" to "Введіть PIN",
            "unlock_text" to "Для доступу до застосунку потрібен PIN.",

            "month_jan" to "��ічень", "month_feb" to "Лютий", "month_mar" to "Березень",
            "month_apr" to "Квітень", "month_may" to "Травень", "month_jun" to "Червень",
            "month_jul" to "Липень", "month_aug" to "Серпень", "month_sep" to "Вересень",
            "month_oct" to "Жовтень", "month_nov" to "Листопад", "month_dec" to "Грудень",

            "month_jan_gen" to "січня",
            "month_feb_gen" to "лютого",
            "month_mar_gen" to "березня",
            "month_apr_gen" to "квітня",
            "month_may_gen" to "травня",
            "month_jun_gen" to "червня",
            "month_jul_gen" to "липня",
            "month_aug_gen" to "серпня",
            "month_sep_gen" to "вересня",
            "month_oct_gen" to "жовтня",
            "month_nov_gen" to "листопада",
            "month_dec_gen" to "грудня",

            "mon" to "Пн", "tue" to "Вт", "wed" to "Ср", "thu" to "Чт", "fri" to "Пт", "sat" to "Сб", "sun" to "Нд"
        )
    )

    fun t(key: String): String {
        val langCode = AppSettings.languageCodes[AppSettings.selectedLanguage] ?: "ru"
        return strings[langCode]?.get(key) ?: strings["en"]?.get(key) ?: key
    }

    fun daysCount(n: Int): String {
        val langCode = AppSettings.languageCodes[AppSettings.selectedLanguage] ?: "ru"
        return when (langCode) {
            "ru" -> "$n ${ruPlural(n, "день", "дня", "дней")}"
            "uk" -> "$n ${ukPlural(n, "день", "дні", "днів")}"
            "it" -> if (n == 1) "$n giorno" else "$n giorni"
            else -> if (n == 1) "$n day" else "$n days"
        }
    }

    fun hoursCount(n: Int): String {
        val langCode = AppSettings.languageCodes[AppSettings.selectedLanguage] ?: "ru"
        return when (langCode) {
            "ru" -> "$n ${ruPlural(n, "час", "часа", "часов")}"
            "uk" -> "$n ${ukPlural(n, "година", "години", "годин")}"
            "it" -> if (n == 1) "$n ora" else "$n ore"
            else -> if (n == 1) "$n hour" else "$n hours"
        }
    }

    private fun ruPlural(n: Int, one: String, few: String, many: String): String {
        val nn = kotlin.math.abs(n) % 100
        val n1 = nn % 10
        return if (nn in 11..14) many else when (n1) {
            1 -> one
            2, 3, 4 -> few
            else -> many
        }
    }

    private fun ukPlural(n: Int, one: String, few: String, many: String): String {
        val nn = kotlin.math.abs(n) % 100
        val n1 = nn % 10
        return if (nn in 11..14) many else when (n1) {
            1 -> one
            2, 3, 4 -> few
            else -> many
        }
    }
}