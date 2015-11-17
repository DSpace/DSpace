﻿<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<% org.dspace.core.Context context = org.dspace.app.webui.util.UIUtil.obtainContext(request); %>

<dspace:layout locbar="commLink" title="About" feedData="NONE">

    <table width="95%" align="center">
        <tr>
            <td class="oddRowEvenCol">
                <p style='text-align:justify'><b>eSSUIR – (Electronic Sumy State University Institutional
                    Repository)</b> – Електронний архів Сумського державного університету, що накопичує, зберігає,
                    розповсюджує та забезпечує довготривалий, постійний та надійний доступ до наукових досліджень
                    професорсько-викладацького складу, співробітників та студентів Університету. </p>

                <p style='text-align:justify'><b>eSSUIR</b> – ресурс відкритого доступу, розміщений на сервері
                    УНІВЕРСИТЕТУ в мережі Інтернет і доступний з будь-якого місця і у будь-який час.</p>

                <p style='text-align:justify'><a name="meta"><b>МЕТА eSSUIR</b></a></p>
                <ul>
                    <li style='text-align:justify'>Забезпечення накопичення, систематизації та зберігання в електронному
                        вигляді інтелектуальних продуктів університетської спільноти.
                    </li>
                    <li style='text-align:justify'>Сприяння поширенню цих матеріалів у світовому науково-освітньому
                        просторі.
                    </li>
                    <li style='text-align:justify'>Шляхом створення, збереження та надання вільного доступу до наукової
                        інформації, досліджень Університету українській та світовій науковій спільноті сприяти розвитку
                        науки та освіти України та світу.
                    </li>
                    <li style='text-align:justify'>Спонукати українську інформаційну, наукову та освітню спільноту до
                        активних дій та кооперації в напрямку вільного поширення (доступу) до наукових інформаційних
                        ресурсів університетів, задля соціальної трансформації ролі науки у сучасному суспільстві.
                    </li>
                </ul>

                <p style='text-align:justify'><a name="task"><b>ЗАВДАННЯ eSSUIR</b></a></p>
                <ul>
                    <li style='text-align:justify'>Створення організаційної, технічної, інформаційної інфраструктури
                        інституційного репозитарію (електронного архіву) Сумського державного університету для розвитку
                        та поширення наукових публікацій у відкритому доступі, збільшення впливу наукових досліджень
                        Університету шляхом забезпечення вільного доступу та розширення аудиторії їх користувачів
                        (науковців, студентів, викладачів, інформаційних працівників України та світу);
                    </li>
                    <li style='text-align:justify'>Накопичення, збереження, розповсюдження та забезпечення
                        довготривалого, постійного та надійного доступу до наукових досліджень
                        професорсько-викладацького складу, співробітників і студентів, аспірантів та докторантів
                        Університету;
                    </li>
                    <li style='text-align:justify'>Забезпечення середовища, що дозволяє науковим підрозділам СумДУ,
                        аспірантам, докторантам, співробітникам та студентам легко розміщувати наукові дослідження в
                        електронній формі у надійний та добре організований архів і стимулювати та забезпечувати
                        відкритий доступ до їх наукових досліджень.
                    </li>
                </ul>

                <p style='text-align:justify'><a name="part3"><b>ЗАГАЛЬНІ ПРИНЦИПИ РОЗМІЩЕННЯ МАТЕРІАЛІВ В
                    eSSUIR</b></a></p>
                <ul>
                    <li style='text-align:justify'>Робота повинна мати науковий, освітній чи дослідницький характер
                        (детальніше дивись - <a href="struct.jsp#part1">Політика щодо змісту</a>).
                    </li>
                    <li style='text-align:justify'>Робота мусить бути повністю або частково створена чи фінансована
                        Університетом, будь-яким його підрозділом, аспірантами, докторантами, співробітниками чи
                        студентами.
                    </li>
                    <li style='text-align:justify'>Депозиторами можуть бути науковці та співробітники СумДУ, особи,
                        офіційно не зареєстровані як співробітники Університету, якщо вони є співавторами
                        університетських авторів чи тісно пов’язані з Університетом, наприклад, заслужені професори,
                        особи, що мають почесні посади в університеті, студенти (за рекомендацією викладачів),
                        аспіранти, докторанти чи випускники університету (детальніше дивись - <a
                                href="struct.jsp#part2">Політика щодо розміщення, депозиторів, якості та авторського
                            права</a>).
                    </li>
                    <li style='text-align:justify'>Розміщувати власні робити в eSSUIR можуть зареєстровані користувачі
                        (детальніше дивитись – <a href="instruction.jsp">Інструкція реєстрації користувача eSSUIR</a>).
                    </li>
                    <li style='text-align:justify'>Робота може бути розміщена в eSSUIR як самим автором
                        (самоархівування), так і координатором eSSUIR (співробітником бібліотеки), за дорученням автора
                        (детальніше дивитись - <a href="struct.jsp#part2">Політика щодо розміщення, депозиторів, якості
                            та авторського права</a>).
                    </li>
                    <li style='text-align:justify'>Розміщення матеріалів в eSSUIR вимагає заповнення основного набору
                        полів метаданих (описової інформації). Детальніше дивись – <a href="struct.jsp#part3">Політика
                            щодо метаданих</a>.
                    </li>
                    <li style='text-align:justify'>Для кращого забезпечення довготермінового зберігання роботу має бути
                        подано у цифровій формі в одному із форматів, рекомендованих у <a href="struct.jsp#part4">Політиці
                            щодо форматів</a>.
                    </li>
                    <li style='text-align:justify'>Копії матеріалів, що розміщені в eSSUIR, можуть бути відтворені,
                        представлені чи передані третій стороні і збережені в базах даних в будь-якому форматі та на
                        будь-якому носію з некомерційною метою без попереднього узгодження (детальніше дивись – <a
                                href="struct.jsp#part5">Політика щодо колекції та даних</a>).
                    </li>
                </ul>

                <p style='text-align:justify'><a name="part4"><b>СТРУКТУРА eSSUIR</b></a></p>

                <p style='text-align:justify'>eSSUIR представлений через спільноти (детальніше дивись – <a
                        href="struct.jsp">Структура eSSUIR</a>).</p>
            </td>
        </tr>
    </table>
    <br/>
</dspace:layout>
