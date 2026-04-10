# OllamaChat API Documentation

The `OllamaChat` plugin provides a public API for other Bukkit/Spigot/Paper plugins to interact with its AI chat functionality, including sending queries to AI models, managing conversations, accessing chat history, and performing web searches. This document explains how to use the `OllamaChatAPI` interface to integrate with the plugin.

## Version
This documentation is for `OllamaChat` version 1.1.9+, compatible with Minecraft 1.20+.

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
    implementation 'com.github.mcraftbbs:Ollama-Chat:1.1.9'
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
        <version>1.1.9</version>
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

### AI Query Methods

#### `CompletableFuture<String> sendAIQuery(Player player, String aiName, String prompt)`
#### `CompletableFuture<String> sendAIQueryWithSearch(Player player, String aiName, String prompt)`
#### `CompletableFuture<String> sendAIQueryWithSearch(Player player, String aiName, String prompt, int resultCount)`

Sends a query to the specified AI model and returns the response asynchronously.

- **Parameters**:
    - `player`: The `Player` initiating the query.
    - `aiName`: The name of the AI model (e.g., `"ollama"` or other configured AIs in `config.yml`).
    - `prompt`: The user's input prompt to send to the AI.
    - `resultCount`: Number of web search results to retrieve (1-50).
- **Returns**: A `CompletableFuture<String>` containing the AI's response or an error message.
- **Note**: The `*WithSearch` methods automatically perform a web search and include results in the AI context.

### Conversation Management Methods

#### `String createConversation(UUID playerUuid, String aiModel, String convName)`

Creates a new conversation for a player with the specified AI model.

- **Returns**: The conversation ID as a `String`, or `null` if creation fails.

#### `boolean conversationExistsByName(UUID playerUuid, String aiModel, String convName)`

Checks if a conversation exists by name for a player and AI model.

- **Returns**: `true` if the conversation exists, `false` otherwise.

#### `String getConversationId(UUID playerUuid, String aiModel, String convName)`

Retrieves the conversation ID for a player's conversation with a specific AI model.

- **Returns**: The conversation ID as a `String`, or `null` if not found.

#### `boolean deleteConversation(UUID playerUuid, String aiModel, String convId)`

Deletes a conversation for a player.

- **Returns**: `true` if the conversation was deleted, `false` otherwise.

#### `Map<String, String> listConversations(UUID playerUuid, String aiModel)`

Lists all conversations for a player and AI model.

- **Returns**: A `Map<String, String>` mapping conversation IDs to conversation names.

### Chat History Methods

#### `void saveChatHistory(UUID playerUuid, String aiModel, String conversationId, String prompt, String response)`

Saves a chat history entry for a player.

#### `String getChatHistory(UUID playerUuid, String aiModel, String conversationId)`

Retrieves the chat history for a player's conversation.

- **Returns**: Chat history as a `String`, formatted with "User: " and "AI: " prefixes.

#### `String getChatHistory(UUID playerUuid, String aiModel, String conversationId, int maxEntries)`

Retrieves chat history with a custom entry limit.

- **Parameters**:
    - `maxEntries`: Maximum number of history entries to retrieve.
- **Returns**: Chat history as a `String`.

#### `int getMaxHistory()`

Gets the maximum number of chat history entries stored per conversation, as configured in `config.yml`.

### Web Search Methods

#### `CompletableFuture<List<WebSearchResult>> searchWeb(String query)`
#### `CompletableFuture<List<WebSearchResult>> searchWeb(String query, int count)`

Performs a web search using the configured search engine.

- **Parameters**:
    - `query`: The search query string.
    - `count`: Number of results to return (default: configured value, max: 50).
- **Returns**: A `CompletableFuture<List<WebSearchResult>>` containing search results.

#### `CompletableFuture<String> searchWebFormatted(String query)`
#### `CompletableFuture<String> searchWebFormatted(String query, int count)`

Performs a web search and returns formatted results ready for AI context.

- **Returns**: A `CompletableFuture<String>` with formatted search results.

#### `SearchEngine getCurrentSearchEngine()`

Gets the currently configured search engine.

- **Returns**: The active `SearchEngine` enum value (`BOCHA` or `BRAVE`).

#### `boolean isWebSearchEnabled()`

Checks if web search functionality is enabled.

- **Returns**: `true` if web search is enabled, `false` otherwise.

### WebSearchResult Class

