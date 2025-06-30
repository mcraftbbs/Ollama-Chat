![Version](https://img.shields.io/badge/version-1.1.1-blue)

# Ollama-Chat

[![Download](https://github.com/gabrielvicenteYT/modrinth-icons/blob/main/Branding/Badge/badge-dark.svg)](https://modrinth.com/plugin/ollama-chat)

## Overview

**Ollama-Chat** is a cutting-edge Minecraft plugin that brings the power of Ollama and other AI models directly into your Minecraft world. This plugin enables players to interact with AI in real-time, creating a unique and immersive gameplay experience. Whether you want to chat with an AI companion, ask questions, or simply explore the capabilities of AI, Ollama-Chat makes it possible within the Minecraft universe.

## Features

- **AI-Powered Conversations**: Communicate with AI entities in Minecraft by sending messages prefixed with `@bot`. The AI will respond intelligently, providing a dynamic and engaging interaction.
- **Ollama Integration**: Leverage the advanced capabilities of Ollama to enhance your Minecraft experience.
- **Multi-Language Support**: Supports multiple languages (e.g., English, Simplified Chinese) through language files in the `lang` folder.
- **Toggle AI Services**: Enable or disable AI services dynamically using the `/ollamachat toggle <ai-name>` command.
- **Simple Commands**: Use the `/ollamachat reload` command to reload the plugin configuration instantly, ensuring seamless updates without server restarts.
- **Prompt Management**: Create, delete, list, select, or clear custom prompts to tailor AI responses for specific interactions.
- **Smart Response Suggestions**: Get AI-generated follow-up questions
- **Conversation Management**: Start, switch, delete, or view player-specific conversations linked to AI entities for personalized experiences.
- **Tab Completion**: Enhanced command usability with Tab completion for all `/ollamachat` subcommands.
- **Progress Display**: Status bar for prompt answer generation, showing progress from 0% to 100%.

## Usage

### Chatting with AI

To interact with the AI, simply type `@bot` followed by your message in the Minecraft chat. The AI will process your input and respond accordingly.

**Example:**
```
@bot What is the weather like today?
```

### Commands

- **/ollamachat reload**: Reloads the plugin configuration, including language files and AI settings.
- **/ollamachat toggle <ai-name>**: Enables or disables the specified AI service.
- **/aichat <ai-name> <prompt>**: Interacts with other AI services (e.g., OpenAI).
- **/ollamachat prompt set <promptName> <promptContent>**: Creates and saves a new prompt.
- **/ollamachat prompt delete <promptName>**: Removes a specified prompt.
- **/ollamachat prompt list**: Lists all available prompts and the current default.
- **/ollamachat prompt select <promptName>**: Sets a prompt as the default.
- **/ollamachat prompt clear**: Resets the default prompt.
- **/ollamachat conversation new <aiName> <convName>**: Starts a new conversation with a specific AI.
- **/ollamachat conversation select <aiName> <convName>**: Switches to an existing conversation.
- **/ollamachat conversation delete <aiName> <convName>**: Deletes a specified conversation.
- **/ollamachat conversation list <aiName>**: Lists all conversations for a given AI.

### Permission
| Command | Permission | Description |
|---------|------------|-------------|
| `/ollamachat reload` | `ollamachat.reload` | Reloads the plugin's configuration files. |
| `/ollamachat toggle <aiName>` | `ollamachat.toggle` | Toggles the availability of a specific AI (e.g., "ollama" or other configured AIs). |
| `/ollamachat prompt set <promptName> <promptContent>` | `ollamachat.prompt.set` | Creates and saves a new prompt to the configuration with the specified name and content. |
| `/ollamachat prompt delete <promptName>` | `ollamachat.prompt.delete` | Removes a specified prompt from the configuration. |
| `/ollamachat prompt list` | `ollamachat.prompt.list` | Displays all available prompts with the current default. |
| `/ollamachat prompt select <promptName>` | `ollamachat.prompt.select` | Sets a specified prompt as the default for AI interactions. |
| `/ollamachat prompt clear` | `ollamachat.prompt.select` | Resets the default prompt to none. |
| `/ollamachat conversation new <aiName> <convName>` | `ollamachat.conversation.new` | Starts a new conversation linked to a specific AI with the given conversation name. |
| `/ollamachat conversation select <aiName> <convName>` | `ollamachat.conversation.select` | Switches to an existing conversation for interaction with the specified AI and conversation name. |
| `/ollamachat conversation delete <aiName> <convName>` | `ollamachat.conversation.delete` | Deletes a specified conversation for the given AI. |
| `/ollamachat conversation list <aiName>` | `ollamachat.conversation.list` | Shows all conversations for a given AI, highlighting the active one. |
| `/aichat <aiName> <message>` | `ollamachat.use` | Sends a message to the specified AI (e.g., "ollama" or other configured AIs) for interaction. |

**Example:**
```
/aichat openai Tell me a joke
/ollamachat prompt set weatherPrompt "Provide a weather forecast"
/ollamachat conversation new ollama MyChat
```

## Installation

1. **Download the Plugin**: Obtain the latest version of **Ollama-Chat** from the [official repository](https://github.com/mcraftbbs/Ollama-Chat).
2. **Install the Plugin**: Place the downloaded `.jar` file into the `plugins` folder of your Minecraft server.
3. **Configure the Plugin**: Modify the `config.yml` file to customize AI settings, prompts, and conversations.
4. **Reload the Plugin**: Use the `/ollamachat reload` command to apply any configuration changes.

## Configuration

The plugin's configuration file (`config.yml`) allows you to customize AI interactions, prompts, and conversations.

Example `config.yml`:
```yaml
# Ollama API
ollama-api-url: "http://localhost:11434/api/generate"
model: "llama3"
ollama-enabled: true

# Streaming settings
stream-settings:
  enabled: true      # Whether to enable streaming

# Chat
trigger-prefixes:
  - "@bot"
  - "@ai"

# Length
max-response-length: 500

# History
max-history: 5

# Language Settings
language: "en_us"

# Progress Display Settings
progress-display:
  enabled: true               # Whether to enable progress display
  type: "bossbar"            # Display type (bossbar or actionbar)
  color: "BLUE"              # BossBar color (BLUE, GREEN, RED, etc.)
  style: "SOLID"             # BossBar style (SOLID, SEGMENTED_6, etc.)
  update-interval: 1         # Progress update frequency (in seconds)

# Suggested Response
suggested-responses-enabled: false
suggested-response-models:
  - "llama3"

# Default prompt to prepend to user inputs (empty for none)
default-prompt: ""

# Custom prompts
prompts:
# Example:
# friendly: "You are a friendly assistant who responds in a cheerful tone."
# formal: "You are a professional assistant who responds formally."

# Other AI Configurations
other-ai-configs:
  openai:
    api-url: "https://api.openai.com/v1/chat/completions"
    api-key: "your-openai-api-key"
    model: "gpt-4"
    enabled: false
    messages-format: true
```

## Contributing

We welcome contributions from the community to improve **Ollama-Chat**! If you have ideas, bug reports, or feature requests, please open an issue or submit a pull request on our [GitHub repository](https://github.com/mcraftbbs/Ollama-Chat).

## License

**Ollama-Chat** is licensed under the MIT License. For more details, see the [LICENSE](https://github.com/mcraftbbs/Ollama-Chat?tab=MIT-1-ov-file) file.

## Support

For assistance, questions, or feedback, please visit our [GitHub repository](https://github.com/mcraftbbs/Ollama-Chat) or join our [community server](https://chat.sarskin.cn/invite/iHgI6LTX).

---

**Note**: **Ollama-Chat** is actively developed, with new features and improvements being added regularly. Stay tuned for updates!
