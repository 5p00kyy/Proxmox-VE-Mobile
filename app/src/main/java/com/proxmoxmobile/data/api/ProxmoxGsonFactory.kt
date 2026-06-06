package com.proxmoxmobile.data.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.proxmoxmobile.data.model.Storage
import java.lang.reflect.Type

object ProxmoxGsonFactory {
    fun create(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(Storage::class.java, StorageDeserializer())
            .create()
    }
}

private class StorageDeserializer : JsonDeserializer<Storage> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Storage {
        val obj = json.asJsonObject
        return Storage(
            storage = obj.string("storage"),
            type = obj.string("type"),
            content = obj.stringList("content"),
            nodes = obj.nullableStringList("nodes"),
            shared = obj.boolean("shared") ?: false,
            active = obj.boolean("active") ?: obj.boolean("enabled") ?: false,
            available = obj.long("available") ?: obj.long("avail") ?: 0,
            used = obj.long("used") ?: 0,
            total = obj.long("total") ?: 0
        )
    }

    private fun JsonObject.string(key: String): String {
        return get(key)?.takeUnless { it.isJsonNull }?.asString.orEmpty()
    }

    private fun JsonObject.long(key: String): Long? {
        return get(key)
            ?.takeUnless { it.isJsonNull }
            ?.runCatching { asLong }
            ?.getOrNull()
    }

    private fun JsonObject.boolean(key: String): Boolean? {
        val value = get(key)?.takeUnless { it.isJsonNull } ?: return null
        return when {
            value.isJsonPrimitive && value.asJsonPrimitive.isBoolean -> value.asBoolean
            value.isJsonPrimitive && value.asJsonPrimitive.isNumber -> value.asInt != 0
            value.isJsonPrimitive && value.asJsonPrimitive.isString -> when (value.asString.lowercase()) {
                "1", "true", "yes", "on" -> true
                "0", "false", "no", "off" -> false
                else -> null
            }
            else -> null
        }
    }

    private fun JsonObject.stringList(key: String): List<String> {
        return nullableStringList(key) ?: emptyList()
    }

    private fun JsonObject.nullableStringList(key: String): List<String>? {
        val value = get(key)?.takeUnless { it.isJsonNull } ?: return null
        return when {
            value.isJsonArray -> value.asJsonArray
                .mapNotNull { it.takeUnless { element -> element.isJsonNull }?.asString?.trim() }
                .filter { it.isNotBlank() }
            value.isJsonPrimitive -> value.asString
                .split(',')
                .map { it.trim() }
                .filter { it.isNotBlank() }
            else -> emptyList()
        }
    }
}
