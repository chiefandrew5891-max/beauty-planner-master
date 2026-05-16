package com.andrey.beautyplanner.appcontent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
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
import com.andrey.beautyplanner.Locales
import com.andrey.beautyplanner.openEmail

private const val PRIVACY_EMAIL = "chief.andrew5891@gmail.com"
private const val AUTHOR_NAME = "KISELOV ANDRII"

@Composable
fun PrivacyPolicyScreen(
    languageCode: String,
    onBack: () -> Unit
) {
    val fontScale = AppSettings.getFontScale()
    val content = privacyPolicyContent(languageCode)

    val titleFontSize = (18 * fontScale).sp
    val titleLineHeight = (24 * fontScale).sp
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 12.dp, end = 20.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .clickable(onClick = onBack)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Reply,
                    contentDescription = Locales.t("cd_back"),
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = content.screenTitle,
                color = MaterialTheme.colors.onBackground,
                fontSize = titleFontSize,
                lineHeight = titleLineHeight,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 6.dp, end = 4.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp)
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
    val primary = MaterialTheme.colors.primary
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
        RichParagraphText(
            text = prefix,
            bodyFontSize = bodyFontSize,
            bodyLineHeight = bodyLineHeight
        )

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
    val screenTitle: String,
    val documentTitle: String,
    val lastUpdated: String,
    val intro: String,
    val sections: List<PrivacyPolicySection>
)

