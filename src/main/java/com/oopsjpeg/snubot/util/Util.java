package com.oopsjpeg.snubot.util;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.rest.util.PermissionSet;

import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class Util
{
    private static final NumberFormat COMMA = NumberFormat.getNumberInstance(Locale.US);
    private static final String CUSTOM_EMOJI_FORMAT = "<:%s:%s>";

    static
    {
        COMMA.setMaximumFractionDigits(0);
    }

    public static String emojiToString(ReactionEmoji emoji)
    {
        return emoji.asCustomEmoji()
                // Custom emoji
                .map(e -> String.format(CUSTOM_EMOJI_FORMAT, e.getName(), e.getId().asString()))
                // Unicode emoji
                .orElse(emoji.asUnicodeEmoji().map(ReactionEmoji.Unicode::getRaw).orElse(null));
    }

    public static ReactionEmoji stringToEmoji(String string)
    {
        // Custom Emoji
        if (string.matches("<a?:.*:\\d+>"))
        {
            String[] split = string.replaceAll("([<>])", "").split(":");
            return ReactionEmoji.custom(Snowflake.of(split[2]), split[1], split[0].equals("a"));
        }
        // Unicode emoji
        else return ReactionEmoji.unicode(string);
    }

    public static boolean searchString(String s1, String s2)
    {
        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();
        return s1.equalsIgnoreCase(s2) || (s1.length() > 3 && (s1.contains(s2) || s2.contains(s1)));
    }

    public static String[] buildArguments(String s)
    {
        String[] split = s.split(" ");
        // Toss the first index, it's the alias
        List<String> base = Arrays.asList(Arrays.copyOfRange(split, 1, split.length));
        List<String> args = new ArrayList<>();

        boolean next = true;
        for (int i = 0; i < base.size(); i++)
        {
            String value = base.get(i);

            if (next) args.add(value.replaceAll("\"", ""));
            else args.set(args.size() - 1, args.get(args.size() - 1) + " " + value.replaceAll("\"", ""));

            // If value starts with " and can be matched
            if (value.startsWith("\"") && base.subList(i + 1, base.size()).stream().anyMatch(a -> a.contains("\"")))
                next = false;
            // If value ends with "
            if (value.endsWith("\""))
                next = true;
        }

        return args.toArray(new String[0]);
    }

    public static boolean isDigits(String s)
    {
        return s.matches("-?\\d+(\\.\\d+)?");
    }

    public static boolean hasPermissions(TextChannel channel, Snowflake userId, PermissionSet permissionSet)
    {
        return channel.getEffectivePermissions(userId).block().containsAll(permissionSet);
    }

    public static String timeDiff(LocalDateTime date1, LocalDateTime date2) {
        Duration duration = Duration.between(date1, date2);
        Stack<String> stack = new Stack<>();

        if (duration.toDays() > 0) stack.push(duration.toDays() + "d");
        duration = duration.minusDays(duration.toDays());

        if (duration.toHours() > 0) stack.push(duration.toHours() + "h");
        duration = duration.minusHours(duration.toHours());

        if (duration.toMinutes() > 0) stack.push(duration.toMinutes() + "m");
        duration = duration.minusMinutes(duration.toMinutes());

        if (duration.getSeconds() > 0) stack.push(duration.getSeconds() + "s");

        return stack.stream().limit(3).collect(Collectors.joining(" "));
    }

    public static String comma(Number number)
    {
        return COMMA.format(number);
    }
}