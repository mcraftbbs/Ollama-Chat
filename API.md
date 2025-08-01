# OllamaChat API Documentation

The `OllamaChat` plugin provides a public API for other Bukkit/Spigot/Paper plugins to interact with its AI chat functionality, including sending queries to AI models, managing conversations, and accessing chat history. This document explains how to use the `OllamaChatAPI` interface to integrate with the plugin.

## Version
This documentation is for `OllamaChat` version 1.1.5, compatible with Minecraft 1.21.1.

## Getting Started

To use the `OllamaChat` API, your plugin must:
1. Add the JitPack repository and `OllamaChat` dependency to your build file.
2. Declare a soft dependency on `OllamaChat` in your `plugin.yml`.
3. Check if the `OllamaChat` plugin is enabled and retrieve the API instance.
4. Use the `OllamaChatAPI` interface to call methods.

### Step 1: Add JitPack Repository and Dependency

To include the `OllamaChat` library in your project, configure your build system to use the JitPack repository and add the `OllamaChat` dependency.

#### For Gradle Users (settings.gradle)
Add the JitPack repository to your `settings.gradle` at the end of the `repositories` block:

```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

Add the dependency in your `build.gradle`:

```gradle
dependencies {
    implementation 'com.github.mcraftbbs:Ollama-Chat:1.1.5'
}
```

#### For Maven Users (pom.xml)
Add the JitPack repository and dependency to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.mcraftbbs</groupId>
        <artifactId>Ollama-Chat</artifactId>
        <version>1.1.5</version>
    </dependency>
</dependencies>
```

### Step 2: Add Dependency in `plugin.yml`

Add `OllamaChat` as a soft dependency in your `plugin.yml` to ensure it loads after `OllamaChat` if present:

```yaml
name: YourPlugin
version: 1.0
main: com.yourplugin.YourPlugin
api-version: 1.21
softdepend: [OllamaChat]
```

The `softdepend` ensures your plugin loads after `OllamaChat` without requiring it to be present.

### Step 3: Accessing the API

To access the `OllamaChatAPI`, check if the `OllamaChat` plugin is enabled and retrieve the API instance from the main plugin class. Here's an example:

```java
import com.ollamachat.api.OllamaChatAPI;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class YourPlugin extends JavaPlugin {
  private OllamaChatAPI ollamaChatAPI;

  @Override
  public void onEnable() {
    Plugin ollamaPlugin = getServer().getPluginManager().getPlugin("OllamaChat");
    if (ollamaPlugin != null && ollamaPlugin.isEnabled()) {
      ollamaChatAPI = ((com.ollamachat.core.Ollamachat) ollamaPlugin).getAPI();
      getLogger().info("Successfully hooked into OllamaChat API!");
    } else {
      getLogger().warning("OllamaChat plugin not found or disabled!");
      ollamaChatAPI = null;
    }
  }

  public OllamaChatAPI getOllamaChatAPI() {
    return ollamaChatAPI;
  }
}
```

Always check if `ollamaChatAPI` is not `null` before calling API methods to avoid errors if `OllamaChat` is not installed or enabled.

## API Methods

The `OllamaChatAPI` interface (`com.ollamachat.api.OllamaChatAPI`) provides the following methods:

### `CompletableFuture<String> sendAIQuery(Player player, String aiName, String prompt)`

Sends a query to the specified AI model and returns the response asynchronously.

- **Parameters**:
    - `player`: The `Player` initiating the query.
    - `aiName`: The name of the AI model (e.g., `"ollama"` or other configured AIs in `config.yml`).
    - `prompt`: The user's input prompt to send to the AI.
- **Returns**: A `CompletableFuture<String>` containing the AI's response or an error message if the request fails.
- **Throws**: None directly, but the `CompletableFuture` may complete exceptionally if the AI request fails (e.g., network issues or disabled AI model).

### `String createConversation(UUID playerUuid, String aiModel, String convName)`

Creates a new conversation for a player with the specified AI model.

- **Parameters**:
    - `playerUuid`: The `UUID` of the player.
    - `aiModel`: The AI model name (e.g., `"ollama"`).
    - `convName`: The name of the conversation.
- **Returns**: The conversation ID as a `String`, or `null` if creation fails.
- **Throws**: None, but logs errors to the server console if the database operation fails.

### `boolean conversationExistsByName(UUID playerUuid, String aiModel, String convName)`

Checks if a conversation exists by name for a player and AI model.

- **Parameters**:
    - `playerUuid`: The `UUID` of the player.
    - `aiModel`: The AI model name.
    - `convName`: The conversation name.
- **Returns**: `true` if the conversation exists, `false` otherwise.
- **Throws**: None, but logs errors to the server console if the database operation fails.

### `String getConversationId(UUID playerUuid, String aiModel, String convName)`

Retrieves the conversation ID for a player's conversation with a specific AI model.

- **Parameters**:
    - `playerUuid`: The `UUID` of the player.
    - `aiModel`: The AI model name.
    - `convName`: The conversation name.
- **Returns**: The conversation ID as a `String`, or `null` if not found.
- **Throws**: None, but logs errors to the server console if the database operation fails.

### `boolean deleteConversation(UUID playerUuid, String aiModel, String convId)`

Deletes a conversation for a player.

- **Parameters**:
    - `playerUuid`: The `UUID` of the player.
    - `aiModel`: The AI model name.
    - `convId`: The conversation ID.
- **Returns**: `true` if the conversation was deleted, `false` otherwise.
- **Throws**: None, but logs errors to the server console if the database operation fails.

### `Map<String, String> listConversations(UUID playerUuid, String aiModel)`

