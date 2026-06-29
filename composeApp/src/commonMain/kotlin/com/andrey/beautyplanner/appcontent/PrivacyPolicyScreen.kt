package com.andrey.beautyplanner.appcontent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.openEmail

private const val PRIVACY_EMAIL = "beautyplanner2026@gmail.com"
private const val AUTHOR_NAME = "KISELOV ANDRII"

@Composable
fun PrivacyPolicyScreen(
    languageCode: String,
    onBack: () -> Unit
) {
    val fontScale = AppSettings.getFontScale()
    val content = privacyPolicyContent(languageCode)

    val docTitleFontSize = (20 * fontScale).sp
    val docTitleLineHeight = (26 * fontScale).sp
    val sectionTitleFontSize = (16 * fontScale).sp
    val sectionTitleLineHeight = (22 * fontScale).sp
    val bodyFontSize = (14 * fontScale).sp
    val bodyLineHeight = (21 * fontScale).sp
    val metaFontSize = (12 * fontScale).sp
    val metaLineHeight = (16 * fontScale).sp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 20.dp)
        ) {
            Text(
                text = content.documentTitle,
                color = MaterialTheme.colors.onBackground,
                fontSize = docTitleFontSize,
                lineHeight = docTitleLineHeight,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.size(8.dp))

            Text(
                text = content.lastUpdated,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f),
                fontSize = metaFontSize,
                lineHeight = metaLineHeight
            )

            Spacer(modifier = Modifier.size(16.dp))

            RichParagraphText(
                text = content.intro,
                bodyFontSize = bodyFontSize,
                bodyLineHeight = bodyLineHeight
            )

            Spacer(modifier = Modifier.size(22.dp))

            content.sections.forEach { section ->
                Text(
                    text = section.title,
                    color = MaterialTheme.colors.onBackground,
                    fontSize = sectionTitleFontSize,
                    lineHeight = sectionTitleLineHeight,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.size(10.dp))

                section.paragraphs.forEach { paragraph ->
                    RichParagraphText(
                        text = paragraph,
                        bodyFontSize = bodyFontSize,
                        bodyLineHeight = bodyLineHeight
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                }

                section.bullets.forEach { bullet ->
                    BulletItem(
                        text = bullet,
                        bodyFontSize = bodyFontSize,
                        bodyLineHeight = bodyLineHeight
                    )
                }

                if (section.emailLines.isNotEmpty()) {
                    Spacer(modifier = Modifier.size(4.dp))
                    section.emailLines.forEach { emailLine ->
                        EmailLine(
                            prefix = emailLine,
                            email = PRIVACY_EMAIL,
                            bodyFontSize = bodyFontSize,
                            bodyLineHeight = bodyLineHeight
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                    }
                }

                Spacer(modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun RichParagraphText(
    text: String,
    bodyFontSize: androidx.compose.ui.unit.TextUnit,
    bodyLineHeight: androidx.compose.ui.unit.TextUnit
) {
    val onBackground = MaterialTheme.colors.onBackground
    val annotated = buildAnnotatedString {
        val parts = text.split(AUTHOR_NAME)
        parts.forEachIndexed { index, part ->
            append(part)
            if (index < parts.lastIndex) {
                pushStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold,
                        color = onBackground
                    )
                )
                append(AUTHOR_NAME)
                pop()
            }
        }
    }

    Text(
        text = annotated,
        color = onBackground,
        fontSize = bodyFontSize,
        lineHeight = bodyLineHeight
    )
}

@Composable
private fun BulletItem(
    text: String,
    bodyFontSize: androidx.compose.ui.unit.TextUnit,
    bodyLineHeight: androidx.compose.ui.unit.TextUnit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            color = MaterialTheme.colors.onBackground,
            fontSize = bodyFontSize,
            lineHeight = bodyLineHeight,
            modifier = Modifier.padding(end = 8.dp)
        )
        RichParagraphText(
            text = text,
            bodyFontSize = bodyFontSize,
            bodyLineHeight = bodyLineHeight
        )
    }
}

