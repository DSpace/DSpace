<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<% org.dspace.core.Context context = org.dspace.app.webui.util.UIUtil.obtainContext(request); %>

<dspace:layout locbar="nolink" title="About" feedData="NONE">

    <table  width="95%" align="center">
        <tr>
            <td class="oddRowEvenCol">
                <p style='text-align:justify'><b>СТРУКТУРА eSSUIR</b></p>
                <ul>
                    <li style='text-align:justify'><b>eSSUIR</b> представлений через спільноти.</li>
                    <li style='text-align:justify'>В <b>eSSUIR</b> виділені наступні спільноти:
                        <ul>
                            <li style='text-align:justify'>Електронний архів бібліотечно-інформаційного центру;</li>
                            <li style='text-align:justify'>Періодичні видання СумДУ;</li>
                            <li style='text-align:justify'>Індивідуальні колекції викладачів та співробітників;</li>
                            <li style='text-align:justify'>Матеріали партнерів.</li>
                        </ul>
                    </li>

                    <li style='text-align:justify'>Кожна спільнота в середині себе поділяється на підрозділи (наприклад, у спільноті «Індивідуальні колекції викладачів та співробітників» є підрозділення на факультети), які в свою чергу поділяються на колекції (зазвичай, колекції виділяються за типом матеріалу, наприклад, матеріали конференцій, наукові видання, навчальні видання).</li>
                    <li style='text-align:justify'>Для зручності доступу до матеріалів, розміщених в <b>eSSUIR</b>, виділено спільноту «Електронний архів бібліотечно-інформаційного центру», яка поділяється на підрозділи, що вказують на тип матеріалу.</li>
                    <li style='text-align:justify'>Для представлення студентських та аспірантських робіт організовані відповідні колекції в кожному підрозділі спільноти «Індивідуальні колекції викладачів та співробітників».</li>
                    <li style='text-align:justify'>Спільнота «Періодичні видання СумДУ» містить архіви усіх періодичних видань університету.</li>
                    <li style='text-align:justify'>Із розвитком архіву, поділ за спільнотами, підрозділами та колекціями може змінюватися. </li>
                </ul>

                <p style='text-align:justify'><b>ПОЛІТИКИ eSSUIR</b></p>

                <p style='text-align:justify'>З розвитком інформаційних технологій всі політики можуть переглядатися та змінюватися.</p>

                <p style='text-align:justify'><b><a name="part5">Політика щодо колекції та даних</a></b></p>
                <ul>
                    <li style='text-align:justify'><b>eSSUIR</b> підтримує політику відкритого доступу та не підтримує депозиторів, які ставлять обмеження доступу до своїх матеріалів;</li>
                    <li style='text-align:justify'>Будь-хто може вільно використовувати повні тексти чи інші цифрові дані, що містяться в розміщених матеріалах; </li>
                    <li style='text-align:justify'>Копії матеріалів, що розміщені в <b>eSSUIR</b>, можуть бути відтворені, представлені чи передані третій стороні і збережені в базах даних в будь-якому форматі та на будь-якому носії з некомерційною метою без попереднього узгодження. При цьому має бути вказано:
                        <ul>
                            <li style='text-align:justify'>автор(и), назва та всі інші елементи бібліографічного опису;</li>
                            <li style='text-align:justify'>гіперпосилання та/або URL на сторінку оригінальних метаданих.</li>
                        </ul>
                        <p>Зміст матеріалу не може бути змінений жодним чином.</p>
                    </li>
                    <li style='text-align:justify'>Повнотекстові матеріали та інші цифрові дані не повинні збиратися роботами та харвестерами, окрім як для індексації повнотекстових статтей або аналізу цитування.</li>
                    <li style='text-align:justify'>Повнотекстові матеріали та інші цифрові дані (наповнення) не можуть бути проданими або іншим чином реалізовані з комерційною метою та переведені у будь-які інші формати без офіційного дозволу власників авторських прав. </li>
                    <li style='text-align:justify'><b>eSSUIR</b> не є видавцем, це просто онлайновий архів.</li>
                    <li style='text-align:justify'>Згадування електронного архіву Сумського державного університету (<b>eSSUIR</b>) вітається, але не є обов'язковим. </li>
                </ul>

                <p style='text-align:justify'><b><a name="part1">Політика щодо змісту</a></b></p>
                <ul>
                    <li style='text-align:justify'><b>eSSUIR</b> є цифровим архівом, що збирає, зберігає та забезпечує постійний та надійний доступ до наукових та освітніх матеріалів, створених науковцями, студентами, аспірантами та докторантами, співробітниками СумДУ. </li>
                    <li style='text-align:justify'><b>eSSUIR</b> доповнює традиційні наукові видання університету та забезпечує можливість збирати матеріали актуальних досліджень;</li>
                    <li style='text-align:justify'>Зміст колекції не обмежується типами матеріалів.</li>
                    <li style='text-align:justify'><b>eSSUIR</b> підтримує всі типи матеріалів.</li>
                    <li style='text-align:justify'>Розміщені матеріали можуть включати:
                        <ul>
                            <li style='text-align:justify'>Робочі версії</li>
                            <li style='text-align:justify'>Версії подані до друку (лише надіслані до журналів)</li>
                            <li style='text-align:justify'>Версії, прийняті до друку (остаточна рецензована версія)</li>
                            <li style='text-align:justify'>Опубліковані версії</li>
                        </ul>
                    </li>
                    <li style='text-align:justify'>Основні мови: українська, російська, англійська.</li>
                </ul>

                <p style='text-align:justify'><b><a name="part4">Політика щодо форматів</a></b></p>
                <ul>
                    <li style='text-align:justify'>Розміщення матеріалів в <b>eSSUIR</b> не обмежується якимось видом цифрового матеріалу (наприклад, текстові файли, чи звукові файли, чи відео файли);</li>
                    <li style='text-align:justify'><b>eSSUIR</b> підтримує усі файлові формати, в яких створені ресурси; </li>
                    <li style='text-align:justify'>Матеріали в <b>eSSUIR</b> зберігаються із використанням найкращих практик управління даними та цифрового збереження;</li>
                    <li style='text-align:justify'>Однак, <b>eSSUIR</b> рекомендує для використання певні формати в кожному з видів матеріалів:
                        <p style='text-align:center'>Формати, що рекомендуються для застосуванні, при розміщенні матеріалів</p>
                        <table align="center" width="95%" border="1">
                            <tr><th class="evenRowEvenCol">Матеріал</th><th class="evenRowEvenCol">Назва формату</th><th class="evenRowEvenCol">Розширення</th></tr>

                            <tr><th class="evenRowOddCol">Текст</td><th class="evenRowOddCol">Adobe PDF</td><th class="evenRowOddCol"><a href="https://zakon.rada.gov.ua/laws/show/835-2015-%D0%BF#Text" target="_blank">pdf у машиночитаному форматі</a></td></tr>
                            <tr><th class="evenRowOddCol">Презентація</td><th class="evenRowOddCol">Microsoft Powerpoint</td><th class="evenRowOddCol">ppt</td></tr>
                            <tr><th class="evenRowOddCol">Таблиці</td> <th class="evenRowOddCol">Microsoft Excel</td><th class="evenRowOddCol">xls</td></tr>
                            <tr><th class="evenRowOddCol">Зображення</td><th class="evenRowOddCol">JPEG, GIF</td><th class="evenRowOddCol">jpg, gif</td></tr>
                            <tr><th class="evenRowOddCol">Аудіо</td><th class="evenRowOddCol">MP3</td><th class="evenRowOddCol">mp3</td></tr>
                            <tr><th class="evenRowOddCol">Відео</td><th class="evenRowOddCol">AVI</td><th class="evenRowOddCol">avi</td></tr>
                        </table>
                    </li>
                </ul>

                <p style='text-align:justify'><b><a name="part3">Політика щодо метаданих</a></b></p>
                <ul>
                    <li style='text-align:justify'>Будь-хто може вільно використовувати метадані матеріалів, розміщених в <b>eSSUIR</b>.</li>
                    <li style='text-align:justify'>Метадані можуть бути використані з некомерційною метою без попереднього узгодження з <b>eSSUIR</b>. При цьому має бути подано OAI ідентифікатор чи посилання на оригінальний запис метаданих та згаданий <b>eSSUIR</b>.</li>
                    <li style='text-align:justify'>Метадані не можуть бути використані з комерційною метою без попереднього офіційного узгодження.</li>
                    <li style='text-align:justify'>Розміщення матеріалів в <b>eSSUIR</b> вимагає заповнення основного набору полів метаданих (описової інформації). Деякі з цих метаданих можуть автоматично генеруватися програмним забезпеченням (DSpace), що використовується в <b>eSSUIR</b>, інші повинні бути заповнені депозитором під час процесу розміщення матеріалу до репозитарію;</li>
                    <li style='text-align:justify'>Використання елементів необхідних метаданих допомагає користувачу мати доступ до архівованих робіт і забезпечувати інформацією, потрібною для підтримки безперервного доступу до операцій управління та зберігання;</li>
                    <li style='text-align:justify'>Деякі поля метаданих є обов’язковими для заповнення, деякі – факультативними.</li>
                </ul>

                <p style='text-align:center'>Поля метаданих обов’язкові для заповнення</p>
                <table align="center" width="95%" border="1">
                    <tr><th class="evenRowEvenCol">Поле</th><th class="evenRowEvenCol">Заповнює депозитор/ автоматично DSpace</th><th class="evenRowEvenCol">Повторюване/ неповторюване</th><th class="evenRowEvenCol">Текст вноситься вручну/ контролюється списками</th></tr>

                    <tr><th class="evenRowOddCol">Назва</td><th class="evenRowOddCol">Депозитор</td><th class="evenRowOddCol">Повторюване (для іншого варіанту назви)</td><th class="evenRowOddCol">Вноситься</td></tr>
                    <tr><th class="evenRowOddCol">Тип ресурсу</td><th class="evenRowOddCol">Депозитор</td><th class="evenRowOddCol">Повторюване</td><th class="evenRowOddCol">Контролюється</td></tr>
                    <tr><th class="evenRowOddCol">Формат ресурсу</td><th class="evenRowOddCol">DSpace</td><th class="evenRowOddCol">Повторюване (якщо до одного ресурсу включено декілька файлів)</td><th class="evenRowOddCol">Контролюється</td></tr>
                    <tr><th class="evenRowOddCol">Розмір ресурсу</td><th class="evenRowOddCol">DSpace</td><th class="evenRowOddCol">Повторюване (якщо до одного ресурсу включено декілька файлів)</td><th class="evenRowOddCol">Контролюється</td></tr>
                    <tr><th class="evenRowOddCol">Дата публікації / розповсюдження</td><th class="evenRowOddCol">Депозитор</td>	<th class="evenRowOddCol">Не повторюване</td><th class="evenRowOddCol">Контролюється</td></tr>
                    <tr><th class="evenRowOddCol">Предмет (ключові слова)</td><th class="evenRowOddCol">Депозитор</td><th class="evenRowOddCol">Повторюване</td><th class="evenRowOddCol">Вноситься</td></tr>
                    <tr><th class="evenRowOddCol">Постійний URL</td><th class="evenRowOddCol">DSpace</td><th class="evenRowOddCol">Не повторюване</td><th class="evenRowOddCol">Контролюється</td></tr>
                </table>

                <p style='text-align:center'>Поля метаданих факультативні для заповнення</p>
                <table align="center" width="95%" border="1">
                    <tr><th class="evenRowEvenCol">Поле метаданих</th><th class="evenRowEvenCol">Рекомендації для застосування</th><th class="evenRowEvenCol">Повторюване/не повторюване поле</th><th class="evenRowEvenCol">Текст вноситься вручну / контролюється списками</th></tr>

                    <tr><th class="evenRowOddCol">Автор</td><th class="evenRowOddCol">Для ресурсів, що мають автора/ів</td><th class="evenRowOddCol">Повторюване</td><th class="evenRowOddCol">Вноситься</td></tr>
                    <tr><th class="evenRowOddCol">Основна мова ресурсу</td><th class="evenRowOddCol">Для всіх текстових ресурсів</td><th class="evenRowOddCol">Не повторюване</td><th class="evenRowOddCol">Контролюється</td></tr>
                    <tr><th class="evenRowOddCol">Анотація укр.мовою</td><th class="evenRowOddCol">Для всіх ресурсів</td><th class="evenRowOddCol">Не повторюване</td><th class="evenRowOddCol">Вноситься</td></tr>
                    <tr><th class="evenRowOddCol">Анотація рос.мовою</td><th class="evenRowOddCol">Для всіх ресурсів</td><th class="evenRowOddCol">Не повторюване</td><th class="evenRowOddCol">Вноситься</td></tr>
                    <tr><th class="evenRowOddCol">Анотація англ.мовою</td><th class="evenRowOddCol">Для всіх ресурсів</td><th class="evenRowOddCol">Не повторюване</td><th class="evenRowOddCol">Вноситься</td></tr>
                    <tr><th class="evenRowOddCol">Редактор</td><th class="evenRowOddCol">Для всіх ресурсів, що мають редактора</td><th class="evenRowOddCol">Повторюване</td><th class="evenRowOddCol">Вноситься</td></tr>
                    <tr><th class="evenRowOddCol">Науковий керівник</td><th class="evenRowOddCol">Для всіх ресурсів, що мають наукового керівника</td><th class="evenRowOddCol">Неповторюване</td><th class="evenRowOddCol">Вноситься</td></tr>
                    <tr><th class="evenRowOddCol">Інші особи</td><th class="evenRowOddCol">Для всіх ресурсів, що мають відповідальними за зміст осіб, інших ніж автори, редактори, наукові керівники.</td><th class="evenRowOddCol">Повторюване</td><th class="evenRowOddCol">Вноситься</td></tr>
                    <tr><th class="evenRowOddCol">Публікаційний статус</td><th class="evenRowOddCol">Для всіх ресурсів, опублікованих раніше, в процесі публікації чи публікується вперше в <b>eSSUIR</b></td><th class="evenRowOddCol">Не повторюване</td><th class="evenRowOddCol">Контролюється</td></tr>
                    <tr><th class="evenRowOddCol">Рецензований</td><th class="evenRowOddCol">Для всіх ресурсів, опублікованих раніше чи в процесі публікації</td><th class="evenRowOddCol">Не повторюване</td><th class="evenRowOddCol">Контролюється</td></tr>
                    <tr><th class="evenRowOddCol">Посилання</td><th class="evenRowOddCol">Для раніше опублікованих ресурсів</td><th class="evenRowOddCol">Не повторюване</td><th class="evenRowOddCol">Вноситься</td></tr>
                    <tr><th class="evenRowOddCol">Видавець</td><th class="evenRowOddCol">Для раніше опублікованих чи розповсюджених ресурсів</td><th class="evenRowOddCol">Не повторюване</td><th class="evenRowOddCol">Вноситься</td></tr>
                    <tr><th class="evenRowOddCol">Серії та номери звіту</td><th class="evenRowOddCol">Для серійних ресурсів (наприклад, серій робочих матеріалів)</td><th class="evenRowOddCol">Повторюване</td><th class="evenRowOddCol">Вноситься</td></tr>
                    <tr><th class="evenRowOddCol">Ідентифікатори</td><th class="evenRowOddCol">Для ресурсів з ідентифікаторами такими, як ISBN чи DOI, для опублікованих версій ресурсу</td><th class="evenRowOddCol">Повторюване</td><th class="evenRowOddCol">Контролюється</td></tr>
                    <tr><th class="evenRowOddCol">Спонсор</td><th class="evenRowOddCol">Для ресурсів, що базуються на роботі, що спонсується чи підтримується організацією чи агенцією поза університетом.</td><th class="evenRowOddCol">Повторюване</td><th class="evenRowOddCol">Вноситься</td></tr>
                </table>


                <p style='text-align:justify'><b><a name="part2">Політика щодо розміщення, депозиторів, якості та авторського права</a></b></p>
                <ul>
                    <li style='text-align:justify'>Матеріали в <b>eSSUIR</b> можуть розміщуватися лише зареєстрованими користувачами: науковцями, студентами, аспірантами та докторантами, співробітниками СумДУ або чи бібліотекою, за дорученням депозитора;</li>
                    <li style='text-align:justify'>Депозиторами можуть бути науковці, особи, офіційно не зареєстровані як співробітники університету, якщо вони є співавторами університетських авторів чи тісно пов’язані з університетом, наприклад, заслужені професори, особи, що мають почесні посади в університеті, чи випускники університету;</li>
                    <li style='text-align:justify'>Студентські роботи розміщуються в <b>eSSUIR</b> за рекомендаціями викладчів;</li>
                    <li style='text-align:justify'>Якщо депозитор розміщує самостійно матеріал в <b>eSSUIR</b>, або передає його до наукової бібліотеки СумДУ для розміщення цього матеріалу в <b>eSSUIR</b> за дорученням, то він погоджуєтесь з умовами Авторського договору про передачу невиключних прав на використання твору через <b>eSSUIR</b>.</li>
                    <li style='text-align:justify'>Автори можуть розміщувати лише свої власні роботи;</li>
                    <li style='text-align:justify'>За якість розміщених матеріалів відповідають депозитори;</li>
                    <li style='text-align:justify'>Матеріали не можуть розміщуватись в <b>eSSUIR</b>, якщо вони знаходяться під періодом ембарго у видавця;</li>
                    <li style='text-align:justify'>За будь-які порушення авторського права повну відповідальність несуть автори/ депозитори.</li>
                    <li style='text-align:justify'>Якщо <b>eSSUIR</b> отримає підтвердження порушення авторських прав щодо розміщеного матеріалу, то відповідний примірник буде одразу вилучений з архіву.</li>
                </ul>

                <p style='text-align:justify'><b>Політика щодо відкликання матеріалів</b></p>
                <ul>
                    <li style='text-align:justify'>У деяких випадках може виникнути необхідність відкликати матеріал із архіву. Відклики можуть ініціюватися: депозитором, організацією чи в правовому порядку.</li>
                    <li style='text-align:justify'>Причиною для відклику може бути публікація статті у видавництві, яке не дозволяє розміщення препринтів чи репрезентація документів у інших системах.</li>
                    <li style='text-align:justify'>Усі запити на відклик проходять через адміністратора <b>eSSUIR</b>.</li>
                    <li style='text-align:justify'>Після відкликання матеріалу з <b>eSSUIR</b> копія утримується в архіві, що є недоступним для широкого загалу, але залишається представленою в <b>eSSUIR</b> метаданими (описовою інформацією) та приміткою про відклик матеріалу в місці посилання на об’єкт.</li>
                </ul>
            </td>
        </tr>
    </table>
    <br/>
</dspace:layout>