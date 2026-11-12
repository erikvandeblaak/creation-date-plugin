package nl.vandeblaak.creationdate;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * The yellow balloon that shows a creation date.
 */
public final class CreationDateBalloon {

    static final int CORNER_RADIUS = 10;
    static final JBColor FILL = new JBColor(new Color(0xFFF4B5), new Color(0xE9C84A));
    static final JBColor BORDER = new JBColor(new Color(0xE3BC2D), new Color(0xB8961E));
    private static final JBColor HEADER_TEXT = new JBColor(new Color(0x8A6D00), new Color(0x5A4500));
    private static final JBColor DATE_TEXT = new JBColor(new Color(0x3B2F00), new Color(0x231A00));

    private CreationDateBalloon() {
    }

    public static Balloon create(Instant created) {
        return JBPopupFactory.getInstance()
                .createBalloonBuilder(createContent(created, ZoneId.systemDefault(), CreationDateMessages.locale()))
                .setFillColor(FILL)
                .setBorderColor(BORDER)
                .setBorderInsets(JBUI.insets(8, 12))
                .setCornerRadius(JBUI.scale(CORNER_RADIUS))
                .setShadow(true)
                .setAnimationCycle(120)
                .setHideOnClickOutside(true)
                .setHideOnKeyOutside(true)
                .setHideOnAction(true)
                .setRequestFocus(false)
                .setBlockClicksThroughBalloon(false)
                .createBalloon();
    }

    static String formatDate(Instant created, ZoneId zone, Locale locale) {
        return StringUtil.capitalize(format("balloon.date.pattern", created, zone, locale));
    }

    static String formatTime(Instant created, ZoneId zone, Locale locale) {
        return format("balloon.time.pattern", created, zone, locale);
    }

    private static String format(String patternKey, Instant created, ZoneId zone, Locale locale) {
        return DateTimeFormatter.ofPattern(CreationDateMessages.get(patternKey, locale), locale)
                .withZone(zone)
                .format(created);
    }

    static JComponent createContent(Instant created, ZoneId zone, Locale locale) {
        JBLabel header = new JBLabel(CreationDateMessages.get("balloon.header", locale));
        header.setFont(JBFont.small());
        header.setForeground(HEADER_TEXT);

        JBLabel date = new JBLabel(formatDate(created, zone, locale));
        date.setFont(JBFont.label().asBold().biggerOn(1));
        date.setForeground(DATE_TEXT);

        JBLabel time = new JBLabel(formatTime(created, zone, locale));
        time.setForeground(DATE_TEXT);

        JPanel lines = new JPanel();
        lines.setLayout(new BoxLayout(lines, BoxLayout.Y_AXIS));
        lines.setOpaque(false);
        lines.add(header);
        lines.add(Box.createVerticalStrut(JBUI.scale(2)));
        lines.add(date);
        lines.add(time);

        JPanel content = new JPanel(new BorderLayout(JBUI.scale(10), 0));
        content.setOpaque(false);
        content.add(new JBLabel(AllIcons.Vcs.History), BorderLayout.WEST);
        content.add(lines, BorderLayout.CENTER);
        return content;
    }
}
