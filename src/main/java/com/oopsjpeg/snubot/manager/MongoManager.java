package com.oopsjpeg.snubot.manager;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.oopsjpeg.snubot.Snubot;
import com.oopsjpeg.snubot.data.GuildData;
import com.oopsjpeg.snubot.data.UserData;
import com.oopsjpeg.snubot.react.ReactMessage;
import org.bson.Document;

import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Collectors;

public class MongoManager implements Manager
{
    private final Snubot parent;
    private final MongoClient client;
    private final MongoDatabase database;

    public MongoManager(Snubot parent, String host, String database)
    {
        this.parent = parent;
        client = new MongoClient(host);
        this.database = client.getDatabase(database);
    }

    public MongoCollection<Document> getUserDataCollection()
    {
        return database.getCollection("users");
    }

    public MongoCollection<Document> getGuildDataCollection()
    {
        return database.getCollection("guilds");
    }

    public MongoCollection<Document> getReactMessageCollection()
    {
        return database.getCollection("react_messages");
    }

    public Map<String, UserData> fetchUserDataMap()
    {
        Snubot.LOGGER.info("Fetching user data map.");
        return getUserDataCollection().find().into(new LinkedList<>()).stream()
                .map(d -> Snubot.GSON.fromJson(d.toJson(), UserData.class))
                .collect(Collectors.toMap(UserData::getRawId, d -> d));
    }

    public Map<String, GuildData> fetchGuildDataMap()
    {
        Snubot.LOGGER.info("Fetching guild data map.");
        return getGuildDataCollection().find().into(new LinkedList<>()).stream()
                .map(d -> Snubot.GSON.fromJson(d.toJson(), GuildData.class))
                .collect(Collectors.toMap(GuildData::getRawId, d -> d));
    }

    public Map<String, ReactMessage> fetchReactMessageMap()
    {
        Snubot.LOGGER.info("Fetching react message map.");
        return getReactMessageCollection().find().into(new LinkedList<>()).stream()
                .map(d -> Snubot.GSON.fromJson(d.toJson(), ReactMessage.class))
                .collect(Collectors.toMap(ReactMessage::getRawId, d -> d));
    }

    public void saveUserData(UserData data)
    {
        Snubot.LOGGER.info("Saving user data of ID " + data.getRawId() + ".");
        getUserDataCollection().replaceOne(Filters.eq("_id", data.getRawId()), Document.parse(Snubot.GSON.toJson(data)), new ReplaceOptions().upsert(true));
    }

    public void saveGuildData(GuildData data)
    {
        Snubot.LOGGER.info("Saving guild data of ID " + data.getRawId() + ".");
        getGuildDataCollection().replaceOne(Filters.eq("_id", data.getRawId()), Document.parse(Snubot.GSON.toJson(data)), new ReplaceOptions().upsert(true));
    }

    public void saveReactMessage(ReactMessage message)
    {
        Snubot.LOGGER.info("Saving react message of ID " + message.getRawId() + ".");
        getReactMessageCollection().replaceOne(Filters.eq("_id", message.getRawId()), Document.parse(Snubot.GSON.toJson(message)), new ReplaceOptions().upsert(true));
    }

    @Override
    public Snubot getParent()
    {
        return parent;
    }

    public MongoClient getClient()
    {
        return client;
    }

    public MongoDatabase getDatabase()
    {
        return database;
    }
}
