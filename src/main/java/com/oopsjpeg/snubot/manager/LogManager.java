package com.oopsjpeg.snubot.manager;

import com.oopsjpeg.snubot.Manager;
import com.oopsjpeg.snubot.Snubot;
import com.oopsjpeg.snubot.data.impl.GuildData;
import com.oopsjpeg.snubot.util.ChatUtil;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageDeleteEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;

import java.time.LocalDateTime;
import java.util.function.Consumer;

public class LogManager implements Manager
{
    private final Snubot parent;

    public LogManager(Snubot parent)
    {
        this.parent = parent;
    }

    public void onMessageUpdate(MessageUpdateEvent event)
    {
        if (!event.isContentChanged()) return;

        Message message = event.getMessage().block();

        TextChannel channel = event.getChannel().ofType(TextChannel.class).block();
        if (channel == null) return;

        Guild guild = message.getGuild().block();
        if (guild == null || !parent.hasGuildData(guild)) return;

        GuildData data = parent.getGuildData(guild);
        if (!data.getLogging().hasChannel() || data.getLogging().hasIgnoredChannel(channel)) return;

        TextChannel logChannel = data.getLogging().getChannel().block();
        if (channel.equals(logChannel)) return;

        User author = message.getAuthor().orElse(null);
        if (author != null && author.isBot()) return;

        if (author != null)
            logChannel.createEmbed(ChatUtil.authorUser(author).andThen(edit(channel, event.getOld().orElse(null), message))).block();
        else
            logChannel.createEmbed(ChatUtil.authorGuild(guild).andThen(edit(channel, event.getOld().orElse(null), message))).block();
    }

    public void onMessageDelete(MessageDeleteEvent event)
    {
        Message message = event.getMessage().orElse(null);
        if (message == null) return;

        TextChannel channel = event.getChannel().ofType(TextChannel.class).block();
        if (channel == null) return;

        Guild guild = channel.getGuild().block();
        if (guild == null || !parent.hasGuildData(guild)) return;

        GuildData data = parent.getGuildData(guild);
        if (!data.getLogging().hasChannel() || data.getLogging().hasIgnoredChannel(channel)) return;

        TextChannel logChannel = data.getLogging().getChannel().block();
        if (channel.equals(logChannel)) return;

        User author = message.getAuthor().orElse(null);
        if (author != null && author.isBot()) return;

        if (author != null)
            logChannel.createEmbed(ChatUtil.authorUser(author).andThen(delete(channel, message))).block();
        else
            logChannel.createEmbed(ChatUtil.authorGuild(guild).andThen(delete(channel, message))).block();
    }

    private Consumer<EmbedCreateSpec> base()
    {
        LocalDateTime ldt = LocalDateTime.now();
        return e -> e.setFooter(ldt.getYear() + "/"
                + String.format("%02d", ldt.getMonthValue()) + "/"
                + String.format("%02d", ldt.getDayOfMonth()) + " "
                + String.format("%02d", ldt.getHour()) + ":"
                + String.format("%02d", ldt.getMinute()) + ":"
                + String.format("%02d", ldt.getSecond()), null);
    }

    private Consumer<EmbedCreateSpec> edit(Channel channel, Message old, Message now)
    {
        return base().andThen(e ->
        {
            e.setColor(Color.CYAN);
            e.setDescription("**Message edited in " + channel.getMention() + "** ([Jump to Message](" + ChatUtil.url(now) + "))");
            e.addField("Before", old == null || old.getContent().isEmpty() ? "None" : old.getContent(), false);
            e.addField("After", now.getContent(), false);
        });
    }

    private Consumer<EmbedCreateSpec> delete(Channel channel, Message message)
    {
        return base().andThen(e ->
        {
            String content = message != null ? message.getContent() : "None";
            e.setColor(Color.RED);
            e.setDescription("**Message deleted in " + channel.getMention() + "**\n" + content);
        });
    }

    @Override
    public void register(GatewayDiscordClient gateway)
    {
        gateway.on(MessageUpdateEvent.class).subscribe(this::onMessageUpdate);
        gateway.on(MessageDeleteEvent.class).subscribe(this::onMessageDelete);
    }

    @Override
    public Snubot getParent()
    {
        return parent;
    }
}