Lists all conversations for a player and AI model.

- **Parameters**:
    - `playerUuid`: The `UUID` of the player.
    - `aiModel`: The AI model name.
- **Returns**: A `Map<String, String>` mapping conversation IDs to conversation names.
- **Throws**: None, but logs errors to the server console if the database operation fails.

### `void saveChatHistory(UUID playerUuid, String aiModel, String conversationId, String prompt, String response)`

Saves a chat history entry for a player.

- **Parameters**:
    - `playerUuid`: The `UUID` of the player.
    - `aiModel`: The AI model name.
    - `conversationId`: The conversation ID (can be `null` for default conversation).
    - `prompt`: The user's prompt.
    - `response`: The AI's response.
- **Returns**: None.
- **Throws**: None, but logs errors to the server console if the database operation fails.

### `String getChatHistory(UUID playerUuid, String aiModel, String conversationId)`

Retrieves the chat history for a player's conversation.

- **Parameters**:
    - `playerUuid`: The `UUID` of the player.
    - `aiModel`: The AI model name.
    - `conversationId`: The conversation ID (can be `null` for default conversation).
- **Returns**: The chat history as a `String`, formatted with "User: " and "AI: " prefixes.
- **Throws**: None, but logs errors to the server console if the database operation fails.

### `int getMaxHistory()`

Gets the maximum number of chat history entries stored per conversation, as configured in `config.yml`.

- **Returns**: The maximum history limit as an `int`.
- **Throws**: None.

## Example Usage

Below is an example plugin that demonstrates how to use the `OllamaChatAPI` to send an AI query and manage conversations.

```java
import com.ollamachat.api.OllamaChatAPI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class ExamplePlugin extends JavaPlugin {
    private OllamaChatAPI ollamaChatAPI;

    @Override
    public void onEnable() {
        Plugin ollamaPlugin = getServer().getPluginManager().getPlugin("OllamaChat");
        if (ollamaPlugin != null && ollamaPlugin.isEnabled()) {
            ollamaChatAPI = ((com.ollamachat.core.Ollamachat) ollamaPlugin).getAPI();
            getLogger().info("Successfully hooked into OllamaChat API!");
            getCommand("testai").setExecutor(new TestAICommand());
        } else {
            getLogger().warning("OllamaChat plugin not found or disabled!");
        }
    }

    private class TestAICommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("This command can only be used by players!");
                return true;
            }
            Player player = (Player) sender;
            if (ollamaChatAPI == null) {
                player.sendMessage("OllamaChat API is not available!");
                return true;
            }

            // Example: Send an AI query
            ollamaChatAPI.sendAIQuery(player, "ollama", "Hello, how are you?")
                    .thenAccept(response -> {
                        if (player.isOnline()) {
                            player.sendMessage("AI Response: " + response);
                        }
                    })
                    .exceptionally(throwable -> {
                        if (player.isOnline()) {
                            player.sendMessage("Error: " + throwable.getMessage());
                        }
                        return null;
                    });

            // Example: Create and list a conversation
            String convId = ollamaChatAPI.createConversation(player.getUniqueId(), "ollama", "MyChat");
            if (convId != null) {
                player.sendMessage("Created conversation with ID: " + convId);
                Map<String, String> conversations = ollamaChatAPI.listConversations(player.getUniqueId(), "ollama");
                player.sendMessage("Conversations: " + conversations.values().toString());
            } else {
                player.sendMessage("Failed to create conversation!");
            }

            return true;
        }
    }
}
```

Corresponding `plugin.yml` for the example plugin:

```yaml
name: ExamplePlugin
version: 1.0
main: com.example.ExamplePlugin
api-version: 1.21
softdepend: [OllamaChat]
commands:
  testai:
    description: Test the OllamaChat API
    usage: /<command>
```

## Notes

- **Asynchronous Operations**: The `sendAIQuery` method is asynchronous and returns a `CompletableFuture`. Use `.thenAccept` or similar methods to handle the response in a non-blocking way.
- **Error Handling**: Always handle exceptions in `CompletableFuture` chains to manage potential errors (e.g., network issues or disabled AI models).
- **Permissions**: The API does not enforce permissions, so your plugin should check for appropriate permissions if needed.
- **Configuration Dependency**: Some API behavior (e.g., available AI models, history limits) depends on the `OllamaChat` plugin's `config.yml`. Ensure the target server has configured the desired AI models.
- **Streaming Support**: The current API does not support streaming AI responses. If you need streaming, contact the `OllamaChat` developers to request an extension to the API.
- **Database**: The API uses the `OllamaChat` plugin's database (SQLite or MySQL with HikariCP) for conversation and history management. Ensure the database is properly configured in `OllamaChat`'s `config.yml`.

## Troubleshooting

- **API is null**: Ensure `OllamaChat` is installed and enabled on the server. Check your plugin's `softdepend` in `plugin.yml`.
- **No response from AI**: Verify that the AI model (`aiName`) is enabled in `OllamaChat`'s `config.yml` and that the server has internet access to reach the AI's API.
- **Database errors**: Check the server console for database-related errors. Ensure MySQL or SQLite is correctly configured, and HikariCP settings are valid if using MySQL.
- **ClassNotFoundException**: If you encounter issues with dependencies (e.g., MySQL, SQLite, or HikariCP), ensure the `OllamaChat` plugin has downloaded them correctly into its `libs` folder.

## Support

For issues or feature requests related to the `OllamaChat` API, contact the plugin developers via [Github Issues](https://github.com/mcraftbbs/Ollama-Chat/issues).