The `WebSearchResult` class represents a single search result:

```java
public class WebSearchResult {
    public String getTitle();      // Result title
    public String getUrl();        // Result URL
    public String getSnippet();    // Result description/snippet
    public String getSiteName();   // Source website name
    public String getDateLastCrawled(); // Last crawl date (if available)
}
```

### SearchEngine Enum

```java
public enum SearchEngine {
    BOCHA,  // Chinese-focused search engine (requires API key)
    BRAVE   // International search engine (requires API key)
}
```

## Example Usage

Below is an example plugin that demonstrates how to use the `OllamaChatAPI` to send AI queries, manage conversations, and perform web searches.

```java
import com.ollamachat.api.OllamaChatAPI;
import com.ollamachat.api.WebSearchResult;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

public class ExamplePlugin extends JavaPlugin {
    private OllamaChatAPI ollamaChatAPI;

    @Override
    public void onEnable() {
        Plugin ollamaPlugin = getServer().getPluginManager().getPlugin("OllamaChat");
        if (ollamaPlugin != null && ollamaPlugin.isEnabled()) {
            ollamaChatAPI = ((com.ollamachat.core.Ollamachat) ollamaPlugin).getAPI();
            getLogger().info("Successfully hooked into OllamaChat API!");
            getCommand("testai").setExecutor(new TestAICommand());
            getCommand("testsearch").setExecutor(new TestSearchCommand());
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

            // Send AI query with web search
            ollamaChatAPI.sendAIQueryWithSearch(player, "ollama", "What's new in Minecraft 1.21?")
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

            // Manage conversations
            String convId = ollamaChatAPI.createConversation(
                player.getUniqueId(), "ollama", "MyChat");
            if (convId != null) {
                player.sendMessage("Created conversation with ID: " + convId);
                Map<String, String> conversations = ollamaChatAPI.listConversations(
                    player.getUniqueId(), "ollama");
                player.sendMessage("Conversations: " + conversations.values());
            }

            return true;
        }
    }

    private class TestSearchCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!ollamaChatAPI.isWebSearchEnabled()) {
                sender.sendMessage("Web search is disabled!");
                return true;
            }

            String query = String.join(" ", args);
            sender.sendMessage("Current search engine: " + 
                ollamaChatAPI.getCurrentSearchEngine().name());

            ollamaChatAPI.searchWeb(query, 5)
                    .thenAccept(results -> {
                        sender.sendMessage("Found " + results.size() + " results:");
                        for (int i = 0; i < results.size(); i++) {
                            WebSearchResult r = results.get(i);
                            sender.sendMessage((i + 1) + ". " + r.getTitle());
                            sender.sendMessage("   " + r.getUrl());
                        }
                    })
                    .exceptionally(throwable -> {
                        sender.sendMessage("Search failed: " + throwable.getMessage());
                        return null;
                    });

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
  testsearch:
    description: Test web search functionality
    usage: /<command> <query>
```

## Notes

- **Asynchronous Operations**: All methods returning `CompletableFuture` are asynchronous. Use `.thenAccept` or similar methods to handle responses in a non-blocking way.
- **Error Handling**: Always handle exceptions in `CompletableFuture` chains to manage potential errors (network issues, disabled AI models, etc.).
- **Permissions**: The API does not enforce permissions, so your plugin should check for appropriate permissions if needed.
- **Web Search**: Web search requires the search engine to be configured with a valid API key in `OllamaChat`'s `config.yml`.
- **Database**: The API uses the `OllamaChat` plugin's database (SQLite or MySQL with HikariCP) for conversation and history management.
- **Streaming Support**: The current API does not support streaming AI responses. Use the standard query methods for complete responses.

## Troubleshooting

- **API is null**: Ensure `OllamaChat` is installed and enabled. Check your plugin's `softdepend` in `plugin.yml`.
- **Web search returns no results**: Verify the search engine is configured with a valid API key in `config.yml`. Check that `web-search.enabled` is set to `true`.
- **No response from AI**: Verify that the AI model (`aiName`) is enabled in `OllamaChat`'s `config.yml` and that the server has internet access.
- **Database errors**: Check the server console for database-related errors. Ensure MySQL or SQLite is correctly configured.

## Support

For issues or feature requests related to the `OllamaChat` API, contact the plugin developers via [GitHub Issues](https://github.com/mcraftbbs/Ollama-Chat/issues).