@Composable
private fun EmailLine(
    prefix: String,
    email: String,
    bodyFontSize: androidx.compose.ui.unit.TextUnit,
    bodyLineHeight: androidx.compose.ui.unit.TextUnit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (prefix.isNotBlank()) {
            RichParagraphText(
                text = prefix,
                bodyFontSize = bodyFontSize,
                bodyLineHeight = bodyLineHeight
            )
        }

        Text(
            text = email,
            color = MaterialTheme.colors.primary,
            fontSize = bodyFontSize,
            lineHeight = bodyLineHeight,
            fontWeight = FontWeight.Medium,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable {
                openEmail(email)
            }
        )
    }
}

private data class PrivacyPolicySection(
    val title: String,
    val paragraphs: List<String> = emptyList(),
    val bullets: List<String> = emptyList(),
    val emailLines: List<String> = emptyList()
)

private data class PrivacyPolicyContent(
    val documentTitle: String,
    val lastUpdated: String,
    val intro: String,
    val sections: List<PrivacyPolicySection>
)

private fun privacyPolicyContent(languageCode: String): PrivacyPolicyContent {
    return when (languageCode) {
        "ru" -> PrivacyPolicyContent(
            documentTitle = "Политика конфиденциальности приложения Beauty Planner",
            lastUpdated = "Последнее обновление: Июнь 2026 г.",
            intro = "Настоящая Политика конфиденциальности описывает, как $AUTHOR_NAME (далее «мы», «наш» или «Разработчик») обрабатывает данные при использовании мобильного приложения Beauty Planner.",
            sections = listOf(
                PrivacyPolicySection(
                    title = "1. Какие данные обрабатываются",
                    paragraphs = listOf(
                        "Приложение Beauty Planner хранит и обрабатывает данные, которые вы вводите: имя клиента, телефон, дату и время записи, название услуги, цену, заметки, настройки приложения, а также связанные технические параметры.",
                        "Для входа и привязки аккаунта могут обрабатываться идентификатор пользователя (Firebase UID), email, отображаемое имя и провайдер входа (например, Google, email или анонимный режим)."
                    )
                ),
                PrivacyPolicySection(
                    title = "2. Где хранятся данные",
                    paragraphs = listOf(
                        "Данные записей и часть настроек хранятся локально на вашем устройстве.",
                        "Для функций аккаунта, подписки и управления доступом часть данных обрабатывается и хранится в облачной инфраструктуре (включая Firebase Authentication, Cloud Firestore и Cloud Functions).",
                        "Резервные копии создаются только по вашему действию (экспорт). Вы сами выбираете место их хранения. При включении шифрования резервная копия защищается паролем, который задаёте вы."
                    )
                ),
                PrivacyPolicySection(
                    title = "3. Покупки, подписка и платежи",
                    paragraphs = listOf(
                        "Для покупок Premium на Android приложение использует Google Play Billing.",
                        "Платёжные данные банковской карты обрабатываются Google Play. Приложение напрямую не получает данные вашей карты.",
                        "Для проверки и статуса подписки приложение может обрабатывать данные покупки, такие как productId, purchaseToken, состояние подписки, срок действия, автопродление и номер заказа (если доступен)."
                    )
                ),
                PrivacyPolicySection(
                    title = "4. Серверная проверка подписки и RTDN",
                    paragraphs = listOf(
                        "Для актуализации статуса подписки может использоваться серверная проверка через Google Play Developer API.",
                        "Также может использоваться обработка Real-time Developer Notifications (RTDN) для обновления статуса подписки."
                    )
                ),
                PrivacyPolicySection(
                    title = "5. Разрешения устройства",
                    paragraphs = listOf(
                        "Для отдельных функций приложение может запрашивать разрешения устройства."
                    ),
                    bullets = listOf(
                        "Уведомления — для напоминаний о записях.",
                        "Контакты — только если вы включаете автопоиск/подсказки по контактам.",
                        "Доступ к файлам — только через системный выбор файла для импорта/экспорта резервной копии.",
                        "Запуск после перезагрузки (на Android) — для восстановления запланированных напоминаний."
                    )
                ),
                PrivacyPolicySection(
                    title = "6. Передача данных третьим лицам",
                    paragraphs = listOf(
                        "Мы не продаём ваши персональные данные.",
                        "Данные могут передаваться только технологическим провайдерам, необходимым для работы функций приложения (например, Firebase и Google Play), в пределах функциональности, описанной в этой Политике."
                    )
                ),
                PrivacyPolicySection(
                    title = "7. Срок хранения и удаление данных",
                    paragraphs = listOf(
                        "Локальные данные можно удалить, очистив данные приложения на устройстве и/или удалив созданные вами файлы резервных копий.",
                        "Если у вас есть аккаунт, связанные серверные данные (например, профиль доступа/подписки) могут храниться в инфраструктуре приложения до удаления или в рамках технически обоснованного срока."
                    )
                ),
                PrivacyPolicySection(
                    title = "8. Безопасность",
                    paragraphs = listOf(
                        "Мы принимаем разумные технические меры для защиты данных в рамках используемых платформ и инфраструктуры.",
                        "Вы несёте ответственность за безопасность устройства, аккаунта, а также пароля резервной копии (если включено шифрование)."
                    )
                ),
                PrivacyPolicySection(
                    title = "9. Изменения Политики",
                    paragraphs = listOf(
                        "Мы можем обновлять настоящую Политику конфиденциальности. Актуальная редакция публикуется в приложении с датой последнего обновления."
                    )
                ),
                PrivacyPolicySection(
                    title = "10. Контакты",
                    paragraphs = listOf(
                        "Если у вас есть вопросы по данной Политике конфиденциальности, вы можете связаться с нами по адресу:"
                    ),
                    emailLines = listOf("")
                )
            )
        )

        "uk" -> PrivacyPolicyContent(
            documentTitle = "Політика конфіденційності додатка Beauty Planner",
            lastUpdated = "Останнє оновлення: Червень 2026 р.",
            intro = "Ця Політика конфіденційності описує, як $AUTHOR_NAME (далі «ми», «наш» або «Розробник») обробляє дані під час використання мобільного додатка Beauty Planner.",
            sections = listOf(
                PrivacyPolicySection(
                    title = "1. Які дані обробляються",
                    paragraphs = listOf(
                        "Додаток Beauty Planner зберігає та обробляє дані, які ви вводите: ім’я клієнта, телефон, дату й час запису, назву послуги, ціну, нотатки, налаштування додатка та пов’язані технічні параметри.",
                        "Для входу та прив’язки акаунта можуть оброблятися ідентифікатор користувача (Firebase UID), email, ім’я профілю та провайдер входу (наприклад, Google, email або анонімний режим)."
                    )
                ),
                PrivacyPolicySection(
                    title = "2. Де зберігаються дані",
                    paragraphs = listOf(
                        "Дані записів і частина налаштувань зберігаються локально на вашому пристрої.",
                        "Для функцій акаунта, підписки та керування доступом частина даних обробляється і зберігається у хмарній інфраструктурі (зокрема Firebase Authentication, Cloud Firestore і Cloud Functions).",
                        "Резервні копії створюються лише за вашою дією (експорт). Ви самостійно обираєте місце їх зберігання. Якщо увімкнено шифрування, резервна копія захищається паролем, який задаєте ви."
                    )
                ),
                PrivacyPolicySection(
                    title = "3. Покупки, підписка та платежі",
                    paragraphs = listOf(
                        "Для покупок Premium на Android додаток використовує Google Play Billing.",
                        "Платіжні дані банківської картки обробляються Google Play. Додаток напряму не отримує дані вашої картки.",
                        "Для перевірки та статусу підписки додаток може обробляти дані покупки, такі як productId, purchaseToken, стан підписки, строк дії, автоподовження та номер замовлення (якщо доступний)."
                    )
                ),
                PrivacyPolicySection(
                    title = "4. Серверна перевірка підписки та RTDN",
                    paragraphs = listOf(
                        "Для актуалізації статусу підписки може використовуватися серверна перевірка через Google Play Developer API.",
                        "Також може використовуватися обробка Real-time Developer Notifications (RTDN) для оновлення статусу підписки."
                    )
                ),
                PrivacyPolicySection(
                    title = "5. Дозволи пристрою",
                    paragraphs = listOf(
                        "Для окремих функцій додаток може запитувати дозволи пристрою."
                    ),
                    bullets = listOf(
                        "Сповіщення — для нагадувань про записи.",
                        "Контакти — лише якщо ви вмикаєте автопошук/підказки за контактами.",
                        "Доступ до файлів — лише через системний вибір файлу для імпорту/експорту резервної копії.",
                        "Запуск після перезавантаження (на Android) — для відновлення запланованих нагадувань."
                    )
                ),
                PrivacyPolicySection(
                    title = "6. Передача даних третім особам",
                    paragraphs = listOf(
                        "Ми не продаємо ваші персональні дані.",
                        "Дані можуть передаватися лише технологічним провайдерам, необхідним для роботи функцій додатка (наприклад, Firebase і Google Play), у межах функціональності, описаної в цій Політиці."
                    )
                ),
                PrivacyPolicySection(
                    title = "7. Строк зберігання та видалення даних",
                    paragraphs = listOf(
                        "Локальні дані можна видалити, очистивши дані додатка на пристрої та/або видаливши створені вами файли резервних копій.",
                        "Якщо у вас є акаунт, пов’язані серверні дані (наприклад, профіль доступу/підписки) можуть зберігатися в інфраструктурі додатка до видалення або в межах технічно обґрунтованого строку."
                    )
                ),
                PrivacyPolicySection(
                    title = "8. Безпека",
                    paragraphs = listOf(
                        "Ми застосовуємо розумні технічні заходи для захисту даних у межах використовуваних платформ та інфраструктури.",
                        "Ви несете відповідальність за безпеку пристрою, акаунта, а також пароля резервної копії (якщо увімкнено шифрування)."
                    )
                ),
                PrivacyPolicySection(
                    title = "9. Зміни Політики",
                    paragraphs = listOf(
                        "Ми можемо оновлювати цю Політику конфіденційності. Актуальна редакція публікується у додатку з датою останнього оновлення."
                    )
                ),
                PrivacyPolicySection(
                    title = "10. Контакти",
                    paragraphs = listOf(
                        "Якщо у вас є запитання щодо цієї Політики конфіденційності, ви можете зв’язатися з нами за адресою:"
                    ),
                    emailLines = listOf("")
                )
            )
        )

        "it" -> PrivacyPolicyContent(
            documentTitle = "Informativa sulla Privacy di Beauty Planner",
            lastUpdated = "Ultimo aggiornamento: Giugno 2026",
            intro = "La presente Informativa sulla Privacy descrive come $AUTHOR_NAME (\"noi\", \"nostro\" o \"Sviluppatore\") tratta i dati durante l’utilizzo dell’app mobile Beauty Planner.",
            sections = listOf(
                PrivacyPolicySection(
                    title = "1. Quali dati vengono trattati",
                    paragraphs = listOf(
                        "Beauty Planner memorizza e tratta i dati che inserisci: nome cliente, telefono, data e ora dell’appuntamento, nome del servizio, prezzo, note, impostazioni dell’app e parametri tecnici correlati.",
                        "Per accesso e collegamento account possono essere trattati identificativo utente (Firebase UID), email, nome visualizzato e provider di accesso (ad esempio Google, email o modalità anonima)."
                    )
                ),
                PrivacyPolicySection(
                    title = "2. Dove sono conservati i dati",
                    paragraphs = listOf(
                        "I dati degli appuntamenti e una parte delle impostazioni sono conservati localmente sul tuo dispositivo.",
                        "Per funzioni di account, abbonamento e gestione accessi, una parte dei dati viene trattata e conservata in infrastruttura cloud (inclusi Firebase Authentication, Cloud Firestore e Cloud Functions).",
                        "I backup vengono creati solo su tua azione (esportazione). Scegli tu dove salvarli. Se abiliti la cifratura, il backup è protetto da una password scelta da te."
                    )
                ),
                PrivacyPolicySection(
                    title = "3. Acquisti, abbonamento e pagamenti",
                    paragraphs = listOf(
                        "Per gli acquisti Premium su Android, l’app utilizza Google Play Billing.",
                        "I dati di pagamento della carta sono trattati da Google Play. L’app non riceve direttamente i dati della tua carta.",
                        "Per verifica e stato dell’abbonamento, l’app può trattare dati d’acquisto come productId, purchaseToken, stato dell’abbonamento, scadenza, rinnovo automatico e numero ordine (se disponibile)."
                    )
                ),
                PrivacyPolicySection(
                    title = "4. Verifica server dell’abbonamento e RTDN",
                    paragraphs = listOf(
                        "Per aggiornare lo stato dell’abbonamento può essere usata la verifica server tramite Google Play Developer API.",
                        "Può essere usata anche l’elaborazione delle Real-time Developer Notifications (RTDN) per aggiornare lo stato dell’abbonamento."
                    )
                ),
                PrivacyPolicySection(
                    title = "5. Permessi del dispositivo",
                    paragraphs = listOf(
                        "Per alcune funzionalità l’app può richiedere permessi del dispositivo."
                    ),
                    bullets = listOf(
                        "Notifiche — per promemoria appuntamenti.",
                        "Contatti — solo se abiliti ricerca/suggerimenti dai contatti.",
                        "Accesso ai file — solo tramite selettore file di sistema per import/export backup.",
                        "Avvio dopo riavvio (su Android) — per ripristinare i promemoria pianificati."
                    )
                ),
                PrivacyPolicySection(
                    title = "6. Condivisione dei dati con terze parti",
                    paragraphs = listOf(
                        "Non vendiamo i tuoi dati personali.",
                        "I dati possono essere condivisi solo con provider tecnologici necessari al funzionamento dell’app (ad esempio Firebase e Google Play), nei limiti delle funzionalità descritte in questa Informativa."
                    )
                ),
                PrivacyPolicySection(
                    title = "7. Conservazione e cancellazione dei dati",
                    paragraphs = listOf(
                        "Puoi eliminare i dati locali cancellando i dati dell’app dal dispositivo e/o eliminando i file di backup creati da te.",
                        "Se utilizzi un account, i dati server correlati (ad esempio profilo di accesso/abbonamento) possono essere conservati nell’infrastruttura dell’app fino a cancellazione o per un periodo tecnicamente giustificato."
                    )
                ),
                PrivacyPolicySection(
                    title = "8. Sicurezza",
                    paragraphs = listOf(
                        "Adottiamo misure tecniche ragionevoli per proteggere i dati nell’ambito delle piattaforme e infrastrutture utilizzate.",
                        "Sei responsabile della sicurezza del dispositivo, dell’account e della password del backup (se la cifratura è attiva)."
                    )
                ),
                PrivacyPolicySection(
                    title = "9. Modifiche all’Informativa",
                    paragraphs = listOf(
                        "Possiamo aggiornare la presente Informativa sulla Privacy. La versione aggiornata è pubblicata nell’app con la data di ultimo aggiornamento."
                    )
                ),
                PrivacyPolicySection(
                    title = "10. Contatti",
                    paragraphs = listOf(
                        "Per domande su questa Informativa sulla Privacy, puoi contattarci a:"
                    ),
                    emailLines = listOf("")
                )
            )
        )

        else -> PrivacyPolicyContent(
            documentTitle = "Privacy Policy for Beauty Planner",
            lastUpdated = "Last Updated: June 2026",
            intro = "This Privacy Policy explains how $AUTHOR_NAME (\"we\", \"our\", or \"Developer\") processes data when you use the Beauty Planner mobile application.",
            sections = listOf(
                PrivacyPolicySection(
                    title = "1. What data is processed",
                    paragraphs = listOf(
                        "Beauty Planner stores and processes data you enter: client name, phone number, appointment date and time, service name, price, notes, app settings, and related technical parameters.",
                        "For sign-in and account linking, the app may process user identifiers (Firebase UID), email, display name, and sign-in provider (for example Google, email, or anonymous mode)."
                    )
                ),
                PrivacyPolicySection(
                    title = "2. Where data is stored",
                    paragraphs = listOf(
                        "Appointment data and part of app settings are stored locally on your device.",
                        "For account, subscription, and access-control features, part of data is processed and stored in cloud infrastructure (including Firebase Authentication, Cloud Firestore, and Cloud Functions).",
                        "Backups are created only by your action (export). You choose where to store them. If encryption is enabled, the backup is protected by a password that you set."
                    )
                ),
                PrivacyPolicySection(
                    title = "3. Purchases, subscription, and payments",
                    paragraphs = listOf(
                        "For Premium purchases on Android, the app uses Google Play Billing.",
                        "Bank card payment data is processed by Google Play. The app does not directly receive your card details.",
                        "To verify and maintain subscription status, the app may process purchase-related data such as productId, purchaseToken, subscription state, expiry time, auto-renew status, and order ID (if available)."
                    )
                ),
                PrivacyPolicySection(
                    title = "4. Server-side subscription verification and RTDN",
                    paragraphs = listOf(
                        "To keep subscription status up to date, server-side verification may be performed via Google Play Developer API.",
                        "Real-time Developer Notifications (RTDN) may also be processed to update subscription status."
                    )
                ),
                PrivacyPolicySection(
                    title = "5. Device permissions",
                    paragraphs = listOf(
                        "For certain features, the app may request device permissions."
                    ),
                    bullets = listOf(
                        "Notifications — for appointment reminders.",
                        "Contacts — only if you enable contact-based autocomplete/suggestions.",
                        "File access — only through the system file picker for backup import/export.",
                        "Run after reboot (on Android) — to restore scheduled reminders."
                    )
                ),
                PrivacyPolicySection(
                    title = "6. Sharing data with third parties",
                    paragraphs = listOf(
                        "We do not sell your personal data.",
                        "Data may be shared only with technology providers required to deliver app functionality (for example Firebase and Google Play), within the scope described in this Policy."
                    )
                ),
                PrivacyPolicySection(
                    title = "7. Data retention and deletion",
                    paragraphs = listOf(
                        "You can delete local data by clearing app storage on your device and/or deleting backup files you created.",
                        "If you use an account, related server-side data (for example access/subscription profile) may be stored in app infrastructure until deletion or for a technically justified retention period."
                    )
                ),
                PrivacyPolicySection(
                    title = "8. Security",
                    paragraphs = listOf(
                        "We apply reasonable technical safeguards to protect data within the platforms and infrastructure used.",
                        "You are responsible for the security of your device, account, and backup password (if backup encryption is enabled)."
                    )
                ),
                PrivacyPolicySection(
                    title = "9. Policy updates",
                    paragraphs = listOf(
                        "We may update this Privacy Policy. The current version is published in the app with the latest update date."
                    )
                ),
                PrivacyPolicySection(
                    title = "10. Contact",
                    paragraphs = listOf(
                        "If you have questions about this Privacy Policy, you can contact us at:"
                    ),
                    emailLines = listOf("")
                )
            )
        )
    }
}