private fun privacyPolicyContent(languageCode: String): PrivacyPolicyContent {
    return when (languageCode) {
        "ru" -> PrivacyPolicyContent(
            screenTitle = "Политика конфиденциальности",
            documentTitle = "Политика конфиденциальности приложения Beauty Planner",
            lastUpdated = "Последнее обновление: Май 2026 г.",
            intro = "Настоящая Политика конфиденциальности описывает, как $AUTHOR_NAME (далее «мы», «наш» или «Разработчик») осуществляет сбор, использование, обработку и защиту информации пользователей (далее «пользователь» или «вы») в мобильном приложении Beauty Planner.",
            sections = listOf(
                PrivacyPolicySection(
                    title = "1. Сбор и использование персональных данных",
                    paragraphs = listOf(
                        "Приложение разработано как персональный органайзер для бьюти-мастеров. Мы собираем минимальный объем данных, необходимый для обеспечения базовой функциональности:"
                    ),
                    bullets = listOf(
                        "Данные профиля и аутентификации: Если в приложении используется функция создания учетной записи, мы можем обрабатывать ваш email и имя.",
                        "Данные планирования и клиентов: Информация, которую вы вносите в календарь (имена клиентов, номера телефонов, типы процедур, даты и стоимость услуг), обрабатывается исключительно для локального отображения и управления вашим расписанием.",
                        "Технические данные: Мы можем собирать обезличенные технические идентификаторы устройств и логи ошибок для улучшения стабильности работы приложения."
                    )
                ),
                PrivacyPolicySection(
                    title = "2. Разрешения (Permissions)",
                    paragraphs = listOf(
                        "Для полноценной работы приложение может запрашивать доступ к:"
                    ),
                    bullets = listOf(
                        "Уведомлениям (Notifications): Для отправки напоминаний о предстоящих записях и процедурах.",
                        "Контактам (Contacts): Исключительно по вашей инициативе через стандартный системный интерфейс Android Contact Picker для быстрого добавления клиента из телефонной книги. Приложение не собирает и не передает вашу адресную книгу на внешние сервера."
                    )
                ),
                PrivacyPolicySection(
                    title = "3. Передача данных третьим лицам",
                    paragraphs = listOf(
                        "Мы не продаем, не обмениваем и не передаем ваши личные данные третьим сторонам. Вся информация о ваших клиентах и записях хранится локально на вашем устройстве или в защищенном облачном хранилище, привязанном к вашей учетной записи."
                    )
                ),
                PrivacyPolicySection(
                    title = "4. Безопасность и хранение данных",
                    paragraphs = listOf(
                        "Мы обеспечиваем безопасную обработку данных с использованием современных методов шифрования. Данные хранятся до тех пор, пока вы используете приложение, или до момента направления запроса на их удаление."
                    )
                ),
                PrivacyPolicySection(
                    title = "5. Политика удаления данных (Data Deletion Policy)",
                    paragraphs = listOf(
                        "В соответствии с требованиями Google Play, вы имеете полное право запросить удаление своей учетной записи и всех связанных с ней данных. Вы можете сделать это двумя способами:"
                    ),
                    bullets = listOf(
                        "Непосредственно внутри приложения в меню «Настройки» -> «Удалить аккаунт»."
                    ),
                    emailLines = listOf(
                        "Отправив официальный запрос на нашу электронную почту:"
                    )
                ),
                PrivacyPolicySection(
                    title = "6. Контакты",
                    paragraphs = listOf(
                        "Если у вас возникли вопросы относительно данной политики конфиденциальности, пожалуйста, свяжитесь с нами по адресу:"
                    ),
                    emailLines = listOf("")
                )
            )
        )

        "uk" -> PrivacyPolicyContent(
            screenTitle = "Політика конфіденційності",
            documentTitle = "Політика конфіденційності додатка Beauty Planner",
            lastUpdated = "Останнє оновлення: Травень 2026 р.",
            intro = "Ця Політика конфіденційності описує, як $AUTHOR_NAME (далі «ми», «наш» або «Розробник») здійснює збір, використання, обробку та захист інформації користувачів (далі «користувач» або «ви») у мобільному додатку Beauty Planner.",
            sections = listOf(
                PrivacyPolicySection(
                    title = "1. Збір та використання персональних даних",
                    paragraphs = listOf(
                        "Додаток розроблений як персональний органайзер для б'юті-майстрів. Ми збираємо мінімальний обсяг даних, необхідний для забезпечення базової функціональності:"
                    ),
                    bullets = listOf(
                        "Дані профілю та автентифікації: Якщо в додатку використовується функція створення облікового запису, ми можемо обробляти ваш email та ім'я.",
                        "Дані планування та клієнтів: Інформація, яку ви вносите в календар (імена клієнтів, номери телефонів, типи процедур, дати та вартість послуг), обробляється виключно для локального відображення та управління вашим розкладом.",
                        "Технічні дані: Ми можемо збирати знеособлені технічні ідентифікатори пристроїв та логи помилок для покращення стабільності роботи додатка."
                    )
                ),
                PrivacyPolicySection(
                    title = "2. Дозволи (Permissions)",
                    paragraphs = listOf(
                        "Для повноцінної роботи додаток може запитувати доступ до:"
                    ),
                    bullets = listOf(
                        "Сповіщень (Notifications): Для надсилання нагадувань про майбутні записи та процедури.",
                        "Контактів (Contacts): Виключно за вашою ініціативою через стандартний системний інтерфейс Android Contact Picker для швидкого додавання клієнта з телефонної книги. Додаток не збирає і не передає вашу адресну книгу на зовнішні сервери."
                    )
                ),
                PrivacyPolicySection(
                    title = "3. Передача даних третім особам",
                    paragraphs = listOf(
                        "Ми не продаємо, не обмінюємо і не передаємо ваші особисті дані третім сторонам. Вся інформація про ваших клієнтів та записи зберігається локально на вашому пристрої або в захищеному хмарному сховищі, прив'язаному до вашого облікового запису."
                    )
                ),
                PrivacyPolicySection(
                    title = "4. Безпека та зберігання даних",
                    paragraphs = listOf(
                        "Ми забезпечуємо безпечну обробку даних з використанням сучасних методів шифрування. Дані зберігаються доти, доки ви використовуєте додаток, або до моменту направлення запиту на їх видалення."
                    )
                ),
                PrivacyPolicySection(
                    title = "5. Політика видалення даних (Data Deletion Policy)",
                    paragraphs = listOf(
                        "Відповідно до вимог Google Play, ви маєте повне право запросити видалення свого облікового запису та всіх пов'язаних з ним даних. Ви можете зробити це двома способами:"
                    ),
                    bullets = listOf(
                        "Безпосередньо всередині додатка в меню «Налаштування» -> «Видалити акаунт»."
                    ),
                    emailLines = listOf(
                        "Надіславши офіційний запит на нашу електронну пошту:"
                    )
                ),
                PrivacyPolicySection(
                    title = "6. Контакти",
                    paragraphs = listOf(
                        "Якщо у вас виникли запитання щодо цієї політики конфіденційності, будь ласка, зв'яжіться з нами за адресою:"
                    ),
                    emailLines = listOf("")
                )
            )
        )

        "it" -> PrivacyPolicyContent(
            screenTitle = "Informativa sulla Privacy",
            documentTitle = "Informativa sulla Privacy di Beauty Planner",
            lastUpdated = "Ultimo aggiornamento: Maggio 2026",
            intro = "La presente Informativa sulla Privacy descrive le modalità con cui $AUTHOR_NAME (\"noi\", \"nostro\" o \"Sviluppatore\") raccoglie, utilizza, elabora e protegge le informazioni degli utenti (\"utente\" o \"tu\") all'interno dell'applicazione mobile Beauty Planner.",
            sections = listOf(
                PrivacyPolicySection(
                    title = "1. Raccolta e Utilizzo dei Dati Personali",
                    paragraphs = listOf(
                        "L'applicazione è progettata come un organizzatore personale per i professionisti del settore della bellezza. Raccogliamo la quantità minima di dati necessari per fornire le funzionalità di base:"
                    ),
                    bullets = listOf(
                        "Dati del Profilo e Autenticazione: Se l'app include la creazione di un account, potremmo elaborare il tuo indirizzo email e il tuo nome.",
                        "Dati di Pianificazione e Clienti: Le informazioni inserite nel calendario (nomi dei clienti, numeri di telefono, tipi di trattamento, date e costi del servizio) sono elaborate esclusivamente per visualizzare e gestire il tuo programma a livello locale.",
                        "Dati Tecnici: Potremmo raccogliere identificatori anonimi del dispositivo e registri di crash per migliorare la stabilità e le prestazioni dell'app."
                    )
                ),
                PrivacyPolicySection(
                    title = "2. Autorizzazioni del Dispositivo (Permissions)",
                    paragraphs = listOf(
                        "Per funzionare correttamente, l'applicazione potrebbe richiedere l'accesso a:"
                    ),
                    bullets = listOf(
                        "Notifiche (Notifications): Per inviare promemoria sui prossimi appuntamenti e trattamenti.",
                        "Contatti (Contacts): Esclusivamente su tua esplicita iniziativa tramite l'interfaccia standard Android Contact Picker per aggiungere rapidamente un cliente dalla rubrica. L'applicazione non raccoglie né trasmette la tua rubrica a server esterni."
                    )
                ),
                PrivacyPolicySection(
                    title = "3. Condivisione dei Dati con Terze Parti",
                    paragraphs = listOf(
                        "Non vendiamo, commerciamo o trasferiamo i tuoi dati personali a terze parti. Tutte le informazioni relative ai tuoi clienti e appuntamenti sono memorizzate localmente sul tuo dispositivo o all'interno di uno spazio di archiviazione cloud sicuro collegato al tuo account."
                    )
                ),
                PrivacyPolicySection(
                    title = "4. Sicurezza e Conservazione dei Dati",
                    paragraphs = listOf(
                        "Garantiamo la gestione sicura dei dati utilizzando pratiche di crittografia standard del settore. I dati vengono conservati finché utilizzi attivamente l'app o fino alla richiesta di cancellazione."
                    )
                ),
                PrivacyPolicySection(
                    title = "5. Politica di Cancellazione dei Dati (Data Deletion Policy)",
                    paragraphs = listOf(
                        "In conformità con i requisiti di Google Play e del GDPR, hai il diritto di richiedere la cancellazione completa del tuo account e dei dati associati. È possibile richiedere la cancellazione:"
                    ),
                    bullets = listOf(
                        "Direttamente all'interno dell'app tramite \"Impostazioni\" -> \"Elimina Account\"."
                    ),
                    emailLines = listOf(
                        "Contattandoci via email all'indirizzo:"
                    )
                ),
                PrivacyPolicySection(
                    title = "6. Contatti",
                    paragraphs = listOf(
                        "Per qualsiasi domanda riguardante la presente Informativa sulla Privacy, si prega di contattarci all'indirizzo:"
                    ),
                    emailLines = listOf("")
                )
            )
        )

        else -> PrivacyPolicyContent(
            screenTitle = "Privacy Policy",
            documentTitle = "Privacy Policy for Beauty Planner",
            lastUpdated = "Last Updated: May 2026",
            intro = "This Privacy Policy describes how $AUTHOR_NAME (\"we\", \"our\", or \"Developer\") collects, uses, processes, and protects the information of users (\"user\" or \"you\") within the Beauty Planner mobile application.",
            sections = listOf(
                PrivacyPolicySection(
                    title = "1. Collection and Use of Personal Data",
                    paragraphs = listOf(
                        "The app is designed as a personal organizer for beauty professionals. We collect the minimum amount of data required to provide essential application functionality:"
                    ),
                    bullets = listOf(
                        "Profile and Authentication Data: If the app includes account creation functionality, we may process your email address and name.",
                        "Scheduling and Client Data: Information you enter into the calendar (client names, phone numbers, appointment types, dates, and service costs) is processed solely to display and manage your schedule locally.",
                        "Technical Data: We may collect anonymous device identifiers and crash logs to improve app stability and performance."
                    )
                ),
                PrivacyPolicySection(
                    title = "2. Device Permissions",
                    paragraphs = listOf(
                        "To function effectively, the app may request access to:"
                    ),
                    bullets = listOf(
                        "Notifications: To send reminders about upcoming appointments and beauty sessions.",
                        "Contacts: Only upon your explicit initiative via the standard Android Contact Picker API to quickly add a client from your phonebook. The app does not collect or transmit your address book to external servers."
                    )
                ),
                PrivacyPolicySection(
                    title = "3. Third-Party Data Sharing",
                    paragraphs = listOf(
                        "We do not sell, trade, or transfer your personal data to third parties. All information regarding your clients and appointments is stored locally on your device or within secure cloud storage tied to your verified account."
                    )
                ),
                PrivacyPolicySection(
                    title = "4. Data Security and Retention",
                    paragraphs = listOf(
                        "We ensure secure data handling using industry-standard encryption practices. Data is retained for as long as you actively use the app or until you request its deletion."
                    )
                ),
                PrivacyPolicySection(
                    title = "5. Data Deletion Policy",
                    paragraphs = listOf(
                        "In compliance with Google Play requirements, you have the right to request the complete deletion of your account and associated data. You can initiate this:"
                    ),
                    bullets = listOf(
                        "Directly within the app via \"Settings\" -> \"Delete Account\"."
                    ),
                    emailLines = listOf(
                        "By contacting us via email at:"
                    )
                ),
                PrivacyPolicySection(
                    title = "6. Contact Information",
                    paragraphs = listOf(
                        "If you have any questions regarding this Privacy Policy, please contact us at:"
                    ),
                    emailLines = listOf("")
                )
            )
        )
    }
}