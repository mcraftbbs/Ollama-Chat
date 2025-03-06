![Version](https://img.shields.io/badge/version-1.0.2-blue)

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

**Example:**
```
/aichat openai Tell me a joke
```

## Installation

1. **Download the Plugin**: Obtain the latest version of **Ollama-Chat** from the [official repository](https://github.com/mcraftbbs/Ollama-Chat).
2. **Install the Plugin**: Place the downloaded `.jar` file into the `plugins` folder of your Minecraft server.
3. **Configure the Plugin**: Modify the `config.yml` file to customize AI settings.
4. **Reload the Plugin**: Use the `/ollamachat reload` command to apply any configuration changes.

## Configuration

The plugin's configuration file (`config.yml`) allows you to customize various aspects of the AI interactions.

Example `config.yml`:
```yaml
# Ollama API
ollama-api-url: "http://localhost:11434/api/generate"
model: "llama3"
ollama-enabled: true

# Chat
trigger-prefix: "@bot "
response-prefix: "§b[AI] §r"

# Length
max-response-length: 500

# History
max-history: 5

# Language Settings
language: "en"  # Default language (en or zh_cn)

# Other AI Configurations
other-ai-configs:
  openai:
    api-url: "https://api.openai.com/v1/chat/completions"
    api-key: "your-openai-api-key"
    model: "gpt-4"
    enabled: true
```

## Contributing

We welcome contributions from the community to improve **Ollama-Chat**! If you have ideas, bug reports, or feature requests, please open an issue or submit a pull request on our [GitHub repository](https://github.com/mcraftbbs/Ollama-Chat).

## License

**Ollama-Chat** is licensed under the MIT License. For more details, see the [LICENSE](LICENSE) file.

## Support

For assistance, questions, or feedback, please visit our [GitHub repository](https://github.com/mcraftbbs/Ollama-Chat) or join our [community server](https://chat.sarskin.cn/invite/iHgI6LTX).

---

**Note**: **Ollama-Chat** is actively developed, with new features and improvements being added regularly. Stay tuned for updates!
