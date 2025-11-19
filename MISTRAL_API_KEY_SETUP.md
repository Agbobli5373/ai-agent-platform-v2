# Mistral AI API Key Setup

## Current Issue

The application is getting `401 Unauthorized` from Mistral AI API because:
1. The `.env` file is not automatically loaded by Quarkus
2. The API key may be invalid or expired

## Solution Options

### Option 1: Use the Batch Script (Recommended for Windows)

Run the application using the provided batch script that loads `.env`:

```cmd
set-env.bat mvnw.cmd quarkus:dev
```

### Option 2: Set Environment Variable Manually

**Windows PowerShell:**
```powershell
$env:MISTRAL_API_KEY="your-actual-api-key-here"
mvnw.cmd quarkus:dev
```

**Windows CMD:**
```cmd
set MISTRAL_API_KEY=your-actual-api-key-here
mvnw.cmd quarkus:dev
```

### Option 3: Add to application.properties (Dev Only)

For development only, you can hardcode it in `application.properties`:

```properties
%dev.quarkus.langchain4j.mistralai.mistral.api-key=your-actual-api-key-here
```

**⚠️ Warning:** Never commit real API keys to version control!

## Getting a Valid Mistral AI API Key

1. Go to https://console.mistral.ai/
2. Sign up or log in
3. Navigate to "API Keys" section
4. Create a new API key
5. Copy the key (it should be much longer than the current one)
6. Update your `.env` file:
   ```
   MISTRAL_API_KEY=your-new-api-key-from-mistral
   ```

## Verify the Key is Loaded

After setting the environment variable, you can verify it's loaded by checking the logs:

```
quarkus.langchain4j.mistralai.mistral.log-requests=true
```

The logs will show the API key (partially masked) in the request headers.

## Current API Key Issue

The current key in `.env` (`1MWLr73t6wFjDeMFaYClHIZI7mZcJww7`) appears to be:
- Too short for a valid Mistral AI key
- Possibly a placeholder or test key
- Returning 401 Unauthorized

You need to replace it with a valid API key from Mistral AI console.
