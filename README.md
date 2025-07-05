# OllamaChat

## Overview

**OllamaChat** is a cutting-edge Minecraft plugin that integrates Ollama and OpenAI-class APIs, enabling real-time AI interactions, multi-language support, and advanced prompt and conversation management for immersive in-game experiences.

## Features

- **AI-Powered Conversations**: Chat with AI using `@bot` or `@ai` prefixes for dynamic, intelligent responses.
- **Ollama & OpenAI Integration**: Leverage advanced AI models to enhance your Minecraft experience.
- **Multi-Language Support**: Supports multiple languages (e.g., English, Simplified Chinese) via `lang` folder files.
- **Toggle AI Services**: Enable/disable AI services with `/ollamachat toggle <ai-name>`.
- **Prompt Management**: Create, delete, list, select, or clear custom prompts to tailor AI interactions.
- **Conversation Management**: Start, switch, delete, or view player-specific conversations with AI entities.
- **Smart Response Suggestions**: Generate configurable, clickable follow-up suggestions with hover text and rate limiting.
- **Tab Completion**: Enhanced usability with Tab completion for `/ollamachat` subcommands.
- **Progress Display**: Visual status bar for prompt answer generation (0% to 100%).
- **Configurable Settings**: Customize suggestion count, prompt templates, preset responses, and model toggles.

## Usage

### Chatting with AI

Type `@bot` or `@ai` followed by your message in Minecraft chat to interact with the AI.

**Example:**
```
@bot What's the best way to build a castle?
```

### Commands

- **/ollamachat reload**: Reloads plugin configuration and language files (`ollamachat.reload`).
- **/ollamachat toggle <ai-name>**: Enables/disables specified AI service (`ollamachat.toggle`).
- **/aichat <ai-name> <prompt>**: Interacts with other AI services (`ollamachat.use`).
- **/ollamachat prompt set <promptName> <promptContent>**: Creates a new prompt (`ollamachat.prompt.set`).
- **/ollamachat prompt delete <promptName>**: Deletes a prompt (`ollamachat.prompt.delete`).
- **/ollamachat prompt list**: Lists all prompts (`ollamachat.prompt.list`).
- **/ollamachat prompt select <promptName>**: Sets default prompt (`ollamachat.prompt.select`).
- **/ollamachat prompt clear**: Resets default prompt (`ollamachat.prompt.select`).
- **/ollamachat conversation new <aiName> <convName>**: Starts a new conversation (`ollamachat.conversation.new`).
- **/ollamachat conversation select <aiName> <convName>**: Switches conversations (`ollamachat.conversation.select`).
- **/ollamachat conversation delete <aiName> <convName>**: Deletes a conversation (`ollamachat.conversation.delete`).
- **/ollamachat conversation list <aiName>**: Lists conversations for an AI (`ollamachat.conversation.list`).
- **/ollamachat suggests toggle**: Toggles suggested responses (`ollamachat.suggests.toggle`).

### Permissions

| Command | Permission | Description |
|---------|------------|-------------|
| `/ollamachat reload` | `ollamachat.reload` | Reloads plugin configuration. |
| `/ollamachat toggle <aiName>` | `ollamachat.toggle` | Toggles specified AI service. |
| `/aichat <aiName> <message>` | `ollamachat.use` | Sends a message to specified AI. |
| `/ollamachat prompt set <promptName> <promptContent>` | `ollamachat.prompt.set` | Creates and saves a new prompt. |
| `/ollamachat prompt delete <promptName>` | `ollamachat.prompt.delete` | Deletes a specified prompt. |
| `/ollamachat prompt list` | `ollamachat.prompt.list` | Lists all prompts and current default. |
| `/ollamachat prompt select <promptName>` | `ollamachat.prompt.select` | Sets a prompt as default. |
| `/ollamachat prompt clear` | `ollamachat.prompt.select` | Resets default prompt. |
| `/ollamachat conversation new <aiName> <convName>` | `ollamachat.conversation.new` | Starts a new conversation. |
| `/ollamachat conversation select <aiName> <convName>` | `ollamachat.conversation.select` | Switches to an existing conversation. |
| `/ollamachat conversation delete <aiName> <convName>` | `ollamachat.conversation.delete` | Deletes a conversation. |
| `/ollamachat conversation list <aiName>` | `ollamachat.conversation.list` | Lists all conversations for an AI. |
| `/ollamachat suggests toggle` | `ollamachat.suggests.toggle` | Toggles suggested responses. |
| `/ollamachat suggests-presets toggle` | `ollamachat.suggests-presets.toggle` | Toggles preset suggested responses. |

**Example:**
```
/aichat ollama Tell me about Redstone
/ollamachat prompt set creativePrompt "Act as a creative Minecraft builder"
/ollamachat suggests toggle
```

## Installation

1. **Download**: Get the latest **OllamaChat**.
2. **Install**: Place the `.jar` file in your server's `plugins` folder.
3. **Configure**: Edit `config.yml` to customize AI settings, prompts, and suggestions.
4. **Reload**: Use `/ollamachat reload` to apply changes.

## Configuration

Customize AI interactions via `config.yml`:

```yaml
# Ollama API
ollama-api-url: "http://localhost:11434/api/generate"
model: "llama3"
ollama-enabled: true

# Streaming settings
stream-settings:
  enabled: true

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
  enabled: true
  type: "bossbar"
  color: "BLUE"
  style: "SOLID"
  update-interval: 1

# Suggested Response
suggested-responses-enabled: false
suggested-response-models:
  - "llama3"
suggested-response-count: 3
suggested-response-prompt: "Conversation:\nUser: {prompt}\nAI: {response}\n\nBased on the above conversation, suggest {count} natural follow-up responses the user might want to say. They should be conversational in tone rather than questions. List them as:\n1. Response 1\n2. Response 2\n3. Response 3"
suggested-response-presets:
  - "I see what you mean."
  - "That's interesting!"
  - "Tell me more about that."
suggested-response-presets-enabled: false
suggested-response-model-toggles:
  - "llama3"
suggested-response-cooldown: 10

# Default prompt
default-prompt: ""

# Custom prompts
prompts:
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

We welcome contributions! Submit issues or pull requests on our [GitHub repository](https://github.com/mcraftbbs/Ollama-Chat).

## License

Licensed under the MIT License. See [LICENSE](https://github.com/mcraftbbs/Ollama-Chat?tab=MIT-1-ov-file).

## Support

For help, visit our [GitHub repository](https://github.com/mcraftbbs/Ollama-Chat) or join our [community server](https://chat.sarskin.cn/invite/iHgI6LTX).


**Note**: **Ollama-Chat** is actively developed, with new features and improvements being added regularly. Stay tuned for updates!
