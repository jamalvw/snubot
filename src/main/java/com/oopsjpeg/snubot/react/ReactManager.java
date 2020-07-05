package com.oopsjpeg.snubot.react;

import com.oopsjpeg.snubot.Snubot;
import com.oopsjpeg.snubot.manager.Manager;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.event.domain.message.ReactionRemoveEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.User;
import discord4j.core.object.reaction.ReactionEmoji;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

import static com.oopsjpeg.snubot.react.ReactRole.Type.ONCE;

public class ReactManager implements Manager
{
    private final Snubot parent;
    private final Map<String, ReactMessage> messageMap = new HashMap<>();

    public ReactManager(Snubot parent)
    {
        this.parent = parent;
    }

    public void onReactAdd(ReactionAddEvent event)
    {
        Message message = event.getMessage().block();
        User user = event.getUser().block();
        // Check if the message has data
        if (!user.isBot() && has(message))
        {
            ReactMessage reactMessage = get(message);
            ReactionEmoji emoji = event.getEmoji();
            // Check if the message has this emoji
            if (reactMessage.hasEmoji(emoji))
            {
                // Give the emoji's roles to the user
                ReactEmoji reactEmoji = reactMessage.getEmoji(emoji);
                Member member = event.getMember().get();
                reactEmoji.getRoleList().forEach(role -> member.addRole(role.getId()).block());
            }
        }
    }

    public void onReactRemove(ReactionRemoveEvent event)
    {
        Message message = event.getMessage().block();
        User user = event.getUser().block();
        // Check if the message has data
        if (!user.isBot() && has(message))
        {
            ReactMessage reactMessage = get(message);
            ReactionEmoji emoji = event.getEmoji();
            // Check if the message has this emoji
            if (reactMessage.hasEmoji(emoji))
            {
                // Remove the emoji's roles from the user
                ReactEmoji reactEmoji = reactMessage.getEmoji(emoji);
                Member member = event.getGuild().flatMap(g -> g.getMemberById(user.getId())).block();
                reactEmoji.getRoleList().stream()
                        .filter(role -> role.getType() != ONCE)
                        .forEach(role -> member.removeRole(role.getId()).block());
            }
        }
    }

    public Map<String, ReactMessage> getMessageMap()
    {
        return messageMap;
    }

    public ReactMessage get(Message message)
    {
        return messageMap.getOrDefault(message.getId().asString(), null);
    }

    public ReactMessage add(Message message)
    {
        return messageMap.put(message.getId().asString(), new ReactMessage(message.getId().asString(), message.getChannelId().asString()));
    }

    public ReactMessage getOrAdd(Message message)
    {
        if (!has(message))
            add(message);
        return get(message);
    }

    public ReactMessage remove(Message message)
    {
        return messageMap.remove(message.getId().asString());
    }

    public boolean has(Message message)
    {
        return messageMap.containsKey(message.getId().asString());
    }

    public void update(ReactMessage reactMessage)
    {
        Message message = reactMessage.getMessage().block();
        // Add each emoji to the message
        reactMessage.getEmojiList().forEach(emoji -> message.addReaction(emoji.getReaction()).block());
        // Add "missed" roles, anything the bot somehow didn't catch
        Snowflake guildId = message.getGuild().block().getId();
        // Get each emoji and role
        reactMessage.getEmojiList().forEach(emoji -> emoji.getRoleList().forEach(role ->
                message.getReactors(emoji.getReaction()).toStream().forEach(user ->
                {
                    Member member = user.asMember(guildId).onErrorResume(error -> Mono.empty()).block();
                    if (member != null && member.getRoleIds().contains(role.getId())) {
                        member.addRole(role.getId()).block();
                    }
                })));
    }

    public void addRoleToEmoji(ReactMessage reactMessage, ReactionEmoji emoji, Role role, ReactRole.Type type)
    {
        reactMessage.getOrAddEmoji(emoji).addRole(role, type);

        reactMessage.getMessage().map(m -> m.addReaction(emoji)).subscribe();
    }

    public void removeRoleFromEmoji(ReactMessage reactMessage, ReactionEmoji emoji, Role role)
    {
        Message message = reactMessage.getMessage().block();
        ReactEmoji reactEmoji = reactMessage.getEmoji(emoji);

        // Remove the role from the emoji
        reactEmoji.removeRole(role);

        // Check if the emoji has no more roles
        if (reactEmoji.getRoleMap().isEmpty())
        {
            // Remove the emoji
            reactMessage.removeEmoji(emoji);
            message.removeReactions(emoji).block();
        }

        // Check if the message has no more emojis
        if (reactMessage.getEmojiMap().isEmpty())
        {
            remove(message);
        }
    }

    public void removeRole(ReactMessage reactMessage, Role role)
    {
        reactMessage.getEmojiList().forEach(e -> removeRoleFromEmoji(reactMessage, e.getReaction(), role));
    }

    @Override
    public Snubot getParent()
    {
        return parent;
    }

    @Override
    public void register(GatewayDiscordClient gateway)
    {
        gateway.on(ReactionAddEvent.class).subscribe(this::onReactAdd);
        gateway.on(ReactionRemoveEvent.class).subscribe(this::onReactRemove);
    }
}
